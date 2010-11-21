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
package org.apache.directory.server.kerberos.shared.messages.components;


import java.util.Arrays;
import java.util.Collections;
import java.util.List;


/**
 * Type-safe enumerator for message component types.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public enum MessageComponentType
{
    /**
     * Constant for the "null" message component type.
     */
    NULL ( 0, "null" ),

    /**
     * Constant for the "ticket" message component type.
     */
    KRB_TKT ( 1, "ticket" ),

    /**
     * Constant for the "authenticator" message component type.
     */
    KRB_AUTHENTICATOR ( 2, "authenticator" ),

    /**
     * Constant for the "encrypted ticket part" message component type.
     */
    KRB_ENC_TKT_PART ( 3, "encrypted ticket part" ),

    /**
     * Constant for the "encrypted initial authentication part" message component type.
     */
    KRB_ENC_AS_REP_PART ( 25,
        "encrypted initial authentication part" ),

    /**
     * Constant for the "encrypted TGS request part" message component type.
     */
    KRB_ENC_TGS_REP_PART ( 26,
        "encrypted TGS request part" ),

    /**
     * Constant for the "encrypted application request part" message component type.
     */
    KRB_ENC_AP_REP_PART ( 27,
        "encrypted application request part" ),

    /**
     * Constant for the "encrypted application message part" message component type.
     */
    KRB_ENC_KRB_PRIV_PART ( 28,
        "encrypted application message part" ),

    /**
     * Constant for the "encrypted credentials forward part" message component type.
     */
    KRB_ENC_KRB_CRED_PART ( 29,
        "encrypted credentials forward part" );

    /**
     * The name of the message component type.
     */
    private String name;

    /**
     * The value/code for the message component type.
     */
    private int ordinal;


    /**
     * Private constructor prevents construction outside of this class.
     */
    private MessageComponentType( int ordinal, String name )
    {
        this.ordinal = ordinal;
        this.name = name;
    }


    /**
     * Returns the message component type when specified by its ordinal.
     *
     * @param type
     * @return The message component type.
     */
    public static MessageComponentType getTypeByOrdinal( int type )
    {
        for ( MessageComponentType mct : MessageComponentType.values() )
        {
            if ( type == mct.getOrdinal() )
            {
                return mct;
            }
        }

        return NULL;
    }


    /**
     * Returns the number associated with this message component type.
     *
     * @return The message component type ordinal.
     */
    public int getOrdinal()
    {
        return ordinal;
    }


    public String toString()
    {
        return name + " (" + ordinal + ")";
    }
}
