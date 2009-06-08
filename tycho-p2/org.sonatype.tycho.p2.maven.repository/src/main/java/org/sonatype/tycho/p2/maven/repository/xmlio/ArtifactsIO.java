package org.sonatype.tycho.p2.maven.repository.xmlio;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.LinkedHashSet;
import java.util.Set;

import javax.xml.parsers.ParserConfigurationException;

import org.eclipse.equinox.internal.provisional.p2.artifact.repository.IArtifactDescriptor;
import org.osgi.framework.BundleContext;
import org.sonatype.tycho.p2.maven.repository.Activator;
import org.sonatype.tycho.p2.maven.repository.xmlio35.SimpleArtifactRepositoryIO;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

@SuppressWarnings( "restriction" )
public class ArtifactsIO
{

    private static class Writer35M7
        extends SimpleArtifactRepositoryIO.Writer
    {

        public Writer35M7( OutputStream output )
            throws IOException
        {
            super( output );
        }

        public void write( Set<IArtifactDescriptor> artifactDescriptors )
        {
            writeArtifacts( artifactDescriptors );
            flush();
        }

    }

    private static class Parser35M7
        extends SimpleArtifactRepositoryIO.Parser
    {

        private Set<IArtifactDescriptor> artifacts;

        public Parser35M7( BundleContext context, String bundleId )
        {
            super( context, bundleId );
        }

        public synchronized void parse( InputStream stream )
            throws IOException
        {
            this.status = null;
            try
            {
                // TODO: currently not caching the parser since we make no assumptions
                // or restrictions on concurrent parsing
                getParser();
                ArtifactsHandler artifactsHandler = new ArtifactsHandler();
                xmlReader.setContentHandler( new RepositoryDocHandler( ARTIFACTS_ELEMENT, artifactsHandler ) );
                xmlReader.parse( new InputSource( stream ) );
                if ( isValidXML() )
                {
                    artifacts = artifactsHandler.getArtifacts();
                }
            }
            catch ( SAXException e )
            {
                throw new IOException( e.getMessage() );
            }
            catch ( ParserConfigurationException e )
            {
                throw new IOException( e.getMessage() );
            }
            finally
            {
                stream.close();
            }
        }

        @Override
        protected Object getRootObject()
        {
            return artifacts;
        }

        protected class ArtifactsHandler
            extends RootHandler
        {

            private Set<IArtifactDescriptor> artifacts = new LinkedHashSet<IArtifactDescriptor>();

            public ArtifactsHandler()
            {
            }

            public Set<IArtifactDescriptor> getArtifacts()
            {
                return artifacts;
            }

            public void startElement( String name, Attributes attributes )
            {
                if ( name.equals( ARTIFACT_ELEMENT ) )
                {
                    new ArtifactHandler( this, attributes, artifacts );
                }
                else
                {
                    invalidElement( name, attributes );
                }
            }

            @Override
            protected void handleRootAttributes( Attributes attributes )
            {
            }
        }

        public Set<IArtifactDescriptor> getArtifacts()
        {
            return artifacts;
        }

    }

    public Set<IArtifactDescriptor> readXML( InputStream is )
        throws IOException
    {
        Parser35M7 parser = new Parser35M7( Activator.getContext(), Activator.ID );

        parser.parse( is );

        return parser.getArtifacts();
    }

    public void writeXML( Set<IArtifactDescriptor> descriptors, OutputStream os )
        throws IOException
    {
        new Writer35M7( os ).write( descriptors );
    }

    public void writeXML( LinkedHashSet<IArtifactDescriptor> descriptors, File file )
        throws IOException
    {
        OutputStream os = new BufferedOutputStream( new FileOutputStream( file ) );
        try
        {
            writeXML( descriptors, os );
        }
        finally
        {
            os.close();
        }
    }
}
