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
public final class LastRequestType implements Comparable<LastRequestType>
{
    /**
     * Constant for the "none" last request type.
     */
    public static final LastRequestType NONE = new LastRequestType( 0, "none" );

    /**
     * Constant for the "time of initial ticket" last request type.
     */
    public static final LastRequestType TIME_OF_INITIAL_TGT = new LastRequestType( 1, "time of initial ticket" );

    /**
     * Constant for the "time of initial request" last request type.
     */
    public static final LastRequestType TIME_OF_INITIAL_REQ = new LastRequestType( 2, "time of initial request" );

    /**
     * Constant for the "time of newest ticket" last request type.
     */
    public static final LastRequestType TIME_OF_NEWEST_TGT = new LastRequestType( 3, "time of newest ticket" );

    /**
     * Constant for the "time of last renewal" last request type.
     */
    public static final LastRequestType TIME_OF_LAST_RENEWAL = new LastRequestType( 4, "time of last renewal" );

    /**
     * Constant for the "time of last request" last request type.
     */
    public static final LastRequestType TIME_OF_LAST_REQ = new LastRequestType( 5, "time of last request" );

    /**
     * Constant for the "time of password expiration" last request type.
     */
    public static final LastRequestType TIME_OF_PASSWORD_EXP = new LastRequestType( 6, "time of password expiration" );

    /**
     * Array for building a List of VALUES.
     */
    private static final LastRequestType[] values =
        { NONE, TIME_OF_INITIAL_TGT, TIME_OF_INITIAL_REQ, TIME_OF_NEWEST_TGT, TIME_OF_LAST_RENEWAL, TIME_OF_LAST_REQ,
            TIME_OF_PASSWORD_EXP };

    /**
     * A List of all the last request type constants.
     */
    public static final List<LastRequestType> VALUES = Collections.unmodifiableList( Arrays.asList( values ) );

    /**
     * The name of the checksum type.
     */
    private final String name;

    /**
     * The value/code for the checksum type.
     */
    private final int ordinal;


    /**
     * Private constructor prevents construction outside of this class.
     */
    private LastRequestType( int ordinal, String name )
    {
        this.ordinal = ordinal;
        this.name = name;
    }


    /**
     * Returns the last request type when specified by its ordinal.
     *
     * @param type
     * @return The last request type.
     */
    public static LastRequestType getTypeByOrdinal( int type )
    {
        for ( int ii = 0; ii < values.length; ii++ )
        {
            if ( values[ii].ordinal == type )
            {
                return values[ii];
            }
        }

        return NONE;
    }


    /**
     * Returns the number associated with this last request type.
     *
     * @return The last request type ordinal.
     */
    public int getOrdinal()
    {
        return ordinal;
    }


    public int compareTo( LastRequestType that )
    {
        return ordinal - that.ordinal;
    }


    public String toString()
    {
        return name + " (" + ordinal + ")";
    }
}
