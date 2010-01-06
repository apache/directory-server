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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.List;

import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.BasicAttribute;
import javax.naming.directory.BasicAttributes;

import org.apache.directory.shared.ldap.entry.Entry;
import org.apache.directory.shared.ldap.entry.EntryAttribute;
import org.apache.directory.shared.ldap.entry.client.DefaultClientAttribute;
import org.apache.directory.shared.ldap.entry.client.DefaultClientEntry;
import org.apache.directory.shared.ldap.name.LdapDN;
import org.apache.directory.shared.ldap.name.Rdn;
import org.apache.directory.shared.ldap.util.StringTools;
import org.junit.Test;


/**
 * Tests the LdifUtils methods
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class LdifUtilsTest
{
    private String testString = "this is a test";
    

    /**
     * Tests the method IsLdifSafe with a null String
     */
    @Test
    public void testIsLdifNullString()
    {
        assertTrue( LdifUtils.isLDIFSafe( null ) );
    }
    

    /**
     * Tests the method IsLdifSafe with an empty String
     */
    @Test
    public void testIsLdifEmptyString()
    {
        assertTrue( LdifUtils.isLDIFSafe( "" ) );
    }
    

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
     * Test a conversion of an entry from a LDIF file
     */
    @Test
    public void testConvertToLdif() throws NamingException
    {
        LdifEntry entry = new LdifEntry();
        entry.setDn( "cn=Saarbr\u00FCcken, dc=example, dc=com" );
        entry.setChangeType( ChangeType.Add );
        
        EntryAttribute oc = new DefaultClientAttribute( "objectClass" );
        oc.add( "top", "person", "inetorgPerson" );
        
        entry.addAttribute( oc );
        
        entry.addAttribute( "cn", "Saarbr\u00FCcken" );
        entry.addAttribute( "sn", "test" );

        LdifUtils.convertToLdif( entry, 15 );
        //Attributes result = LdifUtils.convertFromLdif( ldif );
        //assertEquals( entry, result );
    }
    
    
    /**
     * Test a conversion of an attributes from a LDIF file
     */
    @Test
    public void testConvertAttributesfromLdif() throws NamingException
    {
        Attributes attributes = new BasicAttributes( true );
        
        Attribute oc = new BasicAttribute( "objectclass" );
        oc.add( "top" );
        oc.add( "person" );
        oc.add( "inetorgPerson" );
        
        attributes.put( oc );
        
        attributes.put( "cn", "Saarbrucken" );
        attributes.put( "sn", "test" );

        String ldif = LdifUtils.convertToLdif( attributes, (LdapDN)null, 15 );
        Attributes result = LdifUtils.convertAttributesFromLdif( ldif );
        assertEquals( attributes, result );
    }

    
    /**
     * Check that the correct reverse LDIF is produced for a modifyDn
     * operation that moves and renames the entry while preserving the
     * old rdn.
     *
     * @throws NamingException on error
     */
    @Test
    public void testReverseModifyDNSuperior() throws NamingException
    {
        LdapDN dn = new LdapDN( "cn=john doe, dc=example, dc=com" );
        LdapDN newSuperior = new LdapDN( "ou=system" );

        Entry entry = new DefaultClientEntry( dn );
        entry.add( "objectClass", "person", "uidObject" );
        entry.add( "cn", "john doe", "jack doe" );
        entry.add( "sn", "doe" );
        entry.add( "uid", "jdoe" );

        List<LdifEntry> reverseds = LdifRevertor.reverseMoveAndRename( entry, newSuperior, new Rdn( "cn=jack doe" ), false );

        assertNotNull( reverseds );
        assertEquals( 1, reverseds.size() );
        
        LdifEntry reversed = reverseds.get( 0 );
        assertEquals( "cn=jack doe,ou=system", reversed.getDn().getName() );
        assertEquals( ChangeType.ModRdn, reversed.getChangeType() );
        assertFalse( reversed.isDeleteOldRdn() );
        assertEquals( "cn=john doe", reversed.getNewRdn() );
        assertEquals( "dc=example, dc=com", StringTools.trim( reversed.getNewSuperior() ) );
        assertNull( reversed.getEntry() );
    }


    /**
     * Test a reversed ModifyDN with a deleteOldRdn, rdn change, and a superior
     *
     * @throws NamingException on error
     */
    @Test
    public void testReverseModifyDNDeleteOldRdnSuperior() throws NamingException
    {
        LdapDN dn = new LdapDN( "cn=john doe, dc=example, dc=com" );
        LdapDN newSuperior = new LdapDN( "ou=system" );

        Entry entry = new DefaultClientEntry( dn );
        entry.add( "objectClass", "person", "uidObject" );
        entry.add( "cn", "john doe" );
        entry.add( "sn", "doe" );
        entry.add( "uid", "jdoe" );

        List<LdifEntry> reverseds = LdifRevertor.reverseMoveAndRename( entry, newSuperior, new Rdn( "cn=jack doe" ), false );

        assertNotNull( reverseds );
        assertEquals( 1, reverseds.size() );
        
        LdifEntry reversed = reverseds.get( 0 );
        assertEquals( "cn=jack doe,ou=system", reversed.getDn().getName() );
        assertEquals( ChangeType.ModRdn, reversed.getChangeType() );
        assertTrue( reversed.isDeleteOldRdn() );
        assertEquals( "cn=john doe", reversed.getNewRdn() );
        assertEquals( "dc=example, dc=com", StringTools.trim( reversed.getNewSuperior() ) );
        assertNull( reversed.getEntry() );
    }
}
