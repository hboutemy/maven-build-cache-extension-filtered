package org.apache.maven.project;

import java.util.Date;
import java.util.List;
import java.util.Properties;

import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.model.ModelBuildingRequest;
import org.apache.maven.model.Profile;

public interface ProjectBuildingRequest
{
    ProjectBuildingRequest setLocalRepository( ArtifactRepository localRepository );
    
    ArtifactRepository getLocalRepository();

    ProjectBuildingRequest setRemoteRepositories( List<ArtifactRepository> remoteRepositories );

    List<ArtifactRepository> getRemoteRepositories();

    ProjectBuildingRequest setPluginArtifactRepositories( List<ArtifactRepository> pluginArtifacgRepositories );

    List<ArtifactRepository> getPluginArtifactRepositories();

    ProjectBuildingRequest setExecutionProperties( Properties executionProperties );

    Properties getExecutionProperties();

    void setTopLevelProjectForReactor(MavenProject mavenProject);

    MavenProject getTopLevelProjectFromReactor();
        
    ProjectBuildingRequest setProcessPlugins( boolean processPlugins );
    
    boolean isProcessPlugins();

    /**
     * Controls the level of validation to perform on processed models. By default, models are validated in strict mode.
     * 
     * @param validationLevel The level of validation to perform on processed models, e.g.
     *            {@link ModelBuildingRequest#VALIDATION_LEVEL_STRICT}.
     * @return This configuration, never {@code null}.
     */
    ProjectBuildingRequest setValidationLevel( int validationLevel );

    /**
     * Gets the level of validation to perform on processed models.
     * 
     * @return The level of validation to perform on processed models.
     */
    int getValidationLevel();

    // Profiles
    
    /**
     * Set any active profiles that the {@link ProjectBuilder} should consider while constructing
     * a {@link MavenProject}.
     */
    void setActiveProfileIds( List<String> activeProfileIds );
        
    List<String> getActiveProfileIds();

    void setInactiveProfileIds( List<String> inactiveProfileIds );

    List<String> getInactiveProfileIds();
    
    /**
     * Add a {@link org.apache.maven.model.Profile} that has come from an external source. This may be from a custom configuration
     * like the MavenCLI settings.xml file, or from a custom dialog in an IDE integration like M2Eclipse.
     * @param profile
     */
    void addProfile( Profile profile );
    
    void setProfiles( List<Profile> profiles );
    
    List<Profile> getProfiles();

    /**
     * Gets the start time of the build.
     * 
     * @return The start time of the build or {@code null} if unknown.
     */
    Date getBuildStartTime();

    /**
     * Sets the start time of the build.
     * 
     * @param buildStartTime The start time of the build, may be {@code null}.
     * @return This request, never {@code null}.
     */
    void setBuildStartTime( Date buildStartTime );

}
