/*
 *   Copyright 2004 The Apache Software Foundation
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 */
package org.apache.directory.server;


import javax.naming.directory.*;
import javax.naming.NamingException;

import org.apache.directory.server.unit.AbstractServerTest;

import java.util.Hashtable;


/**
 * Various add scenario tests.
 * 
 * @author szoerner
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class AddITest extends AbstractServerTest
{
    private static final String RDN = "cn=The Person";

    private DirContext ctx = null;


    /**
     * Create an entry for a person.
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
        ctx = new InitialDirContext( env );

        // Create a person
        Attributes attributes = new BasicAttributes( true );
        Attribute attribute = new BasicAttribute( "objectClass" );
        attribute.add( "top" );
        attribute.add( "person" );
        attributes.put( attribute );
        attributes.put( "cn", "The Person" );
        attributes.put( "sn", "Person" );
        attributes.put( "description", "this is a person" );
        DirContext person = ctx.createSubcontext( RDN, attributes );

        assertNotNull( person );
    }


    /**
     * Remove the person.
     */
    public void tearDown() throws Exception
    {
        ctx.unbind( RDN );
        ctx.close();
        ctx = null;
        super.tearDown();
    }


    /**
     * Just a little test to check wether the person is created correctly after
     * setup.
     * 
     * @throws NamingException
     */
    public void testSetUpTearDown() throws NamingException
    {
        DirContext person = ( DirContext ) ctx.lookup( RDN );
        assertNotNull( person );

        // Check object classes

        Attributes attributes = person.getAttributes( "" );
        Attribute ocls = attributes.get( "objectClass" );

        String[] expectedOcls =
            { "top", "person" };
        for ( int i = 0; i < expectedOcls.length; i++ )
        {
            String name = expectedOcls[i];
            assertTrue( "object class " + name + " is NOT present when it should be!", ocls.contains( name ) );
        }
    }


    /**
     * This is the original defect as in JIRA DIREVE-216.
     * 
     * @throws NamingException
     */
    public void testAddObjectClasses() throws NamingException
    {

        // modify object classes, add two more
        Attributes attributes = new BasicAttributes( true );
        Attribute ocls = new BasicAttribute( "objectClass" );
        ocls.add( "organizationalPerson" );
        ocls.add( "inetOrgPerson" );
        attributes.put( ocls );

        DirContext person = ( DirContext ) ctx.lookup( RDN );
        person.modifyAttributes( "", DirContext.ADD_ATTRIBUTE, attributes );

        // Read again from directory
        person = ( DirContext ) ctx.lookup( RDN );
        attributes = person.getAttributes( "" );
        Attribute newOcls = attributes.get( "objectClass" );

        String[] expectedOcls =
            { "top", "person", "organizationalPerson", "inetOrgPerson" };
        for ( int i = 0; i < expectedOcls.length; i++ )
        {
            String name = expectedOcls[i];
            assertTrue( "object class " + name + " is present", newOcls.contains( name ) );
        }
    }


    /**
     * This changes a single attribute value. Just as a reference.
     * 
     * @throws NamingException
     */
    public void testModifyDescription() throws NamingException
    {
        String newDescription = "More info on the user ...";

        // modify object classes, add two more
        Attributes attributes = new BasicAttributes( true );
        Attribute desc = new BasicAttribute( "description", newDescription );
        attributes.put( desc );

        DirContext person = ( DirContext ) ctx.lookup( RDN );
        person.modifyAttributes( "", DirContext.REPLACE_ATTRIBUTE, attributes );

        // Read again from directory
        person = ( DirContext ) ctx.lookup( RDN );
        attributes = person.getAttributes( "" );
        Attribute newDesc = attributes.get( "description" );

        assertTrue( "new Description", newDesc.contains( newDescription ) );
    }


    /**
     * Try to add entry with required attribute missing.
     */
    public void testAddWithMissingRequiredAttributes() throws NamingException
    {
        // person without sn
        Attributes attrs = new BasicAttributes();
        Attribute ocls = new BasicAttribute( "objectClass" );
        ocls.add( "top" );
        ocls.add( "person" );
        attrs.put( ocls );
        attrs.put( "cn", "Fiona Apple" );

        try
        {
            ctx.createSubcontext( "cn=Fiona Apple", attrs );
            fail( "creation of entry should fail" );
        }
        catch ( SchemaViolationException e )
        {
            // expected
        }
    }
}
