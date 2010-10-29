package org.sonatype.tycho.plugins.p2.publisher;

import java.util.Arrays;

import org.codehaus.tycho.TargetEnvironment;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

public class FacadeToolsTest
{
    @Test
    public void testGetConfigurations()
    {
        String[] configsParameter =
            FacadeTools.getConfigurations( Arrays.asList( new TargetEnvironment( "os", "ws", "arch", "nl" ),
                                                          new TargetEnvironment( "os2", "ws3", "arch0", "nl2" ) ) );
        Assert.assertEquals( "ws.os.arch", configsParameter[0] );
        Assert.assertEquals( "ws3.os2.arch0", configsParameter[1] );
    }

    @Test
    @Ignore
    public void testGetConfigurationsForNoTargetEnv()
    {
        // TODO test solution to be found in TYCHO-529
    }
}
