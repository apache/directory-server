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

package org.apache.directory.studio.dsmlv2;


import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;

import org.apache.directory.shared.ldap.codec.LdapMessageCodec;
import org.apache.directory.studio.dsmlv2.request.BatchRequest;
import org.apache.directory.studio.dsmlv2.request.Dsmlv2Grammar;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;


/**
 * This class represents the DSMLv2 Parser.
 * It can be used to parse a DSMLv2 Request input.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class Dsmlv2Parser
{
    /** The associated DSMLv2 container */
    private Dsmlv2Container container;


    /**
     * Creates a new instance of Dsmlv2Parser.
     *
     * @throws XmlPullParserException
     *      if an error occurs while the initialization of the parser
     */
    public Dsmlv2Parser() throws XmlPullParserException
    {
        this.container = new Dsmlv2Container();

        this.container.setGrammar( Dsmlv2Grammar.getInstance() );

        XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
        factory.setNamespaceAware( true );
        XmlPullParser xpp = factory.newPullParser();

        container.setParser( xpp );
    }


    /**
     * Sets the input file the parser is going to parse
     *
     * @param fileName
     *      the name of the file
     * @throws FileNotFoundException
     *      if the file does not exist
     * @throws XmlPullParserException
     *      if an error occurs in the parser
     */
    public void setInputFile( String fileName ) throws FileNotFoundException, XmlPullParserException
    {
        Reader reader = new FileReader( fileName );
        container.getParser().setInput( reader );
    }


    /**
     * Sets the input stream the parser is going to process
     *
     * @param inputStream
     *      contains a raw byte input stream of possibly unknown encoding (when inputEncoding is null)
     * @param inputEncoding
     *      if not null it MUST be used as encoding for inputStream
     * @throws XmlPullParserException
     *      if an error occurs in the parser
     */
    public void setInput( InputStream inputStream, String inputEncoding ) throws XmlPullParserException
    {
        container.getParser().setInput( inputStream, inputEncoding );
    }


    /**
     * Sets the input string the parser is going to parse
     *
     * @param str
     *      the string the parser is going to parse
     * @throws XmlPullParserException
     *      if an error occurs in the parser
     */
    public void setInput( String str ) throws XmlPullParserException
    {
        container.getParser().setInput( new StringReader( str ) );
    }


    /**
     * Launches the parsing on the input
     * 
     * @throws XmlPullParserException 
     *      when an unrecoverable error occurs
     * @throws IOException
     */
    public void parse() throws XmlPullParserException, IOException
    {
        Dsmlv2Grammar grammar = Dsmlv2Grammar.getInstance();

        grammar.executeAction( container );
    }


    /**
     * Launches the parsing of the Batch Request only
     *
     * @throws XmlPullParserException
     *      if an error occurs in the parser
     */
    public void parseBatchRequest() throws XmlPullParserException
    {
        XmlPullParser xpp = container.getParser();

        int eventType = xpp.getEventType();
        do
        {
            if ( eventType == XmlPullParser.START_DOCUMENT )
            {
                container.setState( Dsmlv2StatesEnum.INIT_GRAMMAR_STATE );
            }
            else if ( eventType == XmlPullParser.END_DOCUMENT )
            {
                container.setState( Dsmlv2StatesEnum.END_STATE );
            }
            else if ( eventType == XmlPullParser.START_TAG )
            {
                processTag( container, Tag.START );
            }
            else if ( eventType == XmlPullParser.END_TAG )
            {
                processTag( container, Tag.END );
            }
            try
            {
                eventType = xpp.next();
            }
            catch ( IOException e )
            {
                throw new XmlPullParserException( "An IOException ocurred during parsing : " + e.getMessage(), xpp,
                    null );
            }
        }
        while ( container.getState() != Dsmlv2StatesEnum.BATCHREQUEST_START_TAG );
    }


    /**
     * Processes the task required in the grammar to the given tag type
     *
     * @param container
     *      the DSML container
     * @param tagType
     *      the tag type
     * @throws XmlPullParserException 
     *      when an error occurs during the parsing
     */
    private void processTag( Dsmlv2Container container, int tagType ) throws XmlPullParserException
    {
        XmlPullParser xpp = container.getParser();

        String tagName = xpp.getName().toLowerCase();

        GrammarTransition transition = container.getTransition( container.getState(), new Tag( tagName, tagType ) );

        if ( transition != null )
        {
            container.setState( transition.getNextState() );

            if ( transition.hasAction() )
            {
                transition.getAction().action( container );
            }
        }
        else
        {
            throw new XmlPullParserException( "The tag " + new Tag( tagName, tagType )
                + " can't be found at this position", xpp, null );
        }
    }


    /**
     * Gets the Batch Request or null if the it has not been parsed yet
     *
     * @return 
     *      the Batch Request or null if the it has not been parsed yet
     */
    public BatchRequest getBatchRequest()
    {
        return container.getBatchRequest();
    }


    /**
     * Gets the next Request or null if there's no more request
     * @return
     *      the next Request or null if there's no more request
     * @throws XmlPullParserException      
     *      when an error occurs during the parsing
     */
    public LdapMessageCodec getNextRequest() throws XmlPullParserException
    {
        if ( container.getBatchRequest() == null )
        {
            parseBatchRequest();
        }

        XmlPullParser xpp = container.getParser();

        int eventType = xpp.getEventType();
        do
        {
            while ( eventType == XmlPullParser.TEXT )
            {
                try
                {
                    xpp.next();
                }
                catch ( IOException e )
                {
                    throw new XmlPullParserException( "An IOException ocurred during parsing : " + e.getMessage(), xpp,
                        null );
                }
                eventType = xpp.getEventType();
            }

            if ( eventType == XmlPullParser.START_DOCUMENT )
            {
                container.setState( Dsmlv2StatesEnum.INIT_GRAMMAR_STATE );
            }
            else if ( eventType == XmlPullParser.END_DOCUMENT )
            {
                container.setState( Dsmlv2StatesEnum.END_STATE );
                return null;
            }
            else if ( eventType == XmlPullParser.START_TAG )
            {
                processTag( container, Tag.START );
            }
            else if ( eventType == XmlPullParser.END_TAG )
            {
                processTag( container, Tag.END );
            }
            try
            {
                eventType = xpp.next();
            }
            catch ( IOException e )
            {
                throw new XmlPullParserException( "An IOException ocurred during parsing : " + e.getMessage(), xpp,
                    null );
            }
        }
        while ( container.getState() != Dsmlv2StatesEnum.BATCHREQUEST_LOOP );

        return container.getBatchRequest().getCurrentRequest();
    }


    /**
     * Parses all the requests
     *
     * @throws XmlPullParserException
     *      when an error occurs during the parsing
     */
    public void parseAllRequests() throws XmlPullParserException
    {
        while ( getNextRequest() != null )
        {
            continue;
        }
    }
}
