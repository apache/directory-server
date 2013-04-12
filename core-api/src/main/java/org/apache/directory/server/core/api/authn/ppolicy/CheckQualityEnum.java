/*
 *   Licensed to the Apache Software Foundation (ASF) under one
 *   or more contributor license agreements.  See the NOTICE file
 *   distributed with this work for additional information
 *   regarding copyright ownership.  The ASF licenses this file
 *   to you under the Apache License, Version 2.0 (the
 *   "License"); you may not use this file except in compliance
 *   with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing,
 *   software distributed under the License is distributed on an
 *   "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *   KIND, either express or implied.  See the License for the
 *   specific language governing permissions and limitations
 *   under the License.
 *
 */

package org.apache.directory.server.core.api.authn.ppolicy;


/**
 * The 3 possible values for the password check quality : </br>
 * <ul>
 * <li>NO_CHECK (0) : No check will be done</li>
 * <li>CHECK_ACCEPT (1) : Check the password and accept hashed passwords</li>
 * <li>CHECK_REJECT (2) : Check the password but reject hashed passwords</li>
 * </ul>
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public enum CheckQualityEnum
{
    /** Don't check the password */
    NO_CHECK(0),

    /** Check the password and accept passwords that can't be checked (hashed passwords) */
    CHECK_ACCEPT(1),

    /** Check the password but reject passwords that can't be checked (hashed passwords) */
    CHECK_REJECT(2),

    /** An unknown value */
    UNKNOW(-1);

    /** The stored value */
    private int value;


    /**
     * Create a new instance of this enum
     */
    private CheckQualityEnum( int value )
    {
        this.value = value;
    }


    /**
     * @return The internal value
     */
    public int getValue()
    {
        return value;
    }


    /**
     * Get back the CheckQualityEnum instance associated with a given value
     * 
     * @param value The value we are looking for
     * @return The associated CheckQualityEnum
     */
    public static CheckQualityEnum getCheckQuality( int value )
    {
        switch ( value )
        {
            case 0:
                return NO_CHECK;

            case 1:
                return CHECK_ACCEPT;

            case 2:
                return CHECK_REJECT;

            default:
                return UNKNOW;
        }
    }
}
