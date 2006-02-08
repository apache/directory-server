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


public class DhcpMessage
{
	private MessageType messageType;
	
	private byte  opCode;
	private byte  hardwareAddressType;
	private byte  hardwareAddressLength;
	private byte  hardwareOptions;
	private int   transactionId;
	private short seconds;
	private short flags;
	private byte  actualClientAddress[]   = new byte[4];
	private byte  assignedClientAddress[] = new byte[4];
	private byte  nextServerAddress[]     = new byte[4];
	private byte  relayAgentAddress[]     = new byte[4];
	private byte  clientHardwareAddress[] = new byte[16];
	private byte  serverHostname[]        = new byte[64];
	private byte  bootFileName[]          = new byte[128];
	
	private OptionsField options = new OptionsField();
	
	public DhcpMessage( MessageType messageType, byte opCode, byte hardwareAddressType,
			byte hardwareAddressLength, byte hardwareOptions, int transactionId,
			short seconds, short flags, byte[] actualClientAddress,
			byte[] assignedClientAddress, byte[] nextServerAddress, byte[] relayAgentAddress,
			byte[] clientHardwareAddress, byte[] serverHostname, byte[] bootFileName,
			OptionsField options )
	{
		this.messageType           = messageType;
		this.opCode                = opCode;
		this.hardwareAddressType   = hardwareAddressType;
		this.hardwareAddressLength = hardwareAddressLength;
		this.hardwareOptions       = hardwareOptions;
		this.transactionId         = transactionId;
		this.seconds               = seconds;
		this.flags                 = flags;
		this.actualClientAddress   = actualClientAddress;
		this.assignedClientAddress = assignedClientAddress;
		this.nextServerAddress     = nextServerAddress;
		this.relayAgentAddress     = relayAgentAddress;
		this.clientHardwareAddress = clientHardwareAddress;
		this.serverHostname        = serverHostname;
		this.bootFileName          = bootFileName;
		this.options               = options;
	}
	
	/**
	 * Message type.
	 */
	public MessageType getMessageType()
	{
		return messageType;
	}
	
	/**
	 * Message op code / message type.
	 * 1 = BOOTREQUEST, 2 = BOOTREPLY
	 */
	public byte getOpCode()
	{
		return opCode;
	}

	/**
	 * Hardware address type, see ARP section in
	 * "Assigned Numbers" RFC; e.g., '1' = 10mb ethernet.
	 */
	public byte getHardwareAddressType()
	{
		return hardwareAddressType;
	}

	/**
	 * Hardware address length (e.g.  '6' for 10mb ethernet).
	 */
	public byte getHardwareAddressLength()
	{
		return hardwareAddressLength;
	}

	/**
	 * Client sets to zero, optionally used by relay agents
	 * when booting via a relay agent.
	 */
	public byte getHardwareOptions()
	{
		return hardwareOptions;
	}

	/**
	 * Transaction ID, a random number chosen by the client,
	 * used by the client and server to associate messages
	 * and responses between a client and a server.
	 */
	public int getTransactionId()
	{
		return transactionId;
	}

	/**
	 * Filled in by client, seconds elapsed since client
	 * began address acquisition or renewal process.
	 */
	public short getSeconds()
	{
		return seconds;
	}

	/**
	 * Flags.
	 */
	public short getFlags()
	{
		return flags;
	}

	/**
	 * Client IP address; only filled in if client is in BOUND,
	 * RENEW or REBINDING state and can respond to ARP requests.
	 */
	public byte[] getActualClientAddress()
	{
		return actualClientAddress;
	}

	/**
	 * Get 'your' (client) IP address.
	 */
	public byte[] getAssignedClientAddress()
	{
		return assignedClientAddress;
	}

	/**
	 * IP address of next server to use in bootstrap;
	 * returned in DHCPOFFER, DHCPACK by server.
	 */
	public byte[] getNextServerAddress()
	{
		return nextServerAddress;
	}

	/**
	 * Relay agent IP address, used in booting via a relay agent.
	 */
	public byte[] getRelayAgentAddress()
	{
		return relayAgentAddress;
	}

	/**
	 * Client hardware address.
	 */
	public byte[] getClientHardwareAddress()
	{
		return clientHardwareAddress;
	}

	/**
	 * Optional server host name, null terminated string.
	 */
	public byte[] getServerHostname()
	{
		return serverHostname;
	}

	/**
	 * Boot file name, null terminated string; "generic" name or null
	 * in DHCPDISCOVER, fully qualified directory-path name in DHCPOFFER.
	 */
	public byte[] getBootFileName()
	{
		return bootFileName;
	}
	
	/**
	 * Optional parameters field.  See the options
	 * documents for a list of defined options.
	 */
	public OptionsField getOptions()
	{
		return options;
	}
}

