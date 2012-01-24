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
import org.apache.directory.server.core.annotations.CreateDS;
import org.apache.directory.server.core.integ.AbstractLdapTestUnit;
import org.apache.directory.server.core.integ.FrameworkRunner;
import org.junit.Test;
import org.junit.runner.RunWith;


/**
 * Tests the destroyContext methods of the provider.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
@RunWith(FrameworkRunner.class)
@CreateDS(name = "DestroyContextIT")
@ApplyLdifs(
    {
        "dn: ou=testing00,ou=system",
        "objectclass: top",
        "objectclass: organizationalUnit",
        "ou: testing00",
        "",
        "dn: ou=testing01,ou=system",
        "objectclass: top",
        "objectclass: organizationalUnit",
        "ou: testing01",
        "",
        "dn: ou=testing02,ou=system",
        "objectclass: top",
        "objectclass: organizationalUnit",
        "ou: testing02",
        "",
        "dn: ou=subtest,ou=testing01,ou=system",
        "objectclass: top",
        "objectclass: organizationalUnit",
        "ou: subtest"
})
public class DestroyContextIT extends AbstractLdapTestUnit
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
    LdapContext sysRoot = getSystemContext( getService() );

    /*
     * delete ou=testing00,ou=system
     */
    sysRoot.destroySubcontext( "ou=testing00" );

    try
    {
        sysRoot.lookup( "ou=testing00" );
        fail( "ou=testing00, ou=system should not exist" );
    }
    catch ( Exception e )
    {
        assertTrue( e instanceof NamingException );
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
        assertTrue( e instanceof NamingException );
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
        assertTrue( e instanceof NamingException );
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
        assertTrue( e instanceof NamingException );
    }
}

}
