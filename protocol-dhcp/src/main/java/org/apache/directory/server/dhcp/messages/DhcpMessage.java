/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License. 
 * 
 */

package org.apache.directory.server.dhcp.messages;


import java.net.InetAddress;

import org.apache.directory.server.dhcp.options.OptionsField;


/**
 * A DHCP (RFC 2131) message. Field descriptions contain the oroginal RFC field
 * names in brackets.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class DhcpMessage
{
    /**
     * Flag value: request broadcast answer.
     */
    public static final int FLAG_BROADCAST = 0x01;

    /**
     * [yiaddr] 'your' (client) IP address.
     */
    private InetAddress assignedClientAddress;

    /**
     * [file] Boot file name, null terminated string; "generic" name or null in
     * DHCPDISCOVER, fully qualified directory-path name in DHCPOFFER.
     */
    private String bootFileName;

    /**
     * [ciaddr] Current client IP address; only filled in if client is in BOUND,
     * RENEW or REBINDING state and can respond to ARP requests.
     */
    private InetAddress currentClientAddress;

    /**
     * [flags] Flags. (LSB is broadcast flag)
     */
    private short flags;

    /**
     * [hops] Client sets to zero, optionally used by relay agents when booting
     * via a relay agent.
     */
    private short hopCount;

    /**
     * [op] Message op code. 1 = BOOTREQUEST, 2 = BOOTREPLY, ...
     */
    private byte op;

    /**
     * Operation constant: boot request (client to server).
     * 
     * @see #op
     */
    public static final byte OP_BOOTREQUEST = 1;

    /**
     * Operation constant: boot reply (server to client).
     * 
     * @see #op
     */
    public static final byte OP_BOOTREPLY = 2;

    /**
     * [siaddr] IP address of next server to use in bootstrap; returned in
     * DHCPOFFER, DHCPACK by server.
     */
    private InetAddress nextServerAddress;

    /**
     * [options] Optional parameters field. See the options documents for a list
     * of defined options.
     */
    private OptionsField options = new OptionsField();

    /**
     * [giaddr] Relay agent IP address, used in booting via a relay agent.
     */
    private InetAddress relayAgentAddress;

    /**
     * [secs] Filled in by client, seconds elapsed since client began address
     * acquisition or renewal process.
     */
    private int seconds;

    /**
     * [sname] Optional server host name, null terminated string.
     */
    private String serverHostname;

    /**
     * [xid] Transaction ID, a random number chosen by the client, used by the
     * client and server to associate messages and responses between a client and
     * a server.
     */
    private int transactionId;

    /**
     * The DHCP message type option.
     */
    private MessageType messageType;

    private HardwareAddress hardwareAddress;


    /**
     * Create a default dhcp message.
     */
    public DhcpMessage()
    {

    }


    /**
     * Create a DHCP message based on the supplied values.
     * 
     * @param messageType
     * @param op
     * @param hardwareAddress
     * @param hops
     * @param transactionId
     * @param seconds
     * @param flags
     * @param currentClientAddress
     * @param assignedClientAddress
     * @param nextServerAddress
     * @param relayAgentAddress
     * @param serverHostname
     * @param bootFileName
     * @param options
     */
    public DhcpMessage( MessageType messageType, byte op,
        HardwareAddress hardwareAddress, short hops, int transactionId,
        int seconds, short flags, InetAddress currentClientAddress,
        InetAddress assignedClientAddress, InetAddress nextServerAddress,
        InetAddress relayAgentAddress, String serverHostname,
        String bootFileName, OptionsField options )
    {
        this.messageType = messageType;
        this.op = op;
        this.hardwareAddress = hardwareAddress;
        this.hopCount = hops;
        this.transactionId = transactionId;
        this.seconds = seconds;
        this.flags = flags;
        this.currentClientAddress = currentClientAddress;
        this.assignedClientAddress = assignedClientAddress;
        this.nextServerAddress = nextServerAddress;
        this.relayAgentAddress = relayAgentAddress;
        this.serverHostname = serverHostname;
        this.bootFileName = bootFileName;
        this.options = options;
    }


    public InetAddress getAssignedClientAddress()
    {
        return assignedClientAddress;
    }


    public String getBootFileName()
    {
        return bootFileName;
    }


    public InetAddress getCurrentClientAddress()
    {
        return currentClientAddress;
    }


    public short getFlags()
    {
        return flags;
    }


    public short getHopCount()
    {
        return hopCount;
    }


    public MessageType getMessageType()
    {
        return messageType;
    }


    public InetAddress getNextServerAddress()
    {
        return nextServerAddress;
    }


    public OptionsField getOptions()
    {
        return options;
    }


    public InetAddress getRelayAgentAddress()
    {
        return relayAgentAddress;
    }


    public int getSeconds()
    {
        return seconds;
    }


    public String getServerHostname()
    {
        return serverHostname;
    }


    public int getTransactionId()
    {
        return transactionId;
    }


    public void setAssignedClientAddress( InetAddress assignedClientAddress )
    {
        this.assignedClientAddress = assignedClientAddress;
    }


    public void setBootFileName( String bootFileName )
    {
        this.bootFileName = bootFileName;
    }


    public void setCurrentClientAddress( InetAddress currentClientAddress )
    {
        this.currentClientAddress = currentClientAddress;
    }


    public void setFlags( short flags )
    {
        this.flags = flags;
    }


    public void setHopCount( short hopCount )
    {
        this.hopCount = hopCount;
    }


    public void setMessageType( MessageType messageType )
    {
        this.messageType = messageType;
    }


    public void setNextServerAddress( InetAddress nextServerAddress )
    {
        this.nextServerAddress = nextServerAddress;
    }


    public void setOptions( OptionsField options )
    {
        this.options = options;
    }


    public void setRelayAgentAddress( InetAddress relayAgentAddress )
    {
        this.relayAgentAddress = relayAgentAddress;
    }


    public void setSeconds( int seconds )
    {
        this.seconds = seconds;
    }


    public void setServerHostname( String serverHostname )
    {
        this.serverHostname = serverHostname;
    }


    public void setTransactionId( int transactionId )
    {
        this.transactionId = transactionId;
    }


    public byte getOp()
    {
        return op;
    }


    public void setOp( byte op )
    {
        this.op = op;
    }


    public HardwareAddress getHardwareAddress()
    {
        return hardwareAddress;
    }


    public void setHardwareAddress( HardwareAddress hardwareAddress )
    {
        this.hardwareAddress = hardwareAddress;
    }


    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append( messageType ).append( ": hwAddress=" ).append( hardwareAddress )
            .append( ", tx=" ).append( transactionId ).append( ", options=" ).append(
                options );

        return sb.toString();
    }
}
