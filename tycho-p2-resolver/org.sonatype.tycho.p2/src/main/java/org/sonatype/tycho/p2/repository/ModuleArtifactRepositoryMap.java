package org.sonatype.tycho.p2.repository;

import java.net.URI;

/**
 * Provides the additional information needed to load a module artifact repositories. An
 * implementation of this interface can be registered as service to an
 * <code>IProvisioningAgent</code> so that the module artifact repository layout information is
 * available to the global p2 artifact repository factories.
 * 
 * @see ModuleArtifactRepositoryDescriptor
 * @see org.eclipse.equinox.p2.core.IProvisioningAgent
 * @see org.sonatype.tycho.p2.maven.repository.ModuleArtifactRepositoryFactory
 */
public interface ModuleArtifactRepositoryMap
{
    static final String SERVICE_NAME = ModuleArtifactRepositoryMap.class.getName();

    /**
     * Returns the {@link ModuleArtifactRepositoryDescriptor} for the given location.
     * 
     * @param location The location of a module artifact repository, typically the root of the
     *            module's build directory
     * @return the descriptor for the module artifact repository, or <code>null</code> if the
     *         location is not known
     */
    ModuleArtifactRepositoryDescriptor getRepositoryDescriptor( URI location );
}
