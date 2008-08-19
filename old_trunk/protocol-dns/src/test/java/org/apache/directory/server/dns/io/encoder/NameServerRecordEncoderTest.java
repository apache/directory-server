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
 * Tests for the NS record encoder.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev: 501160 $, $Date: 2007-01-29 12:41:33 -0700 (Mon, 29 Jan 2007) $
 */
public class NameServerRecordEncoderTest extends AbstractResourceRecordEncoderTest
{
    String nsName = "ns1.eu.bitnames.com";
    String[] nsParts = nsName.split( "\\." );


    protected Map getAttributes()
    {
        Map map = new HashMap();
        map.put( DnsAttribute.DOMAIN_NAME.toLowerCase(), nsName );
        return map;
    }


    protected ResourceRecordEncoder getEncoder()
    {
        return new NameServerRecordEncoder();
    }


    protected void putExpectedResourceData( ByteBuffer expectedData )
    {
        expectedData.put( ( byte ) 19 );
        expectedData.put( ( byte ) nsParts[0].length() ); // 1
        expectedData.put( nsParts[0].getBytes() ); // + 3
        expectedData.put( ( byte ) nsParts[1].length() ); // + 1
        expectedData.put( nsParts[1].getBytes() ); // + 2
        expectedData.put( ( byte ) nsParts[2].length() ); // + 1
        expectedData.put( nsParts[2].getBytes() ); // + 7
        expectedData.put( ( byte ) nsParts[3].length() ); // + 1
        expectedData.put( nsParts[3].getBytes() ); // + 3
        expectedData.put( ( byte ) 0x00 ); // + 1 = 19
    }
}
