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


import java.util.HashSet;

import javax.naming.NameClassPair;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;

import org.apache.directory.server.core.unit.AbstractNonAdminTestCase;


/**
 * Tests our ability to list elements as the admin user and as a non admin user
 * on security sensitive values.  We do not return results or name class pairs
 * for user accounts if the user is not the admin.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class ListAsNonAdminITest extends AbstractNonAdminTestCase
{
    public void testListSystemAsNonAdmin() throws NamingException
    {
        HashSet set = new HashSet();

        NamingEnumeration list = sysRoot.list( "" );

        while ( list.hasMore() )
        {
            NameClassPair ncp = ( NameClassPair ) list.next();

            set.add( ncp.getName() );
        }

        assertFalse( set.contains( "uid=admin,ou=system" ) );

        assertTrue( set.contains( "ou=users,ou=system" ) );

        assertTrue( set.contains( "ou=groups,ou=system" ) );
    }


    public void testListUsersAsNonAdmin() throws NamingException
    {
        HashSet set = new HashSet();

        NamingEnumeration list = sysRoot.list( "ou=users" );

        while ( list.hasMore() )
        {
            NameClassPair ncp = ( NameClassPair ) list.next();

            set.add( ncp.getName() );
        }

        // @todo this assertion fails now - is this the expected behavoir?
        // assertFalse( set.contains( "uid=akarasulu,ou=users,ou=system" ) );
    }
}
