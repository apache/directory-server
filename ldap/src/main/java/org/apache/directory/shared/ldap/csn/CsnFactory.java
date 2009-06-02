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
package org.apache.directory.shared.ldap.csn;


/**
 * Generates a new {@link Csn}.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class CsnFactory
{
    /** The last timestamp */
    private static volatile long lastTimestamp;
    
    /** The integer used to disambiguate CSN generated at the same time */
    private static volatile int changeCount;


    public CsnFactory()
    {
        changeCount = 0;
    }


    /**
     * Returns a new {@link Csn}.
     * Generated CSN can be duplicate if user generates CSNs more than 2G 
     * times a milliseconds.
     * 
     * @param replicaId Replica ID.  ReplicaID must be 1-3 digit alphanumeric
     *        value (from 000 to fff).
     */
    public Csn newInstance( int replicaId )
    {
        long newTimestamp = System.currentTimeMillis();
        
        // We will be able to generate 2 147 483 647 CSNs each 10 ms max
        if ( lastTimestamp == newTimestamp )
        {
            changeCount ++;
        }
        else
        {
            lastTimestamp = newTimestamp;
            changeCount = 0;
        }

        return new Csn( lastTimestamp, changeCount, replicaId, 0 );
    }


    /**
     * Returns a new {@link Csn} created from the given values.
     * 
     * This method is <b>not</b> to be used except for test purposes.
     * 
     * @param timestamp The timestamp to use
     * @param replicaId Replica ID.  ReplicaID must be 1-3 digit value
     * @param changeCount The change count to use
     */
    public Csn newInstance( long timestamp, int replicaId, int changeCount )
    {
        return new Csn( timestamp, changeCount, replicaId, 0 );
    }
    
    
    /**
     * Generates a CSN used to purge data. Its replicaID is not associated
     * to a server. 
     * 
     * @param expirationDate The time up to the first CSN we want to keep 
     */
    public Csn newInstance( long expirationDate )
    {
        return new Csn( expirationDate, Integer.MAX_VALUE, -1, Integer.MAX_VALUE );
    }
}
