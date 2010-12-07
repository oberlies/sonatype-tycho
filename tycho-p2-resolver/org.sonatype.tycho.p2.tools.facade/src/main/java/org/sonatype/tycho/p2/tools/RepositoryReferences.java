package org.sonatype.tycho.p2.tools;

import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * List of p2 repositories for a p2 operation. Instances of this class store a list of metadata and
 * artifact repositories each, preserving the order in which the repositories were added.
 */
public final class RepositoryReferences
{
    private final List<URI> metadataRepos = new ArrayList<URI>();

    private final List<URI> artifactRepos = new ArrayList<URI>();

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
        artifactRepos.add( artifactRepositoryLocation.toURI() );
    }

    /**
     * Returns the list of metadata repositories in the order in which they were added.
     * 
     * @return the list metadata repositories.
     */
    public List<URI> getMetadataRepositories()
    {
        return Collections.unmodifiableList( metadataRepos );
    }

    /**
     * Returns the list of artifact repositories in the order in which they were added.
     * 
     * @return the list of artifact repositories.
     */
    public List<URI> getArtifactRepositories()
    {
        return Collections.unmodifiableList( artifactRepos );
    }
}
