package org.sonatype.tycho.p2.maven.repository;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.equinox.p2.repository.IRepositoryManager;

class RepositoryFactoryTools
{
    static void verifyModifiableNotRequested( int flags, String repositoryType )
        throws ProvisionException
    {
        if ( ( flags & IRepositoryManager.REPOSITORY_HINT_MODIFIABLE ) != 0 )
        {
            Status errorStatus =
                new Status( IStatus.ERROR, Activator.ID, ProvisionException.REPOSITORY_READ_ONLY,
                            "Cannot create writable repositories of type " + repositoryType, null );
            throw new ProvisionException( errorStatus );
        }
    }

    static ProvisionException unsupportedCreation( String repositoryType )
    {
        Status errorStatus =
            new Status( IStatus.ERROR, Activator.ID, 0 /* none of the defined codes really fit */,
                        "Cannot create repositories of type " + repositoryType, null );
        return new ProvisionException( errorStatus );
    }
}
