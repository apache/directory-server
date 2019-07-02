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


import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import org.apache.directory.api.util.Strings;
import org.apache.directory.server.dns.store.DnsAttribute;
import org.apache.mina.core.buffer.IoBuffer;


/**
 * Tests for the SOA record encoder.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
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


    @Override
    protected Map<String, Object> getAttributes()
    {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put( Strings.toLowerCaseAscii( DnsAttribute.SOA_M_NAME ), mName );
        map.put( Strings.toLowerCaseAscii( DnsAttribute.SOA_R_NAME ), rName );
        map.put( Strings.toLowerCaseAscii( DnsAttribute.SOA_SERIAL ), serial );
        map.put( Strings.toLowerCaseAscii( DnsAttribute.SOA_REFRESH ), refresh );
        map.put( Strings.toLowerCaseAscii( DnsAttribute.SOA_RETRY ), retry );
        map.put( Strings.toLowerCaseAscii( DnsAttribute.SOA_EXPIRE ), expire );
        map.put( Strings.toLowerCaseAscii( DnsAttribute.SOA_MINIMUM ), minimum );

        return map;
    }


    @Override
    protected ResourceRecordEncoder getEncoder()
    {
        return new StartOfAuthorityRecordEncoder();
    }


    @Override
    protected void putExpectedResourceData( IoBuffer expectedData )
    {
        expectedData.put( ( byte ) 60 ); // 1 + 18 + 1 + 20 + 4 + 4 + 4 + 4 + 4
        expectedData.put( ( byte ) mNameParts[0].length() ); // 1
        expectedData.put( mNameParts[0].getBytes( StandardCharsets.UTF_8 ) ); // + 2
        expectedData.put( ( byte ) mNameParts[1].length() ); // + 1
        expectedData.put( mNameParts[1].getBytes( StandardCharsets.UTF_8 ) ); // + 9
        expectedData.put( ( byte ) mNameParts[2].length() ); // + 1
        expectedData.put( mNameParts[2].getBytes( StandardCharsets.UTF_8 ) ); // + 3
        expectedData.put( ( byte ) 0x00 ); // + 1 = 18
        expectedData.put( ( byte ) rNameParts[0].length() ); // 1
        expectedData.put( rNameParts[0].getBytes( StandardCharsets.UTF_8 ) ); // + 4
        expectedData.put( ( byte ) rNameParts[1].length() ); // + 1
        expectedData.put( rNameParts[1].getBytes( StandardCharsets.UTF_8 ) ); // + 9
        expectedData.put( ( byte ) rNameParts[2].length() ); // + 1
        expectedData.put( rNameParts[2].getBytes( StandardCharsets.UTF_8 ) ); // + 3
        expectedData.put( ( byte ) 0x00 ); // + 1 = 20
        expectedData.putInt( ( int ) Long.parseLong( serial ) );
        expectedData.putInt( Integer.parseInt( refresh ) );
        expectedData.putInt( Integer.parseInt( retry ) );
        expectedData.putInt( Integer.parseInt( expire ) );
        expectedData.putInt( ( int ) Long.parseLong( minimum ) );
    }
}
