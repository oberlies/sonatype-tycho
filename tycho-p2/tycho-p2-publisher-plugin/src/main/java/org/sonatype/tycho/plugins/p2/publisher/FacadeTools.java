package org.sonatype.tycho.plugins.p2.publisher;

import java.util.List;

import org.codehaus.tycho.TargetEnvironment;

/**
 * Tools for converting Tycho objects into forms suitable for the facades to the OSGi world.
 */
public class FacadeTools
{
    static String[] getConfigurations( final List<TargetEnvironment> envs )
    {
        if ( envs.isEmpty() )
        {
            // TODO this isn't a good idea, it leads to the simpleconfiguration not being used (see TYCHO-529)
            return new String[0];
        }

        final String[] configurations = new String[envs.size()];
        int ix = 0;
        for ( final TargetEnvironment env : envs )
        {
            configurations[ix++] = env.getWs() + "." + env.getOs() + "." + env.getArch();
        }
        return configurations;
    }
}
