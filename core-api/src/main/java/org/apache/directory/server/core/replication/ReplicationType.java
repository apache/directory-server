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
package org.apache.directory.server.core.replication;

/**
 * Describe the type of replication :
 * <li>refreshOnly : replication is done only when requested. This is a 
 * <b>Pull</b> mode</li>
 * <li>refreshAndPersist: replication is done and a listener is created to be 
 * informed whenever some modification occurs on the provider. This is a 
 * <b>Push</b> mode</li>
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev: $, $Date: $
 */
public enum ReplicationType
{
    REFRESH_ONLY( "refreshOnly" ),
    REFRESH_AND_PERSIST( "refreshAndPersist" ),
    UNKNOWN( "" );

    /** The inner String associated with the types */
    private String type;
    
    
    /**
     * private constructor to create the instances.
     */
    private ReplicationType( String type )
    {
        this.type = type;
    }
    
    
    /**
     * Get the ReplicationType associated with the given String.
     * @param type The ReplicationType as a String 
     * @return The associated enum instance
     */
    public static ReplicationType getInstance( String type )
    {
        if ( REFRESH_ONLY.type.equalsIgnoreCase( type ) )
        {
            return REFRESH_ONLY;
        }
        else if ( REFRESH_AND_PERSIST.type.equalsIgnoreCase( type ) )
        {
            return REFRESH_AND_PERSIST;
        }
        else
        {
            return UNKNOWN;
        }
    }
}
