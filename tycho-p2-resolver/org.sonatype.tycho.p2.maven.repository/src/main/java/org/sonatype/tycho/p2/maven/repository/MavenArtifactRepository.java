package org.sonatype.tycho.p2.maven.repository;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.equinox.internal.p2.artifact.repository.ArtifactRequest;
import org.eclipse.equinox.internal.p2.core.helpers.FileUtils;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.equinox.p2.repository.artifact.IArtifactDescriptor;
import org.eclipse.equinox.p2.repository.artifact.IArtifactRequest;
import org.sonatype.tycho.p2.repository.GAV;
import org.sonatype.tycho.p2.repository.RepositoryLayoutHelper;
import org.sonatype.tycho.p2.repository.RepositoryReader;
import org.sonatype.tycho.p2.repository.TychoRepositoryIndex;

@SuppressWarnings( "restriction" )
public class MavenArtifactRepository
    extends AbstractMavenArtifactRepository
{

    public MavenArtifactRepository( URI location, TychoRepositoryIndex projectIndex, RepositoryReader contentLocator )
    {
        super( location, projectIndex, contentLocator );
    }

    @Override
    public IStatus getArtifact( IArtifactDescriptor descriptor, OutputStream destination, IProgressMonitor monitor )
    {
        return downloadArtifact( descriptor, destination );
    }

    public IStatus getRawArtifact( IArtifactDescriptor descriptor, OutputStream destination, IProgressMonitor monitor )
    {
        return downloadArtifact( descriptor, destination );
    }

    private IStatus downloadArtifact( IArtifactDescriptor descriptor, OutputStream destination )
    {
        GAV gav = RepositoryLayoutHelper.getGAV( descriptor.getProperties() );

        if ( gav == null )
        {
            return new Status( IStatus.ERROR, Activator.ID, "Not a Maven artifact" );
        }

        try
        {
            // TODO properly deal with pack200 and binary artifacts
            InputStream contents = new BufferedInputStream( getContentLocator().getContents(
                gav,
                null /* classifier */,
                "jar" /* extension */) );

            try
            {
                if ( destination != null )
                {
                    FileUtils.copyStream( contents, false, destination, false );
                }
            }
            finally
            {
                contents.close();
            }
        }
        catch ( IOException e )
        {
            return new Status( IStatus.ERROR, Activator.ID, "Could not retrieve Maven artifact", e );
        }

        return Status.OK_STATUS;
    }

    @Override
    public OutputStream getOutputStream( IArtifactDescriptor descriptor )
        throws ProvisionException
    {
        // can't really happen for this read-only repository
        throw new UnsupportedOperationException();
    }

    @Override
    public IStatus resolve( IArtifactDescriptor descriptor )
    {
        return downloadArtifact( descriptor, null );
    }

    public String getRelativePath( IArtifactDescriptor descriptor )
    {
        GAV gav = getGAV( descriptor );

        String classifier = null;
        String extension = null;

        if ( "packed".equals( descriptor.getProperty( IArtifactDescriptor.FORMAT ) ) )
        {
            classifier = "pack200";
            extension = "jar.pack.gz";
        }

        return RepositoryLayoutHelper.getRelativePath( gav, classifier, extension );
    }

    public URI getLocation( IArtifactDescriptor descriptor )
    {
        String relativePath = getRelativePath( descriptor );
        String baseURI = getLocation().toString();
        if ( !baseURI.endsWith( "/" ) && !relativePath.startsWith( "/" ) )
        {
            baseURI += "/";
        }
        try
        {
            return new URI( baseURI + relativePath );
        }
        catch ( URISyntaxException e )
        {
            throw new RuntimeException( e );
        }
    }
}
