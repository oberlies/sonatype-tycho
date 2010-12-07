package org.sonatype.tycho.p2.tools.impl.publisher;

import java.io.File;
import java.util.Collection;
import java.util.Collections;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.equinox.internal.p2.publisher.eclipse.IProductDescriptor;
import org.eclipse.equinox.internal.p2.publisher.eclipse.ProductFile;
import org.eclipse.equinox.internal.p2.updatesite.CategoryXMLAction;
import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.publisher.IPublisherAction;
import org.eclipse.equinox.p2.publisher.IPublisherInfo;
import org.eclipse.equinox.p2.publisher.Publisher;
import org.eclipse.equinox.p2.publisher.eclipse.ProductAction;
import org.sonatype.tycho.p2.tools.BuildContext;
import org.sonatype.tycho.p2.tools.FacadeException;
import org.sonatype.tycho.p2.tools.publisher.PublisherService;
import org.sonatype.tycho.p2.util.StatusTool;

@SuppressWarnings( "restriction" )
public class PublisherServiceImpl
    implements PublisherService
{

    private final BuildContext context;

    private final IPublisherInfo publisherInfo;

    private IProvisioningAgent agent;

    /**
     * Creates a new service to execute certain publisher actions.
     * 
     * @param context Context information about the running Maven build.
     * @param publisherInfo The publisher info with target and context repositories configured.
     * @param agent The provisioning agent used to populate the publisher info. The responsibility
     *            for this instance is passed to the newly created instance.
     */
    PublisherServiceImpl( BuildContext context, IPublisherInfo publisherInfo, IProvisioningAgent agent )
    {
        this.context = context;
        this.publisherInfo = publisherInfo;
        this.agent = agent;
    }

    public Collection<IInstallableUnit> publishCategories( File categoryDefinition )
        throws FacadeException, IllegalStateException
    {
        checkRunning();

        /*
         * At this point, we expect that the category.xml file does no longer contain any
         * "qualifier" literals; it is expected that they have been replaced before. Nevertheless we
         * pass the build qualifier to the CategoryXMLAction because this positively affects the IDs
         * of the category IUs (see {@link
         * org.eclipse.equinox.internal.p2.updatesite.SiteXMLAction#buildCategoryId(String)}).
         */
        CategoryXMLAction categoryXMLAction =
            new CategoryXMLAction( categoryDefinition.toURI(), context.getQualifier() );

        /*
         * TODO Fix in Eclipse: category publisher should produce root IUs; workaround: the category
         * publisher produces no "inner" IUs, so just return all IUs
         */
        Collection<IInstallableUnit> allIUs = executePublisher( categoryXMLAction );
        return allIUs;
    }

    public Collection<IInstallableUnit> publishProduct( File productDefinition, File launcherBinaries, String flavor )
        throws FacadeException, IllegalStateException
    {
        checkRunning();

        IProductDescriptor productDescriptor = null;
        try
        {
            productDescriptor = new ProductFile( productDefinition.getAbsolutePath() );
        }
        catch ( Exception e )
        {
            throw new IllegalArgumentException( "Unable to load product file " + productDefinition.getAbsolutePath(), e ); //$NON-NLS-1$
        }

        // TODO Fix in Eclipse: the product action should only return the product IU as root IU
        Collection<IInstallableUnit> allIUs =
            executePublisher( new ProductAction( null, productDescriptor, flavor, launcherBinaries ) );

        // workaround: we know the ID of the product IU
        return selectUnit( allIUs, productDescriptor.getId() );
    }

    private Collection<IInstallableUnit> executePublisher( IPublisherAction action )
        throws FacadeException
    {
        ResultSpyAction resultSpy = new ResultSpyAction();
        IPublisherAction[] actions = new IPublisherAction[] { action, resultSpy };
        Publisher publisher = new Publisher( publisherInfo );

        IStatus result = publisher.publish( actions, null );
        if ( !result.isOK() )
        {
            throw new FacadeException( StatusTool.collectProblems( result ), result.getException() );
        }

        return resultSpy.getAllIUs();
    }

    private Collection<IInstallableUnit> selectUnit( Collection<IInstallableUnit> units, String id )
    {
        for ( IInstallableUnit unit : units )
        {
            if ( id.equals( unit.getId() ) )
            {
                return Collections.singleton( unit );
            }
        }
        throw new IllegalStateException( "ProductAction did not produce product IU" );
    }

    public void stop()
    {
        if ( agent != null )
        {
            agent.stop();
            agent = null;
        }
    }

    private void checkRunning()
        throws IllegalStateException
    {
        if ( agent == null )
            throw new IllegalStateException( "Attempt to access stopped publisher service: " + this ); //$NON-NLS-1$
    }
}
