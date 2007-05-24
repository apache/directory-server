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
public final class TransitedEncodingType implements Comparable
{
    /**
     * Constant for the "null" transited encoding type.
     */
    public static final TransitedEncodingType NULL = new TransitedEncodingType( 0, "null" );

    /**
     * Constant for the "Domain X500 compress" transited encoding type.
     */
    public static final TransitedEncodingType DOMAIN_X500_COMPRESS = new TransitedEncodingType( 1,
        "Domain X500 compress" );

    /**
     * Array for building a List of VALUES.
     */
    private static final TransitedEncodingType[] values =
        { NULL, DOMAIN_X500_COMPRESS };

    /**
     * A List of all the transited encoding type constants.
     */
    public static final List VALUES = Collections.unmodifiableList( Arrays.asList( values ) );

    /**
     * The name of the transited encoding type.
     */
    private final String name;

    /**
     * The value/code for the transited encoding type.
     */
    private final int ordinal;


    /**
     * Private constructor prevents construction outside of this class.
     */
    private TransitedEncodingType( int ordinal, String name )
    {
        this.ordinal = ordinal;
        this.name = name;
    }


    /**
     * Returns the transited encoding type when specified by its ordinal.
     *
     * @param type
     * @return The transited encoding type.
     */
    public static TransitedEncodingType getTypeByOrdinal( int type )
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


    /**
     * Returns the number associated with this transited encoding type.
     *
     * @return The transited encoding type ordinal.
     */
    public int getOrdinal()
    {
        return ordinal;
    }


    public int compareTo( Object that )
    {
        return ordinal - ( ( TransitedEncodingType ) that ).ordinal;
    }


    public String toString()
    {
        return name + " (" + ordinal + ")";
    }
}
