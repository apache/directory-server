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
import org.apache.directory.server.kerberos.shared.messages.value.Checksum;
import org.apache.directory.server.kerberos.shared.messages.value.HostAddress;
import org.apache.directory.server.kerberos.shared.messages.value.KerberosTime;


public class SafeMessage extends KerberosMessage
{
    private SafeBody safeBody;
    private Checksum cksum;


    public SafeMessage(SafeBody safeBody, Checksum cksum)
    {
        super( MessageType.KRB_SAFE );
        this.safeBody = safeBody;
        this.cksum = cksum;
    }


    public Checksum getCksum()
    {
        return cksum;
    }


    // SafeBody delegate methods
    public HostAddress getRAddress()
    {
        return safeBody.getRAddress();
    }


    public HostAddress getSAddress()
    {
        return safeBody.getSAddress();
    }


    public Integer getSeqNumber()
    {
        return safeBody.getSeqNumber();
    }


    public KerberosTime getTimestamp()
    {
        return safeBody.getTimestamp();
    }


    public Integer getUsec()
    {
        return safeBody.getUsec();
    }


    public byte[] getUserData()
    {
        return safeBody.getUserData();
    }
}
