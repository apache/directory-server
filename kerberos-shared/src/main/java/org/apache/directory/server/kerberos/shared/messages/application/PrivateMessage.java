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
package org.apache.directory.server.kerberos.shared.messages.application;


import org.apache.directory.server.kerberos.shared.messages.KerberosMessage;
import org.apache.directory.server.kerberos.shared.messages.MessageType;
import org.apache.directory.server.kerberos.shared.messages.value.EncryptedData;


public class PrivateMessage extends KerberosMessage
{
    private EncryptedData encryptedPart;


    public PrivateMessage()
    {
        super( MessageType.KRB_PRIV );
        // used by ASN.1 decoder
    }


    public PrivateMessage(EncryptedData encryptedPart)
    {
        super( MessageType.KRB_PRIV );
        this.encryptedPart = encryptedPart;
    }


    public EncryptedData getEncryptedPart()
    {
        return encryptedPart;
    }


    public void setEncryptedPart( EncryptedData encryptedData )
    {
        encryptedPart = encryptedData;
    }
}
