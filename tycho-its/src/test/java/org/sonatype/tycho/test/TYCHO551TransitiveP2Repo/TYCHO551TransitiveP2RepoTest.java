package org.sonatype.tycho.test.TYCHO551TransitiveP2Repo;

import java.io.File;
import java.io.IOException;

import org.apache.maven.it.Verifier;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.sonatype.tycho.test.AbstractTychoIntegrationTest;

public class TYCHO551TransitiveP2RepoTest
    extends AbstractTychoIntegrationTest
{

    private static final String MODULE_NON_TRANSITIVE = "repository";

    private static final String MODULE_TRANSITIVE = "repository-transitive";

    private static Verifier verifier;

    @BeforeClass
    public static void buildFeatureAndBundlesAndRepos()
        throws Exception
    {
        verifier = new TYCHO551TransitiveP2RepoTest().getVerifier( "/TYCHO551TransitiveP2Repo", false );
        verifier.getCliOptions().add( "-Dp2.repo="
                                          + new File( "repositories/e352" ).getCanonicalFile().toURI().normalize().toString() );
        /*
         * Do not execute "install" to ensure that features and bundles can be included directly from the build results
         * of the local reactor.
         */
        verifier.executeGoal( "package" );
        verifier.verifyErrorFreeLog();
    }

    @Test
    public void testEclipseRepositoryTransitive()
        throws IOException
    {
        File pluginsDir = new File( verifier.getBasedir(), MODULE_TRANSITIVE + "/target/repository/plugins" );
        Assert.assertTrue( checkFileWithPrefixExists( pluginsDir, "tycho551.bundle2_" ) );
        Assert.assertTrue( checkFileWithPrefixExists( pluginsDir, "org.junit_" ) );
    }

    @Test
    public void testEclipseRepositoryNonTransitive()
        throws IOException
    {
        File pluginsDir = new File( verifier.getBasedir(), MODULE_NON_TRANSITIVE + "/target/repository/plugins" );
        Assert.assertFalse( checkFileWithPrefixExists( pluginsDir, "tycho551.bundle2_" ) );
        Assert.assertFalse( checkFileWithPrefixExists( pluginsDir, "org.junit_" ) );
    }

    private boolean checkFileWithPrefixExists( File dir, String prefix )
    {
        File[] files = dir.listFiles();
        if ( files == null )
        {
            return false;
        }
        for ( File file : files )
        {
            if ( file.isFile() && file.getName().startsWith( prefix ) )
            {
                return true;
            }
        }
        return false;
    }
}
