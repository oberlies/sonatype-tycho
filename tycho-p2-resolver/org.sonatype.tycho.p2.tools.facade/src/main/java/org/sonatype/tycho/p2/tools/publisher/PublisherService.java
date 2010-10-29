package org.sonatype.tycho.p2.tools.publisher;

import java.io.File;
import java.util.Collection;

public interface PublisherService
{
    /**
     * Publishes given category definitions.
     * 
     * @param categoryDefinition A category.xml file as defined by the Eclipse PDE
     * @throws IllegalStateException if the instance has already been stopped
     * @throws Exception if an internal exception occurs
     */
    Collection<?> publishCategories( File categoryDefinition )
        throws Exception, IllegalStateException;

    /**
     * Publishes the given product definition.
     * 
     * @param productDefinition A .product file as defined by the Eclipse PDE
     * @param launcherBinaries A folder that contains the native Eclipse launcher binaries
     * @param flavor The installation flavor the product shall be published for
     * @throws IllegalStateException if the instance has already been stopped
     * @throws Exception if an internal exception occurs
     */
    Collection<?> publishProduct( File productDefinition, File launcherBinaries, String flavor )
        throws Exception, IllegalStateException;

    /**
     * Stops this PublisherService instance. This shuts down and unregisters internally used
     * services.
     */
    void stop();
}
