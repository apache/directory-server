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

package org.apache.dhcp.io;

import java.nio.ByteBuffer;
import java.util.Arrays;

import org.apache.dhcp.DhcpException;
import org.apache.dhcp.options.DhcpOption;
import org.apache.dhcp.options.OptionsField;
import org.apache.dhcp.options.dhcp.DhcpMessageType;
import org.apache.dhcp.options.dhcp.IpAddressLeaseTime;
import org.apache.dhcp.options.dhcp.ParameterRequestList;
import org.apache.dhcp.options.dhcp.RequestedIpAddress;
import org.apache.dhcp.options.dhcp.ServerIdentifier;
import org.apache.dhcp.options.vendor.DomainName;
import org.apache.dhcp.options.vendor.DomainNameServers;
import org.apache.dhcp.options.vendor.EndOption;
import org.apache.dhcp.options.vendor.PadOption;
import org.apache.dhcp.options.vendor.SubnetMask;
import org.apache.dhcp.options.vendor.TimeOffset;


public class DhcpOptionsDecoder
{
	private static final byte[] VENDOR_MAGIC_COOKIE =
			{ (byte) 99, (byte) 130, (byte) 83, (byte) 99 };
	
	public OptionsField decode( ByteBuffer message ) throws DhcpException
	{
		byte[] magicCookie = new byte[ 4 ];
		message.get( magicCookie );
		
		if ( !Arrays.equals( VENDOR_MAGIC_COOKIE, magicCookie ) )
		{
			throw new DhcpException("Parse exception.");
		}
		
		byte code;
		byte length;
		byte value[];
		
		OptionsField options = new OptionsField();

		while ( message.get( message.position() ) != (byte) 255 )
		{
			code = message.get();
			length = message.get();
			value = new byte[ length ];
			message.get( value );
			
			options.add( getInstance( code, value ) );
		}
		
		return options;
	}
	
	private DhcpOption getInstance( int tag, byte[] value)
		throws DhcpException
	{
		switch (tag)
		{
			case 0:
				return new PadOption();
			case 1:
				return new EndOption();
			case 2:
				return new SubnetMask( value );
			case 3:
				return new TimeOffset( value );
			case 6:
				return new DomainNameServers( value );
			case 15:
				return new DomainName( value );
			case 50:
				return new RequestedIpAddress( value );
			case 51:
				return new IpAddressLeaseTime( value );
			case 53:
				return new DhcpMessageType( value );
			case 54:
				return new ServerIdentifier( value );
			case 55:
				return new ParameterRequestList( value );
			default:
				throw new DhcpException( "Unsupported or bad option code:  " + tag );
		}
	}
}

