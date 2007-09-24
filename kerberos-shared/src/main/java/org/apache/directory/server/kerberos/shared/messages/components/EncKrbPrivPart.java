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


import org.apache.directory.server.kerberos.shared.messages.Encodable;
import org.apache.directory.server.kerberos.shared.messages.KerberosMessage;
import org.apache.directory.server.kerberos.shared.messages.MessageType;
import org.apache.directory.server.kerberos.shared.messages.value.HostAddress;
import org.apache.directory.server.kerberos.shared.messages.value.KerberosTime;


/**
 * Encrypted part of private messages.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class EncKrbPrivPart extends KerberosMessage implements Encodable
{
    private byte[] userData;
    private KerberosTime timestamp;         // optional
    private int usec;                       // optional
    private int sequenceNumber;             // optional
    private HostAddress senderAddress;      // optional
    private HostAddress recipientAddress;   // optional

    /**
     * Creates a new instance of EncKrbPrivPart.
     *
     * @return The {@link EncKrbPrivPart}.
     */
    public EncKrbPrivPart()
    {
        super( MessageType.ENC_PRIV_PART );
        usec = -1;
        sequenceNumber = -1;
    }


    /**
     * Returns the recipient {@link HostAddress}.
     *
     * @return The recipient {@link HostAddress}.
     */
    public HostAddress getRecipientAddress()
    {
        return recipientAddress;
    }


    /**
     * Sets the recipient {@link HostAddress}.
     *
     * @param address
     */
    public void setRecipientAddress( HostAddress address )
    {
        recipientAddress = address;
    }

    
    /**
     * Returns the sender {@link HostAddress}.
     *
     * @return The sender {@link HostAddress}.
     */
    public HostAddress getSenderAddress()
    {
        return senderAddress;
    }


    /**
     * Sets the sender {@link HostAddress}.
     *
     * @param address
     */
    public void setSenderAddress( HostAddress address )
    {
        senderAddress = address;
    }

    
    /**
     * Returns the sequence number.
     *
     * @return The sequence number.
     */
    public int getSequenceNumber()
    {
        return sequenceNumber;
    }


    /**
     * Sets the sequence number.
     *
     * @param number
     */
    public void setSequenceNumber( int number )
    {
        sequenceNumber = number;
    }

    
    /**
     * Returns the {@link KerberosTime} timestamp.
     *
     * @return The {@link KerberosTime} timestamp.
     */
    public KerberosTime getTimestamp()
    {
        return timestamp;
    }


    /**
     * Sets the {@link KerberosTime} timestamp.
     *
     * @param timestamp
     */
    public void setTimestamp( KerberosTime timestamp )
    {
        this.timestamp = timestamp;
    }

    
    /**
     * Returns the microsecond.
     *
     * @return The microsecond.
     */
    public int getMicroSecond()
    {
        return usec;
    }


    /**
     * Sets the microsecond.
     *
     * @param usec
     */
    public void setMicroSecond( int usec )
    {
        this.usec = usec;
    }


    /**
     * Returns the user data.
     *
     * @return The user data.
     */
    public byte[] getUserData()
    {
        return userData;
    }

    
    /**
     * Sets the user data.
     *
     * @param data
     */
    public void setUserData( byte[] data )
    {
        userData = data;
    }
}
