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
package org.apache.directory.server.kerberos.shared.keytab;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.nio.ByteBuffer;
import java.security.InvalidKeyException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.crypto.spec.DESKeySpec;

import org.apache.directory.server.kerberos.shared.crypto.encryption.KerberosKeyFactory;
import org.apache.directory.shared.kerberos.KerberosTime;
    import org.apache.directory.shared.kerberos.codec.types.EncryptionType;
import org.apache.directory.shared.kerberos.components.EncryptionKey;
import org.junit.jupiter.api.Test;



/**
 * Tests 'keytab' formatted files.
 * 
 * All values are in network byte order.  All text is ASCII.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class KeytabTest
{
    private static final byte[] keytab1 = new byte[]
        { ( byte ) 0x05, ( byte ) 0x02, ( byte ) 0x00, ( byte ) 0x00, ( byte ) 0x00, ( byte ) 0x3C, ( byte ) 0x00,
            ( byte ) 0x02, ( byte ) 0x00, ( byte ) 0x0B, ( byte ) 0x45, ( byte ) 0x58, ( byte ) 0x41, ( byte ) 0x4D,
            ( byte ) 0x50, ( byte ) 0x4C, ( byte ) 0x45, ( byte ) 0x2E, ( byte ) 0x43, ( byte ) 0x4F, ( byte ) 0x4D,
            ( byte ) 0x00, ( byte ) 0x04, ( byte ) 0x6C, ( byte ) 0x64, ( byte ) 0x61, ( byte ) 0x70, ( byte ) 0x00,
            ( byte ) 0x10, ( byte ) 0x77, ( byte ) 0x77, ( byte ) 0x77, ( byte ) 0x2E, ( byte ) 0x76, ( byte ) 0x65,
            ( byte ) 0x72, ( byte ) 0x69, ( byte ) 0x73, ( byte ) 0x69, ( byte ) 0x67, ( byte ) 0x6E, ( byte ) 0x2E,
            ( byte ) 0x63, ( byte ) 0x6F, ( byte ) 0x6D, ( byte ) 0x00, ( byte ) 0x00, ( byte ) 0x00, ( byte ) 0x01,
            ( byte ) 0x45, ( byte ) 0xD9, ( byte ) 0x60, ( byte ) 0xBE, ( byte ) 0x01, ( byte ) 0x00, ( byte ) 0x03,
            ( byte ) 0x00, ( byte ) 0x08, ( byte ) 0xD5, ( byte ) 0xE6, ( byte ) 0xC4, ( byte ) 0xD0, ( byte ) 0xFE,
            ( byte ) 0x25, ( byte ) 0x07, ( byte ) 0x0D };

    private static final byte[] keytab2 = new byte[]
        { ( byte ) 0x05, ( byte ) 0x02, ( byte ) 0x00, ( byte ) 0x00, ( byte ) 0x00, ( byte ) 0x3C, ( byte ) 0x00,
            ( byte ) 0x02, ( byte ) 0x00, ( byte ) 0x0B, ( byte ) 0x45, ( byte ) 0x58, ( byte ) 0x41, ( byte ) 0x4D,
            ( byte ) 0x50, ( byte ) 0x4C, ( byte ) 0x45, ( byte ) 0x2E, ( byte ) 0x43, ( byte ) 0x4F, ( byte ) 0x4D,
            ( byte ) 0x00, ( byte ) 0x04, ( byte ) 0x48, ( byte ) 0x54, ( byte ) 0x54, ( byte ) 0x50, ( byte ) 0x00,
            ( byte ) 0x10, ( byte ) 0x77, ( byte ) 0x77, ( byte ) 0x77, ( byte ) 0x2E, ( byte ) 0x76, ( byte ) 0x65,
            ( byte ) 0x72, ( byte ) 0x69, ( byte ) 0x73, ( byte ) 0x69, ( byte ) 0x67, ( byte ) 0x6E, ( byte ) 0x2E,
            ( byte ) 0x63, ( byte ) 0x6F, ( byte ) 0x6D, ( byte ) 0x00, ( byte ) 0x00, ( byte ) 0x00, ( byte ) 0x01,
            ( byte ) 0x45, ( byte ) 0xD7, ( byte ) 0x96, ( byte ) 0x79, ( byte ) 0x04, ( byte ) 0x00, ( byte ) 0x03,
            ( byte ) 0x00, ( byte ) 0x08, ( byte ) 0x13, ( byte ) 0xD9, ( byte ) 0x19, ( byte ) 0x98, ( byte ) 0x23,
            ( byte ) 0x8F, ( byte ) 0x9E, ( byte ) 0x31 };

    /** Defines a default date format with a "yyyyMMddHHmmss'Z'" pattern */
    private static final SimpleDateFormat UTC_DATE_FORMAT = new SimpleDateFormat( "yyyyMMddHHmmss'Z'", Locale.ROOT );

    /**
     * Read the first keytab test bytes and check for the presence of a valid DES key.
     *
     * @throws Exception
     */
    @Test
    public void testReadKeytab1() throws Exception
    {
        Keytab keytab = Keytab.read( keytab1 );

        assertTrue( "Keytab version", Arrays.equals( Keytab.VERSION_0X502_BYTES, keytab.getKeytabVersion() ) );
        assertEquals( "Entries size", 1, keytab.getEntries().size() );

        KeytabEntry entry = keytab.getEntries().get( 0 );
        EncryptionKey key = entry.getKey();

        try
        {
            assertTrue( DESKeySpec.isParityAdjusted( key.getKeyValue(), 0 ) );
        }
        catch ( InvalidKeyException ike )
        {
            fail( "Key is invalid." );
        }
    }


    /**
     * Read the second keytab test bytes and check for the presence of a valid DES key.
     *
     * @throws Exception
     */
    @Test
    public void testReadKeytab2() throws Exception
    {
        Keytab keytab = Keytab.read( keytab2 );

        assertTrue( "Keytab version", Arrays.equals( Keytab.VERSION_0X502_BYTES, keytab.getKeytabVersion() ) );
        assertEquals( "Entries size", 1, keytab.getEntries().size() );

        KeytabEntry entry = keytab.getEntries().get( 0 );
        EncryptionKey key = entry.getKey();

        try
        {
            assertTrue( DESKeySpec.isParityAdjusted( key.getKeyValue(), 0 ) );
        }
        catch ( InvalidKeyException ike )
        {
            fail( "Key is invalid." );
        }
    }


    /**
     * Test the writing of a keytab file.
     *
     * @throws Exception
     */
    @Test
    public void testWriteKeytab() throws Exception
    {
        List<KeytabEntry> entries = new ArrayList<KeytabEntry>();

        entries.add( getEntry1() );
        entries.add( getEntry1() );

        Keytab writer = Keytab.getInstance();
        writer.setEntries( entries );
        ByteBuffer buffer = writer.write();

        //System.out.println( Strings.dumpBytes( buffer.array() ) );
        assertEquals( "Expected file size.", 130, buffer.limit() );
    }


    private KeytabEntry getEntry1() throws ParseException
    {
        String principalName = "HTTP/www.verisign.com@EXAMPLE.COM";
        int principalType = 1;

        String zuluTime = "20070217235745Z";
        Date date = null;

        synchronized ( UTC_DATE_FORMAT )
        {
            date = UTC_DATE_FORMAT.parse( zuluTime );
        }

        KerberosTime timeStamp = new KerberosTime( date.getTime() );

        byte keyVersion = 1;
        String passPhrase = "secret";
        Map<EncryptionType, EncryptionKey> keys = KerberosKeyFactory.getKerberosKeys( principalName, passPhrase );
        EncryptionKey key = keys.get( EncryptionType.DES_CBC_MD5 );

        return new KeytabEntry( principalName, principalType, timeStamp, keyVersion, key );
    }
}
