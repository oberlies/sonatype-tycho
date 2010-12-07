package org.sonatype.tycho.p2.maven.repository;

import java.io.File;
import java.net.URI;
import java.util.Map;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.equinox.p2.repository.artifact.IArtifactRepository;
import org.eclipse.equinox.p2.repository.artifact.spi.ArtifactRepositoryFactory;

public class ModuleArtifactRepositoryFactory
    extends ArtifactRepositoryFactory
{
    private static final String REPOSITORY_TYPE = ModuleArtifactRepository.class.getSimpleName();

    @Override
    public IArtifactRepository create( URI location, String name, String type, Map<String, String> properties )
        throws ProvisionException
    {
        throw RepositoryFactoryTools.unsupportedCreation( REPOSITORY_TYPE );
    }

    @Override
    public IArtifactRepository load( URI location, int flags, IProgressMonitor monitor )
        throws ProvisionException
    {
        File repositoryDir = RepositoryFactoryTools.asFile( location );
        if ( repositoryDir != null )
        {
            return load( repositoryDir, flags );
        }
        return null;
    }

    private IArtifactRepository load( File repositoryDir, int flags )
        throws ProvisionException
    {
        if ( ModuleArtifactRepository.canAttemptRead( repositoryDir ) )
        {
            RepositoryFactoryTools.verifyModifiableNotRequested( flags, REPOSITORY_TYPE );
            return new ModuleArtifactRepository( getAgent(), repositoryDir );
        }
        return null;
    }
}
