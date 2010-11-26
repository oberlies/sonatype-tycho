package org.sonatype.tycho.p2.maven.repository;

import java.net.URI;
import java.util.Map;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.equinox.p2.repository.artifact.IArtifactRepository;
import org.eclipse.equinox.p2.repository.artifact.spi.ArtifactRepositoryFactory;
import org.sonatype.tycho.p2.repository.ModuleArtifactRepositoryDescriptor;
import org.sonatype.tycho.p2.repository.ModuleArtifactRepositoryMap;

public class ModuleArtifactRepositoryFactory
    extends ArtifactRepositoryFactory
{

    private static final String REPOSITORY_TYPE = "ModuleArtifactRepository";

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
        ModuleArtifactRepositoryMap repositoryMap =
            (ModuleArtifactRepositoryMap) getAgent().getService( ModuleArtifactRepositoryMap.SERVICE_NAME );
        if ( repositoryMap == null )
        {
            // don't load module artifact repositories for current IProvisionAgent 
            return null;
        }

        ModuleArtifactRepositoryDescriptor repository = repositoryMap.getRepositoryDescriptor( location );
        if ( repository == null )
        {
            // not a module artifact repository
            return null;
        }
        else
        {
            RepositoryFactoryTools.verifyModifiableNotRequested( flags, REPOSITORY_TYPE );
            return new ModuleArtifactRepository( getAgent(), location, repository );
        }
    }
}
