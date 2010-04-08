/*
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license
 * agreements. See the NOTICE file distributed with this work for additional information regarding
 * copyright ownership. The ASF licenses this file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License. You may obtain a
 * copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package org.apache.maven.lifecycle.internal;

import org.apache.maven.execution.BuildSuccess;
import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.lifecycle.*;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.logging.Logger;

import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

/**
 * Builds the full lifecycle in weave-mode (phase by phase as opposed to project-by-project)
 * <p/>
 * NOTE: Weave mode is still experimental. It may be either promoted to first class citizen
 * at some later point in time, and it may also be removed entirely. Weave mode has much more aggressive
 * concurrency behaviour than regular threaded mode, and as such is still under test wrt cross platform stability.
 * <p/>
 * To remove weave mode from m3, the following should be removed:
 * ExecutionPlanItem.schedule w/setters and getters
 * DefaultLifeCycles.getScheduling() and all its use
 * ReactorArtifactRepository has a reference to isWeave too.
 * This class and its usage
 *
 * @author Kristian Rosenvold
 *         Builds one or more lifecycles for a full module
 *         <p/>
 *         NOTE: This class is not part of any public api and can be changed or deleted without prior notice.
 */
@Component(role = LifecycleWeaveBuilder.class)
public class LifecycleWeaveBuilder
{
    @Requirement
    private MojoExecutor mojoExecutor;

    @Requirement
    private BuilderCommon builderCommon;

    @Requirement
    private Logger logger;

    @Requirement
    private LifecycleDependencyResolver lifecycleDependencyResolver;


    private final Map<MavenProject, MavenExecutionPlan> executionPlans =
        Collections.synchronizedMap( new HashMap<MavenProject, MavenExecutionPlan>() );


    @SuppressWarnings({"UnusedDeclaration"})
    public LifecycleWeaveBuilder()
    {
    }

    public LifecycleWeaveBuilder( MojoExecutor mojoExecutor, BuilderCommon builderCommon, Logger logger,
                                  LifecycleDependencyResolver lifecycleDependencyResolver )
    {
        this.mojoExecutor = mojoExecutor;
        this.builderCommon = builderCommon;
        this.logger = logger;
        this.lifecycleDependencyResolver = lifecycleDependencyResolver;
    }

    public void build( ProjectBuildList projectBuilds, ReactorContext buildContext, List<TaskSegment> taskSegments,
                       MavenSession session, CompletionService<ProjectSegment> service, ReactorBuildStatus reactorBuildStatus )
        throws ExecutionException, InterruptedException
    {
        ConcurrentBuildLogger concurrentBuildLogger = new ConcurrentBuildLogger();
        try
        {
            final List<Future<ProjectSegment>> futures = new ArrayList<Future<ProjectSegment>>();

            for ( TaskSegment taskSegment : taskSegments )
            {
                ProjectBuildList segmentChunks = projectBuilds.getByTaskSegment(  taskSegment );
                ThreadOutputMuxer muxer = null;  // new ThreadOutputMuxer( segmentChunks, System.out );
                for ( ProjectSegment projectBuild : segmentChunks )
                {
                    try
                    {
                        MavenExecutionPlan executionPlan =
                            builderCommon.resolveBuildPlan( projectBuild.getSession(), projectBuild.getProject(),
                                                            projectBuild.getTaskSegment() );
                        executionPlans.put( projectBuild.getProject(), executionPlan );
                        DependencyContext dependencyContext =
                            new DependencyContext( executionPlan, projectBuild.getTaskSegment().isAggregating() );

                        final Callable<ProjectSegment> projectBuilder =
                            createCallableForBuildingOneFullModule( buildContext, session, reactorBuildStatus,
                                                                    executionPlan, projectBuild, muxer,
                                                                    dependencyContext, concurrentBuildLogger,
                                                                    projectBuilds );

                        futures.add( service.submit( projectBuilder ) );
                    }
                    catch ( Exception e )
                    {
                        throw new ExecutionException( e );
                    }
                }

                for ( Future<ProjectSegment> buildFuture : futures )
                {
                    buildFuture.get();  // At this point, this build *is* finished.
                    // Do not leak threads past here or evil gremlins will get you!
                }
                futures.clear();
            }
        }
        finally
        {
            projectBuilds.closeAll();
        }
        logger.info( concurrentBuildLogger.toString() );
    }

    private Callable<ProjectSegment> createCallableForBuildingOneFullModule( final ReactorContext reactorContext,
                                                                           final MavenSession rootSession,
                                                                           final ReactorBuildStatus reactorBuildStatus,
                                                                           final MavenExecutionPlan executionPlan,
                                                                           final ProjectSegment projectBuild,
                                                                           final ThreadOutputMuxer muxer,
                                                                           final DependencyContext dependencyContext,
                                                                           final ConcurrentBuildLogger concurrentBuildLogger,
                                                                           final ProjectBuildList projectBuilds )
    {
        return new Callable<ProjectSegment>()
        {
            public ProjectSegment call()
                throws Exception
            {
                Iterator<ExecutionPlanItem> planItems = executionPlan.iterator();
                ExecutionPlanItem current = planItems.hasNext() ? planItems.next() : null;
                long buildStartTime = System.currentTimeMillis();

                //muxer.associateThreadWithProjectSegment( projectBuild );

                if ( reactorBuildStatus.isHaltedOrBlacklisted( projectBuild.getProject() ) )
                {
                    DefaultLifecycleExecutor.fireEvent( projectBuild.getSession(), null,
                                                        LifecycleEventCatapult.PROJECT_SKIPPED );
                    return null;
                }

                DefaultLifecycleExecutor.fireEvent( projectBuild.getSession(), null,
                                                    LifecycleEventCatapult.PROJECT_STARTED );

                boolean packagePhaseSeen = false;
                boolean runBAbyRun = false;
                try
                {
                    while ( current != null && !reactorBuildStatus.isHalted() &&
                        !reactorBuildStatus.isBlackListed( projectBuild.getProject() ) )
                    {
                        final String phase = current.getMojoExecution().getMojoDescriptor().getPhase();
                        PhaseRecorder phaseRecorder = new PhaseRecorder( projectBuild.getProject() );

                        if ( !packagePhaseSeen && phase != null && phase.equals( "package" ) )
                        {
                            // Re-resolve. A bit of a kludge ATM
                            packagePhaseSeen = true;
                            lifecycleDependencyResolver.reResolveReactorArtifacts( projectBuilds, false,
                                                                                   projectBuild.getProject(),
                                                                                   projectBuild.getSession(),
                                                                                   executionPlan );

                        }

                        BuiltLogItem builtLogItem =
                            concurrentBuildLogger.createBuildLogItem( projectBuild.getProject(), current );
                        final Schedule schedule = current.getSchedule();

                        if ( schedule != null && schedule.isMojoSynchronized() )
                        {
                            synchronized ( current.getPlugin() )
                            {
                                buildExecutionPlanItem( reactorContext, current, projectBuild, dependencyContext,
                                                        phaseRecorder );
                            }
                        }
                        else
                        {
                            buildExecutionPlanItem( reactorContext, current, projectBuild, dependencyContext,
                                                    phaseRecorder );
                        }

                        current.setComplete();
                        builtLogItem.setComplete();

                        ExecutionPlanItem next = planItems.hasNext() ? planItems.next() : null;
                        if ( next != null )
                        {
                            final Schedule scheduleOfNext = next.getSchedule();
                            if ( !runBAbyRun && ( scheduleOfNext == null || !scheduleOfNext.isParallel() ) )
                            {
                                for ( MavenProject upstreamProject : projectBuild.getImmediateUpstreamProjects() )
                                {
                                    final MavenExecutionPlan upstreamPlan = executionPlans.get( upstreamProject );
                                    final ExecutionPlanItem inSchedule = upstreamPlan.findLastInPhase( next );
                                    if ( inSchedule != null )
                                    {
                                        long startWait = System.currentTimeMillis();
                                        inSchedule.waitUntilDone();
                                        builtLogItem.addWait( upstreamProject, inSchedule, startWait );
                                    }
                                }
                            }
                        }
                        current = next;

                        if ( packagePhaseSeen && !runBAbyRun )
                        {
                            runBAbyRun = true;
                        }
                    }

                    final long wallClockTime = System.currentTimeMillis() - buildStartTime;
                    final BuildSuccess summary =
                        new BuildSuccess( projectBuild.getProject(), wallClockTime ); // - waitingTime 
                    reactorContext.getResult().addBuildSummary( summary );
                    DefaultLifecycleExecutor.fireEvent( projectBuild.getSession(), null,
                                                        LifecycleEventCatapult.PROJECT_SUCCEEDED );

                }
                catch ( Exception e )
                {
                    BuilderCommon.handleBuildError( reactorContext, rootSession, projectBuild.getProject(), e,
                                                    buildStartTime );
                }
                finally
                {
                    if ( current != null )
                    {
                        executionPlan.forceAllComplete();
                    }
                    // muxer.setThisModuleComplete( projectBuild );
                }
                return null;
            }
        };
    }

    private void buildExecutionPlanItem( ReactorContext reactorContext, ExecutionPlanItem node,
                                         ProjectSegment projectBuild, DependencyContext dependencyContext,
                                         PhaseRecorder phaseRecorder )
        throws LifecycleExecutionException
    {

        MavenProject currentProject = projectBuild.getProject();

        long buildStartTime = System.currentTimeMillis();

        MavenSession sessionForThisModule = projectBuild.getSession();
        try
        {

            if ( reactorContext.getReactorBuildStatus().isHaltedOrBlacklisted( currentProject ) )
            {
                return;
            }

            BuilderCommon.attachToThread( currentProject );

            mojoExecutor.execute( sessionForThisModule, node.getMojoExecution(), reactorContext.getProjectIndex(),
                                  dependencyContext, phaseRecorder );

            final BuildSuccess summary =
                new BuildSuccess( currentProject, System.currentTimeMillis() - buildStartTime );
            reactorContext.getResult().addBuildSummary( summary );
        }
        finally
        {
            Thread.currentThread().setContextClassLoader( reactorContext.getOriginalContextClassLoader() );
        }
    }

    public static boolean isWeaveMode( MavenExecutionRequest request )
    {
        return "true".equals( request.getUserProperties().getProperty( "maven3.weaveMode" ) );
    }

    public static void setWeaveMode( Properties properties )
    {
        properties.setProperty( "maven3.weaveMode", "true" );
    }
}