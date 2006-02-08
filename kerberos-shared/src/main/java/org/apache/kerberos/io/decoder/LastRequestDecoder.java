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
package org.apache.kerberos.io.decoder;

import java.util.Enumeration;

import org.apache.directory.shared.asn1.der.DEREncodable;
import org.apache.directory.shared.asn1.der.DERGeneralizedTime;
import org.apache.directory.shared.asn1.der.DERInteger;
import org.apache.directory.shared.asn1.der.DERSequence;
import org.apache.directory.shared.asn1.der.DERTaggedObject;
import org.apache.kerberos.messages.value.KerberosTime;
import org.apache.kerberos.messages.value.LastRequest;
import org.apache.kerberos.messages.value.LastRequestEntry;
import org.apache.kerberos.messages.value.LastRequestType;

public class LastRequestDecoder
{
	/**
	 * LastReq ::=   SEQUENCE OF SEQUENCE {
	 * lr-type[0]               INTEGER,
	 * lr-value[1]              KerberosTime
	 * }
	 */
	protected LastRequest decodeSequence( DERSequence sequence )
	{
		LastRequestEntry[] entries = new LastRequestEntry[ sequence.size() ];
		
		int ii = 0;
		for ( Enumeration e = sequence.getObjects(); e.hasMoreElements(); )
		{
			DERSequence object = (DERSequence) e.nextElement();
			LastRequestEntry entry = decode( object );
			entries[ii] = entry;
			ii++;
		}
		
		return new LastRequest( entries );
	}
	
	protected LastRequestEntry decode( DERSequence sequence )
	{
		LastRequestType type = LastRequestType.NONE;
		KerberosTime value = null;
		
		for ( Enumeration e = sequence.getObjects(); e.hasMoreElements(); )
		{
			DERTaggedObject object = (DERTaggedObject) e.nextElement();
			int tag = object.getTagNo();
			DEREncodable derObject = object.getObject();
			
			switch ( tag )
			{
				case 0:
					DERInteger tag0 = (DERInteger)derObject;
					type = LastRequestType.getTypeByOrdinal( tag0.intValue() );
					break;
				case 1:
					DERGeneralizedTime tag1 = (DERGeneralizedTime)derObject;
					value = KerberosTimeDecoder.decode( tag1 );
					break;
			}
		}
		
		return new LastRequestEntry( type, value );
	}
}
