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
package org.apache.directory.shared.kerberos.messages;


import org.apache.directory.shared.asn1.AbstractAsn1Object;
import org.apache.directory.shared.kerberos.KerberosConstants;
import org.apache.directory.shared.kerberos.KerberosMessageType;


/**
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public abstract class KerberosMessage extends AbstractAsn1Object
{
    /** The protocol version (should be 5) */
    private int protocolVersionNumber = KerberosConstants.KERBEROS_V5;

    /** The message type */
    private KerberosMessageType messageType;


    /**
     * Creates a new instance of KerberosMessage.
     *
     * @param type The message type
     */
    public KerberosMessage( KerberosMessageType type )
    {
        this( KerberosConstants.KERBEROS_V5, type );
    }


    /**
     * Creates a new instance of KerberosMessage.
     *
     * @param versionNumber
     * @param type
     */
    public KerberosMessage( int versionNumber, KerberosMessageType type )
    {
        protocolVersionNumber = versionNumber;
        messageType = type;
    }


    /**
     * Returns the {@link org.apache.directory.server.kerberos.shared.KerberosMessageType}.
     *
     * @return The {@link org.apache.directory.server.kerberos.shared.KerberosMessageType}.
     */
    public KerberosMessageType getMessageType()
    {
        return messageType;
    }


    /**
     * Sets the {@link org.apache.directory.server.kerberos.shared.KerberosMessageType}.
     *
     * @param type
     */
    public void setMessageType( KerberosMessageType type )
    {
        messageType = type;
    }


    /**
     * Returns the protocol version number.
     *
     * @return The protocol version number.
     */
    public int getProtocolVersionNumber()
    {
        return protocolVersionNumber;
    }


    /**
     * Sets the protocol version number.
     *
     * @param versionNumber
     */
    public void setProtocolVersionNumber( int versionNumber )
    {
        protocolVersionNumber = versionNumber;
    }
}
