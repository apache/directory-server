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
package org.apache.eve.jndi;


import java.util.Hashtable;
import javax.naming.NamingException;
import javax.naming.InitialContext;
import javax.naming.Context;
import javax.naming.directory.*;
import javax.naming.ldap.LdapContext;

import junit.framework.TestCase;


/**
 * Tests to see if we can fire up the Eve directory server via JNDI.
 *
 * @author <a href="mailto:directory-dev@incubator.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class EveContextFactoryTest extends TestCase
{
    private LdapContext sysRoot;


    protected void setUp() throws Exception
    {
        super.setUp();

        Hashtable env = new Hashtable();
        env.put( Context.PROVIDER_URL, "ou=system" );
        env.put( Context.INITIAL_CONTEXT_FACTORY, "org.apache.eve.jndi.EveContextFactory" );
        env.put( EveContextFactory.WKDIR_ENV, "target/eve" );
        InitialContext initialContext = new InitialContext( env );
        sysRoot = ( LdapContext ) initialContext.lookup( "" );
    }


    protected void tearDown() throws Exception
    {
        super.tearDown();
    }


    public void testSystemContext() throws NamingException
    {
        assertNotNull( sysRoot );

        Attributes attributes = sysRoot.getAttributes( "" );
        assertNotNull( attributes );
        assertEquals( "system", attributes.get( "ou" ).get() );
        Attribute attribute = attributes.get( "objectClass" );
        assertNotNull( attribute );
        assertTrue( attribute.contains( "top" ) );
        assertTrue( attribute.contains( "organizationalUnit" ) );
    }


    public void testCreateContext() throws NamingException
    {
        Attributes attributes = new BasicAttributes();
        Attribute attribute = new BasicAttribute( "objectClass" );
        attribute.add( "top" );
        attribute.add( "organizationalUnit" );
        attributes.put( attribute );
        attributes.put( "ou", "testing" );
        DirContext ctx = sysRoot.createSubcontext( "ou=testing", attributes );
        assertNotNull( ctx );

        ctx = ( DirContext ) sysRoot.lookup( "ou=testing" );
        assertNotNull( ctx );

        attributes = ctx.getAttributes( "" );
        assertNotNull( attributes );
        assertEquals( "testing", attributes.get( "ou" ).get() );
        attribute = attributes.get( "objectClass" );
        assertNotNull( attribute );
        assertTrue( attribute.contains( "top" ) );
        assertTrue( attribute.contains( "organizationalUnit" ) );
    }

}
