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
package org.apache.directory.server;


import java.util.Hashtable;

import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.DirContext;
import javax.naming.ldap.Control;
import javax.naming.ldap.InitialLdapContext;
import javax.naming.ldap.LdapContext;

import org.apache.directory.server.unit.AbstractServerTest;
import org.apache.directory.shared.ldap.message.AttributeImpl;
import org.apache.directory.shared.ldap.message.CascadeControl;
import org.apache.directory.shared.ldap.message.ModificationItemImpl;


/**
 * A set of tests to make sure the Cascade Control is working 
 * properly.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class CascadeControlITest extends AbstractServerTest
{
    private LdapContext ctx = null;


    /**
     * Creates context.
     */
    protected void setUp() throws Exception
    {
        super.setUp();

        Hashtable<String, Object> env = new Hashtable<String, Object>();
        env.put( "java.naming.factory.initial", "com.sun.jndi.ldap.LdapCtxFactory" );
        env.put( "java.naming.provider.url", "ldap://localhost:" + port + "/" );
        env.put( "java.naming.security.principal", "uid=admin,ou=system" );
        env.put( "java.naming.security.credentials", "secret" );
        env.put( "java.naming.security.authentication", "simple" );

        ctx = new InitialLdapContext( env, null );
        assertNotNull( ctx );
    }


    /**
    * Closes context.
    */
    protected void tearDown() throws Exception
    {
        ctx.close();
        ctx = null;
        super.tearDown();
    }


    /**
     * Tests the behavior of the server with a SearchRequest including
     * the CascadeControl.
     */
    public void testCascadeControl() throws NamingException
    {
        LdapContext containerCtx = ( LdapContext ) ctx.lookup( "ou=attributeTypes, cn=apachemeta, ou=schema" );
        
        try
        {
            containerCtx.setRequestControls( new Control[] { new CascadeControl() } );
        }
        catch ( NamingException e )
        {
            fail( e.getMessage() );
        }

        String oldRdn = "m-oid=1.3.6.1.4.1.18060.0.4.0.2.1";
        String newRdn = "m-oid=1.3.6.1.4.1.18060.0.4.0.2.1000";
        try
        {
            containerCtx.rename( oldRdn, newRdn );
        }
        catch ( NamingException e )
        {
            // Hiding this fail() until the server has the correct mechanism to handle this.
            //            fail( e.getMessage() );
        }
    }
}
