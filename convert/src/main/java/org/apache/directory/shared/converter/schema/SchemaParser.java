/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *  
 *    http://www.apache.org/licenses/LICENSE-2.0
 *  
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License. 
 *  
 */
package org.apache.directory.shared.converter.schema;


import org.apache.directory.shared.converter.schema.antlrSchemaLexer;
import org.apache.directory.shared.converter.schema.antlrSchemaParser;
import org.apache.directory.shared.ldap.util.ExceptionUtils;

import java.util.List;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.Writer;
import java.text.ParseException;

import antlr.RecognitionException;
import antlr.TokenStreamException;


/**
 * A reusable wrapper for antlr generated schema parsers.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev: 494164 $
 */
public class SchemaParser
{
    /** The antlr generated parser */
    private antlrSchemaParser parser = null;
    
    /** A pipe into the parser */
    private PipedOutputStream parserIn = null;

    byte[] buf = new byte[128];

    private InputStream schemaIn;
    
    private Thread producerThread;

    /**
     * Creates a reusable instance of an SchemaParser.
     *
     * @throws IOException if the pipe cannot be formed
     */
    public SchemaParser() throws IOException
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
        antlrSchemaLexer lexer = new antlrSchemaLexer( in );
        parser = new antlrSchemaParser( lexer );
    }


    public synchronized void clear()
    {
        parser.clear();
    }

    /**
     * Thread safe method parses an OpenLDAP schemaObject element/object.
     *
     * @param schemaObject the String image of a complete schema object
     */
    public synchronized List<SchemaElement> parse( String schemaObject ) throws IOException, ParseException
    {
        if ( schemaObject == null || schemaObject.trim().equals( "" ) )
        {
            throw new ParseException( "The schemaObject is either null or is " + "the empty String!", 0 );
        }

        this.schemaIn = new ByteArrayInputStream( schemaObject.getBytes() );

        if ( producerThread == null )
        {
            producerThread = new Thread( new DataProducer() );
        }

        producerThread.start();
        return invokeParser( schemaObject );
    }


    private List<SchemaElement> invokeParser( String subject ) throws IOException, ParseException
    {
        try
        {
            parser.parseSchema();
            
            return parser.getSchemaElements();
        }
        catch ( RecognitionException e )
        {
            String msg = "Parser failure on:\n\t" + subject;
            msg += "\nAntlr exception trace:\n" + ExceptionUtils.getFullStackTrace( e );
            init();
            throw new ParseException( msg, e.getColumn() );
        }
        catch ( TokenStreamException e2 )
        {
            String msg = "Parser failure on:\n\t" + subject;
            msg += "\nAntlr exception trace:\n" + ExceptionUtils.getFullStackTrace( e2 );
            init();
            throw new ParseException( msg, 0 );
        }
    }

    /**
     * Thread safe method parses a stream of OpenLDAP schemaObject elements/objects.
     *
     * @param schemaIn a stream of schema objects
     */
    public synchronized List<SchemaElement> parse( InputStream schemaIn, Writer out ) throws IOException, ParseException
    {
        this.schemaIn = schemaIn;

        if ( producerThread == null )
        {
            producerThread = new Thread( new DataProducer() );
        }

        producerThread.start();

        return invokeParser( "schema input stream ==> " + schemaIn.toString() );
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
