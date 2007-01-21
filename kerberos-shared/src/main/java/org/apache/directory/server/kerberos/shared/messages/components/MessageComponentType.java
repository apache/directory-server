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
 * Type-safe enumerator for message component types
 */
public class MessageComponentType implements Comparable
{
    /**
     * Enumeration elements are constructed once upon class loading.
     * Order of appearance here determines the order of compareTo.
     */
    public static final MessageComponentType NULL = new MessageComponentType( 0, "null" );
    public static final MessageComponentType KRB_TKT = new MessageComponentType( 1, "ticket" );
    public static final MessageComponentType KRB_AUTHENTICATOR = new MessageComponentType( 2, "authenticator" );
    public static final MessageComponentType KRB_ENC_TKT_PART = new MessageComponentType( 3, "encrypted ticket part" );
    public static final MessageComponentType KRB_ENC_AS_REP_PART = new MessageComponentType( 25,
        "encrypted initial authentication part" );
    public static final MessageComponentType KRB_ENC_TGS_REP_PART = new MessageComponentType( 26,
        "encrypted TGS request part" );
    public static final MessageComponentType KRB_ENC_AP_REP_PART = new MessageComponentType( 27,
        "encrypted application request part" );
    public static final MessageComponentType KRB_ENC_KRB_PRIV_PART = new MessageComponentType( 28,
        "encrypted application message part" );
    public static final MessageComponentType KRB_ENC_KRB_CRED_PART = new MessageComponentType( 29,
        "encrypted credentials forward part" );

    /**
     * These two lines are all that's necessary to export a List of VALUES.
     */
    private static final MessageComponentType[] values =
        { NULL, KRB_TKT, KRB_AUTHENTICATOR, KRB_ENC_TKT_PART, KRB_ENC_AS_REP_PART, KRB_ENC_TGS_REP_PART,
            KRB_ENC_AP_REP_PART, KRB_ENC_KRB_PRIV_PART, KRB_ENC_KRB_CRED_PART };

    public static final List VALUES = Collections.unmodifiableList( Arrays.asList( values ) );

    private final String name;
    private final int ordinal;


    /**
     * Private constructor prevents construction outside of this class.
     */
    private MessageComponentType(int ordinal, String name)
    {
        this.ordinal = ordinal;
        this.name = name;
    }


    public static MessageComponentType getTypeByOrdinal( int type )
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


    public String toString()
    {
        return name + " (" + ordinal + ")";
    }


    public int compareTo( Object that )
    {
        return ordinal - ( ( MessageComponentType ) that ).ordinal;
    }
}
