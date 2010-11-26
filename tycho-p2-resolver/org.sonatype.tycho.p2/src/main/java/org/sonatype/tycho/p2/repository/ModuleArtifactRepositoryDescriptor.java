package org.sonatype.tycho.p2.repository;

import java.io.File;
import java.util.Map;

/**
 * Stores information on the layout of artifact files in a module's build directory. Together with
 * the information from the <code>p2artifacts.xml</code> file, this allows to read a build directory
 * as p2 artifact repository:
 * <ul>
 * <li>The <code>p2artifacts.xml</code> file contains a list of all artifacts with p2 <i>and</i>
 * Maven coordinates. (In particular the classifier part of the Maven coordinates is relevant.)</li>
 * <li>This instance maps classifiers back to the locations of the artifacts in the build directory,
 * i.e. to the file locations <i>before</i> the artifacts are uploaded into a Maven repository.</li>
 * </ul>
 */
public class ModuleArtifactRepositoryDescriptor
{
    private final File repositoryLocation;

    private final GAV moduleGAV;

    private final Map<String, File> moduleArtifacts;

    /**
     * Creates a new {@link ModuleArtifactRepositoryDescriptor} instance describing the artifacts in
     * a module's build directory.
     * 
     * @param repositoryLocation The logical location of the repository; this is typically the root
     *            of the module's build directory
     * @param moduleGAV The Maven coordinates of the module
     * @param moduleArtifacts a map from artifact classifiers (or <code>null</code> for the main
     *            artifact) to locations of that artifact in the build target directory
     */
    public ModuleArtifactRepositoryDescriptor( File repositoryLocation, GAV moduleGAV, Map<String, File> moduleArtifacts )
    {
        this.repositoryLocation = repositoryLocation;
        this.moduleGAV = moduleGAV;
        this.moduleArtifacts = moduleArtifacts;
    }

    public File getRepositoryLocation()
    {
        return repositoryLocation;
    }

    public GAV getModuleGAV()
    {
        return moduleGAV;
    }

    public Map<String, File> getModuleArtifacts()
    {
        return moduleArtifacts;
    }
}