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
package org.apache.kerberos.messages.components;

import org.apache.kerberos.messages.Encodable;
import org.apache.kerberos.messages.KerberosMessage;
import org.apache.kerberos.messages.MessageType;
import org.apache.kerberos.messages.value.HostAddress;
import org.apache.kerberos.messages.value.KerberosTime;

/**
 * Encrypted part of private messages
 */
public class EncKrbPrivPart extends KerberosMessage implements Encodable
{
	private byte[]       userData;
	private KerberosTime timestamp;        //optional
	private Integer      usec;             //optional
	private Integer      sequenceNumber;   //optional
	private HostAddress  senderAddress;    //optional
	private HostAddress  recipientAddress; //optional

	public EncKrbPrivPart( byte[] userData, KerberosTime timestamp, Integer usec,
            Integer sequenceNumber, HostAddress senderAddress, HostAddress recipientAddress )
    {
        super( MessageType.ENC_PRIV_PART );
		
		this.userData         = userData;
		this.timestamp        = timestamp;
		this.usec             = usec;
		this.sequenceNumber   = sequenceNumber;
		this.senderAddress    = senderAddress;
		this.recipientAddress = recipientAddress;
	}

    public HostAddress getRecipientAddress()
    {
        return recipientAddress;
    }

    public HostAddress getSenderAddress()
    {
        return senderAddress;
    }

    public Integer getSequenceNumber()
    {
        return sequenceNumber;
    }

    public KerberosTime getTimestamp()
    {
        return timestamp;
    }

    public Integer getMicroSecond()
    {
        return usec;
    }

    public byte[] getUserData()
    {
        return userData;
    }
}
