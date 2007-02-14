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


import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;

import org.apache.directory.server.dns.store.DnsAttribute;
import org.apache.mina.common.ByteBuffer;

/**
 * Tests for the A record encoder.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev: 501160 $, $Date: 2007-01-29 12:41:33 -0700 (Mon, 29 Jan 2007) $
 */
public class AddressRecordEncoderTest extends AbstractResourceRecordEncoderTest
{

    InetAddress address;

    
    protected void setUpResourceData()
    {
        try {
            address = InetAddress.getByName( "127.0.0.1" );
        } catch (UnknownHostException e)
        {
            // should never happen
        }
    }


    protected Map getAttributes()
    {
        Map attributes = new HashMap();
        attributes.put( DnsAttribute.IP_ADDRESS, address );
        return attributes;
    }
    

    protected ResourceRecordEncoder getEncoder()
    {
        return new AddressRecordEncoder();
    }


    protected void putExpectedResourceData( ByteBuffer expectedData )
    {
        expectedData.put( ( byte ) address.getAddress().length );
        expectedData.put( address.getAddress() );
    }
}
