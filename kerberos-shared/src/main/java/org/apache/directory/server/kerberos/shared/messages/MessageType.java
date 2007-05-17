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
package org.apache.directory.server.kerberos.shared.messages;


import java.util.Arrays;
import java.util.Collections;
import java.util.List;


/**
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public final class MessageType implements Comparable
{
    /**
     * Constant for the "null" message type.
     */
    public static final MessageType NULL = new MessageType( 0, "null" );

    /**
     * Constant for the "initial authentication request" message type.
     */
    public static final MessageType KRB_AS_REQ = new MessageType( 10, "initial authentication request" );

    /**
     * Constant for the "initial authentication response" message type.
     */
    public static final MessageType KRB_AS_REP = new MessageType( 11, "initial authentication response" );

    /**
     * Constant for the "request for authentication based on TGT" message type.
     */
    public static final MessageType KRB_TGS_REQ = new MessageType( 12, "request for authentication based on TGT" );

    /**
     * Constant for the "response to authentication based on TGT" message type.
     */
    public static final MessageType KRB_TGS_REP = new MessageType( 13, "response to authentication based on TGT" );

    /**
     * Constant for the "application request" message type.
     */
    public static final MessageType KRB_AP_REQ = new MessageType( 14, "application request" );

    /**
     * Constant for the "application response" message type.
     */
    public static final MessageType KRB_AP_REP = new MessageType( 15, "application response" );

    /**
     * Constant for the "safe (checksummed) application message" message type.
     */
    public static final MessageType KRB_SAFE = new MessageType( 20, "safe (checksummed) application message" );

    /**
     * Constant for the "private (encrypted) application message" message type.
     */
    public static final MessageType KRB_PRIV = new MessageType( 21, "private (encrypted) application message" );

    /**
     * Constant for the "private (encrypted) message to forward credentials" message type.
     */
    public static final MessageType KRB_CRED = new MessageType( 22,
        "private (encrypted) message to forward credentials" );

    /**
     * Constant for the "encrypted application reply part" message type.
     */
    public static final MessageType ENC_AP_REP_PART = new MessageType( 27, "encrypted application reply part" );

    /**
     * Constant for the "encrypted private message part" message type.
     */
    public static final MessageType ENC_PRIV_PART = new MessageType( 28, "encrypted private message part" );

    /**
     * Constant for the "error response" message type.
     */
    public static final MessageType KRB_ERROR = new MessageType( 30, "error response" );

    /**
     * Array for building a List of VALUES.
     */
    private static final MessageType[] values =
        { NULL, KRB_AS_REQ, KRB_AS_REP, KRB_TGS_REQ, KRB_TGS_REP, KRB_AP_REQ, KRB_AP_REP, KRB_SAFE, KRB_PRIV, KRB_CRED,
            ENC_AP_REP_PART, ENC_PRIV_PART, KRB_ERROR };

    /**
     * A list of all the message type constants.
     */
    public static final List VALUES = Collections.unmodifiableList( Arrays.asList( values ) );

    /**
     * The name of the message type.
     */
    private final String name;

    /**
     * The value/code for the message type.
     */
    private final int ordinal;


    /**
     * Private constructor prevents construction outside of this class.
     */
    private MessageType( int ordinal, String name )
    {
        this.ordinal = ordinal;
        this.name = name;
    }


    /**
     * Returns the message type when specified by its ordinal.
     *
     * @param type
     * @return The message type.
     */
    public static MessageType getTypeByOrdinal( int type )
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
     * Returns the number associated with this message type.
     *
     * @return The message type ordinal.
     */
    public int getOrdinal()
    {
        return ordinal;
    }


    public int compareTo( Object that )
    {
        return ordinal - ( ( MessageType ) that ).ordinal;
    }


    public String toString()
    {
        return name + " (" + ordinal + ")";
    }
}
