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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import javax.naming.NameNotFoundException;
import javax.naming.NamingException;
import javax.naming.NoPermissionException;
import javax.naming.directory.Attribute;
import javax.naming.directory.AttributeInUseException;
import javax.naming.directory.Attributes;
import javax.naming.directory.BasicAttribute;
import javax.naming.directory.BasicAttributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.InvalidAttributeValueException;
import javax.naming.directory.InvalidAttributesException;
import javax.naming.directory.ModificationItem;
import javax.naming.directory.SchemaViolationException;
import javax.naming.ldap.LdapContext;

import org.apache.directory.server.core.annotations.ApplyLdifs;
import org.apache.directory.server.core.annotations.CreateDS;
import org.apache.directory.server.core.integ.AbstractLdapTestUnit;
import org.apache.directory.server.core.integ.FrameworkRunner;
import org.apache.directory.shared.util.StringConstants;
import org.junit.Test;
import org.junit.runner.RunWith;
 

/**
 * Tests the modify() methods of the provider.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
@RunWith ( FrameworkRunner.class )
@CreateDS(name = "ModifyAddIT")
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
public class ModifyAddIT extends AbstractLdapTestUnit
{
    private static final String PERSON_DESCRIPTION = "an American singer-songwriter";
    private static final String RDN_HEATHER_NOVA = "cn=Heather Nova";

    
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
    // Add operation
    //---------------------------------------------------------------------------------------------
    // 1 Entry exists
    //  1.1 AT does not exist.
    //    - new valid Ava, new AT is in MAY
    //    - new valid Ava, new AT is not in MAY => error
    //    - new valid Ava, new AT is not in MAY, but OC contains extensibleOC
    //    - new valid Ava, new AT is not in MAY, but OC contains extensibleOC, legal empty value
    //    - new invalid Ava, not existing AT => error
    //    - new invalid Ava, existing AT, two values in a single valued AT => error
    //    - new invalid Ava, not existing AT, extensibleObject in OCs => error
    //    - new invalid Ava (Value is invalid per syntax), AT is in MAY => error
    //    - new invalid Ava (Value is invalid per syntax), AT is not in MAY, but OC contains extensibleOC => error
    //    - new OperationalAttribute => error
    //    - new OperationalAttribute, OC contains extensibleOC => error
    //---------------------------------------------------------------------------------------------
    /**
     * Add a new AT with a valid Value in the entry, the AT is part of the MAY
     */
    @Test
    public void testModifyAddExistingEntryNotExistingATValidAVA() throws Exception
    {
        LdapContext sysRoot = getSystemContext( service );
        createData( sysRoot );

        // A new description attribute value
        String newValue = "ou=test";

        Attributes attrs = new BasicAttributes( "seeAlso", newValue, true );

        sysRoot.modifyAttributes( RDN_HEATHER_NOVA, DirContext.ADD_ATTRIBUTE, attrs );

        // Verify that the attribute value has been added
        attrs = sysRoot.getAttributes( RDN_HEATHER_NOVA );
        Attribute attr = attrs.get( "seeAlso" );
        assertNotNull( attr );
        assertTrue( attr.contains( newValue ) );
        assertEquals( 1, attr.size() );
    }


    /**
     * Add a new AT with a valid Value in the entry, the AT is not part of the MAY or MUST,
     * and the OC does not contain the extensibleObject OC
     */
    @Test( expected = SchemaViolationException.class )
    public void testModifyAddExistingEntryNotExistingATNotInMayValidAVA() throws Exception
    {
        LdapContext sysRoot = getSystemContext( service );
        createData( sysRoot );

        // A valid AT not in MUST or MAY
        Attributes attrs = new BasicAttributes( "crossCertificatePair", "12345", true );

        sysRoot.modifyAttributes( RDN_HEATHER_NOVA, DirContext.ADD_ATTRIBUTE, attrs );
    }


    /**
     * Add a new AT with a valid Value in the entry, the AT is not part of the MAY or MUST,
     * and the OC contains the extensibleObject OC
     */
    @Test
    public void testModifyAddExistingEntryNotExistingATNotInMayExtensibleObjectOCValidAVA() throws Exception
    {
        LdapContext sysRoot = getSystemContext( service );
        createData( sysRoot );

        // A valid AT not in MUST or MAY, but the extensibleObject OC is present in the OCs
        Attributes attrs = new BasicAttributes( "crossCertificatePair", "12345", true );

        // Add the Ava
        sysRoot.modifyAttributes( "ou=testing01", DirContext.ADD_ATTRIBUTE, attrs );

        // Verify that the attribute value has been added
        attrs = sysRoot.getAttributes( "ou=testing01" );
        Attribute attr = attrs.get( "crossCertificatePair" );
        assertNotNull( attr );
        assertTrue( attr.contains( "12345".getBytes() ) );
        assertEquals( 1, attr.size() );
    }


    /**
     * Add a new AT with an empty Value in the entry, the AT is not part of the MAY or MUST,
     * and the OC contains the extensibleObject OC
     */
    @Test
    public void testModifyAddExistingEntryNotExistingAtEmptyValue() throws Exception
    {
        LdapContext sysRoot = getSystemContext( service );
        createData( sysRoot );

        // A valid AT not in MUST or MAY, but the extensibleObject OC is present in the OCs
        // The value is empty
        Attributes attrs = new BasicAttributes( "crossCertificatePair", StringConstants.EMPTY_BYTES, true );

        // Add the Ava
        sysRoot.modifyAttributes( "ou=testing01", DirContext.ADD_ATTRIBUTE, attrs );

        // Verify that the attribute value has been added
        attrs = sysRoot.getAttributes( "ou=testing01" );
        Attribute attr = attrs.get( "crossCertificatePair" );
        assertNotNull( attr );
        assertTrue( attr.contains( StringConstants.EMPTY_BYTES ) );
        assertEquals( 1, attr.size() );
    }


    /**
     * Add a new single valued AT with 2 Values in the entry
     */
    @Test( expected = InvalidAttributeValueException.class )
    public void testModifyAddExistingEntrySingleValuedATWithTwoValues() throws Exception
    {
        LdapContext sysRoot = getSystemContext( service );
        createData( sysRoot );

        // 
        Attribute attr = new BasicAttribute( "c" );
        attr.add( "FR" );
        attr.add( "US" );
        Attributes attrs = new BasicAttributes( "c", true );
        attrs.put( attr );

        // Add the Ava
        sysRoot.modifyAttributes( "ou=testing01", DirContext.ADD_ATTRIBUTE, attrs );
    }

    
    /**
     * Add a bad AT in the entry, the OC does not contain the extensibleObject OC
     */
    @Test( expected = InvalidAttributesException.class )
    public void testModifyAddExistingEntryNotExistingATInvalidAVA() throws Exception
    {
        LdapContext sysRoot = getSystemContext( service );
        createData( sysRoot );

        // An invalid AT
        Attributes attrs = new BasicAttributes( "badAttr", "12345", true );

        sysRoot.modifyAttributes( RDN_HEATHER_NOVA, DirContext.ADD_ATTRIBUTE, attrs );
    }


    /**
     * Add a bad AT in the entry, the OC contains the extensibleObject OC
     */
    @Test( expected = InvalidAttributesException.class )
    public void testModifyAddExistingEntryNotExistingATInvalidAVAExtensibleObjectInOcs() throws Exception
    {
        LdapContext sysRoot = getSystemContext( service );
        createData( sysRoot );

        // An invalid AT
        Attributes attrs = new BasicAttributes( "badAttr", "12345", true );

        sysRoot.modifyAttributes( "ou=testing01", DirContext.ADD_ATTRIBUTE, attrs );
    }
    

    /**
     * Add a AT part of the MAY/MUST, with an invalid value
     */
    @Test( expected = InvalidAttributeValueException.class )
    public void testModifyAddExistingEntryExistingATInvalidValue() throws Exception
    {
        LdapContext sysRoot = getSystemContext( service );
        createData( sysRoot );

        // An invalid AT value
        Attributes attrs = new BasicAttributes( "seeAlso", "AAA", true );

        sysRoot.modifyAttributes( RDN_HEATHER_NOVA, DirContext.ADD_ATTRIBUTE, attrs );
    }
    
    
    /**
     * Add a AT not part of the MAY/MUST, with an invalid value, in an entry with the 
     * extensibleObject OC 
     */
    @Test( expected = InvalidAttributeValueException.class )
    public void testModifyAddExistingEntryExistingATInvalidValueExtensibleObjectInOcs() throws Exception
    {
        LdapContext sysRoot = getSystemContext( service );
        createData( sysRoot );

        // An invalid AT value
        Attributes attrs = new BasicAttributes( "mobile", "AAA", true );

        sysRoot.modifyAttributes( "ou=testing01", DirContext.ADD_ATTRIBUTE, attrs );
    }
    
    
    /**
     * Add an operational AT in an entry with no extensibleObject OC
     */
    @Test( expected = NoPermissionException.class )
    public void testModifyAddExistingEntryOperationalAttribute() throws Exception
    {
        LdapContext sysRoot = getSystemContext( service );
        createData( sysRoot );

        // An operationalAttribute
        Attributes attrs = new BasicAttributes( "subschemaSubentry", "cn=anotherSchema", true );

        sysRoot.modifyAttributes( RDN_HEATHER_NOVA, DirContext.ADD_ATTRIBUTE, attrs );
    }
    
    
    /**
     * Add an operational AT in an entry the extensibleObject OC
     */
    @Test( expected = NoPermissionException.class )
    public void testModifyAddExistingEntryOperationalAttributeExtensibleObjectInOcs() throws Exception
    {
        LdapContext sysRoot = getSystemContext( service );
        createData( sysRoot );

        // An operational attribute
        Attributes attrs = new BasicAttributes( "subschemaSubentry", "cn=anotherSchema", true );

        sysRoot.modifyAttributes( "ou=testing01", DirContext.ADD_ATTRIBUTE, attrs );
    }
    
    
    //---------------------------------------------------------------------------------------------
    // 1 Entry exists
    //  1.2 AT exists.
    //---------------------------------------------------------------------------------------------
    /**
     * Add a new AT with a valid Value in the entry, the AT is part of the MAY
     */
    @Test
    public void testModifyAddExistingEntryExistingATValidAVA() throws Exception
    {
        LdapContext sysRoot = getSystemContext( service );
        createData( sysRoot );

        // A new description attribute value
        String newValue = "test";

        Attributes attrs = new BasicAttributes( "description", newValue, true );

        sysRoot.modifyAttributes( RDN_HEATHER_NOVA, DirContext.ADD_ATTRIBUTE, attrs );

        // Verify that the attribute value has been added
        attrs = sysRoot.getAttributes( RDN_HEATHER_NOVA );
        Attribute attr = attrs.get( "description" );
        assertNotNull( attr );
        assertTrue( attr.contains( newValue ) );
        assertTrue( attr.contains( PERSON_DESCRIPTION ) );
        assertEquals( 2, attr.size() );
    }

    
    /**
     * Add a new AT with a valid Value in the entry, the AT is part of the MAY,
     * the value already exists
     */
    @Test( expected = AttributeInUseException.class )
    public void testModifyAddExistingEntryExistingATExistingValue() throws Exception
    {
        LdapContext sysRoot = getSystemContext( service );
        createData( sysRoot );

        Attributes attrs = new BasicAttributes( "description", PERSON_DESCRIPTION, true );

        sysRoot.modifyAttributes( RDN_HEATHER_NOVA, DirContext.ADD_ATTRIBUTE, attrs );
    }
    
    
    /**
     * Add an empty value in an existing AT in the entry, the AT is not part of the MAY or MUST,
     * and the OC contains the extensibleObject OC
     */
    @Test
    public void testModifyAddExistingEntryExistingAtEmptyValue() throws Exception
    {
        LdapContext sysRoot = getSystemContext( service );
        createData( sysRoot );

        Attributes attrs = new BasicAttributes( "crossCertificatePair", "12345".getBytes(), true );
        
        // Add the first Ava
        sysRoot.modifyAttributes( "ou=testing01", DirContext.ADD_ATTRIBUTE, attrs );

        attrs = new BasicAttributes( "crossCertificatePair", StringConstants.EMPTY_BYTES, true );
        
        // Add the second Ava
        sysRoot.modifyAttributes( "ou=testing01", DirContext.ADD_ATTRIBUTE, attrs );

        // Verify that the attribute value has been added
        attrs = sysRoot.getAttributes( "ou=testing01" );
        Attribute attr = attrs.get( "crossCertificatePair" );
        assertNotNull( attr );
        assertTrue( attr.contains( "12345".getBytes() ) );
        assertTrue( attr.contains( StringConstants.EMPTY_BYTES ) );
        assertEquals( 2, attr.size() );
    }
    
    
    /**
     * Add a new value in a single valued AT
     */
    @Test( expected = InvalidAttributeValueException.class )
    public void testModifyAddExistingEntryExistingSingleValuedAT() throws Exception
    {
        LdapContext sysRoot = getSystemContext( service );
        createData( sysRoot );

        // The initial value
        Attributes attrs = new BasicAttributes( "c", "FR", true );

        // Add the Ava
        sysRoot.modifyAttributes( "ou=testing01", DirContext.ADD_ATTRIBUTE, attrs );
        
        // Add another value
        Attributes attrs2 = new BasicAttributes( "c", "US", true );

        // Add the Ava
        sysRoot.modifyAttributes( "ou=testing01", DirContext.ADD_ATTRIBUTE, attrs2 );
    }
    
    
    /**
     * Add the existing value in a single valued AT
     */
    @Test( expected = AttributeInUseException.class )
    public void testModifyAddExistingEntryExistingSingleValuedATExistingValue() throws Exception
    {
        LdapContext sysRoot = getSystemContext( service );
        createData( sysRoot );

        // The initial value
        Attributes attrs = new BasicAttributes( "c", "FR", true );

        // Add the Ava
        sysRoot.modifyAttributes( "ou=testing01", DirContext.ADD_ATTRIBUTE, attrs );
        
        // Add another value
        Attributes attrs2 = new BasicAttributes( "c", "FR", true );

        // Add the Ava
        sysRoot.modifyAttributes( "ou=testing01", DirContext.ADD_ATTRIBUTE, attrs2 );
    }
    
    
    /**
     * Add an invalue in a existing AT
     */
    @Test( expected = InvalidAttributeValueException.class )
    public void testModifyAddExistingEntryExistingATBadValue() throws Exception
    {
        LdapContext sysRoot = getSystemContext( service );
        createData( sysRoot );

        // The added value
        Attributes attrs = new BasicAttributes( "telephoneNumber", "BAD", true );

        // Add the Ava
        sysRoot.modifyAttributes( "ou=testing01", DirContext.ADD_ATTRIBUTE, attrs );
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
     * Add an AT in an entry which does not exist
     */
    @Test( expected = NameNotFoundException.class )
    public void testModifyAddNotExistingEntry() throws Exception
    {
        LdapContext sysRoot = getSystemContext( service );
        createData( sysRoot );

        // An operational attribute
        Attributes attrs = new BasicAttributes( "cn", "test", true );

        sysRoot.modifyAttributes( "ou=absent", DirContext.ADD_ATTRIBUTE, attrs );
    }
}
