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

import org.apache.directory.api.util.Strings;
import org.apache.directory.server.dns.store.DnsAttribute;
import org.apache.mina.core.buffer.IoBuffer;


/**
 * Tests for the PTR record encoder.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class PointerRecordEncoderTest extends AbstractResourceRecordEncoderTest
{
    String ptrName = "ptr.apache.org";
    String[] ptrParts = ptrName.split( "\\." );


    @Override
    protected Map<String, Object> getAttributes()
    {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put( Strings.toLowerCase( DnsAttribute.DOMAIN_NAME ), ptrName );
        return map;
    }


    @Override
    protected ResourceRecordEncoder getEncoder()
    {
        return new PointerRecordEncoder();
    }


    @Override
    protected void putExpectedResourceData( IoBuffer expectedData )
    {
        expectedData.put( ( byte ) 15 );
        expectedData.put( ( byte ) ptrParts[0].length() ); // 1
        expectedData.put( ptrParts[0].getBytes() ); // + 3
        expectedData.put( ( byte ) ptrParts[1].length() ); // + 1
        expectedData.put( ptrParts[1].getBytes() ); // + 6
        expectedData.put( ( byte ) ptrParts[2].length() ); // + 1
        expectedData.put( ptrParts[2].getBytes() ); // + 3
        expectedData.put( ( byte ) 0x00 ); // + 1 = 15
    }
}
