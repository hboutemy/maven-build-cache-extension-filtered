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

import org.apache.maven.artifact.resolver.filter.ArtifactFilter;
import org.apache.maven.artifact.resolver.filter.CumulativeScopeArtifactFilter;
import org.apache.maven.execution.ExecutionEvent;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.lifecycle.LifecycleExecutionException;
import org.apache.maven.lifecycle.MissingProjectException;
import org.apache.maven.plugin.*;
import org.apache.maven.plugin.descriptor.MojoDescriptor;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.util.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Executes an individual mojo
 *
 * @author Jason van Zyl
 * @author Benjamin Bentmann
 * @author Kristian Rosenvold
 *         <p/>
 *         NOTE: This class is not part of any public api and can be changed or deleted without prior notice.
 */
@Component(role = MojoExecutor.class)
public class MojoExecutor
{

    @Requirement
    private BuildPluginManager pluginManager;

    @Requirement
    private LifecycleDependencyResolver lifeCycleDependencyResolver;

    @Requirement
    private ExecutionEventCatapult eventCatapult;

    public MojoExecutor()
    {
    }

    public void execute( MavenSession session, List<MojoExecution> mojoExecutions, ProjectIndex projectIndex,
                         DependencyContext dependencyContext )
        throws LifecycleExecutionException

    {
        PhaseRecorder phaseRecorder = new PhaseRecorder( session.getCurrentProject() );
        for ( MojoExecution mojoExecution : mojoExecutions )
        {
            execute( session, mojoExecution, projectIndex, dependencyContext, phaseRecorder );
        }

    }

    public void execute( MavenSession session, MojoExecution mojoExecution, ProjectIndex projectIndex,
                         DependencyContext dependencyContext, PhaseRecorder phaseRecorder )
        throws LifecycleExecutionException
    {
        execute( session, mojoExecution, projectIndex, dependencyContext );
        phaseRecorder.observeExecution( mojoExecution );
    }

    @SuppressWarnings({"ThrowableInstanceNeverThrown"})
    private void execute( MavenSession session, MojoExecution mojoExecution, ProjectIndex projectIndex,
                          DependencyContext dependencyContext )
        throws LifecycleExecutionException
    {
        MojoDescriptor mojoDescriptor = mojoExecution.getMojoDescriptor();

        if ( mojoDescriptor.isProjectRequired() && !session.isUsingPOMsFromFilesystem() )
        {
            Throwable cause = new MissingProjectException(
                "Goal requires a project to execute" + " but there is no POM in this directory (" +
                    session.getExecutionRootDirectory() + ")." +
                    " Please verify you invoked Maven from the correct directory." );
            throw new LifecycleExecutionException( mojoExecution, null, cause );
        }

        if ( mojoDescriptor.isOnlineRequired() && session.isOffline() )
        {
            if ( MojoExecution.Source.CLI.equals( mojoExecution.getSource() ) )
            {
                Throwable cause = new IllegalStateException(
                    "Goal requires online mode for execution" + " but Maven is currently offline." );
                throw new LifecycleExecutionException( mojoExecution, session.getCurrentProject(), cause );
            }
            else
            {
                eventCatapult.fire( ExecutionEvent.Type.MojoSkipped, session, mojoExecution );

                return;
            }
        }

        lifeCycleDependencyResolver.checkForUpdate( session, dependencyContext );

        List<MavenProject> forkedProjects =
            executeForkedExecutions( mojoExecution, session, projectIndex, dependencyContext );

        eventCatapult.fire( ExecutionEvent.Type.MojoStarted, session, mojoExecution );

        ArtifactFilter artifactFilter = getArtifactFilter( mojoDescriptor );
        List<MavenProject> resolvedProjects =
            LifecycleDependencyResolver.getProjects( session.getCurrentProject(), session,
                                                     mojoDescriptor.isAggregator() );
        for ( MavenProject project : resolvedProjects )
        {
            project.setArtifactFilter( artifactFilter );
        }

        try
        {
            try
            {
                pluginManager.executeMojo( session, mojoExecution );
            }
            catch ( MojoFailureException e )
            {
                throw new LifecycleExecutionException( mojoExecution, session.getCurrentProject(), e );
            }
            catch ( MojoExecutionException e )
            {
                throw new LifecycleExecutionException( mojoExecution, session.getCurrentProject(), e );
            }
            catch ( PluginConfigurationException e )
            {
                throw new LifecycleExecutionException( mojoExecution, session.getCurrentProject(), e );
            }
            catch ( PluginManagerException e )
            {
                throw new LifecycleExecutionException( mojoExecution, session.getCurrentProject(), e );
            }

            eventCatapult.fire( ExecutionEvent.Type.MojoSucceeded, session, mojoExecution );
        }
        catch ( LifecycleExecutionException e )
        {
            eventCatapult.fire( ExecutionEvent.Type.MojoFailed, session, mojoExecution );

            throw e;
        }
        finally
        {
            for ( MavenProject forkedProject : forkedProjects )
            {
                forkedProject.setExecutionProject( null );
            }
        }
    }

    private ArtifactFilter getArtifactFilter( MojoDescriptor mojoDescriptor )
    {
        String scopeToResolve = mojoDescriptor.getDependencyResolutionRequired();
        String scopeToCollect = mojoDescriptor.getDependencyCollectionRequired();

        List<String> scopes = new ArrayList<String>( 2 );
        if ( StringUtils.isNotEmpty( scopeToCollect ) )
        {
            scopes.add( scopeToCollect );
        }
        if ( StringUtils.isNotEmpty( scopeToResolve ) )
        {
            scopes.add( scopeToResolve );
        }

        if ( scopes.isEmpty() )
        {
            return null;
        }
        else
        {
            return new CumulativeScopeArtifactFilter( scopes );
        }
    }

    private List<MavenProject> executeForkedExecutions( MojoExecution mojoExecution, MavenSession session,
                                                        ProjectIndex projectIndex, DependencyContext dependencyContext )
        throws LifecycleExecutionException
    {
        List<MavenProject> forkedProjects = Collections.emptyList();

        Map<String, List<MojoExecution>> forkedExecutions = mojoExecution.getForkedExecutions();

        if ( !forkedExecutions.isEmpty() )
        {
            eventCatapult.fire( ExecutionEvent.Type.ForkStarted, session, mojoExecution );

            MavenProject project = session.getCurrentProject();

            forkedProjects = new ArrayList<MavenProject>( forkedExecutions.size() );

            dependencyContext = dependencyContext.clone();

            try
            {
                for ( Map.Entry<String, List<MojoExecution>> fork : forkedExecutions.entrySet() )
                {
                    int index = projectIndex.getIndices().get( fork.getKey() );

                    MavenProject forkedProject = projectIndex.getProjects().get( fork.getKey() );

                    forkedProjects.add( forkedProject );

                    MavenProject executedProject = forkedProject.clone();

                    forkedProject.setExecutionProject( executedProject );

                    try
                    {
                        session.setCurrentProject( executedProject );
                        session.getProjects().set( index, executedProject );
                        projectIndex.getProjects().put( fork.getKey(), executedProject );

                        eventCatapult.fire( ExecutionEvent.Type.ForkedProjectStarted, session, mojoExecution );

                        execute( session, fork.getValue(), projectIndex, dependencyContext );

                        eventCatapult.fire( ExecutionEvent.Type.ForkedProjectSucceeded, session, mojoExecution );
                    }
                    catch ( LifecycleExecutionException e )
                    {
                        eventCatapult.fire( ExecutionEvent.Type.ForkedProjectFailed, session, mojoExecution );

                        throw e;
                    }
                    finally
                    {
                        projectIndex.getProjects().put( fork.getKey(), forkedProject );
                        session.getProjects().set( index, forkedProject );
                        session.setCurrentProject( project );
                    }
                }

                eventCatapult.fire( ExecutionEvent.Type.ForkSucceeded, session, mojoExecution );
            }
            catch ( LifecycleExecutionException e )
            {
                eventCatapult.fire( ExecutionEvent.Type.ForkFailed, session, mojoExecution );

                throw e;
            }
        }

        return forkedProjects;
    }


}
