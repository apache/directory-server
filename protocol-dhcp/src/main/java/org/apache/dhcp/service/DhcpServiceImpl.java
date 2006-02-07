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

package org.apache.dhcp.service;

import org.apache.dhcp.DhcpService;
import org.apache.dhcp.messages.DhcpMessage;
import org.apache.dhcp.messages.DhcpMessageModifier;
import org.apache.dhcp.messages.MessageType;

/**
 * DHCP Protocol (RFC 2131, RFC 2132)
 */
public class DhcpServiceImpl implements DhcpService
{
	public DhcpMessage getReplyFor( DhcpMessage request )
    {
		DhcpMessageModifier modifier = new DhcpMessageModifier();
		
		modifier.setMessageType( MessageType.DHCPOFFER );
		modifier.setOpCode( (byte)0x02 );
		modifier.setHardwareAddressType( (byte)0x00 );
		modifier.setHardwareAddressLength( (byte)0xFF );
		modifier.setHardwareOptions( (byte)0x00 );
		modifier.setTransactionId( request.getTransactionId() );
		modifier.setSeconds( (short)0 );
		modifier.setFlags( (short)0 );
		
		byte[] actual = { (byte)0, (byte)0, (byte)0, (byte)0 };
		modifier.setActualClientAddress( actual );
		
		byte[] assigned = { (byte)192, (byte)168, (byte)0, (byte)20 };
		modifier.setAssignedClientAddress( assigned );
		
		byte[] unused = { (byte)0, (byte)0, (byte)0, (byte)0 };
		modifier.setNextServerAddress( unused );
		modifier.setRelayAgentAddress( unused );
		
		modifier.setClientHardwareAddress( request.getClientHardwareAddress() );
		modifier.setServerHostname( request.getServerHostname() );
		modifier.setBootFileName( request.getBootFileName() );
		
		DhcpMessage reply = modifier.getDhcpMessage();
		
		return reply;
	}
}

