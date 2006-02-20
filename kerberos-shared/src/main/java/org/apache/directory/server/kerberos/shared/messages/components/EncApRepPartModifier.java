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


import org.apache.directory.server.kerberos.shared.messages.value.EncryptionKey;
import org.apache.directory.server.kerberos.shared.messages.value.KerberosTime;


public class EncApRepPartModifier
{
    private KerberosTime clientTime;
    private int cusec;
    private EncryptionKey subSessionKey; //optional
    private Integer sequenceNumber; //optional


    public EncApRepPart getEncApRepPart()
    {
        return new EncApRepPart( clientTime, cusec, subSessionKey, sequenceNumber );
    }


    public void setClientTime( KerberosTime clientTime )
    {
        this.clientTime = clientTime;
    }


    public void setClientMicroSecond( int cusec )
    {
        this.cusec = cusec;
    }


    public void setSubSessionKey( EncryptionKey subSessionKey )
    {
        this.subSessionKey = subSessionKey;
    }


    public void setSequenceNumber( Integer sequenceNumber )
    {
        this.sequenceNumber = sequenceNumber;
    }
}
