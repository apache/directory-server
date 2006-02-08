/*
 *   Copyright 2004 The Apache Software Foundation
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 */
package org.apache.directory.server.core.tools.schema;


import org.apache.directory.server.core.tools.schema.antlrOpenLdapSchemaLexer;
import org.apache.directory.server.core.tools.schema.antlrOpenLdapSchemaParser;
import org.apache.directory.shared.ldap.util.ExceptionUtils;

import java.util.Map;
import java.text.ParseException;
import java.io.*;

import antlr.RecognitionException;
import antlr.TokenStreamException;


/**
 * A reusable wrapper for antlr generated OpenLDAP schema parsers.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class OpenLdapSchemaParser
{
    /** the monitor to use for this parser */
    private ParserMonitor monitor = new ParserMonitorAdapter();
    /** The antlr generated parser */
    private antlrOpenLdapSchemaParser parser = null;
    /** A pipe into the parser */
    private PipedOutputStream parserIn = null;


    /**
     * Creates a reusable instance of an OpenLdapSchemaParser.
     *
     * @throws IOException if the pipe cannot be formed
     */
    public OpenLdapSchemaParser() throws IOException
    {
        init();
    }


    /**
     * Initializes a parser and its plumbing.
     *
     * @throws IOException if a pipe cannot be formed.
     */
    public void init() throws IOException
    {
        parserIn = new PipedOutputStream();
        PipedInputStream in = new PipedInputStream();
        parserIn.connect( in );
        antlrOpenLdapSchemaLexer lexer = new antlrOpenLdapSchemaLexer( in );
        parser = new antlrOpenLdapSchemaParser( lexer );
    }


    public synchronized void clear()
    {
        parser.clear();
    }


    public Map getAttributeTypes()
    {
        return parser.getAttributeTypes();
    }


    public Map getObjectClassTypes()
    {
        return parser.getObjectClasses();
    }


    /**
     * Thread safe method parses an OpenLDAP schemaObject element/object.
     *
     * @param schemaObject the String image of a complete schema object
     */
    public synchronized void parse( String schemaObject ) throws IOException, ParseException
    {
        if ( schemaObject == null || schemaObject.trim().equals( "" ) )
        {
            throw new ParseException( "The schemaObject is either null or is "
                + "the empty String!", 0 );
        }

        this.schemaIn = new ByteArrayInputStream( schemaObject.getBytes() );

        if ( producerThread == null )
        {
            producerThread = new Thread( new DataProducer() );
        }

        producerThread.start();
        invokeParser( schemaObject );
    }


    private void invokeParser( String subject ) throws IOException, ParseException
    {
        try
        {
            monitor.startedParse( "starting parse on:\n" + subject );
            parser.parseSchema();
            monitor.finishedParse( "Done parsing!" );
        }
        catch ( RecognitionException e )
        {
            String msg = "Parser failure on:\n\t" + subject ;
            msg += "\nAntlr exception trace:\n" + ExceptionUtils.getFullStackTrace( e );
            init();
            throw new ParseException( msg, e.getColumn() );
        }
        catch ( TokenStreamException e2 )
        {
            String msg = "Parser failure on:\n\t" + subject ;
            msg += "\nAntlr exception trace:\n" + ExceptionUtils.getFullStackTrace( e2 );
            init();
            throw new ParseException( msg, 0 );
        }
    }


    byte[] buf = new byte[128];
    private InputStream schemaIn;
    private Thread producerThread;

    /**
     * Thread safe method parses a stream of OpenLDAP schemaObject elements/objects.
     *
     * @param schemaIn a stream of schema objects
     */
    public synchronized void parse( InputStream schemaIn ) throws IOException, ParseException
    {
        this.schemaIn = schemaIn;

        if ( producerThread == null )
        {
            producerThread = new Thread( new DataProducer() );
        }

        producerThread.start();
        invokeParser( "schema input stream ==> " + schemaIn.toString() );
    }


    /**
     * Thread safe method parses a file of OpenLDAP schemaObject elements/objects.
     *
     * @param schemaFile a file of schema objects
     */
    public synchronized void parse( File schemaFile ) throws IOException, ParseException
    {
        this.schemaIn = new FileInputStream( schemaFile );

        if ( producerThread == null )
        {
            producerThread = new Thread( new DataProducer() );
        }

        producerThread.start();
        invokeParser( "schema file ==> " + schemaFile.getAbsolutePath() );
    }


    public void setParserMonitor( ParserMonitor monitor )
    {
        this.monitor = monitor ;
        this.parser.setParserMonitor( monitor );
    }


    class DataProducer implements Runnable
    {
        public void run()
        {
            int count = -1;

            try
            {
                while ( ( count = schemaIn.read( buf ) ) != -1 )
                {
                    parserIn.write( buf, 0, count );
                    parserIn.flush();
                }

                // using an input termination token END - need extra space to return
                parserIn.write( "END ".getBytes() );
            }
            catch ( IOException e )
            {
                e.printStackTrace();
            }
        }
    }
}
