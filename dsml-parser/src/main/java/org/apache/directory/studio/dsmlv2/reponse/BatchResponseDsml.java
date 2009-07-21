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
package org.apache.directory.studio.dsmlv2.reponse;


import java.util.ArrayList;
import java.util.List;

import org.apache.directory.studio.dsmlv2.DsmlDecorator;
import org.apache.directory.studio.dsmlv2.ParserUtils;
import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;


/**
 * This class represents the Batch Response. It can be used to generate an the XML String of a BatchResponse.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class BatchResponseDsml
{
    /** The Responses list */
    private List<DsmlDecorator> responses;

    /** The ID of the response */
    private int requestID;


    /**
     * Creates a new instance of BatchResponseDsml.
     */
    public BatchResponseDsml()
    {
        responses = new ArrayList<DsmlDecorator>();
    }


    /**
     * Adds a request to the Batch Response DSML.
     *
     * @param response
     *      the request to add
     * @return
     *      true (as per the general contract of the Collection.add method).
     */
    public boolean addResponse( DsmlDecorator response )
    {
        return responses.add( response );
    }


    /**
     * Removes a request from the Batch Response DSML.
     *
     * @param response
     *      the request to remove
     * @return
     *      true if this list contained the specified element.
     */
    public boolean removeResponse( DsmlDecorator response )
    {
        return responses.remove( response );
    }


    /**
     * Gets the ID of the response
     * @return
     *      the ID of the response
     */
    public int getRequestID()
    {
        return requestID;
    }


    /**
     * Sets the ID of the response
     *
     * @param requestID
     *      the ID to set
     */
    public void setRequestID( int requestID )
    {
        this.requestID = requestID;
    }


    /**
     * Converts the Batch Response to its XML representation in the DSMLv2 format.
     */
    public String toDsml()
    {
        Document document = DocumentHelper.createDocument();
        Element element = document.addElement( "batchResponse" );

        // RequestID
        if ( requestID != 0 )
        {
            element.addAttribute( "requestID", "" + requestID );
        }

        for ( DsmlDecorator response : responses )
        {
            response.toDsml( element );
        }

        return ParserUtils.styleDocument( document ).asXML();
    }
}
