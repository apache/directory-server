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


import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.naming.CompoundName;
import javax.naming.InvalidNameException;
import javax.naming.Name;
import javax.naming.NamingException;
import javax.naming.ldap.LdapName;

import org.apache.directory.shared.ldap.name.LdapDN;
import org.apache.directory.shared.ldap.name.LdapDnParser;
import org.apache.directory.shared.ldap.name.Rdn;
import org.apache.directory.shared.ldap.schema.normalizers.DeepTrimToLowerNormalizer;
import org.apache.directory.shared.ldap.schema.normalizers.OidNormalizer;
import org.apache.directory.shared.ldap.util.StringTools;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.fail;
import static org.junit.Assert.assertSame;


/**
 * Test the class LdapDN
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$, 
 */
public class LdapDNTest
{
    private Map<String, OidNormalizer> oids;
    private Map<String, OidNormalizer> oidOids;


    /**
     * Initialize OIDs maps for normalization
     */
    @Before
    public void initMapOids()
    {
        oids = new HashMap<String, OidNormalizer>();

        oids.put( "dc", new OidNormalizer( "dc", new DeepTrimToLowerNormalizer() ) );
        oids.put( "domaincomponent", new OidNormalizer( "dc", new DeepTrimToLowerNormalizer() ) );
        oids.put( "0.9.2342.19200300.100.1.25", new OidNormalizer( "dc", new DeepTrimToLowerNormalizer() ) );

        oids.put( "ou", new OidNormalizer( "ou", new DeepTrimToLowerNormalizer() ) );
        oids.put( "organizationalUnitName", new OidNormalizer( "ou", new DeepTrimToLowerNormalizer() ) );
        oids.put( "2.5.4.11", new OidNormalizer( "ou", new DeepTrimToLowerNormalizer() ) );

        // Another map where we store OIDs instead of names.
        oidOids = new HashMap<String, OidNormalizer>();

        oidOids.put( "dc", new OidNormalizer( "0.9.2342.19200300.100.1.25", new DeepTrimToLowerNormalizer() ) );
        oidOids.put( "domaincomponent", new OidNormalizer( "0.9.2342.19200300.100.1.25",
            new DeepTrimToLowerNormalizer() ) );
        oidOids.put( "0.9.2342.19200300.100.1.25", new OidNormalizer( "0.9.2342.19200300.100.1.25",
            new DeepTrimToLowerNormalizer() ) );
        oidOids.put( "ou", new OidNormalizer( "2.5.4.11", new DeepTrimToLowerNormalizer() ) );
        oidOids.put( "organizationalUnitName", new OidNormalizer( "2.5.4.11", new DeepTrimToLowerNormalizer() ) );
        oidOids.put( "2.5.4.11", new OidNormalizer( "2.5.4.11", new DeepTrimToLowerNormalizer() ) );
    }


    // ~ Methods
    // ------------------------------------------------------------------------------------
    // CONSTRUCTOR functions --------------------------------------------------

    /**
     * Test a null DN
     */
    @Test
    public void testLdapDNNull()
    {
        LdapDN dn = new LdapDN();
        assertEquals( "", dn.getUpName() );
        assertTrue( dn.isEmpty() );
    }


    /**
     * test an empty DN
     */
    @Test
    public void testLdapDNEmpty() throws InvalidNameException
    {
        LdapDN dn = new LdapDN( "" );
        assertEquals( "", dn.getUpName() );
        assertTrue( dn.isEmpty() );
    }


    /**
     * test a simple DN : a = b
     */
    @Test
    public void testLdapDNSimple() throws InvalidNameException
    {
        LdapDN dn = new LdapDN( "a = b" );

        assertTrue( LdapDN.isValid( "a = b" ) );
        assertEquals( "a = b", dn.getUpName() );
        assertEquals( "a=b", dn.toString() );
    }


    /**
     * test a simple DN with some spaces : "a = b  "
     */
    @Test
    public void testLdapDNSimpleWithSpaces() throws InvalidNameException
    {
        LdapDN dn = new LdapDN( "a = b  " );

        assertTrue( LdapDN.isValid( "a = b  " ) );
        assertEquals( "a = b  ", dn.getUpName() );
        assertEquals( "a=b", dn.toString() );
    }


    /**
     * test a composite DN : a = b, d = e
     */
    @Test
    public void testLdapDNComposite() throws InvalidNameException
    {
        LdapDN dn = new LdapDN( "a = b, c = d" );

        assertTrue( LdapDN.isValid( "a = b, c = d" ) );
        assertEquals( "a=b,c=d", dn.toString() );
        assertEquals( "a = b, c = d", dn.getUpName() );
    }


    /**
     * test a composite DN with spaces : a = b  , d = e
     */
    @Test
    public void testLdapDNCompositeWithSpaces() throws InvalidNameException
    {
        LdapDN dn = new LdapDN( "a = b  , c = d" );

        assertTrue( LdapDN.isValid( "a = b  , c = d" ) );
        assertEquals( "a=b,c=d", dn.toString() );
        assertEquals( "a = b  , c = d", dn.getUpName() );
    }


    /**
     * test a composite DN with or without spaces: a=b, a =b, a= b, a = b, a = b
     */
    @Test
    public void testLdapDNCompositeWithSpace() throws InvalidNameException
    {
        LdapDN dn = new LdapDN( "a=b, a =b, a= b, a = b, a  =  b" );

        assertTrue( LdapDN.isValid( "a=b, a =b, a= b, a = b, a  =  b" ) );
        assertEquals( "a=b,a=b,a=b,a=b,a=b", dn.toString() );
        assertEquals( "a=b, a =b, a= b, a = b, a  =  b", dn.getUpName() );
    }


    /**
     * test a composite DN with differents separators : a=b;c=d,e=f It should
     * return a=b,c=d,e=f (the ';' is replaced by a ',')
     */
    @Test
    public void testLdapDNCompositeSepators() throws InvalidNameException
    {
        LdapDN dn = new LdapDN( "a=b;c=d,e=f" );

        assertTrue( LdapDN.isValid( "a=b;c=d,e=f" ) );
        assertEquals( "a=b,c=d,e=f", dn.toString() );
        assertEquals( "a=b;c=d,e=f", dn.getUpName() );
    }


    /**
     * test a simple DN with multiple NameComponents : a = b + c = d
     */
    @Test
    public void testLdapDNSimpleMultivaluedAttribute() throws InvalidNameException
    {
        LdapDN dn = new LdapDN( "a = b + c = d" );

        assertTrue( LdapDN.isValid( "a = b + c = d" ) );
        assertEquals( "a=b+c=d", dn.toString() );
        assertEquals( "a = b + c = d", dn.getUpName() );
    }


    /**
     * test a composite DN with multiple NC and separators : a=b+c=d, e=f + g=h +
     * i=j
     */
    @Test
    public void testLdapDNCompositeMultivaluedAttribute() throws InvalidNameException
    {
        LdapDN dn = new LdapDN( "a=b+c=d, e=f + g=h + i=j" );

        assertTrue( LdapDN.isValid( "a=b+c=d, e=f + g=h + i=j" ) );
        assertEquals( "a=b+c=d,e=f+g=h+i=j", dn.toString() );
        assertEquals( "a=b+c=d, e=f + g=h + i=j", dn.getUpName() );
    }


    /**
    * Test to see if a DN with multiRdn values is preserved after an addAll.
    */
    @Test
    public void testAddAllWithMultivaluedAttribute() throws InvalidNameException
    {
        LdapDN dn = new LdapDN( "cn=Kate Bush+sn=Bush,ou=system" );
        LdapDN target = new LdapDN();

        assertTrue( LdapDN.isValid( "cn=Kate Bush+sn=Bush,ou=system" ) );
        target.addAll( target.size(), dn );
        assertEquals( "cn=Kate Bush+sn=Bush,ou=system", target.toString() );
        assertEquals( "cn=Kate Bush+sn=Bush,ou=system", target.getUpName() );
    }


    /**
     * test a simple DN with an oid prefix (uppercase) : OID.12.34.56 = azerty
     */
    @Test
    public void testLdapDNOidUpper() throws InvalidNameException
    {
        LdapDN dn = new LdapDN( "OID.12.34.56 = azerty" );

        assertTrue( LdapDN.isValid( "OID.12.34.56 = azerty" ) );
        assertEquals( "oid.12.34.56=azerty", dn.toString() );
        assertEquals( "OID.12.34.56 = azerty", dn.getUpName() );
    }


    /**
     * test a simple DN with an oid prefix (lowercase) : oid.12.34.56 = azerty
     */
    @Test
    public void testLdapDNOidLower() throws InvalidNameException
    {
        LdapDN dn = new LdapDN( "oid.12.34.56 = azerty" );

        assertTrue( LdapDN.isValid( "oid.12.34.56 = azerty" ) );
        assertEquals( "oid.12.34.56=azerty", dn.toString() );
        assertEquals( "oid.12.34.56 = azerty", dn.getUpName() );
    }


    /**
     * test a simple DN with an oid attribut without oid prefix : 12.34.56 =
     * azerty
     */
    @Test
    public void testLdapDNOidWithoutPrefix() throws InvalidNameException
    {
        LdapDN dn = new LdapDN( "12.34.56 = azerty" );

        assertTrue( LdapDN.isValid( "12.34.56 = azerty" ) );
        assertEquals( "12.34.56=azerty", dn.toString() );
        assertEquals( "12.34.56 = azerty", dn.getUpName() );
    }


    /**
     * test a composite DN with an oid attribut wiithout oid prefix : 12.34.56 =
     * azerty; 7.8 = test
     */
    @Test
    public void testLdapDNCompositeOidWithoutPrefix() throws InvalidNameException
    {
        LdapDN dn = new LdapDN( "12.34.56 = azerty; 7.8 = test" );

        assertTrue( LdapDN.isValid( "12.34.56 = azerty; 7.8 = test" ) );
        assertEquals( "12.34.56=azerty,7.8=test", dn.toString() );
        assertEquals( "12.34.56 = azerty; 7.8 = test", dn.getUpName() );
    }


    /**
     * test a simple DN with pair char attribute value : a = \,\=\+\<\>\#\;\\\"\C4\8D"
     */
    @Test
    public void testLdapDNPairCharAttributeValue() throws InvalidNameException
    {
        LdapDN dn = new LdapDN( "a = \\,\\=\\+\\<\\>\\#\\;\\\\\\\"\\C4\\8D" );

        assertTrue( LdapDN.isValid( "a = \\,\\=\\+\\<\\>\\#\\;\\\\\\\"\\C4\\8D" ) );
        assertEquals( "a=\\,=\\+\\<\\>#\\;\\\\\\\"\u010D", dn.toString() );
        assertEquals( "a = \\,\\=\\+\\<\\>\\#\\;\\\\\\\"\\C4\\8D", dn.getUpName() );
    }


    /**
     * test a simple DN with pair char attribute value : "SN=Lu\C4\8Di\C4\87"
     */
    @Test
    public void testLdapDNRFC253_Lucic() throws InvalidNameException
    {
        LdapDN dn = new LdapDN( "SN=Lu\\C4\\8Di\\C4\\87" );

        assertTrue( LdapDN.isValid( "SN=Lu\\C4\\8Di\\C4\\87" ) );
        assertEquals( "sn=Lu\u010Di\u0107", dn.toString() );
        assertEquals( "SN=Lu\\C4\\8Di\\C4\\87", dn.getUpName() );
    }


    /**
     * test a simple DN with hexString attribute value : a = #0010A0AAFF
     */
    @Test
    public void testLdapDNHexStringAttributeValue() throws InvalidNameException
    {
        LdapDN dn = new LdapDN( "a = #0010A0AAFF" );

        assertTrue( LdapDN.isValid( "a = #0010A0AAFF" ) );
        assertEquals( "a=#0010A0AAFF", dn.toString() );
        assertEquals( "a = #0010A0AAFF", dn.getUpName() );
    }


    /**
     * Test for DIRSTUDIO-589, DIRSTUDIO-591, DIRSHARED-38 
     * 
     * Check escaped sharp followed by a hex sequence
     * (without the ESC it would be a valid hexstring).
     */
    @Test
    public void testLdapDNEscSharpNumber() throws InvalidNameException, NamingException
    {
        LdapDN dn = new LdapDN( "a = \\#123456" );

        assertTrue( LdapDN.isValid( "a = \\#123456" ) );
        assertEquals( "a=\\#123456", dn.toString() );
        assertEquals( "a = \\#123456", dn.getUpName() );

        Rdn rdn = dn.getRdn();
        assertEquals( "a = \\#123456", rdn.getUpName() );

        assertTrue( LdapDN.isValid( "a = \\#00" ) );
        assertTrue( LdapDN.isValid( "a = \\#11" ) );
        assertTrue( LdapDN.isValid( "a = \\#99" ) );
        assertTrue( LdapDN.isValid( "a = \\#AA" ) );
        assertTrue( LdapDN.isValid( "a = \\#FF" ) );

        assertTrue( LdapDN.isValid( "uid=\\#123456" ) );
        assertTrue( LdapDN.isValid( "cn=\\#ACL_AD-Projects_Author,ou=Notes_Group,o=Contacts,c=DE" ) );
        assertTrue( LdapDN.isValid( "cn=\\#Abraham" ) );
    }


   /**
     * test a simple DN with a # on first position
     */
    @Test
    public void testLdapDNSharpFirst() throws InvalidNameException, NamingException
    {
        LdapDN dn = new LdapDN( "a = \\#this is a sharp" );

        assertTrue( LdapDN.isValid( "a = \\#this is a sharp" ) );
        assertEquals( "a=\\#this is a sharp", dn.toString() );
        assertEquals( "a = \\#this is a sharp", dn.getUpName() );

        Rdn rdn = dn.getRdn();
        assertEquals( "a = \\#this is a sharp", rdn.getUpName() );
    }


    /**
     * Normalize a simple DN with a # on first position
     */
    @Test
    public void testNormalizeLdapDNSharpFirst() throws InvalidNameException, NamingException
    {
        LdapDN dn = new LdapDN( "ou = \\#this is a sharp" );

        assertTrue( LdapDN.isValid( "ou = \\#this is a sharp" ) );
        assertEquals( "ou=\\#this is a sharp", dn.toString() );
        assertEquals( "ou = \\#this is a sharp", dn.getUpName() );

        // Check the normalization now
        LdapDN ndn = dn.normalize( oidOids );

        assertEquals( "ou = \\#this is a sharp", ndn.getUpName() );
        assertEquals( "2.5.4.11=\\#this is a sharp", ndn.toString() );
    }


    /**
     * Normalize a DN with sequence ESC ESC HEX HEX (\\DC).
     * This is a corner case for the parser and normalizer.
     */
    @Test
    public void testNormalizeLdapDNEscEscHexHex() throws NamingException
    {
        LdapDN dn = new LdapDN( "ou = AC\\\\DC" );
        assertTrue( LdapDN.isValid( "ou = AC\\\\DC" ) );
        assertEquals( "ou=AC\\\\DC", dn.toString() );
        assertEquals( "ou = AC\\\\DC", dn.getUpName() );

        // Check the normalization now
        LdapDN ndn = dn.normalize( oidOids );
        assertEquals( "ou = AC\\\\DC", ndn.getUpName() );
        assertEquals( "2.5.4.11=ac\\\\dc", ndn.toString() );
    }


    /**
     * test a simple DN with a wrong hexString attribute value : a = #0010Z0AAFF
     */
    @Test
    public void testLdapDNWrongHexStringAttributeValue()
    {
        try
        {
            new LdapDN( "a = #0010Z0AAFF" );
            fail();
        }
        catch ( InvalidNameException ine )
        {

            assertFalse( LdapDN.isValid( "a = #0010Z0AAFF" ) );
            assertTrue( true );
        }
    }


    /**
     * test a simple DN with a wrong hexString attribute value : a = #AABBCCDD3
     */
    @Test
    public void testLdapDNWrongHexStringAttributeValue2()
    {
        try
        {
            new LdapDN( "a = #AABBCCDD3" );
            fail();
        }
        catch ( InvalidNameException ine )
        {
            assertFalse( LdapDN.isValid( "a = #AABBCCDD3" ) );
            assertTrue( true );
        }
    }


    /**
     * test a simple DN with a quote in attribute value : a = quoted \"value\"
     */
    @Test
    public void testLdapDNQuoteInAttributeValue() throws InvalidNameException
    {
        LdapDN dn = new LdapDN( "a = quoted \\\"value\\\"" );

        assertTrue( LdapDN.isValid( "a = quoted \\\"value\\\"" ) );
        assertEquals( "a=quoted \\\"value\\\"", dn.toString() );
        assertEquals( "a = quoted \\\"value\\\"", dn.getUpName() );
    }


    /**
     * test a simple DN with quoted attribute value : a = \" quoted value \"
     */
    @Test
    public void testLdapDNQuotedAttributeValue() throws InvalidNameException
    {
        LdapDN dn = new LdapDN( "a = \\\" quoted value \\\"" );

        assertTrue( LdapDN.isValid( "a = \\\" quoted value \\\"" ) );
        assertEquals( "a=\\\" quoted value \\\"", dn.toString() );
        assertEquals( "a = \\\" quoted value \\\"", dn.getUpName() );
    }


    /**
     * test a simple DN with a comma at the end
     */
    @Test
    public void testLdapDNComaAtEnd()
    {
        assertFalse( LdapDN.isValid( "a = b," ) );
        assertFalse( LdapDN.isValid( "a = b, " ) );

        try
        {
            new LdapDN( "a = b," );
            fail();
        }
        catch ( InvalidNameException ine )
        {
            assertTrue( true );
        }
    }


    // REMOVE operation -------------------------------------------------------

    /**
     * test a remove from position 0
     */
    @Test
    public void testLdapDNRemove0() throws InvalidNameException
    {
        LdapDN dn = new LdapDN( "a=b, c=d, e=f" );

        assertTrue( LdapDN.isValid( "a=b, c=d, e=f" ) );
        assertEquals( "e=f", dn.remove( 0 ).toString() );
        assertEquals( "a=b,c=d", dn.toString() );
        assertEquals( "a=b, c=d", dn.getUpName() );
    }


    /**
     * test a remove from position 1
     */
    @Test
    public void testLdapDNRemove1() throws InvalidNameException
    {
        LdapDN dn = new LdapDN( "a=b, c=d, e=f" );

        assertTrue( LdapDN.isValid( "a=b, c=d, e=f" ) );
        assertEquals( "c=d", dn.remove( 1 ).toString() );
        assertEquals( "a=b, e=f", dn.getUpName() );
    }


    /**
     * test a remove from position 2
     */
    @Test
    public void testLdapDNRemove2() throws InvalidNameException
    {
        LdapDN dn = new LdapDN( "a=b, c=d, e=f" );

        assertTrue( LdapDN.isValid( "a=b, c=d, e=f" ) );
        assertEquals( "a=b", dn.remove( 2 ).toString() );
        assertEquals( " c=d, e=f", dn.getUpName() );
    }


    /**
     * test a remove from position 1 whith semi colon
     */
    @Test
    public void testLdapDNRemove1WithSemiColon() throws InvalidNameException
    {
        LdapDN dn = new LdapDN( "a=b, c=d; e=f" );

        assertTrue( LdapDN.isValid( "a=b, c=d; e=f" ) );
        assertEquals( "c=d", dn.remove( 1 ).toString() );
        assertEquals( "a=b, e=f", dn.getUpName() );
    }


    /**
     * test a remove out of bound
     */
    @Test
    public void testLdapDNRemoveOutOfBound() throws InvalidNameException
    {
        LdapDN dn = new LdapDN( "a=b, c=d; e=f" );

        assertTrue( LdapDN.isValid( "a=b, c=d; e=f" ) );

        try
        {
            dn.remove( 4 );
            // We whould never reach this point
            fail();
        }
        catch ( ArrayIndexOutOfBoundsException aoobe )
        {
            assertTrue( true );
        }
    }


    // SIZE operations
    /**
     * test a 0 size
     */
    @Test
    public void testLdapDNSize0()
    {
        LdapDN dn = new LdapDN();

        assertTrue( LdapDN.isValid( "" ) );
        assertEquals( 0, dn.size() );
    }


    /**
     * test a 1 size
     */
    @Test
    public void testLdapDNSize1() throws InvalidNameException
    {
        LdapDN dn = new LdapDN( "a=b" );

        assertTrue( LdapDN.isValid( "a=b" ) );
        assertEquals( 1, dn.size() );
    }


    /**
     * test a 3 size
     */
    @Test
    public void testLdapDNSize3() throws InvalidNameException
    {
        LdapDN dn = new LdapDN( "a=b, c=d, e=f" );

        assertTrue( LdapDN.isValid( "a=b, c=d, e=f" ) );
        assertEquals( 3, dn.size() );
    }


    /**
     * test a 3 size with NameComponents
     */
    @Test
    public void testLdapDNSize3NC() throws InvalidNameException
    {
        LdapDN dn = new LdapDN( "a=b+c=d, c=d, e=f" );

        assertTrue( LdapDN.isValid( "a=b+c=d, c=d, e=f" ) );
        assertEquals( 3, dn.size() );
    }


    /**
     * test size after operations
     */
    @Test
    public void testLdapResizing() throws InvalidNameException
    {
        LdapDN dn = new LdapDN();
        assertEquals( 0, dn.size() );

        dn.add( "e = f" );
        assertEquals( 1, dn.size() );

        dn.add( "c = d" );
        assertEquals( 2, dn.size() );

        dn.remove( 0 );
        assertEquals( 1, dn.size() );

        dn.remove( 0 );
        assertEquals( 0, dn.size() );
    }


    // ADD Operations
    /**
     * test Add on a new LdapDN
     */
    @Test
    public void testLdapEmptyAdd() throws InvalidNameException
    {
        LdapDN dn = new LdapDN();

        dn.add( "e = f" );
        assertEquals( "e=f", dn.toString() );
        assertEquals( "e = f", dn.getUpName() );
        assertEquals( 1, dn.size() );
    }


    /**
     * test Add to an existing LdapDN
     */
    @Test
    public void testLdapDNAdd() throws InvalidNameException
    {
        LdapDN dn = new LdapDN( "a=b, c=d" );

        dn.add( "e = f" );
        assertEquals( "e=f,a=b,c=d", dn.toString() );
        assertEquals( "e = f,a=b, c=d", dn.getUpName() );
        assertEquals( 3, dn.size() );
    }


    /**
     * test Add a composite RDN to an existing LdapDN
     */
    @Test
    public void testLdapDNAddComposite() throws InvalidNameException
    {
        LdapDN dn = new LdapDN( "a=b, c=d" );

        dn.add( "e = f + g = h" );

        // Warning ! The order of AVAs has changed during the parsing
        // This has no impact on the correctness of the DN, but the
        // String used to do the comparizon should be inverted.
        assertEquals( "e=f+g=h,a=b,c=d", dn.toString() );
        assertEquals( 3, dn.size() );
    }


    /**
     * test Add at the end of an existing LdapDN
     */
    @Test
    public void testLdapDNAddEnd() throws InvalidNameException
    {
        LdapDN dn = new LdapDN( "a=b, c=d" );

        dn.add( dn.size(), "e = f" );
        assertEquals( "e = f,a=b, c=d", dn.getUpName() );
        assertEquals( 3, dn.size() );
    }


    /**
     * test Add at the start of an existing LdapDN
     */
    @Test
    public void testLdapDNAddStart() throws InvalidNameException
    {
        LdapDN dn = new LdapDN( "a=b, c=d" );

        dn.add( 0, "e = f" );
        assertEquals( "a=b, c=d,e = f", dn.getUpName() );
        assertEquals( 3, dn.size() );
    }


    /**
     * test Add at the middle of an existing LdapDN
     */
    @Test
    public void testLdapDNAddMiddle() throws InvalidNameException
    {
        LdapDN dn = new LdapDN( "a=b, c=d" );

        dn.add( 1, "e = f" );
        assertEquals( "a=b,e = f, c=d", dn.getUpName() );
        assertEquals( 3, dn.size() );
    }


    // ADD ALL Operations
    /**
     * Test AddAll
     *
     * @throws InvalidNameException
     */
    @Test
    public void testLdapDNAddAll() throws InvalidNameException
    {
        LdapDN dn = new LdapDN( "a = b" );
        LdapDN dn2 = new LdapDN( "c = d" );
        dn.addAll( dn2 );
        assertEquals( "c = d,a = b", dn.getUpName() );
    }


    /**
     * Test AddAll with an empty added name
     *
     * @throws InvalidNameException
     */
    @Test
    public void testLdapDNAddAllAddedNameEmpty() throws InvalidNameException
    {
        LdapDN dn = new LdapDN( "a = b" );
        LdapDN dn2 = new LdapDN();
        dn.addAll( dn2 );
        assertEquals( "a=b", dn.toString() );
        assertEquals( "a = b", dn.getUpName() );
    }


    /**
     * Test AddAll to an empty name
     *
     * @throws InvalidNameException
     */
    @Test
    public void testLdapDNAddAllNameEmpty() throws InvalidNameException
    {
        LdapDN dn = new LdapDN();
        LdapDN dn2 = new LdapDN( "a = b" );
        dn.addAll( dn2 );
        assertEquals( "a = b", dn.getUpName() );
    }


    /**
     * Test AddAll at position 0
     *
     * @throws InvalidNameException
     */
    @Test
    public void testLdapDNAt0AddAll() throws InvalidNameException
    {
        LdapDN dn = new LdapDN( "a = b" );
        LdapDN dn2 = new LdapDN( "c = d" );
        dn.addAll( 0, dn2 );
        assertEquals( "a = b,c = d", dn.getUpName() );
    }


    /**
     * Test AddAll at position 1
     *
     * @throws InvalidNameException
     */
    @Test
    public void testLdapDNAt1AddAll() throws InvalidNameException
    {
        LdapDN dn = new LdapDN( "a = b" );
        LdapDN dn2 = new LdapDN( "c = d" );
        dn.addAll( 1, dn2 );
        assertEquals( "c = d,a = b", dn.getUpName() );
    }


    /**
     * Test AddAll at the middle
     *
     * @throws InvalidNameException
     */
    @Test
    public void testLdapDNAtTheMiddleAddAll() throws InvalidNameException
    {
        LdapDN dn = new LdapDN( "a = b, c = d" );
        LdapDN dn2 = new LdapDN( "e = f" );
        dn.addAll( 1, dn2 );
        assertEquals( "a = b,e = f, c = d", dn.getUpName() );
    }


    /**
     * Test AddAll with an empty added name at position 0
     *
     * @throws InvalidNameException
     */
    @Test
    public void testLdapDNAddAllAt0AddedNameEmpty() throws InvalidNameException
    {
        LdapDN dn = new LdapDN( "a = b" );
        LdapDN dn2 = new LdapDN();
        dn.addAll( 0, dn2 );
        assertEquals( "a=b", dn.toString() );
        assertEquals( "a = b", dn.getUpName() );
    }


    /**
     * Test AddAll to an empty name at position 0
     *
     * @throws InvalidNameException
     */
    @Test
    public void testLdapDNAddAllAt0NameEmpty() throws InvalidNameException
    {
        LdapDN dn = new LdapDN();
        LdapDN dn2 = new LdapDN( "a = b" );
        dn.addAll( 0, dn2 );
        assertEquals( "a = b", dn.getUpName() );
    }


    // GET PREFIX actions
    /**
     * Get the prefix at pos 0
     */
    @Test
    public void testLdapDNGetPrefixPos0() throws InvalidNameException
    {
        LdapDN dn = new LdapDN( "a=b, c=d,e = f" );
        LdapDN newDn = ( ( LdapDN ) dn.getPrefix( 0 ) );
        assertEquals( "", newDn.getUpName() );
    }


    /**
     * Get the prefix at pos 1
     */
    @Test
    public void testLdapDNGetPrefixPos1() throws InvalidNameException
    {
        LdapDN dn = new LdapDN( "a=b, c=d,e = f" );
        LdapDN newDn = ( ( LdapDN ) dn.getPrefix( 1 ) );
        assertEquals( "e = f", newDn.getUpName() );
    }


    /**
     * Get the prefix at pos 2
     */
    @Test
    public void testLdapDNGetPrefixPos2() throws InvalidNameException
    {
        LdapDN dn = new LdapDN( "a=b, c=d,e = f" );
        LdapDN newDn = ( ( LdapDN ) dn.getPrefix( 2 ) );
        assertEquals( " c=d,e = f", newDn.getUpName() );
    }


    /**
     * Get the prefix at pos 3
     */
    @Test
    public void testLdapDNGetPrefixPos3() throws InvalidNameException
    {
        LdapDN dn = new LdapDN( "a=b, c=d,e = f" );
        LdapDN newDn = ( ( LdapDN ) dn.getPrefix( 3 ) );
        assertEquals( "a=b, c=d,e = f", newDn.getUpName() );
    }


    /**
     * Get the prefix out of bound
     */
    @Test
    public void testLdapDNGetPrefixPos4() throws InvalidNameException
    {
        LdapDN dn = new LdapDN( "a=b, c=d,e = f" );

        try
        {
            dn.getPrefix( 4 );
            // We should not reach this point.
            fail();
        }
        catch ( ArrayIndexOutOfBoundsException aoobe )
        {
            assertTrue( true );
        }
    }


    /**
     * Get the prefix of an empty LdapName
     */
    @Test
    public void testLdapDNGetPrefixEmptyDN()
    {
        LdapDN dn = new LdapDN();
        LdapDN newDn = ( ( LdapDN ) dn.getPrefix( 0 ) );
        assertEquals( "", newDn.getUpName() );
    }


    // GET SUFFIX operations
    /**
     * Get the suffix at pos 0
     */
    @Test
    public void testLdapDNGetSuffixPos0() throws InvalidNameException
    {
        LdapDN dn = new LdapDN( "a=b, c=d,e = f" );
        LdapDN newDn = ( ( LdapDN ) dn.getSuffix( 0 ) );
        assertEquals( "a=b, c=d,e = f", newDn.getUpName() );
    }


    /**
     * Get the suffix at pos 1
     */
    @Test
    public void testLdapDNGetSuffixPos1() throws InvalidNameException
    {
        LdapDN dn = new LdapDN( "a=b, c=d,e = f" );
        LdapDN newDn = ( ( LdapDN ) dn.getSuffix( 1 ) );
        assertEquals( "a=b, c=d", newDn.getUpName() );
    }


    /**
     * Get the suffix at pos 2
     */
    @Test
    public void testLdapDNGetSuffixPos2() throws InvalidNameException
    {
        LdapDN dn = new LdapDN( "a=b, c=d,e = f" );
        LdapDN newDn = ( ( LdapDN ) dn.getSuffix( 2 ) );
        assertEquals( "a=b", newDn.getUpName() );
    }


    /**
     * Get the suffix at pos 3
     */
    @Test
    public void testLdapDNGetSuffixPos3() throws InvalidNameException
    {
        LdapDN dn = new LdapDN( "a=b, c=d,e = f" );
        LdapDN newDn = ( ( LdapDN ) dn.getSuffix( 3 ) );
        assertEquals( "", newDn.getUpName() );
    }


    /**
     * Get the suffix out of bound
     */
    @Test
    public void testLdapDNGetSuffixPos4() throws InvalidNameException
    {
        LdapDN dn = new LdapDN( "a=b, c=d,e = f" );

        try
        {
            dn.getSuffix( 4 );
            // We should not reach this point.
            fail();
        }
        catch ( ArrayIndexOutOfBoundsException aoobe )
        {
            assertTrue( true );
        }
    }


    /**
     * Get the suffix of an empty LdapName
     */
    @Test
    public void testLdapDNGetSuffixEmptyDN()
    {
        LdapDN dn = new LdapDN();
        LdapDN newDn = ( ( LdapDN ) dn.getSuffix( 0 ) );
        assertEquals( "", newDn.getUpName() );
    }


    // IS EMPTY operations
    /**
     * Test that a LdapDN is empty
     */
    @Test
    public void testLdapDNIsEmpty()
    {
        LdapDN dn = new LdapDN();
        assertEquals( true, dn.isEmpty() );
    }


    /**
     * Test that a LdapDN is empty
     */
    @Test
    public void testLdapDNNotEmpty() throws InvalidNameException
    {
        LdapDN dn = new LdapDN( "a=b" );
        assertEquals( false, dn.isEmpty() );
    }


    /**
     * Test that a LdapDN is empty
     */
    @Test
    public void testLdapDNRemoveIsEmpty() throws InvalidNameException
    {
        LdapDN dn = new LdapDN( "a=b, c=d" );
        dn.remove( 0 );
        dn.remove( 0 );

        assertEquals( true, dn.isEmpty() );
    }


    // STARTS WITH operations
    /**
     * Test a startsWith a null LdapDN
     */
    @Test
    public void testLdapDNStartsWithNull() throws InvalidNameException
    {
        LdapDN dn = new LdapDN( "a=b, c=d,e = f" );
        assertEquals( true, dn.startsWith( null ) );
    }


    /**
     * Test a startsWith an empty LdapDN
     */
    @Test
    public void testLdapDNStartsWithEmpty() throws InvalidNameException
    {
        LdapDN dn = new LdapDN( "a=b, c=d,e = f" );
        assertEquals( true, dn.startsWith( new LdapDN() ) );
    }


    /**
     * Test a startsWith an simple LdapDN
     */
    @Test
    public void testLdapDNStartsWithSimple() throws InvalidNameException
    {
        LdapDN dn = new LdapDN( "a=b, c=d,e = f" );
        assertEquals( true, dn.startsWith( new LdapDN( "e=f" ) ) );
    }


    /**
     * Test a startsWith a complex LdapDN
     */
    @Test
    public void testLdapDNStartsWithComplex() throws InvalidNameException
    {
        LdapDN dn = new LdapDN( "a=b, c=d,e = f" );
        assertEquals( true, dn.startsWith( new LdapDN( "c =  d, e =  f" ) ) );
    }


    /**
     * Test a startsWith a complex LdapDN
     */
    @Test
    public void testLdapDNStartsWithComplexMixedCase() throws InvalidNameException
    {
        LdapDN dn = new LdapDN( "a=b, c=d,e = f" );
        assertEquals( false, dn.startsWith( new LdapDN( "c =  D, E =  f" ) ) );
    }


    /**
     * Test a startsWith a full LdapDN
     */
    @Test
    public void testLdapDNStartsWithFull() throws InvalidNameException
    {
        LdapDN dn = new LdapDN( "a=b, c=d,e = f" );
        assertEquals( true, dn.startsWith( new LdapDN( "a=  b; c =  d, e =  f" ) ) );
    }


    /**
     * Test a startsWith which returns false
     */
    @Test
    public void testLdapDNStartsWithWrong() throws InvalidNameException
    {
        LdapDN dn = new LdapDN( "a=b, c=d,e = f" );
        assertEquals( false, dn.startsWith( new LdapDN( "c =  t, e =  f" ) ) );
    }


    // ENDS WITH operations
    /**
     * Test a endsWith a null LdapDN
     */
    @Test
    public void testLdapDNEndsWithNull() throws InvalidNameException
    {
        LdapDN dn = new LdapDN( "a=b, c=d,e = f" );
        assertEquals( true, dn.endsWith( null ) );
    }


    /**
     * Test a endsWith an empty LdapDN
     */
    @Test
    public void testLdapDNEndsWithEmpty() throws InvalidNameException
    {
        LdapDN dn = new LdapDN( "a=b, c=d,e = f" );
        assertEquals( true, dn.endsWith( new LdapDN() ) );
    }


    /**
     * Test a endsWith an simple LdapDN
     */
    @Test
    public void testLdapDNEndsWithSimple() throws InvalidNameException
    {
        LdapDN dn = new LdapDN( "a=b, c=d,e = f" );
        assertEquals( true, dn.endsWith( new LdapDN( "a=b" ) ) );
    }


    /**
     * Test a endsWith a complex LdapDN
     */
    @Test
    public void testLdapDNEndsWithComplex() throws InvalidNameException
    {
        LdapDN dn = new LdapDN( "a=b, c=d,e = f" );
        assertEquals( true, dn.endsWith( new LdapDN( "a =  b, c =  d" ) ) );
    }


    /**
     * Test a endsWith a complex LdapDN
     */
    @Test
    public void testLdapDNEndsWithComplexMixedCase() throws InvalidNameException
    {
        LdapDN dn = new LdapDN( "a=b, c=d,e = f" );
        assertEquals( false, dn.endsWith( new LdapDN( "a =  B, C =  d" ) ) );
    }


    /**
     * Test a endsWith a full LdapDN
     */
    @Test
    public void testLdapDNEndsWithFull() throws InvalidNameException
    {
        LdapDN dn = new LdapDN( "a=b, c=d,e = f" );
        assertEquals( true, dn.endsWith( new LdapDN( "a=  b; c =  d, e =  f" ) ) );
    }


    /**
     * Test a endsWith which returns false
     */
    @Test
    public void testLdapDNEndsWithWrong() throws InvalidNameException
    {
        LdapDN dn = new LdapDN( "a=b, c=d,e = f" );
        assertEquals( false, dn.endsWith( new LdapDN( "a =  b, e =  f" ) ) );
    }


    // GET ALL operations
    /**
     * test a getAll operation on a null DN
     */
    @Test
    public void testLdapDNGetAllNull()
    {
        LdapDN dn = new LdapDN();
        Enumeration<String> nc = dn.getAll();

        assertEquals( false, nc.hasMoreElements() );
    }


    /**
     * test a getAll operation on an empty DN
     */
    @Test
    public void testLdapDNGetAllEmpty() throws InvalidNameException
    {
        LdapDN dn = new LdapDN( "" );
        Enumeration<String> nc = dn.getAll();

        assertEquals( false, nc.hasMoreElements() );
    }


    /**
     * test a getAll operation on a simple DN
     */
    @Test
    public void testLdapDNGetAllSimple() throws InvalidNameException
    {
        LdapDN dn = new LdapDN( "a=b" );
        Enumeration<String> nc = dn.getAll();

        assertEquals( true, nc.hasMoreElements() );
        assertEquals( "a=b", nc.nextElement() );
        assertEquals( false, nc.hasMoreElements() );
    }


    /**
     * test a getAll operation on a complex DN
     */
    @Test
    public void testLdapDNGetAllComplex() throws InvalidNameException
    {
        LdapDN dn = new LdapDN( "e=f+g=h,a=b,c=d" );
        Enumeration<String> nc = dn.getAll();

        assertEquals( true, nc.hasMoreElements() );
        assertEquals( "c=d", nc.nextElement() );
        assertEquals( true, nc.hasMoreElements() );
        assertEquals( "a=b", nc.nextElement() );
        assertEquals( true, nc.hasMoreElements() );
        assertEquals( "e=f+g=h", nc.nextElement() );
        assertEquals( false, nc.hasMoreElements() );
    }


    /**
     * test a getAll operation on a complex DN
     */
    @Test
    public void testLdapDNGetAllComplexOrdered() throws InvalidNameException
    {
        LdapDN dn = new LdapDN( "g=h+e=f,a=b,c=d" );
        Enumeration<String> nc = dn.getAll();

        assertEquals( true, nc.hasMoreElements() );
        assertEquals( "c=d", nc.nextElement() );
        assertEquals( true, nc.hasMoreElements() );
        assertEquals( "a=b", nc.nextElement() );
        assertEquals( true, nc.hasMoreElements() );

        // The lowest atav should be the first one
        assertEquals( "e=f+g=h", nc.nextElement() );
        assertEquals( false, nc.hasMoreElements() );
    }


    // CLONE Operation
    /**
     * test a clone operation on a empty DN
     */
    @Test
    public void testLdapDNCloneEmpty()
    {
        LdapDN dn = new LdapDN();
        LdapDN clone = ( LdapDN ) dn.clone();

        assertEquals( "", clone.getUpName() );
    }


    /**
     * test a clone operation on a simple DN
     */
    @Test
    public void testLdapDNCloneSimple() throws InvalidNameException
    {
        LdapDN dn = new LdapDN( "a=b" );
        LdapDN clone = ( LdapDN ) dn.clone();

        assertEquals( "a=b", clone.getUpName() );
        dn.remove( 0 );
        assertEquals( "a=b", clone.getUpName() );
    }


    /**
     * test a clone operation on a complex DN
     */
    @Test
    public void testLdapDNCloneComplex() throws InvalidNameException
    {
        LdapDN dn = new LdapDN( "e=f+g=h,a=b,c=d" );
        LdapDN clone = ( LdapDN ) dn.clone();

        assertEquals( "e=f+g=h,a=b,c=d", clone.getUpName() );
        dn.remove( 2 );
        assertEquals( "e=f+g=h,a=b,c=d", clone.getUpName() );
    }


    // GET operations
    /**
     * test a get in a null DN
     */
    @Test
    public void testLdapDNGetNull()
    {
        LdapDN dn = new LdapDN();
        assertEquals( "", dn.get( 0 ) );
    }


    /**
     * test a get in an empty DN
     */
    @Test
    public void testLdapDNGetEmpty() throws InvalidNameException
    {
        LdapDN dn = new LdapDN( "" );
        assertEquals( "", dn.get( 0 ) );
    }


    /**
     * test a get in a simple DN
     */
    @Test
    public void testLdapDNGetSimple() throws InvalidNameException
    {
        LdapDN dn = new LdapDN( "a = b" );
        assertEquals( "a=b", dn.get( 0 ) );
    }


    /**
     * test a get in a complex DN
     */
    @Test
    public void testLdapDNGetComplex() throws InvalidNameException
    {
        LdapDN dn = new LdapDN( "a = b + c= d, e= f; g =h" );
        assertEquals( "g=h", dn.get( 0 ) );
        assertEquals( "e=f", dn.get( 1 ) );
        assertEquals( "a=b+c=d", dn.get( 2 ) );
    }


    /**
     * test a get out of bound
     */
    @Test
    public void testLdapDNGetOutOfBound() throws InvalidNameException
    {
        LdapDN dn = new LdapDN( "a = b + c= d, e= f; g =h" );

        try
        {
            dn.get( 4 );
            fail();
        }
        catch ( IndexOutOfBoundsException aioob )
        {
            assertTrue( true );
        }
    }


    /**
     * Tests the examples from the JNDI tutorials to make sure LdapName behaves
     * appropriately. The example can be found online <a href="">here</a>.
     *
     * @throws Exception
     *             if anything goes wrong
     */
    @Test
    public void testJNDITutorialExample() throws Exception
    {
        // Parse the name
        Name name = new LdapDN( "cn=John,ou=People,ou=Marketing" );

        // Remove the second component from the head: ou=People
        String out = name.remove( 1 ).toString();

        assertEquals( "ou=People", out );

        // Add to the head (first): cn=John,ou=Marketing,ou=East
        out = name.add( 0, "ou=East" ).toString();

        assertEquals( "cn=John,ou=Marketing,ou=East", out );

        // Add to the tail (last): cn=HomeDir,cn=John,ou=Marketing,ou=East
        out = name.add( "cn=HomeDir" ).toString();

        assertEquals( "cn=HomeDir,cn=John,ou=Marketing,ou=East", out );
    }


    @Test
    public void testAttributeEqualsIsCaseInSensitive() throws Exception
    {
        Name name1 = new LdapDN( "cn=HomeDir" );
        Name name2 = new LdapDN( "CN=HomeDir" );

        assertTrue( name1.equals( name2 ) );
    }


    @Test
    public void testAttributeTypeEqualsIsCaseInsensitive() throws Exception
    {
        Name name1 = new LdapDN( "cn=HomeDir+cn=WorkDir" );
        Name name2 = new LdapDN( "cn=HomeDir+CN=WorkDir" );

        assertTrue( name1.equals( name2 ) );
    }


    @Test
    public void testNameEqualsIsInsensitiveToAttributesOrder() throws Exception
    {

        Name name1 = new LdapDN( "cn=HomeDir+cn=WorkDir" );
        Name name2 = new LdapDN( "cn=WorkDir+cn=HomeDir" );

        assertTrue( name1.equals( name2 ) );
    }


    @Test
    public void testAttributeComparisonIsCaseInSensitive() throws Exception
    {
        Name name1 = new LdapDN( "cn=HomeDir" );
        Name name2 = new LdapDN( "CN=HomeDir" );

        assertEquals( 0, name1.compareTo( name2 ) );
    }


    @Test
    public void testAttributeTypeComparisonIsCaseInsensitive() throws Exception
    {
        Name name1 = new LdapDN( "cn=HomeDir+cn=WorkDir" );
        Name name2 = new LdapDN( "cn=HomeDir+CN=WorkDir" );

        assertEquals( 0, name1.compareTo( name2 ) );
    }


    @Test
    public void testNameComparisonIsInsensitiveToAttributesOrder() throws Exception
    {

        Name name1 = new LdapDN( "cn=HomeDir+cn=WorkDir" );
        Name name2 = new LdapDN( "cn=WorkDir+cn=HomeDir" );

        assertEquals( 0, name1.compareTo( name2 ) );
    }


    @Test
    public void testNameComparisonIsInsensitiveToAttributesOrderFailure() throws Exception
    {

        Name name1 = new LdapDN( "cn= HomeDir+cn=Workdir" );
        Name name2 = new LdapDN( "cn = Work+cn=HomeDir" );

        assertEquals( 1, name1.compareTo( name2 ) );
    }


    /**
     * Test the encoding of a LdanDN
     */
    @Test
    public void testNameToBytes() throws Exception
    {
        LdapDN dn = new LdapDN( "cn = John, ou = People, OU = Marketing" );

        byte[] bytes = LdapDN.getBytes( dn );

        assertEquals( 30, LdapDN.getNbBytes( dn ) );
        assertEquals( "cn=John,ou=People,ou=Marketing", new String( bytes, "UTF-8" ) );
    }


    @Test
    public void testStringParser() throws Exception
    {
        String dn = StringTools.utf8ToString( new byte[]
            { 'C', 'N', ' ', '=', ' ', 'E', 'm', 'm', 'a', 'n', 'u', 'e', 'l', ' ', ' ', 'L', ( byte ) 0xc3,
                ( byte ) 0xa9, 'c', 'h', 'a', 'r', 'n', 'y' } );

        Name name = LdapDnParser.getNameParser().parse( dn );

        assertEquals( dn, ( ( LdapDN ) name ).getUpName() );
        assertEquals( "cn=Emmanuel  L\u00E9charny", name.toString() );
    }


    /**
     * Class to test for void LdapName(String)
     *
     * @throws Exception
     *             if anything goes wrong.
     */
    @Test
    public void testLdapNameString() throws Exception
    {
        Name name = new LdapDN( "" );
        Name name50 = new LdapDN();
        assertEquals( name50, name );

        Name name0 = new LdapDN( "ou=Marketing,ou=East" );
        Name copy = new LdapDN( "ou=Marketing,ou=East" );
        Name name1 = new LdapDN( "cn=John,ou=Marketing,ou=East" );
        Name name2 = new LdapDN( "cn=HomeDir,cn=John,ou=Marketing,ou=East" );
        Name name3 = new LdapDN( "cn=HomeDir,cn=John,ou=Marketing,ou=West" );
        Name name4 = new LdapDN( "cn=Website,cn=John,ou=Marketing,ou=West" );
        Name name5 = new LdapDN( "cn=Airline,cn=John,ou=Marketing,ou=West" );

        assertTrue( name0.compareTo( copy ) == 0 );
        assertTrue( name0.compareTo( name1 ) < 0 );
        assertTrue( name0.compareTo( name2 ) < 0 );
        assertTrue( name1.compareTo( name2 ) < 0 );
        assertTrue( name2.compareTo( name1 ) > 0 );
        assertTrue( name2.compareTo( name0 ) > 0 );
        assertTrue( name2.compareTo( name3 ) < 0 );
        assertTrue( name2.compareTo( name4 ) < 0 );
        assertTrue( name3.compareTo( name4 ) < 0 );
        assertTrue( name3.compareTo( name5 ) > 0 );
        assertTrue( name4.compareTo( name5 ) > 0 );
        assertTrue( name2.compareTo( name5 ) < 0 );
    }


    /**
     * Class to test for void LdapName()
     */
    @Test
    public void testLdapName()
    {
        Name name = new LdapDN();
        assertTrue( name.toString().equals( "" ) );
    }


    /**
     * Class to test for void LdapName(List)
     */
    @Test
    public void testLdapNameList() throws InvalidNameException
    {
        List<String> list = new ArrayList<String>();
        list.add( "ou=People" );
        list.add( "dc=example" );
        list.add( "dc=com" );
        Name name = new LdapDN( list );
        assertTrue( name.toString().equals( "ou=People,dc=example,dc=com" ) );
    }


    /**
     * Class to test for void LdapName(Iterator)
     */
    @Test
    public void testLdapNameIterator() throws InvalidNameException
    {
        List<String> list = new ArrayList<String>();
        list.add( "ou=People" );
        list.add( "dc=example" );
        list.add( "dc=com" );
        Name name = new LdapDN( list.iterator() );
        assertTrue( name.toString().equals( "ou=People,dc=example,dc=com" ) );
    }


    /**
     * Class to test for Object clone()
     *
     * @throws Exception
     *             if anything goes wrong.
     */
    @Test
    public void testClone() throws Exception
    {
        String strName = "cn=HomeDir,cn=John,ou=Marketing,ou=East";
        Name name = new LdapDN( strName );
        assertEquals( name, name.clone() );
    }


    /**
     * Class to test for compareTo
     *
     * @throws Exception
     *             if anything goes wrong.
     */
    @Test
    public void testCompareTo() throws Exception
    {
        Name name0 = new LdapDN( "ou=Marketing,ou=East" );
        Name copy = new LdapDN( "ou=Marketing,ou=East" );
        Name name1 = new LdapDN( "cn=John,ou=Marketing,ou=East" );
        Name name2 = new LdapDN( "cn=HomeDir,cn=John,ou=Marketing,ou=East" );
        Name name3 = new LdapDN( "cn=HomeDir,cn=John,ou=Marketing,ou=West" );
        Name name4 = new LdapDN( "cn=Website,cn=John,ou=Marketing,ou=West" );
        Name name5 = new LdapDN( "cn=Airline,cn=John,ou=Marketing,ou=West" );

        assertTrue( name0.compareTo( copy ) == 0 );
        assertTrue( name0.compareTo( name1 ) < 0 );
        assertTrue( name0.compareTo( name2 ) < 0 );
        assertTrue( name1.compareTo( name2 ) < 0 );
        assertTrue( name2.compareTo( name1 ) > 0 );
        assertTrue( name2.compareTo( name0 ) > 0 );
        assertTrue( name2.compareTo( name3 ) < 0 );
        assertTrue( name2.compareTo( name4 ) < 0 );
        assertTrue( name3.compareTo( name4 ) < 0 );
        assertTrue( name3.compareTo( name5 ) > 0 );
        assertTrue( name4.compareTo( name5 ) > 0 );
        assertTrue( name2.compareTo( name5 ) < 0 );

        List<Name> list = new ArrayList<Name>();

        Comparator<Name> comparator = new Comparator<Name>()
        {
            public int compare( Name obj1, Name obj2 )
            {
                Name n1 = obj1;
                Name n2 = obj2;
                return n1.compareTo( n2 );
            }


            public boolean equals( Object obj )
            {
                return super.equals( obj );
            }


            /**
             * Compute the instance's hash code
             * @return the instance's hash code 
             */
            public int hashCode()
            {
                return super.hashCode();
            }
        };

        list.add( name0 );
        list.add( name1 );
        list.add( name2 );
        list.add( name3 );
        list.add( name4 );
        list.add( name5 );
        Collections.sort( list, comparator );

        assertEquals( name0, list.get( 0 ) );
        assertEquals( name1, list.get( 1 ) );
        assertEquals( name2, list.get( 2 ) );
        assertEquals( name5, list.get( 3 ) );
        assertEquals( name3, list.get( 4 ) );
        assertEquals( name4, list.get( 5 ) );
    }


    /**
     * Class to test for size
     *
     * @throws Exception
     *             if anything goes wrong.
     */
    @Test
    public void testSize() throws Exception
    {
        Name name0 = new LdapDN( "" );
        Name name1 = new LdapDN( "ou=East" );
        Name name2 = new LdapDN( "ou=Marketing,ou=East" );
        Name name3 = new LdapDN( "cn=John,ou=Marketing,ou=East" );
        Name name4 = new LdapDN( "cn=HomeDir,cn=John,ou=Marketing,ou=East" );
        Name name5 = new LdapDN( "cn=Website,cn=HomeDir,cn=John,ou=Marketing,ou=West" );
        Name name6 = new LdapDN( "cn=Airline,cn=Website,cn=HomeDir,cn=John,ou=Marketing,ou=West" );

        assertEquals( 0, name0.size() );
        assertEquals( 1, name1.size() );
        assertEquals( 2, name2.size() );
        assertEquals( 3, name3.size() );
        assertEquals( 4, name4.size() );
        assertEquals( 5, name5.size() );
        assertEquals( 6, name6.size() );
    }


    /**
     * Class to test for isEmpty
     *
     * @throws Exception
     *             if anything goes wrong.
     */
    @Test
    public void testIsEmpty() throws Exception
    {
        Name name0 = new LdapDN( "" );
        Name name1 = new LdapDN( "ou=East" );
        Name name2 = new LdapDN( "ou=Marketing,ou=East" );
        Name name3 = new LdapDN( "cn=John,ou=Marketing,ou=East" );
        Name name4 = new LdapDN( "cn=HomeDir,cn=John,ou=Marketing,ou=East" );
        Name name5 = new LdapDN( "cn=Website,cn=HomeDir,cn=John,ou=Marketing,ou=West" );
        Name name6 = new LdapDN( "cn=Airline,cn=Website,cn=HomeDir,cn=John,ou=Marketing,ou=West" );

        assertEquals( true, name0.isEmpty() );
        assertEquals( false, name1.isEmpty() );
        assertEquals( false, name2.isEmpty() );
        assertEquals( false, name3.isEmpty() );
        assertEquals( false, name4.isEmpty() );
        assertEquals( false, name5.isEmpty() );
        assertEquals( false, name6.isEmpty() );
    }


    /**
     * Class to test for getAll
     *
     * @throws Exception
     *             if anything goes wrong.
     */
    @Test
    public void testGetAll() throws Exception
    {
        Name name0 = new LdapDN( "" );
        Name name1 = new LdapDN( "ou=East" );
        Name name2 = new LdapDN( "ou=Marketing,ou=East" );
        Name name3 = new LdapDN( "cn=John,ou=Marketing,ou=East" );
        Name name4 = new LdapDN( "cn=HomeDir,cn=John,ou=Marketing,ou=East" );
        Name name5 = new LdapDN( "cn=Website,cn=HomeDir,cn=John,ou=Marketing,ou=West" );
        Name name6 = new LdapDN( "cn=Airline,cn=Website,cn=HomeDir,cn=John,ou=Marketing,ou=West" );

        Enumeration<String> enum0 = name0.getAll();
        assertEquals( false, enum0.hasMoreElements() );

        Enumeration<String> enum1 = name1.getAll();
        assertEquals( true, enum1.hasMoreElements() );

        for ( int i = 0; enum1.hasMoreElements(); i++ )
        {
            String element = ( String ) enum1.nextElement();

            if ( i == 0 )
            {
                assertEquals( "ou=East", element );
            }
        }

        Enumeration<String> enum2 = name2.getAll();
        assertEquals( true, enum2.hasMoreElements() );

        for ( int i = 0; enum2.hasMoreElements(); i++ )
        {
            String element = ( String ) enum2.nextElement();

            if ( i == 0 )
            {
                assertEquals( "ou=East", element );
            }

            if ( i == 1 )
            {
                assertEquals( "ou=Marketing", element );
            }
        }

        Enumeration<String> enum3 = name3.getAll();
        assertEquals( true, enum3.hasMoreElements() );

        for ( int i = 0; enum3.hasMoreElements(); i++ )
        {
            String element = ( String ) enum3.nextElement();

            if ( i == 0 )
            {
                assertEquals( "ou=East", element );
            }

            if ( i == 1 )
            {
                assertEquals( "ou=Marketing", element );
            }

            if ( i == 2 )
            {
                assertEquals( "cn=John", element );
            }
        }

        Enumeration<String> enum4 = name4.getAll();
        assertEquals( true, enum4.hasMoreElements() );

        for ( int i = 0; enum4.hasMoreElements(); i++ )
        {
            String element = ( String ) enum4.nextElement();

            if ( i == 0 )
            {
                assertEquals( "ou=East", element );
            }

            if ( i == 1 )
            {
                assertEquals( "ou=Marketing", element );
            }

            if ( i == 2 )
            {
                assertEquals( "cn=John", element );
            }

            if ( i == 3 )
            {
                assertEquals( "cn=HomeDir", element );
            }
        }

        Enumeration<String> enum5 = name5.getAll();
        assertEquals( true, enum5.hasMoreElements() );

        for ( int i = 0; enum5.hasMoreElements(); i++ )
        {
            String element = ( String ) enum5.nextElement();

            if ( i == 0 )
            {
                assertEquals( "ou=West", element );
            }

            if ( i == 1 )
            {
                assertEquals( "ou=Marketing", element );
            }

            if ( i == 2 )
            {
                assertEquals( "cn=John", element );
            }

            if ( i == 3 )
            {
                assertEquals( "cn=HomeDir", element );
            }

            if ( i == 4 )
            {
                assertEquals( "cn=Website", element );
            }
        }

        Enumeration<String> enum6 = name6.getAll();
        assertEquals( true, enum6.hasMoreElements() );

        for ( int i = 0; enum6.hasMoreElements(); i++ )
        {
            String element = ( String ) enum6.nextElement();

            if ( i == 0 )
            {
                assertEquals( "ou=West", element );
            }

            if ( i == 1 )
            {
                assertEquals( "ou=Marketing", element );
            }

            if ( i == 2 )
            {
                assertEquals( "cn=John", element );
            }

            if ( i == 3 )
            {
                assertEquals( "cn=HomeDir", element );
            }

            if ( i == 4 )
            {
                assertEquals( "cn=Website", element );
            }

            if ( i == 5 )
            {
                assertEquals( "cn=Airline", element );
            }
        }
    }


    /**
     * Class to test for getAllRdn
     *
     * @throws Exception
     *             if anything goes wrong.
     */
    @Test
    public void testGetAllRdn() throws Exception
    {
        LdapDN name = new LdapDN( "cn=Airline,cn=Website,cn=HomeDir,cn=John,ou=Marketing,ou=West" );

        Enumeration<Rdn> rdns = name.getAllRdn();
        assertEquals( true, rdns.hasMoreElements() );

        for ( int i = 0; rdns.hasMoreElements(); i++ )
        {
            Rdn element = ( Rdn ) rdns.nextElement();

            if ( i == 0 )
            {
                assertEquals( "ou=West", element.toString() );
            }

            if ( i == 1 )
            {
                assertEquals( "ou=Marketing", element.toString() );
            }

            if ( i == 2 )
            {
                assertEquals( "cn=John", element.toString() );
            }

            if ( i == 3 )
            {
                assertEquals( "cn=HomeDir", element.toString() );
            }

            if ( i == 4 )
            {
                assertEquals( "cn=Website", element.toString() );
            }

            if ( i == 5 )
            {
                assertEquals( "cn=Airline", element.toString() );
            }
        }
    }


    /**
     * Test the get( int ) method
     */
    @Test
    public void testGet() throws Exception
    {
        Name name = new LdapDN( "cn=HomeDir,cn=John,ou=Marketing,ou=East" );
        assertEquals( "cn=HomeDir", name.get( 3 ) );
        assertEquals( "cn=John", name.get( 2 ) );
        assertEquals( "ou=Marketing", name.get( 1 ) );
        assertEquals( "ou=East", name.get( 0 ) );
    }


    /**
     * Test the getRdn( int ) method
     */
    @Test
    public void testGetRdn() throws Exception
    {
        LdapDN name = new LdapDN( "cn=HomeDir,cn=John,ou=Marketing,ou=East" );
        assertEquals( "cn=HomeDir", name.getRdn( 3 ).getUpName() );
        assertEquals( "cn=John", name.getRdn( 2 ).getUpName() );
        assertEquals( "ou=Marketing", name.getRdn( 1 ).getUpName() );
        assertEquals( "ou=East", name.getRdn( 0 ).getUpName() );
    }

    /**
     * Class to test for getSuffix
     *
     * @throws Exception
     *             anything goes wrong
     */
    @Test
    public void testGetXSuffix() throws Exception
    {
        Name name = new LdapDN( "cn=HomeDir,cn=John,ou=Marketing,ou=East" );
        assertEquals( "", name.getSuffix( 4 ).toString() );
        assertEquals( "cn=HomeDir", name.getSuffix( 3 ).toString() );
        assertEquals( "cn=HomeDir,cn=John", name.getSuffix( 2 ).toString() );
        assertEquals( "cn=HomeDir,cn=John,ou=Marketing", name.getSuffix( 1 ).toString() );
        assertEquals( "cn=HomeDir,cn=John,ou=Marketing,ou=East", name.getSuffix( 0 ).toString() );
    }


    /**
     * Class to test for getPrefix
     *
     * @throws Exception
     *             anything goes wrong
     */
    @Test
    public void testGetPrefix() throws Exception
    {
        Name name = new LdapDN( "cn=HomeDir,cn=John,ou=Marketing,ou=East" );

        assertEquals( "cn=HomeDir,cn=John,ou=Marketing,ou=East", name.getPrefix( 4 ).toString() );
        assertEquals( "cn=John,ou=Marketing,ou=East", name.getPrefix( 3 ).toString() );
        assertEquals( "ou=Marketing,ou=East", name.getPrefix( 2 ).toString() );
        assertEquals( "ou=East", name.getPrefix( 1 ).toString() );
        assertEquals( "", name.getPrefix( 0 ).toString() );
    }


    /**
     * Class to test for startsWith
     *
     * @throws Exception
     *             anything goes wrong
     */
    @Test
    public void testStartsWith() throws Exception
    {
        Name n0 = new LdapDN( "cn=HomeDir,cn=John,ou=Marketing,ou=East" );
        Name n1 = new LdapDN( "cn=HomeDir,cn=John,ou=Marketing,ou=East" );
        Name n2 = new LdapDN( "cn=John,ou=Marketing,ou=East" );
        Name n3 = new LdapDN( "ou=Marketing,ou=East" );
        Name n4 = new LdapDN( "ou=East" );
        Name n5 = new LdapDN( "" );

        Name n6 = new LdapDN( "cn=HomeDir" );
        Name n7 = new LdapDN( "cn=HomeDir,cn=John" );
        Name n8 = new LdapDN( "cn=HomeDir,cn=John,ou=Marketing" );

        // Check with LdapDN
        assertTrue( n0.startsWith( n1 ) );
        assertTrue( n0.startsWith( n2 ) );
        assertTrue( n0.startsWith( n3 ) );
        assertTrue( n0.startsWith( n4 ) );
        assertTrue( n0.startsWith( n5 ) );

        assertTrue( !n0.startsWith( n6 ) );
        assertTrue( !n0.startsWith( n7 ) );
        assertTrue( !n0.startsWith( n8 ) );
        
        Name nn0 = new LdapDN( "cn=zero" );
        Name nn10 = new LdapDN( "cn=one,cn=zero" );
        Name nn210 = new LdapDN( "cn=two,cn=one,cn=zero" );
        Name nn3210 = new LdapDN( "cn=three,cn=two,cn=one,cn=zero" );
        
        assertTrue( nn0.startsWith( nn0 ) );
        assertTrue( nn10.startsWith( nn0 ) );
        assertTrue( nn210.startsWith( nn0 ) );
        assertTrue( nn3210.startsWith( nn0 ) );

        assertTrue( nn10.startsWith( nn10 ) );
        assertTrue( nn210.startsWith( nn10 ) );
        assertTrue( nn3210.startsWith( nn10 ) );

        assertTrue( nn210.startsWith( nn210 ) );
        assertTrue( nn3210.startsWith( nn210 ) );

        assertTrue( nn3210.startsWith( nn3210 ) );
        
        // Check with LdapName
        Name name0 = new LdapName( "cn=zero" );
        Name name10 = new LdapName( "cn=one,cn=zero" );
        Name name210 = new LdapName( "cn=two,cn=one,cn=zero" );
        Name name3210 = new LdapName( "cn=three,cn=two,cn=one,cn=zero" );
        
        // Check with Name
        assertTrue( nn0.startsWith( name0 ) );
        assertTrue( nn10.startsWith( name0 ) );
        assertTrue( nn210.startsWith( name0 ) );
        assertTrue( nn3210.startsWith( name0 ) );

        assertTrue( nn10.startsWith( name10 ) );
        assertTrue( nn210.startsWith( name10 ) );
        assertTrue( nn3210.startsWith( name10 ) );

        assertTrue( nn210.startsWith( name210 ) );
        assertTrue( nn3210.startsWith( name210 ) );

        assertTrue( nn3210.startsWith( name3210 ) );
        

        assertTrue( "Starting DN fails with ADS LdapDN", 
            new LdapDN( "ou=foo,dc=apache,dc=org" ).startsWith( new LdapDN( "dc=apache,dc=org" ) ) );
        
        assertTrue( "Starting DN fails with Java LdapName", 
            new LdapDN( "ou=foo,dc=apache,dc=org" ).startsWith( new LdapName( "dc=apache,dc=org" ) ) );

        assertTrue( "Starting DN fails with Java LdapName", 
            new LdapDN( "dc=apache,dc=org" ).startsWith( new LdapName( "dc=apache,dc=org" ) ) );
    }


    /**
     * Class to test for endsWith
     *
     * @throws Exception
     *             anything goes wrong
     */
    @Test
    public void testEndsWith() throws Exception
    {
        Name name0 = new LdapDN( "cn=HomeDir,cn=John,ou=Marketing,ou=East" );
        Name name1 = new LdapDN( "cn=HomeDir,cn=John,ou=Marketing,ou=East" );
        Name name2 = new LdapDN( "cn=John,ou=Marketing,ou=East" );
        Name name3 = new LdapDN( "ou=Marketing,ou=East" );
        Name name4 = new LdapDN( "ou=East" );
        Name name5 = new LdapDN( "" );

        Name name6 = new LdapDN( "cn=HomeDir" );
        Name name7 = new LdapDN( "cn=HomeDir,cn=John" );
        Name name8 = new LdapDN( "cn=HomeDir,cn=John,ou=Marketing" );

        assertTrue( name0.endsWith( name1 ) );
        assertTrue( !name0.endsWith( name2 ) );
        assertTrue( !name0.endsWith( name3 ) );
        assertTrue( !name0.endsWith( name4 ) );
        assertTrue( name0.endsWith( name5 ) );

        assertTrue( name0.endsWith( name6 ) );
        assertTrue( name0.endsWith( name7 ) );
        assertTrue( name0.endsWith( name8 ) );
    }


    /**
     * Class to test for Name addAll(Name)
     *
     * @throws Exception
     *             when anything goes wrong
     */
    @Test
    public void testAddAllName0() throws Exception
    {
        Name name = new LdapDN();
        Name name0 = new LdapDN( "cn=HomeDir,cn=John,ou=Marketing,ou=East" );
        assertTrue( name0.equals( name.addAll( name0 ) ) );
    }


    /**
     * Class to test for Name addAll(Name)
     *
     * @throws Exception
     *             when anything goes wrong
     */
    @Test
    public void testAddAllNameExisting0() throws Exception
    {
        Name name1 = new LdapDN( "ou=Marketing,ou=East" );
        Name name2 = new LdapDN( "cn=HomeDir,cn=John" );
        Name nameAdded = new LdapDN( "cn=HomeDir,cn=John, ou=Marketing,ou=East" );
        assertTrue( nameAdded.equals( name1.addAll( name2 ) ) );
    }


    /**
     * Class to test for Name addAll(Name)
     *
     * @throws Exception
     *             when anything goes wrong
     */
    @Test
    public void testAddAllName1() throws Exception
    {
        Name name = new LdapDN();
        Name name0 = new LdapDN( "ou=Marketing,ou=East" );
        Name name1 = new LdapDN( "cn=HomeDir,cn=John" );
        Name name2 = new LdapDN( "cn=HomeDir,cn=John,ou=Marketing,ou=East" );

        assertTrue( name0.equals( name.addAll( name0 ) ) );
        assertTrue( name2.equals( name.addAll( name1 ) ) );
    }


    /**
     * Class to test for Name addAll(int, Name)
     *
     * @throws Exception
     *             when something goes wrong
     */
    @Test
    public void testAddAllintName0() throws Exception
    {
        Name name = new LdapDN();
        Name name0 = new LdapDN( "ou=Marketing,ou=East" );
        Name name1 = new LdapDN( "cn=HomeDir,cn=John" );
        Name name2 = new LdapDN( "cn=HomeDir,cn=John,ou=Marketing,ou=East" );

        assertTrue( name0.equals( name.addAll( name0 ) ) );
        assertTrue( name2.equals( name.addAll( 2, name1 ) ) );
    }


    /**
     * Class to test for Name addAll(int, Name)
     *
     * @throws Exception
     *             when something goes wrong
     */
    @Test
    public void testAddAllintName1() throws Exception
    {
        Name name = new LdapDN();
        Name name0 = new LdapDN( "cn=HomeDir,ou=Marketing,ou=East" );
        Name name1 = new LdapDN( "cn=John" );
        Name name2 = new LdapDN( "cn=HomeDir,cn=John,ou=Marketing,ou=East" );

        assertTrue( name0.equals( name.addAll( name0 ) ) );
        assertTrue( name2.equals( name.addAll( 2, name1 ) ) );

        Name name3 = new LdapDN( "cn=Airport" );
        Name name4 = new LdapDN( "cn=Airport,cn=HomeDir,cn=John,ou=Marketing,ou=East" );

        assertTrue( name4.equals( name.addAll( 4, name3 ) ) );

        Name name5 = new LdapDN( "cn=ABC123" );
        Name name6 = new LdapDN( "cn=Airport,cn=HomeDir,cn=ABC123,cn=John,ou=Marketing,ou=East" );

        assertTrue( name6.equals( name.addAll( 3, name5 ) ) );
    }


    /**
     * Class to test for Name add(String)
     *
     * @throws Exception
     *             when something goes wrong
     */
    @Test
    public void testAddString() throws Exception
    {
        Name name = new LdapDN();
        assertEquals( name, new LdapDN( "" ) );

        Name name4 = new LdapDN( "ou=East" );
        name.add( "ou=East" );
        assertEquals( name4, name );

        Name name3 = new LdapDN( "ou=Marketing,ou=East" );
        name.add( "ou=Marketing" );
        assertEquals( name3, name );

        Name name2 = new LdapDN( "cn=John,ou=Marketing,ou=East" );
        name.add( "cn=John" );
        assertEquals( name2, name );

        Name name0 = new LdapDN( "cn=HomeDir,cn=John,ou=Marketing,ou=East" );
        name.add( "cn=HomeDir" );
        assertEquals( name0, name );
    }


    /**
     * Class to test for Name add(int, String)
     *
     * @throws Exception
     *             if anything goes wrong
     */
    @Test
    public void testAddintString() throws Exception
    {
        Name name = new LdapDN();
        assertEquals( name, new LdapDN( "" ) );

        Name name4 = new LdapDN( "ou=East" );
        name.add( "ou=East" );
        assertEquals( name4, name );

        Name name3 = new LdapDN( "ou=Marketing,ou=East" );
        name.add( 1, "ou=Marketing" );
        assertEquals( name3, name );

        Name name2 = new LdapDN( "cn=John,ou=Marketing,ou=East" );
        name.add( 2, "cn=John" );
        assertEquals( name2, name );

        Name name0 = new LdapDN( "cn=HomeDir,cn=John,ou=Marketing,ou=East" );
        name.add( 3, "cn=HomeDir" );
        assertEquals( name0, name );

        Name name5 = new LdapDN( "cn=HomeDir,cn=John,ou=Marketing,ou=East,o=LL " + "Bean Inc." );
        name.add( 0, "o=LL Bean Inc." );
        assertEquals( name5, name );

        Name name6 = new LdapDN( "cn=HomeDir,cn=John,ou=Marketing,ou=East,c=US,o=LL " + "Bean Inc." );
        name.add( 1, "c=US" );
        assertEquals( name6, name );

        Name name7 = new LdapDN( "cn=HomeDir,cn=John,ou=Advertising,ou=Marketing," + "ou=East,c=US,o=LL " + "Bean Inc." );
        name.add( 4, "ou=Advertising" );
        assertEquals( name7, name );
    }


    /**
     * Class to test for remove
     *
     * @throws Exception
     *             if anything goes wrong
     */
    @Test
    public void testRemove() throws Exception
    {
        Name name = new LdapDN();
        assertEquals( new LdapDN( "" ), name );

        Name name3 = new LdapDN( "ou=Marketing" );
        name.add( "ou=East" );
        name.add( 1, "ou=Marketing" );
        name.remove( 0 );
        assertEquals( name3, name );

        Name name2 = new LdapDN( "cn=HomeDir,ou=Marketing,ou=East" );
        name.add( 0, "ou=East" );
        name.add( 2, "cn=John" );
        name.add( "cn=HomeDir" );
        name.remove( 2 );
        assertEquals( name2, name );

        name.remove( 1 );
        Name name1 = new LdapDN( "cn=HomeDir,ou=East" );
        assertEquals( name1, name );

        name.remove( 1 );
        Name name0 = new LdapDN( "ou=East" );
        assertEquals( name0, name );

        name.remove( 0 );
        assertEquals( new LdapDN( "" ), name );
    }


    /**
     * Class to test for String toString()
     *
     * @throws Exception
     *             if anything goes wrong
     */
    @Test
    public void testToString() throws Exception
    {
        Name name = new LdapDN();
        assertEquals( "", name.toString() );

        name.add( "ou=East" );
        assertEquals( "ou=East", name.toString() );

        name.add( 1, "ou=Marketing" );
        assertEquals( "ou=Marketing,ou=East", name.toString() );

        name.add( "cn=John" );
        assertEquals( "cn=John,ou=Marketing,ou=East", name.toString() );

        name.add( "cn=HomeDir" );
        assertEquals( "cn=HomeDir,cn=John,ou=Marketing,ou=East", name.toString() );
    }


    /**
     * Class to test for boolean equals(Object)
     *
     * @throws Exception
     *             if anything goes wrong
     */
    @Test
    public void testEqualsObject() throws Exception
    {
        assertTrue( new LdapDN( "ou=People" ).equals( new LdapDN( "ou=People" ) ) );

        assertTrue( !new LdapDN( "ou=People,dc=example,dc=com" ).equals( new LdapDN( "ou=People" ) ) );
        assertTrue( !new LdapDN( "ou=people" ).equals( new LdapDN( "ou=People" ) ) );
        assertTrue( !new LdapDN( "ou=Groups" ).equals( new LdapDN( "ou=People" ) ) );
    }


    @Test
    public void testNameFrenchChars() throws Exception
    {
        String cn = new String( new byte[]
            { 'c', 'n', '=', 0x4A, ( byte ) 0xC3, ( byte ) 0xA9, 0x72, ( byte ) 0xC3, ( byte ) 0xB4, 0x6D, 0x65 },
            "UTF-8" );

        Name name = new LdapDN( cn );

        assertEquals( "cn=J\u00e9r\u00f4me", name.toString() );
    }


    @Test
    public void testNameGermanChars() throws Exception
    {
        String cn = new String( new byte[]
            { 'c', 'n', '=', ( byte ) 0xC3, ( byte ) 0x84, ( byte ) 0xC3, ( byte ) 0x96, ( byte ) 0xC3, ( byte ) 0x9C,
                ( byte ) 0xC3, ( byte ) 0x9F, ( byte ) 0xC3, ( byte ) 0xA4, ( byte ) 0xC3, ( byte ) 0xB6,
                ( byte ) 0xC3, ( byte ) 0xBC }, "UTF-8" );

        Name name = new LdapDN( cn );

        assertEquals( "cn=\u00C4\u00D6\u00DC\u00DF\u00E4\u00F6\u00FC", name.toString() );
    }


    @Test
    public void testNameTurkishChars() throws Exception
    {
        String cn = new String( new byte[]
            { 'c', 'n', '=', ( byte ) 0xC4, ( byte ) 0xB0, ( byte ) 0xC4, ( byte ) 0xB1, ( byte ) 0xC5, ( byte ) 0x9E,
                ( byte ) 0xC5, ( byte ) 0x9F, ( byte ) 0xC3, ( byte ) 0x96, ( byte ) 0xC3, ( byte ) 0xB6,
                ( byte ) 0xC3, ( byte ) 0x9C, ( byte ) 0xC3, ( byte ) 0xBC, ( byte ) 0xC4, ( byte ) 0x9E,
                ( byte ) 0xC4, ( byte ) 0x9F }, "UTF-8" );

        Name name = new LdapDN( cn );

        assertEquals( "cn=\u0130\u0131\u015E\u015F\u00D6\u00F6\u00DC\u00FC\u011E\u011F", name.toString() );
    }


    /**
     * Class to test for toOid( Name, Map)
     */
    @Test
    public void testLdapNameToName() throws Exception
    {
        List<String> list = new ArrayList<String>();
        list.add( "ou= Some   People   " );
        list.add( "dc = eXample" );
        list.add( "dc= cOm" );
        LdapDN name = new LdapDN( list.iterator() );

        assertTrue( name.getUpName().equals( "ou= Some   People   ,dc = eXample,dc= cOm" ) );

        Name result = LdapDN.normalize( name, oids );

        assertTrue( result.toString().equals( "ou=some people,dc=example,dc=com" ) );
    }


    @Test
    public void testRdnGetTypeUpName() throws Exception
    {
        List<String> list = new ArrayList<String>();
        list.add( "ou= Some   People   " );
        list.add( "dc = eXample" );
        list.add( "dc= cOm" );
        LdapDN name = new LdapDN( list.iterator() );

        assertTrue( name.getUpName().equals( "ou= Some   People   ,dc = eXample,dc= cOm" ) );

        Rdn rdn = name.getRdn();

        assertEquals( "ou= Some   People   ", rdn.getUpName() );
        assertEquals( "ou", rdn.getNormType() );
        assertEquals( "ou", rdn.getUpType() );

        LdapDN result = LdapDN.normalize( name, oidOids );

        assertTrue( result.getNormName().equals(
            "2.5.4.11=some people,0.9.2342.19200300.100.1.25=example,0.9.2342.19200300.100.1.25=com" ) );
        assertTrue( name.getUpName().equals( "ou= Some   People   ,dc = eXample,dc= cOm" ) );

        Rdn rdn2 = result.getRdn();

        assertEquals( "ou= Some   People   ", rdn2.getUpName() );
        assertEquals( "2.5.4.11", rdn2.getNormType() );
        assertEquals( "ou", rdn2.getUpType() );
    }


    /**
     * Class to test for toOid( Name, Map) with a NULL dn
     */
    @Test
    public void testLdapNameToNameEmpty() throws Exception
    {
        LdapDN name = new LdapDN();

        Name result = LdapDN.normalize( name, oids );
        assertTrue( result.toString().equals( "" ) );
    }


    /**
     * Class to test for toOid( Name, Map) with a multiple NameComponent
     */
    @Test
    public void testLdapNameToNameMultiNC() throws Exception
    {
        LdapDN name = new LdapDN(
            "2.5.4.11= Some   People   + 0.9.2342.19200300.100.1.25=  And   Some anImAls,0.9.2342.19200300.100.1.25 = eXample,dc= cOm" );

        Name result = LdapDN.normalize( name, oidOids );

        assertEquals(
            result.toString(),
            "0.9.2342.19200300.100.1.25=and some animals+2.5.4.11=some people,0.9.2342.19200300.100.1.25=example,0.9.2342.19200300.100.1.25=com" );
        assertTrue( ( ( LdapDN ) result )
            .getUpName()
            .equals(
                "2.5.4.11= Some   People   + 0.9.2342.19200300.100.1.25=  And   Some anImAls,0.9.2342.19200300.100.1.25 = eXample,dc= cOm" ) );
    }


    /**
     * Class to test for toOid( Name, Map) with a multiple NameComponent
     */
    @Test
    public void testLdapNameToNameAliasMultiNC() throws Exception
    {
        LdapDN name = new LdapDN(
            "2.5.4.11= Some   People   + domainComponent=  And   Some anImAls,DomainComponent = eXample,0.9.2342.19200300.100.1.25= cOm" );

        LdapDN result = LdapDN.normalize( name, oidOids );

        assertTrue( result
            .toString()
            .equals(
                "0.9.2342.19200300.100.1.25=and some animals+2.5.4.11=some people,0.9.2342.19200300.100.1.25=example,0.9.2342.19200300.100.1.25=com" ) );
        assertTrue( result
            .getUpName()
            .equals(
                "2.5.4.11= Some   People   + domainComponent=  And   Some anImAls,DomainComponent = eXample,0.9.2342.19200300.100.1.25= cOm" ) );
    }


    /**
     * Class to test for hashCode().
     */
    @Test
    public void testLdapNameHashCode() throws Exception
    {
        Name name1 = LdapDN
            .normalize(
                "2.5.4.11= Some   People   + domainComponent=  And   Some anImAls,DomainComponent = eXample,0.9.2342.19200300.100.1.25= cOm",
                oids );

        Name name2 = LdapDN
            .normalize(
                "2.5.4.11=some people+domainComponent=and some animals,DomainComponent=example,0.9.2342.19200300.100.1.25=com",
                oids );

        assertEquals( name1.hashCode(), name2.hashCode() );
    }


    /**
     * Test for DIRSERVER-191
     */
    @Test
    public void testName() throws NamingException
    {
        Name jName = new javax.naming.ldap.LdapName( "cn=four,cn=three,cn=two,cn=one" );
        Name aName = new LdapDN( "cn=four,cn=three,cn=two,cn=one" );
        assertEquals( jName.toString(), "cn=four,cn=three,cn=two,cn=one" );
        assertEquals( aName.toString(), "cn=four,cn=three,cn=two,cn=one" );
        assertEquals( jName.toString(), aName.toString() );
    }


    /**
     * Test for DIRSERVER-191
     */
    @Test
    public void testGetPrefixName() throws NamingException
    {
        Name jName = new LdapName( "cn=four,cn=three,cn=two,cn=one" );
        Name aName = new LdapDN( "cn=four,cn=three,cn=two,cn=one" );

        assertEquals( jName.getPrefix( 0 ).toString(), aName.getPrefix( 0 ).toString() );
        assertEquals( jName.getPrefix( 1 ).toString(), aName.getPrefix( 1 ).toString() );
        assertEquals( jName.getPrefix( 2 ).toString(), aName.getPrefix( 2 ).toString() );
        assertEquals( jName.getPrefix( 3 ).toString(), aName.getPrefix( 3 ).toString() );
        assertEquals( jName.getPrefix( 4 ).toString(), aName.getPrefix( 4 ).toString() );
    }


    /**
     * Test for DIRSERVER-191
     */
    @Test
    public void testGetSuffix() throws NamingException
    {
        Name jName = new LdapName( "cn=four,cn=three,cn=two,cn=one" );
        Name aName = new LdapDN( "cn=four,cn=three,cn=two,cn=one" );

        assertEquals( jName.getSuffix( 0 ).toString(), aName.getSuffix( 0 ).toString() );
        assertEquals( jName.getSuffix( 1 ).toString(), aName.getSuffix( 1 ).toString() );
        assertEquals( jName.getSuffix( 2 ).toString(), aName.getSuffix( 2 ).toString() );
        assertEquals( jName.getSuffix( 3 ).toString(), aName.getSuffix( 3 ).toString() );
        assertEquals( jName.getSuffix( 4 ).toString(), aName.getSuffix( 4 ).toString() );
    }


    /**
     * Test for DIRSERVER-191
     */
    @Test
    public void testAddStringName() throws NamingException
    {
        Name jName = new LdapName( "cn=four,cn=three,cn=two,cn=one" );
        Name aName = new LdapDN( "cn=four,cn=three,cn=two,cn=one" );

        assertSame( jName, jName.add( "cn=five" ) );
        assertSame( aName, aName.add( "cn=five" ) );
        assertEquals( jName.toString(), aName.toString() );
    }


    /**
     * Test for DIRSERVER-191
     */
    @Test
    public void testAddIntString() throws NamingException
    {
        Name jName = new LdapName( "cn=four,cn=three,cn=two,cn=one" );
        Name aName = new LdapDN( "cn=four,cn=three,cn=two,cn=one" );

        assertSame( jName, jName.add( 0, "cn=zero" ) );
        assertSame( aName, aName.add( 0, "cn=zero" ) );
        assertEquals( jName.toString(), aName.toString() );

        assertSame( jName, jName.add( 2, "cn=one.5" ) );
        assertSame( aName, aName.add( 2, "cn=one.5" ) );
        assertEquals( jName.toString(), aName.toString() );

        assertSame( jName, jName.add( jName.size(), "cn=five" ) );
        assertSame( aName, aName.add( aName.size(), "cn=five" ) );
        assertEquals( jName.toString(), aName.toString() );
    }


    /**
     * Test for DIRSERVER-191
     */
    @Test
    public void testAddAllName() throws NamingException
    {
        Name jName = new LdapName( "cn=four,cn=three,cn=two,cn=one" );
        Name aName = new LdapDN( "cn=four,cn=three,cn=two,cn=one" );

        assertSame( jName, jName.addAll( new LdapName( "cn=seven,cn=six" ) ) );
        assertSame( aName, aName.addAll( new LdapDN( "cn=seven,cn=six" ) ) );
        assertEquals( jName.toString(), aName.toString() );
    }


    /**
     * Test for DIRSERVER-191
     */
    @Test
    public void testAddAllIntName() throws NamingException
    {
        Name jName = new LdapName( "cn=four,cn=three,cn=two,cn=one" );
        Name aName = new LdapDN( "cn=four,cn=three,cn=two,cn=one" );

        assertSame( jName, jName.addAll( 0, new LdapName( "cn=zero,cn=zero.5" ) ) );
        assertSame( aName, aName.addAll( 0, new LdapDN( "cn=zero,cn=zero.5" ) ) );
        assertEquals( jName.toString(), aName.toString() );

        assertSame( jName, jName.addAll( 2, new LdapName( "cn=zero,cn=zero.5" ) ) );
        assertSame( aName, aName.addAll( 2, new LdapDN( "cn=zero,cn=zero.5" ) ) );
        assertEquals( jName.toString(), aName.toString() );

        assertSame( jName, jName.addAll( jName.size(), new LdapName( "cn=zero,cn=zero.5" ) ) );
        assertSame( aName, aName.addAll( aName.size(), new LdapDN( "cn=zero,cn=zero.5" ) ) );
        assertEquals( jName.toString(), aName.toString() );
    }


    /**
     * Test for DIRSERVER-191
     */
    @Test
    public void testStartsWithName() throws NamingException
    {
        Name jName = new LdapName( "cn=four,cn=three,cn=two,cn=one" );
        Name aName = new LdapDN( "cn=four,cn=three,cn=two,cn=one" );

        assertEquals( jName.startsWith( new LdapName( "cn=seven,cn=six,cn=five" ) ), aName.startsWith( new LdapDN(
            "cn=seven,cn=six,cn=five" ) ) );
        assertEquals( jName.startsWith( new LdapName( "cn=three,cn=two,cn=one" ) ), aName.startsWith( new LdapDN(
            "cn=three,cn=two,cn=one" ) ) );
    }


    /**
     * Test for DIRSERVER-191
     */
    @Test
    public void testEndsWithName() throws NamingException
    {
        Name name0 = new LdapName( "cn=zero" );
        Name name10 = new LdapName( "cn=one,cn=zero" );
        Name name210 = new LdapName( "cn=two,cn=one,cn=zero" );
        Name name3210 = new LdapName( "cn=three,cn=two,cn=one,cn=zero" );
        Name name321 =  new LdapName( "cn=three,cn=two,cn=one" );
        Name name32 =  new LdapName( "cn=three,cn=two" );
        Name name3 =  new LdapName( "cn=three" );
        Name name21 =  new LdapName( "cn=two,cn=one" );
        Name name2 =  new LdapName( "cn=two" );
        Name name1 =  new LdapName( "cn=one" );
        
        // Check with Name
        assertTrue( name0.startsWith( name0 ) );
        assertTrue( name10.startsWith( name0 ) );
        assertTrue( name210.startsWith( name0 ) );
        assertTrue( name3210.startsWith( name0 ) );

        assertTrue( name10.startsWith( name10 ) );
        assertTrue( name210.startsWith( name10 ) );
        assertTrue( name3210.startsWith( name10 ) );

        assertTrue( name210.startsWith( name210 ) );
        assertTrue( name3210.startsWith( name210 ) );

        assertTrue( name3210.startsWith( name3210 ) );
        
        assertTrue( name3210.endsWith( name3 ) );
        assertTrue( name3210.endsWith( name32 ) );
        assertTrue( name3210.endsWith( name321 ) );
        assertTrue( name3210.endsWith( name3210 ) );

        assertTrue( name210.endsWith( name2 ) );
        assertTrue( name210.endsWith( name21 ) );
        assertTrue( name210.endsWith( name210 ) );

        assertTrue( name10.endsWith( name1 ) );
        assertTrue( name10.endsWith( name10 ) );

        assertTrue( name0.endsWith( name0 ) );
        
        // Check with DN
        Name n0 = new LdapDN( "cn=zero" );
        Name n10 = new LdapDN( "cn=one,cn=zero" );
        Name n210 = new LdapDN( "cn=two,cn=one,cn=zero" );
        Name n3210 = new LdapDN( "cn=three,cn=two,cn=one,cn=zero" );
        Name n321 =  new LdapDN( "cn=three,cn=two,cn=one" );
        Name n32 =  new LdapDN( "cn=three,cn=two" );
        Name n3 =  new LdapDN( "cn=three" );
        Name n21 =  new LdapDN( "cn=two,cn=one" );
        Name n2 =  new LdapDN( "cn=two" );
        Name n1 =  new LdapDN( "cn=one" );
        
        assertTrue( n3210.endsWith( n3 ) );
        assertTrue( n3210.endsWith( n32 ) );
        assertTrue( n3210.endsWith( n321 ) );
        assertTrue( n3210.endsWith( n3210 ) );

        assertTrue( n210.endsWith( n2 ) );
        assertTrue( n210.endsWith( n21 ) );
        assertTrue( n210.endsWith( n210 ) );

        assertTrue( n10.endsWith( n1 ) );
        assertTrue( n10.endsWith( n10 ) );

        assertTrue( n0.endsWith( n0 ) );

        // Check with DN/Name now
        assertTrue( n3210.endsWith( name3 ) );
        assertTrue( n3210.endsWith( name32 ) );
        assertTrue( n3210.endsWith( name321 ) );
        assertTrue( n3210.endsWith( name3210 ) );

        assertTrue( n210.endsWith( name2 ) );
        assertTrue( n210.endsWith( name21 ) );
        assertTrue( n210.endsWith( name210 ) );

        assertTrue( n10.endsWith( name1 ) );
        assertTrue( n10.endsWith( name10 ) );

        assertTrue( n0.endsWith( name0 ) );
        
        
        Name jName = new LdapName( "cn=four,cn=three,cn=two,cn=one" );
        Name aName = new LdapDN( "cn=four,cn=three,cn=two,cn=one" );

        assertEquals( jName.endsWith( new LdapName( "cn=seven,cn=six,cn=five" ) ), aName.endsWith( new LdapDN(
            "cn=seven,cn=six,cn=five" ) ) );
        assertEquals( jName.endsWith( new LdapName( "cn=three,cn=two,cn=one" ) ), aName.endsWith( new LdapDN(
            "cn=three,cn=two,cn=one" ) ) );
        assertEquals( jName.endsWith( new LdapName( "cn=two,cn=one" ) ), aName.endsWith( new LdapDN(
        "cn=three,cn=two,cn=one" ) ) );
        
        assertTrue( aName.endsWith( new LdapName( "cn=four,cn=three" ) ) );
    }


    /**
     * Test for DIRSERVER-191
     */
    @Test
    public void testRemoveName() throws NamingException
    {
        Name jName = new LdapName( "cn=four,cn=three,cn=two,cn=one" );
        Name aName = new LdapDN( "cn=four,cn=three,cn=two,cn=one" );

        assertEquals( jName.remove( 0 ).toString(), aName.remove( 0 ).toString() );
        assertEquals( jName.toString(), aName.toString() );

        assertEquals( jName.remove( jName.size() - 1 ).toString(), aName.remove( aName.size() - 1 ).toString() );
        assertEquals( jName.toString(), aName.toString() );
    }


    /**
     * Test for DIRSERVER-191
     */
    @Test
    public void testGetAllName() throws NamingException
    {
        Name jName = new LdapName( "cn=four,cn=three,cn=two,cn=one" );
        Name aName = new LdapDN( "cn=four,cn=three,cn=two,cn=one" );

        Enumeration<String> j = jName.getAll();
        Enumeration<String> a = aName.getAll();
        while ( j.hasMoreElements() )
        {
            assertTrue( j.hasMoreElements() );
            assertEquals( j.nextElement(), a.nextElement() );
        }
    }


    /**
     * Test for DIRSERVER-642
     * @throws NamingException
     */
    @Test
    public void testDoubleQuoteInNameDIRSERVER_642() throws NamingException
    {
        Name name1 = new LdapDN( "cn=\"Kylie Minogue\",dc=example,dc=com" );
        Name name2 = new LdapName( "cn=\"Kylie Minogue\",dc=example,dc=com" );

        Enumeration<String> j = name1.getAll();
        Enumeration<String> a = name2.getAll();

        while ( j.hasMoreElements() )
        {
            assertTrue( j.hasMoreElements() );
            assertEquals( j.nextElement(), a.nextElement() );
        }
    }


    /**
     * Test for DIRSERVER-642
     * @throws NamingException
     */
    @Test
    public void testDoubleQuoteInNameDIRSERVER_642_1() throws NamingException
    {
        LdapDN dn = new LdapDN( "cn=\" Kylie Minogue \",dc=example,dc=com" );

        assertEquals( "cn=\" Kylie Minogue \",dc=example,dc=com", dn.getUpName() );
        assertEquals( "cn=\\ Kylie Minogue\\ ,dc=example,dc=com", dn.toString() );
    }


    /**
     * Test for DIRSTUDIO-250
     * @throws NamingException
     */
    @Test
    public void testDoubleQuoteWithSpecialCharsInNameDIRSERVER_250() throws NamingException
    {
        LdapDN dn = new LdapDN( "a=\"b,c\"" );

        assertEquals( "a=\"b,c\"", dn.getUpName() );
        assertEquals( "a=b\\,c", dn.toString() );
    }


    /**
     * Test for DIRSERVER-184
     * @throws NamingException
     */
    @Test
    public void testLeadingAndTrailingSpacesDIRSERVER_184() throws NamingException
    {
        LdapDN name = new LdapDN( "dn= \\ four spaces leading and 3 trailing \\  " );

        assertEquals( "dn=\\ four spaces leading and 3 trailing \\ ", name.toString() );
        assertEquals( "dn= \\ four spaces leading and 3 trailing \\  ", name.getUpName() );
    }


    /**
     * Test for DIRSERVER-184
     * @throws NamingException
     */
    @Test
    public void testDIRSERVER_184_1()
    {
        try
        {
            new LdapDN( "dn=middle\\ spaces" );
        }
        catch ( InvalidNameException ine )
        {
            assertTrue( true );
        }
    }


    /**
     * Test for DIRSERVER-184
     * @throws NamingException
     */
    @Test
    public void testDIRSERVER_184_2()
    {
        try
        {
            new LdapDN( "dn=# a leading pound" );
        }
        catch ( InvalidNameException ine )
        {
            assertTrue( true );
        }
    }


    /**
     * Test for DIRSERVER-184
     * @throws NamingException
     */
    @Test
    public void testDIRSERVER_184_3() throws NamingException
    {
        LdapDN name = new LdapDN( "dn=\\# a leading pound" );

        assertEquals( "dn=\\# a leading pound", name.toString() );
        assertEquals( "dn=\\# a leading pound", name.getUpName() );
    }


    /**
     * Test for DIRSERVER-184
     * @throws NamingException
     */
    @Test
    public void testDIRSERVER_184_4() throws NamingException
    {
        LdapDN name = new LdapDN( "dn=a middle \\# pound" );

        assertEquals( "dn=a middle # pound", name.toString() );
        assertEquals( "dn=a middle \\# pound", name.getUpName() );
    }


    /**
     * Test for DIRSERVER-184
     * @throws NamingException
     */
    @Test
    public void testDIRSERVER_184_5() throws NamingException
    {
        LdapDN name = new LdapDN( "dn=a trailing pound \\#" );

        assertEquals( "dn=a trailing pound #", name.toString() );
        assertEquals( "dn=a trailing pound \\#", name.getUpName() );
    }


    /**
     * Test for DIRSERVER-184
     * @throws NamingException
     */
    @Test
    public void testDIRSERVER_184_6()
    {
        try
        {
            new LdapDN( "dn=a middle # pound" );
        }
        catch ( InvalidNameException ine )
        {
            assertTrue( true );
        }
    }


    /**
     * Test for DIRSERVER-184
     * @throws NamingException
     */
    @Test
    public void testDIRSERVER_184_7()
    {
        try
        {
            new LdapDN( "dn=a trailing pound #" );
        }
        catch ( InvalidNameException ine )
        {
            assertTrue( true );
        }
    }


    @Test
    public void testDIRSERVER_631_1() throws NamingException
    {
        LdapDN name = new LdapDN( "cn=Bush\\, Kate,dc=example,dc=com" );

        assertEquals( "cn=Bush\\, Kate,dc=example,dc=com", name.toString() );
        assertEquals( "cn=Bush\\, Kate,dc=example,dc=com", name.getUpName() );

    }


    /**
     * Added a test to check the parsing of a DN with more than one RDN
     * which are OIDs, and with one RDN which has more than one atav.
     * @throws NamingException
     */
    @Test
    public void testDNWithMultiOidsRDN() throws NamingException
    {
        LdapDN name = new LdapDN(
            "0.9.2342.19200300.100.1.1=00123456789+2.5.4.3=pablo picasso,2.5.4.11=search,2.5.4.10=imc,2.5.4.6=us" );
        assertEquals(
            "0.9.2342.19200300.100.1.1=00123456789+2.5.4.3=pablo picasso,2.5.4.11=search,2.5.4.10=imc,2.5.4.6=us", name
                .toString() );
        assertEquals(
            "0.9.2342.19200300.100.1.1=00123456789+2.5.4.3=pablo picasso,2.5.4.11=search,2.5.4.10=imc,2.5.4.6=us", name
                .getUpName() );
    }


    @Test
    public void testNameAddAll() throws NamingException
    {
        Properties props = new Properties();
        props.setProperty( "jndi.syntax.direction", "right_to_left" );
        props.setProperty( "jndi.syntax.separator", "," );
        props.setProperty( "jndi.syntax.ignorecase", "true" );
        props.setProperty( "jndi.syntax.trimblanks", "true" );

        Name dn = new CompoundName( "cn=blah,dc=example,dc=com", props );
        LdapDN ldapDn = new LdapDN();
        ldapDn.addAll( 0, dn );

        assertEquals( "cn=blah,dc=example,dc=com", ldapDn.toString() );

        dn = new CompoundName( "cn=blah,dc=example,dc=com", props );
        ldapDn = new LdapDN( "cn=xyz" );
        ldapDn.addAll( 0, dn );

        assertEquals( "cn=xyz,cn=blah,dc=example,dc=com", ldapDn.toString() );
    }


    @Test
    public void testDNEquals() throws NamingException
    {
        LdapDN dn1 = new LdapDN( "a=b,c=d,e=f" );
        LdapDN dn2 = new LdapDN( "a=b\\,c\\=d,e=f" );

        assertFalse( dn1.toString().equals( dn2.toString() ) );
    }


    @Test
    public void testDNAddEmptyString() throws NamingException
    {
        LdapDN dn = new LdapDN();
        assertTrue( dn.size() == 0 );
        assertTrue( dn.add( "" ).size() == 0 );
    }


    /**
     * This leads to the bug in DIRSERVER-832.
     */
    @Test
    public void testPreserveAttributeIdCase() throws NamingException
    {
        LdapDN dn = new LdapDN( "uID=kevin" );
        assertEquals( "uID", dn.getRdn().getUpType() );
    }


    /**
     * Tests the LdapDN.isValid() method.
     */
    @Test
    public void testIsValid()
    {
        assertTrue( LdapDN.isValid( "" ) );

        assertFalse( LdapDN.isValid( "a" ) );
        assertFalse( LdapDN.isValid( "a " ) );

        assertTrue( LdapDN.isValid( "a=" ) );
        assertTrue( LdapDN.isValid( "a= " ) );

        assertFalse( LdapDN.isValid( "=" ) );
        assertFalse( LdapDN.isValid( " = " ) );
        assertFalse( LdapDN.isValid( " = a" ) );
    }


    private ByteArrayOutputStream serializeDN( LdapDN dn ) throws IOException
    {
        ObjectOutputStream oOut = null;
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        try
        {
            oOut = new ObjectOutputStream( out );
            oOut.writeObject( dn );
        }
        catch ( IOException ioe )
        {
            throw ioe;
        }
        finally
        {
            try
            {
                if ( oOut != null )
                {
                    oOut.flush();
                    oOut.close();
                }
            }
            catch ( IOException ioe )
            {
                throw ioe;
            }
        }

        return out;
    }


    private LdapDN deserializeDN( ByteArrayOutputStream out ) throws IOException, ClassNotFoundException
    {
        ObjectInputStream oIn = null;
        ByteArrayInputStream in = new ByteArrayInputStream( out.toByteArray() );

        try
        {
            oIn = new ObjectInputStream( in );

            LdapDN dn = ( LdapDN ) oIn.readObject();

            return dn;
        }
        catch ( IOException ioe )
        {
            throw ioe;
        }
        finally
        {
            try
            {
                if ( oIn != null )
                {
                    oIn.close();
                }
            }
            catch ( IOException ioe )
            {
                throw ioe;
            }
        }
    }


    /**
     * Test the serialization of a DN
     *
     * @throws Exception
     */
    @Test
    public void testNameSerialization() throws Exception
    {
        LdapDN dn = new LdapDN( "ou= Some   People   + dc=  And   Some anImAls,dc = eXample,dc= cOm" );
        dn.normalize( oids );

        assertEquals( dn, deserializeDN( serializeDN( dn ) ) );
    }


    @Test
    public void testSerializeEmptyDN() throws Exception
    {
        LdapDN dn = LdapDN.EMPTY_LDAPDN;

        assertEquals( dn, deserializeDN( serializeDN( dn ) ) );
    }


    /**
     * Test the serialization of a DN
     *
     * @throws Exception
     */
    @Test
    public void testNameStaticSerialization() throws Exception
    {
        LdapDN dn = new LdapDN( "ou= Some   People   + dc=  And   Some anImAls,dc = eXample,dc= cOm" );
        dn.normalize( oids );

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream out = new ObjectOutputStream( baos );

        LdapDNSerializer.serialize( dn, out );
        out.flush();

        byte[] data = baos.toByteArray();
        ObjectInputStream in = new ObjectInputStream( new ByteArrayInputStream( data ) );

        assertEquals( dn, LdapDNSerializer.deserialize( in ) );
    }


    /*
    @Test public void testSerializationPerfs() throws Exception
    {
        LdapDN dn = new LdapDN( "ou= Some   People   + dc=  And   Some anImAls,dc = eXample,dc= cOm" );
        dn.normalize( oids );

        long t0 = System.currentTimeMillis();
        
        for ( int i = 0; i < 1000; i++ )
        {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ObjectOutputStream out = new ObjectOutputStream( baos );

            DnSerializer.serialize( dn, out );
            
            byte[] data = baos.toByteArray();
            ObjectInputStream in = new ObjectInputStream( new ByteArrayInputStream( data ) );
            
            LdapDN dn1 = DnSerializer.deserialize( in );
        }
        
        long t1 = System.currentTimeMillis();
        
        System.out.println( "delta :" + ( t1 - t0) );

        long t2 = System.currentTimeMillis();
        
        for ( int i = 0; i < 1000000; i++ )
        {
            //ByteArrayOutputStream baos = new ByteArrayOutputStream();
            //ObjectOutputStream out = new ObjectOutputStream( baos );

            //DnSerializer.serializeString( dn, out );
            
            //byte[] data = baos.toByteArray();
            //ObjectInputStream in = new ObjectInputStream( new ByteArrayInputStream( data ) );
            
            //LdapDN dn1 = DnSerializer.deserializeString( in, oids );
            dn.normalize( oids );
        }
        
        long t3 = System.currentTimeMillis();

        System.out.println( "delta :" + ( t3 - t2) );

        //assertEquals( dn, DnSerializer.deserialize( in ) );
    }
    */

    @Test
    public void testStaticSerializeEmptyDN() throws Exception
    {
        LdapDN dn = LdapDN.EMPTY_LDAPDN;

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream out = new ObjectOutputStream( baos );

        LdapDNSerializer.serialize( dn, out );
        out.flush();

        byte[] data = baos.toByteArray();
        ObjectInputStream in = new ObjectInputStream( new ByteArrayInputStream( data ) );

        assertEquals( dn, LdapDNSerializer.deserialize( in ) );
        assertEquals( dn, deserializeDN( serializeDN( dn ) ) );
    }
    
    @Test
    public void testCompositeRDN() throws InvalidNameException
    {
        assertTrue( LdapDN.isValid( "a=b+c=d+e=f,g=h" ) );

        LdapDN dn = new LdapDN( "a=b+c=d+e=f,g=h" );
        
        assertEquals( "a=b+c=d+e=f,g=h", dn.toString() );
    }

    @Test
    public void testCompositeRDNOids() throws InvalidNameException
    {
        assertTrue( LdapDN.isValid( "1.2.3.4.5=0+1.2.3.4.6=0+1.2.3.4.7=omnischmomni,2.5.4.3=subtree,0.9.2342.19200300.100.1.25=example,0.9.2342.19200300.100.1.25=com" ) );

        LdapDN dn = new LdapDN( "1.2.3.4.5=0+1.2.3.4.6=0+1.2.3.4.7=omnischmomni,2.5.4.3=subtree,0.9.2342.19200300.100.1.25=example,0.9.2342.19200300.100.1.25=com" );
        
        assertEquals( "1.2.3.4.5=0+1.2.3.4.6=0+1.2.3.4.7=omnischmomni,2.5.4.3=subtree,0.9.2342.19200300.100.1.25=example,0.9.2342.19200300.100.1.25=com", dn.toString() );
    }

    /**
     * Tests that AttributeTypeAndValues are correctly trimmed.
     */
    @Test
    public void testTrimAtavs() throws InvalidNameException
    {
        // antlr parser: string value with trailing spaces
        LdapDN dn1 = new LdapDN( " cn = Amos\\,Tori , ou=system " );
        assertEquals( " cn = Amos\\,Tori ", dn1.getRdn().getUpName() );
        AttributeTypeAndValue atav1 = dn1.getRdn().getAtav();
        assertEquals( "cn", atav1.getUpType() );
        assertEquals( "Amos\\,Tori", atav1.getUpValue().getString() );

        // antlr parser: hexstring with trailing spaces
        LdapDN dn3 = new LdapDN( " cn = #414243 , ou=system " );
        assertEquals( " cn = #414243 ", dn3.getRdn().getUpName() );
        AttributeTypeAndValue atav3 = dn3.getRdn().getAtav();
        assertEquals( "cn", atav3.getUpType() );
        assertEquals( "#414243", atav3.getUpValue().getString() );
        assertTrue( Arrays.equals( StringTools.getBytesUtf8( "ABC" ),atav3.getNormValue().getBytes() ) );

        // antlr parser: 
        LdapDN dn4 = new LdapDN( " cn = \\41\\42\\43 , ou=system " );
        assertEquals( " cn = \\41\\42\\43 ", dn4.getRdn().getUpName() );
        AttributeTypeAndValue atav4 = dn4.getRdn().getAtav();
        assertEquals( "cn", atav4.getUpType() );
        assertEquals( "\\41\\42\\43", atav4.getUpValue().getString() );
        assertEquals( "ABC", atav4.getNormValue().getString() );

        // antlr parser: quotestring with trailing spaces
        LdapDN dn5 = new LdapDN( " cn = \"ABC\" , ou=system " );
        assertEquals( " cn = \"ABC\" ", dn5.getRdn().getUpName() );
        AttributeTypeAndValue atav5 = dn5.getRdn().getAtav();
        assertEquals( "cn", atav5.getUpType() );
        assertEquals( "\"ABC\"", atav5.getUpValue().getString() );
        assertEquals( "ABC", atav5.getNormValue().getString() );

        // fast parser: string value with trailing spaces 
        LdapDN dn2 = new LdapDN( " cn = Amos Tori , ou=system " );
        assertEquals( " cn = Amos Tori ", dn2.getRdn().getUpName() );
        AttributeTypeAndValue atav2 = dn2.getRdn().getAtav();
        assertEquals( "cn", atav2.getUpType() );
        assertEquals( "Amos Tori", atav2.getUpValue().getString() );
    }
}
