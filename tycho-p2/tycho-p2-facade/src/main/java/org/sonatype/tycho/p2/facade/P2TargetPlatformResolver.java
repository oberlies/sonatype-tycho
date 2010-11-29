package org.sonatype.tycho.p2.facade;

import java.io.File;
import java.io.FileNotFoundException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.apache.maven.ProjectDependenciesResolver;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.ArtifactUtils;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.ArtifactRepositoryPolicy;
import org.apache.maven.artifact.repository.Authentication;
import org.apache.maven.artifact.repository.layout.ArtifactRepositoryLayout;
import org.apache.maven.artifact.resolver.AbstractArtifactResolutionException;
import org.apache.maven.artifact.resolver.MultipleArtifactsNotFoundException;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Plugin;
import org.apache.maven.project.MavenProject;
import org.apache.maven.repository.RepositorySystem;
import org.apache.maven.settings.Mirror;
import org.apache.maven.settings.Server;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.Initializable;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.InitializationException;
import org.codehaus.tycho.TargetEnvironment;
import org.codehaus.tycho.TargetPlatform;
import org.codehaus.tycho.TargetPlatformConfiguration;
import org.codehaus.tycho.TargetPlatformResolver;
import org.codehaus.tycho.TychoConstants;
import org.codehaus.tycho.model.Target;
import org.codehaus.tycho.osgitools.DebugUtils;
import org.codehaus.tycho.osgitools.DefaultArtifactKey;
import org.codehaus.tycho.osgitools.targetplatform.AbstractTargetPlatformResolver;
import org.codehaus.tycho.osgitools.targetplatform.DefaultTargetPlatform;
import org.codehaus.tycho.osgitools.targetplatform.MultiEnvironmentTargetPlatform;
import org.codehaus.tycho.p2.P2ArtifactRepositoryLayout;
import org.codehaus.tycho.utils.ExecutionEnvironmentUtils;
import org.codehaus.tycho.utils.PlatformPropertiesUtils;
import org.sonatype.tycho.ArtifactKey;
import org.sonatype.tycho.ReactorProject;
import org.sonatype.tycho.equinox.EquinoxServiceFactory;
import org.sonatype.tycho.p2.DependencyMetadataGenerator;
import org.sonatype.tycho.p2.facade.internal.ArtifactFacade;
import org.sonatype.tycho.p2.facade.internal.MavenRepositoryReader;
import org.sonatype.tycho.p2.facade.internal.P2RepositoryCacheImpl;
import org.sonatype.tycho.p2.facade.internal.ReactorArtifactFacade;
import org.sonatype.tycho.p2.repository.DefaultTychoRepositoryIndex;
import org.sonatype.tycho.p2.repository.TychoRepositoryIndex;
import org.sonatype.tycho.p2.resolver.P2Logger;
import org.sonatype.tycho.p2.resolver.P2ResolutionResult;
import org.sonatype.tycho.p2.resolver.P2Resolver;
import org.sonatype.tycho.p2.resolver.P2ResolverFactory;

@Component( role = TargetPlatformResolver.class, hint = P2TargetPlatformResolver.ROLE_HINT, instantiationStrategy = "per-lookup" )
public class P2TargetPlatformResolver
    extends AbstractTargetPlatformResolver
    implements TargetPlatformResolver, Initializable
{

    public static final String ROLE_HINT = "p2";

    @Requirement
    private EquinoxServiceFactory equinox;

    @Requirement
    private PlexusContainer plexus;

    @Requirement
    private RepositorySystem repositorySystem;

    @Requirement( hint = "p2" )
    private ArtifactRepositoryLayout p2layout;

    @Requirement
    private P2RepositoryCacheImpl repositoryCache;

    @Requirement
    private ProjectDependenciesResolver projectDependenciesResolver;

    private P2ResolverFactory resolverFactory;

    private DependencyMetadataGenerator generator;

    private DependencyMetadataGenerator sourcesGenerator;

    private static final ArtifactRepositoryPolicy P2_REPOSITORY_POLICY =
        new ArtifactRepositoryPolicy( true, ArtifactRepositoryPolicy.UPDATE_POLICY_NEVER,
                                      ArtifactRepositoryPolicy.CHECKSUM_POLICY_IGNORE );

    public void setupProjects( MavenSession session, MavenProject project, ReactorProject reactorProject )
    {
        TargetPlatformConfiguration configuration =
            (TargetPlatformConfiguration) project.getContextValue( TychoConstants.CTX_TARGET_PLATFORM_CONFIGURATION );
        List<Map<String, String>> environments = getEnvironments( configuration );
        Set<Object> metadata =
            generator.generateMetadata( new ReactorArtifactFacade( reactorProject, null ), environments );
        reactorProject.setDependencyMetadata( null, metadata );

        // TODO this should be moved to osgi-sources-plugin somehow
        if ( isBundleProject( project ) && hasSourceBundle( project ) )
        {
            ReactorArtifactFacade sourcesArtifact = new ReactorArtifactFacade( reactorProject, "sources" );
            Set<Object> sourcesMetadata = sourcesGenerator.generateMetadata( sourcesArtifact, environments );
            reactorProject.setDependencyMetadata( sourcesArtifact.getClassidier(), sourcesMetadata );
        }
    }

    private static boolean isBundleProject( MavenProject project )
    {
        String type = project.getPackaging();
        return ArtifactKey.TYPE_ECLIPSE_PLUGIN.equals( type ) || ArtifactKey.TYPE_ECLIPSE_TEST_PLUGIN.equals( type );
    }

    private static boolean hasSourceBundle( MavenProject project )
    {
        // TODO this is a fragile way of checking whether we generate a source bundle
        // should we rather use MavenSession to get the actual configured mojo instance?
        for ( Plugin plugin : project.getBuildPlugins() )
        {
            if ( "org.sonatype.tycho:maven-osgi-source-plugin".equals( plugin.getKey() ) )
            {
                return true;
            }
        }
        return false;
    }

    public TargetPlatform resolvePlatform( MavenSession session, MavenProject project,
                                           List<ReactorProject> reactorProjects, List<Dependency> dependencies )
    {
        P2Resolver resolver = resolverFactory.createResolver();

        try
        {
            return doResolvePlatform( session, project, reactorProjects, dependencies, resolver );
        }
        finally
        {
            resolver.stop();
        }
    }

    protected TargetPlatform doResolvePlatform( final MavenSession session, final MavenProject project,
                                                List<ReactorProject> reactorProjects, List<Dependency> dependencies,
                                                P2Resolver resolver )
    {
        TargetPlatformConfiguration configuration =
            (TargetPlatformConfiguration) project.getContextValue( TychoConstants.CTX_TARGET_PLATFORM_CONFIGURATION );

        resolver.setRepositoryCache( repositoryCache );

        resolver.setOffline( session.isOffline() );

        resolver.setLogger( new P2Logger()
        {
            public void debug( String message )
            {
                if ( message != null && message.length() > 0 )
                {
                    getLogger().info( message ); // TODO
                }
            }

            public void info( String message )
            {
                if ( message != null && message.length() > 0 )
                {
                    getLogger().info( message );
                }
            }

            public boolean isDebugEnabled()
            {
                return getLogger().isDebugEnabled() && DebugUtils.isDebugEnabled( session, project );
            }
        } );

        Map<File, ReactorProject> projects = new HashMap<File, ReactorProject>();

        resolver.setLocalRepositoryLocation( new File( session.getLocalRepository().getBasedir() ) );

        resolver.setEnvironments( getEnvironments( configuration ) );

        for ( ReactorProject otherProject : reactorProjects )
        {
            if ( getLogger().isDebugEnabled() )
            {
                getLogger().debug( "P2resolver.addMavenProject " + otherProject.getId() );
            }
            projects.put( otherProject.getBasedir(), otherProject );
            resolver.addReactorArtifact( new ReactorArtifactFacade( otherProject, null ) );

            Map<String, Set<Object>> dependencyMetadata = otherProject.getDependencyMetadata();
            if ( dependencyMetadata != null )
            {
                for ( String classifier : dependencyMetadata.keySet() )
                {
                    resolver.addReactorArtifact( new ReactorArtifactFacade( otherProject, classifier ) );
                }
            }
        }

        if ( dependencies != null )
        {
            for ( Dependency dependency : dependencies )
            {
                resolver.addDependency( dependency.getType(), dependency.getArtifactId(), dependency.getVersion() );
            }
        }

        if ( TargetPlatformConfiguration.POM_DEPENDENCIES_CONSIDER.equals( configuration.getPomDependencies() ) )
        {
            Set<String> projectIds = new HashSet<String>();
            for ( ReactorProject p : reactorProjects )
            {
                String key = ArtifactUtils.key( p.getGroupId(), p.getArtifactId(), p.getVersion() );
                projectIds.add( key );
            }

            ArrayList<String> scopes = new ArrayList<String>();
            scopes.add( Artifact.SCOPE_COMPILE );
            Collection<Artifact> artifacts;
            try
            {
                artifacts = projectDependenciesResolver.resolve( project, scopes, session );
            }
            catch ( MultipleArtifactsNotFoundException e )
            {
                Collection<Artifact> missing = new HashSet<Artifact>( e.getMissingArtifacts() );

                for ( Iterator<Artifact> it = missing.iterator(); it.hasNext(); )
                {
                    Artifact a = it.next();
                    String key = ArtifactUtils.key( a.getGroupId(), a.getArtifactId(), a.getBaseVersion() );
                    if ( projectIds.contains( key ) )
                    {
                        it.remove();
                    }
                }

                if ( !missing.isEmpty() )
                {
                    throw new RuntimeException( "Could not resolve project dependencies", e );
                }

                artifacts = e.getResolvedArtifacts();
                artifacts.removeAll( e.getMissingArtifacts() );
            }
            catch ( AbstractArtifactResolutionException e )
            {
                throw new RuntimeException( "Could not resolve project dependencies", e );
            }
            for ( Artifact artifact : artifacts )
            {
                String key =
                    ArtifactUtils.key( artifact.getGroupId(), artifact.getArtifactId(), artifact.getBaseVersion() );
                if ( projectIds.contains( key ) )
                {
                    // resolved to an older snapshot from the repo, we only want the current project in the reactor
                    continue;
                }
                if ( getLogger().isDebugEnabled() )
                {
                    getLogger().debug( "P2resolver.addMavenArtifact " + artifact.toString() );
                }
                resolver.addMavenArtifact( new ArtifactFacade( artifact ) );
            }
        }

        for ( ArtifactRepository repository : project.getRemoteArtifactRepositories() )
        {
            try
            {
                URI uri = new URL( repository.getUrl() ).toURI();

                if ( repository.getLayout() instanceof P2ArtifactRepositoryLayout )
                {
                    if ( session.isOffline() )
                    {
                        getLogger().debug( "Offline mode, using local cache only for repository " + repository.getId()
                                               + " (" + repository.getUrl() + ")" );
                    }

                    try
                    {
                        Authentication auth = repository.getAuthentication();
                        if ( auth != null )
                        {
                            resolver.setCredentials( uri, auth.getUsername(), auth.getPassword() );
                        }

                        resolver.addP2Repository( uri );

                        getLogger().debug( "Added p2 repository " + repository.getId() + " (" + repository.getUrl()
                                               + ")" );
                    }
                    catch ( Exception e )
                    {
                        String msg =
                            "Failed to access p2 repository " + repository.getId() + " (" + repository.getUrl()
                                + "), will try to use local cache. Reason: " + e.getMessage();
                        if ( getLogger().isDebugEnabled() )
                        {
                            getLogger().warn( msg, e );
                        }
                        else
                        {
                            getLogger().warn( msg );
                        }
                    }
                }
                else
                {
                    if ( !configuration.isIgnoreTychoRepositories() && !session.isOffline() )
                    {
                        try
                        {
                            MavenRepositoryReader reader = plexus.lookup( MavenRepositoryReader.class );
                            reader.setArtifactRepository( repository );
                            reader.setLocalRepository( session.getLocalRepository() );

                            String repositoryKey = getRepositoryKey( repository );
                            TychoRepositoryIndex index = repositoryCache.getRepositoryIndex( repositoryKey );
                            if ( index == null )
                            {
                                index = new DefaultTychoRepositoryIndex( reader );

                                repositoryCache.putRepositoryIndex( repositoryKey, index );
                            }

                            resolver.addMavenRepository( uri, index, reader );
                            getLogger().debug( "Added Maven repository " + repository.getId() + " ("
                                                   + repository.getUrl() + ")" );
                        }
                        catch ( FileNotFoundException e )
                        {
                            // it happens
                        }
                        catch ( Exception e )
                        {
                            getLogger().debug( "Unable to initialize remote Tycho repository", e );
                        }
                    }
                    else
                    {
                        String msg =
                            "Ignoring Maven repository " + repository.getId() + " (" + repository.getUrl() + ")";
                        if ( session.isOffline() )
                        {
                            msg += " while in offline mode";
                        }
                        getLogger().debug( msg );
                    }
                }
            }
            catch ( MalformedURLException e )
            {
                getLogger().warn( "Could not parse repository URL", e );
            }
            catch ( URISyntaxException e )
            {
                getLogger().warn( "Could not parse repository URL", e );
            }
        }

        Target target = configuration.getTarget();

        if ( target != null )
        {
            Set<URI> uris = new HashSet<URI>();

            for ( Target.Location location : target.getLocations() )
            {
                String type = location.getType();
                if ( !"InstallableUnit".equalsIgnoreCase( type ) )
                {
                    getLogger().warn( "Target location type: " + type + " is not supported" );
                    continue;
                }
                for ( Target.Repository repository : location.getRepositories() )
                {

                    try
                    {
                        URI uri = new URI( getMirror( repository, session.getRequest().getMirrors() ) );
                        if ( uris.add( uri ) )
                        {
                            if ( session.isOffline() )
                            {
                                getLogger().debug( "Ignored repository " + uri + " while in offline mode" );
                            }
                            else
                            {
                                String id = repository.getId();
                                if ( id != null )
                                {
                                    Server server = session.getSettings().getServer( id );

                                    if ( server != null )
                                    {
                                        resolver.setCredentials( uri, server.getUsername(), server.getPassword() );
                                    }
                                    else
                                    {
                                        getLogger().info( "Unknown server id=" + id + " for repository location="
                                                              + repository.getLocation() );
                                    }
                                }

                                try
                                {
                                    resolver.addP2Repository( uri );
                                }
                                catch ( Exception e )
                                {
                                    String msg =
                                        "Failed to access p2 repository " + uri
                                            + ", will try to use local cache. Reason: " + e.getMessage();
                                    if ( getLogger().isDebugEnabled() )
                                    {
                                        getLogger().warn( msg, e );
                                    }
                                    else
                                    {
                                        getLogger().warn( msg );
                                    }
                                }
                            }
                        }
                    }
                    catch ( URISyntaxException e )
                    {
                        getLogger().debug( "Could not parse repository URL", e );
                    }
                }

                for ( Target.Unit unit : location.getUnits() )
                {
                    resolver.addDependency( P2Resolver.TYPE_INSTALLABLE_UNIT, unit.getId(), unit.getVersion() );
                }
            }
        }

        if ( !isAllowConflictingDependencies( project, configuration ) )
        {
            List<P2ResolutionResult> results = resolver.resolveProject( project.getBasedir() );

            MultiEnvironmentTargetPlatform multiPlatform = new MultiEnvironmentTargetPlatform();

            // FIXME this is just wrong
            for ( int i = 0; i < configuration.getEnvironments().size(); i++ )
            {
                TargetEnvironment environment = configuration.getEnvironments().get( i );
                P2ResolutionResult result = results.get( i );

                DefaultTargetPlatform platform = newDefaultTargetPlatform( session, projects, result );

                // addProjects( session, platform );

                multiPlatform.addPlatform( environment, platform );
            }

            return multiPlatform;
        }
        else
        {
            P2ResolutionResult result = resolver.collectProjectDependencies( project.getBasedir() );

            return newDefaultTargetPlatform( session, projects, result );
        }
    }

    private boolean isAllowConflictingDependencies( MavenProject project, TargetPlatformConfiguration configuration )
    {
        String packaging = project.getPackaging();

        if ( org.sonatype.tycho.ArtifactKey.TYPE_ECLIPSE_UPDATE_SITE.equals( packaging )
            || org.sonatype.tycho.ArtifactKey.TYPE_ECLIPSE_FEATURE.equals( packaging ) )
        {
            Boolean allow = configuration.getAllowConflictingDependencies();
            if ( allow != null )
            {
                return allow.booleanValue();
            }
        }

        // conflicting dependencies do not make sense for products and bundles
        return false;
    }

    protected DefaultTargetPlatform newDefaultTargetPlatform( MavenSession session, Map<File, ReactorProject> projects,
                                                              P2ResolutionResult result )
    {
        DefaultTargetPlatform platform = new DefaultTargetPlatform();

        platform.addSite( new File( session.getLocalRepository().getBasedir() ) );

        platform.addNonReactorUnits( result.getNonReactorUnits() );

        for ( P2ResolutionResult.Entry entry : result.getArtifacts() )
        {
            ArtifactKey key = new DefaultArtifactKey( entry.getType(), entry.getId(), entry.getVersion() );
            ReactorProject otherProject = projects.get( entry.getLocation() );
            if ( otherProject != null )
            {
                platform.addReactorArtifact( key, otherProject, entry.getClassifier(), entry.getInstallableUnits() );
            }
            else
            {
                platform.addArtifactFile( key, entry.getLocation(), entry.getInstallableUnits() );
            }
        }
        return platform;
    }

    private List<Map<String, String>> getEnvironments( TargetPlatformConfiguration configuration )
    {
        ArrayList<Map<String, String>> environments = new ArrayList<Map<String, String>>();

        for ( TargetEnvironment environment : configuration.getEnvironments() )
        {
            Properties properties = new Properties();
            properties.put( PlatformPropertiesUtils.OSGI_OS, environment.getOs() );
            properties.put( PlatformPropertiesUtils.OSGI_WS, environment.getWs() );
            properties.put( PlatformPropertiesUtils.OSGI_ARCH, environment.getArch() );
            ExecutionEnvironmentUtils.loadVMProfile( properties );

            // TODO does not belong here
            properties.put( "org.eclipse.update.install.features", "true" );

            Map<String, String> map = new LinkedHashMap<String, String>();
            for ( Object key : properties.keySet() )
            {
                map.put( key.toString(), properties.getProperty( key.toString() ) );
            }
            environments.add( map );
        }

        return environments;
    }

    private String getRepositoryKey( ArtifactRepository repository )
    {
        StringBuilder sb = new StringBuilder();
        sb.append( repository.getId() );
        sb.append( '|' ).append( repository.getUrl() );
        return sb.toString();
    }

    private String getMirror( Target.Repository location, List<Mirror> mirrors )
    {
        String url = location.getLocation();
        String id = location.getId();
        if ( id == null )
        {
            id = url;
        }

        ArtifactRepository repository =
            repositorySystem.createArtifactRepository( id, url, p2layout, P2_REPOSITORY_POLICY, P2_REPOSITORY_POLICY );

        Mirror mirror = repositorySystem.getMirror( repository, mirrors );

        return mirror != null ? mirror.getUrl() : url;
    }

    public void initialize()
        throws InitializationException
    {
        this.resolverFactory = equinox.getService( P2ResolverFactory.class );
        this.generator = equinox.getService( DependencyMetadataGenerator.class, "(role-hint=dependency-only)" );
        this.sourcesGenerator = equinox.getService( DependencyMetadataGenerator.class, "(role-hint=source-bundle)" );
    }
}
