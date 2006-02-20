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
package org.apache.directory.server.kerberos.shared.messages.components;


import org.apache.directory.server.kerberos.shared.messages.value.HostAddress;
import org.apache.directory.server.kerberos.shared.messages.value.KerberosTime;


public class EncKrbPrivPartModifier
{
    private byte[] userData;
    private KerberosTime timestamp; //optional
    private Integer usec; //optional
    private Integer sequenceNumber; //optional
    private HostAddress senderAddress; //optional
    private HostAddress recipientAddress; //optional


    public EncKrbPrivPart getEncKrbPrivPart()
    {
        return new EncKrbPrivPart( userData, timestamp, usec, sequenceNumber, senderAddress, recipientAddress );
    }


    public void setRecipientAddress( HostAddress address )
    {
        recipientAddress = address;
    }


    public void setSenderAddress( HostAddress address )
    {
        senderAddress = address;
    }


    public void setSequenceNumber( Integer number )
    {
        sequenceNumber = number;
    }


    public void setTimestamp( KerberosTime timestamp )
    {
        this.timestamp = timestamp;
    }


    public void setMicroSecond( Integer usec )
    {
        this.usec = usec;
    }


    public void setUserData( byte[] data )
    {
        userData = data;
    }
}
