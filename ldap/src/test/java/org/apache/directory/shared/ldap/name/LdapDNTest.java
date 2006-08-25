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



import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.naming.InvalidNameException;
import javax.naming.Name;
import javax.naming.NamingException;
import javax.naming.ldap.LdapName;

import junit.framework.Assert;
import junit.framework.TestCase;

import org.apache.directory.shared.asn1.codec.DecoderException;
import org.apache.directory.shared.ldap.name.LdapDN;
import org.apache.directory.shared.ldap.name.LdapDnParser;
import org.apache.directory.shared.ldap.name.Rdn;
import org.apache.directory.shared.ldap.schema.DeepTrimToLowerNormalizer;
import org.apache.directory.shared.ldap.schema.OidNormalizer;
import org.apache.directory.shared.ldap.util.StringTools;


/**
 * Test the class LdapDN
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class LdapDNTest extends TestCase
{
   // ~ Methods
   // ------------------------------------------------------------------------------------

   /**
    * Setup the test
    */
   protected void setUp()
   {
   }


   // CONSTRUCTOR functions --------------------------------------------------

   /**
    * Test a null DN
    */
   public void testLdapDNNull() throws DecoderException
   {
       Assert.assertEquals( "", new LdapDN().getUpName() );
   }


   /**
    * test an empty DN
    */
   public void testLdapDNEmpty() throws InvalidNameException
   {
       Assert.assertEquals( "", new LdapDN( "" ).getUpName() );
   }


   /**
    * test a simple DN : a = b
    */
   public void testLdapDNSimple() throws InvalidNameException
   {
       LdapDN dn = new LdapDN( "a = b" );
       Assert.assertEquals( "a = b", dn.getUpName() );
       Assert.assertEquals( "a=b", dn.toString() );
   }

   /**
    * test a simple DN with some spaces : "a = b  "
    */
   public void testLdapDNSimpleWithSpaces() throws InvalidNameException
   {
       LdapDN dn = new LdapDN( "a = b  " );
       Assert.assertEquals( "a = b  ", dn.getUpName() );
       Assert.assertEquals( "a=b", dn.toString() );
   }


   /**
    * test a composite DN : a = b, d = e
    */
   public void testLdapDNComposite() throws InvalidNameException
   {
       LdapDN dn = new LdapDN( "a = b, c = d" );
       Assert.assertEquals( "a=b,c=d", dn.toString() );
       Assert.assertEquals( "a = b, c = d", dn.getUpName() );
   }

   /**
    * test a composite DN with spaces : a = b  , d = e
    */
   public void testLdapDNCompositeWithSpaces() throws InvalidNameException
   {
       LdapDN dn = new LdapDN( "a = b  , c = d" );
       Assert.assertEquals( "a=b,c=d", dn.toString() );
       Assert.assertEquals( "a = b  , c = d", dn.getUpName() );
   }


   /**
    * test a composite DN with or without spaces: a=b, a =b, a= b, a = b, a = b
    */
   public void testLdapDNCompositeWithSpace() throws InvalidNameException
   {
       LdapDN dn = new LdapDN( "a=b, a =b, a= b, a = b, a  =  b" );
       Assert.assertEquals( "a=b,a=b,a=b,a=b,a=b", dn.toString() );
       Assert.assertEquals( "a=b, a =b, a= b, a = b, a  =  b", dn.getUpName() );
   }


   /**
    * test a composite DN with differents separators : a=b;c=d,e=f It should
    * return a=b,c=d,e=f (the ';' is replaced by a ',')
    */
   public void testLdapDNCompositeSepators() throws InvalidNameException
   {
       LdapDN dn = new LdapDN( "a=b;c=d,e=f" );
       Assert.assertEquals( "a=b,c=d,e=f", dn.toString() );
       Assert.assertEquals( "a=b;c=d,e=f", dn.getUpName() );
   }


   /**
    * test a simple DN with multiple NameComponents : a = b + c = d
    */
   public void testLdapDNSimpleMultivaluedAttribute() throws InvalidNameException
   {
       LdapDN dn = new LdapDN( "a = b + c = d" );
       Assert.assertEquals( "a=b+c=d", dn.toString() );
       Assert.assertEquals( "a = b + c = d", dn.getUpName() );
   }


   /**
    * test a composite DN with multiple NC and separators : a=b+c=d, e=f + g=h +
    * i=j
    */
   public void testLdapDNCompositeMultivaluedAttribute() throws InvalidNameException
   {
       LdapDN dn = new LdapDN( "a=b+c=d, e=f + g=h + i=j" );
       Assert.assertEquals( "a=b+c=d,e=f+g=h+i=j", dn.toString() );
       Assert.assertEquals( "a=b+c=d, e=f + g=h + i=j", dn.getUpName() );
   }

    
   /**
   * Test to see if a DN with multiRdn values is preserved after an addAll.
   */
   public void testAddAllWithMultivaluedAttribute() throws InvalidNameException
   {
       LdapDN dn = new LdapDN( "cn=Kate Bush+sn=Bush,ou=system" );
       LdapDN target = new LdapDN();
       target.addAll( target.size(), dn );
       assertEquals( "cn=Kate Bush+sn=Bush,ou=system", target.toString() );
       System.out.println( target.getUpName() );
       assertEquals( "cn=Kate Bush+sn=Bush,ou=system", target.getUpName() );
   }


   /**
    * test a simple DN with an oid prefix (uppercase) : OID.12.34.56 = azerty
    */
   public void testLdapDNOidUpper() throws InvalidNameException
   {
       LdapDN dn = new LdapDN( "OID.12.34.56 = azerty" );
       Assert.assertEquals( "oid.12.34.56=azerty", dn.toString() );
       Assert.assertEquals( "OID.12.34.56 = azerty", dn.getUpName() );
   }


   /**
    * test a simple DN with an oid prefix (lowercase) : oid.12.34.56 = azerty
    */
   public void testLdapDNOidLower() throws InvalidNameException
   {
       LdapDN dn = new LdapDN( "oid.12.34.56 = azerty" );
       Assert.assertEquals( "oid.12.34.56=azerty", dn.toString() );
       Assert.assertEquals( "oid.12.34.56 = azerty", dn.getUpName() );
   }


   /**
    * test a simple DN with an oid attribut without oid prefix : 12.34.56 =
    * azerty
    */
   public void testLdapDNOidWithoutPrefix() throws InvalidNameException
   {
       LdapDN dn = new LdapDN( "12.34.56 = azerty" );
       Assert.assertEquals( "12.34.56=azerty", dn.toString() );
       Assert.assertEquals( "12.34.56 = azerty", dn.getUpName() );
   }


   /**
    * test a composite DN with an oid attribut wiithout oid prefix : 12.34.56 =
    * azerty; 7.8 = test
    */
   public void testLdapDNCompositeOidWithoutPrefix() throws InvalidNameException
   {
       LdapDN dn = new LdapDN( "12.34.56 = azerty; 7.8 = test" );
       Assert.assertEquals( "12.34.56=azerty,7.8=test", dn.toString() );
       Assert.assertEquals( "12.34.56 = azerty; 7.8 = test", dn.getUpName() );
   }


   /**
    * test a simple DN with pair char attribute value : a = \,\=\+\<\>\#\;\\\"\C4\8D"
    */
   public void testLdapDNPairCharAttributeValue() throws InvalidNameException
   {
       
       LdapDN dn = new LdapDN( "a = \\,\\=\\+\\<\\>\\#\\;\\\\\\\"\\C4\\8D" );
       byte[] expected = new byte[] { 'a', '=', ',', '=', '+', '<', '>', '#', ';', '\\', '"', (byte)0xC4, (byte)0x8D}; 
       Assert.assertEquals( StringTools.utf8ToString( expected ), dn.toString() );
       Assert.assertEquals( "a = \\,\\=\\+\\<\\>\\#\\;\\\\\\\"\\C4\\8D", dn.getUpName() );
   }

   /**
    * test a simple DN with pair char attribute value : "SN=Lu\C4\8Di\C4\87"
    */
   public void testLdapDNRFC253_Lucic() throws InvalidNameException
   {
       LdapDN dn = new LdapDN( "SN=Lu\\C4\\8Di\\C4\\87" );
       byte[] lucic = new byte[] { 's', 'n', '=', 'L', 'u', (byte)0xC4, (byte)0x8D, 'i', (byte)0xC4, (byte)0x87}; 
       Assert.assertEquals( StringTools.utf8ToString( lucic ), dn.toString() );
       Assert.assertEquals( "SN=Lu\\C4\\8Di\\C4\\87", dn.getUpName() );
   }

   /**
    * test a simple DN with hexString attribute value : a = #0010A0AAFF
    */
   public void testLdapDNHexStringAttributeValue() throws InvalidNameException
   {
       LdapDN dn = new LdapDN( "a = #0010A0AAFF" );
       Assert.assertEquals( "a=#0010A0AAFF", dn.toString() );
       Assert.assertEquals( "a = #0010A0AAFF", dn.getUpName() );
   }

   /**
    * test a simple DN with a wrong hexString attribute value : a = #0010Z0AAFF
    */
   public void testLdapDNWrongHexStringAttributeValue() throws InvalidNameException
   {
       try
       {
           new LdapDN( "a = #0010Z0AAFF" );
           fail();
       }
       catch ( InvalidNameException ine )
       {
           Assert.assertTrue( true );
       }
   }

   /**
    * test a simple DN with a wrong hexString attribute value : a = #AABBCCDD3
    */
   public void testLdapDNWrongHexStringAttributeValue2() throws InvalidNameException
   {
       try
       {
           new LdapDN( "a = #AABBCCDD3" );
           fail();
       }
       catch ( InvalidNameException ine )
       {
           Assert.assertTrue( true );
       }
   }

   /**
    * test a simple DN with a quote in attribute value : a = quoted \"value\"
    */
   public void testLdapDNQuoteInAttributeValue() throws InvalidNameException
   {
       LdapDN dn = new LdapDN( "a = quoted \\\"value\\\"" );
       Assert.assertEquals( "a=quoted \"value\"", dn.toString() );
       Assert.assertEquals( "a = quoted \\\"value\\\"", dn.getUpName() );
   }

   /**
    * test a simple DN with quoted attribute value : a = \" quoted value \"
    */
   public void testLdapDNQuotedAttributeValue() throws InvalidNameException
   {
       LdapDN dn = new LdapDN( "a = \\\" quoted value \\\"" );
       Assert.assertEquals( "a=\" quoted value \"", dn.toString() );
       Assert.assertEquals( "a = \\\" quoted value \\\"", dn.getUpName() );
   }


   // REMOVE operation -------------------------------------------------------

   /**
    * test a remove from position 0
    */
   public void testLdapDNRemove0() throws InvalidNameException
   {
       LdapDN dn = new LdapDN( "a=b, c=d, e=f" );
       Assert.assertEquals( "e=f", dn.remove( 0 ).toString() );
       Assert.assertEquals( "a=b,c=d", dn.toString() );
       Assert.assertEquals( "a=b, c=d", dn.getUpName() );
   }


   /**
    * test a remove from position 1
    */
   public void testLdapDNRemove1() throws InvalidNameException
   {
       LdapDN dn = new LdapDN( "a=b, c=d, e=f" );
       Assert.assertEquals( "c=d", dn.remove( 1 ).toString() );
       Assert.assertEquals( "a=b, e=f", dn.getUpName() );
   }


   /**
    * test a remove from position 2
    */
   public void testLdapDNRemove2() throws InvalidNameException
   {
       LdapDN dn = new LdapDN( "a=b, c=d, e=f" );
       Assert.assertEquals( "a=b", dn.remove( 2 ).toString() );
       Assert.assertEquals( " c=d, e=f", dn.getUpName() );
   }


   /**
    * test a remove from position 1 whith semi colon
    */
   public void testLdapDNRemove1WithSemiColon() throws InvalidNameException
   {
       LdapDN dn = new LdapDN( "a=b, c=d; e=f" );
       Assert.assertEquals( "c=d", dn.remove( 1 ).toString() );
       Assert.assertEquals( "a=b, e=f", dn.getUpName() );
   }


   /**
    * test a remove out of bound
    */
   public void testLdapDNRemoveOutOfBound() throws InvalidNameException
   {
       LdapDN dn = new LdapDN( "a=b, c=d; e=f" );

       try
       {
           dn.remove( 4 );
           // We whould never reach this point
           Assert.fail();
       }
       catch ( ArrayIndexOutOfBoundsException aoobe )
       {
           Assert.assertTrue( true );
       }
   }


   // SIZE operations
   /**
    * test a 0 size
    */
   public void testLdapDNSize0() throws InvalidNameException
   {
       LdapDN dn = new LdapDN();
       Assert.assertEquals( 0, dn.size() );
   }


   /**
    * test a 1 size
    */
   public void testLdapDNSize1() throws InvalidNameException
   {
       LdapDN dn = new LdapDN( "a=b" );
       Assert.assertEquals( 1, dn.size() );
   }


   /**
    * test a 3 size
    */
   public void testLdapDNSize3() throws InvalidNameException
   {
       LdapDN dn = new LdapDN( "a=b, c=d, e=f" );
       Assert.assertEquals( 3, dn.size() );
   }


   /**
    * test a 3 size with NameComponents
    */
   public void testLdapDNSize3NC() throws InvalidNameException
   {
       LdapDN dn = new LdapDN( "a=b+c=d, c=d, e=f" );
       Assert.assertEquals( 3, dn.size() );
   }


   /**
    * test size after operations
    */
   public void testLdapResizing() throws InvalidNameException
   {
       LdapDN dn = new LdapDN();
       Assert.assertEquals( 0, dn.size() );

       dn.add( "e = f" );
       Assert.assertEquals( 1, dn.size() );

       dn.add( "c = d" );
       Assert.assertEquals( 2, dn.size() );

       dn.remove( 0 );
       Assert.assertEquals( 1, dn.size() );

       dn.remove( 0 );
       Assert.assertEquals( 0, dn.size() );
   }


   // ADD Operations
   /**
    * test Add on a new LdapDN
    */
   public void testLdapEmptyAdd() throws InvalidNameException
   {
       LdapDN dn = new LdapDN();

       dn.add( "e = f" );
       Assert.assertEquals( "e=f", dn.toString() );
       Assert.assertEquals( "e = f", dn.getUpName() );
       Assert.assertEquals( 1, dn.size() );
   }


   /**
    * test Add to an existing LdapDN
    */
   public void testLdapDNAdd() throws InvalidNameException
   {
       LdapDN dn = new LdapDN( "a=b, c=d" );

       dn.add( "e = f" );
       Assert.assertEquals( "e=f,a=b,c=d", dn.toString() );
       Assert.assertEquals( "e = f,a=b, c=d", dn.getUpName() );
       Assert.assertEquals( 3, dn.size() );
   }


   /**
    * test Add a composite RDN to an existing LdapDN
    */
   public void testLdapDNAddComposite() throws InvalidNameException
   {
       LdapDN dn = new LdapDN( "a=b, c=d" );

       dn.add( "e = f + g = h" );

       // Warning ! The order of AVAs has changed during the parsing
       // This has no impact on the correctness of the DN, but the
       // String used to do the comparizon should be inverted.
       Assert.assertEquals( "e=f+g=h,a=b,c=d", dn.toString() );
       Assert.assertEquals( 3, dn.size() );
   }


   /**
    * test Add at the end of an existing LdapDN
    */
   public void testLdapDNAddEnd() throws InvalidNameException
   {
       LdapDN dn = new LdapDN( "a=b, c=d" );

       dn.add( dn.size(), "e = f" );
       Assert.assertEquals( "e = f,a=b, c=d", dn.getUpName() );
       Assert.assertEquals( 3, dn.size() );
   }


   /**
    * test Add at the start of an existing LdapDN
    */
   public void testLdapDNAddStart() throws InvalidNameException
   {
       LdapDN dn = new LdapDN( "a=b, c=d" );

       dn.add( 0, "e = f" );
       Assert.assertEquals( "a=b, c=d,e = f", dn.getUpName() );
       Assert.assertEquals( 3, dn.size() );
   }


   /**
    * test Add at the middle of an existing LdapDN
    */
   public void testLdapDNAddMiddle() throws InvalidNameException
   {
       LdapDN dn = new LdapDN( "a=b, c=d" );

       dn.add( 1, "e = f" );
       Assert.assertEquals( "a=b,e = f, c=d", dn.getUpName() );
       Assert.assertEquals( 3, dn.size() );
   }


   // ADD ALL Operations
   /**
    * Test AddAll
    *
    * @throws InvalidNameException
    */
   public void testLdapDNAddAll() throws InvalidNameException
   {
       LdapDN dn = new LdapDN( "a = b" );
       LdapDN dn2 = new LdapDN( "c = d" );
       dn.addAll( dn2 );
       Assert.assertEquals( "c = d,a = b", dn.getUpName() );
   }


   /**
    * Test AddAll with an empty added name
    *
    * @throws InvalidNameException
    */
   public void testLdapDNAddAllAddedNameEmpty() throws InvalidNameException
   {
       LdapDN dn = new LdapDN( "a = b" );
       LdapDN dn2 = new LdapDN();
       dn.addAll( dn2 );
       Assert.assertEquals( "a=b", dn.toString() );
       Assert.assertEquals( "a = b", dn.getUpName() );
   }


   /**
    * Test AddAll to an empty name
    *
    * @throws InvalidNameException
    */
   public void testLdapDNAddAllNameEmpty() throws InvalidNameException
   {
       LdapDN dn = new LdapDN();
       LdapDN dn2 = new LdapDN( "a = b" );
       dn.addAll( dn2 );
       Assert.assertEquals( "a = b", dn.getUpName() );
   }


   /**
    * Test AddAll at position 0
    *
    * @throws InvalidNameException
    */
   public void testLdapDNAt0AddAll() throws InvalidNameException
   {
       LdapDN dn = new LdapDN( "a = b" );
       LdapDN dn2 = new LdapDN( "c = d" );
       dn.addAll( 0, dn2 );
       Assert.assertEquals( "a = b,c = d", dn.getUpName() );
   }


   /**
    * Test AddAll at position 1
    *
    * @throws InvalidNameException
    */
   public void testLdapDNAt1AddAll() throws InvalidNameException
   {
       LdapDN dn = new LdapDN( "a = b" );
       LdapDN dn2 = new LdapDN( "c = d" );
       dn.addAll( 1, dn2 );
       Assert.assertEquals( "c = d,a = b", dn.getUpName() );
   }


   /**
    * Test AddAll at the middle
    *
    * @throws InvalidNameException
    */
   public void testLdapDNAtTheMiddleAddAll() throws InvalidNameException
   {
       LdapDN dn = new LdapDN( "a = b, c = d" );
       LdapDN dn2 = new LdapDN( "e = f" );
       dn.addAll( 1, dn2 );
       Assert.assertEquals( "a = b,e = f, c = d", dn.getUpName() );
   }


   /**
    * Test AddAll with an empty added name at position 0
    *
    * @throws InvalidNameException
    */
   public void testLdapDNAddAllAt0AddedNameEmpty() throws InvalidNameException
   {
       LdapDN dn = new LdapDN( "a = b" );
       LdapDN dn2 = new LdapDN();
       dn.addAll( 0, dn2 );
       Assert.assertEquals( "a=b", dn.toString() );
       Assert.assertEquals( "a = b", dn.getUpName() );
   }


   /**
    * Test AddAll to an empty name at position 0
    *
    * @throws InvalidNameException
    */
   public void testLdapDNAddAllAt0NameEmpty() throws InvalidNameException
   {
       LdapDN dn = new LdapDN();
       LdapDN dn2 = new LdapDN( "a = b" );
       dn.addAll( 0, dn2 );
       Assert.assertEquals( "a = b", dn.getUpName() );
   }


   // GET PREFIX actions
   /**
    * Get the prefix at pos 0
    */
   public void testLdapDNGetPrefixPos0() throws InvalidNameException
   {
       LdapDN dn = new LdapDN( "a=b, c=d,e = f" );
       LdapDN newDn = ( ( LdapDN ) dn.getPrefix( 0 ) );
       Assert.assertEquals( "", newDn.getUpName() );
   }


   /**
    * Get the prefix at pos 1
    */
   public void testLdapDNGetPrefixPos1() throws InvalidNameException
   {
       LdapDN dn = new LdapDN( "a=b, c=d,e = f" );
       LdapDN newDn = ( ( LdapDN ) dn.getPrefix( 1 ) );
       Assert.assertEquals( "e = f", newDn.getUpName() );
   }


   /**
    * Get the prefix at pos 2
    */
   public void testLdapDNGetPrefixPos2() throws InvalidNameException
   {
       LdapDN dn = new LdapDN( "a=b, c=d,e = f" );
       LdapDN newDn = ( ( LdapDN ) dn.getPrefix( 2 ) );
       Assert.assertEquals( " c=d,e = f", newDn.getUpName() );
   }


   /**
    * Get the prefix at pos 3
    */
   public void testLdapDNGetPrefixPos3() throws InvalidNameException
   {
       LdapDN dn = new LdapDN( "a=b, c=d,e = f" );
       LdapDN newDn = ( ( LdapDN ) dn.getPrefix( 3 ) );
       Assert.assertEquals( "a=b, c=d,e = f", newDn.getUpName() );
   }


   /**
    * Get the prefix out of bound
    */
   public void testLdapDNGetPrefixPos4() throws InvalidNameException
   {
       LdapDN dn = new LdapDN( "a=b, c=d,e = f" );

       try
       {
           dn.getPrefix( 4 );
           // We should not reach this point.
           Assert.fail();
       }
       catch ( ArrayIndexOutOfBoundsException aoobe )
       {
           Assert.assertTrue( true );
       }
   }


   /**
    * Get the prefix of an empty LdapName
    */
   public void testLdapDNGetPrefixEmptyDN() throws InvalidNameException
   {
       LdapDN dn = new LdapDN();
       LdapDN newDn = ( ( LdapDN ) dn.getPrefix( 0 ) );
       Assert.assertEquals( "", newDn.getUpName() );
   }


   // GET SUFFIX operations
   /**
    * Get the suffix at pos 0
    */
   public void testLdapDNGetSuffixPos0() throws InvalidNameException
   {
       LdapDN dn = new LdapDN( "a=b, c=d,e = f" );
       LdapDN newDn = ( ( LdapDN ) dn.getSuffix( 0 ) );
       Assert.assertEquals( "a=b, c=d,e = f", newDn.getUpName() );
   }


   /**
    * Get the suffix at pos 1
    */
   public void testLdapDNGetSuffixPos1() throws InvalidNameException
   {
       LdapDN dn = new LdapDN( "a=b, c=d,e = f" );
       LdapDN newDn = ( ( LdapDN ) dn.getSuffix( 1 ) );
       Assert.assertEquals( "a=b, c=d", newDn.getUpName() );
   }


   /**
    * Get the suffix at pos 2
    */
   public void testLdapDNGetSuffixPos2() throws InvalidNameException
   {
       LdapDN dn = new LdapDN( "a=b, c=d,e = f" );
       LdapDN newDn = ( ( LdapDN ) dn.getSuffix( 2 ) );
       Assert.assertEquals( "a=b", newDn.getUpName() );
   }


   /**
    * Get the suffix at pos 3
    */
   public void testLdapDNGetSuffixPos3() throws InvalidNameException
   {
       LdapDN dn = new LdapDN( "a=b, c=d,e = f" );
       LdapDN newDn = ( ( LdapDN ) dn.getSuffix( 3 ) );
       Assert.assertEquals( "", newDn.getUpName() );
   }


   /**
    * Get the suffix out of bound
    */
   public void testLdapDNGetSuffixPos4() throws InvalidNameException
   {
       LdapDN dn = new LdapDN( "a=b, c=d,e = f" );

       try
       {
           dn.getSuffix( 4 );
           // We should not reach this point.
           Assert.fail();
       }
       catch ( ArrayIndexOutOfBoundsException aoobe )
       {
           Assert.assertTrue( true );
       }
   }


   /**
    * Get the suffix of an empty LdapName
    */
   public void testLdapDNGetSuffixEmptyDN() throws InvalidNameException
   {
       LdapDN dn = new LdapDN();
       LdapDN newDn = ( ( LdapDN ) dn.getSuffix( 0 ) );
       Assert.assertEquals( "", newDn.getUpName() );
   }


   // IS EMPTY operations
   /**
    * Test that a LdapDN is empty
    */
   public void testLdapDNIsEmpty() throws InvalidNameException
   {
       LdapDN dn = new LdapDN();
       Assert.assertEquals( true, dn.isEmpty() );
   }


   /**
    * Test that a LdapDN is empty
    */
   public void testLdapDNNotEmpty() throws InvalidNameException
   {
       LdapDN dn = new LdapDN( "a=b" );
       Assert.assertEquals( false, dn.isEmpty() );
   }


   /**
    * Test that a LdapDN is empty
    */
   public void testLdapDNRemoveIsEmpty() throws InvalidNameException
   {
       LdapDN dn = new LdapDN( "a=b, c=d" );
       dn.remove( 0 );
       dn.remove( 0 );

       Assert.assertEquals( true, dn.isEmpty() );
   }


   // STARTS WITH operations
   /**
    * Test a startsWith a null LdapDN
    */
   public void testLdapDNStartsWithNull() throws InvalidNameException
   {
       LdapDN dn = new LdapDN( "a=b, c=d,e = f" );
       Assert.assertEquals( true, dn.startsWith( null ) );
   }


   /**
    * Test a startsWith an empty LdapDN
    */
   public void testLdapDNStartsWithEmpty() throws InvalidNameException
   {
       LdapDN dn = new LdapDN( "a=b, c=d,e = f" );
       Assert.assertEquals( true, dn.startsWith( new LdapDN() ) );
   }


   /**
    * Test a startsWith an simple LdapDN
    */
   public void testLdapDNStartsWithSimple() throws InvalidNameException
   {
       LdapDN dn = new LdapDN( "a=b, c=d,e = f" );
       Assert.assertEquals( true, dn.startsWith( new LdapDN( "e=f" ) ) );
   }


   /**
    * Test a startsWith a complex LdapDN
    */
   public void testLdapDNStartsWithComplex() throws InvalidNameException
   {
       LdapDN dn = new LdapDN( "a=b, c=d,e = f" );
       Assert.assertEquals( true, dn.startsWith( new LdapDN( "c =  d, e =  f" ) ) );
   }


   /**
    * Test a startsWith a complex LdapDN
    */
   public void testLdapDNStartsWithComplexMixedCase() throws InvalidNameException
   {
       LdapDN dn = new LdapDN( "a=b, c=d,e = f" );
       Assert.assertEquals( false, dn.startsWith( new LdapDN( "c =  D, E =  f" ) ) );
   }


   /**
    * Test a startsWith a full LdapDN
    */
   public void testLdapDNStartsWithFull() throws InvalidNameException
   {
       LdapDN dn = new LdapDN( "a=b, c=d,e = f" );
       Assert.assertEquals( true, dn.startsWith( new LdapDN( "a=  b; c =  d, e =  f" ) ) );
   }


   /**
    * Test a startsWith which returns false
    */
   public void testLdapDNStartsWithWrong() throws InvalidNameException
   {
       LdapDN dn = new LdapDN( "a=b, c=d,e = f" );
       Assert.assertEquals( false, dn.startsWith( new LdapDN( "c =  t, e =  f" ) ) );
   }


   // ENDS WITH operations
   /**
    * Test a endsWith a null LdapDN
    */
   public void testLdapDNEndsWithNull() throws InvalidNameException
   {
       LdapDN dn = new LdapDN( "a=b, c=d,e = f" );
       Assert.assertEquals( true, dn.endsWith( null ) );
   }


   /**
    * Test a endsWith an empty LdapDN
    */
   public void testLdapDNEndsWithEmpty() throws InvalidNameException
   {
       LdapDN dn = new LdapDN( "a=b, c=d,e = f" );
       Assert.assertEquals( true, dn.endsWith( new LdapDN() ) );
   }


   /**
    * Test a endsWith an simple LdapDN
    */
   public void testLdapDNEndsWithSimple() throws InvalidNameException
   {
       LdapDN dn = new LdapDN( "a=b, c=d,e = f" );
       Assert.assertEquals( true, dn.endsWith( new LdapDN( "a=b" ) ) );
   }


   /**
    * Test a endsWith a complex LdapDN
    */
   public void testLdapDNEndsWithComplex() throws InvalidNameException
   {
       LdapDN dn = new LdapDN( "a=b, c=d,e = f" );
       Assert.assertEquals( true, dn.endsWith( new LdapDN( "a =  b, c =  d" ) ) );
   }


   /**
    * Test a endsWith a complex LdapDN
    */
   public void testLdapDNEndsWithComplexMixedCase() throws InvalidNameException
   {
       LdapDN dn = new LdapDN( "a=b, c=d,e = f" );
       Assert.assertEquals( false, dn.endsWith( new LdapDN( "a =  B, C =  d" ) ) );
   }


   /**
    * Test a endsWith a full LdapDN
    */
   public void testLdapDNEndsWithFull() throws InvalidNameException
   {
       LdapDN dn = new LdapDN( "a=b, c=d,e = f" );
       Assert.assertEquals( true, dn.endsWith( new LdapDN( "a=  b; c =  d, e =  f" ) ) );
   }


   /**
    * Test a endsWith which returns false
    */
   public void testLdapDNEndsWithWrong() throws InvalidNameException
   {
       LdapDN dn = new LdapDN( "a=b, c=d,e = f" );
       Assert.assertEquals( false, dn.endsWith( new LdapDN( "a =  b, e =  f" ) ) );
   }


   // GET ALL operations
   /**
    * test a getAll operation on a null DN
    */
   public void testLdapDNGetAllNull() throws InvalidNameException
   {
       LdapDN dn = new LdapDN();
       Enumeration nc = dn.getAll();

       Assert.assertEquals( false, nc.hasMoreElements() );
   }


   /**
    * test a getAll operation on an empty DN
    */
   public void testLdapDNGetAllEmpty() throws InvalidNameException
   {
       LdapDN dn = new LdapDN( "" );
       Enumeration nc = dn.getAll();

       Assert.assertEquals( false, nc.hasMoreElements() );
   }


   /**
    * test a getAll operation on a simple DN
    */
   public void testLdapDNGetAllSimple() throws InvalidNameException
   {
       LdapDN dn = new LdapDN( "a=b" );
       Enumeration nc = dn.getAll();

       Assert.assertEquals( true, nc.hasMoreElements() );
       Assert.assertEquals( "a=b", nc.nextElement() );
       Assert.assertEquals( false, nc.hasMoreElements() );
   }


   /**
    * test a getAll operation on a complex DN
    */
   public void testLdapDNGetAllComplex() throws InvalidNameException
   {
       LdapDN dn = new LdapDN( "e=f+g=h,a=b,c=d" );
       Enumeration nc = dn.getAll();

       Assert.assertEquals( true, nc.hasMoreElements() );
       Assert.assertEquals( "c=d", nc.nextElement() );
       Assert.assertEquals( true, nc.hasMoreElements() );
       Assert.assertEquals( "a=b", nc.nextElement() );
       Assert.assertEquals( true, nc.hasMoreElements() );
       Assert.assertEquals( "e=f+g=h", nc.nextElement() );
       Assert.assertEquals( false, nc.hasMoreElements() );
   }


   /**
    * test a getAll operation on a complex DN
    */
   public void testLdapDNGetAllComplexOrdered() throws InvalidNameException
   {
       LdapDN dn = new LdapDN( "g=h+e=f,a=b,c=d" );
       Enumeration nc = dn.getAll();

       Assert.assertEquals( true, nc.hasMoreElements() );
       Assert.assertEquals( "c=d", nc.nextElement() );
       Assert.assertEquals( true, nc.hasMoreElements() );
       Assert.assertEquals( "a=b", nc.nextElement() );
       Assert.assertEquals( true, nc.hasMoreElements() );

       // The lowest atav should be the first one
       Assert.assertEquals( "e=f+g=h", nc.nextElement() );
       Assert.assertEquals( false, nc.hasMoreElements() );
   }


   // CLONE Operation
   /**
    * test a clone operation on a empty DN
    */
   public void testLdapDNCloneEmpty() throws InvalidNameException
   {
       LdapDN dn = new LdapDN();
       LdapDN clone = ( LdapDN ) dn.clone();

       Assert.assertEquals( "", clone.getUpName() );
   }


   /**
    * test a clone operation on a simple DN
    */
   public void testLdapDNCloneSimple() throws InvalidNameException
   {
       LdapDN dn = new LdapDN( "a=b" );
       LdapDN clone = ( LdapDN ) dn.clone();

       Assert.assertEquals( "a=b", clone.getUpName() );
       dn.remove( 0 );
       Assert.assertEquals( "a=b", clone.getUpName() );
   }


   /**
    * test a clone operation on a complex DN
    */
   public void testLdapDNCloneComplex() throws InvalidNameException
   {
       LdapDN dn = new LdapDN( "e=f+g=h,a=b,c=d" );
       LdapDN clone = ( LdapDN ) dn.clone();

       Assert.assertEquals( "e=f+g=h,a=b,c=d", clone.getUpName() );
       dn.remove( 2 );
       Assert.assertEquals( "e=f+g=h,a=b,c=d", clone.getUpName() );
   }


   // GET operations
   /**
    * test a get in a null DN
    */
   public void testLdapDNGetNull() throws InvalidNameException
   {
       LdapDN dn = new LdapDN();
       Assert.assertEquals( "", dn.get( 0 ) );
   }


   /**
    * test a get in an empty DN
    */
   public void testLdapDNGetEmpty() throws InvalidNameException
   {
       LdapDN dn = new LdapDN( "" );
       Assert.assertEquals( "", dn.get( 0 ) );
   }


   /**
    * test a get in a simple DN
    */
   public void testLdapDNGetSimple() throws InvalidNameException
   {
       LdapDN dn = new LdapDN( "a = b" );
       Assert.assertEquals( "a=b", dn.get( 0 ) );
   }


   /**
    * test a get in a complex DN
    */
   public void testLdapDNGetComplex() throws InvalidNameException
   {
       LdapDN dn = new LdapDN( "a = b + c= d, e= f; g =h" );
       Assert.assertEquals( "g=h", dn.get( 0 ) );
       Assert.assertEquals( "e=f", dn.get( 1 ) );
       Assert.assertEquals( "a=b+c=d", dn.get( 2 ) );
   }


   /**
    * test a get out of bound
    */
   public void testLdapDNGetOutOfBound() throws InvalidNameException
   {
       LdapDN dn = new LdapDN( "a = b + c= d, e= f; g =h" );

       try
       {
           dn.get( 4 );
           Assert.fail();
       }
       catch ( ArrayIndexOutOfBoundsException aioob )
       {
           Assert.assertTrue( true );
       }
   }


   /**
    * Tests the examples from the JNDI tutorials to make sure LdapName behaves
    * appropriately. The example can be found online <a href="">here</a>.
    *
    * @throws Exception
    *             if anything goes wrong
    */
   public void testJNDITutorialExample() throws Exception
   {
       // Parse the name
       Name name = new LdapDN( "cn=John,ou=People,ou=Marketing" );

       // Remove the second component from the head: ou=People
       String out = name.remove( 1 ).toString();

       // System.out.println( l_out ) ;
       assertEquals( "ou=People", out );

       // Add to the head (first): cn=John,ou=Marketing,ou=East
       out = name.add( 0, "ou=East" ).toString();

       assertEquals( "cn=John,ou=Marketing,ou=East", out );

       // Add to the tail (last): cn=HomeDir,cn=John,ou=Marketing,ou=East
       out = name.add( "cn=HomeDir" ).toString();

       assertEquals( "cn=HomeDir,cn=John,ou=Marketing,ou=East", out );
   }


   public void testAttributeEqualsIsCaseInSensitive() throws Exception
   {
       Name name1 = new LdapDN( "cn=HomeDir" );
       Name name2 = new LdapDN( "CN=HomeDir" );

       assertTrue( name1.equals( name2 ) );
   }


   public void testAttributeTypeEqualsIsCaseInsensitive() throws Exception
   {
       Name name1 = new LdapDN( "cn=HomeDir+cn=WorkDir" );
       Name name2 = new LdapDN( "cn=HomeDir+CN=WorkDir" );

       assertTrue( name1.equals( name2 ) );
   }


   public void testNameEqualsIsInsensitiveToAttributesOrder() throws Exception
   {

       Name name1 = new LdapDN( "cn=HomeDir+cn=WorkDir" );
       Name name2 = new LdapDN( "cn=WorkDir+cn=HomeDir" );

       assertTrue( name1.equals( name2 ) );
   }


   public void testAttributeComparisonIsCaseInSensitive() throws Exception
   {
       Name name1 = new LdapDN( "cn=HomeDir" );
       Name name2 = new LdapDN( "CN=HomeDir" );

       assertEquals( 0, name1.compareTo( name2 ) );
   }


   public void testAttributeTypeComparisonIsCaseInsensitive() throws Exception
   {
       Name name1 = new LdapDN( "cn=HomeDir+cn=WorkDir" );
       Name name2 = new LdapDN( "cn=HomeDir+CN=WorkDir" );

       assertEquals( 0, name1.compareTo( name2 ) );
   }


   public void testNameComparisonIsInsensitiveToAttributesOrder() throws Exception
   {

       Name name1 = new LdapDN( "cn=HomeDir+cn=WorkDir" );
       Name name2 = new LdapDN( "cn=WorkDir+cn=HomeDir" );

       assertEquals( 0, name1.compareTo( name2 ) );
   }


   public void testNameComparisonIsInsensitiveToAttributesOrderFailure() throws Exception
   {

       Name name1 = new LdapDN( "cn= HomeDir+cn=Workdir" );
       Name name2 = new LdapDN( "cn = Work+cn=HomeDir" );

       assertEquals( 1, name1.compareTo( name2 ) );
   }


   /**
    * Test the encoding of a LdanDN
    */
   public void testNameToBytes() throws Exception
   {
       LdapDN dn = new LdapDN( "cn = John, ou = People, OU = Marketing" );

       byte[] bytes = LdapDN.getBytes( dn );

       Assert.assertEquals( 30, LdapDN.getNbBytes( dn ) );
       Assert.assertEquals( "cn=John,ou=People,ou=Marketing", new String( bytes, "UTF-8" ) );
   }


   public void testStringParser() throws Exception
   {
       
       String dn = StringTools.utf8ToString( new byte[]{'C', 'N', ' ', '=', ' ', 'E', 'm', 'm', 'a', 'n', 'u', 'e', 
           'l', ' ', ' ', 'L', (byte)0xc3, (byte)0xa9, 'c', 'h', 'a', 'r', 'n', 'y'} );

       String expected = StringTools.utf8ToString( new byte[]{'c', 'n', '=','E', 'm', 'm', 'a', 'n', 'u', 'e', 
           'l', ' ', ' ', 'L', (byte)0xc3, (byte)0xa9, 'c', 'h', 'a', 'r', 'n', 'y'} );

       Name name = LdapDnParser.getNameParser().parse( dn );

       Assert.assertEquals( dn, ( ( LdapDN ) name ).getUpName() );
       Assert.assertEquals( expected, name.toString() );
   }


   /**
    * Class to test for void LdapName(String)
    *
    * @throws Exception
    *             if anything goes wrong.
    */
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
   public void testLdapName()
   {
       Name name = new LdapDN();
       assertTrue( name.toString().equals( "" ) );
   }


   /**
    * Class to test for void LdapName(List)
    */
   public void testLdapNameList() throws InvalidNameException
   {
       ArrayList list = new ArrayList();
       list.add( "ou=People" );
       list.add( "dc=example" );
       list.add( "dc=com" );
       Name name = new LdapDN( list );
       assertTrue( name.toString().equals( "ou=People,dc=example,dc=com" ) );
   }


   /**
    * Class to test for void LdapName(Iterator)
    */
   public void testLdapNameIterator() throws InvalidNameException
   {
       ArrayList list = new ArrayList();
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

       List list = new ArrayList();

       Comparator comparator = new Comparator()
       {
           public int compare( Object obj1, Object obj2 )
           {
               Name name1 = ( LdapDN ) obj1;
               Name name2 = ( LdapDN ) obj2;
               return name1.compareTo( name2 );
           }


           public boolean equals( Object obj )
           {
               return super.equals( obj );
           }


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
   public void testGetAll() throws Exception
   {
       Name name0 = new LdapDN( "" );
       Name name1 = new LdapDN( "ou=East" );
       Name name2 = new LdapDN( "ou=Marketing,ou=East" );
       Name name3 = new LdapDN( "cn=John,ou=Marketing,ou=East" );
       Name name4 = new LdapDN( "cn=HomeDir,cn=John,ou=Marketing,ou=East" );
       Name name5 = new LdapDN( "cn=Website,cn=HomeDir,cn=John,ou=Marketing,ou=West" );
       Name name6 = new LdapDN( "cn=Airline,cn=Website,cn=HomeDir,cn=John,ou=Marketing,ou=West" );

       Enumeration enum0 = name0.getAll();
       assertEquals( false, enum0.hasMoreElements() );

       Enumeration enum1 = name1.getAll();
       assertEquals( true, enum1.hasMoreElements() );

       for ( int i = 0; enum1.hasMoreElements(); i++ )
       {
           String element = ( String ) enum1.nextElement();

           if ( i == 0 )
           {
               assertEquals( "ou=East", element );
           }
       }

       Enumeration enum2 = name2.getAll();
       assertEquals( true, enum2.hasMoreElements() );

       for ( int i = 0; enum2.hasMoreElements(); i++ )
       {
           String element = ( String ) enum2.nextElement();

           if ( i == 0 )
           {
               assertEquals( "ou=East", element.toString() );
           }

           if ( i == 1 )
           {
               assertEquals( "ou=Marketing", element.toString() );
           }
       }

       Enumeration enum3 = name3.getAll();
       assertEquals( true, enum3.hasMoreElements() );

       for ( int i = 0; enum3.hasMoreElements(); i++ )
       {
           String element = ( String ) enum3.nextElement();

           if ( i == 0 )
           {
               assertEquals( "ou=East", element.toString() );
           }

           if ( i == 1 )
           {
               assertEquals( "ou=Marketing", element.toString() );
           }

           if ( i == 2 )
           {
               assertEquals( "cn=John", element.toString() );
           }
       }

       Enumeration enum4 = name4.getAll();
       assertEquals( true, enum4.hasMoreElements() );

       for ( int i = 0; enum4.hasMoreElements(); i++ )
       {
           String element = ( String ) enum4.nextElement();

           if ( i == 0 )
           {
               assertEquals( "ou=East", element.toString() );
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
       }

       Enumeration enum5 = name5.getAll();
       assertEquals( true, enum5.hasMoreElements() );

       for ( int i = 0; enum5.hasMoreElements(); i++ )
       {
           String element = ( String ) enum5.nextElement();

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
       }

       Enumeration enum6 = name6.getAll();
       assertEquals( true, enum6.hasMoreElements() );

       for ( int i = 0; enum6.hasMoreElements(); i++ )
       {
           String element = ( String ) enum6.nextElement();

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
    * Class to test for getAllRdn
    *
    * @throws Exception
    *             if anything goes wrong.
    */
   public void testGetAllRdn() throws Exception
   {
       LdapDN name = new LdapDN( "cn=Airline,cn=Website,cn=HomeDir,cn=John,ou=Marketing,ou=West" );

       Enumeration rdns = name.getAllRdn();
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
    * Class to test for get
    *
    * @throws Exception
    *             anything goes wrong
    */
   public void testGet() throws Exception
   {
       Name name = new LdapDN( "cn=HomeDir,cn=John,ou=Marketing,ou=East" );
       assertEquals( "cn=HomeDir", name.get( 3 ) );
       assertEquals( "cn=John", name.get( 2 ) );
       assertEquals( "ou=Marketing", name.get( 1 ) );
       assertEquals( "ou=East", name.get( 0 ) );
   }


   /**
    * Class to test for getSuffix
    *
    * @throws Exception
    *             anything goes wrong
    */
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
   public void testStartsWith() throws Exception
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

       assertTrue( name0.startsWith( name1 ) );
       assertTrue( name0.startsWith( name2 ) );
       assertTrue( name0.startsWith( name3 ) );
       assertTrue( name0.startsWith( name4 ) );
       assertTrue( name0.startsWith( name5 ) );

       assertTrue( !name0.startsWith( name6 ) );
       assertTrue( !name0.startsWith( name7 ) );
       assertTrue( !name0.startsWith( name8 ) );
   }


   /**
    * Class to test for endsWith
    *
    * @throws Exception
    *             anything goes wrong
    */
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

       /*
        * Hashtable env = new Hashtable() ; env.put(
        * Context.SECURITY_AUTHENTICATION, "simple" ) ; env.put(
        * Context.SECURITY_PRINCIPAL, "cn=admin,dc=example,dc=com" ) ; env.put(
        * Context.SECURITY_CREDENTIALS, "jPasswordField1" ) ; env.put(
        * Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory" ) ;
        * env.put( Context.PROVIDER_URL,
        * "ldap://localhost:1396/dc=example,dc=com" ) ; DirContext ctx = new
        * InitialDirContext( env ) ; NamingEnumeration enum = ctx.listBindings( "" ) ;
        * Name name0 = new LdapDN( "ou=Special Users,dc=example,dc=com" ) ;
        * Name name1 = new LdapDN( "dc=example,dc=com" ) ; Name name2 = new
        * LdapDN( "dc=com" ) ; Name name3 = new LdapDN( "ou=Special Users" ) ;
        * Name name4 = new LdapDN( "ou=Special Users,dc=example" ) ; Name name5 =
        * new LdapDN( "" ) ; while ( enum.hasMore() ) { Binding binding = (
        * Binding ) enum.next() ; DirContext dirCtx = ( DirContext )
        * binding.getObject() ; NameParser parser = dirCtx.getNameParser( "" ) ;
        * Name namex = parser.parse( dirCtx.getNameInNamespace() ) ; //
        * DirContext dirCtx = ( DirContext ) enum.next() ; }
        */
   }


   /**
    * Class to test for Name addAll(Name)
    *
    * @throws Exception
    *             when anything goes wrong
    */
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
   public void testEqualsObject() throws Exception
   {
       assertTrue( new LdapDN( "ou=People" ).equals( new LdapDN( "ou=People" ) ) );

       assertTrue( !new LdapDN( "ou=People,dc=example,dc=com" ).equals( new LdapDN( "ou=People" ) ) );
       assertTrue( !new LdapDN( "ou=people" ).equals( new LdapDN( "ou=People" ) ) );
       assertTrue( !new LdapDN( "ou=Groups" ).equals( new LdapDN( "ou=People" ) ) );
   }


   public void testNameFrenchChars() throws Exception
   {
       String cn = new String( new byte[]
           { 'c', 'n', '=', 0x4A, ( byte ) 0xC3, ( byte ) 0xA9, 0x72, ( byte ) 0xC3, ( byte ) 0xB4, 0x6D, 0x65 } );

       Name name = new LdapDN( cn );

       assertEquals( cn, name.toString() );
   }


   public void testNameGermanChars() throws Exception
   {
       String cn = new String( new byte[]
           { 'c', 'n', '=', ( byte ) 0xC3, ( byte ) 0x84, ( byte ) 0xC3, ( byte ) 0x96, ( byte ) 0xC3, ( byte ) 0x9C,
               ( byte ) 0xC3, ( byte ) 0x9F, ( byte ) 0xC3, ( byte ) 0xA4, ( byte ) 0xC3, ( byte ) 0xB6,
               ( byte ) 0xC3, ( byte ) 0xBC }, "UTF-8" );

       Name name = new LdapDN( cn );

       assertEquals( cn, name.toString() );
   }


   public void testNameTurkishChars() throws Exception
   {
       String cn = new String( new byte[]
           { 'c', 'n', '=', ( byte ) 0xC4, ( byte ) 0xB0, ( byte ) 0xC4, ( byte ) 0xB1, ( byte ) 0xC5, ( byte ) 0x9E,
               ( byte ) 0xC5, ( byte ) 0x9F, ( byte ) 0xC3, ( byte ) 0x96, ( byte ) 0xC3, ( byte ) 0xB6,
               ( byte ) 0xC3, ( byte ) 0x9C, ( byte ) 0xC3, ( byte ) 0xBC, ( byte ) 0xC4, ( byte ) 0x9E,
               ( byte ) 0xC4, ( byte ) 0x9F }, "UTF-8" );

       Name name = new LdapDN( cn );

       assertEquals( cn, name.toString() );
   }


   /**
    * Class to test for toOid( Name, Map)
    */
   public void testLdapNameToName() throws Exception
   {
       ArrayList list = new ArrayList();
       list.add( "ou= Some   People   " );
       list.add( "dc = eXample" );
       list.add( "dc= cOm" );
       LdapDN name = new LdapDN( list.iterator() );

       Map oids = new HashMap();

       oids.put( "dc", new OidNormalizer( "dc", new DeepTrimToLowerNormalizer() ) );
       oids.put( "domaincomponent", new OidNormalizer( "dc", new DeepTrimToLowerNormalizer() ) );
       oids.put( "0.9.2342.19200300.100.1.25", new OidNormalizer( "dc", new DeepTrimToLowerNormalizer() ) );
       oids.put( "ou", new OidNormalizer( "ou", new DeepTrimToLowerNormalizer() ) );
       oids.put( "organizationalUnitName", new OidNormalizer( "ou", new DeepTrimToLowerNormalizer() ) );
       oids.put( "2.5.4.11", new OidNormalizer( "ou", new DeepTrimToLowerNormalizer() ) );

       assertTrue( name.getUpName().equals( "ou= Some   People   ,dc = eXample,dc= cOm" ) );

       Name result = LdapDN.normalize( name, oids );

       assertTrue( result.toString().equals( "ou=some people,dc=example,dc=com" ) );
   }


   /**
    * Class to test for toOid( Name, Map) with a NULL dn
    */
   public void testLdapNameToNameEmpty() throws Exception
   {
       LdapDN name = new LdapDN();

       Map oids = new HashMap();

       oids.put( "dc", new OidNormalizer( "dc", new DeepTrimToLowerNormalizer() ) );
       oids.put( "domaincomponent", new OidNormalizer( "dc", new DeepTrimToLowerNormalizer() ) );
       oids.put( "0.9.2342.19200300.100.1.25", new OidNormalizer( "dc", new DeepTrimToLowerNormalizer() ) );
       oids.put( "ou", new OidNormalizer( "ou", new DeepTrimToLowerNormalizer() ) );
       oids.put( "organizationalUnitName", new OidNormalizer( "ou", new DeepTrimToLowerNormalizer() ) );
       oids.put( "2.5.4.11", new OidNormalizer( "ou", new DeepTrimToLowerNormalizer() ) );

       Name result = LdapDN.normalize( name, oids );
       assertTrue( result.toString().equals( "" ) );
   }


   /**
    * Class to test for toOid( Name, Map) with a multiple NameComponent
    */
   public void testLdapNameToNameMultiNC() throws Exception
   {
       LdapDN name = new LdapDN(
           "2.5.4.11= Some   People   + 0.9.2342.19200300.100.1.25=  And   Some anImAls,0.9.2342.19200300.100.1.25 = eXample,dc= cOm" );

       Map oids = new HashMap();

       oids.put( "dc", new OidNormalizer( "dc", new DeepTrimToLowerNormalizer() ) );
       oids.put( "domaincomponent", new OidNormalizer( "dc", new DeepTrimToLowerNormalizer() ) );
       oids.put( "0.9.2342.19200300.100.1.25", new OidNormalizer( "dc", new DeepTrimToLowerNormalizer() ) );
       oids.put( "ou", new OidNormalizer( "ou", new DeepTrimToLowerNormalizer() ) );
       oids.put( "organizationalUnitName", new OidNormalizer( "ou", new DeepTrimToLowerNormalizer() ) );
       oids.put( "2.5.4.11", new OidNormalizer( "ou", new DeepTrimToLowerNormalizer() ) );

       Name result = LdapDN.normalize( name, oids );

       assertTrue( result.toString().equals( "dc=and some animals+ou=some people,dc=example,dc=com" ) );
       assertTrue( ( ( LdapDN ) result )
           .getUpName()
           .equals(
               "2.5.4.11= Some   People   + 0.9.2342.19200300.100.1.25=  And   Some anImAls,0.9.2342.19200300.100.1.25 = eXample,dc= cOm" ) );
   }


   /**
    * Class to test for toOid( Name, Map) with a multiple NameComponent
    */
   public void testLdapNameToNameAliasMultiNC() throws Exception
   {
       LdapDN name = new LdapDN(
           "2.5.4.11= Some   People   + domainComponent=  And   Some anImAls,DomainComponent = eXample,0.9.2342.19200300.100.1.25= cOm" );

       Map oids = new HashMap();

       oids.put( "dc", new OidNormalizer( "dc", new DeepTrimToLowerNormalizer() ) );
       oids.put( "domaincomponent", new OidNormalizer( "dc", new DeepTrimToLowerNormalizer() ) );
       oids.put( "0.9.2342.19200300.100.1.25", new OidNormalizer( "dc", new DeepTrimToLowerNormalizer() ) );
       oids.put( "ou", new OidNormalizer( "ou", new DeepTrimToLowerNormalizer() ) );
       oids.put( "organizationalUnitName", new OidNormalizer( "ou", new DeepTrimToLowerNormalizer() ) );
       oids.put( "2.5.4.11", new OidNormalizer( "ou", new DeepTrimToLowerNormalizer() ) );

       LdapDN result = LdapDN.normalize( name, oids );

       assertTrue( result.toString().equals( "dc=and some animals+ou=some people,dc=example,dc=com" ) );
       assertTrue( ( ( LdapDN ) result )
           .getUpName()
           .equals(
               "2.5.4.11= Some   People   + domainComponent=  And   Some anImAls,DomainComponent = eXample,0.9.2342.19200300.100.1.25= cOm" ) );
   }


   /**
    * Test the serialization of a DN
    *
    * @throws Exception
    */
   public void testNameSerialization() throws Exception
   {
       LdapDN name = new LdapDN( "ou= Some   People   + dc=  And   Some anImAls,dc = eXample,dc= cOm" );

       FileOutputStream fOut = null;
       ObjectOutputStream oOut = null;
       File file = new File( "LdapDN.ser" );

       try
       {
           fOut = new FileOutputStream( file );
           oOut = new ObjectOutputStream( fOut );
           oOut.writeObject( name );
       }
       catch ( IOException ioe )
       {
           throw ioe;
       }
       finally
       {
           try
           {
               oOut.flush();
               oOut.close();
               fOut.close();
           }
           catch ( IOException ioe )
           {
               throw ioe;
           }
       }

       FileInputStream fIn = null;
       ObjectInputStream oIn = null;

       try
       {
           fIn = new FileInputStream( file );
           oIn = new ObjectInputStream( fIn );

           LdapDN nameSer = ( LdapDN ) oIn.readObject();

           Assert.assertEquals( 0, nameSer.compareTo( name ) );
       }
       catch ( IOException ioe )
       {
           throw ioe;
       }
       finally
       {
           try
           {
               oIn.close();
               fIn.close();
               file.delete();
           }
           catch ( IOException ioe )
           {
               throw ioe;
           }
       }
   }

   /**
    * Class to test for hashCode()
    */
   public void testLdapNameHashCode() throws Exception
   {
       Name name1 = new LdapDN(
           "2.5.4.11= Some   People   + domainComponent=  And   Some anImAls,DomainComponent = eXample,0.9.2342.19200300.100.1.25= cOm" );

       Name name2 = new LdapDN(
           "2.5.4.11=some people+domainComponent=and some animals,DomainComponent=example,0.9.2342.19200300.100.1.25=com" );

       assertEquals( name1.hashCode(), name2.hashCode() );
   }

   /**
    * Test for DIRSERVER-191
    */
   public void testName() throws NamingException
   {
       Name jName = new javax.naming.ldap.LdapName("cn=four,cn=three,cn=two,cn=one");
       Name aName = new LdapDN("cn=four,cn=three,cn=two,cn=one");
       assertEquals(jName.toString(), "cn=four,cn=three,cn=two,cn=one");
       assertEquals(aName.toString(), "cn=four,cn=three,cn=two,cn=one");
       assertEquals(jName.toString(), aName.toString());
   }

   /**
    * Test for DIRSERVER-191
    */
   public void testGetPrefixName() throws NamingException
   {
       Name jName = new LdapName("cn=four,cn=three,cn=two,cn=one");
       Name aName = new LdapDN("cn=four,cn=three,cn=two,cn=one");

       assertEquals(jName.getPrefix(0).toString(), aName.getPrefix(0).toString());
       assertEquals(jName.getPrefix(1).toString(), aName.getPrefix(1).toString());
       assertEquals(jName.getPrefix(2).toString(), aName.getPrefix(2).toString());
       assertEquals(jName.getPrefix(3).toString(), aName.getPrefix(3).toString());
       assertEquals(jName.getPrefix(4).toString(), aName.getPrefix(4).toString());
   }

   /**
    * Test for DIRSERVER-191
    */
   public void testGetSuffix() throws NamingException
   {
       Name jName = new LdapName("cn=four,cn=three,cn=two,cn=one");
       Name aName = new LdapDN("cn=four,cn=three,cn=two,cn=one");

       assertEquals(jName.getSuffix(0).toString(), aName.getSuffix(0).toString());
       assertEquals(jName.getSuffix(1).toString(), aName.getSuffix(1).toString());
       assertEquals(jName.getSuffix(2).toString(), aName.getSuffix(2).toString());
       assertEquals(jName.getSuffix(3).toString(), aName.getSuffix(3).toString());
       assertEquals(jName.getSuffix(4).toString(), aName.getSuffix(4).toString());
   }

   /**
    * Test for DIRSERVER-191
    */
   public void testAddStringName() throws NamingException
   {
       Name jName = new LdapName("cn=four,cn=three,cn=two,cn=one");
       Name aName = new LdapDN("cn=four,cn=three,cn=two,cn=one");

       assertSame(jName, jName.add("cn=five"));
       assertSame(aName, aName.add("cn=five"));
       assertEquals(jName.toString(), aName.toString());
   }

   /**
    * Test for DIRSERVER-191
    */
   public void testAddIntString() throws NamingException
   {
       Name jName = new LdapName("cn=four,cn=three,cn=two,cn=one");
       Name aName = new LdapDN("cn=four,cn=three,cn=two,cn=one");

       assertSame(jName, jName.add(0,"cn=zero"));
       assertSame(aName, aName.add(0,"cn=zero"));
       assertEquals(jName.toString(), aName.toString());

       assertSame(jName, jName.add(2,"cn=one.5"));
       assertSame(aName, aName.add(2,"cn=one.5"));
       assertEquals(jName.toString(), aName.toString());

       assertSame(jName, jName.add(jName.size(),"cn=five"));
       assertSame(aName, aName.add(aName.size(),"cn=five"));
       assertEquals(jName.toString(), aName.toString());
   }

   /**
    * Test for DIRSERVER-191
    */
   public void testAddAllName() throws NamingException
   {
       Name jName = new LdapName("cn=four,cn=three,cn=two,cn=one");
       Name aName = new LdapDN("cn=four,cn=three,cn=two,cn=one");

       assertSame(jName, jName.addAll(new LdapName("cn=seven,cn=six")));
       assertSame(aName, aName.addAll(new LdapDN("cn=seven,cn=six")));
       assertEquals(jName.toString(), aName.toString());
   }

   /**
    * Test for DIRSERVER-191
    */
   public void testAddAllIntName() throws NamingException
   {
       Name jName = new LdapName("cn=four,cn=three,cn=two,cn=one");
       Name aName = new LdapDN("cn=four,cn=three,cn=two,cn=one");

       assertSame(jName, jName.addAll(0, new LdapName("cn=zero,cn=zero.5")));
       assertSame(aName, aName.addAll(0, new LdapDN("cn=zero,cn=zero.5")));
       assertEquals(jName.toString(), aName.toString());


       assertSame(jName, jName.addAll(2, new LdapName("cn=zero,cn=zero.5")));
       assertSame(aName, aName.addAll(2, new LdapDN("cn=zero,cn=zero.5")));
       assertEquals(jName.toString(), aName.toString());


       assertSame(jName, jName.addAll(jName.size(), new LdapName("cn=zero,cn=zero.5")));
       assertSame(aName, aName.addAll(aName.size(), new LdapDN("cn=zero,cn=zero.5")));
       assertEquals(jName.toString(), aName.toString());
   }

   /**
    * Test for DIRSERVER-191
    */
   public void testStartsWithName() throws NamingException
   {
       Name jName = new LdapName("cn=four,cn=three,cn=two,cn=one");
       Name aName = new LdapDN("cn=four,cn=three,cn=two,cn=one");

       assertEquals(jName.startsWith(new LdapName("cn=seven,cn=six,cn=five")),
               aName.startsWith(new LdapDN("cn=seven,cn=six,cn=five")));
       assertEquals(jName.startsWith(new LdapName("cn=three,cn=two,cn=one")),
               aName.startsWith(new LdapDN("cn=three,cn=two,cn=one")));
   }

   /**
    * Test for DIRSERVER-191
    */
   public void testEndsWithName() throws NamingException
   {
       Name jName = new LdapName("cn=four,cn=three,cn=two,cn=one");
       Name aName = new LdapDN("cn=four,cn=three,cn=two,cn=one");

       assertEquals(jName.endsWith(new LdapName("cn=seven,cn=six,cn=five")),
               aName.endsWith(new LdapDN("cn=seven,cn=six,cn=five")));
       assertEquals(jName.endsWith(new LdapName("cn=three,cn=two,cn=one")),
               aName.endsWith(new LdapDN("cn=three,cn=two,cn=one")));
   }

   /**
    * Test for DIRSERVER-191
    */
   public void testRemoveName() throws NamingException
   {
       Name jName = new LdapName("cn=four,cn=three,cn=two,cn=one");
       Name aName = new LdapDN("cn=four,cn=three,cn=two,cn=one");

       assertEquals(jName.remove(0).toString(), aName.remove(0).toString());
       assertEquals(jName.toString(), aName.toString());

       assertEquals(jName.remove(jName.size() - 1).toString(), aName.remove(aName.size() - 1).toString());
       assertEquals(jName.toString(), aName.toString());
   }

   /**
    * Test for DIRSERVER-191
    */
   public void testGetAllName() throws NamingException
   {
       Name jName = new LdapName("cn=four,cn=three,cn=two,cn=one");
       Name aName = new LdapDN("cn=four,cn=three,cn=two,cn=one");

       Enumeration j = jName.getAll();
       Enumeration a = aName.getAll();
       while (j.hasMoreElements())
       {
           assertTrue(j.hasMoreElements());
           assertEquals(j.nextElement(), a.nextElement());
       }
   }

   /**
    * Test for DIRSERVER-642
    * @throws NamingException
    */
   public void testDoubleQuoteInNameDIRSERVER_642() throws NamingException
   {
       Name name1 = new LdapDN( "cn=\"Kylie Minogue\",dc=example,dc=com" );
       Name name2 = new LdapName( "cn=\"Kylie Minogue\",dc=example,dc=com" );

       Enumeration j = name1.getAll();
       Enumeration a = name2.getAll();

       while (j.hasMoreElements())
       {
           assertTrue(j.hasMoreElements());
           assertEquals(j.nextElement(), a.nextElement());
       }
   }

   /**
    * Test for DIRSERVER-642
    * @throws NamingException
    */
   public void testDoubleQuoteInNameDIRSERVER_642_1() throws NamingException
   {
       LdapDN dn = new LdapDN( "cn=\" Kylie Minogue \",dc=example,dc=com" );

       Assert.assertEquals( "cn=\" Kylie Minogue \",dc=example,dc=com", dn.getUpName() );
       Assert.assertEquals( "cn= Kylie Minogue ,dc=example,dc=com", dn.toString() );
   }

   /**
    * Test for DIRSERVER-184
    * @throws NamingException
    */
   public void testLeadingAndTrailingSpacesDIRSERVER_184() throws NamingException
   {
       LdapDN name = new LdapDN( "dn= \\ four spaces leading and 3 trailing \\  " );

       Assert.assertEquals( "dn= four spaces leading and 3 trailing  ", name.toString() );
       Assert.assertEquals( "dn= \\ four spaces leading and 3 trailing \\  ", name.getUpName() );
   }

   /**
    * Test for DIRSERVER-184
    * @throws NamingException
    */
   public void testDIRSERVER_184_1() throws NamingException
   {
       try
       {
           new LdapDN( "dn=middle\\ spaces" );
       }
       catch ( InvalidNameException ine )
       {
           Assert.assertTrue( true );
       }
   }

   /**
    * Test for DIRSERVER-184
    * @throws NamingException
    */
   public void testDIRSERVER_184_2() throws NamingException
   {
       try
       {
           new LdapDN( "dn=# a leading pound" );
       }
       catch ( InvalidNameException ine )
       {
           Assert.assertTrue( true );
       }
   }

   /**
    * Test for DIRSERVER-184
    * @throws NamingException
    */
   public void testDIRSERVER_184_3() throws NamingException
   {
       LdapDN name = new LdapDN( "dn=\\# a leading pound" );

       Assert.assertEquals( "dn=# a leading pound", name.toString() );
       Assert.assertEquals( "dn=\\# a leading pound", name.getUpName() );
   }

   /**
    * Test for DIRSERVER-184
    * @throws NamingException
    */
   public void testDIRSERVER_184_4() throws NamingException
   {
       LdapDN name = new LdapDN( "dn=a middle \\# pound" );

       Assert.assertEquals( "dn=a middle # pound", name.toString() );
       Assert.assertEquals( "dn=a middle \\# pound", name.getUpName() );
   }

   /**
    * Test for DIRSERVER-184
    * @throws NamingException
    */
   public void testDIRSERVER_184_5() throws NamingException
   {
       LdapDN name = new LdapDN( "dn=a trailing pound \\#" );

       Assert.assertEquals( "dn=a trailing pound #", name.toString() );
       Assert.assertEquals( "dn=a trailing pound \\#", name.getUpName() );
   }

   /**
    * Test for DIRSERVER-184
    * @throws NamingException
    */
   public void testDIRSERVER_184_6() throws NamingException
   {
       try
       {
           new LdapDN( "dn=a middle # pound" );
       }
       catch ( InvalidNameException ine )
       {
           Assert.assertTrue( true );
       }
   }

   /**
    * Test for DIRSERVER-184
    * @throws NamingException
    */
   public void testDIRSERVER_184_7() throws NamingException
   {
       try
       {
           new LdapDN( "dn=a trailing pound #" );
       }
       catch ( InvalidNameException ine )
       {
           Assert.assertTrue( true );
       }
   }

   public void testDIRSERVER_631_1() throws NamingException
   {
       LdapDN name = new LdapDN( "cn=Bush\\, Kate,dc=example,dc=com" );

       Assert.assertEquals( "cn=Bush, Kate,dc=example,dc=com", name.toString() );
       Assert.assertEquals( "cn=Bush\\, Kate,dc=example,dc=com", name.getUpName() );

   }

   
   /**
    * Added a test to check the parsing of a DN with more than one RDN
    * which are OIDs, and with one RDN which has more than one atav.
    * @throws NamingException
    */
   public void testDNWithMultiOidsRDN() throws NamingException
   {
       LdapDN name = new LdapDN( "0.9.2342.19200300.100.1.1=00123456789+2.5.4.3=pablo picasso,2.5.4.11=search,2.5.4.10=imc,2.5.4.6=us" );
       Assert.assertEquals( "0.9.2342.19200300.100.1.1=00123456789+2.5.4.3=pablo picasso,2.5.4.11=search,2.5.4.10=imc,2.5.4.6=us", name.toString() );
       Assert.assertEquals( "0.9.2342.19200300.100.1.1=00123456789+2.5.4.3=pablo picasso,2.5.4.11=search,2.5.4.10=imc,2.5.4.6=us", name.getUpName() );
   }
}
