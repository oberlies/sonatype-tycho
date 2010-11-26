package org.sonatype.tycho.p2.tools;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

import java.io.File;
import java.net.URI;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.sonatype.tycho.p2.repository.ModuleArtifactRepositoryDescriptor;

public class SourceDescriptorTest
{
    private static final File LOCATION_A = new File( "a/location" );

    private static final File LOCATION_B = new File( "another/location" );

    private static final File LOCATION_C = new File( "yet/another/location" );

    private static final ModuleArtifactRepositoryDescriptor MODULE_REPO_A =
        new ModuleArtifactRepositoryDescriptor( LOCATION_A, null, null );

    private static final ModuleArtifactRepositoryDescriptor MODULE_REPO_B =
        new ModuleArtifactRepositoryDescriptor( LOCATION_B, null, null );

    RepositoryReferences subject;

    @Before
    public void initSubject()
    {
        subject = new RepositoryReferences();
    }

    @Test
    public void testNoMetadataRepo()
    {
        assertEquals( 0, subject.getMetadataRepositories().size() );
    }

    @Test
    public void testNoArtifactRepo()
    {
        assertEquals( 0, subject.getArtifactRepositories().size() );
    }

    @Test
    public void testNoModuleArtifactRepos()
    {
        assertEquals( 0, subject.getModuleRepositoriesMap().size() );
    }

    @Test
    public void testMetadataReposWithOrder()
    {
        subject.addMetadataRepository( LOCATION_B );
        subject.addMetadataRepository( LOCATION_A );

        List<URI> repositories = subject.getMetadataRepositories();

        assertEquals( 2, repositories.size() );
        assertEquals( LOCATION_B.toURI(), repositories.get( 0 ) );
        assertEquals( LOCATION_A.toURI(), repositories.get( 1 ) );
    }

    @Test
    public void testNormalArtifactRepos()
    {
        subject.addArtifactRepository( LOCATION_A );
        subject.addArtifactRepository( LOCATION_B );

        List<URI> repositories = subject.getArtifactRepositories();

        assertEquals( 2, repositories.size() );
        assertEquals( LOCATION_A.toURI(), repositories.get( 0 ) );
        assertEquals( LOCATION_B.toURI(), repositories.get( 1 ) );
    }

    @Test
    public void testModuleArtifactRepositoryMap()
    {
        subject.addArtifactRepository( MODULE_REPO_A );
        subject.addArtifactRepository( MODULE_REPO_B );

        Map<URI, ModuleArtifactRepositoryDescriptor> map = subject.getModuleRepositoriesMap();

        assertEquals( 2, map.size() );
        assertSame( MODULE_REPO_A, map.get( LOCATION_A.toURI() ) );
        assertSame( MODULE_REPO_B, map.get( LOCATION_B.toURI() ) );
    }

    @Test
    public void testArtifactRepoOrder()
    {
        subject.addArtifactRepository( MODULE_REPO_B );
        subject.addArtifactRepository( LOCATION_C );
        subject.addArtifactRepository( MODULE_REPO_A );

        List<URI> repositories = subject.getArtifactRepositories();

        assertEquals( 3, repositories.size() );
        assertEquals( LOCATION_B.toURI(), repositories.get( 0 ) );
        assertEquals( LOCATION_C.toURI(), repositories.get( 1 ) );
        assertEquals( LOCATION_A.toURI(), repositories.get( 2 ) );
    }
}
