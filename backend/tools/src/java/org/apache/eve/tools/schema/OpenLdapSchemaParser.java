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
package org.apache.eve.tools.schema;


import org.apache.ldap.common.util.ExceptionUtils;
import org.apache.ldap.common.NotImplementedException;

import java.io.PipedOutputStream;
import java.io.IOException;
import java.io.PipedInputStream;
import java.text.ParseException;
import java.util.Map;

import antlr.RecognitionException;
import antlr.TokenStreamException;


/**
 * A reusable wrapper for antlr generated OpenLDAP schema parsers.
 *
 * @author <a href="mailto:directory-dev@incubator.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class OpenLdapSchemaParser
{
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


    public void reset()
    {
        throw new NotImplementedException( "impl to clear all parsed objects" );
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
     * Thread safe method parses an OpenLDAP schema file.
     */
    public synchronized void parse( String schema )
        throws IOException, ParseException
    {
        if ( schema == null || schema.trim().equals( "" ) )
        {
            throw new ParseException( "The schema is either null or is "
                + "the empty String!", 0 );
        }

        if ( null == monitor )
        {
            monitor = new ParserMonitorAdapter();
        }

        parserIn.write( schema.getBytes() );

        // using an input termination token END - need extra space to return
        parserIn.write( "END ".getBytes() );
        parserIn.flush();

        try
        {
            parser.parseSchema();
        }
        catch ( RecognitionException e )
        {
            String msg = "Parser failure on schema:\n\t" + schema ;
            msg += "\nAntlr exception trace:\n" + ExceptionUtils.getFullStackTrace( e );
            init();
            throw new ParseException( msg, e.getColumn() );
        }
        catch ( TokenStreamException e2 )
        {
            String msg = "Parser failure on schema:\n\t" + schema ;
            msg += "\nAntlr exception trace:\n" + ExceptionUtils.getFullStackTrace( e2 );
            init();
            throw new ParseException( msg, 0 );
        }
    }


    public void setParserMonitor( ParserMonitor monitor )
    {
        this.monitor = monitor ;
        this.parser.setParserMonitor( monitor );
    }
}
