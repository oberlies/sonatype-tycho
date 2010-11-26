package org.sonatype.tycho.p2.facade;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.tycho.TargetPlatform;
import org.codehaus.tycho.TychoConstants;
import org.sonatype.tycho.ArtifactKey;
import org.sonatype.tycho.equinox.EquinoxServiceFactory;
import org.sonatype.tycho.p2.MetadataSerializable;
import org.sonatype.tycho.p2.repository.GAV;
import org.sonatype.tycho.p2.repository.ModuleArtifactRepositoryDescriptor;
import org.sonatype.tycho.p2.repository.RepositoryLayoutHelper;
import org.sonatype.tycho.p2.tools.RepositoryReferences;

/**
 * Tool to obtain the list of p2 repositories that contain the dependencies of a module.
 */
@Component( role = RepositoryReferenceTool.class )
public class RepositoryReferenceTool
{
    /**
     * Option to indicate that the publisher results of the given module shall be included in the
     * list of repositories.
     */
    public static int REPOSITORIES_INCLUDE_SELF_MODULE = 1;

    public static String PUBLISHER_REPOSITORY_PATH = "publisherRepository";

    @Requirement
    private EquinoxServiceFactory osgiServices;

    // TODO delete this method
    public void write( OutputStream out, TargetPlatform platform, String qualifier )
        throws IOException
    {
        MetadataSerializable serializer = osgiServices.getService( MetadataSerializable.class );
        serializer.serialize( out, platform.getNonReactorUnits() );
    }

    /**
     * Returns the list of visible p2 repositories for the build of the given module. The list
     * includes the p2 repositories of the referenced reactor modules, the target platform, and
     * optionally the module itself. The repositories are sorted in a reasonable order of
     * precedence, so if there should be duplicate installable units or artifacts, the hope is that
     * it is deterministic from which repository the unit or artifact is taken. The order is:
     * <ol>
     * <li>The publisher results of the given module (only if the flag
     * {@link #REPOSITORIES_INCLUDE_SELF_MODULE} is set),
     * <li>The results of the referenced reactor modules,
     * <li>The non-reactor content of the module's target platform.
     * </ol>
     * 
     * @param module The module for which the visible repositories shall be returned
     * @param session The current maven session
     * @param flags Options flags; supported flags are {@link #REPOSITORIES_INCLUDE_SELF_MODULE}
     * @return a {@link RepositoryReferences} instance with the repositories.
     * @throws MojoExecutionException in case of internal errors
     * @throws MojoFailureException in case required artifacts are missing
     */
    public RepositoryReferences getVisibleRepositories( MavenProject module, MavenSession session, int flags )
        throws MojoExecutionException, MojoFailureException
    {
        RepositoryReferences repositories = new RepositoryReferences();

        if ( ( flags & REPOSITORIES_INCLUDE_SELF_MODULE ) != 0 )
        {
            File publisherResults = new File( module.getBuild().getDirectory(), PUBLISHER_REPOSITORY_PATH );
            repositories.addMetadataRepository( publisherResults );
            repositories.addArtifactRepository( publisherResults );
        }

        addRepositoriesOfReferencedModules( repositories, module );

        // metadata and artifacts of target platform
        File targetPlatform = materializeTargetPlatformRepository( module );
        repositories.addMetadataRepository( targetPlatform );
        repositories.addArtifactRepository( new File( session.getLocalRepository().getBasedir() ) );
        return repositories;
    }

    private static void addRepositoriesOfReferencedModules( RepositoryReferences sources, MavenProject currentProject )
        throws MojoExecutionException, MojoFailureException
    {
        for ( MavenProject referencedProject : currentProject.getProjectReferences().values() )
        {
            String packaging = referencedProject.getPackaging();
            if ( ArtifactKey.TYPE_ECLIPSE_PLUGIN.equals( packaging )
                || ArtifactKey.TYPE_ECLIPSE_FEATURE.equals( packaging ) )
            {
                File p2ContentXML =
                    getProjectArtifact( referencedProject, RepositoryLayoutHelper.CLASSIFIER_P2_METADATA );
                if ( p2ContentXML == null )
                    throw new MojoFailureException( "Missing required artifact '"
                        + RepositoryLayoutHelper.CLASSIFIER_P2_METADATA + "' in module " + referencedProject.getId() );

                sources.addMetadataRepository( p2ContentXML.getParentFile() );
                sources.addArtifactRepository( getModuleArtifactRepositoryDescriptor( referencedProject ) );
            }
        }
    }

    private static ModuleArtifactRepositoryDescriptor getModuleArtifactRepositoryDescriptor( MavenProject project )
    {
        File repositoryLocation =
            getProjectArtifact( project, RepositoryLayoutHelper.CLASSIFIER_P2_ARTIFACTS ).getParentFile();
        Map<String, File> projectArtifacts = getAllProjectArtifacts( project );
        return new ModuleArtifactRepositoryDescriptor( repositoryLocation, getGAV( project ), projectArtifacts );
    }

    private static GAV getGAV( MavenProject project )
    {
        return new GAV( project.getGroupId(), project.getArtifactId(), project.getVersion() );
    }

    private static File getProjectArtifact( MavenProject project, String classifier )
    {
        if ( classifier == null )
        {
            return project.getArtifact().getFile();
        }
        for ( Artifact artifact : project.getAttachedArtifacts() )
        {
            if ( classifier.equals( artifact.getClassifier() ) )
            {
                return artifact.getFile();
            }
        }
        return null;
    }

    /**
     * Returns a map from classifiers to artifact files of the given project. The classifier
     * <code>null</code> is mapped to the project's main artifact.
     */
    private static Map<String, File> getAllProjectArtifacts( MavenProject project )
    {
        Map<String, File> artifacts = new HashMap<String, File>();
        artifacts.put( null, project.getArtifact().getFile() );
        for ( Artifact attachedArtifact : project.getAttachedArtifacts() )
        {
            artifacts.put( attachedArtifact.getClassifier(), attachedArtifact.getFile() );
        }
        return artifacts;
    }

    /**
     * Restores the p2 metadata view on the module's build target platform (without reactor
     * projects) that was calculated during the initial dependency resolution (see
     * org.sonatype.tycho.p2.resolver.P2ResolverImpl.toResolutionResult(...)).
     */
    private File materializeTargetPlatformRepository( MavenProject module )
        throws MojoExecutionException, MojoFailureException
    {
        try
        {
            File repositoryLocation = new File( module.getBuild().getDirectory(), "targetPlatformRepository" );
            repositoryLocation.mkdirs();
            FileOutputStream stream = new FileOutputStream( new File( repositoryLocation, "content.xml" ) );
            try
            {
                MetadataSerializable serializer = osgiServices.getService( MetadataSerializable.class );
                Set<?> targetPlatformInstallableUnits = getTargetPlatform( module ).getNonReactorUnits();
                serializer.serialize( stream, targetPlatformInstallableUnits );
            }
            finally
            {
                stream.close();
            }
            return repositoryLocation;
        }
        catch ( IOException e )
        {
            throw new MojoExecutionException( "I/O exception while writing the build target platform to disk", e );
        }
    }

    private static TargetPlatform getTargetPlatform( MavenProject module )
        throws MojoFailureException
    {
        TargetPlatform targetPlatform = (TargetPlatform) module.getContextValue( TychoConstants.CTX_TARGET_PLATFORM );
        if ( targetPlatform == null )
            throw new MojoFailureException( "Tycho target platform missing for project " + module.toString() );
        return targetPlatform;
    }

}
