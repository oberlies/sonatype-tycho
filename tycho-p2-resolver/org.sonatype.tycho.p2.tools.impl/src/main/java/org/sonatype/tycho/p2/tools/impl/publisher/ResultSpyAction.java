package org.sonatype.tycho.p2.tools.impl.publisher;

import java.util.Collection;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.publisher.IPublisherAction;
import org.eclipse.equinox.p2.publisher.IPublisherInfo;
import org.eclipse.equinox.p2.publisher.IPublisherResult;
import org.eclipse.equinox.p2.publisher.Publisher;

/**
 * This publisher action does nothing but storing the IPublisherResult instance used by the
 * publisher. This is a workaround for missing getters in {@link Publisher}.
 */
// TODO Provide patch to Eclipse to get rid of this workaround
@SuppressWarnings( "restriction" )
public class ResultSpyAction
    implements IPublisherAction
{
    private Collection<IInstallableUnit> rootIUs = null;

    public IStatus perform( IPublisherInfo info, IPublisherResult results, IProgressMonitor monitor )
    {
        if ( rootIUs != null )
        {
            throw new IllegalStateException( ResultSpyAction.class.getSimpleName()
                + " cannot be performed more than once" );
        }
        rootIUs = results.getIUs( null, IPublisherResult.ROOT );
        return Status.OK_STATUS;
    }

    /**
     * Returns the root IUs in the publisher result at the time when this action was invoked by the
     * {@link Publisher}.
     * 
     * @throws IllegalStateException if the action has not been performed.
     */
    public Collection<IInstallableUnit> getRootIUs()
        throws IllegalStateException
    {
        if ( rootIUs == null )
        {
            throw new IllegalStateException( ResultSpyAction.class.getSimpleName() + " has not been performed" );
        }
        return rootIUs;
    }
}
