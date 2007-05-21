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
import org.apache.directory.server.kerberos.shared.messages.value.HostAddresses;
import org.apache.directory.server.kerberos.shared.messages.value.KerberosTime;
import org.apache.directory.server.kerberos.shared.messages.value.KrbCredInfo;


/**
 * Encrypted part of credential message types.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class EncKrbCredPart
{
    private KrbCredInfo[] ticketInfo;
    private Integer nonce; //optional
    private KerberosTime timeStamp; //optional
    private Integer usec; //optional
    private HostAddress sAddress; //optional
    private HostAddresses rAddress; //optional


    /**
     * Creates a new instance of EncKrbCredPart.
     *
     * @param ticketInfo
     * @param timeStamp
     * @param usec
     * @param nonce
     * @param sAddress
     * @param rAddress
     */
    public EncKrbCredPart( KrbCredInfo[] ticketInfo, KerberosTime timeStamp, Integer usec, Integer nonce,
        HostAddress sAddress, HostAddresses rAddress )
    {
        this.ticketInfo = ticketInfo;
        this.nonce = nonce;
        this.timeStamp = timeStamp;
        this.usec = usec;
        this.sAddress = sAddress;
        this.rAddress = rAddress;
    }


    /**
     * Returns the nonce.
     * 
     * @return The nonce.
     */
    public Integer getNonce()
    {
        return nonce;
    }


    /**
     * Returns the "R" {@link HostAddresses}.
     * 
     * @return The "R" {@link HostAddresses}.
     */
    public HostAddresses getRAddress()
    {
        return rAddress;
    }


    /**
     * Returns the "S" {@link HostAddresses}.
     * 
     * @return The "S" {@link HostAddresses}.
     */
    public HostAddress getSAddress()
    {
        return sAddress;
    }


    /**
     * Returns the {@link KrbCredInfo}s.
     * 
     * @return The {@link KrbCredInfo}s.
     */
    public KrbCredInfo[] getTicketInfo()
    {
        return ticketInfo;
    }


    /**
     * Returns the timestamp.
     * 
     * @return The timeStamp.
     */
    public KerberosTime getTimeStamp()
    {
        return timeStamp;
    }


    /**
     * Returns the microseconds.
     * 
     * @return The microseconds.
     */
    public Integer getUsec()
    {
        return usec;
    }
}
