package org.apache.maven.project;

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

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.DefaultArtifactRepository;
import org.apache.maven.artifact.repository.layout.ArtifactRepositoryLayout;
import org.codehaus.plexus.util.FileUtils;

public class DefaultMavenProjectBuilderTest
    extends AbstractMavenProjectTestCase
{

    private List<File> filesToDelete = new ArrayList<File>();

    private File localRepoDir;

    @Override
    public void setUp()
        throws Exception
    {
        super.setUp();

        projectBuilder = lookup( ProjectBuilder.class );

        localRepoDir = new File( System.getProperty( "java.io.tmpdir" ), "local-repo." + System.currentTimeMillis() );
        localRepoDir.mkdirs();

        filesToDelete.add( localRepoDir );
    }

    @Override
    public void tearDown()
        throws Exception
    {
        super.tearDown();

        if ( !filesToDelete.isEmpty() )
        {
            for ( Iterator<File> it = filesToDelete.iterator(); it.hasNext(); )
            {
                File file = it.next();

                if ( file.exists() )
                {
                    if ( file.isDirectory() )
                    {
                        FileUtils.deleteDirectory( file );
                    }
                    else
                    {
                        file.delete();
                    }
                }
            }
        }
    }

    /**
     * Check that we can build ok from the middle pom of a (parent,child,grandchild) heirarchy
     * @throws Exception
     */
    public void testBuildFromMiddlePom() throws Exception
    {
        File f1 = getTestFile( "src/test/resources/projects/grandchild-check/child/pom.xml");
        File f2 = getTestFile( "src/test/resources/projects/grandchild-check/child/grandchild/pom.xml");

        getProject( f1 );

        // it's the building of the grandchild project, having already cached the child project
        // (but not the parent project), which causes the problem.
        getProject( f2 );
    }

    public void testDuplicatePluginDefinitionsMerged()
        throws Exception
    {
        File f1 = getTestFile( "src/test/resources/projects/duplicate-plugins-merged-pom.xml" );

        MavenProject project = getProject( f1 );
        assertEquals( 2, project.getBuildPlugins().get( 0 ).getDependencies().size() );
        assertEquals( 2, project.getBuildPlugins().get( 0 ).getExecutions().size() );
        assertEquals( "first", project.getBuildPlugins().get( 0 ).getExecutions().get( 0 ).getId() );
    }


    @Override
    protected ArtifactRepository getLocalRepository()
        throws Exception
    {
        ArtifactRepositoryLayout repoLayout = lookup( ArtifactRepositoryLayout.class, "default" );
        ArtifactRepository r = repositorySystem.createArtifactRepository( "local", "file://" + localRepoDir.getAbsolutePath(), repoLayout, null, null );
        return r;
    }
    
    public void xtestLoop() throws Exception
    {
        while( true )
        {
        File f1 = getTestFile( "src/test/resources/projects/duplicate-plugins-merged-pom.xml" );
        getProject( f1 );
        }
    }
    
}
