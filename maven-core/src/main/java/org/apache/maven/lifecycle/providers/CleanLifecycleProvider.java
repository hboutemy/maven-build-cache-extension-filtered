package org.apache.maven.lifecycle.providers;

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
import java.util.Collections;
import java.util.HashMap;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.apache.maven.lifecycle.Lifecycle;
import org.apache.maven.lifecycle.mapping.LifecyclePhase;

@Named( "clean" )
@Singleton
public final class CleanLifecycleProvider
    implements Provider<Lifecycle>
{
  private final Lifecycle lifecycle;

  @Inject
  public CleanLifecycleProvider()
  {
    HashMap<String, LifecyclePhase> phases = new HashMap<>();
    phases.put( "clean", new LifecyclePhase( "org.apache.maven.plugins:maven-clean-plugin:3.1.0:clean" ) );

    this.lifecycle = new Lifecycle(
        "clean",
        Collections.unmodifiableList( Arrays.asList(
                "pre-clean",
                "clean",
                "post-clean"
        ) ),
        Collections.unmodifiableMap( phases )
    );
  }

  @Override
  public Lifecycle get()
  {
    return lifecycle;
  }
}
