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


import java.util.Arrays;

import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.NoPermissionException;
import javax.naming.directory.Attribute;
import javax.naming.directory.AttributeInUseException;
import javax.naming.directory.AttributeModificationException;
import javax.naming.directory.Attributes;
import javax.naming.directory.BasicAttribute;
import javax.naming.directory.DirContext;
import javax.naming.directory.InvalidAttributeIdentifierException;
import javax.naming.directory.InvalidAttributeValueException;
import javax.naming.directory.ModificationItem;
import javax.naming.directory.NoSuchAttributeException;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;

import org.apache.directory.server.core.integ.Level;
import org.apache.directory.server.core.integ.annotations.ApplyLdifs;
import org.apache.directory.server.core.integ.annotations.CleanupLevel;
import org.apache.directory.server.integ.SiRunner;
import static org.apache.directory.server.integ.ServerIntegrationUtils.getWiredContext;

import org.apache.directory.server.ldap.LdapServer;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.fail;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.apache.directory.shared.ldap.message.AttributeImpl;
import org.apache.directory.shared.ldap.message.AttributesImpl;
import org.apache.directory.shared.ldap.message.ModificationItemImpl;


/**
 * Test case with different modify operations on a person entry. Each includes a
 * single add op only. Created to demonstrate DIREVE-241 ("Adding an already
 * existing attribute value with a modify operation does not cause an error.").
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
@RunWith ( SiRunner.class ) 
@CleanupLevel ( Level.SUITE )
@ApplyLdifs( {
    // Entry # 1
    "dn: cn=Tori Amos,ou=system\n" +
    "objectClass: inetOrgPerson\n" +
    "objectClass: organizationalPerson\n" +
    "objectClass: person\n" +
    "objectClass: top\n" +
    "description: an American singer-songwriter\n" +
    "cn: Tori Amos\n" +
    "sn: Amos\n\n" + 
    // Entry # 2
    "dn: cn=Debbie Harry,ou=system\n" +
    "objectClass: inetOrgPerson\n" +
    "objectClass: organizationalPerson\n" +
    "objectClass: person\n" +
    "objectClass: top\n" +
    "cn: Debbie Harry\n" +
    "sn: Harry\n\n" 
    }
)
public class ModifyAddIT 
{
    private static final String BASE = "ou=system";
    private static final String RDN_TORI_AMOS = "cn=Tori Amos";
    private static final String PERSON_DESCRIPTION = "an American singer-songwriter";
    private static final String RDN_DEBBIE_HARRY = "cn=Debbie Harry";

    public static LdapServer ldapServer;
    

    /**
     * Creation of required attributes of a person entry.
     */
    protected Attributes getPersonAttributes( String sn, String cn )
    {
        Attributes attributes = new AttributesImpl();
        Attribute attribute = new AttributeImpl( "objectClass" );
        attribute.add( "top" );
        attribute.add( "person" );
        attribute.add( "organizationalperson" );
        attribute.add( "inetorgperson" );
        attributes.put( attribute );
        attributes.put( "cn", cn );
        attributes.put( "sn", sn );

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
        Attributes attrs = new AttributesImpl( "telephoneNumber", newValue );
        ctx.modifyAttributes( RDN_TORI_AMOS, DirContext.ADD_ATTRIBUTE, attrs );

        // Verify, that attribute value is added
        attrs = ctx.getAttributes( RDN_TORI_AMOS );
        Attribute attr = attrs.get( "telephoneNumber" );
        assertNotNull( attr );
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
        Attribute attr = new AttributeImpl( "telephoneNumber" );
        attr.add( newValues[0] );
        attr.add( newValues[1] );
        Attributes attrs = new AttributesImpl();
        attrs.put( attr );
        ctx.modifyAttributes( RDN_TORI_AMOS, DirContext.ADD_ATTRIBUTE, attrs );

        // Verify, that attribute values are present
        attrs = ctx.getAttributes( RDN_TORI_AMOS );
        attr = attrs.get( "telephoneNumber" );
        assertNotNull( attr );
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
        Attributes attrs = new AttributesImpl( "description", newValue );

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
        Attributes attrs = new AttributesImpl( "description", PERSON_DESCRIPTION );
        
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
        Attributes attrs = new AttributesImpl( true );
        attrs.put( new AttributeImpl( "telephoneNumber", "attr 1" ) );
        attrs.put( new AttributeImpl( "telephoneNumber", "attr 2" ) );
        attrs.put( new AttributeImpl( "telephoneNumber", "attr 3" ) );
        attrs.put( new AttributeImpl( "telephoneNumber", "attr 4" ) );
        attrs.put( new AttributeImpl( "telephoneNumber", "attr 5" ) );
        attrs.put( new AttributeImpl( "telephoneNumber", "attr 6" ) );
        attrs.put( new AttributeImpl( "telephoneNumber", "attr 7" ) );
        attrs.put( new AttributeImpl( "telephoneNumber", "attr 8" ) );
        attrs.put( new AttributeImpl( "telephoneNumber", "attr 9" ) );
        attrs.put( new AttributeImpl( "telephoneNumber", "attr 10" ) );
        attrs.put( new AttributeImpl( "telephoneNumber", "attr 11" ) );
        attrs.put( new AttributeImpl( "telephoneNumber", "attr 12" ) );
        attrs.put( new AttributeImpl( "telephoneNumber", "attr 13" ) );
        attrs.put( new AttributeImpl( "telephoneNumber", "attr 14" ) );
        
        Attribute attr = new AttributeImpl( "description", PERSON_DESCRIPTION );

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
        Attributes attrs = new AttributesImpl( true );
        Attribute attr = new AttributeImpl( "description", "a British singer-songwriter with an expressive four-octave voice" );
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
        Attribute ocls = new AttributeImpl( "objectClass", "organizationalPerson" );
        ModificationItemImpl[] modItems = new ModificationItemImpl[2];
        modItems[0] = new ModificationItemImpl( DirContext.ADD_ATTRIBUTE, ocls );
        modItems[1] = new ModificationItemImpl( DirContext.ADD_ATTRIBUTE, ocls );
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
        Attribute desc = new AttributeImpl( "description", "another description value besides songwriter" );
        ModificationItemImpl[] modItems = new ModificationItemImpl[2];
        modItems[0] = new ModificationItemImpl( DirContext.ADD_ATTRIBUTE, desc );
        modItems[1] = new ModificationItemImpl( DirContext.ADD_ATTRIBUTE, desc );
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
     * Create an entry with a bad attribute : this should fail.
     */
    @Test
    public void testAddUnexistingAttribute() throws Exception
    {
        DirContext ctx = ( DirContext ) getWiredContext( ldapServer ).lookup( BASE );
        
        // Create a third person with a voice attribute
        Attributes attributes = this.getPersonAttributes( "Jackson", "Michael Jackson" );
        attributes.put( "voice", "He is bad ..." );

        try
        {
            ctx.createSubcontext( "cn=Mickael Jackson", attributes );
        }
        catch ( InvalidAttributeIdentifierException iaie )
        {
            assertTrue( true );
            return;
        }

        fail( "Should never reach this point" );
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
        Attributes attrs = new AttributesImpl( "voice", newValue );

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
        Attribute desc1 = new AttributeImpl("description");
        desc1.add(descriptions[0]);
        desc1.add(descriptions[1]);

        ModificationItemImpl addModOp = new ModificationItemImpl(
                DirContext.ADD_ATTRIBUTE, desc1);

        Attribute desc2 = new AttributeImpl("description");
        desc2.add(descriptions[1]);
        ModificationItemImpl delModOp = new ModificationItemImpl(
                DirContext.REMOVE_ATTRIBUTE, desc2);

        ctx.modifyAttributes(rdn, new ModificationItemImpl[] { addModOp,
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
        
        ModificationItem modifyOp = new ModificationItemImpl( DirContext.ADD_ATTRIBUTE, new BasicAttribute(
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
     * attribute which is part of the DN. This is not allowed.
     * 
     * A JIRA has been created for this bug : DIRSERVER_687
     */
    @Test
     public void testDNAttributeMemberMofificationDIRSERVER_687() throws Exception 
     {
         DirContext ctx = ( DirContext ) getWiredContext( ldapServer ).lookup( BASE );
         
        // Create a person entry
        Attributes attrs = getPersonAttributes("Bush", "Kate Bush");
        String rdn = "cn=Kate Bush";
        ctx.createSubcontext(rdn, attrs);

        // Try to modify the cn attribute
        Attribute desc1 = new AttributeImpl( "cn", "Georges Bush" );

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
        
        Attributes attrs = new AttributesImpl();
        Attribute ocls = new AttributeImpl( "objectClass" );
        ocls.add( "top" );
        ocls.add( "inetOrgPerson" );
        attrs.put( ocls );
        attrs.put( "cn", "Fiona Apple" );
        attrs.put( "sn", "Apple" );
        ctx.createSubcontext( "cn=Fiona Apple", attrs );
        
        // add two displayNames to an inetOrgPerson
        attrs = new AttributesImpl();
        Attribute displayName = new AttributeImpl( "displayName" );
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
     * Add a new attribute to a person entry.
     */
    @Test
    public void testAddNewBinaryAttributeValue() throws Exception
    {
        DirContext ctx = ( DirContext ) getWiredContext( ldapServer ).lookup( BASE );
        
        // Add a binary attribute
        byte[] newValue = new byte[]{0x00, 0x01, 0x02, 0x03};
        Attributes attrs = new AttributesImpl( "userCertificate;binary", newValue );
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
}
