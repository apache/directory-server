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


import javax.naming.NamingException;

import org.apache.apseda.DefaultFrontend;
import org.apache.apseda.DefaultFrontendFactory;
import org.apache.apseda.DefaultFrontendFactory;
import org.apache.apseda.DefaultFrontend;
import org.apache.ldap.server.AbstractServerTest;


/**
 * Tests to make sure the frontend passthru property actually works.
 *
 * @author <a href="mailto:directory-dev@incubator.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class FrontendPassthruTest extends AbstractServerTest
{
    DefaultFrontend fe;


    protected void setUp() throws Exception
    {
        if ( getName().equals( "testUsePassthru" ) )
        {
            fe = ( DefaultFrontend ) new DefaultFrontendFactory().create();
            super.extras.put( EnvKeys.PASSTHRU, fe );
        }

        super.setUp();
    }


    protected void tearDown() throws Exception
    {
        super.tearDown();
        fe = null;
    }


    public void testUsePassthru() throws NamingException
    {
        assertTrue( sysRoot.getEnvironment().containsKey( EnvKeys.PASSTHRU ) );
        assertEquals( String.class, sysRoot.getEnvironment().get( EnvKeys.PASSTHRU ).getClass() );
        assertEquals( "Handoff Succeeded!", sysRoot.getEnvironment().get( EnvKeys.PASSTHRU ) );
    }


    public void testDoNotUsePassthru() throws NamingException
    {
        assertFalse( sysRoot.getEnvironment().containsKey( EnvKeys.PASSTHRU ) );
    }
}
