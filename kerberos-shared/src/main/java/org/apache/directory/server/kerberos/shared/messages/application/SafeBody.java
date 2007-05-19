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


import org.apache.directory.server.kerberos.shared.messages.value.HostAddress;
import org.apache.directory.server.kerberos.shared.messages.value.KerberosTime;


/**
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class SafeBody
{
    private byte[] userData;
    private KerberosTime timestamp; //optional
    private Integer usec; //optional
    private Integer seqNumber; //optional
    private HostAddress sAddress; //optional
    private HostAddress rAddress; //optional


    /**
     * Creates a new instance of SafeBody.
     *
     * @param userData
     * @param timestamp
     * @param usec
     * @param seqNumber
     * @param sAddress
     * @param rAddress
     */
    public SafeBody( byte[] userData, KerberosTime timestamp, Integer usec, Integer seqNumber, HostAddress sAddress,
        HostAddress rAddress )
    {
        this.userData = userData;
        this.timestamp = timestamp;
        this.usec = usec;
        this.seqNumber = seqNumber;
        this.sAddress = sAddress;
        this.rAddress = rAddress;
    }


    /**
     * Returns the "R" {@link HostAddress}.
     *
     * @return The "R" {@link HostAddress}.
     */
    public HostAddress getRAddress()
    {
        return rAddress;
    }


    /**
     * Returns the "S" {@link HostAddress}.
     *
     * @return The "S" {@link HostAddress}.
     */
    public HostAddress getSAddress()
    {
        return sAddress;
    }


    /**
     * Returns the sequence number.
     *
     * @return The sequence number.
     */
    public Integer getSeqNumber()
    {
        return seqNumber;
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
     * Returns the microsecond.
     *
     * @return The microsecond.
     */
    public Integer getUsec()
    {
        return usec;
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
}
