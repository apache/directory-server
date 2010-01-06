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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.naming.NamingException;
import javax.naming.directory.Attributes;
import javax.naming.directory.BasicAttributes;

import org.apache.directory.shared.ldap.entry.Entry;
import org.apache.directory.shared.ldap.entry.EntryAttribute;
import org.apache.directory.shared.ldap.entry.Modification;
import org.apache.directory.shared.ldap.entry.ModificationOperation;
import org.apache.directory.shared.ldap.entry.client.ClientModification;
import org.apache.directory.shared.ldap.entry.client.DefaultClientAttribute;
import org.apache.directory.shared.ldap.entry.client.DefaultClientEntry;
import org.apache.directory.shared.ldap.name.LdapDN;
import org.apache.directory.shared.ldap.name.Rdn;
import org.apache.directory.shared.ldap.util.StringTools;
import org.junit.Test;

/**
 * Tests the LdifReverter methods
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class LdifRevertorTest
{
    /**
     * Helper method to build a basic entry used by the Modify tests
     */
    private Entry buildEntry()
    {
        Entry entry = new DefaultClientEntry();
        entry.put( "objectclass", "top", "person" );
        entry.put( "cn", "test" );
        entry.put( "sn", "joe doe" );
        entry.put( "l", "USA" );
        
        return entry;
    }

    
    /**
     * Test a AddRequest reverse
     *
     * @throws NamingException
     */
    @Test
    public void testReverseAdd() throws NamingException
    {
        LdapDN dn = new LdapDN( "dc=apache, dc=com" );
        LdifEntry reversed = LdifRevertor.reverseAdd( dn );
        
        assertNotNull( reversed );
        assertEquals( dn.getName(), reversed.getDn().getName() );
        assertEquals( ChangeType.Delete, reversed.getChangeType() );
        assertNull( reversed.getEntry() );
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
        
        Entry deletedEntry = new DefaultClientEntry( dn );
        
        EntryAttribute oc = new DefaultClientAttribute( "objectClass" );
        oc.add( "top", "person" );
        
        deletedEntry.put( oc );
        
        deletedEntry.put( "cn", "test" );
        deletedEntry.put( "sn", "apache" );
        deletedEntry.put( "dc", "apache" );
        
        LdifEntry reversed = LdifRevertor.reverseDel( dn, deletedEntry );
        
        assertNotNull( reversed );
        assertEquals( dn.getName(), reversed.getDn().getName() );
        assertEquals( ChangeType.Add, reversed.getChangeType() );
        assertNotNull( reversed.getEntry() );
        assertEquals( deletedEntry, reversed.getEntry() );
    }
    
    
    /**
     * Test a reversed Modify adding a existing value from an existing attribute
     */
    @Test
    public void testReverseModifyDelExistingOuValue() throws NamingException
    {
        Entry modifiedEntry = buildEntry();
        
        EntryAttribute ou = new DefaultClientAttribute( "ou" );
        ou.add( "apache", "acme corp" );
        modifiedEntry.put( ou );

        LdapDN dn = new LdapDN( "cn=test, ou=system" );

        Modification mod = new ClientModification( 
            ModificationOperation.REMOVE_ATTRIBUTE, 
            new DefaultClientAttribute( "ou", "acme corp" ) );

        LdifEntry reversed = LdifRevertor.reverseModify( dn,
                Collections.<Modification>singletonList( mod ), modifiedEntry );

        assertNotNull( reversed );
        assertEquals( dn.getName(), reversed.getDn().getName() );
        assertEquals( ChangeType.Modify, reversed.getChangeType() );
        assertNull( reversed.getEntry() );
        
        List<Modification> mods = reversed.getModificationItems();
        
        assertNotNull( mods );
        assertEquals( 1, mods.size() );
        
        Modification modif = mods.get( 0 );
        
        assertEquals( ModificationOperation.ADD_ATTRIBUTE, modif.getOperation() );

        EntryAttribute attr = modif.getAttribute();
        
        assertNotNull( attr );

        assertEquals( "ou", attr.getId() );
        assertEquals( "acme corp", attr.getString() );
    }


    /**
     * Test a reversed Modify deleting an existing attribute
     */
    @Test
    public void testReverseModifyDeleteOU() throws NamingException
    {
        Entry modifiedEntry = buildEntry();
        
        EntryAttribute ou = new DefaultClientAttribute( "ou" );
        ou.add( "apache", "acme corp" );
        modifiedEntry.put( ou );

        LdapDN dn = new LdapDN( "cn=test, ou=system" );

        Modification mod = new ClientModification(
            ModificationOperation.REMOVE_ATTRIBUTE, 
            new DefaultClientAttribute( "ou" ) );

        LdifEntry reversed = LdifRevertor.reverseModify( dn,
                Collections.<Modification>singletonList( mod ), modifiedEntry );


        assertNotNull( reversed );
        assertEquals( dn.getName(), reversed.getDn().getName() );
        assertEquals( ChangeType.Modify, reversed.getChangeType() );
        assertNull( reversed.getEntry() );
        
        List<Modification> mods = reversed.getModificationItems();
        
        assertNotNull( mods );
        assertEquals( 1, mods.size() );
        
        Modification modif = mods.get( 0 );
        
        assertEquals( ModificationOperation.ADD_ATTRIBUTE, modif.getOperation() );

        EntryAttribute attr = modif.getAttribute();
        
        assertNotNull( attr );
        assertEquals( "ou", attr.getId() );
        
        assertEquals( ou, attr );
    }

   
    /**
     * Test a reversed Modify deleting all values of an existing attribute
     */
    @Test
    public void testReverseModifyDelExistingOuWithAllValues() throws NamingException
    {
        Entry modifiedEntry = buildEntry();

        EntryAttribute ou = new DefaultClientAttribute( "ou", "apache", "acme corp" );
        modifiedEntry.put( ou );
        
        LdapDN dn = new LdapDN( "cn=test, ou=system" );
        
        Modification mod = new ClientModification(
            ModificationOperation.REMOVE_ATTRIBUTE, ou );

        LdifEntry reversed = LdifRevertor.reverseModify( dn, 
                Collections.<Modification>singletonList( mod ), modifiedEntry );


        assertNotNull( reversed );
        assertEquals( dn.getName(), reversed.getDn().getName() );
        assertEquals( ChangeType.Modify, reversed.getChangeType() );
        assertNull( reversed.getEntry() );
        
        List<Modification> mods = reversed.getModificationItems();
        
        assertNotNull( mods );
        assertEquals( 1, mods.size() );
        
        Modification modif = mods.get( 0 );
        
        assertEquals( ModificationOperation.ADD_ATTRIBUTE, modif.getOperation() );

        EntryAttribute attr = modif.getAttribute();
        
        assertNotNull( attr );
        assertEquals( "ou", attr.getId() );
        
        assertEquals( ou, attr );
    }

    
    /**
     * Test a reversed Modify replacing existing values with new values
     */
    @Test
    public void testReverseModifyReplaceExistingOuValues() throws NamingException
    {
        Entry modifiedEntry = buildEntry();
        
        EntryAttribute ou = new DefaultClientAttribute( "ou" );
        ou.add( "apache", "acme corp" );
        modifiedEntry.put( ou );

        LdapDN dn = new LdapDN( "cn=test, ou=system" );

        EntryAttribute ouModified = new DefaultClientAttribute( "ou" );
        ouModified.add( "directory" );
        ouModified.add( "BigCompany inc." );
        
        Modification mod = new ClientModification(
            ModificationOperation.REPLACE_ATTRIBUTE, ouModified );

        LdifEntry reversed = LdifRevertor.reverseModify( dn,
                Collections.<Modification>singletonList( mod ), modifiedEntry );



        assertNotNull( reversed );
        assertEquals( dn.getName(), reversed.getDn().getName() );
        assertEquals( ChangeType.Modify, reversed.getChangeType() );
        assertNull( reversed.getEntry() );
        
        List<Modification> mods = reversed.getModificationItems();
        
        assertNotNull( mods );
        assertEquals( 1, mods.size() );
        
        Modification modif = mods.get( 0 );
        
        assertEquals( ModificationOperation.REPLACE_ATTRIBUTE, modif.getOperation() );

        EntryAttribute attr = modif.getAttribute();
        
        assertNotNull( attr );
        assertEquals( ou, attr );
    }


    /**
     * Test a reversed Modify replace by injecting a new attribute
     */
    @Test
    public void testReverseModifyReplaceNewAttribute() throws NamingException
    {
        Entry modifiedEntry = buildEntry();
        
        LdapDN dn = new LdapDN( "cn=test, ou=system" );
        
        EntryAttribute newOu = new DefaultClientAttribute( "ou" );
        newOu.add( "apache" );
        newOu.add( "acme corp" );

        
        Modification mod = new ClientModification(
            ModificationOperation.REPLACE_ATTRIBUTE, newOu );

        LdifEntry reversed = LdifRevertor.reverseModify( dn,
                Collections.<Modification>singletonList( mod ), modifiedEntry );

        assertNotNull( reversed );
        assertEquals( dn.getName(), reversed.getDn().getName() );
        assertEquals( ChangeType.Modify, reversed.getChangeType() );
        assertNull( reversed.getEntry() );
        
        List<Modification> mods = reversed.getModificationItems();
        
        assertNotNull( mods );
        assertEquals( 1, mods.size() );
        
        Modification modif = mods.get( 0 );
        
        assertEquals( ModificationOperation.REPLACE_ATTRIBUTE, modif.getOperation() );

        EntryAttribute attr = modif.getAttribute();
        
        assertNotNull( attr );
        assertEquals( "ou", attr.getId() );
        
        assertNull( attr.get() );
    }

   
    /**
     * Test a reversed Modify replace by removing an attribute
     */
    @Test
    public void testReverseModifyReplaceExistingOuWithNothing() throws NamingException
    {
        Entry modifiedEntry = buildEntry();

        EntryAttribute ou = new DefaultClientAttribute( "ou" );
        ou.add( "apache" );
        ou.add( "acme corp" );
        modifiedEntry.put( ou );
        
        LdapDN dn = new LdapDN( "cn=test, ou=system" );
        
        Modification mod = new ClientModification( 
            ModificationOperation.REPLACE_ATTRIBUTE, new DefaultClientAttribute( "ou" ) );

        LdifEntry reversed = LdifRevertor.reverseModify( dn,
                Collections.<Modification>singletonList( mod ), modifiedEntry );

        assertNotNull( reversed );
        assertEquals( dn.getName(), reversed.getDn().getName() );
        assertEquals( ChangeType.Modify, reversed.getChangeType() );
        assertNull( reversed.getEntry() );
        
        List<Modification> mods = reversed.getModificationItems();
        
        assertNotNull( mods );
        assertEquals( 1, mods.size() );
        
        Modification modif = mods.get( 0 );
        
        assertEquals( ModificationOperation.REPLACE_ATTRIBUTE, modif.getOperation() );

        EntryAttribute attr = modif.getAttribute();
        
        assertNotNull( attr );
        assertEquals( "ou", attr.getId() );
        
        assertEquals( ou, attr );
    }
    
    
    /**
     * Test a multiple modifications reverse.
     * 
     * On the following entry :
     *  dn: cn=test, ou=system
     *  objectclass: top
     *  objectclass: person
     *  cn: test
     *  sn: joe doe
     *  l: USA
     *  ou: apache
     *  ou: acme corp
     * 
     * We will :
     *  - add an 'ou' value 'BigCompany inc.'
     *  - delete the 'l' attribute
     *  - add the 'l=FR' attribute
     *  - replace the 'l=FR' by a 'l=USA' attribute
     *  - replace the 'ou' attribute with 'apache' value.
     *  
     * The modify ldif will be :
     * 
     *  dn: cn=test, ou=system
     *  changetype: modify
     *  add: ou
     *  ou: BigCompany inc.
     *  -
     *  delete: l
     *  -
     *  add: l
     *  l: FR
     *  -
     *  replace: l
     *  l: USA
     *  -
     *  replace: ou
     *  ou: apache
     *  -
     *  
     * At the end, the entry will looks like :
     *  dn: cn=test, ou=system
     *  objectclass: top
     *  objectclass: person
     *  cn: test
     *  sn: joe doe
     *  l: USA
     *  ou: apache
     *  
     * and the reversed LDIF will be :
     * 
     *  dn: cn=test, ou=system
     *  changetype: modify
     *  replace: ou
     *  ou: apache
     *  ou: acme corp
     *  -
     *  replace: l
     *  l: USA
     *  -
     *  delete: l
     *  l: FR
     *  -
     *  add: l
     *  l: USA
     *  -
     *  delete: ou 
     *  ou: BigCompany inc.
     *  -
     * 
     */
    @Test
    public void testReverseMultipleModifications() throws Exception
    {
        String initialEntryLdif = 
                "dn: cn=test, ou=system\n" + 
                "objectclass: top\n" + 
                "objectclass: person\n" + 
                "cn: test\n" + 
                "sn: joe doe\n" + 
                "l: USA\n" + 
                "ou: apache\n" + 
                "ou: acme corp\n"; 
        
        LdifReader reader = new LdifReader();
        List<LdifEntry> entries = reader.parseLdif( initialEntryLdif );
        reader.close();
        
        LdifEntry initialEntry = entries.get( 0 );
 
        // We will :
        //   - add an 'ou' value 'BigCompany inc.'
        //   - delete the 'l' attribute
        //   - add the 'l=FR' attribute
        //   - replace the 'l=FR' by a 'l=USA' attribute
        //   - replace the 'ou' attribute with 'apache' value.
        LdapDN dn = new LdapDN( "cn=test, ou=system" );
        
        List<Modification> modifications = new ArrayList<Modification>();

        // First, inject the 'ou'
        
        Modification mod = new ClientModification( 
            ModificationOperation.ADD_ATTRIBUTE, new DefaultClientAttribute( "ou", "BigCompany inc." ) );
        modifications.add( mod );

        // Remove the 'l'
        mod = new ClientModification(
            ModificationOperation.REMOVE_ATTRIBUTE, new DefaultClientAttribute( "l" ) );
        modifications.add( mod );
        
        // Add 'l=FR'
        mod = new ClientModification( 
            ModificationOperation.ADD_ATTRIBUTE, new DefaultClientAttribute( "l", "FR" ) );
        modifications.add( mod );

        // Replace it with 'l=USA'
        mod = new ClientModification( 
            ModificationOperation.REPLACE_ATTRIBUTE, new DefaultClientAttribute( "l", "USA" ) );
        modifications.add( mod );

        // Replace the ou value
        mod = new ClientModification( 
            ModificationOperation.REPLACE_ATTRIBUTE, new DefaultClientAttribute( "ou", "apache" ) );
        modifications.add( mod );
        
        LdifEntry reversedEntry = LdifRevertor.reverseModify( dn, modifications, initialEntry.getEntry() );

        String expectedEntryLdif = 
            "dn: cn=test, ou=system\n" +
            "changetype: modify\n" +
            "replace: ou\n" +
            "ou: apache\n" +
            "ou: acme corp\n" +
            "ou: BigCompany inc.\n" +
            "-\n" +
            "replace: l\n" +
            "l: FR\n" +
            "-\n" +
            "delete: l\n" +
            "l: FR\n" +
            "-\n" +
            "add: l\n" +
            "l: USA\n" +
            "-\n" +
            "delete: ou\n" + 
            "ou: BigCompany inc.\n" +
            "-\n\n";
    
        reader = new LdifReader();
        entries = reader.parseLdif( expectedEntryLdif );
        reader.close();

        LdifEntry expectedEntry = entries.get( 0 );
        
        assertEquals( expectedEntry, reversedEntry );
    }


    /**
     * Test a reversed Modify adding a new attribute value
     * in an exiting attribute
     */
    @Test
    public void testReverseModifyAddNewOuValue() throws NamingException
    {
        Entry modifiedEntry = buildEntry();

        EntryAttribute ou = new DefaultClientAttribute( "ou" );
        ou.add( "apache" );
        ou.add( "acme corp" );
        modifiedEntry.put( ou );
        
        LdapDN dn = new LdapDN( "cn=test, ou=system" );
        Modification mod = new ClientModification(
            ModificationOperation.ADD_ATTRIBUTE, 
            new DefaultClientAttribute( "ou", "BigCompany inc." ) );

        LdifEntry reversed = LdifRevertor.reverseModify( dn,
                Collections.<Modification>singletonList( mod ), modifiedEntry );

        assertNotNull( reversed );
        assertEquals( dn.getName(), reversed.getDn().getName() );
        assertEquals( ChangeType.Modify, reversed.getChangeType() );
        assertNull( reversed.getEntry() );
        List<Modification> mods = reversed.getModificationItems();
        
        assertNotNull( mods );
        assertEquals( 1, mods.size() );
        
        Modification modif = mods.get( 0 );
        
        assertEquals( ModificationOperation.REMOVE_ATTRIBUTE, modif.getOperation() );

        EntryAttribute attr = modif.getAttribute();
        
        assertNotNull( attr );
        assertEquals( "ou", attr.getId() );
        assertEquals( "BigCompany inc.", attr.getString() );
    }


    /**
     * Test a reversed Modify adding a new attribute
     */
    @Test
    public void testReverseModifyAddNewOu() throws NamingException
    {
        Entry modifiedEntry = buildEntry();
        
        LdapDN dn = new LdapDN( "cn=test, ou=system" );
        Modification mod = new ClientModification(
            ModificationOperation.ADD_ATTRIBUTE, 
            new DefaultClientAttribute( "ou", "BigCompany inc." ) );

        LdifEntry reversed = LdifRevertor.reverseModify( dn,
                Collections.<Modification>singletonList( mod ), modifiedEntry );

        assertNotNull( reversed );
        assertEquals( dn.getName(), reversed.getDn().getName() );
        assertEquals( ChangeType.Modify, reversed.getChangeType() );
        assertNull( reversed.getEntry() );
        List<Modification> mods = reversed.getModificationItems();
        
        assertNotNull( mods );
        assertEquals( 1, mods.size() );
        
        Modification modif = mods.get( 0 );

        assertEquals( ModificationOperation.REMOVE_ATTRIBUTE, modif.getOperation() );

        EntryAttribute attr = modif.getAttribute();
        
        assertNotNull( attr );
        assertEquals( "ou", attr.getId() );
        assertEquals( "BigCompany inc.", attr.getString() );
    }
    
    
    /**
     * Test a AddRequest reverse where the DN is to be base64 encoded 
     *
     * @throws NamingException
     */
    @Test
    public void testReverseAddBase64DN() throws NamingException
    {
        LdapDN dn = new LdapDN( "dc=Emmanuel L\u00c9charny" );
        LdifEntry reversed = LdifRevertor.reverseAdd( dn );
        assertNotNull( reversed );
        assertEquals( dn.getName(), reversed.getDn().getName() );
        assertEquals( ChangeType.Delete, reversed.getChangeType() );
        assertNull( reversed.getEntry() );
    }


    /**
     * Test a reversed move ModifyDN
     *
     * @throws NamingException on error
     */
    @Test
    public void testReverseModifyDNMove() throws NamingException
    {
        LdapDN dn = new LdapDN( "cn=john doe, dc=example, dc=com" );
        LdapDN newSuperior = new LdapDN( "ou=system" );
        Rdn rdn = new Rdn( "cn=john doe" );

        Attributes attrs = new BasicAttributes( "objectClass", "person", true );
        attrs.get( "objectClass" ).add( "uidObject" );
        attrs.put( "cn", "john doe" );
        attrs.put( "cn", "jack doe" );
        attrs.put( "sn", "doe" );
        attrs.put( "uid", "jdoe" );

        LdifEntry reversed = LdifRevertor.reverseMove( newSuperior, dn );

        assertNotNull( reversed );
        
        assertEquals( "cn=john doe,ou=system", reversed.getDn().getName() );
        assertEquals( ChangeType.ModDn, reversed.getChangeType() );
        assertFalse( reversed.isDeleteOldRdn() );
        assertEquals( rdn.getUpName(), reversed.getNewRdn() );
        assertEquals( "dc=example, dc=com", StringTools.trim( reversed.getNewSuperior() ) );
        assertNull( reversed.getEntry() );
    }


    /**
     * Test a reversed rename ModifyDN, where the RDN are both simple, not overlapping,
     * with deleteOldRdn = false, and the AVA not present in the initial entry?
     * 
     * Covers case 1.1 of http://cwiki.apache.org/confluence/display/DIRxSRVx11/Reverse+LDIF
     * 
     * Initial entry
     * dn: cn=test,ou=system
     * objectclass: top
     * objectclass: person
     * cn: test
     * sn: This is a test 
     * 
     * new RDN : cn=joe
     *
     * @throws NamingException on error
     */
    @Test
    public void test11ReverseRenameSimpleSimpleNotOverlappingKeepOldRdnDontExistInEntry() throws NamingException
    {
        LdapDN dn = new LdapDN( "cn=test,ou=system" );
        Rdn oldRdn = new Rdn( "cn=test" );
        Rdn newRdn = new Rdn( "cn=joe" );

        Entry entry = new DefaultClientEntry( dn );
        entry.put( "cn", "test" );
        entry.put( "objectClass", "person", "top" );
        entry.put( "sn", "this is a test" );

        List<LdifEntry> reverseds = LdifRevertor.reverseRename( entry, newRdn, LdifRevertor.KEEP_OLD_RDN );

        assertNotNull( reverseds );
        assertEquals( 1, reverseds.size() );
        LdifEntry reversed = reverseds.get( 0 );
        
        assertEquals( "cn=joe,ou=system", reversed.getDn().getName() );
        assertEquals( ChangeType.ModRdn, reversed.getChangeType() );
        assertTrue( reversed.isDeleteOldRdn() );
        assertEquals( oldRdn.getUpName(), reversed.getNewRdn() );
        assertNull( reversed.getNewSuperior() );
    }


    /**
     * Test a reversed rename ModifyDN, where the RDN are both simple, not overlapping,
     * with deleteOldRdn = false, and with a AVA present in the initial entry.
     * 
     * Covers case 1.2 of http://cwiki.apache.org/confluence/display/DIRxSRVx11/Reverse+LDIF
     * 
     * Initial entry
     * dn: cn=test,ou=system
     * objectclass: top
     * objectclass: person
     * cn: test
     * cn: small
     * sn: This is a test 
     * 
     * new RDN : cn=small
     *
     * @throws NamingException on error
     */
    @Test
    public void test12ReverseRenameSimpleSimpleNotOverlappingKeepOldRdnExistInEntry() throws NamingException
    {
        LdapDN dn = new LdapDN( "cn=test,ou=system" );
        Rdn oldRdn = new Rdn( "cn=test" );
        Rdn newRdn = new Rdn( "cn=small" );

        Entry entry = new DefaultClientEntry( dn );
        entry.put( "cn", "test", "small" );
        entry.put( "objectClass", "person", "top" );
        entry.put( "sn", "this is a test" );

        List<LdifEntry> reverseds = LdifRevertor.reverseRename( entry, newRdn, LdifRevertor.KEEP_OLD_RDN );

        assertNotNull( reverseds );
        assertEquals( 1, reverseds.size() );
        LdifEntry reversed = reverseds.get( 0 );
        
        assertEquals( "cn=small,ou=system", reversed.getDn().getName() );
        assertEquals( ChangeType.ModRdn, reversed.getChangeType() );
        assertFalse( reversed.isDeleteOldRdn() );
        assertEquals( oldRdn.getUpName(), reversed.getNewRdn() );
        assertNull( reversed.getNewSuperior() );
    }


    /**
     * Test a reversed rename ModifyDN, where the RDN are both simple, not overlapping,
     * with deleteOldRdn = true, and the AVA not present in the initial entry
     * 
     * Covers case 2.1 of http://cwiki.apache.org/confluence/display/DIRxSRVx11/Reverse+LDIF
     * 
     * Initial entry
     * dn: cn=test,ou=system
     * objectclass: top
     * objectclass: person
     * cn: test
     * sn: This is a test 
     * 
     * new RDN : cn=joe
     *
     * @throws NamingException on error
     */
    @Test
    public void test21ReverseRenameSimpleSimpleNotOverlappingDeleteOldRdnDontExistInEntry() throws NamingException
    {
        LdapDN dn = new LdapDN( "cn=test,ou=system" );
        Rdn oldRdn = new Rdn( "cn=test" );
        Rdn newRdn = new Rdn( "cn=joe" );

        Entry entry = new DefaultClientEntry( dn );
        entry.put( "cn", "test" );
        entry.put( "objectClass", "person", "top" );
        entry.put( "sn", "this is a test" );

        List<LdifEntry> reverseds = LdifRevertor.reverseRename( entry, newRdn, LdifRevertor.DELETE_OLD_RDN );

        assertNotNull( reverseds );
        assertEquals( 1, reverseds.size() );
        LdifEntry reversed = reverseds.get( 0 );
        
        assertEquals( "cn=joe,ou=system", reversed.getDn().getName() );
        assertEquals( ChangeType.ModRdn, reversed.getChangeType() );
        assertTrue( reversed.isDeleteOldRdn() );
        assertEquals( oldRdn.getUpName(), reversed.getNewRdn() );
        assertNull( reversed.getNewSuperior() );
    }


    /**
     * Test a reversed rename ModifyDN, where the RDN are both simple, not overlapping,
     * with deleteOldRdn = true, and with a AVA present in the initial entry.
     * 
     * Covers case 2.2 of http://cwiki.apache.org/confluence/display/DIRxSRVx11/Reverse+LDIF
     * 
     * Initial entry
     * dn: cn=test,ou=system
     * objectclass: top
     * objectclass: person
     * cn: test
     * cn: small
     * sn: This is a test 
     * 
     * new RDN : cn=small
     *
     * @throws NamingException on error
     */
    @Test
    public void test22ReverseRenameSimpleSimpleNotOverlappingDeleteOldRdnExistInEntry() throws NamingException
    {
        LdapDN dn = new LdapDN( "cn=test,ou=system" );
        Rdn oldRdn = new Rdn( "cn=test" );
        Rdn newRdn = new Rdn( "cn=small" );

        Entry entry = new DefaultClientEntry( dn );
        entry.put( "cn", "test", "small" );
        entry.put( "objectClass", "person", "top" );
        entry.put( "sn", "this is a test" );

        List<LdifEntry> reverseds = LdifRevertor.reverseRename( entry, newRdn, LdifRevertor.DELETE_OLD_RDN );

        assertNotNull( reverseds );
        assertEquals( 1, reverseds.size() );
        LdifEntry reversed = reverseds.get( 0 );
        
        assertEquals( "cn=small,ou=system", reversed.getDn().getName() );
        assertEquals( ChangeType.ModRdn, reversed.getChangeType() );
        assertFalse( reversed.isDeleteOldRdn() );
        assertEquals( oldRdn.getUpName(), reversed.getNewRdn() );
        assertNull( reversed.getNewSuperior() );
    }


    /**
     * Test a reversed rename ModifyDN, where the initial RDN is composite, 
     * the new RDN is simple, not overlapping, with deleteOldRdn = false, and 
     * with a AVA not present in the initial entry.
     * 
     * Covers case 3 of http://cwiki.apache.org/confluence/display/DIRxSRVx11/Reverse+LDIF
     * 
     * Initial entry
     * dn: cn=small+cn=test,ou=system
     * objectclass: top
     * objectclass: person
     * cn: test
     * cn: small
     * sn: This is a test 
     * 
     * new RDN : cn=joe
     *
     * @throws NamingException on error
     */
    @Test
    public void test3ReverseRenameCompositeSimpleNotOverlappingKeepOldRdnDontExistInEntry() throws NamingException
    {
        LdapDN dn = new LdapDN( "cn=small+cn=test,ou=system" );
        Rdn oldRdn = new Rdn( "cn=small+cn=test" );
        Rdn newRdn = new Rdn( "cn=joe" );

        Entry entry = new DefaultClientEntry( dn );
        entry.put( "cn", "test", "small" );
        entry.put( "objectClass", "person", "top" );
        entry.put( "sn", "this is a test" );

        List<LdifEntry> reverseds = LdifRevertor.reverseRename( entry, newRdn, LdifRevertor.KEEP_OLD_RDN );

        assertNotNull( reverseds );
        assertEquals( 1, reverseds.size() );
        LdifEntry reversed = reverseds.get( 0 );
        
        assertEquals( "cn=joe,ou=system", reversed.getDn().getName() );
        assertEquals( ChangeType.ModRdn, reversed.getChangeType() );
        assertTrue( reversed.isDeleteOldRdn() );
        assertEquals( oldRdn.getUpName(), reversed.getNewRdn() );
        assertNull( reversed.getNewSuperior() );
    }


    /**
     * Test a reversed rename ModifyDN, where the initial RDN is composite, 
     * the new RDN is simple, not overlapping, with deleteOldRdn = false, and 
     * with an AVA present in the initial entry.
     * 
     * Covers case 3 of http://cwiki.apache.org/confluence/display/DIRxSRVx11/Reverse+LDIF
     * 
     * Initial entry
     * dn: cn=small+cn=test,ou=system
     * objectclass: top
     * objectclass: person
     * cn: test
     * cn: small
     * cn: big
     * sn: This is a test 
     * 
     * new RDN : cn=big
     *
     * @throws NamingException on error
     */
    @Test
    public void test3ReverseRenameCompositeSimpleNotOverlappingKeepOldRdnExistsInEntry() throws NamingException
    {
        LdapDN dn = new LdapDN( "cn=small+cn=test,ou=system" );
        Rdn oldRdn = new Rdn( "cn=small+cn=test" );
        Rdn newRdn = new Rdn( "cn=big" );

        Entry entry = new DefaultClientEntry( dn );
        entry.put( "cn", "test", "small", "big" );
        entry.put( "objectClass", "person", "top" );
        entry.put( "sn", "this is a test" );

        List<LdifEntry> reverseds = LdifRevertor.reverseRename( entry, newRdn, LdifRevertor.KEEP_OLD_RDN );

        assertNotNull( reverseds );
        assertEquals( 1, reverseds.size() );
        LdifEntry reversed = reverseds.get( 0 );
        
        assertEquals( "cn=big,ou=system", reversed.getDn().getName() );
        assertEquals( ChangeType.ModRdn, reversed.getChangeType() );
        assertFalse( reversed.isDeleteOldRdn() );
        assertEquals( oldRdn.getUpName(), reversed.getNewRdn() );
        assertNull( reversed.getNewSuperior() );
    }


    /**
     * Test a reversed rename ModifyDN, where the initial RDN is composite, 
     * the new RDN is simple, not overlapping, with deleteOldRdn = true, and 
     * with an AVA not present in the initial entry.
     * 
     * Covers case 4 of http://cwiki.apache.org/confluence/display/DIRxSRVx11/Reverse+LDIF
     * 
     * Initial entry
     * dn: c,=small+cn=test,ou=system
     * objectclass: top
     * objectclass: person
     * cn: test
     * cn: small
     * sn: This is a test 
     * 
     * new RDN : cn=joe
     *
     * @throws NamingException on error
     */
    @Test
    public void test4ReverseRenameCompositeSimpleNotOverlappingDeleteOldRdnDontExistsInEntry() throws NamingException
    {
        LdapDN dn = new LdapDN( "cn=small+cn=test,ou=system" );
        Rdn oldRdn = new Rdn( "cn=small+cn=test" );
        Rdn newRdn = new Rdn( "cn=joe" );

        Entry entry = new DefaultClientEntry( dn );
        entry.put( "cn", "test", "small" );
        entry.put( "objectClass", "person", "top" );
        entry.put( "sn", "this is a test" );

        List<LdifEntry> reverseds = LdifRevertor.reverseRename( entry, newRdn, LdifRevertor.DELETE_OLD_RDN );

        assertNotNull( reverseds );
        assertEquals( 1, reverseds.size() );
        LdifEntry reversed = reverseds.get( 0 );
        
        assertEquals( "cn=joe,ou=system", reversed.getDn().getName() );
        assertEquals( ChangeType.ModRdn, reversed.getChangeType() );
        assertTrue( reversed.isDeleteOldRdn() );
        assertEquals( oldRdn.getUpName(), reversed.getNewRdn() );
        assertNull( reversed.getNewSuperior() );
    }


    /**
     * Test a reversed rename ModifyDN, where the initial RDN is composite, 
     * the new RDN is simple, not overlapping, with deleteOldRdn = true, and 
     * with an AVA present in the initial entry.
     * 
     * Covers case 4 of http://cwiki.apache.org/confluence/display/DIRxSRVx11/Reverse+LDIF
     * 
     * Initial entry
     * dn: cn=small+cn=test,ou=system
     * objectclass: top
     * objectclass: person
     * cn: test
     * cn: small
     * cn: big
     * sn: This is a test 
     * 
     * new RDN : cn=big
     *
     * @throws NamingException on error
     */
    @Test
    public void test4ReverseRenameCompositeSimpleNotOverlappingDeleteOldRdnExistInEntry() throws NamingException
    {
        LdapDN dn = new LdapDN( "cn=small+cn=test,ou=system" );
        Rdn oldRdn = new Rdn( "cn=small+cn=test" );
        Rdn newRdn = new Rdn( "cn=big" );

        Entry entry = new DefaultClientEntry( dn );
        entry.put( "cn", "test", "small", "big" );
        entry.put( "objectClass", "person", "top" );
        entry.put( "sn", "this is a test" );

        List<LdifEntry> reverseds = LdifRevertor.reverseRename( entry, newRdn, LdifRevertor.DELETE_OLD_RDN );

        assertNotNull( reverseds );
        assertEquals( 1, reverseds.size() );
        LdifEntry reversed = reverseds.get( 0 );
        
        assertEquals( "cn=big,ou=system", reversed.getDn().getName() );
        assertEquals( ChangeType.ModRdn, reversed.getChangeType() );
        assertFalse( reversed.isDeleteOldRdn() );
        assertEquals( oldRdn.getUpName(), reversed.getNewRdn() );
        assertNull( reversed.getNewSuperior() );
    }


    /**
     * Test a reversed rename ModifyDN, where the initial RDN is composite, 
     * the new RDN is simple, they overlap, with deleteOldRdn = false.
     * 
     * Covers case 5 of http://cwiki.apache.org/confluence/display/DIRxSRVx11/Reverse+LDIF
     * 
     * Initial entry
     * dn: cn=small+cn=test,ou=system
     * objectclass: top
     * objectclass: person
     * cn: test
     * cn: small
     * sn: This is a test 
     * 
     * new RDN : cn=test
     *
     * @throws NamingException on error
     */
    @Test
    public void test5ReverseRenameCompositeSimpleOverlappingKeepOldRdn() throws NamingException
    {
        LdapDN dn = new LdapDN( "cn=small+cn=test,ou=system" );
        Rdn oldRdn = new Rdn( "cn=small+cn=test" );
        Rdn newRdn = new Rdn( "cn=test" );

        Entry entry = new DefaultClientEntry( dn );
        entry.put( "cn", "test", "small" );
        entry.put( "objectClass", "person", "top" );
        entry.put( "sn", "this is a test" );

        List<LdifEntry> reverseds = LdifRevertor.reverseRename( entry, newRdn, LdifRevertor.KEEP_OLD_RDN );

        assertNotNull( reverseds );
        assertEquals( 1, reverseds.size() );
        LdifEntry reversed = reverseds.get( 0 );
        
        assertEquals( "cn=test,ou=system", reversed.getDn().getName() );
        assertEquals( ChangeType.ModRdn, reversed.getChangeType() );
        assertFalse( reversed.isDeleteOldRdn() );
        assertEquals( oldRdn.getUpName(), reversed.getNewRdn() );
        assertNull( reversed.getNewSuperior() );
    }


    /**
     * Test a reversed rename ModifyDN, where the initial RDN is composite, 
     * the new RDN is simple, they overlap, with deleteOldRdn = true.
     * 
     * Covers case 5 of http://cwiki.apache.org/confluence/display/DIRxSRVx11/Reverse+LDIF
     * 
     * Initial entry
     * dn: cn=small+cn=test,ou=system
     * objectclass: top
     * objectclass: person
     * cn: test
     * cn: small
     * sn: This is a test 
     * 
     * new RDN : cn=test
     *
     * @throws NamingException on error
     */
    @Test
    public void test5ReverseRenameCompositeSimpleOverlappingDeleteOldRdn() throws NamingException
    {
        LdapDN dn = new LdapDN( "cn=small+cn=test,ou=system" );
        Rdn oldRdn = new Rdn( "cn=small+cn=test" );
        Rdn newRdn = new Rdn( "cn=test" );

        Entry entry = new DefaultClientEntry( dn );
        entry.put( "cn", "test", "small" );
        entry.put( "objectClass", "person", "top" );
        entry.put( "sn", "this is a test" );

        List<LdifEntry> reverseds = LdifRevertor.reverseRename( entry, newRdn, LdifRevertor.DELETE_OLD_RDN );

        assertNotNull( reverseds );
        assertEquals( 1, reverseds.size() );
        LdifEntry reversed = reverseds.get( 0 );
        
        assertEquals( "cn=test,ou=system", reversed.getDn().getName() );
        assertEquals( ChangeType.ModRdn, reversed.getChangeType() );
        assertFalse( reversed.isDeleteOldRdn() );
        assertEquals( oldRdn.getUpName(), reversed.getNewRdn() );
        assertNull( reversed.getNewSuperior() );
    }


    /**
     * Test a reversed rename ModifyDN, where the initial RDN is simple, 
     * the new RDN is composite, they don't overlap, with deleteOldRdn = false, and
     * the new values don't exist in the entry.
     * 
     * Covers case 6.1 of http://cwiki.apache.org/confluence/display/DIRxSRVx11/Reverse+LDIF
     * 
     * Initial entry
     * dn: cn=test,ou=system
     * objectclass: top
     * objectclass: person
     * cn: test
     * cn: small
     * sn: This is a test 
     * 
     * new RDN : cn=joe+cn=plumber
     *
     * @throws NamingException on error
     */
    @Test
    public void test61ReverseRenameSimpleCompositeNotOverlappingKeepOldRdnDontExistInEntry() throws NamingException
    {
        LdapDN dn = new LdapDN( "cn=test,ou=system" );
        Rdn oldRdn = new Rdn( "cn=test" );
        Rdn newRdn = new Rdn( "cn=joe+cn=plumber" );

        Entry entry = new DefaultClientEntry( dn );
        entry.put( "cn", "test", "small" );
        entry.put( "objectClass", "person", "top" );
        entry.put( "sn", "this is a test" );

        List<LdifEntry> reverseds = LdifRevertor.reverseRename( entry, newRdn, LdifRevertor.KEEP_OLD_RDN );

        assertNotNull( reverseds );
        assertEquals( 1, reverseds.size() );
        LdifEntry reversed = reverseds.get( 0 );
        
        assertEquals( "cn=joe+cn=plumber,ou=system", reversed.getDn().getName() );
        assertEquals( ChangeType.ModRdn, reversed.getChangeType() );
        assertTrue( reversed.isDeleteOldRdn() );
        assertEquals( oldRdn.getUpName(), reversed.getNewRdn() );
        assertNull( reversed.getNewSuperior() );
    }


    /**
     * Test a reversed rename ModifyDN, where the initial RDN is simple, 
     * the new RDN is composite, they don't overlap, with deleteOldRdn = false, and
     * the new values exists in the entry.
     * 
     * Covers case 6.2 of http://cwiki.apache.org/confluence/display/DIRxSRVx11/Reverse+LDIF
     * 
     * Initial entry
     * dn: cn=test,ou=system
     * objectclass: top
     * objectclass: person
     * cn: test
     * cn: small
     * sn: This is a test 
     * 
     * new RDN : cn=joe+cn=small
     *
     * @throws NamingException on error
     */
    @Test
    public void test62ReverseRenameSimpleCompositeNotOverlappingKeepOldRdnDontExistInEntry() throws NamingException
    {
        LdapDN dn = new LdapDN( "cn=test,ou=system" );
        Rdn oldRdn = new Rdn( "cn=test" );
        Rdn newRdn = new Rdn( "cn=joe+cn=small" );

        Entry entry = new DefaultClientEntry( dn );
        entry.put( "cn", "test", "small" );
        entry.put( "objectClass", "person", "top" );
        entry.put( "sn", "this is a test" );

        List<LdifEntry> reverseds = LdifRevertor.reverseRename( entry, newRdn, LdifRevertor.KEEP_OLD_RDN );

        assertNotNull( reverseds );
        assertEquals( 2, reverseds.size() );
        LdifEntry reversed = reverseds.get( 0 );
        
        assertEquals( "cn=joe+cn=small,ou=system", reversed.getDn().getName() );
        assertEquals( ChangeType.ModRdn, reversed.getChangeType() );
        assertFalse( reversed.isDeleteOldRdn() );
        assertEquals( oldRdn.getUpName(), reversed.getNewRdn() );
        assertNull( reversed.getNewSuperior() );

        reversed = reverseds.get( 1 );
        
        assertEquals( "cn=test,ou=system", reversed.getDn().getName() );
        assertEquals( ChangeType.Modify, reversed.getChangeType() );
        Modification[] mods = reversed.getModificationItemsArray();
        
        assertNotNull( mods );
        assertEquals( 1, mods.length );
        assertEquals( ModificationOperation.REMOVE_ATTRIBUTE, mods[0].getOperation() );
        assertNotNull( mods[0].getAttribute() );
        assertEquals( "cn", mods[0].getAttribute().getId() );
        assertEquals( "joe", mods[0].getAttribute().getString() );
    }


    /**
     * Test a reversed rename ModifyDN, where the initial RDN is simple, 
     * the new RDN is composite, they don't overlap, with deleteOldRdn = true, and
     * none of new values exists in the entry.
     * 
     * Covers case 7.1 of http://cwiki.apache.org/confluence/display/DIRxSRVx11/Reverse+LDIF
     * 
     * Initial entry
     * dn: cn=test,ou=system
     * objectclass: top
     * objectclass: person
     * cn: test
     * cn: small
     * sn: This is a test 
     * 
     * new RDN : cn=joe+cn=plumber
     *
     * @throws NamingException on error
     */
    @Test
    public void test71ReverseRenameSimpleCompositeNotOverlappingDeleteOldRdnDontExistInEntry() throws NamingException
    {
        LdapDN dn = new LdapDN( "cn=test,ou=system" );
        Rdn oldRdn = new Rdn( "cn=test" );
        Rdn newRdn = new Rdn( "cn=joe+cn=plumber" );

        Entry entry = new DefaultClientEntry( dn );
        entry.put( "cn", "test", "small" );
        entry.put( "objectClass", "person", "top" );
        entry.put( "sn", "this is a test" );

        List<LdifEntry> reverseds = LdifRevertor.reverseRename( entry, newRdn, LdifRevertor.DELETE_OLD_RDN );

        assertNotNull( reverseds );
        assertEquals( 1, reverseds.size() );
        LdifEntry reversed = reverseds.get( 0 );
        
        assertEquals( "cn=joe+cn=plumber,ou=system", reversed.getDn().getName() );
        assertEquals( ChangeType.ModRdn, reversed.getChangeType() );
        assertTrue( reversed.isDeleteOldRdn() );
        assertEquals( oldRdn.getUpName(), reversed.getNewRdn() );
        assertNull( reversed.getNewSuperior() );
    }


    /**
     * Test a reversed rename ModifyDN, where the initial RDN is simple, 
     * the new RDN is composite, they don't overlap, with deleteOldRdn = true, and
     * some of new values exists in the entry.
     * 
     * Covers case 7.2 of http://cwiki.apache.org/confluence/display/DIRxSRVx11/Reverse+LDIF
     * 
     * Initial entry
     * dn: cn=test,ou=system
     * objectclass: top
     * objectclass: person
     * cn: test
     * cn: small
     * sn: This is a test 
     * 
     * new RDN : cn=joe+cn=small
     *
     * @throws NamingException on error
     */
    @Test
    public void test72ReverseRenameSimpleCompositeNotOverlappingDeleteOldRdnExistInEntry() throws NamingException
    {
        LdapDN dn = new LdapDN( "cn=test,ou=system" );
        Rdn oldRdn = new Rdn( "cn=test" );
        Rdn newRdn = new Rdn( "cn=joe+cn=small" );

        Entry entry = new DefaultClientEntry( dn );
        entry.put( "cn", "test", "small" );
        entry.put( "objectClass", "person", "top" );
        entry.put( "sn", "this is a test" );

        List<LdifEntry> reverseds = LdifRevertor.reverseRename( entry, newRdn, LdifRevertor.DELETE_OLD_RDN );

        assertNotNull( reverseds );
        assertEquals( 2, reverseds.size() );
        LdifEntry reversed = reverseds.get( 0 );
        
        assertEquals( "cn=joe+cn=small,ou=system", reversed.getDn().getName() );
        assertEquals( ChangeType.ModRdn, reversed.getChangeType() );
        assertFalse( reversed.isDeleteOldRdn() );
        assertEquals( oldRdn.getUpName(), reversed.getNewRdn() );
        assertNull( reversed.getNewSuperior() );
        
        reversed = reverseds.get( 1 );
        
        assertEquals( "cn=test,ou=system", reversed.getDn().getName() );
        assertEquals( ChangeType.Modify, reversed.getChangeType() );
        Modification[] mods = reversed.getModificationItemsArray();
        
        assertNotNull( mods );
        assertEquals( 1, mods.length );
        assertEquals( ModificationOperation.REMOVE_ATTRIBUTE, mods[0].getOperation() );
        assertNotNull( mods[0].getAttribute() );
        assertEquals( "cn", mods[0].getAttribute().getId() );
        assertEquals( "joe", mods[0].getAttribute().getString() );
    }


    /**
     * Test a reversed rename ModifyDN, where the initial RDN is simple, 
     * the new RDN is composite, they overlap, with deleteOldRdn = false, and
     * none of new values exists in the entry.
     * 
     * Covers case 8.1 of http://cwiki.apache.org/confluence/display/DIRxSRVx11/Reverse+LDIF
     * 
     * Initial entry
     * dn: cn=test,ou=system
     * objectclass: top
     * objectclass: person
     * cn: test
     * cn: big
     * sn: This is a test 
     * 
     * new RDN : cn=small+cn=test
     *
     * @throws NamingException on error
     */
    @Test
    public void test81ReverseRenameSimpleCompositeOverlappingKeepOldRdnDontExistInEntry() throws NamingException
    {
        LdapDN dn = new LdapDN( "cn=test,ou=system" );
        Rdn oldRdn = new Rdn( "cn=test" );
        Rdn newRdn = new Rdn( "cn=small+cn=test" );

        Entry entry = new DefaultClientEntry( dn );
        entry.put( "cn", "test", "big" );
        entry.put( "objectClass", "person", "top" );
        entry.put( "sn", "this is a test" );

        List<LdifEntry> reverseds = LdifRevertor.reverseRename( entry, newRdn, LdifRevertor.KEEP_OLD_RDN );

        assertNotNull( reverseds );
        assertEquals( 1, reverseds.size() );
        LdifEntry reversed = reverseds.get( 0 );
        
        assertEquals( "cn=small+cn=test,ou=system", reversed.getDn().getName() );
        assertEquals( ChangeType.ModRdn, reversed.getChangeType() );
        assertTrue( reversed.isDeleteOldRdn() );
        assertEquals( oldRdn.getUpName(), reversed.getNewRdn() );
        assertNull( reversed.getNewSuperior() );
    }




    /**
     * Test a reversed rename ModifyDN, where the initial RDN is simple, 
     * the new RDN is composite, they overlap, with deleteOldRdn = false, and
     * some of the new values exist in the entry.
     * 
     * Covers case 8.2 of http://cwiki.apache.org/confluence/display/DIRxSRVx11/Reverse+LDIF
     * 
     * Initial entry
     * dn: cn=test,ou=system
     * objectclass: top
     * objectclass: person
     * cn: test
     * cn: big
     * sn: This is a test 
     * 
     * new RDN : cn=small+cn=test
     *
     * @throws NamingException on error
     */
    @Test
    public void test82ReverseRenameSimpleCompositeOverlappingKeepOldRdnExistInEntry() throws NamingException
    {
        LdapDN dn = new LdapDN( "cn=test,ou=system" );
        Rdn oldRdn = new Rdn( "cn=test" );
        Rdn newRdn = new Rdn( "cn=small+cn=test+cn=big" );

        Entry entry = new DefaultClientEntry( dn );
        entry.put( "cn", "test", "big" );
        entry.put( "objectClass", "person", "top" );
        entry.put( "sn", "this is a test" );

        List<LdifEntry> reverseds = LdifRevertor.reverseRename( entry, newRdn, LdifRevertor.KEEP_OLD_RDN );

        assertNotNull( reverseds );
        assertEquals( 2, reverseds.size() );
        LdifEntry reversed = reverseds.get( 0 );
        
        assertEquals( "cn=small+cn=test+cn=big,ou=system", reversed.getDn().getName() );
        assertEquals( ChangeType.ModRdn, reversed.getChangeType() );
        assertFalse( reversed.isDeleteOldRdn() );
        assertEquals( oldRdn.getUpName(), reversed.getNewRdn() );
        assertNull( reversed.getNewSuperior() );

        reversed = reverseds.get( 1 );
        
        assertEquals( "cn=test,ou=system", reversed.getDn().getName() );
        assertEquals( ChangeType.Modify, reversed.getChangeType() );
        Modification[] mods = reversed.getModificationItemsArray();
        
        assertNotNull( mods );
        assertEquals( 1, mods.length );
        assertEquals( ModificationOperation.REMOVE_ATTRIBUTE, mods[0].getOperation() );
        assertNotNull( mods[0].getAttribute() );
        assertEquals( "cn", mods[0].getAttribute().getId() );
        assertEquals( "small", mods[0].getAttribute().getString() );
    }


    /**
     * Test a reversed rename ModifyDN, where the initial RDN is simple, 
     * the new RDN is composite, they overlap, with deleteOldRdn = true, and
     * none of new values exists in the entry.
     * 
     * Covers case 9.1 of http://cwiki.apache.org/confluence/display/DIRxSRVx11/Reverse+LDIF
     * 
     * Initial entry
     * dn: cn=test,ou=system
     * objectclass: top
     * objectclass: person
     * cn: test
     * cn: big
     * sn: This is a test 
     * 
     * new RDN : cn=small+cn=test
     *
     * @throws NamingException on error
     */
    @Test
    public void test91ReverseRenameSimpleCompositeOverlappingDeleteOldRdnDontExistInEntry() throws NamingException
    {
        LdapDN dn = new LdapDN( "cn=test,ou=system" );
        Rdn oldRdn = new Rdn( "cn=test" );
        Rdn newRdn = new Rdn( "cn=small+cn=test" );

        Entry entry = new DefaultClientEntry( dn );
        entry.put( "cn", "test", "big" );
        entry.put( "objectClass", "person", "top" );
        entry.put( "sn", "this is a test" );

        List<LdifEntry> reverseds = LdifRevertor.reverseRename( entry, newRdn, LdifRevertor.DELETE_OLD_RDN );

        assertNotNull( reverseds );
        assertEquals( 1, reverseds.size() );
        LdifEntry reversed = reverseds.get( 0 );
        
        assertEquals( "cn=small+cn=test,ou=system", reversed.getDn().getName() );
        assertEquals( ChangeType.ModRdn, reversed.getChangeType() );
        assertTrue( reversed.isDeleteOldRdn() );
        assertEquals( oldRdn.getUpName(), reversed.getNewRdn() );
        assertNull( reversed.getNewSuperior() );
    }


    /**
     * Test a reversed rename ModifyDN, where the initial RDN is simple, 
     * the new RDN is composite, they overlap, with deleteOldRdn = true, and
     * some of the new values exists in the entry.
     * 
     * Covers case 9.2 of http://cwiki.apache.org/confluence/display/DIRxSRVx11/Reverse+LDIF
     * 
     * Initial entry
     * dn: cn=test,ou=system
     * objectclass: top
     * objectclass: person
     * cn: test
     * cn: big
     * sn: This is a test 
     * 
     * new RDN : cn=small+cn=test+cn=big
     *
     * @throws NamingException on error
     */
    @Test
    public void test92ReverseRenameSimpleCompositeOverlappingDeleteOldRdnDontExistInEntry() throws NamingException
    {
        LdapDN dn = new LdapDN( "cn=test,ou=system" );
        Rdn oldRdn = new Rdn( "cn=test" );
        Rdn newRdn = new Rdn( "cn=small+cn=test+cn=big" );

        Entry entry = new DefaultClientEntry( dn );
        entry.put( "cn", "test", "big" );
        entry.put( "objectClass", "person", "top" );
        entry.put( "sn", "this is a test" );

        List<LdifEntry> reverseds = LdifRevertor.reverseRename( entry, newRdn, LdifRevertor.DELETE_OLD_RDN );

        assertNotNull( reverseds );
        assertEquals( 2, reverseds.size() );
        LdifEntry reversed = reverseds.get( 0 );
        
        assertEquals( "cn=small+cn=test+cn=big,ou=system", reversed.getDn().getName() );
        assertEquals( ChangeType.ModRdn, reversed.getChangeType() );
        assertFalse( reversed.isDeleteOldRdn() );
        assertEquals( oldRdn.getUpName(), reversed.getNewRdn() );
        assertNull( reversed.getNewSuperior() );

        reversed = reverseds.get( 1 );
        
        assertEquals( "cn=test,ou=system", reversed.getDn().getName() );
        assertEquals( ChangeType.Modify, reversed.getChangeType() );
        Modification[] mods = reversed.getModificationItemsArray();
        
        assertNotNull( mods );
        assertEquals( 1, mods.length );
        assertEquals( ModificationOperation.REMOVE_ATTRIBUTE, mods[0].getOperation() );
        assertNotNull( mods[0].getAttribute() );
        assertEquals( "cn", mods[0].getAttribute().getId() );
        assertEquals( "small", mods[0].getAttribute().getString() );
    }


    /**
     * Test a reversed rename ModifyDN, where the initial RDN is composite, 
     * the new RDN is composite, they don't overlap, with deleteOldRdn = false, and
     * none of new values exists in the entry.
     * 
     * Covers case 10.1 of http://cwiki.apache.org/confluence/display/DIRxSRVx11/Reverse+LDIF
     * 
     * Initial entry
     * dn: cn=small+cn=test,ou=system
     * objectclass: top
     * objectclass: person
     * cn: test
     * cn: small
     * cn: big
     * sn: This is a test 
     * 
     * new RDN : cn=joe+cn=plumber
     *
     * @throws NamingException on error
     */
    @Test
    public void test101ReverseRenameCompositeCompositeNotOverlappingKeepOldRdnDontExistInEntry() throws NamingException
    {
        LdapDN dn = new LdapDN( "cn=small+cn=test,ou=system" );
        Rdn oldRdn = new Rdn( "cn=small+cn=test" );
        Rdn newRdn = new Rdn( "cn=joe+cn=plumber" );

        Entry entry = new DefaultClientEntry( dn );
        entry.put( "cn", "test", "big", "small" );
        entry.put( "objectClass", "person", "top" );
        entry.put( "sn", "this is a test" );

        List<LdifEntry> reverseds = LdifRevertor.reverseRename( entry, newRdn, LdifRevertor.KEEP_OLD_RDN );

        assertNotNull( reverseds );
        assertEquals( 1, reverseds.size() );
        LdifEntry reversed = reverseds.get( 0 );
        
        assertEquals( "cn=joe+cn=plumber,ou=system", reversed.getDn().getName() );
        assertEquals( ChangeType.ModRdn, reversed.getChangeType() );
        assertTrue( reversed.isDeleteOldRdn() );
        assertEquals( oldRdn.getUpName(), reversed.getNewRdn() );
        assertNull( reversed.getNewSuperior() );
    }


    /**
     * Test a reversed rename ModifyDN, where the initial RDN is composite, 
     * the new RDN is composite, they don't overlap, with deleteOldRdn = false, and
     * some of the new values exists in the entry.
     * 
     * Covers case 10.2 of http://cwiki.apache.org/confluence/display/DIRxSRVx11/Reverse+LDIF
     * 
     * Initial entry
     * dn: cn=small+cn=test,ou=system
     * objectclass: top
     * objectclass: person
     * cn: test
     * cn: small
     * cn: big
     * sn: This is a test 
     * 
     * new RDN : cn=joe+cn=big
     *
     * @throws NamingException on error
     */
    @Test
    public void test102ReverseRenameCompositeCompositeNotOverlappingKeepOldRdnExistInEntry() throws NamingException
    {
        LdapDN dn = new LdapDN( "cn=small+cn=test,ou=system" );
        Rdn oldRdn = new Rdn( "cn=small+cn=test" );
        Rdn newRdn = new Rdn( "cn=joe+cn=big" );

        Entry entry = new DefaultClientEntry( dn );
        entry.put( "cn", "test", "big", "small" );
        entry.put( "objectClass", "person", "top" );
        entry.put( "sn", "this is a test" );

        List<LdifEntry> reverseds = LdifRevertor.reverseRename( entry, newRdn, LdifRevertor.KEEP_OLD_RDN );

        assertNotNull( reverseds );
        assertEquals( 2, reverseds.size() );
        LdifEntry reversed = reverseds.get( 0 );
        
        assertEquals( "cn=joe+cn=big,ou=system", reversed.getDn().getName() );
        assertEquals( ChangeType.ModRdn, reversed.getChangeType() );
        assertFalse( reversed.isDeleteOldRdn() );
        assertEquals( oldRdn.getUpName(), reversed.getNewRdn() );
        assertNull( reversed.getNewSuperior() );

        reversed = reverseds.get( 1 );
        
        assertEquals( "cn=small+cn=test,ou=system", reversed.getDn().getName() );
        assertEquals( ChangeType.Modify, reversed.getChangeType() );
        Modification[] mods = reversed.getModificationItemsArray();
        
        assertNotNull( mods );
        assertEquals( 1, mods.length );
        assertEquals( ModificationOperation.REMOVE_ATTRIBUTE, mods[0].getOperation() );
        assertNotNull( mods[0].getAttribute() );
        assertEquals( "cn", mods[0].getAttribute().getId() );
        assertEquals( "joe", mods[0].getAttribute().getString() );
    }


    /**
     * Test a reversed rename ModifyDN, where the initial RDN is composite, 
     * the new RDN is composite, they don't overlap, with deleteOldRdn = true, and
     * none of new values exists in the entry.
     * 
     * Covers case 11.1 of http://cwiki.apache.org/confluence/display/DIRxSRVx11/Reverse+LDIF
     * 
     * Initial entry
     * dn: cn=small+cn=test,ou=system
     * objectclass: top
     * objectclass: person
     * cn: test
     * cn: small
     * cn: big
     * sn: This is a test 
     * 
     * new RDN : cn=joe+cn=plumber
     *
     * @throws NamingException on error
     */
    @Test
    public void test111ReverseRenameCompositeCompositeNotOverlappingDeleteOldRdnDontExistInEntry() throws NamingException
    {
        LdapDN dn = new LdapDN( "cn=small+cn=test,ou=system" );
        Rdn oldRdn = new Rdn( "cn=small+cn=test" );
        Rdn newRdn = new Rdn( "cn=joe+cn=plumber" );

        Entry entry = new DefaultClientEntry( dn );
        entry.put( "cn", "test", "big", "small" );
        entry.put( "objectClass", "person", "top" );
        entry.put( "sn", "this is a test" );

        List<LdifEntry> reverseds = LdifRevertor.reverseRename( entry, newRdn, LdifRevertor.DELETE_OLD_RDN );

        assertNotNull( reverseds );
        assertEquals( 1, reverseds.size() );
        LdifEntry reversed = reverseds.get( 0 );
        
        assertEquals( "cn=joe+cn=plumber,ou=system", reversed.getDn().getName() );
        assertEquals( ChangeType.ModRdn, reversed.getChangeType() );
        assertTrue( reversed.isDeleteOldRdn() );
        assertEquals( oldRdn.getUpName(), reversed.getNewRdn() );
        assertNull( reversed.getNewSuperior() );
    }


    /**
     * Test a reversed rename ModifyDN, where the initial RDN is composite, 
     * the new RDN is composite, they don't overlap, with deleteOldRdn = true, and
     * some of the new values exists in the entry.
     * 
     * Covers case 11.2 of http://cwiki.apache.org/confluence/display/DIRxSRVx11/Reverse+LDIF
     * 
     * Initial entry
     * dn: cn=small+cn=test,ou=system
     * objectclass: top
     * objectclass: person
     * cn: test
     * cn: small
     * cn: big
     * sn: This is a test 
     * 
     * new RDN : cn=joe+cn=plumber
     *
     * @throws NamingException on error
     */
    @Test
    public void test112ReverseRenameCompositeCompositeNotOverlappingDeleteOldRdnExistInEntry() throws NamingException
    {
        LdapDN dn = new LdapDN( "cn=small+cn=test,ou=system" );
        Rdn oldRdn = new Rdn( "cn=small+cn=test" );
        Rdn newRdn = new Rdn( "cn=joe+cn=big" );

        Entry entry = new DefaultClientEntry( dn );
        entry.put( "cn", "test", "big", "small" );
        entry.put( "objectClass", "person", "top" );
        entry.put( "sn", "this is a test" );

        List<LdifEntry> reverseds = LdifRevertor.reverseRename( entry, newRdn, LdifRevertor.DELETE_OLD_RDN );

        assertNotNull( reverseds );
        assertEquals( 2, reverseds.size() );
        LdifEntry reversed = reverseds.get( 0 );
        
        assertEquals( "cn=joe+cn=big,ou=system", reversed.getDn().getName() );
        assertEquals( ChangeType.ModRdn, reversed.getChangeType() );
        assertFalse( reversed.isDeleteOldRdn() );
        assertEquals( oldRdn.getUpName(), reversed.getNewRdn() );
        assertNull( reversed.getNewSuperior() );

        reversed = reverseds.get( 1 );
        
        assertEquals( "cn=small+cn=test,ou=system", reversed.getDn().getName() );
        assertEquals( ChangeType.Modify, reversed.getChangeType() );
        Modification[] mods = reversed.getModificationItemsArray();
        
        assertNotNull( mods );
        assertEquals( 1, mods.length );
        assertEquals( ModificationOperation.REMOVE_ATTRIBUTE, mods[0].getOperation() );
        assertNotNull( mods[0].getAttribute() );
        assertEquals( "cn", mods[0].getAttribute().getId() );
        assertEquals( "joe", mods[0].getAttribute().getString() );
    }


    /**
     * Test a reversed rename ModifyDN, where the initial RDN is composite, 
     * the new RDN is composite, they are overlapping, with deleteOldRdn = false, and
     * none of new values exists in the entry.
     * 
     * Covers case 12.1 of http://cwiki.apache.org/confluence/display/DIRxSRVx11/Reverse+LDIF
     * 
     * Initial entry
     * dn: cn=small+cn=test,ou=system
     * objectclass: top
     * objectclass: person
     * cn: test
     * cn: small
     * cn: big
     * sn: This is a test 
     * 
     * new RDN : cn=joe+cn=test
     *
     * @throws NamingException on error
     */
    @Test
    public void test121ReverseRenameCompositeCompositeOverlappingKeepOldRdnDontExistInEntry() throws NamingException
    {
        LdapDN dn = new LdapDN( "cn=small+cn=test,ou=system" );
        Rdn oldRdn = new Rdn( "cn=small+cn=test" );
        Rdn newRdn = new Rdn( "cn=joe+cn=test" );

        Entry entry = new DefaultClientEntry( dn );
        entry.put( "cn", "test", "big", "small" );
        entry.put( "objectClass", "person", "top" );
        entry.put( "sn", "this is a test" );

        List<LdifEntry> reverseds = LdifRevertor.reverseRename( entry, newRdn, LdifRevertor.KEEP_OLD_RDN );

        assertNotNull( reverseds );
        assertEquals( 1, reverseds.size() );
        LdifEntry reversed = reverseds.get( 0 );
        
        assertEquals( "cn=joe+cn=test,ou=system", reversed.getDn().getName() );
        assertEquals( ChangeType.ModRdn, reversed.getChangeType() );
        assertTrue( reversed.isDeleteOldRdn() );
        assertEquals( oldRdn.getUpName(), reversed.getNewRdn() );
        assertNull( reversed.getNewSuperior() );
    }


    /**
     * Test a reversed rename ModifyDN, where the initial RDN is composite, 
     * the new RDN is composite, they are overlapping, with deleteOldRdn = false, and
     * some of the new values exists in the entry.
     * 
     * Covers case 12.2 of http://cwiki.apache.org/confluence/display/DIRxSRVx11/Reverse+LDIF
     * 
     * Initial entry
     * dn: cn=small+cn=test,ou=system
     * objectclass: top
     * objectclass: person
     * cn: test
     * cn: small
     * cn: big
     * sn: This is a test 
     * 
     * new RDN : cn=joe+cn=test
     *
     * @throws NamingException on error
     */
    @Test
    public void test122ReverseRenameCompositeCompositeOverlappingKeepOldRdnExistInEntry() throws NamingException
    {
        LdapDN dn = new LdapDN( "cn=small+cn=test,ou=system" );
        Rdn oldRdn = new Rdn( "cn=small+cn=test" );
        Rdn newRdn = new Rdn( "cn=big+cn=test" );

        Entry entry = new DefaultClientEntry( dn );
        entry.put( "cn", "test", "big", "small" );
        entry.put( "objectClass", "person", "top" );
        entry.put( "sn", "this is a test" );

        List<LdifEntry> reverseds = LdifRevertor.reverseRename( entry, newRdn, LdifRevertor.KEEP_OLD_RDN );

        assertNotNull( reverseds );
        assertEquals( 1, reverseds.size() );
        LdifEntry reversed = reverseds.get( 0 );
        
        assertEquals( "cn=big+cn=test,ou=system", reversed.getDn().getName() );
        assertEquals( ChangeType.ModRdn, reversed.getChangeType() );
        assertFalse( reversed.isDeleteOldRdn() );
        assertEquals( oldRdn.getUpName(), reversed.getNewRdn() );
        assertNull( reversed.getNewSuperior() );
    }


    /**
     * Test a reversed rename ModifyDN, where the initial RDN is composite, 
     * the new RDN is composite, they are overlapping, with deleteOldRdn = true, and
     * none of new values exists in the entry.
     * 
     * Covers case 13.1 of http://cwiki.apache.org/confluence/display/DIRxSRVx11/Reverse+LDIF
     * 
     * Initial entry
     * dn: cn=small+cn=test,ou=system
     * objectclass: top
     * objectclass: person
     * cn: test
     * cn: small
     * cn: big
     * sn: This is a test 
     * 
     * new RDN : cn=joe+cn=test
     *
     * @throws NamingException on error
     */
    @Test
    public void test131ReverseRenameCompositeCompositeOverlappingDeleteOldRdnDontExistInEntry() throws NamingException
    {
        LdapDN dn = new LdapDN( "cn=small+cn=test,ou=system" );
        Rdn oldRdn = new Rdn( "cn=small+cn=test" );
        Rdn newRdn = new Rdn( "cn=joe+cn=test" );

        Entry entry = new DefaultClientEntry( dn );
        entry.put( "cn", "test", "big", "small" );
        entry.put( "objectClass", "person", "top" );
        entry.put( "sn", "this is a test" );

        List<LdifEntry> reverseds = LdifRevertor.reverseRename( entry, newRdn, LdifRevertor.DELETE_OLD_RDN );

        assertNotNull( reverseds );
        assertEquals( 1, reverseds.size() );
        LdifEntry reversed = reverseds.get( 0 );
        
        assertEquals( "cn=joe+cn=test,ou=system", reversed.getDn().getName() );
        assertEquals( ChangeType.ModRdn, reversed.getChangeType() );
        assertTrue( reversed.isDeleteOldRdn() );
        assertEquals( oldRdn.getUpName(), reversed.getNewRdn() );
        assertNull( reversed.getNewSuperior() );
    }


    /**
     * Test a reversed rename ModifyDN, where the initial RDN is composite, 
     * the new RDN is composite, they are overlapping, with deleteOldRdn = true, and
     * some of the new values exists in the entry.
     * 
     * Covers case 13.1 of http://cwiki.apache.org/confluence/display/DIRxSRVx11/Reverse+LDIF
     * 
     * Initial entry
     * dn: cn=small+cn=test,ou=system
     * objectclass: top
     * objectclass: person
     * cn: test
     * cn: small
     * cn: big
     * sn: This is a test 
     * 
     * new RDN : cn=big+cn=test
     *
     * @throws NamingException on error
     */
    @Test
    public void test132ReverseRenameCompositeCompositeOverlappingDeleteOldRdnExistInEntry() throws NamingException
    {
        LdapDN dn = new LdapDN( "cn=small+cn=test,ou=system" );
        Rdn oldRdn = new Rdn( "cn=small+cn=test" );
        Rdn newRdn = new Rdn( "cn=big+cn=test" );

        Entry entry = new DefaultClientEntry( dn );
        entry.put( "cn", "test", "big", "small" );
        entry.put( "objectClass", "person", "top" );
        entry.put( "sn", "this is a test" );

        List<LdifEntry> reverseds = LdifRevertor.reverseRename( entry, newRdn, LdifRevertor.DELETE_OLD_RDN );

        assertNotNull( reverseds );
        assertEquals( 1, reverseds.size() );
        LdifEntry reversed = reverseds.get( 0 );
        
        assertEquals( "cn=big+cn=test,ou=system", reversed.getDn().getName() );
        assertEquals( ChangeType.ModRdn, reversed.getChangeType() );
        assertFalse( reversed.isDeleteOldRdn() );
        assertEquals( oldRdn.getUpName(), reversed.getNewRdn() );
        assertNull( reversed.getNewSuperior() );
    }
}
