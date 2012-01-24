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
package org.apache.directory.shared.kerberos.codec.types;


import org.apache.directory.shared.ldap.model.constants.AuthenticationLevel;


/**
 * The LastRequest types
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public enum LastReqType
{
    /**
     * Constant for the "none" last request type.
     */
    NONE(0, AuthenticationLevel.NONE.toString()),

    /**
     * Constant for the "time of initial ticket" last request type.
     */
    TIME_OF_INITIAL_TGT(1, "time of initial ticket"),

    /**
     * Constant for the "time of initial request" last request type.
     */
    TIME_OF_INITIAL_REQ(2, "time of initial request"),

    /**
     * Constant for the "time of newest ticket" last request type.
     */
    TIME_OF_NEWEST_TGT(3, "time of newest ticket"),

    /**
     * Constant for the "time of last renewal" last request type.
     */
    TIME_OF_LAST_RENEWAL(4, "time of last renewal"),

    /**
     * Constant for the "time of last request" last request type.
     */
    TIME_OF_LAST_REQ(5, "time of last request"),

    /**
     * Constant for the "time of password expiration" last request type.
     */
    TIME_OF_PASSWORD_EXP(6, "time of password expiration");

    /**
     * The name of the checksum type.
     */
    private String name;

    /**
     * The value/code for the checksum type.
     */
    private int value;


    /**
     * Private constructor prevents construction outside of this class.
     */
    private LastReqType( int value, String name )
    {
        this.value = value;
        this.name = name;
    }


    /**
     * Returns the last request type when specified by its ordinal.
     *
     * @param type The numeric type
     * @return The last request type.
     */
    public static LastReqType getTypeByValue( int type )
    {
        for ( LastReqType lrt : LastReqType.values() )
        {
            if ( type == lrt.getValue() )
            {
                return lrt;
            }
        }

        return NONE;
    }


    /**
     * Returns the number associated with this last request type.
     *
     * @return The last request type ordinal.
     */
    public int getValue()
    {
        return value;
    }


    /**
     * @see Object#toString()
     */
    public String toString()
    {
        return name + " (" + value + ")";
    }
}
