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


import java.io.IOException;
import java.util.HashMap;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;


/**
 * The abstract IGrammar which is the Mother of all the grammars. It contains
 * the transitions table.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public abstract class AbstractGrammar implements IGrammar
{

    /**
     * Table of transitions. It's a two dimension array, the first dimension
     * indice the states, the second dimension indices the Tag value, so it is
     * 256 wide.
     */
    protected HashMap<Tag, GrammarTransition>[] transitions;

    /** The grammar name */
    protected String name;

    /** The grammar's states */
    protected IStates statesEnum;


    /**
     * Return the grammar's name
     * 
     * @return The grammar name
     */
    public String getName()
    {
        return name;
    }


    /**
     * Set the grammar's name
     * 
     * @param name
     *      the name to set
     */
    public void setName( String name )
    {
        this.name = name;
    }


    /**
     * Get the transition associated with the state and tag
     * 
     * @param state
     *            The current state
     * @param tag
     *            The current tag
     * @return A valid transition if any, or null.
     */
    public GrammarTransition getTransition( int state, Tag tag )
    {
        return transitions[state].get( tag );
    }


    /**
     * Get the states of the current grammar
     * 
     * @return 
     *      Returns the statesEnum.
     */
    public IStates getStatesEnum()
    {
        return statesEnum;
    }


    /**
     * Set the states for this grammar
     * 
     * @param statesEnum
     *      The statesEnum to set.
     */
    public void setStatesEnum( IStates statesEnum )
    {
        this.statesEnum = statesEnum;
    }


    /* (non-Javadoc)
     * @see org.apache.directory.studio.dsmlv2.IGrammar#executeAction(org.apache.directory.studio.dsmlv2.Dsmlv2Container)
     */
    public void executeAction( Dsmlv2Container container ) throws XmlPullParserException, IOException
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
            eventType = xpp.next();
        }
        while ( eventType != XmlPullParser.END_DOCUMENT );
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

        GrammarTransition transition = getTransition( container.getState(), new Tag( tagName, tagType ) );

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
}
