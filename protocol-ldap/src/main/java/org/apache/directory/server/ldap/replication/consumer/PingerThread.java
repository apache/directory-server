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
package org.apache.directory.server.ldap.replication.consumer;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.apache.log4j.NDC;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A thread used to ping the provider o check if they are alive or not.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class PingerThread extends Thread
{
    /** Logger for the replication consumer */
    private static final Logger CONSUMER_LOG = LoggerFactory.getLogger( "CONSUMER_LOG" );

    /** The list of consumers we want to check */
    private Queue<ReplicationConsumer> consumers = new ConcurrentLinkedQueue<ReplicationConsumer>();
    
    /** A flag to stop the pinger */
    private boolean stop = false;

    /**
     * Create a new instance of this thread.
     */
    public PingerThread()
    {
        setDaemon( true );
    }
    
    
    /**
     * Starts the thread
     */
    public void run()
    {
        try
        {
            if ( CONSUMER_LOG.isDebugEnabled() )
            {
                NDC.pop();
                NDC.push( "Pinger" );
                
                CONSUMER_LOG.debug( "Starting the provider's pinger" );
            }

            while ( !stop )
            {
                for ( ReplicationConsumer consumer : consumers )
                {
                    consumer.ping();
                }

                Thread.sleep( 5000 );
            }
        }
        catch ( InterruptedException ie )
        {
            CONSUMER_LOG.debug( "The pinger has been interrupted" );
        }
    }
    
    
    /**
     * Add a new consumer to ping
     * 
     * @param consumer The consumer we want to ping
     */
    public void addConsumer( ReplicationConsumer consumer )
    {
        if ( !consumers.contains( consumer ) )
        {
            consumers.add( consumer );
        }
    }
    
    
    /**
     * Remove a consumer to ping
     * @param consumer The consumer we want to remove
     */
    public void removeConsumer( ReplicationConsumer consumer )
    {
        consumers.remove( consumer );
    }
    
    
    /**
     * Stops the ping for all the consumers
     */
    public void stopPinging()
    {
        stop = true;
    }
}