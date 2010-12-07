package org.sonatype.tycho.p2.repository;

import java.util.Map;
import java.util.StringTokenizer;

public class RepositoryLayoutHelper
{
    public static final String PROP_GROUP_ID = "maven-groupId";

    public static final String PROP_ARTIFACT_ID = "maven-artifactId";

    public static final String PROP_VERSION = "maven-version";

    public static final String PROP_CLASSIFIER = "maven-classifier";

    public static final String CLASSIFIER_P2_METADATA = "p2metadata";

    public static final String EXTENSION_P2_METADATA = "xml";

    /**
     * Name of the file where the module p2 metadata is stored in the target directory. The name
     * needs to be known so that the target folder can be read as p2 metadata repository.
     */
    public static final String FILE_NAME_P2_METADATA = "p2content.xml";

    public static final String CLASSIFIER_P2_ARTIFACTS = "p2artifacts";

    public static final String EXTENSION_P2_ARTIFACTS = "xml";

    /**
     * Name of the file that contains the p2 artifact index. This file is one of the files needed to
     * read the target folder as p2 artifact repository. The location is relative to the build
     * target directory root.
     */
    public static final String FILE_NAME_P2_ARTIFACTS = "p2artifacts.xml";

    /**
     * Name of the file that stores the location of the Maven artifact in the target folder. This
     * file is one of the files needed to read the target folder as p2 artifact repository.
     */
    public static final String FILE_NAME_LOCAL_ARTIFACTS = "local-artifacts.properties";

    /**
     * Key for the main artifact location in {@value #FILE_NAME_LOCAL_ARTIFACTS} files.
     */
    public static final String KEY_ARTIFACT_MAIN = "artifact.main";

    /**
     * Key prefix for attached artifact locations in {@value #FILE_NAME_LOCAL_ARTIFACTS} files.
     */
    public static final String KEY_ARTIFACT_ATTACHED = "artifact.attached.";

    public static final String DEFAULT_EXTERNSION = "jar";

    public static String getRelativePath( GAV gav, String classifier, String extension )
    {
        return getRelativePath( gav.getGroupId(), gav.getArtifactId(), gav.getVersion(), classifier, extension );
    }

    public static String getRelativePath( String groupId, String artifactId, String version, String classifier,
                                          String extension )
    {
        StringBuilder sb = new StringBuilder();

        // basedir
        StringTokenizer st = new StringTokenizer( groupId, "." );
        while ( st.hasMoreTokens() )
        {
            sb.append( st.nextToken() ).append( '/' );
        }
        sb.append( artifactId ).append( '/' ).append( version ).append( '/' );

        // filename
        sb.append( artifactId ).append( '-' ).append( version );
        if ( classifier != null )
        {
            sb.append( '-' ).append( classifier );
        }
        sb.append( '.' ).append( extension != null ? extension : DEFAULT_EXTERNSION );

        return sb.toString();
    }

    public static GAV getGAV( Map properties )
    {
        String groupId = (String) properties.get( PROP_GROUP_ID );
        String artifactId = (String) properties.get( PROP_ARTIFACT_ID );
        String version = (String) properties.get( PROP_VERSION );

        return getGAV( groupId, artifactId, version );
    }

    public static GAV getGAV( String groupId, String artifactId, String version )
    {
        if ( groupId != null && artifactId != null && version != null )
        {
            return new GAV( groupId, artifactId, version );
        }

        return null;
    }

    public static GAV getP2Gav( String classifier, String id, String version )
    {
        // Should match AbstractDependenciesReader#newExternalDependency
        return new GAV( "p2." + classifier, id, version );
    }
}
