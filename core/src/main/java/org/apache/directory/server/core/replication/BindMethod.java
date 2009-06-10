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
package org.apache.directory.server.core.replication;

/**
 * An enum used to store the Bind Methods : SIMPLE or SASL 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 *
 */
public enum BindMethod
{
    /** SIMPLE bind */
    SIMPLE( "simple" ),
    
    /** SASL bind */
    SASL( "sasl" ),
    
    /** Unkwnon bind method */ 
    UNKWNOWN( "" );
    
    /** A storage for the String representation of the BindMethod */
    private String bindMethod;
    
    
    /**
     * Build the Enum's instances.
     */
    private BindMethod( String bindMethod )
    {
        this.bindMethod = bindMethod;
    }
    
    
    public static BindMethod getInstance( String bindMethod )
    {
        if ( SIMPLE.bindMethod.equalsIgnoreCase( bindMethod ) )
        {
            return SIMPLE;
        }
        
        if ( SASL.bindMethod.equalsIgnoreCase( bindMethod ) )
        {
            return SASL;
        }
        
        return UNKWNOWN;
    }
}
