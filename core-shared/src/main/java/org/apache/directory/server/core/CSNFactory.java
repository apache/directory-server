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
package org.apache.directory.server.core;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;


/**
 * Generates a new {@link CSN}.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class CSNFactory
{
    /** The last timestamp */
    private static volatile long lastTimestamp;
    
    /** The integer used to disambiguate CSN generated at the same time */
    private static volatile int changeCount;
    
    /** A lock to protect CSN creations against concurrent access */
    private Lock lock = new ReentrantLock();


    public CSNFactory()
    {
    }


    /**
     * Returns a new {@link CSN}.
     * Generated CSN can be duplicate if user generates CSNs more than 2G 
     * times a milliseconds.
     * 
     * @param replicaId Replica ID. ReplicaID must be 1-3 digit
     *        string.
     */
    public CSN newInstance( int replicaId )
    {
        long newTimestamp = System.currentTimeMillis();
        
        // Here, we enter a protected zone
        lock.lock();
        
        try
        {
            // We will be able to generate 2 147 483 647 CSNs every 10 ms max
            if ( lastTimestamp == newTimestamp )
            {
                changeCount ++;
            }
            else
            {
                lastTimestamp = newTimestamp;
                changeCount = 0;
            }

            return new CSN( lastTimestamp, changeCount, replicaId, 0 );
        }
        finally
        {
            lock.unlock();
        }
    }


    /**
     * Generates a CSN used to purge data. Its replicaID is not associated
     * to a server. 
     * 
     * @param expirationDate The time up to the first CSN we want to keep 
     */
    public CSN createPurgeCSN( long expirationDate )
    {
        return new CSN( expirationDate, Integer.MAX_VALUE, 999, Integer.MAX_VALUE );
    }
}
