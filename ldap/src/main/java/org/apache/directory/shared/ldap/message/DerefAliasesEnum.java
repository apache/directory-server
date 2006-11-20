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


import java.util.Map;


/**
 * Type-safe derefAliases search parameter enumeration which determines the mode
 * of alias handling. Note that the names of these ValuedEnums correspond to the
 * value for the java.naming.ldap.derefAliases JNDI LDAP specific property.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Revision$
 */
public enum DerefAliasesEnum
{
    /** Alias handling mode value that treats aliases like entries */
    NEVER_DEREF_ALIASES(0),

    /** Alias handling mode value that dereferences only when searching */
    DEREF_IN_SEARCHING(1),

    /** Alias handling mode value that dereferences only in finding the base */
    DEREF_FINDING_BASE_OBJ(2),

    /** Alias handling mode value that dereferences always */
    DEREF_ALWAYS(3);


    /** Stores the integer value of each element of the enumeration */
    private int value;
    
    /**
     * Private constructor so no other instances can be created other than the
     * public static constants in this class.
     * 
     * @param value the integer value of the enumeration.
     */
    private DerefAliasesEnum( int value )
    {
       this.value = value;
    }

    
    /**
     * @return The value associated with the current element.
     */
    public int getValue()
    {
        return value;
    }

    /** java.naming.ldap.derefAliases JNDI property */
    public static final String JNDI_DEREF_ALIAS_PROP = "java.naming.ldap.derefAliases";

    /** Alias handling mode name that treats aliases like entries */
    public static final String NEVER_DEREF_ALIASES_NAME = "never";

    /** Alias handling mode name that dereferences only when searching */
    public static final String DEREF_IN_SEARCHING_NAME = "searching";

    /** Alias handling mode name that dereferences only in finding the base */
    public static final String DEREF_FINDING_BASE_OBJ_NAME = "finding";

    /** Alias handling mode name that dereferences always */
    public static final String DEREF_ALWAYS_NAME = "always";

    /**
     * Gets the enumeration from by extracting the value for the JNDI LDAP
     * specific environment property, java.naming.ldap.derefAliases, from the
     * environment.
     * 
     * @param env
     *            the JNDI environment with a potential value for the
     *            java.naming.ldap.derefAliases property
     * @return the enumeration for the environment
     */
    public static DerefAliasesEnum getEnum( Map env )
    {
        String property = ( String ) env.get( JNDI_DEREF_ALIAS_PROP );
        
        if ( null == property )
        {
            return DEREF_ALWAYS;
        }
        
        DerefAliasesEnum result = valueOf( property );
        return ( result == null ? DEREF_ALWAYS : result );
    }
    
    /**
     * Checks to see if we dereference while searching and finding the base.
     * 
     * @return true if value is DEREF_ALWAYS, false otherwise
     */
    public boolean isDerefAlways()
    {
        return this == DEREF_ALWAYS;
    }


    /**
     * Checks to see if we never dereference aliases.
     * 
     * @return true if value is NEVER_DEREF_ALIASES, false otherwise
     */
    public boolean isNeverDeref()
    {
        return this == NEVER_DEREF_ALIASES;
    }


    /**
     * Checks to see if we dereference while searching.
     * 
     * @return true if value is DEREF_ALWAYS_VAL, or DEREF_IN_SEARCHING, and
     *         false otherwise.
     */
    public boolean isDerefInSearching()
    {
        switch ( this )
        {
            case DEREF_ALWAYS :
                return true;
            
            case DEREF_FINDING_BASE_OBJ :
                return false;
            
            case DEREF_IN_SEARCHING :
                return true;
            
            case NEVER_DEREF_ALIASES :
                return false;
            
            default:
                throw new IllegalArgumentException( "Class has bug: check for valid enumeration values" );
        }
    }


    /**
     * Checks to see if we dereference while finding the base.
     * 
     * @return true if value is DEREF_ALWAYS, or DEREF_FINDING_BASE_OBJ, and
     *         false otherwise.
     */
    public boolean isDerefFindingBase()
    {
        switch ( this )
        {
            case DEREF_ALWAYS :
                return true;
            
            case DEREF_FINDING_BASE_OBJ :
                return true;
            
            case DEREF_IN_SEARCHING :
                return false;
            
            case NEVER_DEREF_ALIASES :
                return false;
            
            default:
                throw new IllegalArgumentException( "Class has bug: check for valid enumeration values" );
        }
    }
}
