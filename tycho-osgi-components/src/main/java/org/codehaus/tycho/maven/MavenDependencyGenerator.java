package org.codehaus.tycho.maven;

import java.io.File;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.model.Dependency;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.logging.Logger;
import org.codehaus.tycho.TargetPlatform;
import org.sonatype.tycho.ArtifactDescriptor;
import org.sonatype.tycho.ArtifactKey;
import org.sonatype.tycho.ReactorProject;

public final class MavenDependencyGenerator
{
    /**
     * Injects the dependencies of a project (as determined by the p2 target platform resolver) back
     * into the Maven model.
     * 
     * @param project A project
     * @param target The p2-resolved target platform of the project.
     * @param logger A logger
     */
    public static void injectMavenDependencies( MavenProject project, TargetPlatform target, Logger logger )
    {
        MavenDependencyGenerator generator = new MavenDependencyGenerator( project, logger );
        for ( ArtifactDescriptor artifact : target.getArtifacts() )
        {
            generator.addDependency( artifact );
        }
    }

    private final MavenProject project;

    private final Logger logger;

    private MavenDependencyGenerator( MavenProject project, Logger logger )
    {
        this.project = project;
        this.logger = logger;
    }

    private void addDependency( ArtifactDescriptor artifact )
    {
        Dependency dependency = null;
        if ( artifact.getMavenProject() != null )
        {
            if ( !artifact.getMavenProject().sameProject( project ) )
            {
                dependency = newProjectDependency( artifact.getMavenProject() );
            }
        }
        else
        {
            ArtifactKey key = artifact.getKey();
            dependency = newExternalDependency( artifact.getLocation(), key.getType(), key.getId(), key.getVersion() );
        }
        // can be null for directory-based features/bundles
        if ( dependency != null )
        {
            project.getModel().addDependency( dependency );
        }
    }

    private Dependency newExternalDependency( File location, String p2Classifier, String artifactId, String version )
    {
        if ( !location.exists() || !location.isFile() || !location.canRead() )
        {
            logger.warn( "Dependency at location " + location
                + " can not be represented in Maven model and will not be visible to non-OSGi aware Maven plugins" );
            return null;
        }

        Dependency dependency = new Dependency();
        dependency.setArtifactId( artifactId );
        dependency.setGroupId( "p2." + p2Classifier ); // See also RepositoryLayoutHelper#getP2Gav
        dependency.setVersion( version );
        dependency.setScope( Artifact.SCOPE_SYSTEM );
        dependency.setSystemPath( location.getAbsolutePath() );
        return dependency;
    }

    private Dependency newProjectDependency( ReactorProject dependentMavenProjectProxy )
    {
        if ( dependentMavenProjectProxy == null )
        {
            return null;
        }

        Dependency dependency = new Dependency();
        dependency.setArtifactId( dependentMavenProjectProxy.getArtifactId() );
        dependency.setGroupId( dependentMavenProjectProxy.getGroupId() );
        dependency.setVersion( dependentMavenProjectProxy.getVersion() );
        dependency.setType( dependentMavenProjectProxy.getPackaging() );
        dependency.setScope( Artifact.SCOPE_PROVIDED );
        return dependency;
    }
}
