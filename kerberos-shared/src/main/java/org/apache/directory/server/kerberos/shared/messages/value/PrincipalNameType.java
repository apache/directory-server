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
package org.apache.directory.server.kerberos.shared.messages.value;


import java.util.Arrays;
import java.util.Collections;
import java.util.List;


/**
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public final class PrincipalNameType implements Comparable
{
    /**
     * Constant for the "unknown name type" principal name type.
     */
    public static final PrincipalNameType KRB_NT_UNKNOWN = new PrincipalNameType( 0, "unknown name type" );

    /**
     * Constant for the "user principal name type" principal name type.
     */
    public static final PrincipalNameType KRB_NT_PRINCIPAL = new PrincipalNameType( 1, "user principal name type" );

    /**
     * Constant for the "service and other unique instance (krbtgt) name type" principal name type.
     */
    public static final PrincipalNameType KRB_NT_SRV_INST = new PrincipalNameType( 2,
        "service and other unique instance (krbtgt) name type" );

    /**
     * Constant for the "service with host name as instance (telnet, rcommands)" principal name type.
     */
    public static final PrincipalNameType KRB_NT_SRV_HST = new PrincipalNameType( 3,
        "service with host name as instance (telnet, rcommands)" );

    /**
     * Constant for the "service with host name as instance (telnet, rcommands) name type" principal name type.
     */
    public static final PrincipalNameType KRB_NT_SRV_XHST = new PrincipalNameType( 4,
        "service with host name as instance (telnet, rcommands) name type" );

    /**
     * Constant for the "unique ID name type" principal name type.
     */
    public static final PrincipalNameType KRB_NT_UID = new PrincipalNameType( 5, "unique ID name type" );

    /**
     * Constant for the "nt x500 principal; encoded X.509 Distinguished name [RFC 2253]" principal name type.
     */
    public static final PrincipalNameType KRB_NT_X500_PRINCIPAL = new PrincipalNameType( 6,
        "nt x500 principal; encoded X.509 Distinguished name [RFC 2253]" );

    /**
     * Array for building a List of VALUES.
     */
    private static final PrincipalNameType[] values =
        { KRB_NT_UNKNOWN, KRB_NT_PRINCIPAL, KRB_NT_SRV_INST, KRB_NT_SRV_HST, KRB_NT_SRV_XHST, KRB_NT_UID,
            KRB_NT_X500_PRINCIPAL };

    /**
     * A List of all the principal name type constants.
     */
    public static final List VALUES = Collections.unmodifiableList( Arrays.asList( values ) );

    /**
     * The name of the principal name type.
     */
    private final String name;

    /**
     * The value/code for the principal name type.
     */
    private final int ordinal;


    /**
     * Private constructor prevents construction outside of this class.
     */
    private PrincipalNameType( int ordinal, String name )
    {
        this.ordinal = ordinal;
        this.name = name;
    }


    /**
     * Returns the principal name type when specified by its ordinal.
     *
     * @param type
     * @return The principal name type.
     */
    public static PrincipalNameType getTypeByOrdinal( int type )
    {
        for ( int ii = 0; ii < values.length; ii++ )
        {
            if ( values[ii].ordinal == type )
            {
                return values[ii];
            }
        }

        return KRB_NT_UNKNOWN;
    }


    /**
     * Returns the number associated with this principal name type.
     *
     * @return The principal name type ordinal.
     */
    public int getOrdinal()
    {
        return ordinal;
    }


    public int compareTo( Object that )
    {
        return ordinal - ( ( PrincipalNameType ) that ).ordinal;
    }


    public String toString()
    {
        return name + " (" + ordinal + ")";
    }
}
