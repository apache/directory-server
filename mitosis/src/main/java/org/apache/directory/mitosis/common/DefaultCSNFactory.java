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
package org.apache.directory.mitosis.common;


/**
 * A default {@link CSNFactory} implementation.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class DefaultCSNFactory implements CSNFactory
{
    private static int operationSequence;


    public DefaultCSNFactory()
    {
    }


    /**
     * Returns a new {@link CSN}.
     * Generated CSN can be duplicate if user generates CSNs more than 2G 
     * times a milliseconds.
     * 
     * @param replicaId Replica ID.  ReplicaID must be 1-8 digit alphanumeric
     *        string.
     */
    public synchronized CSN newInstance( String replicaId )
    {
        long newTimestamp = System.currentTimeMillis();
        
        if ( operationSequence == Integer.MAX_VALUE )
        {
            // Roll over when reaching the limit.
            operationSequence = 0;
        }

        CSN newCSN = new DefaultCSN( newTimestamp, replicaId, operationSequence++ );
        return newCSN;
    }
}
