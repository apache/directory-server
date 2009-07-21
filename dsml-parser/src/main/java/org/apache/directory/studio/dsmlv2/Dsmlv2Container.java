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


import org.apache.directory.studio.dsmlv2.reponse.BatchResponse;
import org.apache.directory.studio.dsmlv2.request.BatchRequest;
import org.xmlpull.v1.XmlPullParser;


/**
 * This class represents the DSML Container.
 * It used by the DSML Parser to store information.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class Dsmlv2Container implements Container
{
    /** The current state of the decoding */
    private int state;

    /** The current transition */
    private int transition;

    /** Store the different states for debug purpose */
    private IStates states;

    /** The pool parser */
    private XmlPullParser parser;

    /** The BatchRequest of the parsing */
    private BatchRequest batchRequest;

    /** The BatchResponse of the parsing */
    private BatchResponse batchResponse;

    /**  The associated grammar */
    private AbstractGrammar grammar;


    /**
     * Gets the DSML Batch Request
     * 
     * @return
     *      Returns the Batch Request
     */
    public BatchRequest getBatchRequest()
    {
        return batchRequest;
    }


    /**
     * Sets the DSML Batch Request
     * 
     * @param batchRequest
     *      the Batch Request to set
     */
    public void setBatchRequest( BatchRequest batchRequest )
    {
        this.batchRequest = batchRequest;
    }


    /**
     * Gets the DSML Batch Response
     * 
     * @return
     *      Returns the Batch Response
     */
    public BatchResponse getBatchResponse()
    {
        return batchResponse;
    }


    /**
     * Sets the DSML Batch Request
     * 
     * @param batchResponse
     *      the Batch Response to set
     */
    public void setBatchResponse( BatchResponse batchResponse )
    {
        this.batchResponse = batchResponse;
    }


    /**
     * Gets the parser
     * 
     * @return
     *      the parser
     */
    public XmlPullParser getParser()
    {
        return parser;
    }


    /**
     * Sets the parser
     * 
     * @param parser
     *      the parser to set
     */
    public void setParser( XmlPullParser parser )
    {
        this.parser = parser;
    }


    /**
     * Get the current grammar state
     * 
     * @return
     *      the current grammar state
     */
    public int getState()
    {
        return state;
    }


    /**
     * Set the new current state
     * 
     * @param state
     *      the new state
     */
    public void setState( int state )
    {
        this.state = state;
    }


    /**
     * Get the transition
     * 
     * @return
     *      the transition from the previous state to the new state
     */
    public int getTransition()
    {
        return transition;
    }


    /**
     * Update the transition from a state to another
     * 
     * @param transition
     *      the transition to set
     */
    public void setTransition( int transition )
    {
        this.transition = transition;
    }


    /**
     * Get the states for this container's grammars
     * 
     * @return
     *      the states.
     */
    public IStates getStates()
    {
        return states;
    }


    /**
     * Gets the grammar
     *
     * @return
     *      the grammar
     */
    public AbstractGrammar getGrammar()
    {
        return grammar;
    }


    /**
     * Sets the Grammar
     * 
     * @param grammar
     *      the grammar to set
     */
    public void setGrammar( AbstractGrammar grammar )
    {
        this.grammar = grammar;
    }


    /**
     * Get the transition associated with the state and tag
     * 
     * @param state
     *      the current state
     * @param tag
     *      the current tag
     * @return
     *      a valid transition if any, or null.
     */
    public GrammarTransition getTransition( int state, Tag tag )
    {
        return grammar.getTransition( state, tag );
    }
}
