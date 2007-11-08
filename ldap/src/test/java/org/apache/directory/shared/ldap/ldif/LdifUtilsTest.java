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
package org.apache.directory.shared.ldap.ldif;

import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.BasicAttribute;
import javax.naming.directory.BasicAttributes;

import org.apache.directory.shared.ldap.message.AddRequest;
import org.apache.directory.shared.ldap.message.AddRequestImpl;
import org.apache.directory.shared.ldap.message.AttributeImpl;
import org.apache.directory.shared.ldap.message.AttributesImpl;
import org.apache.directory.shared.ldap.message.ModifyDnRequest;
import org.apache.directory.shared.ldap.message.ModifyDnRequestImpl;
import org.apache.directory.shared.ldap.name.LdapDN;
import org.apache.directory.shared.ldap.name.Rdn;
import org.junit.Test;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;


/**
 * Tests the LdifUtils methods
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class LdifUtilsTest
{
	private String testString = "this is a test";
	
	/**
	 * Tests the method IsLdifSafe with a String starting with the
	 * char NUL (ASCII code 0)
	 */
	@Test
	public void testIsLdifSafeStartingWithNUL()
    {
		char c = ( char ) 0;
		
		assertFalse( LdifUtils.isLDIFSafe( c + testString ) );
    }
	
	/**
	 * Tests the method IsLdifSafe with a String starting with the
	 * char LF (ASCII code 10)
	 */
    @Test
	public void testIsLdifSafeStartingWithLF()
    {
		char c = ( char ) 10;
		
		assertFalse( LdifUtils.isLDIFSafe( c + testString ) );
    }

	/**
	 * Tests the method IsLdifSafe with a String starting with the
	 * char CR (ASCII code 13)
	 */
    @Test
	public void testIsLdifSafeStartingWithCR()
    {
		char c = ( char ) 13;
		
		assertFalse( LdifUtils.isLDIFSafe( c + testString ) );
    }

	/**
	 * Tests the method IsLdifSafe with a String starting with the
	 * char SPACE (ASCII code 32)
	 */
    @Test
	public void testIsLdifSafeStartingWithSpace()
    {
		char c = ( char ) 32;
		
		assertFalse( LdifUtils.isLDIFSafe( c + testString ) );
    }
	
	/**
	 * Tests the method IsLdifSafe with a String starting with the
	 * char COLON (:) (ASCII code 58)
	 */
    @Test
	public void testIsLdifSafeStartingWithColon()
    {
		char c = ( char ) 58;
		
		assertFalse( LdifUtils.isLDIFSafe( c + testString ) );
    }
	
	/**
	 * Tests the method IsLdifSafe with a String starting with the
	 * char LESS_THAN (<) (ASCII code 60)
	 */
    @Test
	public void testIsLdifSafeStartingWithLessThan()
    {
		char c = ( char ) 60;
		
		assertFalse( LdifUtils.isLDIFSafe( c + testString ) );
    }
	
	/**
	 * Tests the method IsLdifSafe with a String starting with the
	 * char with ASCII code 127
	 */
    @Test
	public void testIsLdifSafeStartingWithCharGreaterThan127()
    {
		char c = ( char ) 127;
		
		assertTrue( LdifUtils.isLDIFSafe( c + testString ) );
    }
	
	/**
	 * Tests the method IsLdifSafe with a String starting with the
	 * char with ASCII code greater than 127
	 */
    @Test
	public void testIsLdifSafeStartingWithCharGreaterThan127Bis()
    {
		char c = ( char ) 222;
		
		assertFalse( LdifUtils.isLDIFSafe( c + testString ) );
    }
	
	/**
	 * Tests the method IsLdifSafe with a String containing the
	 * char NUL (ASCII code 0)
	 */
    @Test
	public void testIsLdifSafeContainsNUL()
    {
		char c = ( char ) 0;
		
		assertFalse( LdifUtils.isLDIFSafe( testString + c + testString ) );
    }
	
	/**
	 * Tests the method IsLdifSafe with a String containing the
	 * char LF (ASCII code 10)
	 */
    @Test
	public void testIsLdifSafeContainsLF()
    {
		char c = ( char ) 10;
		
		assertFalse( LdifUtils.isLDIFSafe( testString + c + testString ) );
    }
	
	/**
	 * Tests the method IsLdifSafe with a String containing the
	 * char CR (ASCII code 13)
	 */
    @Test
	public void testIsLdifSafeContainsCR()
    {
		char c = ( char ) 13;
		
		assertFalse( LdifUtils.isLDIFSafe( testString + c + testString ) );
    }
	
	/**
	 * Tests the method IsLdifSafe with a String containing the
	 * char with ASCII code 127
	 */
    @Test
	public void testIsLdifSafeContainsCharGreaterThan127()
    {
		char c = ( char ) 127;
		
		assertTrue( LdifUtils.isLDIFSafe( testString + c + testString ) );
    }
	
	/**
	 * Tests the method IsLdifSafe with a String containing a
	 * char with ASCII code greater than 127
	 */
    @Test
	public void testIsLdifSafeContainsCharGreaterThan127Bis()
    {
		char c = ( char ) 328;
		
		assertFalse( LdifUtils.isLDIFSafe( testString + c + testString ) );
    }
	
	/**
	 * Tests the method IsLdifSafe with a String ending with the
	 * char SPACE (ASCII code 32)
	 */
    @Test
	public void testIsLdifSafeEndingWithSpace()
    {
		char c = ( char ) 32;
		
		assertFalse( LdifUtils.isLDIFSafe( testString  + c) );
    }
	
	/**
	 * Tests the method IsLdifSafe with a correct String
	 */
    @Test
	public void testIsLdifSafeCorrectString()
    {		
		assertTrue( LdifUtils.isLDIFSafe( testString ) );
    }
    
    
    /**
     * Test the way LDIF lines are stripped to a number of chars
     */
    @Test
    public void testStripLineToNChars()
    {
        String line = "abc";
        
        try
        {
            LdifUtils.stripLineToNChars( line, 1 );
            fail();
        }
        catch ( IllegalArgumentException iae )
        {
            // This is correct
        }
        
        String res = LdifUtils.stripLineToNChars( line, 2 );
        assertEquals( "ab\n c", res );
        assertEquals( "abc", LdifUtils.stripLineToNChars( line, 3 ) );
    }

    /**
     * Test that the LDIF is stripped to 5 chars per line
     *
     */
    @Test
    public void testStripLineTo5Chars()
    {
        assertEquals( "a", LdifUtils.stripLineToNChars( "a", 5 ) );
        assertEquals( "ab", LdifUtils.stripLineToNChars( "ab", 5 ) );
        assertEquals( "abc", LdifUtils.stripLineToNChars( "abc", 5 ) );
        assertEquals( "abcd", LdifUtils.stripLineToNChars( "abcd", 5 ) );
        assertEquals( "abcde", LdifUtils.stripLineToNChars( "abcde", 5 ) );
        assertEquals( "abcde\n f", LdifUtils.stripLineToNChars( "abcdef", 5 ) );
        assertEquals( "abcde\n fg", LdifUtils.stripLineToNChars( "abcdefg", 5 ) );
        assertEquals( "abcde\n fgh", LdifUtils.stripLineToNChars( "abcdefgh", 5 ) );
        assertEquals( "abcde\n fghi", LdifUtils.stripLineToNChars( "abcdefghi", 5 ) );
        assertEquals( "abcde\n fghi\n j", LdifUtils.stripLineToNChars( "abcdefghij", 5 ) );
        assertEquals( "abcde\n fghi\n jk", LdifUtils.stripLineToNChars( "abcdefghijk", 5 ) );
        assertEquals( "abcde\n fghi\n jkl", LdifUtils.stripLineToNChars( "abcdefghijkl", 5 ) );
        assertEquals( "abcde\n fghi\n jklm", LdifUtils.stripLineToNChars( "abcdefghijklm", 5 ) );
        assertEquals( "abcde\n fghi\n jklm\n n", LdifUtils.stripLineToNChars( "abcdefghijklmn", 5 ) );
        assertEquals( "abcde\n fghi\n jklm\n no", LdifUtils.stripLineToNChars( "abcdefghijklmno", 5 ) );
        assertEquals( "abcde\n fghi\n jklm\n nop", LdifUtils.stripLineToNChars( "abcdefghijklmnop", 5 ) );
    }
    
    
    /**
     * Tests that unsafe characters are encoded using UTF-8 charset. 
     * 
     * @throws NamingException
     */
    @Test
    public void testConvertToLdifEncoding() throws NamingException
    {
        Attributes attributes = new BasicAttributes( "cn", "Saarbr\u00FCcken" );
        String ldif = LdifUtils.convertToLdif( attributes );
        assertEquals( "cn:: U2FhcmJyw7xja2Vu\n", ldif );
    }
    
    
    /**
     * Tests that null values are correctly encoded 
     * 
     * @throws NamingException
     */
    @Test
    public void testConvertToLdifAttrWithNullValues() throws NamingException
    {
        Attributes attributes = new BasicAttributes( "cn", null );
        String ldif = LdifUtils.convertToLdif( attributes );
        assertEquals( "cn:\n", ldif );
    }
    
    
    /**
     * Test a conversion of an entry to a LDIF file
     */
    @Test
    public void testConvertEntryToLdif() throws NamingException
    {
        String expected = 
            "dn:: Y249U2Fhcm\n" +
            " Jyw7xja2VuLCBk\n" +
            " Yz1leGFtcGxlLC\n" +
            " BkYz1jb20=\n" +
            "changeType: Add\n" +
            "sn: test\n" +
            "cn:: U2FhcmJyw7\n xja2Vu\n" +
            "objectClass: to\n p\n" +
            "objectClass: pe\n rson\n" +
            "objectClass: in\n etorgPerson\n\n";
        
        Entry entry = new Entry();
        entry.setDn( "cn=Saarbr\u00FCcken, dc=example, dc=com" );
        entry.setChangeType( ChangeType.Add );
        
        Attribute oc = new BasicAttribute( "objectClass" );
        oc.add( "top" );
        oc.add( "person" );
        oc.add( "inetorgPerson" );
        
        entry.addAttribute( oc );
        
        entry.addAttribute( "cn", "Saarbr\u00FCcken" );
        entry.addAttribute( "sn", "test" );

        String ldif = LdifUtils.convertToLdif( entry, 15 );
        assertEquals( expected, ldif );
    }
    
    
    /**
     * Test a AddRequest reverse
     *
     * @throws NamingException
     */
    @Test
    public void testReverseAdd() throws NamingException
    {
        AddRequest addRequest = new AddRequestImpl( 1 );
        
        LdapDN dn = new LdapDN( "dc=apache, dc=com" );
        
        addRequest.setEntry( dn );
        
        Attributes attributes = new AttributesImpl( "dc", "apache" );
        addRequest.setAttributes( attributes );
        
        Entry reversed = LdifUtils.reverseAdd( addRequest );
        
        assertNotNull( reversed );
        assertEquals( dn.getUpName(), reversed.getDn() );
        assertEquals( ChangeType.Delete, reversed.getChangeType() );
        assertNull( reversed.getAttributes() );
    }


    /**
     * Test a AddRequest reverse where the DN is to be base64 encoded 
     *
     * @throws NamingException
     */
    @Test
    public void testReverseAddBase64DN() throws NamingException
    {
        AddRequest addRequest = new AddRequestImpl( 1 );
        
        LdapDN dn = new LdapDN( "dc=Emmanuel L\u00c9charny" );
        
        addRequest.setEntry( dn );
        
        Attributes attributes = new AttributesImpl( "dc", "test" );
        addRequest.setAttributes( attributes );
        
        Entry reversed = LdifUtils.reverseAdd( addRequest );
        
        assertNotNull( reversed );
        assertEquals( dn.getUpName(), reversed.getDn() );
        assertEquals( ChangeType.Delete, reversed.getChangeType() );
        assertNull( reversed.getAttributes() );
    }


    /**
     * Test a DelRequest reverse
     *
     * @throws NamingException
     */
    @Test
    public void testReverseDel() throws NamingException
    {
        LdapDN dn = new LdapDN( "dc=apache, dc=com" );
        
        Attributes deletedEntry = new AttributesImpl();
        
        Attribute oc = new AttributeImpl( "objectClass" );
        oc.add( "top" );
        oc.add( "person" );
        
        deletedEntry.put( oc );
        
        deletedEntry.put( "cn", "test" );
        deletedEntry.put( "sn", "apache" );
        deletedEntry.put( "dc", "apache" );
        
        Entry reversed = LdifUtils.reverseDel( dn, deletedEntry );
        
        assertNotNull( reversed );
        assertEquals( dn.getUpName(), reversed.getDn() );
        assertEquals( ChangeType.Add, reversed.getChangeType() );
        assertNotNull( reversed.getAttributes() );
        assertEquals( deletedEntry, reversed.getAttributes() );
    }
    
    
    /**
     * Test a reversed ModifyDN with no deleteOldRdn and no superior
     */
    @Test
    public void testReverseModifyDNNoDeleteOldRdnNoSuperior() throws NamingException
    {
        LdapDN dnModified = new LdapDN( "cn=joe, dc=example, dc=com" );
        
        ModifyDnRequest modifyDn = new ModifyDnRequestImpl( 1 );
        
        LdapDN dn = new LdapDN( "cn=test, dc=example, dc=com" );

        modifyDn.setName( dn );
        modifyDn.setNewRdn( new Rdn( "cn=joe" ) );
        
        Entry reversed = LdifUtils.reverseModifyDN( modifyDn );

        assertNotNull( reversed );
        assertEquals( dnModified.getUpName(), reversed.getDn() );
        assertEquals( ChangeType.ModDn, reversed.getChangeType() );
        assertNull( reversed.getAttributes() );
        assertTrue( reversed.isDeleteOldRdn() );
        assertEquals( new Rdn( "cn=test" ).getUpName(), reversed.getNewRdn() );
        assertNull( reversed.getNewSuperior() );
    }


    /**
     * Test a reversed ModifyDN with a deleteOldRdn and no superior
     */
    @Test
    public void testReverseModifyDNDeleteOldRdnNoSuperior() throws NamingException
    {
        LdapDN dnModified = new LdapDN( "cn=joe, dc=example, dc=com" );
        
        ModifyDnRequest modifyDn = new ModifyDnRequestImpl( 1 );
        
        LdapDN dn = new LdapDN( "cn=test, dc=example, dc=com" );

        modifyDn.setName( dn );
        modifyDn.setNewRdn( new Rdn( "cn=joe" ) );
        modifyDn.setDeleteOldRdn( true );
        
        Entry reversed = LdifUtils.reverseModifyDN( modifyDn );

        assertNotNull( reversed );
        assertEquals( dnModified.getUpName(), reversed.getDn() );
        assertEquals( ChangeType.ModDn, reversed.getChangeType() );
        assertNull( reversed.getAttributes() );
        assertTrue( reversed.isDeleteOldRdn() );
        assertEquals( new Rdn( "cn=test" ).getUpName(), reversed.getNewRdn() );
        assertNull( reversed.getNewSuperior() );
    }


    /**
     * Test a reversed ModifyDN with no deleteOldRdn and a superior
     */
    @Test
    public void testReverseModifyDNNoDeleteOldRdnSuperior() throws NamingException
    {
        LdapDN dnModified = new LdapDN( "cn=joe,ou=system" );
        
        ModifyDnRequest modifyDn = new ModifyDnRequestImpl( 1 );
        
        LdapDN dn = new LdapDN( "cn=test, dc=example, dc=com" );

        modifyDn.setName( dn );
        modifyDn.setNewRdn( new Rdn( "cn=joe" ) );
        modifyDn.setNewSuperior( new LdapDN( "ou=system" ) );
        modifyDn.setDeleteOldRdn( false );
        
        Entry reversed = LdifUtils.reverseModifyDN( modifyDn );

        assertNotNull( reversed );
        assertEquals( dnModified.getUpName(), reversed.getDn() );
        assertEquals( ChangeType.ModDn, reversed.getChangeType() );
        assertNull( reversed.getAttributes() );
        assertTrue( reversed.isDeleteOldRdn() );
        assertEquals( new Rdn( "cn=test" ).getUpName(), reversed.getNewRdn() );
        assertNotNull( reversed.getNewSuperior() );
        assertEquals( new LdapDN( "dc=example, dc=com" ).getUpName(), reversed.getNewSuperior() );
    }


    /**
     * Test a reversed ModifyDN with a deleteOldRdn and a superior
     */
    @Test
    public void testReverseModifyDNDeleteOldRdnSuperior() throws NamingException
    {
        LdapDN dnModified = new LdapDN( "cn=joe,ou=system" );
        
        ModifyDnRequest modifyDn = new ModifyDnRequestImpl( 1 );
        
        LdapDN dn = new LdapDN( "cn=test, dc=example, dc=com" );

        modifyDn.setName( dn );
        modifyDn.setNewRdn( new Rdn( "cn=joe" ) );
        modifyDn.setNewSuperior( new LdapDN( "ou=system" ) );
        modifyDn.setDeleteOldRdn( true );
        
        Entry reversed = LdifUtils.reverseModifyDN( modifyDn );

        assertNotNull( reversed );
        assertEquals( dnModified.getUpName(), reversed.getDn() );
        assertEquals( ChangeType.ModDn, reversed.getChangeType() );
        assertNull( reversed.getAttributes() );
        assertTrue( reversed.isDeleteOldRdn() );
        assertEquals( new Rdn( "cn=test" ).getUpName(), reversed.getNewRdn() );
        assertNotNull( reversed.getNewSuperior() );
        assertEquals( new LdapDN( "dc=example, dc=com" ).getUpName(), reversed.getNewSuperior() );
    }
}
