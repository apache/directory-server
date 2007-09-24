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
package org.apache.directory.server.kerberos.shared.messages.value.types;


import java.util.Arrays;
import java.util.Collections;
import java.util.List;


/**
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev: 540371 $, $Date: 2007-05-22 02:00:43 +0200 (Tue, 22 May 2007) $
 */
public enum PreAuthenticationDataType
{
    /**
     * Constant for the "null" pre-authentication data type.
     */
    NULL( 0 ),

    /**
     * Constant for the "TGS request" pre-authentication data type.
     */
    PA_TGS_REQ( 1 ),

    /**
     * Constant for the "encrypted timestamp" pre-authentication data type.
     */
    PA_ENC_TIMESTAMP( 2 ),

    /**
     * Constant for the "password salt" pre-authentication data type.
     */
    PA_PW_SALT( 3 ),

    /**
     * Constant for the "enc unix time" pre-authentication data type.
     */
    PA_ENC_UNIX_TIME( 5 ),

    /**
     * Constant for the "sandia secureid" pre-authentication data type.
     */
    PA_SANDIA_SECUREID( 6 ),

    /**
     * Constant for the "sesame" pre-authentication data type.
     */
    PA_SESAME( 7 ),

    /**
     * Constant for the "OSF DCE" pre-authentication data type.
     */
    PA_OSF_DCE( 8 ),

    /**
     * Constant for the "cybersafe secureid" pre-authentication data type.
     */
    PA_CYBERSAFE_SECUREID( 9 ),

    /**
     * Constant for the "ASF3 salt" pre-authentication data type.
     */
    PA_ASF3_SALT( 10 ),

    /**
     * Constant for the "encryption info" pre-authentication data type.
     */
    PA_ENCTYPE_INFO( 11 ),

    /**
     * Constant for the "SAM challenge" pre-authentication data type.
     */
    SAM_CHALLENGE( 12 ),

    /**
     * Constant for the "SAM response" pre-authentication data type.
     */
    SAM_RESPONSE( 13 ),

    /**
     * Constant for the "PK as request" pre-authentication data type.
     */
    PA_PK_AS_REQ( 14 ),

    /**
     * Constant for the "PK as response" pre-authentication data type.
     */
    PA_PK_AS_REP( 15 ),

    /**
     * Constant for the "use specified key version" pre-authentication data type.
     */
    PA_USE_SPECIFIED_KVNO( 20 ),

    /**
     * Constant for the "SAM redirect" pre-authentication data type.
     */
    SAM_REDIRECT( 21 ),

    /**
     * Constant for the "get from typed data" pre-authentication data type.
     */
    PA_GET_FROM_TYPED_DATA( 22 );

    /**
     * Array for building a List of VALUES.
     */
    private static final PreAuthenticationDataType[] values =
        { NULL, PA_TGS_REQ, PA_ENC_TIMESTAMP, PA_PW_SALT, PA_ENC_UNIX_TIME, PA_SANDIA_SECUREID, PA_SESAME, PA_OSF_DCE,
            PA_CYBERSAFE_SECUREID, PA_ASF3_SALT, PA_ENCTYPE_INFO, SAM_CHALLENGE, SAM_RESPONSE, PA_PK_AS_REQ,
            PA_PK_AS_REP, PA_USE_SPECIFIED_KVNO, SAM_REDIRECT, PA_GET_FROM_TYPED_DATA };

    /**
     * A list of all the pre-authentication type constants.
     */
    public static final List VALUES = Collections.unmodifiableList( Arrays.asList( values ) );

    /**
     * The value/code for the pre-authentication type.
     */
    private final int ordinal;


    /**
     * Private constructor prevents construction outside of this class.
     */
    private PreAuthenticationDataType( int ordinal )
    {
        this.ordinal = ordinal;
    }


    /**
     * Returns the number associated with this pre-authentication type.
     *
     * @return The pre-authentication type ordinal.
     */
    public int getOrdinal()
    {
        return ordinal;
    }
    
    /**
     * Returns the pre authentication data type when specified by its ordinal.
     *
     * @param type The ordinal
     * @return The pre authentication type.
     */
    public static PreAuthenticationDataType getTypeByOrdinal( int type )
    {
        switch ( type )
        {
            case 1 :    return PA_TGS_REQ;
            case 2 :    return PA_ENC_TIMESTAMP;
            case 3 :    return PA_PW_SALT;
            case 5 :    return PA_ENC_UNIX_TIME;
            case 6 :    return PA_SANDIA_SECUREID;
            case 7 :    return PA_SESAME;
            case 8 :    return PA_OSF_DCE;
            case 9 :    return PA_CYBERSAFE_SECUREID;
            case 10 :   return PA_ASF3_SALT;
            case 11 :   return PA_ENCTYPE_INFO;
            case 12 :   return SAM_CHALLENGE;
            case 13 :   return SAM_RESPONSE;
            case 14 :   return PA_PK_AS_REQ;
            case 15 :   return PA_PK_AS_REQ;
            case 20 :   return PA_USE_SPECIFIED_KVNO;
            case 21 :   return SAM_REDIRECT;
            case 22 :   return PA_GET_FROM_TYPED_DATA;
            default :   return NULL;
        }
    }

    /**
     * @see Object#toString()
     */
    public String toString()
    {
        switch ( this )
        {
            case PA_TGS_REQ                     : 
                return "TGS request." + "(" + ordinal + ")";
            
            case PA_ENC_TIMESTAMP : 
                return "Encrypted timestamp." + "(" + ordinal + ")";
            
            case PA_PW_SALT : 
                return "password salt" + "(" + ordinal + ")";
            
            case PA_ENC_UNIX_TIME : 
                return "enc unix time" + "(" + ordinal + ")";
            
            case PA_SANDIA_SECUREID : 
                return "sandia secureid" + "(" + ordinal + ")";
            
            case PA_SESAME : 
                return "sesame" + "(" + ordinal + ")";
            
            case PA_OSF_DCE : 
                return "OSF DCE" + "(" + ordinal + ")";
            
            case PA_CYBERSAFE_SECUREID : 
                return "cybersafe secureid" + "(" + ordinal + ")";
            
            case PA_ASF3_SALT : 
                return "ASF3 salt" + "(" + ordinal + ")";
            
            case PA_ENCTYPE_INFO : 
                return "Encryption info." + "(" + ordinal + ")";
            
            case SAM_CHALLENGE : 
                return "SAM challenge." + "(" + ordinal + ")";
            
            case SAM_RESPONSE : 
                return "SAM response." + "(" + ordinal + ")";
            
            case PA_PK_AS_REQ : 
                return "PK as request" + "(" + ordinal + ")";
            
            case PA_PK_AS_REP : 
                return "PK as response" + "(" + ordinal + ")";
                
            case PA_USE_SPECIFIED_KVNO :
                return "use specified key version" + "(" + ordinal + ")";
            
            case SAM_REDIRECT :
                return "SAM redirect." + "(" + ordinal + ")";
            
            case PA_GET_FROM_TYPED_DATA :
                return "Get from typed data" + "(" + ordinal + ")";
            
            default : 
                return "null" + "(" + ordinal + ")";
        }
    }
}