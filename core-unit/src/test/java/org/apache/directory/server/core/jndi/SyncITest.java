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


import javax.naming.directory.Attributes;

import org.apache.directory.server.core.configuration.SyncConfiguration;
import org.apache.directory.server.core.unit.AbstractAdminTestCase;


/**
 * Tests the sync operation on the JNDI provider.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class SyncITest extends AbstractAdminTestCase
{
    /**
     * Makes sure the changes to the JNDI provider take effect where a sync op
     * does not throw an exception due to a null context being returned.
     *
     * @throws Exception if the test fails by generating a null context
     */
    public void testSyncNoException() throws Exception
    {
        sysRoot = setSysRoot( "uid=admin,ou=system", "secret", new SyncConfiguration() );
        assertNotNull( sysRoot );
    }


    /**
     * Makes sure entries can still be accessed after a sync operation.
     * Considering the cache I don't know just how effective such a test is.
     *
     * @throws Exception if the test fails to retrieve and verify an entry
     */
    public void testPostSyncLookup() throws Exception
    {
        sysRoot = setSysRoot( "uid=admin,ou=system", "secret", new SyncConfiguration() );

        Attributes users = sysRoot.getAttributes( "ou=users" );

        // assert making sure the entry is ok
        assertNotNull( users );
        assertNotNull( users.get( "ou" ) );
        assertTrue( users.get( "ou" ).contains( "users" ) );
        assertNotNull( users.get( "objectClass" ) );
        assertTrue( users.get( "objectClass" ).contains( "top" ) );
        assertTrue( users.get( "objectClass" ).contains( "organizationalUnit" ) );
    }
}
