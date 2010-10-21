package org.sonatype.tycho.equinox.embedder;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.logging.AbstractLogEnabled;
import org.eclipse.core.runtime.adaptor.EclipseStarter;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.packageadmin.PackageAdmin;

@Component( role = EquinoxEmbedder.class )
public class DefaultEquinoxEmbedder
    extends AbstractLogEnabled
    implements EquinoxEmbedder
{
    @Requirement
    private EquinoxRuntimeLocator equinoxLocator;

    private BundleContext frameworkContext;

    private String[] nonFrameworkArgs;

    private List<Runnable> afterStartCallbacks = new ArrayList<Runnable>();
    
    public synchronized void start()
        throws Exception
    {
        if ( frameworkContext != null )
        {
            return;
        }

        if ( "Eclipse".equals( System.getProperty( "org.osgi.framework.vendor" ) ) )
        {
            throw new IllegalStateException( "Nested Equinox instance is not supported" );
        }

        // https://bugs.eclipse.org/bugs/show_bug.cgi?id=308949
        // restore TCCL to make sure equinox classloader does not leak into our clients
        ClassLoader tccl = Thread.currentThread().getContextClassLoader();
        try
        {
            doStart();
        }
        finally
        {
            Thread.currentThread().setContextClassLoader( tccl );
        }
    }

    protected void doStart()
        throws Exception
    {
        List<File> locations = equinoxLocator.getRuntimeLocations();

        if ( locations == null || locations.isEmpty() || !locations.get( 0 ).isDirectory() )
        {
            throw new RuntimeException( "Equinox runtime location is missing or invalid" );
        }

        File frameworkDir = locations.get( 0 );
        String frameworkLocation = frameworkDir.getAbsolutePath();

        System.setProperty( "osgi.framework.useSystemProperties", "false" ); //$NON-NLS-1$ //$NON-NLS-2$

        Map<String, String> properties = new HashMap<String, String>();
        properties.put( "osgi.install.area", frameworkLocation );
        properties.put( "osgi.syspath", frameworkLocation + "/plugins" );
        properties.put( "osgi.configuration.area", frameworkLocation + "/configuration" );

        StringBuilder bundles = new StringBuilder();
        addBundlesDir( bundles, new File( frameworkDir, "plugins" ).listFiles(), false );
        for ( int i = 1; i < locations.size(); i++ )
        {
            File location = locations.get( i );
            if ( location.isDirectory() )
            {
                addBundlesDir( bundles, location.listFiles(), true );
            }
            else
            {
                bundles.append( ',' ).append( getReferenceUrl( location ) );
            }
        }
        properties.put( "osgi.bundles", bundles.toString() );

        // this tells framework to use our classloader as parent, so it can see classes that we see
        properties.put( "osgi.parentClassloader", "fwk" );

        // this tells framework to check parent classloader first
        // TODO specific package names
        properties.put( "org.osgi.framework.bootdelegation", "*" );
        properties.put( "org.osgi.framework.system.packages.extra", "org.sonatype.tycho.p2.tools.director" );

        // properties.put( "eclipse.p2.data.area", dataArea.getAbsolutePath() );

        // debug
        // properties.put( "osgi.console", "" );
        // properties.put( "osgi.debug", "" );
        // properties.put( "eclipse.consoleLog", "true" );

        // TODO switch to org.eclipse.osgi.launch.Equinox
        // EclipseStarter is not helping here

        EclipseStarter.setInitialProperties( properties );

        EclipseStarter.startup( nonFrameworkArgs != null ? nonFrameworkArgs : new String[0], null );

        frameworkContext = EclipseStarter.getSystemBundleContext();

        PackageAdmin packageAdmin = null;
        ServiceReference packageAdminRef = frameworkContext.getServiceReference( PackageAdmin.class.getName() );
        if ( packageAdminRef != null )
        {
            packageAdmin = (PackageAdmin) frameworkContext.getService( packageAdminRef );
        }

        if ( packageAdmin == null )
        {
            throw new IllegalStateException( "Could not obtain PackageAdmin service" );
        }

        for ( Bundle bundle : frameworkContext.getBundles() )
        {
            if ( ( packageAdmin.getBundleType( bundle ) & PackageAdmin.BUNDLE_TYPE_FRAGMENT ) == 0 )
            {
                try
                {
                    bundle.start();
                }
                catch ( BundleException e )
                {
                    getLogger().warn( "Could not start bundle " + bundle.getSymbolicName(), e );
                }
            }
        }

        frameworkContext.ungetService( packageAdminRef );
        for ( Runnable callback  : afterStartCallbacks )
        {
            callback.run();
        }
    }

    private void addBundlesDir( StringBuilder bundles, File[] files, boolean absolute )
    {
        if ( files != null )
        {
            for ( File file : files )
            {
                if ( file.getName().startsWith( "org.eclipse.osgi_" ) )
                {
                    continue;
                }

                if ( bundles.length() > 0 )
                {
                    bundles.append( ',' );
                }

                if ( absolute )
                {
                    bundles.append( getReferenceUrl( file ) );
                }
                else
                {
                    String name = file.getName();
                    int verIdx = name.indexOf( '_' );
                    if ( verIdx > 0 )
                    {
                        bundles.append( name.substring( 0, verIdx ) );
                    }
                    else
                    {
                        // TODO barf
                    }
                }

                if ( file.getName().startsWith( "org.eclipse.equinox.ds_" ) )
                {
                    bundles.append( "@1:start" );
                }
            }
        }
    }

    String getReferenceUrl( File file )
    {
        return "reference:" + file.getAbsoluteFile().toURI().normalize().toString();
    }

    public <T> T getService( Class<T> clazz )
    {
        try
        {
            start();
        }
        catch ( Exception e )
        {
            throw new RuntimeException( e );
        }

        // TODO technically, we're leaking service references here
        ServiceReference serviceReference = frameworkContext.getServiceReference( clazz.getName() );

        if ( serviceReference == null )
        {
            throw new IllegalStateException( "Service is not registered " + clazz );
        }

        return clazz.cast( frameworkContext.getService( serviceReference ) );
    }

    public void setNonFrameworkArgs( String[] args )
    {
        nonFrameworkArgs = args;
    }

    public void registerAfterStartCallback( Runnable callback )
    {
        this.afterStartCallbacks.add( callback );
    }
    
}
