package org.apache.maven;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

import org.apache.maven.execution.ProjectDependencyGraph;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectSorter;

/**
 * Describes the inter-dependencies between projects in the reactor.
 * 
 * @author Benjamin Bentmann
 */
class DefaultProjectDependencyGraph
    implements ProjectDependencyGraph
{

    private ProjectSorter sorter;

    /**
     * Creates a new project dependency graph based on the specified project sorting.
     * 
     * @param sorter The project sorter backing the graph, must not be {@code null}.
     */
    public DefaultProjectDependencyGraph( ProjectSorter sorter )
    {
        if ( sorter == null )
        {
            throw new IllegalArgumentException( "project sorter missing" );
        }

        this.sorter = sorter;
    }

    public List<MavenProject> getSortedProjects()
    {
        return new ArrayList<MavenProject>( sorter.getSortedProjects() );
    }

    public List<MavenProject> getDownstreamProjects( MavenProject project, boolean transitive )
    {
        if ( project == null )
        {
            throw new IllegalArgumentException( "project missing" );
        }

        Collection<String> projectIds = new HashSet<String>();

        getDownstreamProjects( ProjectSorter.getId( project ), projectIds, transitive );

        return getProjects( projectIds );
    }

    private void getDownstreamProjects( String projectId, Collection<String> projectIds, boolean transitive )
    {
        for ( String id : sorter.getDependents( projectId ) )
        {
            projectIds.add( id );

            if ( transitive )
            {
                getDownstreamProjects( id, projectIds, transitive );
            }
        }
    }

    public List<MavenProject> getUpstreamProjects( MavenProject project, boolean transitive )
    {
        if ( project == null )
        {
            throw new IllegalArgumentException( "project missing" );
        }

        Collection<String> projectIds = new HashSet<String>();

        getUpstreamProjects( ProjectSorter.getId( project ), projectIds, transitive );

        return getProjects( projectIds );
    }

    private void getUpstreamProjects( String projectId, Collection<String> projectIds, boolean transitive )
    {
        for ( String id : sorter.getDependencies( projectId ) )
        {
            projectIds.add( id );

            if ( transitive )
            {
                getUpstreamProjects( id, projectIds, transitive );
            }
        }
    }

    private List<MavenProject> getProjects( Collection<String> projectIds )
    {
        List<MavenProject> projects = new ArrayList<MavenProject>();

        for ( MavenProject p : sorter.getSortedProjects() )
        {
            if ( projectIds.contains( ProjectSorter.getId( p ) ) )
            {
                projects.add( p );
            }
        }

        return projects;
    }

    @Override
    public String toString()
    {
        return sorter.getSortedProjects().toString();
    }

}
