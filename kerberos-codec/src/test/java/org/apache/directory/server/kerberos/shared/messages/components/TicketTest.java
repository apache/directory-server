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
package org.apache.directory.server.kerberos.shared.messages.components;

import static org.junit.Assert.assertEquals;

import javax.security.auth.kerberos.KerberosPrincipal;

import org.apache.directory.junit.tools.Concurrent;
import org.apache.directory.junit.tools.ConcurrentJunitRunner;
import org.apache.directory.server.kerberos.shared.store.TicketFactory;
import org.apache.directory.shared.kerberos.components.EncryptionKey;
import org.apache.directory.shared.kerberos.messages.Ticket;
import org.apache.directory.shared.util.Strings;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Test the Ticket encoding and decoding
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
@RunWith(ConcurrentJunitRunner.class)
@Concurrent()
public class TicketTest
{
    @Test
    @Ignore
    public void testTicket() throws Exception
    {
        TicketFactory ticketFactory = new TicketFactory();

        KerberosPrincipal clientPrincipal = new KerberosPrincipal( "hnelson@EXAMPLE.COM" );
        KerberosPrincipal serverPrincipal = new KerberosPrincipal( "kadmin/changepw@EXAMPLE.COM" );
        String serverPassword = "s3crEt";

        EncryptionKey serverKey = ticketFactory.getServerKey( serverPrincipal, serverPassword );

        Ticket serviceTicket = ticketFactory.getTicket( clientPrincipal, serverPrincipal, serverKey );

        byte[] encodedTicket = serviceTicket.encode( null ).array();
        
        byte[] expectedResult = new byte[]
            {
              0x61, (byte)0x81, (byte)0xF7,
                0x30,  (byte)0x81, (byte)0xF4, 
                  (byte)0xA0, 0x03,
                    0x02, 0x01, 0x05,
                  (byte)0xA1, 0x0D,
                    0x1B, 0x0B, 
                      'E', 'X', 'A', 'M', 'P', 'L', 'E', '.', 'C', 'O', 'M',
                  (byte)0xA2, 0x1D,
                    0x30, 0x1B,
                      (byte)0xA0, 0x03, 
                        0x02, 0x01, 0x01, 
                      (byte)0xA1, 0x14, 
                        0x30, 0x12, 
                          0x1B, 0x06, 
                            'k', 'a', 'd', 'm', 'i', 'n',
                          0x1B, 0x08,
                            'c', 'h', 'a', 'n', 'g', 'e', 'p', 'w',
                  (byte)0xA3, (byte)0x81, (byte)0xBE, 
                    0x30, (byte)0x81, (byte)0xBB,
                      (byte)0xA0, 0x03,
                        0x02, 0x01, 0x03,
                      (byte)0xA2, (byte)0x81, (byte)0xB3,
                        0x04, (byte)0x81, (byte)0xB0
            };

        // We will just compared the first bytes (everything before the encrypted data)
        String expectedResultString = Strings.dumpBytes(expectedResult);
        String resultString = Strings.dumpBytes(encodedTicket).substring( 0,  expectedResultString.length() );
        
        assertEquals( expectedResultString, resultString );
    }

    /*
    public void testTicketPerf() throws Exception
    {
        TicketFactory ticketFactory = new TicketFactory();

        KerberosPrincipal clientPrincipal = new KerberosPrincipal( "hnelson@EXAMPLE.COM" );
        KerberosPrincipal serverPrincipal = new KerberosPrincipal( "kadmin/changepw@EXAMPLE.COM" );
        String serverPassword = "s3crEt";

        EncryptionKey serverKey = ticketFactory.getServerKey( serverPrincipal, serverPassword );

        Ticket serviceTicket = ticketFactory.getTicket( clientPrincipal, serverPrincipal, serverKey );

        byte[] encodedTicket = TicketEncoder.encodeTicket( serviceTicket );
        
        long t0 = System.currentTimeMillis();
        
        for ( int i=0; i < 1000000; i++ )
        {
            TicketEncoder.encodeTicket( serviceTicket );
        }
        
        long t1 = System.currentTimeMillis();
        
        System.out.println( "Delta slow = " + ( t1 - t0 ) );

        long t2 = System.currentTimeMillis();
        
        for ( int i=0; i < 1000000; i++ )
        {
            serviceTicket.encode();
        }
        
        long t3 = System.currentTimeMillis();
        
        System.out.println( "Delta slow = " + ( t3 - t2 ) );

        assertTrue( Arrays.equals( encodedTicket, encodedTicket ) );
    }
    */
}
