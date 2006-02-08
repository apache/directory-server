/*
 *   Copyright 2005 The Apache Software Foundation
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 */
package org.apache.directory.shared.ldap.name;


import javax.naming.InvalidNameException;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;

import junit.framework.Assert;
import junit.framework.TestCase;

import org.apache.directory.shared.ldap.name.Rdn;
import org.apache.directory.shared.ldap.name.RdnParser;
import org.apache.directory.shared.ldap.util.StringTools;


/**
 * Test the class Rdn
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class RdnTest extends TestCase
{
    // ~ Methods
    // ------------------------------------------------------------------------------------

    /**
     * Setup the test
     */
    protected void setUp()
    {
    }


    /**
     * Test a null RDN
     */
    public void testRdnNull() throws InvalidNameException
    {
        Assert.assertEquals( "", new Rdn().toString() );
    }


    /**
     * test an empty RDN
     */
    public void testRdnEmpty() throws InvalidNameException
    {
        Assert.assertEquals( "", new Rdn( "" ).toString() );
    }


    /**
     * test a simple RDN : a = b
     */
    public void testRdnSimple() throws InvalidNameException
    {
        Assert.assertEquals( "a=b", new Rdn( "a = b" ).toString() );
    }


    /**
     * test a composite RDN : a = b, d = e
     */
    public void testRdnComposite() throws InvalidNameException
    {
        Assert.assertEquals( "a=b+c=d", new Rdn( "a = b + c = d" ).toString() );
    }


    /**
     * test a composite RDN with or without spaces: a=b, a =b, a= b, a = b, a =
     * b
     */
    public void testRdnCompositeWithSpace() throws InvalidNameException
    {
        Assert.assertEquals( "a=b", new Rdn( "a=b" ).toString() );
        Assert.assertEquals( "a=b", new Rdn( " a=b" ).toString() );
        Assert.assertEquals( "a=b", new Rdn( "a =b" ).toString() );
        Assert.assertEquals( "a=b", new Rdn( "a= b" ).toString() );
        Assert.assertEquals( "a=b", new Rdn( "a=b " ).toString() );
        Assert.assertEquals( "a=b", new Rdn( " a =b" ).toString() );
        Assert.assertEquals( "a=b", new Rdn( " a= b" ).toString() );
        Assert.assertEquals( "a=b", new Rdn( " a=b " ).toString() );
        Assert.assertEquals( "a=b", new Rdn( "a = b" ).toString() );
        Assert.assertEquals( "a=b", new Rdn( "a =b " ).toString() );
        Assert.assertEquals( "a=b", new Rdn( "a= b " ).toString() );
        Assert.assertEquals( "a=b", new Rdn( " a = b" ).toString() );
        Assert.assertEquals( "a=b", new Rdn( " a =b " ).toString() );
        Assert.assertEquals( "a=b", new Rdn( " a= b " ).toString() );
        Assert.assertEquals( "a=b", new Rdn( "a = b " ).toString() );
        Assert.assertEquals( "a=b", new Rdn( " a = b " ).toString() );
    }


    /**
     * test a simple RDN with differents separators : a = b + c = d
     */
    public void testRdnSimpleMultivaluedAttribute() throws InvalidNameException
    {
        String result = new Rdn( "a = b + c = d" ).toString();
        Assert.assertEquals( "a=b+c=d", result );
    }


    /**
     * test a composite RDN with differents separators : a=b+c=d, e=f + g=h +
     * i=j
     */
    public void testRdnCompositeMultivaluedAttribute() throws InvalidNameException
    {
        Rdn rdn = new Rdn( "a =b+c=d + e=f + g  =h + i =j " );

        // NameComponent are not ordered
        Assert.assertEquals( "b", rdn.getValue( "a" ) );
        Assert.assertEquals( "d", rdn.getValue( "c" ) );
        Assert.assertEquals( "f", rdn.getValue( "  E  " ) );
        Assert.assertEquals( "h", rdn.getValue( "g" ) );
        Assert.assertEquals( "j", rdn.getValue( "i" ) );
    }


    /**
     * test a simple RDN with an oid prefix (uppercase) : OID.12.34.56 = azerty
     */
    public void testRdnOidUpper() throws InvalidNameException
    {
        Assert.assertEquals( "oid.12.34.56=azerty", new Rdn( "OID.12.34.56 =  azerty" ).toString() );
    }


    /**
     * test a simple RDN with an oid prefix (lowercase) : oid.12.34.56 = azerty
     */
    public void testRdnOidLower() throws InvalidNameException
    {
        Assert.assertEquals( "oid.12.34.56=azerty", new Rdn( "oid.12.34.56 = azerty" ).toString() );
    }


    /**
     * test a simple RDN with an oid attribut wiithout oid prefix : 12.34.56 =
     * azerty
     */
    public void testRdnOidWithoutPrefix() throws InvalidNameException
    {
        Assert.assertEquals( "12.34.56=azerty", new Rdn( "12.34.56 = azerty" ).toString() );
    }


    /**
     * test a composite RDN with an oid attribut wiithout oid prefix : 12.34.56 =
     * azerty; 7.8 = test
     */
    public void testRdnCompositeOidWithoutPrefix() throws InvalidNameException
    {
        String result = new Rdn( "12.34.56 = azerty + 7.8 = test" ).toString();
        Assert.assertEquals( "12.34.56=azerty+7.8=test", result );
    }


    /**
     * test a simple RDN with pair char attribute value : a = \,\=\+\<\>\#\;\\\"\A0\00"
     */
    public void testRdnPairCharAttributeValue() throws InvalidNameException
    {
        Assert.assertEquals( "a=\\,\\=\\+\\<\\>\\#\\;\\\\\\\"\\A0\\00", new Rdn(
            "a = \\,\\=\\+\\<\\>\\#\\;\\\\\\\"\\A0\\00" ).toString() );
    }


    /**
     * test a simple RDN with hexString attribute value : a = #0010A0AAFF
     */
    public void testRdnHexStringAttributeValue() throws InvalidNameException
    {
        Assert.assertEquals( "a=#0010A0AAFF", new Rdn( "a = #0010A0AAFF" ).toString() );
    }


    /**
     * test a simple RDN with quoted attribute value : a = "quoted \"value"
     */
    public void testRdnQuotedAttributeValue() throws InvalidNameException
    {
        Assert.assertEquals( "a=quoted \\\"value", new Rdn( "a = quoted \\\"value" ).toString() );
    }


    /**
     * Test the clone method for a RDN.
     */
    public void testRDNCloningOneNameComponent() throws InvalidNameException
    {
        Rdn rdn = new Rdn( "a", "b" );

        Rdn rdnClone = ( Rdn ) rdn.clone();

        RdnParser.parse( "c=d", rdn );

        Assert.assertEquals( "b", rdnClone.getValue( "a" ) );
    }


    /**
     * Test the clone method for a RDN.
     */
    public void testRDNCloningTwoNameComponent() throws InvalidNameException
    {
        Rdn rdn = new Rdn( "a = b + aa = bb" );

        Rdn rdnClone = ( Rdn ) rdn.clone();

        rdn.clear();
        RdnParser.parse( "c=d", rdn );

        Assert.assertEquals( "b", rdnClone.getValue( "a" ) );
        Assert.assertEquals( "bb", rdnClone.getValue( "aa" ) );
        Assert.assertEquals( "", rdnClone.getValue( "c" ) );
    }


    /**
     * Test the compareTo method for a RDN.
     */
    public void testRDNCompareToNull() throws InvalidNameException
    {
        Rdn rdn1 = new Rdn( " a = b + c = d + a = f + g = h " );
        Rdn rdn2 = null;
        Assert.assertEquals( 1, rdn1.compareTo( rdn2 ) );
    }


    /**
     * Compares a composite NC to a single NC.
     */
    public void testRDNCompareToNCS2NC() throws InvalidNameException
    {
        Rdn rdn1 = new Rdn( " a = b + c = d + a = f + g = h " );
        Rdn rdn2 = new Rdn( " a = b " );
        Assert.assertTrue( rdn1.compareTo( rdn2 ) > 0 );
    }


    /**
     * Compares a single NC to a composite NC.
     */
    public void testRDNCompareToNC2NCS() throws InvalidNameException
    {
        Rdn rdn1 = new Rdn( " a = b " );
        Rdn rdn2 = new Rdn( " a = b + c = d + a = f + g = h " );

        Assert.assertTrue( rdn1.compareTo( rdn2 ) < 0 );
    }


    /**
     * Compares a composite NCS to a composite NCS in the same order.
     */
    public void testRDNCompareToNCS2NCSOrdered() throws InvalidNameException
    {
        Rdn rdn1 = new Rdn( " a = b + c = d + a = f + g = h " );
        Rdn rdn2 = new Rdn( " a = b + c = d + a = f + g = h " );

        Assert.assertEquals( 0, rdn1.compareTo( rdn2 ) );
    }


    /**
     * Compares a composite NCS to a composite NCS in a different order.
     */
    public void testRDNCompareToNCS2NCSUnordered() throws InvalidNameException
    {
        Rdn rdn1 = new Rdn( " a = b + a = f + g = h + c = d " );
        Rdn rdn2 = new Rdn( " a = b + c = d + a = f + g = h " );

        Assert.assertEquals( 0, rdn1.compareTo( rdn2 ) );
    }


    /**
     * Compares a composite NCS to a different composite NCS.
     */
    public void testRDNCompareToNCS2NCSNotEquals() throws InvalidNameException
    {
        Rdn rdn1 = new Rdn( " a = f + g = h + c = d " );
        Rdn rdn2 = new Rdn( " c = d + a = h + g = h " );

        Assert.assertEquals( -1, rdn1.compareTo( rdn2 ) );
    }


    /**
     * Compares a simple NC to a simple NC.
     */
    public void testRDNCompareToNC2NC() throws InvalidNameException
    {
        Rdn rdn1 = new Rdn( " a = b " );
        Rdn rdn2 = new Rdn( " a = b " );

        Assert.assertEquals( 0, rdn1.compareTo( rdn2 ) );
    }


    /**
     * Compares a simple NC to a simple NC in UperCase.
     */
    public void testRDNCompareToNC2NCUperCase() throws InvalidNameException
    {
        Rdn rdn1 = new Rdn( " a = b " );
        Rdn rdn2 = new Rdn( " A = b " );

        Assert.assertEquals( 0, rdn1.compareTo( rdn2 ) );
        Assert.assertEquals( true, rdn1.equals( rdn2 ) );
    }


    /**
     * Compares a simple NC to a different simple NC.
     */
    public void testRDNCompareToNC2NCNotEquals() throws InvalidNameException
    {
        Rdn rdn1 = new Rdn( " a = b " );
        Rdn rdn2 = new Rdn( " A = d " );

        Assert.assertTrue( rdn1.compareTo( rdn2 ) < 0 );
    }


    public void testToAttributes() throws InvalidNameException, NamingException
    {
        Rdn rdn = new Rdn( " a = b + a = f + g = h + c = d " );

        Attributes attributes = rdn.toAttributes();

        Assert.assertNotNull( attributes.get( "a" ) );
        Assert.assertNotNull( attributes.get( "g" ) );
        Assert.assertNotNull( attributes.get( "c" ) );

        Attribute attribute = attributes.get( "a" );

        Assert.assertNotNull( attribute.get( 0 ) );
        Assert.assertEquals( "b", attribute.get( 0 ) );

        Assert.assertNotNull( attribute.get( 1 ) );
        Assert.assertEquals( "f", attribute.get( 1 ) );

        attribute = attributes.get( "g" );
        Assert.assertNotNull( attribute.get( 0 ) );
        Assert.assertEquals( "h", attribute.get( 0 ) );

        attribute = attributes.get( "c" );
        Assert.assertNotNull( attribute.get( 0 ) );
        Assert.assertEquals( "d", attribute.get( 0 ) );
    }


    public void testGetValue() throws InvalidNameException
    {
        Rdn rdn = new Rdn( " a = b + a = f + g = h + c = d " );

        Assert.assertEquals( "b", rdn.getValue() );
    }


    public void testGetType() throws InvalidNameException
    {
        Rdn rdn = new Rdn( " a = b + a = f + g = h + c = d " );

        Assert.assertEquals( "a", rdn.getType() );
    }


    public void testGetSize() throws InvalidNameException
    {
        Rdn rdn = new Rdn( " a = b + a = f + g = h + c = d " );

        Assert.assertEquals( 4, rdn.size() );
    }


    public void testGetSize0() throws InvalidNameException
    {
        Rdn rdn = new Rdn();

        Assert.assertEquals( 0, rdn.size() );
    }


    public void testEquals() throws InvalidNameException
    {
        Rdn rdn = new Rdn( "a=b + c=d + a=f" );

        Assert.assertFalse( rdn.equals( null ) );
        Assert.assertFalse( rdn.equals( new String( "test" ) ) );
        Assert.assertFalse( rdn.equals( new Rdn( "a=c + c=d + a=f" ) ) );
        Assert.assertFalse( rdn.equals( new Rdn( "a=b" ) ) );
        Assert.assertTrue( rdn.equals( new Rdn( "a=b + c=d + a=f" ) ) );
        Assert.assertTrue( rdn.equals( new Rdn( "a=b + C=d + A=f" ) ) );
        Assert.assertTrue( rdn.equals( new Rdn( "c=d + a=f + a=b" ) ) );
    }


    public void testUnescapeValueHexa() throws InvalidNameException
    {
        byte[] res = ( byte[] ) Rdn.unescapeValue( "#fF" );

        Assert.assertEquals( "0xFF ", StringTools.dumpBytes( res ) );

        res = ( byte[] ) Rdn.unescapeValue( "#0123456789aBCDEF" );
        Assert.assertEquals( "0x01 0x23 0x45 0x67 0x89 0xAB 0xCD 0xEF ", StringTools.dumpBytes( res ) );
    }


    public void testUnescapeValueHexaWrong() throws InvalidNameException
    {
        try
        {
            Rdn.unescapeValue( "#fF1" );
            Assert.fail(); // Should not happen
        }
        catch ( IllegalArgumentException iae )
        {
            Assert.assertTrue( true );
        }
    }


    public void testUnescapeValueString() throws InvalidNameException
    {
        String res = ( String ) Rdn.unescapeValue( "azerty" );

        Assert.assertEquals( "azerty", res );
    }


    public void testUnescapeValueStringSpecial() throws InvalidNameException
    {
        String res = ( String ) Rdn.unescapeValue( "\\\\\\#\\,\\+\\;\\<\\>\\=\\\"\\ " );

        Assert.assertEquals( "\\#,+;<>=\" ", res );
    }


    public void testEscapeValueString() throws InvalidNameException
    {
        String res = Rdn.escapeValue( StringTools.getBytesUtf8( "azerty" ) );

        Assert.assertEquals( "azerty", res );
    }


    public void testEscapeValueStringSpecial() throws InvalidNameException
    {
        String res = Rdn.escapeValue( StringTools.getBytesUtf8( "\\#,+;<>=\" " ) );

        Assert.assertEquals( "\\\\\\#\\,\\+\\;\\<\\>\\=\\\"\\ ", res );
    }


    public void testEscapeValueNumeric() throws InvalidNameException
    {
        String res = Rdn.escapeValue( new byte[]
            { '-', 0x00, '-', 0x1F, '-', 0x7F, '-' } );

        Assert.assertEquals( "-\\00-\\1F-\\7F-", res );
    }


    public void testEscapeValueMix() throws InvalidNameException
    {
        String res = Rdn.escapeValue( new byte[]
            { '\\', 0x00, '-', '+', '#', 0x7F, '-' } );

        Assert.assertEquals( "\\\\\\00-\\+\\#\\7F-", res );
    }

}
