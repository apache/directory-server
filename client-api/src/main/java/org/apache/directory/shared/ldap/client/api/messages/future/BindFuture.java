/*
 * Licensed to the Apache Software Foundation (ASF) under one
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
package org.apache.directory.shared.ldap.client.api.messages.future;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.apache.directory.shared.ldap.client.api.messages.BindResponse;

/**
 * A Future to manage BindRequests
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class BindFuture implements Future<BindResponse>
{
    /** The queue where BindResponse are stored */
    private BlockingQueue<BindResponse> bindResponseQueue;
    
   
    /**
     * 
     * Creates a new instance of BindFuture.
     *
     * @param bindResponseQueue The associated BindResponse queue
     */
    public BindFuture( BlockingQueue<BindResponse> bindResponseQueue )
    {
        this.bindResponseQueue = bindResponseQueue;
    }
    
    
    /**
     * {@inheritDoc}
     */
    public boolean cancel( boolean mayInterruptIfRunning )
    {
        throw new RuntimeException( "Not Yet Implemented" );
    }

    
    /**
     * Get the BindResponse, blocking until one is received.
     * 
     * @return The BindResponse
     */
    public BindResponse get() throws InterruptedException, ExecutionException
    {
        return bindResponseQueue.poll();
    }

    
    /**
     * Get the BindResponse, blocking until one is received, or until the
     * given timeout is reached.
     * 
     * @param timeout Number of TimeUnit to wait
     * @param unit The TimeUnit
     * @return The BindResponse The BindResponse found
     */
    public BindResponse get( long timeout, TimeUnit unit ) throws InterruptedException, ExecutionException,
        TimeoutException
    {
        return bindResponseQueue.poll( timeout, unit );        
    }

    
    /**
     * {@inheritDoc}
     */
    public boolean isCancelled()
    {
        throw new RuntimeException( "Not Yet Implemented" );
    }


    /**
     * {@inheritDoc}
     */
    public boolean isDone()
    {
        throw new RuntimeException( "Not Yet Implemented" );
    }
}
