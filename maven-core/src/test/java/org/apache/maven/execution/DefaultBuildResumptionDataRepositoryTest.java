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
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.is;

@RunWith( MockitoJUnitRunner.class )
public class DefaultBuildResumptionDataRepositoryTest
{
    private final DefaultBuildResumptionDataRepository repository = new DefaultBuildResumptionDataRepository();

    @Test
    public void resumeFromPropertyGetsApplied()
    {
        MavenExecutionRequest request = new DefaultMavenExecutionRequest();
        Properties properties = new Properties();
        properties.setProperty( "resumeFrom", ":module-a" );

        repository.applyResumptionProperties( request, properties );

        assertThat( request.getResumeFrom(), is( ":module-a" ) );
    }

    @Test
    public void resumeFromPropertyDoesNotOverrideExistingRequestParameters()
    {
        MavenExecutionRequest request = new DefaultMavenExecutionRequest();
        request.setResumeFrom( ":module-b" );
        Properties properties = new Properties();
        properties.setProperty( "resumeFrom", ":module-a" );

        repository.applyResumptionProperties( request, properties );

        assertThat( request.getResumeFrom(), is( ":module-b" ) );
    }

    @Test
    public void excludedProjectsFromPropertyGetsAddedToExistingRequestParameters()
    {
        MavenExecutionRequest request = new DefaultMavenExecutionRequest();
        List<String> excludedProjects = new ArrayList<>();
        excludedProjects.add( ":module-a" );
        request.setExcludedProjects( excludedProjects );
        Properties properties = new Properties();
        properties.setProperty( "excludedProjects", ":module-b, :module-c" );

        repository.applyResumptionProperties( request, properties );

        assertThat( request.getExcludedProjects(), contains( ":module-a", ":module-b", ":module-c" ) );
    }
}
