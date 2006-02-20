/*
 * Copyright (c) 2004 Solarsis Group LLC.
 *
 * Licensed under the Open Software License, Version 2.1 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://opensource.org/licenses/osl-2.1.php
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.directory.server;


import java.util.Hashtable;

import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.BasicAttribute;
import javax.naming.directory.BasicAttributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.InvalidAttributeIdentifierException;
import javax.naming.directory.NoSuchAttributeException;
import javax.naming.directory.SchemaViolationException;
import javax.naming.ldap.InitialLdapContext;
import javax.naming.ldap.LdapContext;

import org.apache.directory.server.unit.AbstractServerTest;


/**
 * Testcase with different modify operations on a person entry. Each includes a
 * single removal op only.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class ModifyRemoveTest extends AbstractServerTest
{

    private LdapContext ctx = null;

    public static final String RDN = "cn=Tori Amos";


    /**
     * Creation of required attributes of a person entry.
     */
    protected Attributes getPersonAttributes( String sn, String cn )
    {
        Attributes attributes = new BasicAttributes();
        Attribute attribute = new BasicAttribute( "objectClass" );
        attribute.add( "top" );
        attribute.add( "person" );
        attributes.put( attribute );
        attributes.put( "cn", cn );
        attributes.put( "sn", sn );

        return attributes;
    }


    /**
     * Create context and a person entry.
     */
    public void setUp() throws Exception
    {
        super.setUp();

        Hashtable env = new Hashtable();
        env.put( "java.naming.factory.initial", "com.sun.jndi.ldap.LdapCtxFactory" );
        env.put( "java.naming.provider.url", "ldap://localhost:" + port + "/ou=system" );
        env.put( "java.naming.security.principal", "uid=admin,ou=system" );
        env.put( "java.naming.security.credentials", "secret" );
        env.put( "java.naming.security.authentication", "simple" );

        ctx = new InitialLdapContext( env, null );
        assertNotNull( ctx );

        // Create a person with description
        Attributes attributes = this.getPersonAttributes( "Amos", "Tori Amos" );
        attributes.put( "description", "an American singer-songwriter" );
        ctx.createSubcontext( RDN, attributes );

    }


    /**
     * Remove person entry and close context.
     */
    public void tearDown() throws Exception
    {
        ctx.unbind( RDN );
        ctx.close();
        ctx = null;
        super.tearDown();
    }


    /**
     * Just a little test to check wether opening the connection and creation of
     * the person succeeds succeeds.
     */
    public void testSetUpTearDown() throws NamingException
    {
        assertNotNull( ctx );
        DirContext tori = ( DirContext ) ctx.lookup( RDN );
        assertNotNull( tori );
    }


    /**
     * Remove an attribute, which is not required.
     * 
     * Expected result: After successful deletion, attribute is not present in
     * entry.
     * 
     * @throws NamingException
     */
    public void testRemoveNotRequiredAttribute() throws NamingException
    {
        // Remove description Attribute
        Attribute attr = new BasicAttribute( "description" );
        Attributes attrs = new BasicAttributes();
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
     * 
     * @throws NamingException
     */
    public void testRemoveTwoNotRequiredAttributes() throws NamingException
    {

        // add telephoneNumber to entry
        Attributes tn = new BasicAttributes( "telephoneNumber", "12345678" );
        ctx.modifyAttributes( RDN, DirContext.ADD_ATTRIBUTE, tn );

        // Remove description and telephoneNumber to Attribute
        Attributes attrs = new BasicAttributes();
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
     * 
     * @throws NamingException
     */
    public void testRemoveRequiredAttribute() throws NamingException
    {

        // Remove sn attribute
        Attribute attr = new BasicAttribute( "sn" );
        Attributes attrs = new BasicAttributes();
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
     * Remove a required attribute from RDN.
     * 
     * Expected Result: Deletion fails with SchemaViolationException.
     * 
     * @throws NamingException
     */
    public void testRemovePartOfRdn() throws NamingException
    {

        // Remove sn attribute
        Attribute attr = new BasicAttribute( "cn" );
        Attributes attrs = new BasicAttributes();
        attrs.put( attr );

        try
        {
            ctx.modifyAttributes( RDN, DirContext.REMOVE_ATTRIBUTE, attrs );
            fail( "Deletion of RDN attribute should fail." );
        }
        catch ( SchemaViolationException e )
        {
            // expected behaviour
        }
    }


    /**
     * Remove a not required attribute from RDN.
     * 
     * Expected Result: Deletion fails with SchemaViolationException.
     * 
     * @throws NamingException
     */
    public void testRemovePartOfRdnNotRequired() throws NamingException
    {

        // Change RDN to another attribute
        String newRdn = "description=an American singer-songwriter";
        ctx.addToEnvironment( "java.naming.ldap.deleteRDN", "false" );
        ctx.rename( RDN, newRdn );

        // Remove description, which is now RDN attribute
        Attribute attr = new BasicAttribute( "description" );
        Attributes attrs = new BasicAttributes();
        attrs.put( attr );

        try
        {
            ctx.modifyAttributes( newRdn, DirContext.REMOVE_ATTRIBUTE, attrs );
            fail( "Deletion of RDN attribute should fail." );
        }
        catch ( SchemaViolationException e )
        {
            // expected behaviour
        }

        // Change RDN back to original
        ctx.addToEnvironment( "java.naming.ldap.deleteRDN", "false" );
        ctx.rename( newRdn, RDN );
    }


    /**
     * Remove a an attribute which is not present on the entry, but in the
     * schema.
     * 
     * Expected result: Deletion fails with NoSuchAttributeException
     * 
     * @throws NamingException
     */
    public void testRemoveAttributeNotPresent() throws NamingException
    {

        // Remove telephoneNumber Attribute
        Attribute attr = new BasicAttribute( "telephoneNumber" );
        Attributes attrs = new BasicAttributes();
        attrs.put( attr );

        try
        {
            ctx.modifyAttributes( RDN, DirContext.REMOVE_ATTRIBUTE, attrs );
            fail( "Deletion of attribute, which is not present in the entry, should fail." );
        }
        catch ( NoSuchAttributeException e )
        {
            // expected behaviour
        }
    }


    /**
     * Remove a an attribute which is not present in the schema.
     * 
     * Expected result: Deletion fails with NoSuchAttributeException
     * 
     * @throws NamingException
     */
    public void testRemoveAttributeNotValid() throws NamingException
    {

        // Remove phantasy attribute
        Attribute attr = new BasicAttribute( "XXX" );
        Attributes attrs = new BasicAttributes();
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

}
