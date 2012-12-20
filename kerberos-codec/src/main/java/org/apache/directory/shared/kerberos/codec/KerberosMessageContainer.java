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
package org.apache.directory.shared.kerberos.codec;


import org.apache.directory.api.asn1.ber.AbstractContainer;
import org.apache.directory.shared.kerberos.components.PrincipalName;
import org.apache.directory.shared.kerberos.messages.KerberosMessage;
import org.apache.directory.shared.kerberos.messages.Ticket;


/**
 * The KerberosMessage container stores all the messages decoded by the Asn1Decoder.
 * When dealing with an incoding PDU, we will obtain a KerberosMessage in the
 * container.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class KerberosMessageContainer extends AbstractContainer
{
    /** The internal kerberos message */
    private KerberosMessage message;

    /** A PrincipalName container */
    private PrincipalName principalName;

    /** A flag used when the protocol used to transfer the PDU is TCP */
    private boolean isTCP;

    /** When the connection is using a TCP protocol, the PDU length */
    private int tcpLength = -1;


    /**
     * Creates a new KerberosMessageContainer object. We will store ten grammars,
     * it's enough ...
     */
    public KerberosMessageContainer()
    {
        super();
        this.stateStack = new int[1];
        this.grammar = KerberosMessageGrammar.getInstance();
        setTransition( KerberosMessageStatesEnum.START_STATE );
    }


    /**
     * @return Returns the KerberosMessage.
     */
    public KerberosMessage getMessage()
    {
        return message;
    }


    /**
     * Set a Message Object into the container. It will be completed by the
     * KerberosDecoder.
     * 
     * @param message The message to set.
     */
    public void setMessage( KerberosMessage message )
    {
        this.message = message;
    }


    /**
     * @return Returns the Ticket if the interned message is a Ticket.
     */
    public Ticket getTicket()
    {
        return ( Ticket ) message;
    }


    /**
     * @return Returns the PrincipalName.
     */
    public PrincipalName getPrincipalName()
    {
        return principalName;
    }


    /**
     * Set a PrincipalName Object into the container. It will be completed by the
     * KerberosDecoder.
     * 
     * @param principalName The principalName to set.
     */
    public void setPrincipalName( PrincipalName principalName )
    {
        this.principalName = principalName;
    }


    /**
     * @return the isTCP
     */
    public boolean isTCP()
    {
        return isTCP;
    }


    /**
     * @param isTCP the isTCP to set
     */
    public void setTCP( boolean isTCP )
    {
        this.isTCP = isTCP;
    }


    /**
     * @return the tcpLength
     */
    public int getTcpLength()
    {
        return tcpLength;
    }


    /**
     * @param tcpLength the tcpLength to set
     */
    public void setTcpLength( int tcpLength )
    {
        this.tcpLength = tcpLength;
    }
}
