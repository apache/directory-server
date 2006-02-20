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


import org.apache.directory.server.kerberos.shared.messages.Encodable;
import org.apache.directory.server.kerberos.shared.messages.KerberosMessage;
import org.apache.directory.server.kerberos.shared.messages.MessageType;
import org.apache.directory.server.kerberos.shared.messages.value.EncryptionKey;
import org.apache.directory.server.kerberos.shared.messages.value.KerberosTime;


/**
 * Encrypted part of the application response
 */
public class EncApRepPart extends KerberosMessage implements Encodable
{
    private KerberosTime clientTime;
    private int cusec;
    private EncryptionKey subSessionKey; //optional
    private Integer sequenceNumber; //optional


    public EncApRepPart(KerberosTime clientTime, int cusec, EncryptionKey subSessionKey, Integer sequenceNumber)
    {
        super( MessageType.ENC_AP_REP_PART );

        this.clientTime = clientTime;
        this.cusec = cusec;
        this.subSessionKey = subSessionKey;
        this.sequenceNumber = sequenceNumber;
    }


    public KerberosTime getClientTime()
    {
        return clientTime;
    }


    public int getClientMicroSecond()
    {
        return cusec;
    }


    public Integer getSequenceNumber()
    {
        return sequenceNumber;
    }


    public EncryptionKey getSubSessionKey()
    {
        return subSessionKey;
    }
}
