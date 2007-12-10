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
package org.apache.directory.server.kerberos.shared.messages.application;


import org.apache.directory.server.kerberos.shared.messages.KerberosMessage;
import org.apache.directory.server.kerberos.shared.messages.MessageType;
import org.apache.directory.server.kerberos.shared.messages.value.Checksum;
import org.apache.directory.server.kerberos.shared.messages.value.HostAddress;
import org.apache.directory.server.kerberos.shared.messages.value.KerberosTime;


/**
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class SafeMessage extends KerberosMessage
{
    private SafeBody safeBody;
    private Checksum cksum;


    /**
     * Creates a new instance of SafeMessage.
     *
     * @param safeBody
     * @param cksum
     */
    public SafeMessage( SafeBody safeBody, Checksum cksum )
    {
        super( MessageType.KRB_SAFE );
        this.safeBody = safeBody;
        this.cksum = cksum;
    }


    /**
     * Returns the {@link Checksum}.
     *
     * @return The {@link Checksum}.
     */
    public Checksum getCksum()
    {
        return cksum;
    }


    // SafeBody delegate methods

    /**
     * Returns the "R" {@link HostAddress}.
     *
     * @return The "R" {@link HostAddress}.
     */
    public HostAddress getRAddress()
    {
        return safeBody.getRAddress();
    }


    /**
     * Returns the "S" {@link HostAddress}.
     *
     * @return The "S" {@link HostAddress}.
     */
    public HostAddress getSAddress()
    {
        return safeBody.getSAddress();
    }


    /**
     * Returns the sequence number.
     *
     * @return The sequence number.
     */
    public Integer getSeqNumber()
    {
        return safeBody.getSeqNumber();
    }


    /**
     * Returns the {@link KerberosTime} timestamp.
     *
     * @return The {@link KerberosTime} timestamp.
     */
    public KerberosTime getTimestamp()
    {
        return safeBody.getTimestamp();
    }


    /**
     * Returns the microsecond.
     *
     * @return The microsecond.
     */
    public Integer getUsec()
    {
        return safeBody.getUsec();
    }


    /**
     * Returns the user data.
     *
     * @return The user data.
     */
    public byte[] getUserData()
    {
        return safeBody.getUserData();
    }
}
