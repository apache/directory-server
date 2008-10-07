/*
 *   Licensed to the Apache Software Foundation (ASF) under one
 *   or more contributor license agreements.  See the NOTICE file
 *   distributed with this work for additional information
 *   regarding copyright ownership.  The ASF licenses this file
 *   to you under the Apache License, Version 2.0 (the
 *   "License"); you may not use this file except in compliance
 *   with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing,
 *   software distributed under the License is distributed on an
 *   "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *   KIND, either express or implied.  See the License for the
 *   specific language governing permissions and limitations
 *   under the License.
 *
 */
package org.apache.directory.server.ldap.handlers.controls;

import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;

/**
 * 
 * A container for the Page search cookie. We store multiple informations :
 *  - 
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class PagedSearchCookie
{
    /** The total number of entries already returned */ 
    private int cumulativeSize;
    
    /** The original MessageId */
    private int messageId;
    /**
     * 
     * Creates a new instance of PagedSearchCookie.
     *
     */
    public PagedSearchCookie()
    {
        cumulativeSize = 0;
        messageId = -1;
    }


    /**
     * 
     * Creates a new instance of PagedSearchCookie,
     * deserializing the cookie.
     * 
     * @throws BufferUnderflowException if the buffer is not large enough to 
     * contain correct values
     *
     */
    public PagedSearchCookie( byte[] cookie ) throws BufferUnderflowException
    {
        ByteBuffer bb = ByteBuffer.allocate( cookie.length );
        bb.put( cookie );
        
        cumulativeSize = bb.getInt();
        messageId = bb.getInt();
    }


    /**
     * @return The current number of entries returned since the first request 
     */
    public int getCumulativeSize()
    {
        return cumulativeSize;
    }


    /**
     * Increment the cumulativeSize field with the number of
     * entries returned with the last request
     *
     * @param size
     */
    public void incrementCumulativeSize()
    {
        cumulativeSize ++;
    }
    
    
    /**
     * @return The cookie associated messageId
     */
    public int getMessageId()
    {
        return messageId;
    }


    /**
     * Assign the message ID to this cookie
     *
     * @param messageId The request message ID
     */
    public void setMessageId( int messageId )
    {
        this.messageId = messageId;
    }

    
    /**
     * Serialize the cookie 
     *
     * @return A byte array containing the data returned to the client
     */
    public byte[] serialize()
    {
        ByteBuffer bb = ByteBuffer.allocate( 12 );
        
        bb.putInt( cumulativeSize );
        bb.putInt( messageId );
        
        return bb.array();
    }
}
