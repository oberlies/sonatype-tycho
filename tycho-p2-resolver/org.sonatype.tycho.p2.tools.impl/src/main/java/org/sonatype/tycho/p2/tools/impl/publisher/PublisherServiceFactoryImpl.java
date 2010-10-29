package org.sonatype.tycho.p2.tools.impl.publisher;

import java.io.File;
import java.util.Collection;

import org.eclipse.equinox.internal.p2.artifact.repository.CompositeArtifactRepository;
import org.eclipse.equinox.internal.p2.metadata.repository.CompositeMetadataRepository;
import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.publisher.IPublisherInfo;
import org.eclipse.equinox.p2.publisher.Publisher;
import org.eclipse.equinox.p2.publisher.PublisherInfo;
import org.eclipse.equinox.p2.repository.ICompositeRepository;
import org.eclipse.equinox.p2.repository.artifact.IArtifactRepository;
import org.sonatype.tycho.p2.tools.impl.Activator;
import org.sonatype.tycho.p2.tools.publisher.BuildContext;
import org.sonatype.tycho.p2.tools.publisher.PublisherService;
import org.sonatype.tycho.p2.tools.publisher.PublisherServiceFactory;

@SuppressWarnings( "restriction" )
public class PublisherServiceFactoryImpl
    implements PublisherServiceFactory
{

    public PublisherService createPublisher( File targetRepository, Collection<File> contextMetadataRepositories,
                                             Collection<File> contextArtifactRepositories, BuildContext context,
                                             int flags )
        throws Exception
    {
        // create an own instance of the provisioning agent to prevent cross talk with other things
        // that happen in the Tycho OSGi runtime
        File agentConfigurationFolder = new File( context.getTargetDirectory(), "p2agent" );
        IProvisioningAgent agent = Activator.createProvisioningAgent( agentConfigurationFolder.toURI() );

        try
        {
            PublisherInfo publisherInfo = new PublisherInfo();
            publisherInfo.setArtifactOptions( IPublisherInfo.A_INDEX | IPublisherInfo.A_PUBLISH );

            boolean compress = ( flags & REPOSITORY_COMPRESS ) != 0;
            boolean reusePackedFiles = false; // TODO check if we can/should use this
            String repositoryName = "eclipse-repository"; // TODO proper name for repo, e.g. GAV? (pending in TYCHO-513)
            IArtifactRepository targetArtifactRepo =
                Publisher.createArtifactRepository( agent, targetRepository.toURI(), repositoryName, compress,
                                                    reusePackedFiles );
            publisherInfo.setArtifactRepository( targetArtifactRepo );

            boolean append = true;
            publisherInfo.setMetadataRepository( Publisher.createMetadataRepository( agent, targetRepository.toURI(),
                                                                                     repositoryName, append, compress ) );

            // the ProductAction needs to know for which configurations it needs to generate configIUs
            publisherInfo.setConfigurations( context.getConfigurations() );

            // set context repositories
            if ( contextMetadataRepositories != null && contextMetadataRepositories.size() > 0 )
            {
                CompositeMetadataRepository contextMetadata = CompositeMetadataRepository.createMemoryComposite( agent );
                addToComposite( contextMetadataRepositories, contextMetadata );
                publisherInfo.setContextMetadataRepository( contextMetadata );
            }
            if ( contextArtifactRepositories != null && contextArtifactRepositories.size() > 0 )
            {
                CompositeArtifactRepository contextArtifact = CompositeArtifactRepository.createMemoryComposite( agent );
                addToComposite( contextArtifactRepositories, contextArtifact );
                publisherInfo.setContextArtifactRepository( contextArtifact );
            }

            return new PublisherServiceImpl( context, publisherInfo, agent );
        }
        catch ( Exception e )
        {
            agent.stop();
            throw e;
        }
    }

    private void addToComposite( Collection<File> repositoryLocations, ICompositeRepository<?> compositeRepository )
    {
        for ( File repositoryLocation : repositoryLocations )
        {
            compositeRepository.addChild( repositoryLocation.toURI() );
        }
    }
}
