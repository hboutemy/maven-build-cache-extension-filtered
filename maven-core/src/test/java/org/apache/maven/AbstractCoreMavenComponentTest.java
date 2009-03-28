package org.apache.maven;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.execution.DefaultMavenExecutionRequest;
import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.monitor.event.DefaultEventMonitor;
import org.apache.maven.plugin.MavenPluginCollector;
import org.apache.maven.plugin.MavenPluginDiscoverer;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.plugin.PluginManager;
import org.apache.maven.plugin.descriptor.MojoDescriptor;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.project.DefaultProjectBuilderConfiguration;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectBuilder;
import org.apache.maven.project.ProjectBuilderConfiguration;
import org.apache.maven.repository.RepositorySystem;
import org.codehaus.plexus.ContainerConfiguration;
import org.codehaus.plexus.PlexusTestCase;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.logging.console.ConsoleLogger;
import org.codehaus.plexus.util.FileUtils;

public abstract class AbstractCoreMavenComponentTest
    extends PlexusTestCase
{
    @Requirement
    protected RepositorySystem repositorySystem;

    @Requirement
    protected MavenProjectBuilder projectBuilder;
    
    protected void setUp()
        throws Exception
    {
        super.setUp();
        repositorySystem = lookup( RepositorySystem.class );
        projectBuilder = lookup( MavenProjectBuilder.class );        
    }

    abstract protected String getProjectsDirectory();
    
    protected File getProject( String name )
        throws Exception
    {
        File source = new File( new File( getBasedir(), getProjectsDirectory() ), name );
        File target = new File( new File ( getBasedir(), "target" ), name );
        if ( !target.exists() )
        {
            FileUtils.copyDirectoryStructure( source, target );
        }
        return new File( target, "pom.xml" );
    }   
    
    /**
     * We need to customize the standard Plexus container with the plugin discovery listener which
     * is what looks for the META-INF/maven/plugin.xml resources that enter the system when a
     * Maven plugin is loaded.
     * 
     * We also need to customize the Plexus container with a standard plugin discovery listener
     * which is the MavenPluginCollector. When a Maven plugin is discovered the MavenPluginCollector
     * collects the plugin descriptors which are found. 
     */
    protected void customizeContainerConfiguration( ContainerConfiguration containerConfiguration )
    {
        containerConfiguration.addComponentDiscoverer( new MavenPluginDiscoverer() );
        containerConfiguration.addComponentDiscoveryListener( new MavenPluginCollector() );
    }
    
    // - remove the event monitor, just default or get rid of it
    // layer the creation of a project builder configuration with a request, but this will need to be
    // a Maven subclass because we don't want to couple maven to the project builder which we need to
    // separate.
    protected MavenSession createMavenSession( File pom )
        throws Exception
    {
        ArtifactRepository localRepository = repositorySystem.createDefaultLocalRepository();
        ArtifactRepository remoteRepository = repositorySystem.createDefaultRemoteRepository();
        
        MavenExecutionRequest request = new DefaultMavenExecutionRequest()
            .setProjectPresent( true )
            .setPluginGroups( Arrays.asList( new String[] { "org.apache.maven.plugins" } ) )
            .setLocalRepository( localRepository )
            .setRemoteRepositories( Arrays.asList( remoteRepository ) )
            .setGoals( Arrays.asList( new String[] { "package" } ) )   
            // This is wrong
            .addEventMonitor( new DefaultEventMonitor( new ConsoleLogger( 0, "" ) ) )
            .setProperties( new Properties() );

        ProjectBuilderConfiguration configuration = new DefaultProjectBuilderConfiguration()
            .setLocalRepository( localRepository )
            .setRemoteRepositories( Arrays.asList( remoteRepository ) );

        MavenProject project = projectBuilder.build( pom, configuration );        
                        
        MavenSession session = new MavenSession( getContainer(), request, project );
        
        return session;
    }
}
