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


import org.apache.directory.server.kerberos.shared.messages.value.HostAddress;
import org.apache.directory.server.kerberos.shared.messages.value.KerberosTime;


/**
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class EncKrbPrivPartModifier
{
    private byte[] userData;
    private KerberosTime timestamp; //optional
    private Integer usec; //optional
    private Integer sequenceNumber; //optional
    private HostAddress senderAddress; //optional
    private HostAddress recipientAddress; //optional


    /**
     * Returns the {@link EncKrbPrivPart}.
     *
     * @return The {@link EncKrbPrivPart}.
     */
    public EncKrbPrivPart getEncKrbPrivPart()
    {
        return new EncKrbPrivPart( userData, timestamp, usec, sequenceNumber, senderAddress, recipientAddress );
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
     * Sets the sender {@link HostAddress}.
     *
     * @param address
     */
    public void setSenderAddress( HostAddress address )
    {
        senderAddress = address;
    }


    /**
     * Sets the sequence number.
     *
     * @param number
     */
    public void setSequenceNumber( Integer number )
    {
        sequenceNumber = number;
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
     * Sets the microsecond.
     *
     * @param usec
     */
    public void setMicroSecond( Integer usec )
    {
        this.usec = usec;
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
