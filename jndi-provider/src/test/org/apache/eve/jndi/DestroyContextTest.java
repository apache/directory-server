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


import javax.naming.NamingException;

import org.apache.eve.exception.EveNameNotFoundException;


/**
 * Tests the destroyContext methods of the provider.
 *
 * @author <a href="mailto:directory-dev@incubator.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class DestroyContextTest extends AbstractJndiTest
{
    protected void setUp() throws Exception
    {
        super.setUp();

        CreateContextTest createTest = new CreateContextTest();
        createTest.setUp();
        createTest.testCreateContexts();
    }


    /**
     * Tests the creation and subsequent read of a new JNDI context under the
     * system context root.
     *
     * @throws NamingException if there are failures
     */
    public void testDestroyContext() throws NamingException
    {
        /*
         * delete ou=testing00,ou=system
         */
        sysRoot.destroySubcontext( "ou=testing00");

        try
        {
            sysRoot.lookup( "ou=testing00" );
            fail( "ou=testing00, ou=system should not exist" );
        }
        catch( NamingException e )
        {
            assertTrue( e instanceof EveNameNotFoundException );
        }

        /*
         * delete ou=subtest,ou=testing01,ou=system
         */
        sysRoot.destroySubcontext( "ou=subtest,ou=testing01");

        try
        {
            sysRoot.lookup( "ou=subtest,ou=testing01" );
            fail( "ou=subtest,ou=testing01,ou=system should not exist" );
        }
        catch( NamingException e )
        {
            assertTrue( e instanceof EveNameNotFoundException );
        }

        /*
         * delete ou=testing01,ou=system
         */
        sysRoot.destroySubcontext( "ou=testing01");

        try
        {
            sysRoot.lookup( "ou=testing01" );
            fail( "ou=testing01, ou=system should not exist" );
        }
        catch( NamingException e )
        {
            assertTrue( e instanceof EveNameNotFoundException );
        }


        /*
         * delete ou=testing01,ou=system
         */
        sysRoot.destroySubcontext( "ou=testing02");

        try
        {
            sysRoot.lookup( "ou=testing02" );
            fail( "ou=testing02, ou=system should not exist" );
        }
        catch( NamingException e )
        {
            assertTrue( e instanceof EveNameNotFoundException );
        }
    }


}
