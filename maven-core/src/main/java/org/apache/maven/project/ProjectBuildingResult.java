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

import java.io.File;
import java.util.List;

import org.apache.maven.model.building.ModelProblem;

/**
 * Collects the output of the project builder.
 * 
 * @author Benjamin Bentmann
 */
public interface ProjectBuildingResult
{

    /**
     * Gets the POM file from which the project was built.
     * 
     * @return The POM file or {@code null} if unknown.
     */
    File getPomFile();

    /**
     * Gets the project that was built.
     * 
     * @return The project that was built or {@code null} if an error occurred.
     */
    MavenProject getProject();

    /**
     * Gets the problems that were encountered during the project building.
     * 
     * @return The problems that were encountered during the project building, can be empty but never {@code null}.
     */
    List<ModelProblem> getProblems();

}
