/*
 *   Licensed to the Apache Software Foundation (ASF) under one
 *   or more contributor license agreements.  See the NOTICE file
 *   distributed with this work for additional information
 *   regarding copyright ownership.  The ASF licenses this file
 *   to you under the Apache License, Version 2.0 (the
 *   "License"); you may not use this file except in compliance
 *   with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing,
 *   software distributed under the License is distributed on an
 *   "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *   KIND, either express or implied.  See the License for the
 *   specific language governing permissions and limitations
 *   under the License.
 *
 */

package org.apache.directory.shared.kerberos.codec;


import static org.junit.Assert.assertTrue;

import java.lang.reflect.Field;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.directory.api.asn1.DecoderException;
import org.apache.directory.api.asn1.ber.Asn1Decoder;
import org.apache.directory.shared.kerberos.KerberosTime;
import org.apache.directory.shared.kerberos.codec.encKrbCredPart.EncKrbCredPartContainer;
import org.apache.directory.shared.kerberos.codec.types.EncryptionType;
import org.apache.directory.shared.kerberos.components.EncKrbCredPart;
import org.apache.directory.shared.kerberos.components.EncryptionKey;
import org.apache.directory.shared.kerberos.components.HostAddress;
import org.apache.directory.shared.kerberos.components.KrbCredInfo;
import org.apache.directory.shared.util.Strings;
import org.junit.Before;
import org.junit.Test;


/**
 * test cases for EncKrbCredPart codec
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class EncKrbCredPartDecoderTest
{
    private List<KrbCredInfo> ticketInfo;
    private int nonce;
    private KerberosTime timestamp;
    private int usec;
    private HostAddress senderAddress;
    private HostAddress recipientAddress;

    private List<FieldValueHolder> optionalFieldValueList;

    class FieldValueHolder
    {
        String fieldName;
        Object value;


        FieldValueHolder( String fieldName, Object value )
        {
            this.fieldName = fieldName;
            this.value = value;
        }


        @Override
        public String toString()
        {
            return "FieldValueHolder [fieldName=" + fieldName + ", value=" + value + "]";
        }
    }


    @Before
    public void setup() throws Exception
    {
        optionalFieldValueList = new ArrayList<FieldValueHolder>();

        ticketInfo = new ArrayList<KrbCredInfo>();
        KrbCredInfo info1 = new KrbCredInfo();
        info1.setKey( new EncryptionKey( EncryptionType.DES3_CBC_MD5, new byte[]
            { 0, 1 } ) );
        ticketInfo.add( info1 );

        KrbCredInfo info2 = new KrbCredInfo();
        info2.setKey( new EncryptionKey( EncryptionType.AES128_CTS_HMAC_SHA1_96, new byte[]
            { 2, 3 } ) );
        ticketInfo.add( info2 );

        nonce = 100;
        optionalFieldValueList.add( new FieldValueHolder( "nonce", nonce ) );

        timestamp = new KerberosTime( new Date().getTime() );
        optionalFieldValueList.add( new FieldValueHolder( "timestamp", timestamp ) );

        usec = 1;
        optionalFieldValueList.add( new FieldValueHolder( "usec", usec ) );

        senderAddress = new HostAddress( InetAddress.getByName( "localhost" ) );
        optionalFieldValueList.add( new FieldValueHolder( "senderAddress", senderAddress ) );

        recipientAddress = new HostAddress( InetAddress.getByName( "localhost" ) );
        optionalFieldValueList.add( new FieldValueHolder( "recipientAddress", recipientAddress ) );
    }


    @Test
    public void testEncKrbCredPart() throws Exception
    {
        int size = optionalFieldValueList.size();
        for ( int i = 0; i < size; i++ )
        {
            EncKrbCredPart expected = new EncKrbCredPart();
            expected.setTicketInfo( ticketInfo );

            Map<String, Field> EncKrbCrePartFieldNameMap = getFieldMap( expected );

            List<FieldValueHolder> presentFieldList = new ArrayList<FieldValueHolder>();

            FieldValueHolder fieldValHolder = optionalFieldValueList.get( i );
            presentFieldList.add( fieldValHolder );

            Field f = EncKrbCrePartFieldNameMap.get( Strings.toLowerCase( fieldValHolder.fieldName ) );
            f.set( expected, fieldValHolder.value );

            for ( int j = i + 1; j < size; j++ )
            {
                fieldValHolder = optionalFieldValueList.get( j );
                presentFieldList.add( fieldValHolder );
                f = EncKrbCrePartFieldNameMap.get( Strings.toLowerCase( fieldValHolder.fieldName ) );
                f.set( expected, fieldValHolder.value );
            }

            ByteBuffer stream = ByteBuffer.allocate( expected.computeLength() );
            expected.encode( stream );
            stream.flip();

            Asn1Decoder decoder = new Asn1Decoder();
            EncKrbCredPartContainer container = new EncKrbCredPartContainer( stream );

            try
            {
                decoder.decode( stream, container );
            }
            catch ( DecoderException e )
            {
                // NOTE: keep this sysout for easy debugging (no need to setup a logger)
                System.out.println( "failed sequence:\n" + expected );
                throw e;
            }

            EncKrbCredPart actual = container.getEncKrbCredPart();
            assertValues( presentFieldList, actual );
        }
    }


    @Test
    public void testKrbCredInfoWithEachOptElement() throws Exception
    {
        // algorithm:
        // start from the first mandatory element and add ONLY one OPTIONAL element and then test decoding

        int size = optionalFieldValueList.size();
        for ( int i = size - 1; i >= 0; i-- )
        {
            EncKrbCredPart expected = new EncKrbCredPart();
            expected.setTicketInfo( ticketInfo );
            Map<String, Field> encKrbCredPartFieldNameMap = getFieldMap( expected );

            List<FieldValueHolder> presentFieldList = new ArrayList<FieldValueHolder>();

            FieldValueHolder fieldValHolder = optionalFieldValueList.get( i );
            presentFieldList.add( fieldValHolder );

            Field f = encKrbCredPartFieldNameMap.get( Strings.toLowerCase( fieldValHolder.fieldName ) );
            f.set( expected, fieldValHolder.value );

            ByteBuffer stream = ByteBuffer.allocate( expected.computeLength() );
            expected.encode( stream );
            stream.flip();

            Asn1Decoder decoder = new Asn1Decoder();
            EncKrbCredPartContainer container = new EncKrbCredPartContainer( stream );

            try
            {
                decoder.decode( stream, container );
            }
            catch ( DecoderException e )
            {
                // NOTE: keep this sysout for easy debugging (no need to setup a logger)
                System.out.println( "failed sequence:\n" + expected );
                throw e;
            }

            EncKrbCredPart actual = container.getEncKrbCredPart();
            assertValues( presentFieldList, actual );
        }
    }


    /**
     * compare the values that are inserted into the EncKrbCredPart objects before encoding to
     * those that are obtained from decoded EncKrbCredPart
     * 
     * @param presentFieldList the list of values that were inserted in the EncKrbCredPart class before encoding
     * @param decoded the decoded EncKrbCredPart object 
     */
    private void assertValues( List<FieldValueHolder> presentFieldList, EncKrbCredPart decoded ) throws Exception
    {
        Map<String, Field> fieldNameMap = getFieldMap( decoded );
        for ( FieldValueHolder fh : presentFieldList )
        {
            Field actualField = fieldNameMap.get( Strings.toLowerCase( fh.fieldName ) );
            Object decodedValue = actualField.get( decoded );

            //System.out.println( fh.fieldName + " expected: " + fh.value + " , actual: " + decodedValue );

            assertTrue( decodedValue.equals( fh.value ) );
        }
    }


    /**
     * create a map with the field's name and field objects of the KrbCreInfo
     * 
     * @param source the EncKrbCredPart object
     */
    private Map<String, Field> getFieldMap( EncKrbCredPart source )
    {
        Field[] fields = source.getClass().getDeclaredFields();

        Map<String, Field> fieldNameMap = new HashMap<String, Field>();

        for ( Field f : fields )
        {
            f.setAccessible( true );
            fieldNameMap.put( Strings.toLowerCase( f.getName() ), f );
        }

        return fieldNameMap;
    }

}
