package org.sonatype.tycho.plugins.p2.publisher;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.sonatype.tycho.equinox.EquinoxServiceFactory;
import org.sonatype.tycho.p2.tools.FacadeException;
import org.sonatype.tycho.p2.tools.RepositoryReferences;
import org.sonatype.tycho.p2.tools.publisher.PublisherService;
import org.sonatype.tycho.p2.tools.publisher.PublisherServiceFactory;

public abstract class AbstractPublishMojo
    extends AbstractP2Mojo
{
    static String PUBLISHER_BUNDLE_ID = "org.eclipse.equinox.p2.publisher";

    /** @component */
    private EquinoxServiceFactory osgiServices;

    protected PublisherService createPublisherService()
        throws MojoExecutionException, MojoFailureException
    {
        try
        {
            RepositoryReferences contextRepositories = getVisibleRepositories( false );

            final PublisherServiceFactory publisherServiceFactory =
                osgiServices.getService( PublisherServiceFactory.class );
            return publisherServiceFactory.createPublisher( getPublisherRepositoryLocation(), contextRepositories,
                                                            getBuildContext() );
        }
        catch ( FacadeException e )
        {
            throw new MojoExecutionException( "Exception while initializing the publisher service", e );
        }
    }

    /**
     * Adds the just published installable units into a shared list. The assemble-repository goal
     * eventually uses the units in that list as entry-points for mirroring content into the
     * assembly p2 repository.
     */
    protected void postPublishedIUs( Collection<?> units )
    {
        final MavenProject project = getProject();
        synchronized ( PUBLISHED_ROOT_IUS )
        {
            List<Object> publishedIUs = (List<Object>) project.getContextValue( PUBLISHED_ROOT_IUS );
            if ( publishedIUs == null )
            {
                publishedIUs = new ArrayList<Object>();
                project.setContextValue( PUBLISHED_ROOT_IUS, publishedIUs );
            }
            publishedIUs.addAll( units );
        }
    }
}
