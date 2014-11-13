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

import org.apache.directory.api.ldap.model.constants.Loggers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;


/**
 * A thread used to ping the provider o check if they are alive or not.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class PingerThread extends Thread
{
    /** Logger for the replication consumer */
    private static final Logger CONSUMER_LOG = LoggerFactory.getLogger( Loggers.CONSUMER_LOG.getName() );

    /** The list of consumers we want to check */
    private Queue<ReplicationConsumer> consumers = new ConcurrentLinkedQueue<ReplicationConsumer>();

    /** A flag to stop the pinger */
    private boolean stop = false;

    /** the time interval before this thread pings each replication provider. Default value is 5 seconds */
    private long sleepTime = 5000;

    /**
     * Create a new instance of this thread.
     * 
     * @param sleepSec the number of seconds pinger thread should sleep before pinging the providers
     */
    public PingerThread( int sleepSec )
    {
        if( sleepSec > 0 )
        {
            sleepTime = sleepSec * 1000;
        }
        
        CONSUMER_LOG.info( "Configured pinger thread to sleep for {} seconds", ( sleepTime / 1000 ) );
        
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
                MDC.put( "Replica", "Pinger" );

                CONSUMER_LOG.debug( "Starting the provider's pinger" );
            }

            while ( !stop )
            {
                for ( ReplicationConsumer consumer : consumers )
                {
                    consumer.ping();
                }

                Thread.sleep( sleepTime );
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