package org.sonatype.tycho.p2.maven.repository;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.query.IQuery;
import org.eclipse.equinox.p2.query.IQueryResult;
import org.eclipse.equinox.p2.repository.IRepositoryReference;
import org.eclipse.equinox.p2.repository.metadata.spi.AbstractMetadataRepository;
import org.sonatype.tycho.p2.maven.repository.xmlio.MetadataIO;
import org.sonatype.tycho.p2.repository.GAV;
import org.sonatype.tycho.p2.repository.RepositoryLayoutHelper;
import org.sonatype.tycho.p2.repository.RepositoryReader;
import org.sonatype.tycho.p2.repository.TychoRepositoryIndex;

public abstract class AbstractMavenMetadataRepository
    extends AbstractMetadataRepository
{
//    private static final String REPOSITORY_TYPE = AbstractMavenMetadataRepository.class.getName();
//
//    private static final String REPOSITORY_VERSION = "1.0.0";

    protected final TychoRepositoryIndex projectIndex;

    protected final RepositoryReader contentLocator;

    protected Set<IInstallableUnit> units = new LinkedHashSet<IInstallableUnit>();

    protected Map<GAV, Set<IInstallableUnit>> unitsMap = new LinkedHashMap<GAV, Set<IInstallableUnit>>();

    public AbstractMavenMetadataRepository( URI location, TychoRepositoryIndex projectIndex,
                                            RepositoryReader contentLocator )
    {
        this( Activator.getProvisioningAgent(), location, projectIndex, contentLocator );
    }

    public AbstractMavenMetadataRepository( IProvisioningAgent agent, URI location, TychoRepositoryIndex projectIndex,
                                            RepositoryReader contentLocator )
    {
        // super( location.toString(), REPOSITORY_TYPE, REPOSITORY_VERSION, location, null, null, properties );
        super( agent );

        setLocation( location );

        this.projectIndex = projectIndex;
        this.contentLocator = contentLocator;

        if ( projectIndex != null && contentLocator != null )
        {
            load();
        }
    }

    protected void load()
    {
        MetadataIO io = new MetadataIO();

        for ( GAV gav : projectIndex.getProjectGAVs() )
        {
            try
            {
                InputStream is =
                    contentLocator.getContents( gav, RepositoryLayoutHelper.CLASSIFIER_P2_METADATA,
                                                RepositoryLayoutHelper.EXTENSION_P2_METADATA );
                try
                {
                    Set<IInstallableUnit> gavUnits = io.readXML( is );

                    unitsMap.put( gav, gavUnits );
                    units.addAll( gavUnits );
                }
                finally
                {
                    is.close();
                }
            }
            catch ( IOException e )
            {
                // TODO throw properly typed exception if repository cannot be loaded
                e.printStackTrace();
            }

        }
    }

    @Override
    public void initialize( RepositoryState state )
    {
    }

    public IQueryResult<IInstallableUnit> query( IQuery<IInstallableUnit> query, IProgressMonitor monitor )
    {
        return query.perform( units.iterator() );
    }

    /**
     * For testing purposes only
     */
    public Map<GAV, Set<IInstallableUnit>> getGAVs()
    {
        return unitsMap;
    }

    public Collection<IRepositoryReference> getReferences()
    {
        return Collections.emptyList();
    }
}
