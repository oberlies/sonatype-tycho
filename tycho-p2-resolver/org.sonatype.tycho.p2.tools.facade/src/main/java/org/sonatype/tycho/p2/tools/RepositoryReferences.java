package org.sonatype.tycho.p2.tools;

import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.sonatype.tycho.p2.repository.ModuleArtifactRepositoryDescriptor;

/**
 * List of p2 repositories for a p2 operation. Instances of this class store a list of metadata and
 * artifact repositories each, preserving the order in which the repositories were added.
 */
public class RepositoryReferences
{
    final List<URI> metadataRepos = new ArrayList<URI>();

    List<URI> artifactRepos;

    /**
     * Additional information needed to load module artifact repositories. Since module artifact
     * repositories are artifact repositories, all entries in this list are also contained in the
     * member {@link #artifactRepos}.
     */
    Map<URI, ModuleArtifactRepositoryDescriptor> moduleRepos;

    /**
     * Adds the metadata repository at the given location.
     * 
     * @param metadataRepositoryLocation The folder containing the metadata repository file (
     *            <code>content.xml</code> or <code>content.jar</code>)
     */
    public void addMetadataRepository( File metadataRepositoryLocation )
    {
        metadataRepos.add( metadataRepositoryLocation.toURI() );
    }

    /**
     * Adds the artifact repository at the given location.
     * 
     * @param artifactRepositoryLocation The folder containing the artifact repository file
     *            structure
     */
    public void addArtifactRepository( File artifactRepositoryLocation )
    {
        if ( artifactRepos == null )
            artifactRepos = new ArrayList<URI>();
        artifactRepos.add( artifactRepositoryLocation.toURI() );
    }

    /**
     * Adds an artifact repository with Maven module layout.
     * 
     * @param moduleArtifactRepository The descriptor of the module artifact repository
     */
    public void addArtifactRepository( ModuleArtifactRepositoryDescriptor moduleArtifactRepository )
    {
        if ( moduleRepos == null )
            moduleRepos = new HashMap<URI, ModuleArtifactRepositoryDescriptor>();
        moduleRepos.put( moduleArtifactRepository.getRepositoryLocation().toURI(), moduleArtifactRepository );

        addArtifactRepository( moduleArtifactRepository.getRepositoryLocation() );
    }

    /**
     * Returns the list of metadata repositories in the order that they were added.
     * 
     * @return the list metadata repositories.
     */
    public List<URI> getMetadataRepositories()
    {
        return Collections.unmodifiableList( metadataRepos );
    }

    /**
     * Returns the list of artifact repositories in the order that they were added. This includes
     * both normal repositories as well as module artifact repositories.
     * 
     * @return the list of artifact repositories.
     */
    public List<URI> getArtifactRepositories()
    {
        if ( artifactRepos == null )
            return Collections.emptyList();
        return Collections.unmodifiableList( artifactRepos );
    }

    /**
     * Returns the descriptors of the module artifact repositories, indexed by the location of these
     * repositories.
     * 
     * @return a map from module artifact repository locations to their descriptor
     */
    public Map<URI, ModuleArtifactRepositoryDescriptor> getModuleRepositoriesMap()
    {
        if ( moduleRepos == null )
            return Collections.emptyMap();
        return Collections.unmodifiableMap( moduleRepos );
    }
}
