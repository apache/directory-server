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

package org.apache.directory.shared.ldap.client.api.messages.future;


import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.apache.directory.shared.ldap.client.api.messages.AbstractResponseWithResult;
import org.apache.directory.shared.ldap.client.api.messages.Response;


/**
 * A Future implementation used in LdapConnection operations.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class ResponseFuture implements Future<Response>
{
    /** the blocking queue holding LDAP responses */
    private final BlockingQueue<Response> responseQueue;

    /** flag to determine if this future is cancelled */
    private boolean cancelled = false;

    /** an object to indicate a cancelled/abandoned operation */
    // 'poision pill shutdown' refer p. 155-156 Java Concurrency in Practice - Brian Goetz
    private static final Response CANCEL_POISION = new AbstractResponseWithResult(){};


    /**
     * Creates a new instance of ResponseFuture.
     *
     * @param responseQueue a non-null blocking queue
     */
    public ResponseFuture( final BlockingQueue responseQueue )
    {
        if ( responseQueue == null )
        {
            throw new NullPointerException( "response queue cannot be null" );
        }

        this.responseQueue = responseQueue;
    }


    /**
     * {@inheritDoc}
     */
    public boolean cancel( boolean mayInterruptIfRunning )
    {
        if( cancelled )
        {
            return cancelled;
        }
        
        cancelled = true;
        responseQueue.add( CANCEL_POISION );
        
        return cancelled;
    }


    /**
     * {@inheritDoc}
     * @throws InterruptedException if the operation has been cancelled by client
     */
    public Response get() throws InterruptedException, ExecutionException
    {
        Response resp = responseQueue.poll();
        
        if( resp == CANCEL_POISION )
        {
            throw new InterruptedException( "cancelled" );
        }
        
        return resp;
    }


    /**
     * {@inheritDoc}
     * @throws InterruptedException if the operation has been cancelled by client
     */
    public Response get( long timeout, TimeUnit unit ) throws InterruptedException, ExecutionException,
        TimeoutException
    {
        Response resp = responseQueue.poll( timeout, unit );
        
        if( resp == CANCEL_POISION )
        {
            throw new InterruptedException( "cancelled" );
        }
        
        return resp;
    }


    /**
     * {@inheritDoc}
     */
    public boolean isCancelled()
    {
        return cancelled;
    }


    /**
     * This operation is not supported in this implementation of Future
     */
    public boolean isDone()
    {
        throw new UnsupportedOperationException( "Operation not supported" );
    }

}
