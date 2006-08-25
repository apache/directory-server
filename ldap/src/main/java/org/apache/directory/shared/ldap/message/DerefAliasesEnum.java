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

import org.apache.directory.shared.ldap.util.ValuedEnum;


/**
 * Type-safe derefAliases search parameter enumeration which determines the mode
 * of alias handling. Note that the names of these ValuedEnums correspond to the
 * value for the java.naming.ldap.derefAliases JNDI LDAP specific property.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Revision$
 */
public class DerefAliasesEnum extends ValuedEnum
{
    static final long serialVersionUID = 1501231261415745531L;

    /** java.naming.ldap.derefAliases JNDI property */
    public static final String JNDI_PROP = "java.naming.ldap.derefAliases";

    /** Alias handling mode name that treats aliases like entries */
    public static final String NEVERDEREFALIASES_NAME = "never";

    /** Alias handling mode name that dereferences only when searching */
    public static final String DEREFINSEARCHING_NAME = "searching";

    /** Alias handling mode name that dereferences only in finding the base */
    public static final String DEREFFINDINGBASEOBJ_NAME = "finding";

    /** Alias handling mode name that dereferences always */
    public static final String DEREFALWAYS_NAME = "always";

    /** Alias handling mode value that treats aliases like entries */
    public static final int NEVERDEREFALIASES_VAL = 0;

    /** Alias handling mode value that dereferences only when searching */
    public static final int DEREFINSEARCHING_VAL = 1;

    /** Alias handling mode value that dereferences only in finding the base */
    public static final int DEREFFINDINGBASEOBJ_VAL = 2;

    /** Alias handling mode value that dereferences always */
    public static final int DEREFALWAYS_VAL = 3;

    /** Alias handling mode that treats aliases like entries */
    public static final DerefAliasesEnum NEVERDEREFALIASES = new DerefAliasesEnum( NEVERDEREFALIASES_NAME,
        NEVERDEREFALIASES_VAL );

    /** Alias handling mode that dereferences only when searching */
    public static final DerefAliasesEnum DEREFINSEARCHING = new DerefAliasesEnum( DEREFINSEARCHING_NAME,
        DEREFINSEARCHING_VAL );

    /** Alias handling mode that dereferences only in finding the base */
    public static final DerefAliasesEnum DEREFFINDINGBASEOBJ = new DerefAliasesEnum( DEREFFINDINGBASEOBJ_NAME,
        DEREFFINDINGBASEOBJ_VAL );

    /** Alias handling mode that dereferences always */
    public static final DerefAliasesEnum DEREFALWAYS = new DerefAliasesEnum( DEREFALWAYS_NAME, DEREFALWAYS_VAL );


    /**
     * Private construct so no other instances can be created other than the
     * public static constants in this class.
     * 
     * @param a_name
     *            a string name for the enumeration value.
     * @param a_value
     *            the integer value of the enumeration.
     */
    private DerefAliasesEnum(final String a_name, final int a_value)
    {
        super( a_name, a_value );
    }


    /**
     * Checks to see if we dereference while searching and finding the base.
     * 
     * @return true if value is DEREFALWAYS_VAL, false otherwise
     */
    public boolean derefAlways()
    {
        return getValue() == DEREFALWAYS_VAL;
    }


    /**
     * Checks to see if we never dereference aliases.
     * 
     * @return true if value is NEVERDEREFALIASES_VAL, false otherwise
     */
    public boolean neverDeref()
    {
        return getValue() == NEVERDEREFALIASES_VAL;
    }


    /**
     * Checks to see if we dereference while searching.
     * 
     * @return true if value is DEREFALWAYS_VAL, or DEREFINSEARCHING_VAL, and
     *         false otherwise.
     */
    public boolean derefInSearching()
    {
        switch ( getValue() )
        {
            case ( DEREFALWAYS_VAL ):
                return true;
            case ( DEREFFINDINGBASEOBJ_VAL ):
                return false;
            case ( DEREFINSEARCHING_VAL ):
                return true;
            case ( NEVERDEREFALIASES_VAL ):
                return false;
            default:
                throw new IllegalArgumentException( "Class has bug: check for valid enumeration values" );
        }
    }


    /**
     * Checks to see if we dereference while finding the base.
     * 
     * @return true if value is DEREFALWAYS_VAL, or DEREFFINDINGBASEOBJ_VAL, and
     *         false otherwise.
     */
    public boolean derefFindingBase()
    {
        switch ( getValue() )
        {
            case ( DEREFALWAYS_VAL ):
                return true;
            case ( DEREFFINDINGBASEOBJ_VAL ):
                return true;
            case ( DEREFINSEARCHING_VAL ):
                return false;
            case ( NEVERDEREFALIASES_VAL ):
                return false;
            default:
                throw new IllegalArgumentException( "Class has bug: check for valid enumeration values" );
        }
    }


    /**
     * Gets the enumeration for a enumeration name which also happens to be the
     * value of the java.naming.ldap.derefAliases LDAP proovider property.
     * 
     * @param a_name
     *            the value for the java.naming.ldap.derefAliases or a name of
     *            an enum value.
     * @return the enumeration for a name
     */
    public static DerefAliasesEnum getEnum( String a_name )
    {
        if ( null == a_name || a_name.equalsIgnoreCase( DEREFALWAYS_NAME ) )
        {
            return DEREFALWAYS;
        }
        else if ( a_name.equalsIgnoreCase( DEREFFINDINGBASEOBJ_NAME ) )
        {
            return DEREFFINDINGBASEOBJ;
        }
        else if ( a_name.equalsIgnoreCase( DEREFINSEARCHING_NAME ) )
        {
            return DEREFINSEARCHING;
        }
        else if ( a_name.equalsIgnoreCase( NEVERDEREFALIASES_NAME ) )
        {
            return NEVERDEREFALIASES;
        }

        throw new IllegalArgumentException( "Unrecognized JNDI environment property " + JNDI_PROP + " value: " + a_name );
    }


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
        return getEnum( ( String ) env.get( JNDI_PROP ) );
    }
}
