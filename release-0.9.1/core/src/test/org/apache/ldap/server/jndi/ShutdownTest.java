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


import org.apache.ldap.server.AbstractAdminTestCase;
import org.apache.ldap.server.configuration.ShutdownConfiguration;


/**
 * Tests the shutdown operation on the JNDI provider.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class ShutdownTest extends AbstractAdminTestCase
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
        setSysRoot( "uid=admin,ou=system", "secret", new ShutdownConfiguration() );
        assertNotNull( sysRoot );
    }


    /**
     *
     *
     * @throws Exception
     */
    public void testShutdownRestart() throws Exception
    {
        setSysRoot( "uid=admin,ou=system", "secret", new ShutdownConfiguration() );
        assertNotNull( sysRoot );

        // restart the system now
        setSysRoot( "uid=admin,ou=system", "secret", configuration );
        
        // Shutdown again (tearDown is overriden)
        setSysRoot( "uid=admin,ou=system", "secret", new ShutdownConfiguration() );
    }
}
