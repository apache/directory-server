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


import org.apache.ldap.server.AbstractCoreTest;


/**
 * Tests the shutdown operation on the JNDI provider.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class ShutdownTest extends AbstractCoreTest
{
    protected void tearDown() throws Exception
    {
        // since we shutdown the system already there is no point why the
        // tearDown code should try to shut it down as well - in fact the
        // tearDown super method will throw an LdapServiceUnavailableException
        sysRoot = null;
    }


    /**
     *
     * @throws Exception if the test fails by generating a null context
     */
    public void testShutdownNonNullContext() throws Exception
    {
        overrides.put( EnvKeys.SHUTDOWN, "true" );

        try
        {
            setSysRoot( "uid=admin,ou=system", "secret" );
        }
        finally
        {
            overrides.remove( EnvKeys.SHUTDOWN );
        }

        assertNotNull( sysRoot );
    }


    /**
     *
     *
     * @throws Exception
     */
    public void testShutdownRestart() throws Exception
    {
        overrides.put( EnvKeys.SHUTDOWN, "true" );

        try
        {
            setSysRoot( "uid=admin,ou=system", "secret" );
        }
        finally
        {
            overrides.remove( EnvKeys.SHUTDOWN );
        }

        assertNotNull( sysRoot );

        // restart the system now
        setSysRoot( "uid=admin,ou=system", "secret" );
    }
}
