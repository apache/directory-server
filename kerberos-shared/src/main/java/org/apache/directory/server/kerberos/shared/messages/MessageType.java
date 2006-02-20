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
package org.apache.directory.server.kerberos.shared.messages;


import java.util.Arrays;
import java.util.Collections;
import java.util.List;


public final class MessageType implements Comparable
{
    /**
     * Enumeration elements are constructed once upon class loading.
     * Order of appearance here determines the order of compareTo.
     */
    public static final MessageType NULL = new MessageType( 0, "null" );
    public static final MessageType KRB_AS_REQ = new MessageType( 10, "initial authentication request" );
    public static final MessageType KRB_AS_REP = new MessageType( 11, "initial authentication response" );
    public static final MessageType KRB_TGS_REQ = new MessageType( 12, "request for authentication based on TGT" );
    public static final MessageType KRB_TGS_REP = new MessageType( 13, "response to authentication based on TGT" );
    public static final MessageType KRB_AP_REQ = new MessageType( 14, "application request" );
    public static final MessageType KRB_AP_REP = new MessageType( 15, "application response" );
    public static final MessageType KRB_SAFE = new MessageType( 20, "safe (checksummed) application message" );
    public static final MessageType KRB_PRIV = new MessageType( 21, "private (encrypted) application message" );
    public static final MessageType KRB_CRED = new MessageType( 22,
        "private (encrypted) message to forward credentials" );
    public static final MessageType ENC_AP_REP_PART = new MessageType( 27, "encrypted application reply part" );
    public static final MessageType ENC_PRIV_PART = new MessageType( 28, "encrypted private message part" );
    public static final MessageType KRB_ERROR = new MessageType( 30, "error response" );

    /** Array for building a List of VALUES. */
    private static final MessageType[] values =
        { NULL, KRB_AS_REQ, KRB_AS_REP, KRB_TGS_REQ, KRB_TGS_REP, KRB_AP_REQ, KRB_AP_REP, KRB_SAFE, KRB_PRIV, KRB_CRED,
            ENC_AP_REP_PART, ENC_PRIV_PART, KRB_ERROR };

    /** A list of all the message type constants. */
    public static final List VALUES = Collections.unmodifiableList( Arrays.asList( values ) );

    /** the name of the message type */
    private final String name;

    /** the value/code for the message type */
    private final int ordinal;


    /**
     * Private constructor prevents construction outside of this class.
     */
    private MessageType(int ordinal, String name)
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
        return ordinal - ( ( MessageType ) that ).ordinal;
    }


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


    public int getOrdinal()
    {
        return ordinal;
    }
}
