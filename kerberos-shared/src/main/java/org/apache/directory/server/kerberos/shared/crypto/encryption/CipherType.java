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
package org.apache.directory.server.kerberos.shared.crypto.encryption;


import java.util.Arrays;
import java.util.Collections;
import java.util.List;


public final class CipherType implements Comparable
{
    /**
     * Enumeration elements are constructed once upon class loading.
     * Order of appearance here determines the order of compareTo.
     */
    public static final CipherType NULL = new CipherType( "NULL" );
    public static final CipherType DES = new CipherType( "DES" );
    public static final CipherType DES3 = new CipherType( "DES3" );
    public static final CipherType AES128 = new CipherType( "AES128" );

    /**
     * These two lines are all that's necessary to export a List of VALUES.
     */
    private static final CipherType[] values =
        { NULL, DES, DES3, AES128 };
    // VALUES needs to be located here, otherwise illegal forward reference
    public static final List VALUES = Collections.unmodifiableList( Arrays.asList( values ) );

    private final String name;
    private static int nextOrdinal = 0;
    private final int ordinal = nextOrdinal++;


    /**
     * Private constructor prevents construction outside of this class.
     */
    private CipherType(String name)
    {
        this.name = name;
    }


    public CipherType getTypeByOrdinal( int type )
    {
        for ( int ii = 0; ii < values.length; ii++ )
        {
            if ( values[ii].ordinal == type )
            {
                return values[ii];
            }
        }

        return NULL;
    }


    public int compareTo( Object that )
    {
        return ordinal - ( ( CipherType ) that ).ordinal;
    }


    public String toString()
    {
        return name;
    }
}
