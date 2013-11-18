package org.apache.maven.execution.scope.internal;

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

import java.util.LinkedList;
import java.util.Map;

import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.execution.scope.MojoExecutionListener;
import org.apache.maven.execution.scope.MojoExecutionScoped;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;

import com.google.common.collect.Maps;
import com.google.inject.AbstractModule;
import com.google.inject.Key;
import com.google.inject.Module;
import com.google.inject.OutOfScopeException;
import com.google.inject.Provider;
import com.google.inject.Scope;
import com.google.inject.util.Providers;

@Named
@Singleton
public class MojoExecutionScope
    implements Scope
{
    private static final Provider<Object> SEEDED_KEY_PROVIDER = new Provider<Object>()
    {
        public Object get()
        {
            throw new IllegalStateException();
        }
    };

    private static final class ScopeState
    {
        public final Map<Key<?>, Provider<?>> seeded = Maps.newHashMap();

        public final Map<Key<?>, Object> provided = Maps.newHashMap();
    }

    private final ThreadLocal<LinkedList<ScopeState>> values = new ThreadLocal<LinkedList<ScopeState>>();

    public MojoExecutionScope()
    {
    }

    public void enter()
    {
        LinkedList<ScopeState> stack = values.get();
        if ( stack == null )
        {
            stack = new LinkedList<ScopeState>();
            values.set( stack );
        }
        stack.addFirst( new ScopeState() );
    }

    private ScopeState getScopeState()
    {
        LinkedList<ScopeState> stack = values.get();
        if ( stack == null || stack.isEmpty() )
        {
            throw new IllegalStateException();
        }
        return stack.getFirst();
    }

    public void exit()
        throws MojoExecutionException
    {
        final LinkedList<ScopeState> stack = values.get();
        if ( stack == null || stack.isEmpty() )
        {
            throw new IllegalStateException();
        }
        stack.removeFirst();
        if ( stack.isEmpty() )
        {
            values.remove();
        }
    }

    public <T> void seed( Class<T> clazz, Provider<T> value )
    {
        getScopeState().seeded.put( Key.get( clazz ), value );
    }

    public <T> void seed( Class<T> clazz, final T value )
    {
        getScopeState().seeded.put( Key.get( clazz ), Providers.of( value ) );
    }

    public <T> Provider<T> scope( final Key<T> key, final Provider<T> unscoped )
    {
        return new Provider<T>()
        {
            @SuppressWarnings( "unchecked" )
            public T get()
            {
                LinkedList<ScopeState> stack = values.get();
                if ( stack == null || stack.isEmpty() )
                {
                    throw new OutOfScopeException( "Cannot access " + key + " outside of a scoping block" );
                }

                ScopeState state = stack.getFirst();

                Provider<?> seeded = state.seeded.get( key );

                if ( seeded != null )
                {
                    return (T) seeded.get();
                }

                T provided = (T) state.provided.get( key );
                if ( provided == null )
                {
                    provided = unscoped.get();
                    state.provided.put( key, provided );
                }

                return provided;
            }
        };
    }

    @SuppressWarnings( { "unchecked" } )
    public static <T> Provider<T> seededKeyProvider()
    {
        return (Provider<T>) SEEDED_KEY_PROVIDER;
    }

    public static Module getScopeModule( PlexusContainer container )
        throws ComponentLookupException
    {
        final MojoExecutionScope scope = container.lookup( MojoExecutionScope.class );
        return new AbstractModule()
        {
            @Override
            protected void configure()
            {
                bindScope( MojoExecutionScoped.class, scope );

                // standard scope bindings
                bind( MavenSession.class ).toProvider( MojoExecutionScope.<MavenSession> seededKeyProvider() ).in( scope );
                bind( MavenProject.class ).toProvider( MojoExecutionScope.<MavenProject> seededKeyProvider() ).in( scope );
                bind( MojoExecution.class ).toProvider( MojoExecutionScope.<MojoExecution> seededKeyProvider() ).in( scope );
            }
        };
    }

    public void afterExecutionSuccess()
        throws MojoExecutionException
    {
        for ( Object provided : getScopeState().provided.values() )
        {
            if ( provided instanceof MojoExecutionListener )
            {
                ( (MojoExecutionListener) provided ).afterMojoExecutionSuccess();
                // TODO maybe deal with multiple MojoExecutionExceptions
            }
        }
    }

    public void afterExecutionAlways()
        throws MojoExecutionException
    {
        for ( Object provided : getScopeState().provided.values() )
        {
            if ( provided instanceof MojoExecutionListener )
            {
                ( (MojoExecutionListener) provided ).afterMojoExecutionAlways();
                // TODO maybe deal with multiple MojoExecutionExceptions
            }
        }
    }

}
