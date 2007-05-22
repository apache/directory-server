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
package org.apache.directory.server.kerberos.shared.crypto.encryption;


import java.util.Arrays;
import java.util.Collections;
import java.util.List;


/**
 * A type-safe enumeration of Kerberos cipher types.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public final class CipherType implements Comparable
{
    /**
     * The null cipher type.
     */
    public static final CipherType NULL = new CipherType( "NULL" );

    /**
     * The DES cipher type.
     */
    public static final CipherType DES = new CipherType( "DES" );

    /**
     * The Triple-DES cipher type.
     */
    public static final CipherType DES3 = new CipherType( "DESede" );

    /**
     * The AES (both 128 and 256) cipher type.
     */
    public static final CipherType AES = new CipherType( "AES" );

    /**
     * The ARCFOUR cipher type.
     */
    public static final CipherType ARCFOUR = new CipherType( "ARCFOUR" );

    /**
     * Array for building a List of VALUES.
     */
    private static final CipherType[] values =
        { NULL, DES, DES3, AES, ARCFOUR };

    /**
     * A List of all the cipher type constants.
     */
    public static final List VALUES = Collections.unmodifiableList( Arrays.asList( values ) );

    private final String name;
    private static int nextOrdinal = 0;
    private final int ordinal = nextOrdinal++;


    /**
     * Private constructor prevents construction outside of this class.
     */
    private CipherType( String name )
    {
        this.name = name;
    }


    /**
     * Returns the cipher type when specified by its ordinal.
     *
     * @param type
     * @return The cipher type.
     */
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
