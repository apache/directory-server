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
package org.apache.directory.server.operations.modify;


import static org.apache.directory.server.integ.ServerIntegrationUtils.getWiredContext;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Arrays;

import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.NoPermissionException;
import javax.naming.directory.Attribute;
import javax.naming.directory.AttributeInUseException;
import javax.naming.directory.AttributeModificationException;
import javax.naming.directory.Attributes;
import javax.naming.directory.BasicAttribute;
import javax.naming.directory.BasicAttributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.InvalidAttributeValueException;
import javax.naming.directory.ModificationItem;
import javax.naming.directory.NoSuchAttributeException;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;

import org.apache.directory.server.annotations.CreateLdapServer;
import org.apache.directory.server.annotations.CreateTransport;
import org.apache.directory.server.core.annotations.ApplyLdifs;
import org.apache.directory.server.core.integ.AbstractLdapTestUnit;
import org.apache.directory.server.core.integ.FrameworkRunner;
import org.apache.directory.shared.ldap.model.exception.LdapException;
import org.apache.directory.shared.ldap.model.ldif.LdifUtils;
import org.junit.Test;
import org.junit.runner.RunWith;


/**
 * Test case with different modify operations on a person entry. Each includes a
 * single add op only. Created to demonstrate DIREVE-241 ("Adding an already
 * existing attribute value with a modify operation does not cause an error.").
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
@RunWith ( FrameworkRunner.class )
@ApplyLdifs( {
    // Entry # 1
    "dn: cn=Tori Amos,ou=system",
    "objectClass: inetOrgPerson",
    "objectClass: organizationalPerson",
    "objectClass: person",
    "objectClass: top",
    "description: an American singer-songwriter",
    "cn: Tori Amos",
    "sn: Amos",
    // Entry # 2
    "dn: cn=Debbie Harry,ou=system",
    "objectClass: inetOrgPerson",
    "objectClass: organizationalPerson",
    "objectClass: person",
    "objectClass: top",
    "cn: Debbie Harry",
    "sn: Harry"
    }
)
//@CreateDS( allowAnonAccess=true, name="BindIT-class")
@CreateLdapServer (
    transports =
    {
        @CreateTransport( protocol = "LDAP" )
    })
public class ModifyAddIT  extends AbstractLdapTestUnit
{
    private static final String BASE = "ou=system";
    private static final String RDN_TORI_AMOS = "cn=Tori Amos";
    private static final String PERSON_DESCRIPTION = "an American singer-songwriter";
    private static final String RDN_DEBBIE_HARRY = "cn=Debbie Harry";


    /**
     * Creation of required attributes of a person entry.
     */
    protected Attributes getPersonAttributes( String sn, String cn ) throws LdapException
    {
        Attributes attributes = LdifUtils.createAttributes(
            "objectClass: top",
            "objectClass: person",
            "objectClass: organizationalperson",
            "objectClass: inetorgperson",
            "cn", cn,
            "sn", sn );

        return attributes;
    }


    /**
     * Add a new attribute to a person entry.
     */
    @Test
    public void testAddNewAttributeValue() throws Exception
    {
        DirContext ctx = ( DirContext ) getWiredContext( ldapServer ).lookup( BASE );

        // Add telephoneNumber attribute
        String newValue = "1234567890";
        Attributes attrs = new BasicAttributes( "telephoneNumber", newValue, true );
        ctx.modifyAttributes( RDN_TORI_AMOS, DirContext.ADD_ATTRIBUTE, attrs );

        // Verify, that
        // - case of attribute description is correct
        // - attribute value is added
        attrs = ctx.getAttributes( RDN_TORI_AMOS );
        Attribute attr = attrs.get( "telephoneNumber" );
        assertNotNull( attr );
        assertEquals( "telephoneNumber", attr.getID() );
        assertTrue( attr.contains( newValue ) );
        assertEquals( 1, attr.size() );
    }


    /**
     * Add a new attribute with two values.
     */
    @Test
    public void testAddNewAttributeValues() throws Exception
    {
        DirContext ctx = ( DirContext ) getWiredContext( ldapServer ).lookup( BASE );

        // Add telephoneNumber attribute
        String[] newValues =
            { "1234567890", "999999999" };
        Attribute attr = new BasicAttribute( "telephoneNumber" );
        attr.add( newValues[0] );
        attr.add( newValues[1] );
        Attributes attrs = new BasicAttributes( true );
        attrs.put( attr );
        ctx.modifyAttributes( RDN_TORI_AMOS, DirContext.ADD_ATTRIBUTE, attrs );

        // Verify, that
        // - case of attribute description is correct
        // - attribute values are present
        attrs = ctx.getAttributes( RDN_TORI_AMOS );
        attr = attrs.get( "telephoneNumber" );
        assertNotNull( attr );
        assertEquals( "telephoneNumber", attr.getID() );
        assertTrue( attr.contains( newValues[0] ) );
        assertTrue( attr.contains( newValues[1] ) );
        assertEquals( newValues.length, attr.size() );
    }


    /**
     * Add an additional value.
     */
    @Test
    public void testAddAdditionalAttributeValue() throws Exception
    {
        DirContext ctx = ( DirContext ) getWiredContext( ldapServer ).lookup( BASE );

        // A new description attribute value
        String newValue = "A new description for this person";
        assertFalse( newValue.equals( PERSON_DESCRIPTION ) );
        Attributes attrs = new BasicAttributes( "description", newValue, true );

        ctx.modifyAttributes( RDN_TORI_AMOS, DirContext.ADD_ATTRIBUTE, attrs );

        // Verify, that attribute value is added
        attrs = ctx.getAttributes( RDN_TORI_AMOS );
        Attribute attr = attrs.get( "description" );
        assertNotNull( attr );
        assertTrue( attr.contains( newValue ) );
        assertTrue( attr.contains( PERSON_DESCRIPTION ) );
        assertEquals( 2, attr.size() );
    }


    /**
     * Try to add an already existing attribute value.
     *
     * Expected behaviour: Modify operation fails with an
     * AttributeInUseException. Original LDAP Error code: 20 (Indicates that the
     * attribute value specified in a modify or add operation already exists as
     * a value for that attribute).
     */
    @Test
    public void testAddExistingAttributeValue() throws Exception
    {
        DirContext ctx = ( DirContext ) getWiredContext( ldapServer ).lookup( BASE );

        // Change description attribute
        Attributes attrs = new BasicAttributes( "description", PERSON_DESCRIPTION, true );

        try
        {
            ctx.modifyAttributes( RDN_TORI_AMOS, DirContext.ADD_ATTRIBUTE, attrs );
            fail( "Adding an already existing atribute value should fail." );
        }
        catch ( AttributeInUseException e )
        {
            // expected behaviour
        }

        // Verify, that attribute is still there, and is the only one
        attrs = ctx.getAttributes( RDN_TORI_AMOS );
        Attribute attr = attrs.get( "description" );
        assertNotNull( attr );
        assertTrue( attr.contains( PERSON_DESCRIPTION ) );
        assertEquals( 1, attr.size() );
    }


    /**
     * Try to add an already existing attribute value.
     *
     * Expected behaviour: Modify operation fails with an
     * AttributeInUseException. Original LDAP Error code: 20 (Indicates that the
     * attribute value specified in a modify or add operation already exists as
     * a value for that attribute).
     *
     * Check for bug DIR_SERVER664
     */
    @Test
    public void testAddExistingNthAttributesDirServer664() throws Exception
    {
        DirContext ctx = ( DirContext ) getWiredContext( ldapServer ).lookup( BASE );

        // Change description attribute
        Attributes attrs = new BasicAttributes( true );
        attrs.put( new BasicAttribute( "telephoneNumber", "1" ) );
        attrs.put( new BasicAttribute( "telephoneNumber", "2" ) );
        attrs.put( new BasicAttribute( "telephoneNumber", "3" ) );
        attrs.put( new BasicAttribute( "telephoneNumber", "4" ) );
        attrs.put( new BasicAttribute( "telephoneNumber", "5" ) );
        attrs.put( new BasicAttribute( "telephoneNumber", "6" ) );
        attrs.put( new BasicAttribute( "telephoneNumber", "7" ) );
        attrs.put( new BasicAttribute( "telephoneNumber", "8" ) );
        attrs.put( new BasicAttribute( "telephoneNumber", "9" ) );
        attrs.put( new BasicAttribute( "telephoneNumber", "10" ) );
        attrs.put( new BasicAttribute( "telephoneNumber", "11" ) );
        attrs.put( new BasicAttribute( "telephoneNumber", "12" ) );
        attrs.put( new BasicAttribute( "telephoneNumber", "13" ) );
        attrs.put( new BasicAttribute( "telephoneNumber", "14" ) );

        Attribute attr = new BasicAttribute( "description", PERSON_DESCRIPTION );

        attrs.put( attr );

        try
        {
            ctx.modifyAttributes( RDN_TORI_AMOS, DirContext.ADD_ATTRIBUTE, attrs );
            fail( "Adding an already existing atribute value should fail." );
        }
        catch ( AttributeInUseException e )
        {
            // expected behaviour
        }

        // Verify, that attribute is still there, and is the only one
        attrs = ctx.getAttributes( RDN_TORI_AMOS );
        attr = attrs.get( "description" );
        assertNotNull( attr );
        assertTrue( attr.contains( PERSON_DESCRIPTION ) );
        assertEquals( 1, attr.size() );
    }


    /**
     * Check for DIR_SERVER_643
     */
    @Test
    public void testTwoDescriptionDirServer643() throws Exception
    {
        DirContext ctx = ( DirContext ) getWiredContext( ldapServer ).lookup( BASE );

        // Change description attribute
        Attributes attrs = new BasicAttributes( true );
        Attribute attr = new BasicAttribute( "description", "a British singer-songwriter with an expressive four-octave voice" );
        attr.add( "one of the most influential female artists of the twentieth century" );
        attrs.put( attr );

        ctx.modifyAttributes( RDN_TORI_AMOS, DirContext.ADD_ATTRIBUTE, attrs );

        // Verify, that attribute is still there, and is the only one
        attrs = ctx.getAttributes( RDN_TORI_AMOS );
        attr = attrs.get( "description" );
        assertNotNull( attr );
        assertEquals( 3, attr.size() );
        assertTrue( attr.contains( "a British singer-songwriter with an expressive four-octave voice" ) );
        assertTrue( attr.contains( "one of the most influential female artists of the twentieth century" ) );
        assertTrue( attr.contains( PERSON_DESCRIPTION ) );
    }

    /**
     * Try to add a duplicate attribute value to an entry, where this attribute
     * is already present (objectclass in this case). Expected behaviour is that
     * the modify operation causes an error (error code 20, "Attribute or value
     * exists").
     */
    @Test
    public void testAddDuplicateValueToExistingAttribute() throws Exception
    {
        DirContext ctx = ( DirContext ) getWiredContext( ldapServer ).lookup( BASE );

        // modify object classes, add a new value twice
        Attribute ocls = new BasicAttribute( "objectClass", "organizationalPerson" );
        ModificationItem[] modItems = new ModificationItem[2];
        modItems[0] = new ModificationItem( DirContext.ADD_ATTRIBUTE, ocls );
        modItems[1] = new ModificationItem( DirContext.ADD_ATTRIBUTE, ocls );
        try
        {
            ctx.modifyAttributes( RDN_TORI_AMOS, modItems );
            fail( "Adding a duplicate attribute value should cause an error." );
        }
        catch ( AttributeInUseException ex )
        {
        }

        // Check, whether attribute objectClass is unchanged
        Attributes attrs = ctx.getAttributes( RDN_TORI_AMOS );
        ocls = attrs.get( "objectClass" );
        assertEquals( ocls.size(), 4 );
        assertTrue( ocls.contains( "top" ) );
        assertTrue( ocls.contains( "person" ) );
    }


    /**
     * Try to add a duplicate attribute value to an entry, where this attribute
     * is not present. Expected behaviour is that the modify operation causes an
     * error (error code 20, "Attribute or value exists").
     */
    @Test
    public void testAddDuplicateValueToNewAttribute() throws Exception
    {
        DirContext ctx = ( DirContext ) getWiredContext( ldapServer ).lookup( BASE );

        // add the same description value twice
        Attribute desc = new BasicAttribute( "description", "another description value besides songwriter" );
        ModificationItem[] modItems = new ModificationItem[2];
        modItems[0] = new ModificationItem( DirContext.ADD_ATTRIBUTE, desc );
        modItems[1] = new ModificationItem( DirContext.ADD_ATTRIBUTE, desc );
        try
        {
            ctx.modifyAttributes( RDN_TORI_AMOS, modItems );
            fail( "Adding a duplicate attribute value should cause an error." );
        }
        catch ( AttributeInUseException ex )
        {
        }

        // Check, whether attribute description is still not present
        Attributes attrs = ctx.getAttributes( RDN_TORI_AMOS );
        assertEquals( 1, attrs.get( "description" ).size() );
    }


    /**
     * Modify the entry with a bad attribute : this should fail
     */
    @Test
    public void testSearchBadAttribute() throws Exception
    {
        DirContext ctx = ( DirContext ) getWiredContext( ldapServer ).lookup( BASE );

        // Add a not existing attribute
        String newValue = "unbelievable";
        Attributes attrs = new BasicAttributes( "voice", newValue, true );

        try
        {
            ctx.modifyAttributes( RDN_TORI_AMOS, DirContext.ADD_ATTRIBUTE, attrs );
        }
        catch ( NoSuchAttributeException nsae )
        {
            // We have a failure : the attribute is unknown in the schema
            assertTrue( true );
            return;
        }

        fail( "Cannot reach this point" );
    }


    /**
     * Create a person entry and perform a modify op, in which
     * we modify an attribute two times.
     */
    @Test
    public void testAttributeValueMultiMofificationDIRSERVER_636() throws Exception
    {
        DirContext ctx = ( DirContext ) getWiredContext( ldapServer ).lookup( BASE );

        // Create a person entry
        Attributes attrs = getPersonAttributes("Bush", "Kate Bush");
        String rdn = "cn=Kate Bush";
        ctx.createSubcontext(rdn, attrs);

        // Add a description with two values
        String[] descriptions = {
                "Kate Bush is a British singer-songwriter.",
                "She has become one of the most influential female artists of the twentieth century." };
        Attribute desc1 = new BasicAttribute("description");
        desc1.add(descriptions[0]);
        desc1.add(descriptions[1]);

        ModificationItem addModOp = new ModificationItem(
                DirContext.ADD_ATTRIBUTE, desc1);

        Attribute desc2 = new BasicAttribute("description");
        desc2.add(descriptions[1]);
        ModificationItem delModOp = new ModificationItem(
                DirContext.REMOVE_ATTRIBUTE, desc2);

        ctx.modifyAttributes(rdn, new ModificationItem[] { addModOp,
                        delModOp });

        SearchControls sctls = new SearchControls();
        sctls.setSearchScope(SearchControls.SUBTREE_SCOPE);
        String filter = "(cn=*Bush)";
        String base = "";

        // Check entry
        NamingEnumeration<SearchResult> enm = ctx.search(base, filter, sctls);
        assertTrue(enm.hasMore());

        while (enm.hasMore()) {
            SearchResult sr = enm.next();
            attrs = sr.getAttributes();
            Attribute desc = sr.getAttributes().get("description");
            assertEquals(1, desc.size());
            assertTrue(desc.contains(descriptions[0]));
        }

        // Remove the person entry
        ctx.destroySubcontext(rdn);
    }


    /**
     * Try to add subschemaSubentry attribute to an entry
     */
    @Test
    public void testModifyOperationalAttributeAdd() throws Exception
    {
        DirContext ctx = ( DirContext ) getWiredContext( ldapServer ).lookup( BASE );

        ModificationItem modifyOp = new ModificationItem( DirContext.ADD_ATTRIBUTE, new BasicAttribute(
            "subschemaSubentry", "cn=anotherSchema" ) );

        try
        {
            ctx.modifyAttributes( RDN_DEBBIE_HARRY, new ModificationItem[]
                { modifyOp } );

            fail( "modification of entry should fail" );
        }
        catch ( InvalidAttributeValueException e )
        {
            // Expected result
        }
        catch ( NoPermissionException e )
        {
            // Expected result
        }
    }


    /**
     * Create a person entry and perform a modify op on an
     * attribute which is part of the Dn. This is not allowed.
     *
     * A JIRA has been created for this bug : DIRSERVER_687
     */
    @Test
    public void testDNAttributeMemberModificationDIRSERVER_687() throws Exception
    {
        DirContext ctx = ( DirContext ) getWiredContext( ldapServer ).lookup( BASE );

        // Create a person entry
        Attributes attrs = getPersonAttributes("Bush", "Kate Bush");
        String rdn = "cn=Kate Bush";
        ctx.createSubcontext(rdn, attrs);

        // Try to modify the cn attribute
        Attribute desc1 = new BasicAttribute( "cn", "Georges Bush" );

        ModificationItem addModOp = new ModificationItem(
                DirContext.REPLACE_ATTRIBUTE, desc1);

        try
        {
            ctx.modifyAttributes( rdn, new ModificationItem[] { addModOp } );
            fail();
        }
        catch ( AttributeModificationException ame )
        {
            assertTrue( true );
            // Remove the person entry
            ctx.destroySubcontext(rdn);
        }
        catch ( NamingException ne )
        {
            assertTrue( true );
            // Remove the person entry
            ctx.destroySubcontext(rdn);
        }
    }


    /**
     * Try to modify an entry adding invalid number of values for a single-valued atribute
     *
     * @see <a href="http://issues.apache.org/jira/browse/DIRSERVER-614">DIRSERVER-614</a>
     */
    @Test
    public void testModifyAddWithInvalidNumberOfAttributeValues() throws Exception
    {
        DirContext ctx = ( DirContext ) getWiredContext( ldapServer ).lookup( BASE );

        Attributes attrs = new BasicAttributes( true );
        Attribute ocls = new BasicAttribute( "objectClass" );
        ocls.add( "top" );
        ocls.add( "inetOrgPerson" );
        attrs.put( ocls );
        attrs.put( "cn", "Fiona Apple" );
        attrs.put( "sn", "Apple" );
        ctx.createSubcontext( "cn=Fiona Apple", attrs );

        // add two displayNames to an inetOrgPerson
        attrs = new BasicAttributes( true );
        Attribute displayName = new BasicAttribute( "displayName" );
        displayName.add( "Fiona" );
        displayName.add( "Fiona A." );
        attrs.put( displayName );

        try
        {
            ctx.modifyAttributes( "cn=Fiona Apple", DirContext.ADD_ATTRIBUTE, attrs );
            fail( "modification of entry should fail" );
        }
        catch ( InvalidAttributeValueException e )
        {

        }
    }


    /**
     * Add a new binary attribute to a person entry.
     */
    @Test
    public void testAddNewBinaryAttributeValue() throws Exception
    {
        DirContext ctx = ( DirContext ) getWiredContext( ldapServer ).lookup( BASE );

        // Add a binary attribute
        byte[] newValue = new byte[]{0x00, 0x01, 0x02, 0x03};
        Attributes attrs = new BasicAttributes( "userCertificate;binary", newValue, true );
        ctx.modifyAttributes( RDN_TORI_AMOS, DirContext.ADD_ATTRIBUTE, attrs );

        // Verify, that attribute value is added
        attrs = ctx.getAttributes( RDN_TORI_AMOS );
        Attribute attr = attrs.get( "userCertificate" );
        assertNotNull( attr );
        assertTrue( attr.contains( newValue ) );
        byte[] certificate = (byte[])attr.get();
        assertTrue( Arrays.equals( newValue, certificate ) );
        assertEquals( 1, attr.size() );
    }


    /**
     * Add a new attribute to a person entry.
     */
    @Test
    public void testAddNewBinaryAttributeValueAbove0x80() throws Exception
    {
        DirContext ctx = ( DirContext ) getWiredContext( ldapServer ).lookup( BASE );

        // Add a binary attribute
        byte[] newValue = new byte[]{(byte)0x80, (byte)0x81, (byte)0x82, (byte)0x83};
        Attributes attrs = new BasicAttributes( "userCertificate;binary", newValue, true );
        ctx.modifyAttributes( RDN_TORI_AMOS, DirContext.ADD_ATTRIBUTE, attrs );

        // Verify, that attribute value is added
        attrs = ctx.getAttributes( RDN_TORI_AMOS );
        Attribute attr = attrs.get( "userCertificate" );
        assertNotNull( attr );
        assertTrue( attr.contains( newValue ) );
        byte[] certificate = (byte[])attr.get();
        assertTrue( Arrays.equals( newValue, certificate ) );
        assertEquals( 1, attr.size() );
    }


    /**
     * Add a new binary attribute to a person entry.
     */
    @Test
    public void testRetrieveEntryWithBinaryAttributeValue() throws Exception
    {
        DirContext ctx = ( DirContext ) getWiredContext( ldapServer ).lookup( BASE );

        // Add a ;binary attribute
        byte[] newValue = new byte[]{0x00, 0x01, 0x02, 0x03};
        Attributes attrs = new BasicAttributes( "userCertificate;binary", newValue );
        ctx.modifyAttributes( RDN_TORI_AMOS, DirContext.ADD_ATTRIBUTE, attrs );

        // Search entry an request ;binary attribute
        SearchControls sctls = new SearchControls();
        sctls.setSearchScope(SearchControls.OBJECT_SCOPE);
        sctls.setReturningAttributes( new String[]{ "userCertificate;binary" } );
        String filter = "(objectClass=*)";
        String base = RDN_TORI_AMOS;

        // Test that ;binary attribute is present
        NamingEnumeration<SearchResult> enm = ctx.search( base, filter, sctls);
        assertTrue(enm.hasMore());

        while (enm.hasMore())
        {
            SearchResult sr = enm.next();
            attrs = sr.getAttributes();
            Attribute attr = attrs.get("userCertificate");
            assertNotNull(attr);
            assertTrue( attr.contains( newValue ) );
            byte[] certificate = (byte[])attr.get();
            assertTrue( Arrays.equals( newValue, certificate ) );
            assertEquals( 1, attr.size() );
        }

    }


    /**
     * Add a new ;binary attribute with bytes greater than 0x80
     * to a person entry.
     * Test for DIRSERVER-1146
     *
     * @throws NamingException
     */
    public void testAddNewBinaryAttributeValue0x80() throws Exception
    {
        DirContext ctx = ( DirContext ) getWiredContext( ldapServer ).lookup( BASE );

        // Add a ;binary attribute with high-bytes
        byte[] newValue = new byte[]{(byte)0x80, (byte)0x81, (byte)0x82, (byte)0x83};
        Attributes attrs = new BasicAttributes( "userCertificate;binary", newValue );
        ctx.modifyAttributes( RDN_TORI_AMOS, DirContext.ADD_ATTRIBUTE, attrs );

        // Verify, that attribute value is added
        attrs = ctx.getAttributes( RDN_TORI_AMOS );
        Attribute attr = attrs.get( "userCertificate" );
        assertNotNull( attr );
        assertTrue( attr.contains( newValue ) );
        byte[] certificate = (byte[])attr.get();
        assertTrue( Arrays.equals( newValue, certificate ) );
        assertEquals( 1, attr.size() );
    }}
