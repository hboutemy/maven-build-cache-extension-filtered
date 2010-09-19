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

import org.codehaus.plexus.logging.Logger;
import org.sonatype.aether.RepositoryEvent;
import org.sonatype.aether.transfer.MetadataNotFoundException;
import org.sonatype.aether.util.listener.AbstractRepositoryListener;

/**
 * @author Benjamin Bentmann
 */
class LoggingRepositoryListener
    extends AbstractRepositoryListener
{

    private final Logger logger;

    public LoggingRepositoryListener( Logger logger )
    {
        this.logger = logger;
    }

    @Override
    public void artifactInstalling( RepositoryEvent event )
    {
        logger.info( "Installing " + event.getArtifact().getFile() + " to " + event.getFile() );
    }

    @Override
    public void metadataInstalling( RepositoryEvent event )
    {
        logger.debug( "Installing " + event.getMetadata() + " to " + event.getFile() );
    }

    @Override
    public void metadataResolved( RepositoryEvent event )
    {
        Exception e = event.getException();
        if ( e != null )
        {
            if ( e instanceof MetadataNotFoundException )
            {
                logger.debug( e.getMessage() );
            }
            else if ( logger.isDebugEnabled() )
            {
                logger.warn( e.getMessage(), e );
            }
            else
            {
                logger.warn( e.getMessage() );
            }
        }
    }

    @Override
    public void metadataInvalid( RepositoryEvent event )
    {
        StringBuilder buffer = new StringBuilder( 256 );
        buffer.append( "The metadata " );
        if ( event.getMetadata().getFile() != null )
        {
            buffer.append( event.getMetadata().getFile() );
        }
        else
        {
            buffer.append( event.getMetadata() );
        }
        buffer.append( " is invalid" );
        if ( event.getException() != null )
        {
            buffer.append( ": " );
            buffer.append( event.getException().getMessage() );
        }

        if ( logger.isDebugEnabled() )
        {
            logger.warn( buffer.toString(), event.getException() );
        }
        else
        {
            logger.warn( buffer.toString() );
        }
    }

    @Override
    public void artifactDescriptorInvalid( RepositoryEvent event )
    {
        StringBuilder buffer = new StringBuilder( 256 );
        buffer.append( "The POM for " );
        buffer.append( event.getArtifact() );
        buffer.append( " is invalid, transitive dependencies (if any) will not be available" );

        if ( logger.isDebugEnabled() )
        {
            logger.warn( buffer + ": " + event.getException().getMessage() );
        }
        else
        {
            logger.warn( buffer + ", enable debug logging for more details" );
        }
    }

    @Override
    public void artifactDescriptorMissing( RepositoryEvent event )
    {
        logger.warn( "The POM for " + event.getArtifact() + " is missing, no dependency information available" );
    }

}
