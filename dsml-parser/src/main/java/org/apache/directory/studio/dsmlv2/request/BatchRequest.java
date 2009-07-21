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

package org.apache.directory.studio.dsmlv2.request;


import java.util.ArrayList;
import java.util.List;

import org.apache.directory.shared.ldap.codec.LdapMessageCodec;


/**
 * This class represents the Batch Request of a DSML Request
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class BatchRequest
{
    /**
     * The requests contained in the Batch Request
     */
    private List<LdapMessageCodec> requests;

    /**
     * The ID of the request
     */
    private int requestID;

    /**
     * This enum represents the different types of processing for a Batch Request 
     *
     * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
     * @version $Rev$, $Date$
     */
    public enum Processing
    {
        SEQUENTIAL, PARALLEL
    };

    /**
     * The type of processing of the Batch Request
     */
    private Processing processing;

    /**
     * This enum represents the different types of on error handling for a BatchRequest
     *
     * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
     * @version $Rev$, $Date$
     */
    public enum OnError
    {
        RESUME, EXIT
    };

    /**
     * The type of on error handling
     */
    private OnError onError;

    /**
     * This enum represents the different types of response order for a Batch Request
     *
     * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
     * @version $Rev$, $Date$
     */
    public enum ResponseOrder
    {
        SEQUENTIAL, UNORDERED
    };

    /**
     * The response order
     */
    private ResponseOrder responseOrder;


    /**
     * Creates a new instance of BatchRequest.
     */
    public BatchRequest()
    {
        requests = new ArrayList<LdapMessageCodec>();
        responseOrder = ResponseOrder.SEQUENTIAL;
        processing = Processing.SEQUENTIAL;
        onError = OnError.EXIT;
    }


    /**
     * Adds a request
     *
     * @param request
     *      the resquest to add
     * @return
     *      true (as per the general contract of the Collection.add method)
     */
    public boolean addRequest( LdapMessageCodec request )
    {
        return requests.add( request );
    }


    /**
     * Gets the current request
     *
     * @return
     *      the current request
     */
    public LdapMessageCodec getCurrentRequest()
    {
        return requests.get( requests.size() - 1 );
    }


    /**
     * Gets the ID of the request
     *
     * @return
     *      the ID of the request
     */
    public int getRequestID()
    {
        return requestID;
    }


    /**
     * Sets the ID of the request
     *
     * @param requestID
     *      the ID to set
     */
    public void setRequestID( int requestID )
    {
        this.requestID = requestID;
    }


    /**
     * Gets the processing type of the request
     *
     * @return
     *      the processing type of the request
     */
    public Processing getProcessing()
    {
        return processing;
    }


    /**
     * Sets the processing type of the request
     *
     * @param processing
     *      the processing type to set
     */
    public void setProcessing( Processing processing )
    {
        this.processing = processing;
    }


    /**
     * Gets the on error handling type of the request
     *
     * @return
     *      the on error handling type of the request
     */
    public OnError getOnError()
    {
        return onError;
    }


    /**
     * Sets the on error handling type of the request
     *
     * @param onError
     *      the on error handling type to set
     */
    public void setOnError( OnError onError )
    {
        this.onError = onError;
    }


    /**
     * Gets the reponse order type of the request
     *
     * @return
     *      the reponse order type of the request
     */
    public ResponseOrder getResponseOrder()
    {
        return responseOrder;
    }


    /**
     * Sets the reponse order type of the request
     *
     * @param responseOrder
     *      the reponse order type to set
     */
    public void setResponseOrder( ResponseOrder responseOrder )
    {
        this.responseOrder = responseOrder;
    }


    /**
     * Gets the List of all the requests in the Batch Request
     *
     * @return
     *      the List of all the requests in the Batch Request
     */
    public List getRequests()
    {
        return requests;
    }


    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString()
    {
        StringBuffer sb = new StringBuffer();

        sb.append( "[" );
        sb.append( "processing: " + processing );
        sb.append( " - " );
        sb.append( "onError: " + onError );
        sb.append( " - " );
        sb.append( "responseOrder: " + responseOrder );
        sb.append( "]" );

        return sb.toString();
    }
}
