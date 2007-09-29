/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *  
 *    http://www.apache.org/licenses/LICENSE-2.0
 *  
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License. 
 *  
 */
package org.apache.directory.server.core.jndi;


import org.apache.directory.server.core.configuration.ShutdownConfiguration;
import org.apache.directory.server.core.configuration.SyncConfiguration;
import org.apache.directory.server.core.unit.AbstractAdminTestCase;


/**
 * Tests the shutdown operation on the JNDI provider.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class ShutdownITest extends AbstractAdminTestCase
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
        // for some reason if we don't synch first windows blows chuncks when
        // attempting to delete the db files - perhaps this buys more time rather
        // because there is less to sync when shutting down so shutdown happens
        // faster before the doDelete method is called. Regardless this does
        // deserve some investigation at some point after the bigbang cleanup.
        setContextRoots( "uid=admin,ou=system", "secret", new SyncConfiguration() );
        setContextRoots( "uid=admin,ou=system", "secret", new ShutdownConfiguration() );
        assertNotNull( sysRoot );
        doDelete( configuration.getWorkingDirectory() );
    }


    /**
     *
     *
     * @throws Exception
     */
    public void testShutdownRestart() throws Exception
    {
        setContextRoots( "uid=admin,ou=system", "secret", new SyncConfiguration() );
        setContextRoots( "uid=admin,ou=system", "secret", new ShutdownConfiguration() );
        assertNotNull( sysRoot );

        // restart the system now
        setContextRoots( "uid=admin,ou=system", "secret", configuration );

        // (tearDown is overriden)
        super.tearDown();
        doDelete( configuration.getWorkingDirectory() );
    }
}
