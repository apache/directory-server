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
package org.apache.directory.shared.ldap.constants;


/**
 * An enumeration that represents the level of authentication.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public enum AuthenticationLevel
{
    /**
     * No authentication (anonymous access)
     */
    NONE( 0, "none" ),

    /**
     * Simple authentication (bound with plain-text credentials)
     */
    SIMPLE( 1, "simple" ),

    /**
     * Strong authentication (bound with encrypted cerdentials)
     */
    STRONG( 2, "string" );
    
    private int level;
    
    private final String name;

    private AuthenticationLevel( int level, String name )
    {
        this.level = level;
        this.name = name;
    }

    /**
     * Returns the integer value of this level (greater value, stronger level).
     */
    public int getLevel()
    {
        return level;
    }


    /**
     * Returns the name of this level.
     */
    public String getName()
    {
        return name;
    }


    public String toString()
    {
        return name;
    }
}
