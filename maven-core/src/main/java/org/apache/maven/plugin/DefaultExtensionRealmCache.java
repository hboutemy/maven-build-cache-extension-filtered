package org.apache.maven.plugin;

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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.project.ExtensionDescriptor;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.classworlds.realm.ClassRealm;
import org.codehaus.plexus.component.annotations.Component;

/**
 * Default extension realm cache implementation. Assumes cached data does not change.
 */
@Component( role = ExtensionRealmCache.class )
public class DefaultExtensionRealmCache
    implements ExtensionRealmCache
{

    private static class CacheKey
    {

        private final List<File> files;

        private final List<String> ids;

        private final int hashCode;

        public CacheKey( List<? extends Artifact> extensionArtifacts )
        {
            this.files = new ArrayList<File>( extensionArtifacts.size() );
            this.ids = new ArrayList<String>( extensionArtifacts.size() );

            for ( Artifact artifact : extensionArtifacts )
            {
                files.add( artifact.getFile() );
                ids.add( artifact.getVersion() );
            }

            this.hashCode = files.hashCode() * 31 + ids.hashCode();
        }

        @Override
        public int hashCode()
        {
            return hashCode;
        }

        @Override
        public boolean equals( Object o )
        {
            if ( o == this )
            {
                return true;
            }

            if ( !( o instanceof CacheKey ) )
            {
                return false;
            }

            CacheKey other = (CacheKey) o;

            return files.equals( other.files ) && ids.equals( other.ids );
        }

    }

    private final Map<CacheKey, CacheRecord> cache = new HashMap<CacheKey, CacheRecord>();

    public CacheRecord get( List<? extends Artifact> extensionArtifacts )
    {
        return cache.get( new CacheKey( extensionArtifacts ) );
    }

    public CacheRecord put( List<? extends Artifact> extensionArtifacts, ClassRealm extensionRealm,
                            ExtensionDescriptor extensionDescriptor )
    {
        if ( extensionRealm == null )
        {
            throw new NullPointerException();
        }

        CacheKey key = new CacheKey( extensionArtifacts );

        if ( cache.containsKey( key ) )
        {
            throw new IllegalStateException( "Duplicate extension realm for extension " + extensionArtifacts );
        }

        CacheRecord record = new CacheRecord( extensionRealm, extensionDescriptor );

        cache.put( key, record );

        return record;
    }

    public void flush()
    {
        cache.clear();
    }

    public void register( MavenProject project, CacheRecord record )
    {
        // default cache does not track extension usage
    }

}
