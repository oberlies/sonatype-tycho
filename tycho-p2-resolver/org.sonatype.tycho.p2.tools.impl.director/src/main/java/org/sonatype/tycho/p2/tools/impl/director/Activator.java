package org.sonatype.tycho.p2.tools.impl.director;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.sonatype.tycho.p2.tools.director.DirectorApplicationWrapper;

public class Activator
    implements BundleActivator
{
    public void start( BundleContext context )
        throws Exception
    {
        context.registerService( DirectorApplicationWrapper.class.getName(), new DirectorApplicationWrapperImpl(), null );
    }

    public void stop( BundleContext context )
        throws Exception
    {
    }
}
