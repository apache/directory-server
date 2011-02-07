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

import org.apache.directory.shared.asn1.DecoderException;
import org.apache.directory.shared.asn1.ber.Asn1Decoder;
import org.apache.directory.shared.kerberos.KerberosTime;
import org.apache.directory.shared.kerberos.codec.krbCredInfo.KrbCredInfoContainer;
import org.apache.directory.shared.kerberos.codec.types.EncryptionType;
import org.apache.directory.shared.kerberos.codec.types.PrincipalNameType;
import org.apache.directory.shared.kerberos.components.EncryptionKey;
import org.apache.directory.shared.kerberos.components.HostAddress;
import org.apache.directory.shared.kerberos.components.HostAddresses;
import org.apache.directory.shared.kerberos.components.KrbCredInfo;
import org.apache.directory.shared.kerberos.components.PrincipalName;
import org.apache.directory.shared.kerberos.flags.TicketFlag;
import org.apache.directory.shared.kerberos.flags.TicketFlags;
import org.junit.Before;
import org.junit.Test;


/**
 * Test cases for KrbCredInfo codec.
 *
 * This test class assumes that the encoding of KrbCredInfo class is correct and highly relies on its functionality
 * to generate test PDUs instead of generating them by hand. Also note that the accuracy of this test case depends on
 * the accuracy of the encoding done by the components constituted in KrbCredInfo ASN.1 structure
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class KrbCredInfoDecoderTest
{
    private EncryptionKey key;
    private String pRealm;
    private PrincipalName pName;
    private TicketFlags ticketFlags;
    private KerberosTime authTime;
    private KerberosTime startTime;
    private KerberosTime endTime;
    private KerberosTime renewtill;
    private String sRealm;
    private PrincipalName sName;
    private HostAddresses clientAddresses;

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

        key = new EncryptionKey( EncryptionType.DES3_CBC_MD5, new byte[]
            { 0, 1 } );

        pRealm = "prealm";
        // NOTE: we have to add each field manually cause order is important
        optionalFieldValueList.add( new FieldValueHolder( "pRealm", pRealm ) );

        ticketFlags = new TicketFlags( TicketFlag.INITIAL.getValue() );
        optionalFieldValueList.add( new FieldValueHolder( "ticketFlags", ticketFlags ) );

        authTime = new KerberosTime( new Date().getTime() );
        optionalFieldValueList.add( new FieldValueHolder( "authTime", authTime ) );

        startTime = new KerberosTime( new Date().getTime() );
        optionalFieldValueList.add( new FieldValueHolder( "startTime", startTime ) );

        endTime = new KerberosTime( new Date().getTime() );
        optionalFieldValueList.add( new FieldValueHolder( "endTime", endTime ) );

        renewtill = new KerberosTime( new Date().getTime() );
        optionalFieldValueList.add( new FieldValueHolder( "renewtill", renewtill ) );

        sRealm = "srealm";
        optionalFieldValueList.add( new FieldValueHolder( "sRealm", sRealm ) );

        pName = new PrincipalName( "pname", PrincipalNameType.KRB_NT_PRINCIPAL );
        optionalFieldValueList.add( new FieldValueHolder( "pName", pName ) );

        sName = new PrincipalName( "sname", PrincipalNameType.KRB_NT_PRINCIPAL );
        optionalFieldValueList.add( new FieldValueHolder( "sName", sName ) );

        clientAddresses = new HostAddresses( new HostAddress[]
            { new HostAddress( InetAddress.getByName( "localhost" ) ) } );
        optionalFieldValueList.add( new FieldValueHolder( "clientAddresses", clientAddresses ) );
    }


    @Test
    public void testKrbCredInfo() throws Exception
    {
        // algorithm:
        // start from the first mandatory element and then add one of the subsequent OPTIONAL elements(in order) then
        // start adding/removing subsequent OPTIONAL elements(those which fall after the above OPTIONAL element)
        // one by one and then test decoding

        int size = optionalFieldValueList.size();

        for ( int i = 0; i < size; i++ )
        {
            KrbCredInfo expected = new KrbCredInfo();
            expected.setKey( key );
            Map<String, Field> krbCredInfoFieldNameMap = getFieldMap( expected );

            List<FieldValueHolder> presentFieldList = new ArrayList<FieldValueHolder>();

            FieldValueHolder fieldValHolder = optionalFieldValueList.get( i );
            presentFieldList.add( fieldValHolder );

            Field f = krbCredInfoFieldNameMap.get( fieldValHolder.fieldName.toLowerCase() );
            f.set( expected, fieldValHolder.value );

            for ( int j = i + 1; j < size; j++ )
            {
                fieldValHolder = optionalFieldValueList.get( j );
                presentFieldList.add( fieldValHolder );
                f = krbCredInfoFieldNameMap.get( fieldValHolder.fieldName.toLowerCase() );
                f.set( expected, fieldValHolder.value );
            }

            ByteBuffer stream = ByteBuffer.allocate( expected.computeLength() );
            expected.encode( stream );
            stream.flip();

            Asn1Decoder decoder = new Asn1Decoder();
            KrbCredInfoContainer container = new KrbCredInfoContainer();
            container.setStream( stream );

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

            KrbCredInfo actual = container.getKrbCredInfo();
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
            KrbCredInfo expected = new KrbCredInfo();
            expected.setKey( key );
            Map<String, Field> krbCredInfoFieldNameMap = getFieldMap( expected );

            List<FieldValueHolder> presentFieldList = new ArrayList<FieldValueHolder>();

            FieldValueHolder fieldValHolder = optionalFieldValueList.get( i );
            presentFieldList.add( fieldValHolder );

            Field f = krbCredInfoFieldNameMap.get( fieldValHolder.fieldName.toLowerCase() );
            f.set( expected, fieldValHolder.value );

            ByteBuffer stream = ByteBuffer.allocate( expected.computeLength() );
            expected.encode( stream );
            stream.flip();

            Asn1Decoder decoder = new Asn1Decoder();
            KrbCredInfoContainer container = new KrbCredInfoContainer();
            container.setStream( stream );

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

            KrbCredInfo actual = container.getKrbCredInfo();
            assertValues( presentFieldList, actual );
        }
    }


    /**
     * compare the values that are inserted into the KrbCredInfo objects before encoding to
     * those that are obtained from decoded KrbCredInfo
     *
     * @param presentFieldList the list of values that were inserted in the KrbCredInfo class before encoding
     * @param decoded the decoded KrbCredInfo object
     */
    private void assertValues( List<FieldValueHolder> presentFieldList, KrbCredInfo decoded ) throws Exception
    {
        Map<String, Field> krbCredInfoFieldNameMap = getFieldMap( decoded );

        for ( FieldValueHolder fh : presentFieldList )
        {
            Field actualField = krbCredInfoFieldNameMap.get( fh.fieldName.toLowerCase() );
            Object decodedValue = actualField.get( decoded );

            //System.out.println( fh.fieldName + " expected: " + fh.value + " , actual: " + decodedValue );

            assertTrue( decodedValue.equals( fh.value ) );
        }
    }


    /**
     * create a map with the field's name and field objects of the KrbCreInfo
     *
     * @param source the KrbCredInfo object
     */
    private Map<String, Field> getFieldMap( KrbCredInfo source )
    {
        Field[] fields = source.getClass().getDeclaredFields();

        Map<String, Field> fieldNameMap = new HashMap<String, Field>();

        for ( Field f : fields )
        {
            f.setAccessible( true );
            fieldNameMap.put( f.getName().toLowerCase(), f );
        }

        return fieldNameMap;
    }
}
