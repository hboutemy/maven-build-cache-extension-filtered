package org.apache.maven.caching.xml;

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

import org.apache.maven.caching.domain.CacheType;
import org.apache.maven.caching.domain.io.xpp3.CacheConfigXpp3Reader;
import org.apache.maven.caching.domain.io.xpp3.CacheConfigXpp3Writer;
import org.apache.maven.caching.domain.io.xpp3.CacheDiffXpp3Reader;
import org.apache.maven.caching.domain.io.xpp3.CacheDiffXpp3Writer;
import org.apache.maven.caching.domain.io.xpp3.CacheDomainXpp3Reader;
import org.apache.maven.caching.domain.BuildDiffType;
import org.apache.maven.caching.domain.BuildInfoType;
import org.apache.maven.caching.domain.CacheReportType;
import org.apache.maven.caching.domain.io.xpp3.CacheDomainXpp3Writer;
import org.apache.maven.caching.domain.io.xpp3.CacheReportXpp3Reader;
import org.apache.maven.caching.domain.io.xpp3.CacheReportXpp3Writer;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

/**
 * XmlService
 */
@Component( role = XmlService.class )
public class XmlService
{

    public byte[] toBytes( CacheType cache ) throws IOException
    {
        try ( ByteArrayOutputStream baos = new ByteArrayOutputStream() )
        {
            new CacheConfigXpp3Writer().write( baos, cache );
            return baos.toByteArray();
        }
    }

    public byte[] toBytes( BuildInfoType buildInfo ) throws IOException
    {
        try ( ByteArrayOutputStream baos = new ByteArrayOutputStream() )
        {
            new CacheDomainXpp3Writer().write( baos, buildInfo );
            return baos.toByteArray();
        }
    }

    public byte[] toBytes( BuildDiffType diff ) throws IOException
    {
        try ( ByteArrayOutputStream baos = new ByteArrayOutputStream() )
        {
            new CacheDiffXpp3Writer().write( baos, diff );
            return baos.toByteArray();
        }
    }

    public byte[] toBytes( CacheReportType cacheReportType ) throws IOException
    {
        try ( ByteArrayOutputStream baos = new ByteArrayOutputStream() )
        {
            new CacheReportXpp3Writer().write( baos, cacheReportType );
            return baos.toByteArray();
        }
    }

    public <T> T fromFile( Class<T> clazz, File file ) throws IOException, XmlPullParserException
    {
        if ( clazz == BuildInfoType.class )
        {
            return clazz.cast( new CacheDomainXpp3Reader().read( Files.newInputStream( file.toPath() ) ) );
        }
        else if ( clazz == CacheType.class )
        {
            return clazz.cast( new CacheConfigXpp3Reader().read( Files.newInputStream( file.toPath() ) ) );
        }
        else if ( clazz == BuildDiffType.class )
        {
            return clazz.cast( new CacheDiffXpp3Reader().read( Files.newInputStream( file.toPath() ) ) );
        }
        else if ( clazz == CacheReportType.class )
        {
            return clazz.cast( new CacheReportXpp3Reader().read( Files.newInputStream( file.toPath() ) ) );
        }
        else
        {
            throw new IllegalArgumentException( "Unsupported type " + clazz );
        }
    }

    public <T> T fromBytes( Class<T> clazz, byte[] bytes )
    {
        try
        {
            if ( clazz == BuildInfoType.class )
            {
                return clazz.cast( new CacheDomainXpp3Reader().read( new ByteArrayInputStream( bytes ) ) );
            }
            else if ( clazz == CacheType.class )
            {
                return clazz.cast( new CacheConfigXpp3Reader().read( new ByteArrayInputStream( bytes ) ) );
            }
            else if ( clazz == BuildDiffType.class )
            {
                return clazz.cast( new CacheDiffXpp3Reader().read( new ByteArrayInputStream( bytes ) ) );
            }
            else if ( clazz == CacheReportType.class )
            {
                return clazz.cast( new CacheReportXpp3Reader().read( new ByteArrayInputStream( bytes ) ) );
            }
            else
            {
                throw new IllegalArgumentException( "Unsupported type " + clazz );
            }
        }
        catch ( IOException | XmlPullParserException e )
        {
            throw new RuntimeException( "Unable to parse cache xml element", e );
        }
    }
}
