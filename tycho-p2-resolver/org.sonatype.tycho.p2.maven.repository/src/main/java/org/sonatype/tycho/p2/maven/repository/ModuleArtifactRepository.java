package org.sonatype.tycho.p2.maven.repository;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.util.Collections;
import java.util.Map;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.equinox.internal.p2.core.helpers.FileUtils;
import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.equinox.p2.repository.artifact.IArtifactDescriptor;
import org.sonatype.tycho.p2.repository.GAV;
import org.sonatype.tycho.p2.repository.ModuleArtifactRepositoryDescriptor;
import org.sonatype.tycho.p2.repository.RepositoryLayoutHelper;
import org.sonatype.tycho.p2.repository.RepositoryReader;
import org.sonatype.tycho.p2.repository.TychoRepositoryIndex;

/**
 * Exposes a module's build target directory as p2 artifact repository. The data sources are the
 * artifact files in the build directory, with the index coming from artifact with classifier
 * "p2artifacts". The artifact files files are found with the help of the
 * {@link ModuleArtifactRepositoryDescriptor} instance for the module.
 * 
 * @see RepositoryLayoutHelper#CLASSIFIER_P2_ARTIFACTS
 */
public class ModuleArtifactRepository
    extends AbstractMavenArtifactRepository
{
    public ModuleArtifactRepository( IProvisioningAgent agent, URI location,
                                     ModuleArtifactRepositoryDescriptor descriptor )
    {
        super( agent, location, createSingletonIndex( descriptor.getModuleGAV() ),
               new ClassifierMapReader( descriptor.getModuleGAV(), descriptor.getModuleArtifacts() ) );
    }

    private static TychoRepositoryIndex createSingletonIndex( final GAV moduleGAV )
    {
        return new MemoryTychoRepositoryIndex( Collections.singletonList( moduleGAV ) );
    }

    private static class ClassifierMapReader
        implements RepositoryReader
    {
        private final GAV moduleGAV;

        private final Map<String, File> artifacts;

        ClassifierMapReader( GAV moduleGAV, Map<String, File> artifacts )
        {
            this.moduleGAV = moduleGAV;
            this.artifacts = artifacts;
        }

        public InputStream getContents( String remoteRelpath )
            throws IOException
        {
            // can only return artifacts with GAV
            throw new UnsupportedOperationException();
        }

        public InputStream getContents( GAV gav, String classifier, String extension )
            throws IOException
        {
            if ( !moduleGAV.equals( gav ) )
                throw new IllegalStateException( "Artifact repository of module " + moduleGAV
                    + " cannot contain artifact " + gav + ":" + classifier );

            File artifactFile = artifacts.get( classifier );
            if ( artifactFile == null )
                throw new IllegalStateException( "Classifier " + classifier + " is missing in descriptor of module "
                    + moduleGAV );
            return new FileInputStream( artifactFile );
        }

    }// end nested class

    @Override
    public IStatus getArtifact( IArtifactDescriptor descriptor, OutputStream destination, IProgressMonitor monitor )
    {
        return getRawArtifact( descriptor, destination, monitor );
    }

    @SuppressWarnings( "restriction" )
    public IStatus getRawArtifact( IArtifactDescriptor descriptor, OutputStream destination, IProgressMonitor monitor )
    {
        GAV gav = RepositoryLayoutHelper.getGAV( descriptor.getProperties() );
        String classifier = descriptor.getProperty( RepositoryLayoutHelper.PROP_CLASSIFIER );
        if ( gav == null )
        {
            return new Status( IStatus.ERROR, Activator.ID, "Maven coordinates in artifact "
                + descriptor.getArtifactKey().toExternalForm() + " are missing" );
        }

        try
        {
            String extension = null; // not needed
            InputStream source = getContentLocator().getContents( gav, classifier, extension );

            // copy to destination and close source 
            FileUtils.copyStream( source, true, destination, false );
        }
        catch ( IOException e )
        {
            return new Status( IStatus.ERROR, Activator.ID, "I/O exception while reading artifact "
                + descriptor.getArtifactKey().toExternalForm(), e );
        }

        return Status.OK_STATUS;
    }

    @Override
    public OutputStream getOutputStream( IArtifactDescriptor descriptor )
        throws ProvisionException
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public IStatus resolve( IArtifactDescriptor descriptor )
    {
        // nothing to do (?)
        return Status.OK_STATUS;
    }

}
