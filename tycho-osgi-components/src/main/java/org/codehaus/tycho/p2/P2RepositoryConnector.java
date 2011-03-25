package org.codehaus.tycho.p2;

import java.util.Collection;

import org.sonatype.aether.repository.RemoteRepository;
import org.sonatype.aether.spi.connector.ArtifactDownload;
import org.sonatype.aether.spi.connector.ArtifactUpload;
import org.sonatype.aether.spi.connector.MetadataDownload;
import org.sonatype.aether.spi.connector.MetadataUpload;
import org.sonatype.aether.spi.connector.RepositoryConnector;
import org.sonatype.aether.transfer.ArtifactNotFoundException;
import org.sonatype.aether.transfer.MetadataNotFoundException;

public class P2RepositoryConnector
    implements RepositoryConnector
{

    private final RemoteRepository repository;

    public P2RepositoryConnector( RemoteRepository repository )
    {
        this.repository = repository;
    }

    public void get( Collection<? extends ArtifactDownload> artifactDownloads,
                     Collection<? extends MetadataDownload> metadataDownloads )
    {
        if ( artifactDownloads != null )
        {
            for ( ArtifactDownload a : artifactDownloads )
            {
                a.setException( new ArtifactNotFoundException( a.getArtifact(), repository ) );
            }
        }
        if ( metadataDownloads != null )
        {
            for ( MetadataDownload m : metadataDownloads )
            {
                m.setException( new MetadataNotFoundException( m.getMetadata(), repository ) );
            }
        }
    }

    public void put( Collection<? extends ArtifactUpload> artifactUploads,
                     Collection<? extends MetadataUpload> metadataUploads )
    {
        if ( artifactUploads != null )
        {
            for ( ArtifactUpload a : artifactUploads )
            {
                a.setException( new ArtifactNotFoundException( a.getArtifact(), repository ) );
            }
        }
        if ( metadataUploads != null )
        {
            for ( MetadataUpload m : metadataUploads )
            {
                m.setException( new MetadataNotFoundException( m.getMetadata(), repository ) );
            }
        }
    }

    public void close()
    {
    }

}
