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
package org.apache.directory.server.core.operations.modify;


import static org.apache.directory.server.core.integ.IntegrationUtils.getSchemaContext;
import static org.apache.directory.server.core.integ.IntegrationUtils.getSystemContext;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import javax.naming.NameNotFoundException;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.BasicAttribute;
import javax.naming.directory.BasicAttributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.ModificationItem;
import javax.naming.directory.NoSuchAttributeException;
import javax.naming.directory.SchemaViolationException;
import javax.naming.ldap.LdapContext;

import org.apache.directory.server.core.annotations.ApplyLdifs;
import org.apache.directory.server.core.annotations.CreateDS;
import org.apache.directory.server.core.integ.AbstractLdapTestUnit;
import org.apache.directory.server.core.integ.FrameworkRunner;
import org.junit.Test;
import org.junit.runner.RunWith;
 

/**
 * Tests the modify() methods of the provider.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
@RunWith ( FrameworkRunner.class )
@CreateDS(name = "ModifyDelIT")
@ApplyLdifs(
    {
        "dn: m-oid=2.2.0, ou=attributeTypes, cn=apachemeta, ou=schema",
        "objectclass: metaAttributeType",
        "objectclass: metaTop",
        "objectclass: top",
        "m-oid: 2.2.0",
        "m-name: integerAttribute",
        "m-description: the precursor for all integer attributes",
        "m-equality: integerMatch",
        "m-ordering: integerOrderingMatch",
        "m-syntax: 1.3.6.1.4.1.1466.115.121.1.27",
        "m-length: 0",
        "",
        "dn: ou=testing00,ou=system",
        "objectClass: top",
        "objectClass: organizationalUnit",
        "objectClass: extensibleObject",
        "ou: testing00",
        "integerAttribute: 0",
        "",
        "dn: ou=testing01,ou=system",
        "objectClass: top",
        "objectClass: organizationalUnit",
        "objectClass: extensibleObject",
        "ou: testing01",
        "integerAttribute: 1",
        "",
        "dn: ou=testing02,ou=system",
        "objectClass: top",
        "objectClass: organizationalUnit",
        "objectClass: extensibleObject",
        "ou: testing02",
        "integerAttribute: 2",
        "c: FR",
        "",
        "dn: ou=testing03,ou=system",
        "objectClass: top",
        "objectClass: organizationalUnit",
        "objectClass: extensibleObject",
        "ou: testing03",
        "integerAttribute: 3",
        "",
        "dn: ou=testing04,ou=system",
        "objectClass: top",
        "objectClass: organizationalUnit",
        "objectClass: extensibleObject",
        "ou: testing04",
        "integerAttribute: 4",
        "",
        "dn: ou=testing05,ou=system",
        "objectClass: top",
        "objectClass: organizationalUnit",
        "objectClass: extensibleObject",
        "ou: testing05",
        "integerAttribute: 5",
        "",
        "dn: ou=subtest,ou=testing01,ou=system",
        "objectClass: top",
        "objectClass: organizationalUnit",
        "ou: subtest",
        "",
        "dn: cn=Heather Nova, ou=system",
        "objectClass: top",
        "objectClass: person",
        "cn: Heather Nova",
        "sn: Nova",
        "telephoneNumber: 1 801 555 1212 ",
        "description: an American singer-songwriter",
        "",
        "dn: cn=Kim Wilde, ou=system",
        "objectClass: top",
        "objectClass: person",
        "cn: Kim Wilde",
        "sn: Wilde",
        "telephoneNumber: 1 801 555 1212 ",
        "description: an American singer-songwriter",
        "description: She has blond hair",
        "",
        "dn: cn=with-dn, ou=system",
        "objectClass: top",
        "objectClass: person",
        "objectClass: organizationalPerson",
        "objectClass: inetorgPerson",
        "cn: singer",
        "sn: manager",
        "telephoneNumber: 1 801 555 1212 ",
        "manager: cn=Heather Nova, ou=system"
    }
)
public class ModifyDelIT extends AbstractLdapTestUnit
{
    private static final String RDN_HEATHER_NOVA = "cn=Heather Nova";
    private static final String RDN_KIM_WILDE = "cn=kim wilde";

    
    /**
     * @param sysRoot the system root to add entries to
     * @throws NamingException on errors
     */
    protected void createData( LdapContext sysRoot ) throws Exception
    {
        /*
         * Check ou=testing00,ou=system
         */
        DirContext ctx = ( DirContext ) sysRoot.lookup( "ou=testing00" );
        assertNotNull( ctx );
        Attributes attributes = ctx.getAttributes( "" );
        assertNotNull( attributes );
        assertEquals( "testing00", attributes.get( "ou" ).get() );
        Attribute attribute = attributes.get( "objectClass" );
        assertNotNull( attribute );
        assertTrue( attribute.contains( "top" ) );
        assertTrue( attribute.contains( "organizationalUnit" ) );

        /*
         * check ou=testing01,ou=system
         */
        ctx = ( DirContext ) sysRoot.lookup( "ou=testing01" );
        assertNotNull( ctx );
        attributes = ctx.getAttributes( "" );
        assertNotNull( attributes );
        assertEquals( "testing01", attributes.get( "ou" ).get() );
        attribute = attributes.get( "objectClass" );
        assertNotNull( attribute );
        assertTrue( attribute.contains( "top" ) );
        assertTrue( attribute.contains( "organizationalUnit" ) );

        /*
         * Check ou=testing02,ou=system
         */
        ctx = ( DirContext ) sysRoot.lookup( "ou=testing02" );
        assertNotNull( ctx );

        attributes = ctx.getAttributes( "" );
        assertNotNull( attributes );
        assertEquals( "testing02", attributes.get( "ou" ).get() );

        attribute = attributes.get( "objectClass" );
        assertNotNull( attribute );
        assertTrue( attribute.contains( "top" ) );
        assertTrue( attribute.contains( "organizationalUnit" ) );

        /*
         * Check ou=subtest,ou=testing01,ou=system
         */
        ctx = ( DirContext ) sysRoot.lookup( "ou=subtest,ou=testing01" );
        assertNotNull( ctx );

        attributes = ctx.getAttributes( "" );
        assertNotNull( attributes );
        assertEquals( "subtest", attributes.get( "ou" ).get() );

        attribute = attributes.get( "objectClass" );
        assertNotNull( attribute );
        assertTrue( attribute.contains( "top" ) );
        assertTrue( attribute.contains( "organizationalUnit" ) );

        /*
         *  Check entry cn=Heather Nova, ou=system
         */
        ctx = ( DirContext ) sysRoot.lookup( RDN_HEATHER_NOVA );
        assertNotNull( ctx );


        // -------------------------------------------------------------------
        // Enable the nis schema
        // -------------------------------------------------------------------

        // check if nis is disabled
        LdapContext schemaRoot = getSchemaContext( service );
        Attributes nisAttrs = schemaRoot.getAttributes( "cn=nis" );
        boolean isNisDisabled = false;
        
        if ( nisAttrs.get( "m-disabled" ) != null )
        {
            isNisDisabled = ( ( String ) nisAttrs.get( "m-disabled" ).get() ).equalsIgnoreCase( "TRUE" );
        }

        // if nis is disabled then enable it
        if ( isNisDisabled )
        {
            Attribute disabled = new BasicAttribute( "m-disabled" );
            ModificationItem[] mods = new ModificationItem[] {
                new ModificationItem( DirContext.REMOVE_ATTRIBUTE, disabled ) };
            schemaRoot.modifyAttributes( "cn=nis", mods );
        }

        // -------------------------------------------------------------------
        // Add a bunch of nis groups
        // -------------------------------------------------------------------
        addNisPosixGroup( "testGroup0", 0 );
        addNisPosixGroup( "testGroup1", 1 );
        addNisPosixGroup( "testGroup2", 2 );
        addNisPosixGroup( "testGroup4", 4 );
        addNisPosixGroup( "testGroup5", 5 );
        
        // Create a test account
        Attributes test = new BasicAttributes( true );
        Attribute oc = new BasicAttribute( "ObjectClass" );
        oc.add( "top" );
        oc.add( "account" );
        oc.add( "posixAccount" );
        test.put( oc );
        
        test.put( "cn", "test" );
        test.put( "uid", "1" );
        test.put( "uidNumber", "1" );
        test.put( "gidNumber", "1" );
        test.put( "homeDirectory", "/" );
        test.put( "description", "A test account" );
        
        getSystemContext( service ).createSubcontext( "cn=test", test );
        
    }


    /**
     * Create a NIS group
     */
    private DirContext addNisPosixGroup( String name, int gid ) throws Exception
    {
        Attributes attrs = new BasicAttributes( "objectClass", "top", true );
        attrs.get( "objectClass" ).add( "posixGroup" );
        attrs.put( "cn", name );
        attrs.put( "gidNumber", String.valueOf( gid ) );
        return getSystemContext( service ).createSubcontext( "cn="+name+",ou=groups", attrs );
    }


    //---------------------------------------------------------------------------------------------
    // Del operation
    //---------------------------------------------------------------------------------------------
    // 1 Entry exists
    //  1.1 AT exists
    //   - The Value exists, it's not part of the Rdn, the AT is not singleValued
    //   - The Value exists, it's not part of the Rdn, the AT is singleValued, the AT is not in MUST
    //   - The Value exists, it's not part of the Rdn, the AT is singleValued, the AT is in MUST => error
    //   - The Value exists, it's part of the Rdn => error
    //   - The Value does not exists => error
    //   - Delete all the values, AT is not in MUST => AT must be removed
    //   - Delete all the values, AT is in MUST => error
    //---------------------------------------------------------------------------------------------
    /**
     * Delete a value from an existing AT. There are more than one value
     */
    @Test
    public void testModifyDelExistingEntryExistingATNotInRdnNotSV() throws Exception
    {
        LdapContext sysRoot = getSystemContext( service );
        createData( sysRoot );

        // A new description attribute value
        String deletedValue = "she has blond hair";

        Attributes attrs = new BasicAttributes( "description", deletedValue, true );

        sysRoot.modifyAttributes( RDN_KIM_WILDE, DirContext.REMOVE_ATTRIBUTE, attrs );

        // Verify that the attribute value has been removed
        attrs = sysRoot.getAttributes( RDN_KIM_WILDE );
        Attribute attr = attrs.get( "description" );
        assertNotNull( attr );
        assertTrue( attr.contains( "an American singer-songwriter" ) );
        assertFalse( attr.contains( deletedValue ) );
        assertEquals( 1, attr.size() );
    }


    /**
     * Delete all the values from an existing AT not in MUST
     */
    @Test
    public void testModifyDelExistingEntryExistingATNotInRdnNotInMustNotSVAllValues() throws Exception
    {
        LdapContext sysRoot = getSystemContext( service );
        createData( sysRoot );

        // A new description attribute value
        Attributes attrs = new BasicAttributes( "description", true );
        Attribute descr = new BasicAttribute( "description" );
        descr.add( "an American singer-songwriter" );
        descr.add( "she has blond hair" );
        attrs.put( descr );
        
        sysRoot.modifyAttributes( RDN_KIM_WILDE, DirContext.REMOVE_ATTRIBUTE, attrs );

        // Verify that the attribute value has been removed
        attrs = sysRoot.getAttributes( RDN_KIM_WILDE );
        Attribute attr = attrs.get( "description" );
        assertNull( attr );
    }
    
    
    /**
     * Delete all the values from an existing AT in MUST
     */
    @Test( expected = SchemaViolationException.class )
    public void testModifyDelExistingEntryExistingATNotInRdnNotSVAllValues() throws Exception
    {
        LdapContext sysRoot = getSystemContext( service );
        createData( sysRoot );

        Attributes sn = new BasicAttributes( "sn", "Wilde", true );
        
        sysRoot.modifyAttributes( RDN_KIM_WILDE, DirContext.REMOVE_ATTRIBUTE, sn );
    }

    
    //---------------------------------------------------------------------------------------------
    // 1 Entry exists
    //  1.2 AT does not exists.
    //---------------------------------------------------------------------------------------------
    /**
     * Remove a non existing AT from an entry, the AT is part of MAY/MUST
     */
    @Test( expected = NoSuchAttributeException.class )
    public void testModifyDelExistingEntryNonExistingATInMay() throws Exception
    {
        LdapContext sysRoot = getSystemContext( service );
        createData( sysRoot );

        // A non existing AT 
        Attributes attrs = new BasicAttributes( "seeAlso", "cn=test", true );

        sysRoot.modifyAttributes( RDN_HEATHER_NOVA, DirContext.REMOVE_ATTRIBUTE, attrs );
    }
    
    
    /**
     * Remove a non existing AT from an entry, the AT is not part of MAY/MUST
     */
    @Test( expected = NoSuchAttributeException.class )
    public void testModifyDelExistingEntryNonExistingATNotInMayMust() throws Exception
    {
        LdapContext sysRoot = getSystemContext( service );
        createData( sysRoot );

        // A non existing AT 
        Attributes attrs = new BasicAttributes( "c", "FR", true );

        sysRoot.modifyAttributes( RDN_HEATHER_NOVA, DirContext.REMOVE_ATTRIBUTE, attrs );
    }
    
    
    /**
     * Delete a value from an existing SingleValued AT, not in MUST, not in Rdn
     */
    @Test
    public void testModifyDelExistingEntryExistingATNotInRdnSV() throws Exception
    {
        LdapContext sysRoot = getSystemContext( service );
        createData( sysRoot );

        Attributes attrs = new BasicAttributes( "c", "FR", true );

        sysRoot.modifyAttributes( "ou=testing02", DirContext.REMOVE_ATTRIBUTE, attrs );

        // Verify that the attribute value has been removed
        attrs = sysRoot.getAttributes( "ou=testing02" );
        Attribute country = attrs.get( "c" );
        assertNull( country );
    }
    
    
    /**
     * Delete a value from an existing SingleValued AT, in MUST, not in Rdn
     */
    @Test( expected = SchemaViolationException.class )
    public void testModifyDelExistingEntryExistingATNotInRdnSVInMust() throws Exception
    {
        LdapContext sysRoot = getSystemContext( service );
        createData( sysRoot );

        Attributes attrs = new BasicAttributes( "uidNumber", "1", true );

        sysRoot.modifyAttributes( "cn=test", DirContext.REMOVE_ATTRIBUTE, attrs );
    }
    
    
    /**
     * Delete a value part of the Rdn
     */
    @Test( expected = SchemaViolationException.class )
    public void testModifyDelExistingEntryExistingATPartOfRdn() throws Exception
    {
        LdapContext sysRoot = getSystemContext( service );
        createData( sysRoot );

        Attributes attrs = new BasicAttributes( "cn", "test", true );

        sysRoot.modifyAttributes( "cn=test", DirContext.REMOVE_ATTRIBUTE, attrs );
    }
    
    
    /**
     * Delete an existing AT not part of the Rdn, not in MUST
     */
    @Test
    public void testModifyDelExistingEntryExistingATNoInRdnNotInMust() throws Exception
    {
        LdapContext sysRoot = getSystemContext( service );
        createData( sysRoot );

        Attributes attrs = new BasicAttributes( "description", null, true );

        sysRoot.modifyAttributes( RDN_HEATHER_NOVA, DirContext.REMOVE_ATTRIBUTE, attrs );
        
        // Verify that the attribute has been removed
        attrs = sysRoot.getAttributes( RDN_HEATHER_NOVA );
        Attribute descr = attrs.get( "description" );
        assertNull( descr );
    }
    
    
    /**
     * Delete an existing AT not part of the Rdn, but in MUST
     */
    @Test( expected = SchemaViolationException.class )
    public void testModifyDelExistingEntryExistingATNoInRdnInMust() throws Exception
    {
        LdapContext sysRoot = getSystemContext( service );
        createData( sysRoot );

        Attributes attrs = new BasicAttributes( "sn", null, true );

        sysRoot.modifyAttributes( RDN_HEATHER_NOVA, DirContext.REMOVE_ATTRIBUTE, attrs );
    }
    
    
    /**
     * Delete an existing AT part of the Rdn
     */
    @Test( expected = SchemaViolationException.class )
    public void testModifyDelExistingEntryExistingATInRdn() throws Exception
    {
        LdapContext sysRoot = getSystemContext( service );
        createData( sysRoot );

        Attributes attrs = new BasicAttributes( "cn", null, true );

        sysRoot.modifyAttributes( RDN_HEATHER_NOVA, DirContext.REMOVE_ATTRIBUTE, attrs );
    }
    
    
    /**
     * Delete a value not present in an existing AT
     */
    @Test( expected = NoSuchAttributeException.class )
    public void testModifyDelExistingEntryValueNotPresentInExistingAT() throws Exception
    {
        LdapContext sysRoot = getSystemContext( service );
        createData( sysRoot );

        Attributes attrs = new BasicAttributes( "description", "Not present", true );

        sysRoot.modifyAttributes( RDN_HEATHER_NOVA, DirContext.REMOVE_ATTRIBUTE, attrs );
    }

    //---------------------------------------------------------------------------------------------
    // 1 Entry exists
    //  1.3 Entry is an alias
    //---------------------------------------------------------------------------------------------
    
    
    //---------------------------------------------------------------------------------------------
    // 1 Entry exists
    //  1.4 Entry is a referral.
    //---------------------------------------------------------------------------------------------
    
    
    //---------------------------------------------------------------------------------------------
    // 1 Entry exists
    //  1.2 Entry is a schema element.
    //---------------------------------------------------------------------------------------------
    
    
    //---------------------------------------------------------------------------------------------
    // 1 Entry exists
    //  1.2 The added AT is ObjectClass.
    //---------------------------------------------------------------------------------------------
    
    
    //---------------------------------------------------------------------------------------------
    // 2 Entry does not exist
    //---------------------------------------------------------------------------------------------
    /**
     * Del an AT in an entry which does not exist
     */
    @Test( expected = NameNotFoundException.class )
    public void testModifyDelNotExistingEntry() throws Exception
    {
        LdapContext sysRoot = getSystemContext( service );
        createData( sysRoot );

        // An operational attribute
        Attributes attrs = new BasicAttributes( "cn", "test", true );

        sysRoot.modifyAttributes( "ou=absent", DirContext.REMOVE_ATTRIBUTE, attrs );
    }
}
