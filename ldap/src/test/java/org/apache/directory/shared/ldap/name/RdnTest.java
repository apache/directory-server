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
package org.apache.directory.shared.ldap.name;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Iterator;

import javax.naming.InvalidNameException;
import javax.naming.NamingException;

import org.apache.directory.shared.ldap.util.StringTools;
import org.junit.Test;


/**
 * Test the class Rdn
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$, 
 */
public class RdnTest
{
    // ~ Methods
    // ------------------------------------------------------------------------------------
    /**
     * Test a null RDN
     */
    @Test
    public void testRdnNull()
    {
        assertEquals( "", new Rdn().toString() );
    }


    /**
     * test an empty RDN
     * 
     * @throws InvalidNameException
     */
    @Test
    public void testRdnEmpty() throws InvalidNameException
    {
        assertEquals( "", new Rdn( "" ).toString() );
    }


    /**
     * test a simple RDN : a = b
     * 
     * @throws InvalidNameException
     */
    @Test
    public void testRdnSimple() throws InvalidNameException
    {
        assertEquals( "a=b", new Rdn( "a = b" ).toString() );
    }


    /**
     * test a composite RDN : a = b, d = e
     * 
     * @throws InvalidNameException
     */
    @Test
    public void testRdnComposite() throws InvalidNameException
    {
        assertEquals( "a=b+c=d", new Rdn( "a = b + c = d" ).toString() );
    }


    /**
     * test a composite RDN with or without spaces: a=b, a =b, a= b, a = b, a =
     * b
     * 
     * @throws InvalidNameException
     */
    @Test
    public void testRdnCompositeWithSpace() throws InvalidNameException
    {
        assertEquals( "a=b", new Rdn( "a=b" ).toString() );
        assertEquals( "a=b", new Rdn( " a=b" ).toString() );
        assertEquals( "a=b", new Rdn( "a =b" ).toString() );
        assertEquals( "a=b", new Rdn( "a= b" ).toString() );
        assertEquals( "a=b", new Rdn( "a=b " ).toString() );
        assertEquals( "a=b", new Rdn( " a =b" ).toString() );
        assertEquals( "a=b", new Rdn( " a= b" ).toString() );
        assertEquals( "a=b", new Rdn( " a=b " ).toString() );
        assertEquals( "a=b", new Rdn( "a = b" ).toString() );
        assertEquals( "a=b", new Rdn( "a =b " ).toString() );
        assertEquals( "a=b", new Rdn( "a= b " ).toString() );
        assertEquals( "a=b", new Rdn( " a = b" ).toString() );
        assertEquals( "a=b", new Rdn( " a =b " ).toString() );
        assertEquals( "a=b", new Rdn( " a= b " ).toString() );
        assertEquals( "a=b", new Rdn( "a = b " ).toString() );
        assertEquals( "a=b", new Rdn( " a = b " ).toString() );
    }


    /**
     * test a simple RDN with differents separators : a = b + c = d
     * 
     * @throws InvalidNameException
     */
    @Test
    public void testRdnSimpleMultivaluedAttribute() throws InvalidNameException
    {
        String result = new Rdn( "a = b + c = d" ).toString();
        assertEquals( "a=b+c=d", result );
    }


    /**
     * test a composite RDN with differents separators : a=b+c=d, e=f + g=h +
     * i=j
     * 
     * @throws InvalidNameException
     */
    @Test
    public void testRdnCompositeMultivaluedAttribute() throws InvalidNameException
    {
        Rdn rdn = new Rdn( "a =b+c=d + e=f + g  =h + i =j " );

        // NameComponent are not ordered
        assertEquals( "b", rdn.getValue( "a" ) );
        assertEquals( "d", rdn.getValue( "c" ) );
        assertEquals( "f", rdn.getValue( "  E  " ) );
        assertEquals( "h", rdn.getValue( "g" ) );
        assertEquals( "j", rdn.getValue( "i" ) );
    }


    /**
     * test a simple RDN with an oid prefix (uppercase) : OID.12.34.56 = azerty
     * 
     * @throws InvalidNameException
     */
    @Test
    public void testRdnOidUpper() throws InvalidNameException
    {
        assertEquals( "oid.12.34.56=azerty", new Rdn( "OID.12.34.56 =  azerty" ).toString() );
    }


    /**
     * test a simple RDN with an oid prefix (lowercase) : oid.12.34.56 = azerty
     * 
     * @throws InvalidNameException
     */
    @Test
    public void testRdnOidLower() throws InvalidNameException
    {
        assertEquals( "oid.12.34.56=azerty", new Rdn( "oid.12.34.56 = azerty" ).toString() );
    }


    /**
     * test a simple RDN with an oid attribut wiithout oid prefix : 12.34.56 =
     * azerty
     * 
     * @throws InvalidNameException
     */
    @Test
    public void testRdnOidWithoutPrefix() throws InvalidNameException
    {
        assertEquals( "12.34.56=azerty", new Rdn( "12.34.56 = azerty" ).toString() );
    }


    /**
     * test a composite RDN with an oid attribut wiithout oid prefix : 12.34.56 =
     * azerty; 7.8 = test
     * 
     * @throws InvalidNameException
     */
    @Test
    public void testRdnCompositeOidWithoutPrefix() throws InvalidNameException
    {
        String result = new Rdn( "12.34.56 = azerty + 7.8 = test" ).toString();
        assertEquals( "12.34.56=azerty+7.8=test", result );
    }


    /**
     * test a simple RDN with pair char attribute value : a = \,\=\+\<\>\#\;\\\"\C3\A9"
     * 
     * @throws InvalidNameException
     */
    @Test
    public void testRdnPairCharAttributeValue() throws InvalidNameException
    {
        String rdn = StringTools.utf8ToString( new byte[]
            { 'a', '=', '\\', ',', '=', '\\', '+', '\\', '<', '\\', '>', '#', '\\', ';', '\\', '\\', '\\', '"', '\\',
                'C', '3', '\\', 'A', '9' } );
        assertEquals( "a=\\,=\\+\\<\\>#\\;\\\\\\\"\u00E9", new Rdn( rdn ).toString() );
    }


    /**
     * test a simple RDN with hexString attribute value : a = #0010A0AAFF
     */
    @Test
    public void testRdnHexStringAttributeValue() throws InvalidNameException
    {
        assertEquals( "a=#0010A0AAFF", new Rdn( "a = #0010A0AAFF" ).toString() );
    }

    /**
     * test exception from illegal hexString attribute value : a=#zz.
     */
    @Test
    public void testBadRdnHexStringAttributeValue() throws InvalidNameException
    {
        try
        {
            new Rdn( "a=#zz" );
            fail();
        }
        catch ( InvalidNameException ine )
        {
            assertTrue( true );
        }
    }

    /**
     * test a simple RDN with quoted attribute value : a = "quoted \"value"
     * 
     * @throws InvalidNameException
     */
    @Test
    public void testRdnQuotedAttributeValue() throws InvalidNameException
    {
        assertEquals( "a=quoted \\\"value", new Rdn( "a = quoted \\\"value" ).toString() );
    }


    /**
     * Test the clone method for a RDN.
     */
    @Test
    public void testParseRDNNull()
    {
        Rdn rdn = null;

        try
        {
            RdnParser.parse( "c=d", rdn );
            fail();
        }
        catch ( InvalidNameException ine )
        {
            assertTrue( true );
        }
    }


    /**
     * Test the clone method for a RDN.
     * 
     * @throws InvalidNameException
     */
    @Test
    public void testRDNCloningOneNameComponent() throws InvalidNameException
    {
        Rdn rdn = new Rdn( "a", "a", "b", "b" );

        Rdn rdnClone = ( Rdn ) rdn.clone();

        RdnParser.parse( "c=d", rdn );

        assertEquals( "b", rdnClone.getValue( "a" ) );
    }


    /**
     * Test the clone method for a RDN.
     * 
     * @throws InvalidNameException
     */
    @Test
    public void testRDNCloningTwoNameComponent() throws InvalidNameException
    {
        Rdn rdn = new Rdn( "a = b + aa = bb" );

        Rdn rdnClone = ( Rdn ) rdn.clone();

        rdn.clear();
        RdnParser.parse( "c=d", rdn );

        assertEquals( "b", rdnClone.getValue( "a" ) );
        assertEquals( "bb", rdnClone.getValue( "aa" ) );
        assertEquals( "", rdnClone.getValue( "c" ) );
    }


    /**
     * Test the compareTo method for a RDN.
     * 
     * @throws InvalidNameException
     */
    @Test
    public void testRDNCompareToNull() throws InvalidNameException
    {
        Rdn rdn1 = new Rdn( " a = b + c = d + a = f + g = h " );
        Rdn rdn2 = null;
        assertTrue( rdn1.compareTo( rdn2 ) > 0 );
    }


    /**
     * Compares a composite NC to a single NC.
     * 
     * @throws InvalidNameException
     */
    @Test
    public void testRDNCompareToNCS2NC() throws InvalidNameException
    {
        Rdn rdn1 = new Rdn( " a = b + c = d + a = f + g = h " );
        Rdn rdn2 = new Rdn( " a = b " );
        assertTrue( rdn1.compareTo( rdn2 ) > 0 );
    }


    /**
     * Compares a single NC to a composite NC.
     * 
     * @throws InvalidNameException
     */
    @Test
    public void testRDNCompareToNC2NCS() throws InvalidNameException
    {
        Rdn rdn1 = new Rdn( " a = b " );
        Rdn rdn2 = new Rdn( " a = b + c = d + a = f + g = h " );

        assertTrue( rdn1.compareTo( rdn2 ) < 0 );
    }


    /**
     * Compares a composite NCS to a composite NCS in the same order.
     * 
     * @throws InvalidNameException
     */
    @Test
    public void testRDNCompareToNCS2NCSOrdered() throws InvalidNameException
    {
        Rdn rdn1 = new Rdn( " a = b + c = d + a = f + g = h " );
        Rdn rdn2 = new Rdn( " a = b + c = d + a = f + g = h " );

        assertEquals( 0, rdn1.compareTo( rdn2 ) );
    }


    /**
     * Compares a composite NCS to a composite NCS in a different order.
     * 
     * @throws InvalidNameException
     */
    @Test
    public void testRDNCompareToNCS2NCSUnordered() throws InvalidNameException
    {
        Rdn rdn1 = new Rdn( " a = b + a = f + g = h + c = d " );
        Rdn rdn2 = new Rdn( " a = b + c = d + a = f + g = h " );

        assertEquals( 0, rdn1.compareTo( rdn2 ) );
    }


    /**
     * Compares a composite NCS to a different composite NCS.
     * 
     * @throws InvalidNameException
     */
    @Test
    public void testRDNCompareToNCS2NCSNotEquals() throws InvalidNameException
    {
        Rdn rdn1 = new Rdn( " a = f + g = h + c = d " );
        Rdn rdn2 = new Rdn( " c = d + a = h + g = h " );

        assertTrue( rdn1.compareTo( rdn2 ) < 0 );
        assertTrue( rdn2.compareTo( rdn1 ) > 0 );
        assertEquals( 0, rdn1.compareTo( rdn2 ) + rdn2.compareTo( rdn1 ) );
    }


    /**
     * Test for DIRSHARED-2.
     * The first ATAV is equal, the second or following ATAV differs.
     * 
     * @throws InvalidNameException
     */
    @Test
    public void testCompareSecondAtav() throws InvalidNameException
    {
        // the second ATAV differs
        Rdn rdn1 = new Rdn( " a = b + c = d " );
        Rdn rdn2 = new Rdn( " a = b + c = y " );
        assertTrue( rdn1.compareTo( rdn2 ) < 0 );
        assertTrue( rdn2.compareTo( rdn1 ) > 0 );
        assertEquals( 0, rdn1.compareTo( rdn2 ) + rdn2.compareTo( rdn1 ) );

        // the third ATAV differs
        Rdn rdn3 = new Rdn( " a = b + c = d + e = f " );
        Rdn rdn4 = new Rdn( " a = b + c = d + e = y " );
        assertTrue( rdn3.compareTo( rdn4 ) < 0 );
        assertTrue( rdn4.compareTo( rdn3 ) > 0 );
        assertEquals( 0, rdn3.compareTo( rdn4 ) + rdn4.compareTo( rdn3 ) );

        // the second ATAV differs in value only
        Rdn rdn5 = new Rdn( " a = b + a = c " );
        Rdn rdn6 = new Rdn( " a = b + a = y " );
        assertTrue( rdn5.compareTo( rdn6 ) < 0 );
        assertTrue( rdn6.compareTo( rdn5 ) > 0 );
        assertEquals( 0, rdn5.compareTo( rdn6 ) + rdn6.compareTo( rdn5 ) );
    }


    /**
     * Test for DIRSHARED-2.
     * The compare operation should return a correct value (1 or -1)
     * depending on the ATAVs, not on their position.
     * 
     * @throws InvalidNameException
     */
    @Test
    public void testCompareIndependentFromOrder() throws InvalidNameException
    {
        Rdn rdn1 = new Rdn( " a = b + c = d " );
        Rdn rdn2 = new Rdn( " c = d + a = b " );
        assertEquals( 0, rdn1.compareTo( rdn2 ) );

        rdn1 = new Rdn( " a = b + c = e " );
        rdn2 = new Rdn( " c = d + a = b " );
        assertTrue( rdn1.compareTo( rdn2 ) > 0 );
        assertTrue( rdn2.compareTo( rdn1 ) < 0 );
        assertEquals( 0, rdn1.compareTo( rdn2 ) + rdn2.compareTo( rdn1 ) );

        rdn1 = new Rdn( " a = b + c = d " );
        rdn2 = new Rdn( " e = f + g = h " );
        assertTrue( rdn1.compareTo( rdn2 ) < 0 );
        assertTrue( rdn2.compareTo( rdn1 ) > 0 );
        assertEquals( 0, rdn1.compareTo( rdn2 ) + rdn2.compareTo( rdn1 ) );
    }


    /**
     * Test for DIRSHARED-3.
     * Tests that compareTo() is invertable for single-valued RDNs.
     * 
     * @throws InvalidNameException
     */
    @Test
    public void testCompareInvertableNC2NC() throws InvalidNameException
    {
        Rdn rdn1 = new Rdn( " a = b " );
        Rdn rdn2 = new Rdn( " a = c " );
        assertTrue( rdn1.compareTo( rdn2 ) < 0 );
        assertTrue( rdn2.compareTo( rdn1 ) > 0 );
        assertEquals( 0, rdn1.compareTo( rdn2 ) + rdn2.compareTo( rdn1 ) );

    }


    /**
     * Test for DIRSHARED-3.
     * Tests that compareTo() is invertable for multi-valued RDNs with different values.
     * 
     * @throws InvalidNameException
     */
    @Test
    public void testCompareInvertableNCS2NCSDifferentValues() throws InvalidNameException
    {
        Rdn rdn1 = new Rdn( " a = b + a = c " );
        Rdn rdn2 = new Rdn( " a = b + a = y " );
        assertTrue( rdn1.compareTo( rdn2 ) < 0 );
        assertTrue( rdn2.compareTo( rdn1 ) > 0 );
        assertEquals( 0, rdn1.compareTo( rdn2 ) + rdn2.compareTo( rdn1 ) );
    }


    /**
     * Test for DIRSHARED-3.
     * Tests that compareTo() is invertable for multi-valued RDNs with different types.
     * 
     * @throws InvalidNameException
     */
    @Test
    public void testCompareInvertableNCS2NCSDifferentTypes() throws InvalidNameException
    {
        Rdn rdn1 = new Rdn( " a = b + c = d  " );
        Rdn rdn2 = new Rdn( " e = f + g = h " );
        assertTrue( rdn1.compareTo( rdn2 ) < 0 );
        assertTrue( rdn2.compareTo( rdn1 ) > 0 );
        assertEquals( 0, rdn1.compareTo( rdn2 ) + rdn2.compareTo( rdn1 ) );
    }


    /**
     * Test for DIRSHARED-3.
     * Tests that compareTo() is invertable for multi-valued RDNs with different order.
     * 
     * @throws InvalidNameException
     */
    @Test
    public void testCompareInvertableNCS2NCSUnordered() throws InvalidNameException
    {
        Rdn rdn1 = new Rdn( " c = d + a = b " );
        Rdn rdn2 = new Rdn( " a = b + e = f " );
        assertTrue( rdn1.compareTo( rdn2 ) < 0 );
        assertTrue( rdn2.compareTo( rdn1 ) > 0 );
        assertEquals( 0, rdn1.compareTo( rdn2 ) + rdn2.compareTo( rdn1 ) );
    }


    /**
     * Compares with a null RDN.
     * 
     * @throws InvalidNameException
     */
    @Test
    public void testRDNCompareToNullRdn() throws InvalidNameException
    {
        Rdn rdn1 = new Rdn( " a = b " );

        assertEquals( 1, rdn1.compareTo( null ) );
    }


    /**
     * Compares with a bad object
     * 
     * @throws InvalidNameException
     */
    @Test
    public void testRDNCompareToBadObject() throws InvalidNameException
    {
        Rdn rdn1 = new Rdn( " a = b " );

        assertEquals( Rdn.UNDEFINED, rdn1.compareTo( "test" ) );
    }


    /**
     * Compares a simple NC to a simple NC.
     * 
     * @throws InvalidNameException
     */
    @Test
    public void testRDNCompareToNC2NC() throws InvalidNameException
    {
        Rdn rdn1 = new Rdn( " a = b " );
        Rdn rdn2 = new Rdn( " a = b " );

        assertEquals( 0, rdn1.compareTo( rdn2 ) );
    }


    /**
     * Compares a simple NC to a simple NC in UperCase.
     * 
     * @throws InvalidNameException
     */
    @Test
    public void testRDNCompareToNC2NCUperCase() throws InvalidNameException
    {
        Rdn rdn1 = new Rdn( " a = b " );
        Rdn rdn2 = new Rdn( " A = b " );

        assertEquals( 0, rdn1.compareTo( rdn2 ) );
        assertEquals( true, rdn1.equals( rdn2 ) );
    }


    /**
     * Compares a simple NC to a different simple NC.
     * 
     * @throws InvalidNameException
     */
    @Test
    public void testRDNCompareToNC2NCNotEquals() throws InvalidNameException
    {
        Rdn rdn1 = new Rdn( " a = b " );
        Rdn rdn2 = new Rdn( " A = d " );

        assertTrue( rdn1.compareTo( rdn2 ) < 0 );
    }


    /**
     * 
     * Test the getValue method.
     *
     * @throws InvalidNameException
     */
    @Test
    public void testGetValue() throws InvalidNameException
    {
        Rdn rdn = new Rdn( " a = b + a = f + g = h + c = d " );

        assertEquals( "b", rdn.getValue() );
    }


    /**
     * 
     * Test the getType method.
     *
     * @throws InvalidNameException
     */
    @Test
    public void testGetType() throws InvalidNameException
    {
        Rdn rdn = new Rdn( " a = b + a = f + g = h + c = d " );

        assertEquals( "a", rdn.getNormType() );
    }


    /**
     * Test the getSize method.
     *
     * @throws InvalidNameException
     */
    @Test
    public void testGetSize() throws InvalidNameException
    {
        Rdn rdn = new Rdn( " a = b + a = f + g = h + c = d " );

        assertEquals( 4, rdn.size() );
    }


    /**
     * Test the getSize method.
     *
     */
    @Test
    public void testGetSize0()
    {
        Rdn rdn = new Rdn();

        assertEquals( 0, rdn.size() );
    }


    /**
     * Test the equals method
     *
     * @throws InvalidNameException
     */
    @Test
    public void testEquals() throws InvalidNameException
    {
        Rdn rdn = new Rdn( "a=b + c=d + a=f" );

        assertFalse( rdn.equals( null ) );
        assertFalse( rdn.equals( "test" ) );
        assertFalse( rdn.equals( new Rdn( "a=c + c=d + a=f" ) ) );
        assertFalse( rdn.equals( new Rdn( "a=b" ) ) );
        assertTrue( rdn.equals( new Rdn( "a=b + c=d + a=f" ) ) );
        assertTrue( rdn.equals( new Rdn( "a=b + C=d + A=f" ) ) );
        assertTrue( rdn.equals( new Rdn( "c=d + a=f + a=b" ) ) );
    }


    @Test
    public void testUnescapeValueHexa()
    {
        byte[] res = ( byte[] ) Rdn.unescapeValue( "#fF" );

        assertEquals( "0xFF ", StringTools.dumpBytes( res ) );

        res = ( byte[] ) Rdn.unescapeValue( "#0123456789aBCDEF" );
        assertEquals( "0x01 0x23 0x45 0x67 0x89 0xAB 0xCD 0xEF ", StringTools.dumpBytes( res ) );
    }


    @Test
    public void testUnescapeValueHexaWrong()
    {
        try
        {
            Rdn.unescapeValue( "#fF1" );
            fail(); // Should not happen
        }
        catch ( IllegalArgumentException iae )
        {
            assertTrue( true );
        }
    }


    @Test
    public void testUnescapeValueString()
    {
        String res = ( String ) Rdn.unescapeValue( "azerty" );

        assertEquals( "azerty", res );
    }


    @Test
    public void testUnescapeValueStringSpecial()
    {
        String res = ( String ) Rdn.unescapeValue( "\\\\\\#\\,\\+\\;\\<\\>\\=\\\"\\ " );

        assertEquals( "\\#,+;<>=\" ", res );
    }


    @Test
    public void testUnescapeValueStringWithSpaceInTheMiddle()
    {
        String res = ( String ) Rdn.unescapeValue( "a b" );

        assertEquals( "a b", res );
    }


    @Test
    public void testUnescapeValueStringWithSpaceInAtTheBeginning()
    {
        String res = ( String ) Rdn.unescapeValue( "\\ a b" );

        assertEquals( " a b", res );
    }


    @Test
    public void testUnescapeValueStringWithSpaceInAtTheEnd()
    {
        String res = ( String ) Rdn.unescapeValue( "a b\\ " );

        assertEquals( "a b ", res );
    }
    
    
    @Test
    public void testUnescapeValueStringWithPoundInTheMiddle()
    {
        String res = ( String ) Rdn.unescapeValue( "a#b" );

        assertEquals( "a#b", res );
    }
    
    
    @Test
    public void testUnescapeValueStringWithPoundAtTheEnd()
    {
        String res = ( String ) Rdn.unescapeValue( "ab#" );

        assertEquals( "ab#", res );
    }
    
    
    @Test
    public void testEscapeValueString()
    {
        String res = Rdn.escapeValue( StringTools.getBytesUtf8( "azerty" ) );

        assertEquals( "azerty", res );
    }


    @Test
    public void testEscapeValueStringSpecial()
    {
        String res = Rdn.escapeValue( StringTools.getBytesUtf8( "\\#,+;<>=\" " ) );

        assertEquals( "\\\\#\\,\\+\\;\\<\\>\\=\\\"\\ ", res );
    }


    @Test
    public void testEscapeValueNumeric()
    {
        String res = Rdn.escapeValue( new byte[]
            { '-', 0x00, '-', 0x1F, '-', 0x7F, '-' } );

        assertEquals( "-\\00-\\1F-\\7F-", res );
    }


    @Test
    public void testEscapeValueMix()
    {
        String res = Rdn.escapeValue( new byte[]
            { '\\', 0x00, '-', '+', '#', 0x7F, '-' } );

        assertEquals( "\\\\\\00-\\+#\\7F-", res );
    }


    @Test
    public void testDIRSERVER_703() throws InvalidNameException
    {
        Rdn rdn = new Rdn( "cn=Kate Bush+sn=Bush" );
        assertEquals( "cn=Kate Bush+sn=Bush", rdn.getUpName() );
    }


    @Test
    public void testMultiValuedIterator() throws InvalidNameException
    {
        Rdn rdn = new Rdn( "cn=Kate Bush+sn=Bush" );
        Iterator<AttributeTypeAndValue> iterator = rdn.iterator();
        assertNotNull( iterator );
        assertTrue( iterator.hasNext() );
        assertNotNull( iterator.next() );
        assertTrue( iterator.hasNext() );
        assertNotNull( iterator.next() );
        assertFalse( iterator.hasNext() );
    }


    @Test
    public void testSingleValuedIterator() throws InvalidNameException
    {
        Rdn rdn = new Rdn( "cn=Kate Bush" );
        Iterator<AttributeTypeAndValue> iterator = rdn.iterator();
        assertNotNull( iterator );
        assertTrue( iterator.hasNext() );
        assertNotNull( iterator.next() );
        assertFalse( iterator.hasNext() );
    }


    @Test
    public void testEmptyIterator()
    {
        Rdn rdn = new Rdn();
        Iterator<AttributeTypeAndValue> iterator = rdn.iterator();
        assertNotNull( iterator );
        assertFalse( iterator.hasNext() );
    }


    @Test
    public void testRdnWithSpaces() throws InvalidNameException
    {
        Rdn rdn = new Rdn( "cn=a\\ b\\ c" );
        assertEquals( "cn=a b c", rdn.toString() );
    }


    @Test
    public void testEscapedSpaceInValue() throws InvalidNameException
    {
        Rdn rdn1 = new Rdn( "cn=a b c" );
        Rdn rdn2 = new Rdn( "cn=a\\ b\\ c" );
        assertEquals( "cn=a b c", rdn1.toString() );
        assertEquals( "cn=a b c", rdn2.toString() );
        assertTrue( rdn1.equals( rdn2 ) );

        Rdn rdn3 = new Rdn( "cn=\\ a b c\\ " );
        Rdn rdn4 = new Rdn( "cn=\\ a\\ b\\ c\\ " );
        assertEquals( "cn=\\ a b c\\ ", rdn3.toString() );
        assertEquals( "cn=\\ a b c\\ ", rdn4.toString() );
        assertTrue( rdn3.equals( rdn4 ) );
    }


    @Test
    public void testEscapedHashInValue() throws InvalidNameException
    {
        Rdn rdn1 = new Rdn( "cn=a#b#c" );
        Rdn rdn2 = new Rdn( "cn=a\\#b\\#c" );
        assertEquals( "cn=a#b#c", rdn1.toString() );
        assertEquals( "cn=a#b#c", rdn2.toString() );
        assertTrue( rdn1.equals( rdn2 ) );

        Rdn rdn3 = new Rdn( "cn=\\#a#b#c\\#" );
        Rdn rdn4 = new Rdn( "cn=\\#a\\#b\\#c\\#" );
        assertEquals( "cn=\\#a#b#c#", rdn3.toString() );
        assertEquals( "cn=\\#a#b#c#", rdn4.toString() );
        assertTrue( rdn3.equals( rdn4 ) );
    }


    @Test
    public void testEscapedAttributeValue()
    {
        // space doesn't need to be escaped in the middle of a string
        assertEquals( "a b", Rdn.escapeValue( "a b" ) );
        assertEquals( "a b c", Rdn.escapeValue( "a b c" ) );
        assertEquals( "a b c d", Rdn.escapeValue( "a b c d" ) );

        // space must be escaped at the beginning and the end of a string
        assertEquals( "\\ a b", Rdn.escapeValue( " a b" ) );
        assertEquals( "a b\\ ", Rdn.escapeValue( "a b " ) );
        assertEquals( "\\ a b\\ ", Rdn.escapeValue( " a b " ) );
        assertEquals( "\\  a  b \\ ", Rdn.escapeValue( "  a  b  " ) );

        // hash doesn't need to be escaped in the middle and the end of a string
        assertEquals( "a#b", Rdn.escapeValue( "a#b" ) );
        assertEquals( "a#b#", Rdn.escapeValue( "a#b#" ) );
        assertEquals( "a#b#c", Rdn.escapeValue( "a#b#c" ) );
        assertEquals( "a#b#c#", Rdn.escapeValue( "a#b#c#" ) );
        assertEquals( "a#b#c#d", Rdn.escapeValue( "a#b#c#d" ) );
        assertEquals( "a#b#c#d#", Rdn.escapeValue( "a#b#c#d#" ) );

        // hash must be escaped at the beginning of a string
        assertEquals( "\\#a#b", Rdn.escapeValue( "#a#b" ) );
        assertEquals( "\\##a#b", Rdn.escapeValue( "##a#b" ) );
    }


    /** Serialization tests ------------------------------------------------- */

    /**
     * Test serialization of an empty RDN
     */
    @Test
    public void testEmptyRDNSerialization() throws NamingException, IOException, ClassNotFoundException
    {
        Rdn rdn = new Rdn( "" );

        rdn.normalize();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream out = new ObjectOutputStream( baos );

        out.writeObject( rdn );

        ObjectInputStream in = null;

        byte[] data = baos.toByteArray();
        in = new ObjectInputStream( new ByteArrayInputStream( data ) );

        Rdn rdn2 = (Rdn)in.readObject();

        assertEquals( rdn, rdn2 );
    }


    @Test
    public void testNullRdnSerialization() throws IOException, ClassNotFoundException
    {
        Rdn rdn = new Rdn();

        rdn.normalize();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream out = new ObjectOutputStream( baos );

        out.writeObject( rdn );

        ObjectInputStream in = null;

        byte[] data = baos.toByteArray();
        in = new ObjectInputStream( new ByteArrayInputStream( data ) );

        Rdn rdn2 = (Rdn)in.readObject();

        assertEquals( rdn, rdn2 );
    }


    /**
     * Test serialization of a simple Rdn
     */
    @Test
    public void testSimpleRdnSerialization() throws NamingException, IOException, ClassNotFoundException
    {
        Rdn rdn = new Rdn( "a=b" );
        rdn.normalize();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream out = new ObjectOutputStream( baos );

        out.writeObject( rdn );

        ObjectInputStream in = null;

        byte[] data = baos.toByteArray();
        in = new ObjectInputStream( new ByteArrayInputStream( data ) );

        Rdn rdn2 = (Rdn)in.readObject();

        assertEquals( rdn, rdn2 );
    }


    /**
     * Test serialization of a simple Rdn
     */
    @Test
    public void testSimpleRdn2Serialization() throws NamingException, IOException, ClassNotFoundException
    {
        Rdn rdn = new Rdn( " ABC  = DEF " );
        rdn.normalize();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream out = new ObjectOutputStream( baos );

        out.writeObject( rdn );

        ObjectInputStream in = null;

        byte[] data = baos.toByteArray();
        in = new ObjectInputStream( new ByteArrayInputStream( data ) );

        Rdn rdn2 = (Rdn)in.readObject();

        assertEquals( rdn, rdn2 );
    }


    /**
     * Test serialization of a simple Rdn with no value
     */
    @Test
    public void testSimpleRdnNoValueSerialization() throws NamingException, IOException, ClassNotFoundException
    {
        Rdn rdn = new Rdn( " ABC  =" );
        rdn.normalize();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream out = new ObjectOutputStream( baos );

        out.writeObject( rdn );

        ObjectInputStream in = null;

        byte[] data = baos.toByteArray();
        in = new ObjectInputStream( new ByteArrayInputStream( data ) );

        Rdn rdn2 = (Rdn)in.readObject();

        assertEquals( rdn, rdn2 );
    }


    /**
     * Test serialization of a simple Rdn with one value
     */
    @Test
    public void testSimpleRdnOneValueSerialization() throws NamingException, IOException, ClassNotFoundException
    {
        Rdn rdn = new Rdn( " ABC  = def " );
        rdn.normalize();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream out = new ObjectOutputStream( baos );

        out.writeObject( rdn );

        ObjectInputStream in = null;

        byte[] data = baos.toByteArray();
        in = new ObjectInputStream( new ByteArrayInputStream( data ) );

        Rdn rdn2 = (Rdn)in.readObject();

        assertEquals( rdn, rdn2 );
    }


    /**
     * Test serialization of a simple Rdn with three values
     */
    @Test
    public void testSimpleRdnThreeValuesSerialization() throws NamingException, IOException, ClassNotFoundException
    {
        Rdn rdn = new Rdn( " A = a + B = b + C = c " );
        rdn.normalize();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream out = new ObjectOutputStream( baos );

        out.writeObject( rdn );

        ObjectInputStream in = null;

        byte[] data = baos.toByteArray();
        in = new ObjectInputStream( new ByteArrayInputStream( data ) );

        Rdn rdn2 = (Rdn)in.readObject();

        assertEquals( rdn, rdn2 );
    }


    /**
     * Test serialization of a simple Rdn with three unordered values
     */
    @Test
    public void testSimpleRdnThreeValuesUnorderedSerialization() throws NamingException, IOException,
        ClassNotFoundException
    {
        Rdn rdn = new Rdn( " B = b + A = a + C = c " );
        rdn.normalize();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream out = new ObjectOutputStream( baos );

        out.writeObject( rdn );

        ObjectInputStream in = null;

        byte[] data = baos.toByteArray();
        in = new ObjectInputStream( new ByteArrayInputStream( data ) );

        Rdn rdn2 = (Rdn)in.readObject();

        assertEquals( rdn, rdn2 );
    }


    /** Static Serialization tests ------------------------------------------------- */

    /**
     * Test serialization of an empty RDN
     */
    @Test
    public void testEmptyRDNStaticSerialization() throws NamingException, IOException, ClassNotFoundException
    {
        Rdn rdn = new Rdn( "" );

        rdn.normalize();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream out = new ObjectOutputStream( baos );

        RdnSerializer.serialize( rdn, out );
        out.flush();

        ObjectInputStream in = null;

        byte[] data = baos.toByteArray();
        in = new ObjectInputStream( new ByteArrayInputStream( data ) );

        Rdn rdn2 = RdnSerializer.deserialize( in );

        assertEquals( rdn, rdn2 );
    }


    @Test
    public void testNullRdnStaticSerialization() throws IOException, ClassNotFoundException
    {
        Rdn rdn = new Rdn();

        rdn.normalize();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream out = new ObjectOutputStream( baos );

        RdnSerializer.serialize( rdn, out );
        out.flush();

        ObjectInputStream in = null;

        byte[] data = baos.toByteArray();
        in = new ObjectInputStream( new ByteArrayInputStream( data ) );

        Rdn rdn2 = RdnSerializer.deserialize( in );

        assertEquals( rdn, rdn2 );
    }


    /**
     * Test serialization of a simple Rdn
     */
    @Test
    public void testSimpleRdnStaticSerialization() throws NamingException, IOException, ClassNotFoundException
    {
        Rdn rdn = new Rdn( "a=b" );
        rdn.normalize();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream out = new ObjectOutputStream( baos );

        RdnSerializer.serialize( rdn, out );
        out.flush();

        ObjectInputStream in = null;

        byte[] data = baos.toByteArray();
        in = new ObjectInputStream( new ByteArrayInputStream( data ) );

        Rdn rdn2 = RdnSerializer.deserialize( in );

        assertEquals( rdn, rdn2 );
    }


    /**
     * Test serialization of a simple Rdn
     */
    @Test
    public void testSimpleRdn2StaticSerialization() throws NamingException, IOException, ClassNotFoundException
    {
        Rdn rdn = new Rdn( " ABC  = DEF " );
        rdn.normalize();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream out = new ObjectOutputStream( baos );

        RdnSerializer.serialize( rdn, out );
        out.flush();

        ObjectInputStream in = null;

        byte[] data = baos.toByteArray();
        in = new ObjectInputStream( new ByteArrayInputStream( data ) );

        Rdn rdn2 = RdnSerializer.deserialize( in );

        assertEquals( rdn, rdn2 );
    }


    /**
     * Test serialization of a simple Rdn with no value
     */
    @Test
    public void testSimpleRdnNoValueStaticSerialization() throws NamingException, IOException, ClassNotFoundException
    {
        Rdn rdn = new Rdn( " ABC  =" );
        rdn.normalize();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream out = new ObjectOutputStream( baos );

        RdnSerializer.serialize( rdn, out );
        out.flush();

        ObjectInputStream in = null;

        byte[] data = baos.toByteArray();
        in = new ObjectInputStream( new ByteArrayInputStream( data ) );

        Rdn rdn2 = RdnSerializer.deserialize( in );

        assertEquals( rdn, rdn2 );
    }


    /**
     * Test serialization of a simple Rdn with one value
     */
    @Test
    public void testSimpleRdnOneValueStaticSerialization() throws NamingException, IOException, ClassNotFoundException
    {
        Rdn rdn = new Rdn( " ABC  = def " );
        rdn.normalize();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream out = new ObjectOutputStream( baos );

        RdnSerializer.serialize( rdn, out );
        out.flush();

        ObjectInputStream in = null;

        byte[] data = baos.toByteArray();
        in = new ObjectInputStream( new ByteArrayInputStream( data ) );

        Rdn rdn2 = RdnSerializer.deserialize( in );

        assertEquals( rdn, rdn2 );
    }


    /**
     * Test serialization of a simple Rdn with three values
     */
    @Test
    public void testSimpleRdnThreeValuesStaticSerialization() throws NamingException, IOException,
        ClassNotFoundException
    {
        Rdn rdn = new Rdn( " A = a + B = b + C = c " );
        rdn.normalize();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream out = new ObjectOutputStream( baos );

        RdnSerializer.serialize( rdn, out );
        out.flush();

        ObjectInputStream in = null;

        byte[] data = baos.toByteArray();
        in = new ObjectInputStream( new ByteArrayInputStream( data ) );

        Rdn rdn2 = RdnSerializer.deserialize( in );

        assertEquals( rdn, rdn2 );
    }


    /**
     * Test serialization of a simple Rdn with three unordered values
     */
    @Test
    public void testSimpleRdnThreeValuesUnorderedStaticSerialization() throws NamingException, IOException,
        ClassNotFoundException
    {
        Rdn rdn = new Rdn( " B = b + A = a + C = c " );
        rdn.normalize();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream out = new ObjectOutputStream( baos );

        RdnSerializer.serialize( rdn, out );
        out.flush();

        ObjectInputStream in = null;

        byte[] data = baos.toByteArray();
        in = new ObjectInputStream( new ByteArrayInputStream( data ) );

        Rdn rdn2 = RdnSerializer.deserialize( in );

        assertEquals( rdn, rdn2 );
    }


    /**
     * test an RDN with empty value
     */
    @Test
    public void testRdnWithEmptyValue() throws InvalidNameException
    {
        assertTrue( RdnParser.isValid( "a=" ) );
        assertTrue( RdnParser.isValid( "a=\"\"" ) );
        assertEquals( "a=", new Rdn( "a=\"\"" ).toString() );
        assertEquals( "a=", new Rdn( "a=" ).toString() );
    }


    /**
     * test an RDN with escaped comma
     */
    @Test
    public void testRdnWithEscapedComa() throws InvalidNameException
    {
        assertTrue( RdnParser.isValid( "a=b\\,c" ) );
        assertEquals( "a=b\\,c", new Rdn( "a=b\\,c" ).toString() );

        assertTrue( RdnParser.isValid( "a=\"b,c\"" ) );
        assertEquals( "a=b\\,c", new Rdn( "a=\"b,c\"" ).toString() );
        assertEquals( "a=\"b,c\"", new Rdn( "a=\"b,c\"" ).getUpName() );

        assertTrue( RdnParser.isValid( "a=\"b\\,c\"" ) );
        Rdn rdn = new Rdn( "a=\"b\\,c\"" );
        assertEquals( "a=\"b\\,c\"", rdn.getUpName() );
        assertEquals( "a=b\\,c", rdn.toString() );
    }


    /**
     * Tests the equals and compareTo results of cloned multi-valued RDNs.
     * Test for DIRSHARED-9.
     * 
     * @throws InvalidNameException
     */
    @Test
    public void testComparingOfClonedMultiValuedRDNs() throws InvalidNameException
    {
        // Use upper case attribute types to test if normalized types are used 
        // for comparison
        Rdn rdn = new Rdn( " A = b + C = d" );
        Rdn clonedRdn = ( Rdn ) rdn.clone();

        assertEquals( 0, rdn.compareTo( clonedRdn ) );
        assertEquals( true, rdn.equals( clonedRdn ) );
    }


    /**
     * Tests the equals and compareTo results of copy constructed multi-valued RDNs.
     * Test for DIRSHARED-9.
     * 
     * @throws InvalidNameException
     */
    @Test
    public void testComparingOfCopyConstructedMultiValuedRDNs() throws InvalidNameException
    {
        // Use upper case attribute types to test if normalized types are used 
        // for comparison
        Rdn rdn = new Rdn( " A = b + C = d" );
        Rdn copiedRdn = new Rdn( rdn );

        assertEquals( 0, rdn.compareTo( copiedRdn ) );
        assertEquals( true, rdn.equals( copiedRdn ) );
    }


    /**
     * test the UpName method on a RDN with more than one atav
     */
    @Test 
    public void testGetUpNameMultipleAtav() throws InvalidNameException
    {
        Rdn rdn = new Rdn( " A = b + C = d " );
        
        assertEquals( " A = b + C = d ", rdn.getUpName() );
    }
}
