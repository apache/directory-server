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
 * Tests for the SRV record encoder.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev: 501160 $, $Date: 2007-01-29 12:41:33 -0700 (Mon, 29 Jan 2007) $
 */
public class ServerSelectionRecordEncoderTest extends AbstractResourceRecordEncoderTest
{
    
    String priority = "0";
    String weight = "3";
    String port = "9";
    String srvName = "srv.apache.org";
    String[] srvParts = srvName.split( "\\." );

    @Override
    protected Map getAttributes()
    {
        Map map = new HashMap();
        map.put( DnsAttribute.SERVICE_PRIORITY.toLowerCase(), priority );
        map.put( DnsAttribute.SERVICE_WEIGHT.toLowerCase(), weight );
        map.put( DnsAttribute.SERVICE_PORT.toLowerCase(), port );
        map.put( DnsAttribute.DOMAIN_NAME.toLowerCase(), srvName );
        return map;
    }

    @Override
    protected ResourceRecordEncoder getEncoder()
    {
        return new ServerSelectionRecordEncoder();
    }

    @Override
    protected void putExpectedResourceData( ByteBuffer expectedData )
    {
        expectedData.put( ( byte ) 22 );
        expectedData.putShort( Short.parseShort( priority ) );
        expectedData.putShort( Short.parseShort( weight ) );
        expectedData.putShort( Short.parseShort( port ) );
        expectedData.put( ( byte ) 16 );
        expectedData.put( ( byte ) srvParts[0].length() );  // 1
        expectedData.put( srvParts[0].getBytes() );         // + 3
        expectedData.put( ( byte ) srvParts[1].length() );  // + 1
        expectedData.put( srvParts[1].getBytes() );         // + 6
        expectedData.put( ( byte ) srvParts[2].length() );  // + 1
        expectedData.put( srvParts[2].getBytes() );         // + 3
        expectedData.put( ( byte ) 0x00 );                  // + 1 = 16
    }

}
