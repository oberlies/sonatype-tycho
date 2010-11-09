package org.sonatype.tycho.p2.tools.impl;

import java.net.URI;

import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.core.IProvisioningAgentProvider;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

public class Activator
    implements BundleActivator
{
    private static BundleContext context;

    public void start( BundleContext context )
        throws Exception
    {
        this.context = context;
    }

    public void stop( BundleContext context )
        throws Exception
    {
    }

    public static IProvisioningAgent createProvisioningAgent( final URI location )
        throws ProvisionException
    {
        ServiceReference serviceReference = context.getServiceReference( IProvisioningAgentProvider.SERVICE_NAME );
        IProvisioningAgentProvider agentFactory = (IProvisioningAgentProvider) context.getService( serviceReference );
        try
        {
            return agentFactory.createAgent( location );
        }
        finally
        {
            context.ungetService( serviceReference );
        }
    }
}
