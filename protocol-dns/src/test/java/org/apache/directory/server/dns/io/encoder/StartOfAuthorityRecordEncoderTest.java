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

package org.apache.directory.server.dns.io.encoder;


import java.util.HashMap;
import java.util.Map;

import org.apache.directory.server.dns.store.DnsAttribute;
import org.apache.mina.common.ByteBuffer;


/**
 * Tests for the SOA record encoder.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev: 501160 $, $Date: 2007-01-29 12:41:33 -0700 (Mon, 29 Jan 2007) $
 */
public class StartOfAuthorityRecordEncoderTest extends AbstractResourceRecordEncoderTest
{
    String mName = "ns.hyperreal.org";
    String[] mNameParts = mName.split( "\\." );
    String rName = "root.hyperreal.org";
    String[] rNameParts = rName.split( "\\." );
    String serial = "2007013001";
    String refresh = "3600";
    String retry = "900";
    String expire = "604800";
    String minimum = "3600";
    
    protected Map getAttributes()
    {
        Map map = new HashMap();
        map.put( DnsAttribute.SOA_M_NAME.toLowerCase(), mName );
        map.put( DnsAttribute.SOA_R_NAME.toLowerCase(), rName );
        map.put( DnsAttribute.SOA_SERIAL.toLowerCase(), serial );
        map.put( DnsAttribute.SOA_REFRESH.toLowerCase(), refresh );
        map.put( DnsAttribute.SOA_RETRY.toLowerCase(), retry );
        map.put( DnsAttribute.SOA_EXPIRE.toLowerCase(), expire );
        map.put( DnsAttribute.SOA_MINIMUM.toLowerCase(), minimum );
        return map;
    }


    protected ResourceRecordEncoder getEncoder()
    {
        return new StartOfAuthorityRecordEncoder();
    }


    protected void putExpectedResourceData( ByteBuffer expectedData )
    {
        expectedData.put( ( byte ) 60 );   // 1 + 18 + 1 + 20 + 4 + 4 + 4 + 4 + 4
        expectedData.put( ( byte ) mNameParts[0].length() );    // 1
        expectedData.put( mNameParts[0].getBytes() );           // + 2
        expectedData.put( ( byte ) mNameParts[1].length() );    // + 1
        expectedData.put( mNameParts[1].getBytes() );           // + 9
        expectedData.put( ( byte ) mNameParts[2].length() );    // + 1
        expectedData.put( mNameParts[2].getBytes() );           // + 3
        expectedData.put( ( byte  ) 0x00 );                     // + 1 = 18
        expectedData.put( ( byte ) rNameParts[0].length() );    // 1
        expectedData.put( rNameParts[0].getBytes() );           // + 4
        expectedData.put( ( byte ) rNameParts[1].length() );    // + 1
        expectedData.put( rNameParts[1].getBytes() );           // + 9
        expectedData.put( ( byte ) rNameParts[2].length() );    // + 1
        expectedData.put( rNameParts[2].getBytes() );           // + 3
        expectedData.put( ( byte  ) 0x00 );                     // + 1 = 20
        expectedData.putInt( ( int ) Long.parseLong( serial ) );
        expectedData.putInt( Integer.parseInt( refresh ) );
        expectedData.putInt( Integer.parseInt( retry ) );
        expectedData.putInt( Integer.parseInt( expire ) );
        expectedData.putInt( ( int ) Long.parseLong( minimum ) );
    }

}
