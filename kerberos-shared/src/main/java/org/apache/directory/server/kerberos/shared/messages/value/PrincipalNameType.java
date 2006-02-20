/*
 *   Copyright 2004 The Apache Software Foundation
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 */
package org.apache.directory.server.kerberos.shared.messages.value;


import java.util.Arrays;
import java.util.Collections;
import java.util.List;


public final class PrincipalNameType implements Comparable
{
    /**
     * Enumeration elements are constructed once upon class loading.
     * Order of appearance here determines the order of compareTo.
     */
    public static final PrincipalNameType KRB_NT_UNKNOWN = new PrincipalNameType( 0, "unknown name type" );
    public static final PrincipalNameType KRB_NT_PRINCIPAL = new PrincipalNameType( 1, "user principal name type" );
    public static final PrincipalNameType KRB_NT_SRV_INST = new PrincipalNameType( 2,
        "service and other unique instance (krbtgt) name type" );
    public static final PrincipalNameType KRB_NT_SRV_HST = new PrincipalNameType( 3,
        "service with host name as instance (telnet, rcommands)" );
    public static final PrincipalNameType KRB_NT_SRV_XHST = new PrincipalNameType( 4,
        "service with host name as instance (telnet, rcommands) name type" );
    public static final PrincipalNameType KRB_NT_UID = new PrincipalNameType( 5, "unique ID name type" );
    public static final PrincipalNameType KRB_NT_X500_PRINCIPAL = new PrincipalNameType( 6,
        "nt x500 principal; encoded X.509 Distinguished name [RFC 2253]" );


    public String toString()
    {
        return name + " (" + ordinal + ")";
    }


    public int compareTo( Object that )
    {
        return ordinal - ( ( PrincipalNameType ) that ).ordinal;
    }


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


    public int getOrdinal()
    {
        return ordinal;
    }

    /// PRIVATE /////
    private final String name;
    private final int ordinal;


    /**
     * Private constructor prevents construction outside of this class.
     */
    private PrincipalNameType(int ordinal, String name)
    {
        this.ordinal = ordinal;
        this.name = name;
    }

    /**
     * These two lines are all that's necessary to export a List of VALUES.
     */
    private static final PrincipalNameType[] values =
        { KRB_NT_UNKNOWN, KRB_NT_PRINCIPAL, KRB_NT_SRV_INST, KRB_NT_SRV_HST, KRB_NT_SRV_XHST, KRB_NT_UID,
            KRB_NT_X500_PRINCIPAL };
    // VALUES needs to be located here, otherwise illegal forward reference
    public static final List VALUES = Collections.unmodifiableList( Arrays.asList( values ) );
}
