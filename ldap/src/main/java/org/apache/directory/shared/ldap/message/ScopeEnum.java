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
package org.apache.directory.shared.ldap.message;


import javax.naming.directory.SearchControls;

/**
 * Type-safe scope parameter enumeration.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Revision$
 */
public enum ScopeEnum
{
    /** Search scope parameter value for base object search */
    BASE_OBJECT( SearchControls.OBJECT_SCOPE ),

    /** Search scope parameter value for single level search */
    SINGLE_LEVEL( SearchControls.ONELEVEL_SCOPE ),

    /** Search scope parameter value for whole subtree level search */
    WHOLE_SUBTREE( SearchControls.SUBTREE_SCOPE );

    /** Stores the integer value of each element of the enumeration */
    private int value;

    /**
     * Private construct so no other instances can be created other than the
     * public static constants in this class.
     * 
     * @param value
     *            the integer value of the enumeration.
     */
    private ScopeEnum( int value )
    {
        this.value = value;
    }

    /**
     * Gets the LdapValue for the scope enumeration as opposed to the JNDI value
     * which is returned using getValue().
     * 
     * @return the LDAP enumeration value for the scope parameter on a search
     *         request.
     */
    public int getValue()
    {
        return value;
    }

    /**
     * Gets the type safe enumeration constant corresponding to a SearchControls
     * scope value.
     * 
     * @param controls
     *            the SearchControls whose scope value we convert to enum
     * @return the SopeEnum for the scope int value
     */
    public static ScopeEnum getScope( SearchControls controls )
    {
        switch ( controls.getSearchScope() )
        {
            case ( SearchControls.OBJECT_SCOPE  ):
                return BASE_OBJECT;
            
            case ( SearchControls.ONELEVEL_SCOPE  ):
                return SINGLE_LEVEL;
            
            case ( SearchControls.SUBTREE_SCOPE  ):
                return WHOLE_SUBTREE;
            
            default:
                throw new IllegalArgumentException( "Unrecognized search scope in SearchControls: "
                    + controls.getSearchScope() );
        }
    }

    /**
     * Gets the enum corresponding to the given integer value. 
     *
     * @param value The integer we want the corresponding enum
     * @return The enumeration element associated with the given integer
     */
    public static ScopeEnum getScope( int value )
    {
        switch ( value )
        {
            case 0: return BASE_OBJECT;
            case 1: return SINGLE_LEVEL;
            case 2: return WHOLE_SUBTREE;
            default : return BASE_OBJECT;
        }
    }
}
