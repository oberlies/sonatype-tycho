package org.sonatype.tycho.plugins.p2.publisher;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.codehaus.plexus.archiver.ArchiverException;
import org.codehaus.plexus.archiver.UnArchiver;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.tycho.buildversion.VersioningHelper;
import org.codehaus.tycho.model.FeatureRef;
import org.codehaus.tycho.model.PluginRef;
import org.codehaus.tycho.model.ProductConfiguration;
import org.sonatype.tycho.ArtifactDescriptor;
import org.sonatype.tycho.ArtifactKey;
import org.sonatype.tycho.p2.tools.FacadeException;
import org.sonatype.tycho.p2.tools.publisher.PublisherService;

/**
 * This goal invokes the product publisher for each product file found.
 * 
 * @see http://wiki.eclipse.org/Equinox/p2/Publisher
 * @goal publish-products
 */
public final class PublishProductMojo
    extends AbstractPublishMojo
{

    /**
     * @parameter default-value="tooling"
     */
    private String flavor;

    /**
     * @component role="org.codehaus.plexus.archiver.UnArchiver" role-hint="zip"
     */
    private UnArchiver deflater;

    public void execute()
        throws MojoExecutionException, MojoFailureException
    {
        publishProducts();
    }

    private void publishProducts()
        throws MojoExecutionException, MojoFailureException
    {
        PublisherService publisherService = createPublisherService();
        try
        {
            for ( Product product : getProducts() )
            {
                try
                {
                    final Product buildProduct =
                        prepareBuildProduct( product, new File( getProject().getBuild().getDirectory() ),
                                             getQualifier() );

                    publisherService.publishProduct( buildProduct.productFile, getEquinoxExecutableFeature(), flavor );
                }
                catch ( FacadeException e )
                {
                    throw new MojoExecutionException( "Exception while publishing product "
                        + product.getProductFile().getAbsolutePath(), e );
                }
            }
        }
        finally
        {
            publisherService.stop();
        }
    }

    /**
     * Prepare the product file for the Eclipse publisher application.
     * <p>
     * Copies the product file and, if present, corresponding p2 advice file to a working directory.
     * The folder is named after the product ID (stored in the 'uid' attribute!), and the p2 advice
     * file is renamed to "p2.inf" so that the publisher application finds it.
     * </p>
     */
    static Product prepareBuildProduct( Product product, File targetDir, String qualifier )
        throws MojoExecutionException
    {
        try
        {
            ProductConfiguration productConfiguration = ProductConfiguration.read( product.productFile );

            qualifyVersions( productConfiguration, qualifier );

            File buildProductDir = new File( targetDir, "products/" + productConfiguration.getId() );
            buildProductDir.mkdirs();
            final Product buildProduct =
                new Product( new File( buildProductDir, product.getProductFile().getName() ),
                             new File( buildProductDir, "p2.inf" ) );
            ProductConfiguration.write( productConfiguration, buildProduct.productFile );
            copyP2Inf( product.p2infFile, buildProduct.p2infFile );

            return buildProduct;
        }
        catch ( IOException e )
        {
            throw new MojoExecutionException( "I/O exception while writing product definition to disk", e );
        }
    }

    static void copyP2Inf( final File sourceP2Inf, final File buildP2Inf )
        throws IOException
    {
        if ( sourceP2Inf.exists() )
        {
            FileUtils.copyFile( sourceP2Inf, buildP2Inf );
        }
    }

    /**
     * Value class identifying a product file (and optionally an associated p2.inf file) for the
     * {@link PublishProductMojo}.
     */
    static class Product
    {
        private final File productFile;

        private final File p2infFile;

        public Product( File productFile )
        {
            this( productFile, getSourceP2InfFile( productFile ) );
        }

        public Product( File productFile, File p2infFile )
        {
            this.productFile = productFile;
            this.p2infFile = p2infFile;
        }

        public File getProductFile()
        {
            return productFile;
        }

        public File getP2infFile()
        {
            return p2infFile;
        }

        /**
         * We expect an p2 advice file called "xx.p2.inf" next to a product file "xx.product".
         */
        static File getSourceP2InfFile( File productFile )
        {
            final int indexOfExtension = productFile.getName().indexOf( ".product" );
            final String p2infFilename = productFile.getName().substring( 0, indexOfExtension ) + ".p2.inf";
            return new File( productFile.getParentFile(), p2infFilename );
        }

    }

    static void qualifyVersions( ProductConfiguration productConfiguration, String buildQualifier )
    {
        // we need to expand the version otherwise the published artifact still has the '.qualifier'
        String productVersion = productConfiguration.getVersion();
        if ( productVersion != null )
        {
            productVersion = productVersion.replace( VersioningHelper.QUALIFIER, buildQualifier );
            productConfiguration.setVersion( productVersion );
        }

        // now same for the features and bundles that version would be something else than "0.0.0"
        for ( FeatureRef featRef : productConfiguration.getFeatures() )
        {
            if ( featRef.getVersion() != null && featRef.getVersion().indexOf( VersioningHelper.QUALIFIER ) != -1 )
            {
                String newVersion = featRef.getVersion().replace( VersioningHelper.QUALIFIER, buildQualifier );
                featRef.setVersion( newVersion );
            }
        }
        for ( PluginRef plugRef : productConfiguration.getPlugins() )
        {
            if ( plugRef.getVersion() != null && plugRef.getVersion().indexOf( VersioningHelper.QUALIFIER ) != -1 )
            {
                String newVersion = plugRef.getVersion().replace( VersioningHelper.QUALIFIER, buildQualifier );
                plugRef.setVersion( newVersion );
            }
        }
    }

    /**
     * Same code than in the ProductExportMojo. Needed to get the launcher binaries.
     */
    private File getEquinoxExecutableFeature()
        throws MojoExecutionException, MojoFailureException
    {
        ArtifactDescriptor artifact =
            getTargetPlatform().getArtifact( ArtifactKey.TYPE_ECLIPSE_FEATURE, "org.eclipse.equinox.executable", null );

        if ( artifact == null )
        {
            throw new MojoExecutionException( "Unable to locate the equinox launcher feature (aka delta-pack)" );
        }

        File equinoxExecFeature = artifact.getLocation();
        if ( equinoxExecFeature.isDirectory() )
        {
            return equinoxExecFeature.getAbsoluteFile();
        }
        else
        {
            File unzipped =
                new File( getProject().getBuild().getOutputDirectory(), artifact.getKey().getId() + "-"
                    + artifact.getKey().getVersion() );
            if ( unzipped.exists() )
            {
                return unzipped.getAbsoluteFile();
            }
            try
            {
                // unzip now then:
                unzipped.mkdirs();
                deflater.setSourceFile( equinoxExecFeature );
                deflater.setDestDirectory( unzipped );
                deflater.extract();
                return unzipped.getAbsoluteFile();
            }
            catch ( ArchiverException e )
            {
                throw new MojoFailureException( "Unable to unzip the eqiuinox executable feature", e );
            }
        }
    }

    private List<Product> getProducts()
    {
        List<Product> result = new ArrayList<Product>();
        for ( File productFile : getEclipseRepositoryProject().getProductFiles( getProject() ) )
        {
            result.add( new Product( productFile ) );
        }
        return result;
    }

}
