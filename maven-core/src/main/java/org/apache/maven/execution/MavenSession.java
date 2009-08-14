package org.apache.maven.execution;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.RepositoryCache;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectBuildingRequest;
import org.apache.maven.settings.Settings;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;

/**
 * @author Jason van Zyl
 * @version $Id$
 */
public class MavenSession
{
    private PlexusContainer container;
    
    private MavenExecutionRequest request;

    private MavenExecutionResult result;

    private Properties executionProperties;

    private MavenProject currentProject;
        
    /**
     * These projects have already been topologically sorted in the {@link org.apache.maven.Maven} component before
     * being passed into the session.
     */
    private List<MavenProject> projects;
    
    private MavenProject topLevelProject;

    private ProjectDependencyGraph projectDependencyGraph;

    private Collection<String> blackListedProjects;

    @Deprecated
    public MavenSession( PlexusContainer container, MavenExecutionRequest request, MavenExecutionResult result, MavenProject project )
    {
        this( container, request, result, Arrays.asList( new MavenProject[]{ project } ) );        
    }    

    @Deprecated
    public MavenSession( PlexusContainer container, MavenExecutionRequest request, MavenExecutionResult result, List<MavenProject> projects )
    {
        this.container = container;
        this.request = request;
        this.result = result;
        setProjects( projects );     
    }

    public MavenSession( PlexusContainer container, MavenExecutionRequest request, MavenExecutionResult result )
    {
        this.container = container;
        this.request = request;
        this.result = result;
    }

    public void setProjects( List<MavenProject> projects )
    {
        //TODO: Current for testing classes creating the session
        if ( projects.size() > 0 )
        {
            this.currentProject = projects.get( 0 );
            this.topLevelProject = projects.get(  0 );
        }
        this.projects = projects;
    }    
        
    @Deprecated
    public PlexusContainer getContainer()
    {
        return container;
    }

    @Deprecated
    public Object lookup( String role )
        throws ComponentLookupException
    {
        return container.lookup( role );
    }

    @Deprecated
    public Object lookup( String role, String roleHint )
        throws ComponentLookupException
    {
        return container.lookup( role, roleHint );
    }

    @Deprecated
    public List lookupList( String role )
        throws ComponentLookupException
    {
        return container.lookupList( role );
    }

    @Deprecated
    public Map lookupMap( String role )
        throws ComponentLookupException
    {
        return container.lookupMap( role );
    }

    public RepositoryCache getRepositoryCache()
    {
        return request.getRepositoryCache();
    }

    public ArtifactRepository getLocalRepository()
    {
        return request.getLocalRepository();
    }

    public List<String> getGoals()
    {
        return request.getGoals();
    }

    /**
     * Gets the user properties to use for interpolation and profile activation. The user properties have been
     * configured directly by the user on his discretion, e.g. via the {@code -Dkey=value} parameter on the command
     * line.
     * 
     * @return The user properties, never {@code null}.
     */
    public Properties getUserProperties()
    {
        return request.getUserProperties();
    }

    /**
     * Gets the system properties to use for interpolation and profile activation. The system properties are collected
     * from the runtime environment like {@link System#getProperties()} and environment variables.
     * 
     * @return The system properties, never {@code null}.
     */
    public Properties getSystemProperties()
    {
        return request.getSystemProperties();
    }

    /**
     * @deprecated Use either {@link #getUserProperties()} or {@link #getSystemProperties()}.
     */
    @Deprecated
    public Properties getExecutionProperties()
    {
        if ( executionProperties == null )
        {
            executionProperties = new Properties();
            executionProperties.putAll( request.getSystemProperties() );
            executionProperties.putAll( request.getUserProperties() );
        }

        return executionProperties;
    }

    public Settings getSettings()
    {
        return request.getSettings();
    }
    
    public List<MavenProject> getProjects()
    {
        return projects;
    }

    public String getExecutionRootDirectory()
    {
        return request.getBaseDirectory();
    }

    public boolean isUsingPOMsFromFilesystem()
    {
        return request.isProjectPresent();
    }

    public MavenExecutionRequest getRequest()
    {
        return request;
    }

    public void setCurrentProject( MavenProject currentProject )
    {
        this.currentProject = currentProject;
    }

    public MavenProject getCurrentProject()
    {
        return currentProject;
    }

    public ProjectBuildingRequest getProjectBuildingRequest()
    {
        return request.getProjectBuildingRequest();
    }
    
    public List<String> getPluginGroups()
    {
        return request.getPluginGroups();
    }
    
    public boolean isOffline()
    {
        return request.isOffline();
    }        

    public MavenProject getTopLevelProject()
    {
        return topLevelProject;
    }

    public MavenExecutionResult getResult()
    {
        return result;
    }        

    private Map<String,Map<String,Map<String,Object>>>  pluginContextsByProjectAndPluginKey = new HashMap<String,Map<String,Map<String,Object>>> ();
    
    // Backward compat
    public Map<String, Object> getPluginContext( PluginDescriptor plugin, MavenProject project )
    {
        String projectKey = project.getId();

        Map<String, Map<String, Object>> pluginContextsByKey = pluginContextsByProjectAndPluginKey.get( projectKey );

        if ( pluginContextsByKey == null )
        {
            pluginContextsByKey = new HashMap<String, Map<String, Object>>();

            pluginContextsByProjectAndPluginKey.put( projectKey, pluginContextsByKey );
        }

        String pluginKey = plugin.getPluginLookupKey();

        Map<String, Object> pluginContext = pluginContextsByKey.get( pluginKey );

        if ( pluginContext == null )
        {
            pluginContext = new HashMap<String, Object>();

            pluginContextsByKey.put( pluginKey, pluginContext );
        }

        return pluginContext;
    }

    public void setProjectDependencyGraph( ProjectDependencyGraph projectDependencyGraph )
    {
        this.projectDependencyGraph = projectDependencyGraph;
    }

    public String getReactorFailureBehavior()
    {
        return request.getReactorFailureBehavior();
    }

    public boolean isBlackListed( MavenProject project )
    {
        return blackListedProjects != null && blackListedProjects.contains( getId( project ) );
    }

    public void blackList( MavenProject project )
    {
        if ( blackListedProjects == null )
        {
            blackListedProjects = new HashSet<String>();
        }

        if ( blackListedProjects.add( getId( project ) ) && projectDependencyGraph != null )
        {
            for ( MavenProject downstreamProject : projectDependencyGraph.getDownstreamProjects( project, true ) )
            {
                blackListedProjects.add( getId( downstreamProject ) );
            }
        }
    }

    private String getId( MavenProject project )
    {
        return project.getGroupId() + ':' + project.getArtifactId() + ':' + project.getVersion();
    }

}
