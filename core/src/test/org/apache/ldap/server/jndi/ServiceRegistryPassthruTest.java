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
import org.apache.mina.registry.ServiceRegistry;
import org.apache.mina.registry.SimpleServiceRegistry;


/**
 * Tests to make sure the frontend passthru property actually works.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class ServiceRegistryPassthruTest extends AbstractCoreTest
{
    private ServiceRegistry registry;


    protected void setUp() throws Exception
    {
        registry = new SimpleServiceRegistry();

        if ( getName().equals( "testUsePassthru" ) )
        {
            configuration.setMinaServiceRegistry( registry );
        }

        super.setUp();
    }


    protected void tearDown() throws Exception
    {
        super.tearDown();
        registry.unbindAll();
    }


    public void testUsePassthru() throws Exception
    {
        assertEquals( 1, registry.getAllServices().size() );
    }


    public void testDoNotUsePassthru() throws Exception
    {
        assertEquals( 0, registry.getAllServices().size() );
    }
}
