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
package org.apache.directory.server.kerberos.shared.messages.value;


import java.nio.ByteBuffer;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.directory.shared.ldap.util.StringTools;

import junit.framework.TestCase;


/**
 * Test the PA-ENC-TIMESTAMP encoding and decoding
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev: 542147 $, $Date: 2007-05-28 10:14:21 +0200 (Mon, 28 May 2007) $
 */
public class PreAuthEncryptedTimestampTest extends TestCase
{
    private static Date date = null;
    
    static
    {
        try
        {
            date = new SimpleDateFormat( "yyyyMMddHHmmss'Z'" ).parse( "20070717114503Z" );
        }
        catch ( ParseException pe )
        {
            // Do nothing
        }
    }

    public void testEncodingPreAuthEncryptedTimestamp() throws Exception
    {
        KerberosTime paTimestamp = new KerberosTime( date );

        PreAuthEncryptedTimestamp paet = new PreAuthEncryptedTimestamp( paTimestamp, 128 );
        
        ByteBuffer encoded = ByteBuffer.allocate( paet.computeLength() );

        paet.encode( encoded );

        byte[] expectedResult = new byte[]
            { 
            0x30, 0x19, 
              (byte)0xA0, 0x11,
                0x18, 0x0F,
                  '2', '0', '0', '7', '0', '7', '1', '7', '0', '9', '4', '5', '0', '3', 'Z',
              (byte)0xA1, 0x04, 
                0x02, 0x02, 0x00, (byte)0x80 
            };

        assertEquals( StringTools.dumpBytes( expectedResult ), StringTools.dumpBytes( encoded.array() ) );
    }
}
