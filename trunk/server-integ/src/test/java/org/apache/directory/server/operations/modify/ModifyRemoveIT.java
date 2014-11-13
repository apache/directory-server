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
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import javax.naming.NamingEnumeration;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.BasicAttribute;
import javax.naming.directory.BasicAttributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.InvalidAttributeIdentifierException;
import javax.naming.directory.InvalidAttributeValueException;
import javax.naming.directory.ModificationItem;
import javax.naming.directory.NoSuchAttributeException;
import javax.naming.directory.SchemaViolationException;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;

import org.apache.directory.server.annotations.CreateLdapServer;
import org.apache.directory.server.annotations.CreateTransport;
import org.apache.directory.server.core.annotations.ApplyLdifs;
import org.apache.directory.server.core.integ.AbstractLdapTestUnit;
import org.apache.directory.server.core.integ.FrameworkRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;


/**
 * Test case with different modify operations on a person entry. Each includes a
 * single removal operation only.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
@RunWith(FrameworkRunner.class)
//@CreateDS( name="ModifyRemoveIT-class", enableChangeLog=false )
@CreateLdapServer(
    transports =
        {
            @CreateTransport(protocol = "LDAP")
    })
@ApplyLdifs(
    {
        // Entry # 1
        "dn: cn=Tori Amos,ou=system",
        "objectClass: person",
        "objectClass: top",
        "description: an American singer-songwriter",
        "cn: Tori Amos",
        "sn: Amos" })
public class ModifyRemoveIT extends AbstractLdapTestUnit
{
    private static final String BASE = "ou=system";
    private static final String RDN = "cn=Tori Amos";


    /**
     * Enable the krb5kdc schema.
     */
    @Before
    public void setUp() throws Exception
    {
        DirContext schemaRoot = ( DirContext ) getWiredContext( getLdapServer() ).lookup( "ou=schema" );

        // -------------------------------------------------------------------
        // Enable the krb5kdc schema
        // -------------------------------------------------------------------

        // check if krb5kdc is disabled
        Attributes krb5kdcAttrs = schemaRoot.getAttributes( "cn=Krb5kdc" );
        boolean isKrb5KdcDisabled = false;

        if ( krb5kdcAttrs.get( "m-disabled" ) != null )
        {
            isKrb5KdcDisabled = ( ( String ) krb5kdcAttrs.get( "m-disabled" ).get() ).equalsIgnoreCase( "TRUE" );
        }

        // if krb5kdc is disabled then enable it
        if ( isKrb5KdcDisabled )
        {
            Attribute disabled = new BasicAttribute( "m-disabled" );
            ModificationItem[] mods = new ModificationItem[]
                { new ModificationItem( DirContext.REMOVE_ATTRIBUTE, disabled ) };
            schemaRoot.modifyAttributes( "cn=Krb5kdc", mods );
        }
    }


    /**
     * Creation of required attributes of a person entry.
     */
    protected Attributes getPersonAttributes( String sn, String cn )
    {
        Attributes attributes = new BasicAttributes( true );
        Attribute attribute = new BasicAttribute( "objectClass" );
        attribute.add( "top" );
        attribute.add( "person" );
        attributes.put( attribute );
        attributes.put( "cn", cn );
        attributes.put( "sn", sn );

        return attributes;
    }


    /**
     * Creation of required attributes of an inetOrgPerson entry.
     */
    protected Attributes getInetOrgPersonAttributes( String sn, String cn )
    {
        Attributes attrs = new BasicAttributes( true );
        Attribute ocls = new BasicAttribute( "objectClass" );
        ocls.add( "top" );
        ocls.add( "person" );
        ocls.add( "organizationalPerson" );
        ocls.add( "inetOrgPerson" );
        attrs.put( ocls );
        attrs.put( "cn", cn );
        attrs.put( "sn", sn );

        return attrs;
    }


    /**
     * Remove a value which does not exist in an attribute making sure
     * it does not remove other values in that attribute.  Tests if the
     * following JIRA issue is still valid:
     * 
     *    https://issues.apache.org/jira/browse/DIRSERVER-1149
     */
    @Test
    public void testRemoveAttemptWithoutChange() throws Exception
    {
        DirContext ctx = ( DirContext ) getWiredContext( getLdapServer() ).lookup( BASE );

        // Get the attributes and check the contents
        Attributes tori = ctx.getAttributes( RDN );
        assertNotNull( tori.get( "objectClass" ) );
        assertNotNull( tori.get( "cn" ) );
        assertEquals( 1, tori.get( "cn" ).size() );
        assertEquals( "Tori Amos", tori.get( "cn" ).get() );
        assertNotNull( tori.get( "sn" ) );

        // Test an add operation first
        ModificationItem mod = new ModificationItem( DirContext.ADD_ATTRIBUTE, new BasicAttribute( "cn", "foo" ) );
        ctx.modifyAttributes( RDN, new ModificationItem[]
            { mod } );
        tori = ctx.getAttributes( RDN );
        assertNotNull( tori.get( "objectClass" ) );
        assertNotNull( tori.get( "cn" ) );
        assertEquals( 2, tori.get( "cn" ).size() );
        assertEquals( "Tori Amos", tori.get( "cn" ).get( 0 ) );
        assertEquals( "foo", tori.get( "cn" ).get( 1 ) );
        assertNotNull( tori.get( "sn" ) );

        // Now test remove of value ( bar ) that does not exist in cn
        mod = new ModificationItem( DirContext.REMOVE_ATTRIBUTE, new BasicAttribute( "cn", "bar" ) );
        try
        {
            ctx.modifyAttributes( RDN, new ModificationItem[]
                { mod } );
            fail();
        }
        catch ( NoSuchAttributeException nsae )
        {
            assertTrue( true );
        }

        tori = ctx.getAttributes( RDN );
        assertNotNull( tori.get( "objectClass" ) );
        assertNotNull( tori.get( "cn" ) );
        assertEquals( 2, tori.get( "cn" ).size() );
        assertEquals( "Tori Amos", tori.get( "cn" ).get( 0 ) );
        assertEquals( "foo", tori.get( "cn" ).get( 1 ) );
        assertNotNull( tori.get( "sn" ) );
    }


    /**
     * Remove an attribute, which is not required.
     * 
     * Expected result: After successful deletion, attribute is not present in
     * entry.
     */
    @Test
    public void testRemoveNotRequiredAttribute() throws Exception
    {
        DirContext ctx = ( DirContext ) getWiredContext( getLdapServer() ).lookup( BASE );

        // Remove description Attribute
        Attribute attr = new BasicAttribute( "description" );
        Attributes attrs = new BasicAttributes( true );
        attrs.put( attr );
        ctx.modifyAttributes( RDN, DirContext.REMOVE_ATTRIBUTE, attrs );

        // Verify, that attribute is deleted
        attrs = ctx.getAttributes( RDN );
        attr = attrs.get( "description" );
        assertNull( attr );
    }


    /**
     * Remove two not required attributes.
     * 
     * Expected result: After successful deletion, both attributes ar not
     * present in entry.
     */
    @Test
    public void testRemoveTwoNotRequiredAttributes() throws Exception
    {
        DirContext ctx = ( DirContext ) getWiredContext( getLdapServer() ).lookup( BASE );

        // add telephoneNumber to entry
        Attributes tn = new BasicAttributes( "telephoneNumber", "12345678", true );
        ctx.modifyAttributes( RDN, DirContext.ADD_ATTRIBUTE, tn );

        // Remove description and telephoneNumber to Attribute
        Attributes attrs = new BasicAttributes( true );
        attrs.put( new BasicAttribute( "description" ) );
        attrs.put( new BasicAttribute( "telephoneNumber" ) );
        ctx.modifyAttributes( RDN, DirContext.REMOVE_ATTRIBUTE, attrs );

        // Verify, that attributes are deleted
        attrs = ctx.getAttributes( RDN );
        assertNull( attrs.get( "description" ) );
        assertNull( attrs.get( "telephoneNumber" ) );
        assertNotNull( attrs.get( "cn" ) );
        assertNotNull( attrs.get( "sn" ) );
    }


    /**
     * Remove a required attribute. The sn attribute of the person entry is used
     * here.
     * 
     * Expected Result: Deletion fails with NamingException (Schema Violation).
     */
    @Test
    public void testRemoveRequiredAttribute() throws Exception
    {
        DirContext ctx = ( DirContext ) getWiredContext( getLdapServer() ).lookup( BASE );

        // Remove sn attribute
        Attribute attr = new BasicAttribute( "sn" );
        Attributes attrs = new BasicAttributes( true );
        attrs.put( attr );

        try
        {
            ctx.modifyAttributes( RDN, DirContext.REMOVE_ATTRIBUTE, attrs );
            fail( "Deletion of required attribute should fail." );
        }
        catch ( SchemaViolationException e )
        {
            // expected behaviour
        }
    }


    /**
     * Remove a required attribute from Rdn.
     * 
     * Expected Result: Deletion fails with SchemaViolationException.
     */
    @Test
    public void testRemovePartOfRdn() throws Exception
    {
        DirContext ctx = ( DirContext ) getWiredContext( getLdapServer() ).lookup( BASE );

        // Remove sn attribute
        Attribute attr = new BasicAttribute( "cn" );
        Attributes attrs = new BasicAttributes( true );
        attrs.put( attr );

        try
        {
            ctx.modifyAttributes( RDN, DirContext.REMOVE_ATTRIBUTE, attrs );
            fail( "Deletion of Rdn attribute should fail." );
        }
        catch ( SchemaViolationException e )
        {
            // expected behaviour
        }
    }


    /**
     * Remove a not required attribute from Rdn.
     * 
     * Expected Result: Deletion fails with SchemaViolationException.
     */
    @Test
    public void testRemovePartOfRdnNotRequired() throws Exception
    {
        DirContext ctx = ( DirContext ) getWiredContext( getLdapServer() ).lookup( BASE );

        // Change Rdn to another attribute
        String newRdn = "description=an American singer-songwriter";
        ctx.addToEnvironment( "java.naming.ldap.deleteRDN", "false" );
        ctx.rename( RDN, newRdn );

        // Remove description, which is now Rdn attribute
        Attribute attr = new BasicAttribute( "description" );
        Attributes attrs = new BasicAttributes( true );
        attrs.put( attr );

        try
        {
            ctx.modifyAttributes( newRdn, DirContext.REMOVE_ATTRIBUTE, attrs );
            fail( "Deletion of Rdn attribute should fail." );
        }
        catch ( SchemaViolationException e )
        {
            // expected behaviour
        }

        // Change Rdn back to original
        ctx.addToEnvironment( "java.naming.ldap.deleteRDN", "false" );
        ctx.rename( newRdn, RDN );
    }


    /**
     * Remove a an attribute which is not present on the entry, but in the
     * schema.
     * 
     * Expected result: Deletion fails with NoSuchAttributeException
     */
    @Test
    public void testRemoveAttributeNotPresent() throws Exception
    {
        DirContext ctx = ( DirContext ) getWiredContext( getLdapServer() ).lookup( BASE );

        // Remove telephoneNumber Attribute
        Attribute attr = new BasicAttribute( "telephoneNumber" );
        Attributes attrs = new BasicAttributes( true );
        attrs.put( attr );

        try
        {
            ctx.modifyAttributes( RDN, DirContext.REMOVE_ATTRIBUTE, attrs );
            fail( "Deletion of attribute, which is not present in the entry, should fail." );
        }
        catch ( NoSuchAttributeException e )
        {
            assertTrue( true );
            // expected behaviour
        }
    }


    /**
     * Remove a an attribute value which is not present in the entry
     * 
     * Expected result: Deletion fails with NoSuchAttributeException
     */
    @Test
    public void testRemoveAttributeValueNotPresent() throws Exception
    {
        DirContext ctx = ( DirContext ) getWiredContext( getLdapServer() ).lookup( BASE );

        // Remove telephoneNumber Attribute
        Attribute attr = new BasicAttribute( "telephoneNumber", "12345" );
        Attributes attrs = new BasicAttributes( true );
        attrs.put( attr );

        // Inject the new attribute
        ctx.modifyAttributes( RDN, DirContext.ADD_ATTRIBUTE, attrs );

        // Now try to remove a value which is not present
        Attribute attr2 = new BasicAttribute( "telephoneNumber", "7890" );
        Attributes attrs2 = new BasicAttributes( true );
        attrs2.put( attr2 );

        try
        {
            ctx.modifyAttributes( RDN, DirContext.REMOVE_ATTRIBUTE, attrs2 );
            // We should get an exception
            fail();
        }
        catch ( NoSuchAttributeException nsae )
        {
            assertTrue( true );
        }
    }


    /**
     * Remove a an attribute which is not present in the schema.
     * 
     * Expected result: Deletion fails with NoSuchAttributeException
     */
    @Test
    public void testRemoveAttributeNotValid() throws Exception
    {
        DirContext ctx = ( DirContext ) getWiredContext( getLdapServer() ).lookup( BASE );

        // Remove phantasy attribute
        Attribute attr = new BasicAttribute( "XXX" );
        Attributes attrs = new BasicAttributes( true );
        attrs.put( attr );

        try
        {
            ctx.modifyAttributes( RDN, DirContext.REMOVE_ATTRIBUTE, attrs );
            fail( "Deletion of an invalid attribute should fail." );
        }
        catch ( NoSuchAttributeException e )
        {
            // expected behaviour
        }
        catch ( InvalidAttributeIdentifierException e )
        {
            // expected behaviour
        }
    }


    /**
     * Create a person entry and try to remove an attribute value
     */
    @Test
    public void testReplaceNonExistingAttribute() throws Exception
    {
        DirContext ctx = ( DirContext ) getWiredContext( getLdapServer() ).lookup( BASE );

        // Create an entry
        Attributes attrs = getInetOrgPersonAttributes( "Bush", "Kate Bush" );
        attrs.put( "givenname", "Kate" );
        String rdn = "cn=Kate Bush";
        ctx.createSubcontext( rdn, attrs );

        // replace attribute givenName with empty value (=> deletion)
        Attribute attr = new BasicAttribute( "givenname" );
        ModificationItem item = new ModificationItem( DirContext.REPLACE_ATTRIBUTE, attr );
        ctx.modifyAttributes( rdn, new ModificationItem[]
            { item } );

        SearchControls sctls = new SearchControls();
        sctls.setSearchScope( SearchControls.ONELEVEL_SCOPE );
        String filter = "(cn=Kate Bush)";
        String base = "";
        NamingEnumeration<SearchResult> enm = ctx.search( base, filter, sctls );
        if ( enm.hasMore() )
        {
            SearchResult sr = enm.next();
            attrs = sr.getAttributes();
            Attribute cn = sr.getAttributes().get( "cn" );
            assertNotNull( cn );
            assertTrue( cn.contains( "Kate Bush" ) );

            // Check whether attribute has been removed
            Attribute givenName = sr.getAttributes().get( "givenname" );
            assertNull( givenName );
        }
        else
        {
            fail( "entry not found" );
        }

        ctx.destroySubcontext( rdn );
    }


    /**
     * Create a person entry and try to remove an attribute value from the Rdn
     * by Replacement
     */
    @Test
    public void testReplaceRdnByEmptyValueAttribute() throws Exception
    {
        DirContext ctx = ( DirContext ) getWiredContext( getLdapServer() ).lookup( BASE );

        // Create an entry
        Attributes attrs = getPersonAttributes( "Bush", "Kate Bush" );
        String rdn = "cn=Kate Bush";
        ctx.createSubcontext( rdn, attrs );

        // replace attribute cn with empty value (=> deletion)
        Attribute attr = new BasicAttribute( "cn" );
        ModificationItem item = new ModificationItem( DirContext.REPLACE_ATTRIBUTE, attr );

        try
        {
            ctx.modifyAttributes( rdn, new ModificationItem[]
                { item } );
            fail( "modify should fail" );
        }
        catch ( SchemaViolationException e )
        {
            // Expected behaviour
        }

        ctx.destroySubcontext( rdn );
    }


    /**
     * Create a person entry and try to remove an attribute from the Rdn
     */
    @Test
    public void testRemoveRdnAttribute() throws Exception
    {
        DirContext ctx = ( DirContext ) getWiredContext( getLdapServer() ).lookup( BASE );

        // Create an entry
        Attributes attrs = getPersonAttributes( "Bush", "Kate Bush" );
        String rdn = "cn=Kate Bush";
        ctx.createSubcontext( rdn, attrs );

        // replace attribute cn with empty value (=> deletion)
        Attribute attr = new BasicAttribute( "cn" );
        ModificationItem item = new ModificationItem( DirContext.REMOVE_ATTRIBUTE, attr );

        try
        {
            ctx.modifyAttributes( rdn, new ModificationItem[]
                { item } );
            fail( "modify should fail" );
        }
        catch ( SchemaViolationException e )
        {
            // Expected behaviour
        }

        ctx.destroySubcontext( rdn );
    }


    /**
     * Create a person entry and try to remove an attribute from the Rdn
     */
    @Test
    public void testRemoveRdnAttributeValue() throws Exception
    {
        DirContext ctx = ( DirContext ) getWiredContext( getLdapServer() ).lookup( BASE );

        // Create an entry
        Attributes attrs = getPersonAttributes( "Bush", "Kate Bush" );
        String rdn = "cn=Kate Bush";
        ctx.createSubcontext( rdn, attrs );

        // replace attribute cn with empty value (=> deletion)
        Attribute attr = new BasicAttribute( "cn", "Kate Bush" );
        ModificationItem item = new ModificationItem( DirContext.REMOVE_ATTRIBUTE, attr );

        try
        {
            ctx.modifyAttributes( rdn, new ModificationItem[]
                { item } );
            fail( "modify should fail" );
        }
        catch ( SchemaViolationException e )
        {
            // Expected behaviour
        }

        ctx.destroySubcontext( rdn );
    }


    /**
     * Create a person entry and try to remove objectClass attribute
     */
    @Test
    public void testDeleteOclAttrWithTopPersonOrganizationalpersonInetorgperson() throws Exception
    {
        DirContext ctx = ( DirContext ) getWiredContext( getLdapServer() ).lookup( BASE );

        // Create an entry
        Attributes attrs = getInetOrgPersonAttributes( "Bush", "Kate Bush" );
        String rdn = "cn=Kate Bush";
        ctx.createSubcontext( rdn, attrs );

        ModificationItem delModOp = new ModificationItem( DirContext.REMOVE_ATTRIBUTE, new BasicAttribute(
            "objectclass",
            "" ) );

        try
        {
            ctx.modifyAttributes( rdn, new ModificationItem[]
                { delModOp } );
            fail( "deletion of objectclass should fail" );
        }
        catch ( SchemaViolationException e )
        {
            // expected
        }
        catch ( NoSuchAttributeException e )
        {
            // expected
        }
        catch ( InvalidAttributeValueException e )
        {
            // expected
        }
        catch ( Exception e )
        {
            e.printStackTrace();
        }

        ctx.destroySubcontext( rdn );
    }


    /**
     * Create a person entry and try to remove objectClass attribute. A variant
     * which works.
     */
    @Test
    public void testDeleteOclAttrWithTopPersonOrganizationalpersonInetorgpersonVariant() throws Exception
    {
        DirContext ctx = ( DirContext ) getWiredContext( getLdapServer() ).lookup( BASE );

        // Create an entry
        Attributes attrs = getInetOrgPersonAttributes( "Bush", "Kate Bush" );
        String rdn = "cn=Kate Bush";
        ctx.createSubcontext( rdn, attrs );

        ModificationItem delModOp = new ModificationItem( DirContext.REMOVE_ATTRIBUTE, new BasicAttribute(
            "objectclass" ) );

        try
        {
            ctx.modifyAttributes( rdn, new ModificationItem[]
                { delModOp } );
            fail( "deletion of objectclass should fail" );
        }
        catch ( SchemaViolationException e )
        {
            // expected
        }

        ctx.destroySubcontext( rdn );
    }


    /**
     * Test for DIRSERVER-1308:
     * Remove an objectClass and a mandatory attribute.
     * 
     * Expected result: After successful deletion, both the objectClass
     * and the attribute are not present in entry.
     */
    @Test
    public void testRemoveObjectClassAndMandatoryAttribute() throws Exception
    {
        DirContext ctx = ( DirContext ) getWiredContext( getLdapServer() ).lookup( BASE );

        // add objectClass:krb5Principal and krb5PrincipalName:test to entry
        Attributes tn = new BasicAttributes( true );
        tn.put( new BasicAttribute( "objectClass", "krb5Principal", true ) );
        tn.put( new BasicAttribute( "krb5PrincipalName", "test", true ) );
        ctx.modifyAttributes( RDN, DirContext.ADD_ATTRIBUTE, tn );

        // remove objectClass:krb5Principal and krb5PrincipalName
        Attributes attrs = new BasicAttributes( true );
        attrs.put( new BasicAttribute( "objectClass", "krb5Principal", true ) );
        attrs.put( new BasicAttribute( "krb5PrincipalName" ) );
        ctx.modifyAttributes( RDN, DirContext.REMOVE_ATTRIBUTE, attrs );

        // Verify, that attributes are deleted
        attrs = ctx.getAttributes( RDN );
        assertNotNull( attrs.get( "objectClass" ) );
        assertFalse( attrs.get( "objectClass" ).contains( "krb5Principal" ) );
        assertNull( attrs.get( "krb5PrincipalName" ) );
        assertNotNull( attrs.get( "cn" ) );
        assertNotNull( attrs.get( "sn" ) );
    }
}
