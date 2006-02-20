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
package org.apache.directory.server.core.jndi;


import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.BasicAttribute;
import javax.naming.directory.BasicAttributes;
import javax.naming.directory.DirContext;

import org.apache.directory.server.core.unit.AbstractAdminTestCase;
import org.apache.directory.shared.ldap.exception.LdapNameNotFoundException;


/**
 * Tests the destroyContext methods of the provider.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class DestroyContextTest extends AbstractAdminTestCase
{
    protected void setUp() throws Exception
    {
        super.setUp();

        /*
         * create ou=testing00,ou=system
         */
        Attributes attributes = new BasicAttributes( true );
        Attribute attribute = new BasicAttribute( "objectClass" );
        attribute.add( "top" );
        attribute.add( "organizationalUnit" );
        attributes.put( attribute );
        attributes.put( "ou", "testing00" );
        DirContext ctx = sysRoot.createSubcontext( "ou=testing00", attributes );
        assertNotNull( ctx );

        ctx = ( DirContext ) sysRoot.lookup( "ou=testing00" );
        assertNotNull( ctx );

        attributes = ctx.getAttributes( "" );
        assertNotNull( attributes );
        assertEquals( "testing00", attributes.get( "ou" ).get() );
        attribute = attributes.get( "objectClass" );
        assertNotNull( attribute );
        assertTrue( attribute.contains( "top" ) );
        assertTrue( attribute.contains( "organizationalUnit" ) );

        /*
         * create ou=testing01,ou=system
         */
        attributes = new BasicAttributes( true );
        attribute = new BasicAttribute( "objectClass" );
        attribute.add( "top" );
        attribute.add( "organizationalUnit" );
        attributes.put( attribute );
        attributes.put( "ou", "testing01" );
        ctx = sysRoot.createSubcontext( "ou=testing01", attributes );
        assertNotNull( ctx );

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
         * create ou=testing02,ou=system
         */
        attributes = new BasicAttributes( true );
        attribute = new BasicAttribute( "objectClass" );
        attribute.add( "top" );
        attribute.add( "organizationalUnit" );
        attributes.put( attribute );
        attributes.put( "ou", "testing02" );
        ctx = sysRoot.createSubcontext( "ou=testing02", attributes );
        assertNotNull( ctx );

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
         * create ou=subtest,ou=testing01,ou=system
         */
        ctx = ( DirContext ) sysRoot.lookup( "ou=testing01" );

        attributes = new BasicAttributes( true );
        attribute = new BasicAttribute( "objectClass" );
        attribute.add( "top" );
        attribute.add( "organizationalUnit" );
        attributes.put( attribute );
        attributes.put( "ou", "subtest" );
        ctx = ctx.createSubcontext( "ou=subtest", attributes );
        assertNotNull( ctx );

        ctx = ( DirContext ) sysRoot.lookup( "ou=subtest,ou=testing01" );
        assertNotNull( ctx );

        attributes = ctx.getAttributes( "" );
        assertNotNull( attributes );
        assertEquals( "subtest", attributes.get( "ou" ).get() );
        attribute = attributes.get( "objectClass" );
        assertNotNull( attribute );
        assertTrue( attribute.contains( "top" ) );
        assertTrue( attribute.contains( "organizationalUnit" ) );
    }


    /**
     * Tests the creation and subsequent read of a new JNDI context under the
     * system context root.
     *
     * @throws NamingException if there are failures
     */
    public void testDestroyContext() throws NamingException
    {
        /*
         * delete ou=testing00,ou=system
         */
        sysRoot.destroySubcontext( "ou=testing00" );

        try
        {
            sysRoot.lookup( "ou=testing00" );
            fail( "ou=testing00, ou=system should not exist" );
        }
        catch ( NamingException e )
        {
            assertTrue( e instanceof LdapNameNotFoundException );
        }

        /*
         * delete ou=subtest,ou=testing01,ou=system
         */
        sysRoot.destroySubcontext( "ou=subtest,ou=testing01" );

        try
        {
            sysRoot.lookup( "ou=subtest,ou=testing01" );
            fail( "ou=subtest,ou=testing01,ou=system should not exist" );
        }
        catch ( NamingException e )
        {
            assertTrue( e instanceof LdapNameNotFoundException );
        }

        /*
         * delete ou=testing01,ou=system
         */
        sysRoot.destroySubcontext( "ou=testing01" );

        try
        {
            sysRoot.lookup( "ou=testing01" );
            fail( "ou=testing01, ou=system should not exist" );
        }
        catch ( NamingException e )
        {
            assertTrue( e instanceof LdapNameNotFoundException );
        }

        /*
         * delete ou=testing01,ou=system
         */
        sysRoot.destroySubcontext( "ou=testing02" );

        try
        {
            sysRoot.lookup( "ou=testing02" );
            fail( "ou=testing02, ou=system should not exist" );
        }
        catch ( NamingException e )
        {
            assertTrue( e instanceof LdapNameNotFoundException );
        }
    }

}
