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
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import java.util.List;

import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.metadata.ArtifactMetadataRetrievalException;
import org.apache.maven.artifact.metadata.ResolutionGroup;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.metadata.RepositoryMetadataManager;
import org.apache.maven.plugin.LegacySupport;
import org.apache.maven.project.artifact.MavenMetadataCache;
import org.apache.maven.project.artifact.MavenMetadataSource;

@SuppressWarnings( "deprecation" )
@Named( "classpath" )
@Singleton
public class TestMetadataSource
    extends MavenMetadataSource
{

    public TestMetadataSource( RepositoryMetadataManager repositoryMetadataManager, ArtifactFactory repositorySystem, ProjectBuilder projectBuilder, MavenMetadataCache cache, LegacySupport legacySupport) {
        super( repositoryMetadataManager, repositorySystem, projectBuilder, cache, legacySupport );
    }

    @Override
    public ResolutionGroup retrieve( Artifact artifact, ArtifactRepository localRepository,
                                     List<ArtifactRepository> remoteRepositories )
        throws ArtifactMetadataRetrievalException
    {
        ResolutionGroup rg = super.retrieve( artifact, localRepository, remoteRepositories );

        for ( Artifact a : rg.getArtifacts() )
        {
            a.setResolved( true );
        }

        return rg;
    }
}
