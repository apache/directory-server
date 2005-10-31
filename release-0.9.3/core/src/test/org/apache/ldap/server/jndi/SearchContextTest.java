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
package org.apache.ldap.server.jndi;


import java.util.HashMap;

import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.BasicAttribute;
import javax.naming.directory.BasicAttributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;

import org.apache.ldap.common.message.DerefAliasesEnum;
import org.apache.ldap.server.AbstractAdminTestCase;


/**
 * Tests the search() methods of the provider.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class SearchContextTest extends AbstractAdminTestCase
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


    public void testSearchOneLevel() throws NamingException
    {
        SearchControls controls = new SearchControls();

        controls.setSearchScope( SearchControls.ONELEVEL_SCOPE );

        controls.setDerefLinkFlag( false );

        sysRoot.addToEnvironment( DerefAliasesEnum.JNDI_PROP, DerefAliasesEnum.NEVERDEREFALIASES.getName() );

        HashMap map = new HashMap();

        NamingEnumeration list = sysRoot.search( "", "(ou=*)", controls );

        while ( list.hasMore() )
        {
            SearchResult result = ( SearchResult ) list.next();

            map.put( result.getName(), result.getAttributes() );
        }

        assertEquals( "Expected number of results returned was incorrect!", 6, map.size() );

        assertTrue( map.containsKey( "ou=testing00,ou=system" ) );

        assertTrue( map.containsKey( "ou=testing01,ou=system" ) );

        assertTrue( map.containsKey( "ou=testing02,ou=system" ) );
    }


    public void testSearchSubTreeLevel() throws NamingException
    {
        SearchControls controls = new SearchControls();

        controls.setSearchScope( SearchControls.SUBTREE_SCOPE );

        controls.setDerefLinkFlag( false );

        sysRoot.addToEnvironment( DerefAliasesEnum.JNDI_PROP, DerefAliasesEnum.NEVERDEREFALIASES.getName() );

        HashMap map = new HashMap();

        NamingEnumeration list = sysRoot.search( "", "(ou=*)", controls );

        while ( list.hasMore() )
        {
            SearchResult result = ( SearchResult ) list.next();

            map.put( result.getName(), result.getAttributes() );
        }

        assertEquals( "Expected number of results returned was incorrect", 12, map.size() );

        assertTrue( map.containsKey( "ou=system" ) );

        assertTrue( map.containsKey( "ou=testing00,ou=system" ) );

        assertTrue( map.containsKey( "ou=testing01,ou=system" ) );

        assertTrue( map.containsKey( "ou=testing02,ou=system" ) );

        assertTrue( map.containsKey( "ou=subtest,ou=testing01,ou=system" ) );
    }
    
    public void testSearchFilterArgs() throws NamingException
    {
        SearchControls controls = new SearchControls();

        controls.setSearchScope( SearchControls.ONELEVEL_SCOPE );

        controls.setDerefLinkFlag( false );

        sysRoot.addToEnvironment( DerefAliasesEnum.JNDI_PROP, DerefAliasesEnum.NEVERDEREFALIASES.getName() );

        HashMap map = new HashMap();

        NamingEnumeration list = sysRoot.search( "", "(| (ou={0}) (ou={1}))", new Object[] {"testing00", "testing01"}, controls );

        while ( list.hasMore() )
        {
            SearchResult result = ( SearchResult ) list.next();

            map.put( result.getName(), result.getAttributes() );
        }

        assertEquals( "Expected number of results returned was incorrect!", 2, map.size() );

        assertTrue( map.containsKey( "ou=testing00,ou=system" ) );

        assertTrue( map.containsKey( "ou=testing01,ou=system" ) );
    }

}
