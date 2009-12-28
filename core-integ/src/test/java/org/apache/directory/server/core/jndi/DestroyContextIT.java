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


import static org.apache.directory.server.core.integ.IntegrationUtils.getSystemContext;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import javax.naming.NamingException;
import javax.naming.ldap.LdapContext;

import org.apache.directory.server.core.annotations.ApplyLdifs;
import org.apache.directory.server.core.integ.AbstractTestUnit;
import org.apache.directory.server.core.integ.FrameworkRunner;
import org.apache.directory.shared.ldap.exception.LdapNameNotFoundException;
import org.junit.Test;
import org.junit.runner.RunWith;


/**
 * Tests the destroyContext methods of the provider.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
@RunWith ( FrameworkRunner.class )
@ApplyLdifs({
    "dn: ou=testing00,ou=system\n" +
    "objectclass: top\n" +
    "objectclass: organizationalUnit\n" +
    "ou: testing00\n" +
    "\n" +
    "dn: ou=testing01,ou=system\n" +
    "objectclass: top\n" +
    "objectclass: organizationalUnit\n" +
    "ou: testing01\n" +
    "\n" +
    "dn: ou=testing02,ou=system\n" +
    "objectclass: top\n" +
    "objectclass: organizationalUnit\n" +
    "ou: testing02\n" +
    "\n" +
    "dn: ou=subtest,ou=testing01,ou=system\n" +
    "objectclass: top\n" +
    "objectclass: organizationalUnit\n" +
    "ou: subtest\n"
})
public class DestroyContextIT extends AbstractTestUnit
{
    /**
     * Tests the creation and subsequent read of a new JNDI context under the
     * system context root.
     *
     * @throws NamingException if there are failures
     */
    @Test
    public void testDestroyContext() throws Exception
    {
        LdapContext sysRoot = getSystemContext( service );

        /*
         * delete ou=testing00,ou=system
         */
        sysRoot.destroySubcontext( "ou=testing00" );

        try
        {
            sysRoot.lookup( "ou=testing00" );
            fail( "ou=testing00, ou=system should not exist" );
        }
        catch ( NamingException e )
        {
            assertTrue( e instanceof LdapNameNotFoundException );
        }

        /*
         * delete ou=subtest,ou=testing01,ou=system
         */
        sysRoot.destroySubcontext( "ou=subtest,ou=testing01" );

        try
        {
            sysRoot.lookup( "ou=subtest,ou=testing01" );
            fail( "ou=subtest,ou=testing01,ou=system should not exist" );
        }
        catch ( NamingException e )
        {
            assertTrue( e instanceof LdapNameNotFoundException );
        }

        /*
         * delete ou=testing01,ou=system
         */
        sysRoot.destroySubcontext( "ou=testing01" );

        try
        {
            sysRoot.lookup( "ou=testing01" );
            fail( "ou=testing01, ou=system should not exist" );
        }
        catch ( NamingException e )
        {
            assertTrue( e instanceof LdapNameNotFoundException );
        }

        /*
         * delete ou=testing01,ou=system
         */
        sysRoot.destroySubcontext( "ou=testing02" );

        try
        {
            sysRoot.lookup( "ou=testing02" );
            fail( "ou=testing02, ou=system should not exist" );
        }
        catch ( NamingException e )
        {
            assertTrue( e instanceof LdapNameNotFoundException );
        }
    }

}
