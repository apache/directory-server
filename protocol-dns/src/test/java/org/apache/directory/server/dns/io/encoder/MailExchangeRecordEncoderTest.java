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
 * Tests for the MX record encoder.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class MailExchangeRecordEncoderTest extends AbstractResourceRecordEncoderTest
{
    String mxPreference = "10";
    String mxHost = "mail.apache.org";
    String[] mxParts = mxHost.split( "\\." );


    @Override
    protected Map<String, Object> getAttributes()
    {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put( Strings.toLowerCaseAscii( DnsAttribute.MX_PREFERENCE ), mxPreference );
        map.put( Strings.toLowerCaseAscii( DnsAttribute.DOMAIN_NAME ), mxHost );
        return map;
    }


    @Override
    protected ResourceRecordEncoder getEncoder()
    {
        return new MailExchangeRecordEncoder();
    }


    @Override
    protected void putExpectedResourceData( IoBuffer expectedData )
    {
        expectedData.put( ( byte ) 20 );
        expectedData.putShort( Short.parseShort( mxPreference ) );
        expectedData.put( ( byte ) mxParts[0].length() ); // 1
        expectedData.put( mxParts[0].getBytes( StandardCharsets.UTF_8 ) ); // + 4
        expectedData.put( ( byte ) mxParts[1].length() ); // + 1
        expectedData.put( mxParts[1].getBytes( StandardCharsets.UTF_8 ) ); // + 6
        expectedData.put( ( byte ) mxParts[2].length() ); // + 1
        expectedData.put( mxParts[2].getBytes( StandardCharsets.UTF_8 ) ); // + 3
        expectedData.put( ( byte ) 0x00 ); // + 1 = 17
    }
}
