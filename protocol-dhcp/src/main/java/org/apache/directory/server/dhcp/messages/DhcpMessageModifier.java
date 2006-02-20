/*
 *   Copyright 2005 The Apache Software Foundation
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
package org.apache.directory.server.dhcp.messages;


import org.apache.directory.server.dhcp.options.OptionsField;


public class DhcpMessageModifier
{
    private MessageType messageType;

    private byte opCode;
    private byte hardwareAddressType;
    private byte hardwareAddressLength;
    private byte hardwareOptions;
    private int transactionId;
    private short seconds;
    private short flags;
    private byte actualClientAddress[] = new byte[4];
    private byte assignedClientAddress[] = new byte[4];
    private byte nextServerAddress[] = new byte[4];
    private byte relayAgentAddress[] = new byte[4];
    private byte clientHardwareAddress[] = new byte[16];
    private byte serverHostname[] = new byte[64];
    private byte bootFileName[] = new byte[128];

    private OptionsField options = new OptionsField();


    public DhcpMessage getDhcpMessage()
    {
        return new DhcpMessage( messageType, opCode, hardwareAddressType, hardwareAddressLength, hardwareOptions,
            transactionId, seconds, flags, actualClientAddress, assignedClientAddress, nextServerAddress,
            relayAgentAddress, clientHardwareAddress, serverHostname, bootFileName, options );
    }


    /**
     * Message type.
     */
    public void setMessageType( MessageType messageType )
    {
        this.messageType = messageType;
    }


    /**
     * Message op code / message type.
     * 1 = BOOTREQUEST, 2 = BOOTREPLY
     */
    public void setOpCode( byte opCode )
    {
        this.opCode = opCode;
    }


    /**
     * Hardware address type, see ARP section in
     * "Assigned Numbers" RFC; e.g., '1' = 10mb ethernet.
     */
    public void setHardwareAddressType( byte hardwareAddressType )
    {
        this.hardwareAddressType = hardwareAddressType;
    }


    /**
     * Hardware address length (e.g.  '6' for 10mb ethernet).
     */
    public void setHardwareAddressLength( byte hardwareAddressLength )
    {
        this.hardwareAddressLength = hardwareAddressLength;
    }


    /**
     * Set hops field.
     * 
     * @param hardwareOptions hops field
     */
    public void setHardwareOptions( byte hardwareOptions )
    {
        this.hardwareOptions = hardwareOptions;
    }


    /**
     * Transaction ID, a random number chosen by the client,
     * used by the client and server to associate messages
     * and responses between a client and a server.
     */
    public void setTransactionId( int transactionId )
    {
        this.transactionId = transactionId;
    }


    /**
     * Filled in by client, seconds elapsed since client
     * began address acquisition or renewal process.
     */
    public void setSeconds( short seconds )
    {
        this.seconds = seconds;
    }


    /**
     * Flags.
     */
    public void setFlags( short flags )
    {
        this.flags = flags;
    }


    /**
     * Client IP address; only filled in if client is in BOUND,
     * RENEW or REBINDING state and can respond to ARP requests.
     */
    public void setActualClientAddress( byte[] actualClientAddress )
    {
        this.actualClientAddress = actualClientAddress;
    }


    /**
     * Get 'your' (client) IP address.
     */
    public void setAssignedClientAddress( byte[] assignedClientAddress )
    {
        this.assignedClientAddress = assignedClientAddress;
    }


    /**
     * IP address of next server to use in bootstrap;
     * returned in DHCPOFFER, DHCPACK by server.
     */
    public void setNextServerAddress( byte[] nextServerAddress )
    {
        this.nextServerAddress = nextServerAddress;
    }


    /**
     * Relay agent IP address, used in booting via a relay agent.
     */
    public void setRelayAgentAddress( byte[] relayAgentAddress )
    {
        this.relayAgentAddress = relayAgentAddress;
    }


    /**
     * Client hardware address.
     */
    public void setClientHardwareAddress( byte[] clientHardwareAddress )
    {
        this.clientHardwareAddress = clientHardwareAddress;
    }


    /**
     * Optional server host name, null terminated string.
     */
    public void setServerHostname( byte[] serverHostname )
    {
        this.serverHostname = serverHostname;
    }


    /**
     * Boot file name, null terminated string; "generic" name or null
     * in DHCPDISCOVER, fully qualified directory-path name in DHCPOFFER.
     */
    public void setBootFileName( byte[] bootFileName )
    {
        this.bootFileName = bootFileName;
    }


    /**
     * Optional parameters field.  See the options
     * documents for a list of defined options.
     */
    public void setOptions( OptionsField options )
    {
        this.options = options;
    }
}
