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


public class PreAuthenticationDataType implements Comparable
{
    /**
     * Enumeration elements are constructed once upon class loading.
     * Order of appearance here determines the order of compareTo.
     */
    public static final PreAuthenticationDataType NULL = new PreAuthenticationDataType( 0, "null" );
    public static final PreAuthenticationDataType PA_TGS_REQ = new PreAuthenticationDataType( 1, "TGS Request." );
    public static final PreAuthenticationDataType PA_ENC_TIMESTAMP = new PreAuthenticationDataType( 2,
        "Encrypted timestamp." );
    public static final PreAuthenticationDataType PA_PW_SALT = new PreAuthenticationDataType( 3, "password salt" );
    public static final PreAuthenticationDataType PA_ENC_UNIX_TIME = new PreAuthenticationDataType( 5, "enc unix time" );
    public static final PreAuthenticationDataType PA_SANDIA_SECUREID = new PreAuthenticationDataType( 6,
        "sandia secureid" );
    public static final PreAuthenticationDataType PA_SESAME = new PreAuthenticationDataType( 7, "sesame" );
    public static final PreAuthenticationDataType PA_OSF_DCE = new PreAuthenticationDataType( 8, "OSF DCE" );
    public static final PreAuthenticationDataType PA_CYBERSAFE_SECUREID = new PreAuthenticationDataType( 9,
        "cybersafe secureid" );
    public static final PreAuthenticationDataType PA_ASF3_SALT = new PreAuthenticationDataType( 10, "ASF3 salt" );
    public static final PreAuthenticationDataType PA_ENCTYPE_INFO = new PreAuthenticationDataType( 11,
        "Encryption info." );
    public static final PreAuthenticationDataType SAM_CHALLENGE = new PreAuthenticationDataType( 12, "SAM challenge." );
    public static final PreAuthenticationDataType SAM_RESPONSE = new PreAuthenticationDataType( 13, "SAM response." );
    public static final PreAuthenticationDataType PA_PK_AS_REQ = new PreAuthenticationDataType( 14, "PK as request" );
    public static final PreAuthenticationDataType PA_PK_AS_REP = new PreAuthenticationDataType( 15, "PK as response" );
    public static final PreAuthenticationDataType PA_USE_SPECIFIED_KVNO = new PreAuthenticationDataType( 20,
        "use specified key version" );
    public static final PreAuthenticationDataType SAM_REDIRECT = new PreAuthenticationDataType( 21, "SAM redirect." );
    public static final PreAuthenticationDataType PA_GET_FROM_TYPED_DATA = new PreAuthenticationDataType( 22,
        "Get from typed data" );

    /** Array for building a List of VALUES. */
    private static final PreAuthenticationDataType[] values =
        { NULL, PA_TGS_REQ, PA_ENC_TIMESTAMP, PA_PW_SALT, PA_ENC_UNIX_TIME, PA_SANDIA_SECUREID, PA_SESAME, PA_OSF_DCE,
            PA_CYBERSAFE_SECUREID, PA_ASF3_SALT, PA_ENCTYPE_INFO, SAM_CHALLENGE, SAM_RESPONSE, PA_PK_AS_REQ,
            PA_PK_AS_REP, PA_USE_SPECIFIED_KVNO, SAM_REDIRECT, PA_GET_FROM_TYPED_DATA };

    /** A list of all the pre-authentication type constants. */
    public static final List VALUES = Collections.unmodifiableList( Arrays.asList( values ) );

    /** The name of the pre-authentication type. */
    private final String name;

    /** The value/code for the pre-authentication type. */
    private final int ordinal;


    /**
     * Private constructor prevents construction outside of this class.
     */
    private PreAuthenticationDataType(int ordinal, String name)
    {
        this.ordinal = ordinal;
        this.name = name;
    }


    public String toString()
    {
        return name + " (" + ordinal + ")";
    }


    public int compareTo( Object that )
    {
        return ordinal - ( ( PreAuthenticationDataType ) that ).ordinal;
    }


    public static PreAuthenticationDataType getTypeByOrdinal( int type )
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


    public int getOrdinal()
    {
        return ordinal;
    }
}
