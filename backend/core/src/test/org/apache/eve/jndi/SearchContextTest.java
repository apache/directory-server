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


import java.util.HashMap;
import javax.naming.NamingException;
import javax.naming.NamingEnumeration;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;

import org.apache.ldap.common.message.DerefAliasesEnum;


/**
 * Tests the search() methods of the provider.
 *
 * @author <a href="mailto:directory-dev@incubator.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class SearchContextTest extends AbstractJndiTest
{
    protected void setUp() throws Exception
    {
        super.setUp();

        CreateContextTest createContextTest = new CreateContextTest();
        createContextTest.setUp();
        createContextTest.testCreateContexts();
    }


    public void testSearchOneLeve() throws NamingException
    {
        SearchControls controls = new SearchControls();
        controls.setSearchScope( SearchControls.ONELEVEL_SCOPE );
        controls.setDerefLinkFlag( false );
        sysRoot.addToEnvironment( DerefAliasesEnum.JNDI_PROP,
                DerefAliasesEnum.NEVERDEREFALIASES.getName() );

        HashMap map = new HashMap();
        NamingEnumeration list = sysRoot.search( "", "(ou = *)", new SearchControls() );
        while ( list.hasMore() )
        {
            SearchResult result = ( SearchResult ) list.next();
            map.put( result.getName(), result.getAttributes() );
        }

        assertEquals( "Expected number of results returned was incorrect!", 3, map.size() );
        assertTrue( map.containsKey( "ou=testing00,ou=system" ) );
        assertTrue( map.containsKey( "ou=testing01,ou=system" ) );
        assertTrue( map.containsKey( "ou=testing02,ou=system" ) );
    }


    public void testSearchSubTreeLeve() throws NamingException
    {
        SearchControls controls = new SearchControls();
        controls.setSearchScope( SearchControls.SUBTREE_SCOPE );
        controls.setDerefLinkFlag( false );
        sysRoot.addToEnvironment( DerefAliasesEnum.JNDI_PROP,
                DerefAliasesEnum.NEVERDEREFALIASES.getName() );

        HashMap map = new HashMap();
        NamingEnumeration list = sysRoot.search( "", "(ou = *)", new SearchControls() );
        while ( list.hasMore() )
        {
            SearchResult result = ( SearchResult ) list.next();
            map.put( result.getName(), result.getAttributes() );
        }

        assertEquals( "Expected number of results returned was incorrect", 5, map.size() );
        assertTrue( map.containsKey( "ou=system" ) );
        assertTrue( map.containsKey( "ou=testing00,ou=system" ) );
        assertTrue( map.containsKey( "ou=testing01,ou=system" ) );
        assertTrue( map.containsKey( "ou=testing02,ou=system" ) );
        assertTrue( map.containsKey( "ou=subtest,ou=testing01,ou=system" ) );
    }
}
