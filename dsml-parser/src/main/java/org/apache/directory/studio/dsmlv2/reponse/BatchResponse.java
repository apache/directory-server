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

import org.apache.directory.shared.ldap.codec.LdapResponseCodec;


/**
 * This class represents the Batch Response of a DSML Response
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class BatchResponse
{

    /**
     * The responses contained in the Batch Response
     */
    private List<LdapResponseCodec> responses;

    /**
     * The ID of the response
     */
    private int requestID;


    /**
     * Creates a new instance of BatchResponse.
     */
    public BatchResponse()
    {
        responses = new ArrayList<LdapResponseCodec>();
    }


    /**
     * Adds a reponse
     *
     * @param response
     *      the response to add
     * @return
     *      true (as per the general contract of the Collection.add method)
     */
    public boolean addResponse( LdapResponseCodec response )
    {
        return responses.add( response );
    }


    /**
     * Gets the current reponse
     *
     * @return
     *      the current response
     */
    public LdapResponseCodec getCurrentResponse()
    {
        return responses.get( responses.size() - 1 );
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
     * Gets the List of all the reponses
     *
     * @return
     *      the List of all the responses
     */
    public List getResponses()
    {
        return responses;
    }
}
