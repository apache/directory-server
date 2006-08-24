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
package ${groupId};


import java.util.Hashtable;

import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;

import org.apache.directory.server.unit.AbstractServerTest;



/**
 * An example ApacheDS integration test.  
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class ApacheDSITest extends AbstractServerTest
{
    private DirContext ctx = null;


    /**
     * Get a connection to the embedded server using the SUN JNDI LDAP provider
     * over the wire.
     */
    public void setUp() throws Exception
    {
        // Starts up the embedded ApacheDS instance.
        super.setUp();

        // -------------------------------------------------------------------
        // Get a connection (JNDI context) to the LDAP server       
        // -------------------------------------------------------------------

        Hashtable env = new Hashtable();
        env.put( "java.naming.factory.initial", "com.sun.jndi.ldap.LdapCtxFactory" );
        env.put( "java.naming.provider.url", "ldap://localhost:" + port + "/ou=system" );
        env.put( "java.naming.security.principal", "uid=admin,ou=system" );
        env.put( "java.naming.security.credentials", "secret" );
        env.put( "java.naming.security.authentication", "simple" );
        ctx = new InitialDirContext( env );
    }


    /**
     * Closes our context before shuting down the server.
     */
    public void tearDown() throws Exception
    {
        ctx.close();
        super.tearDown();
    }


    /**
     * An example test reads and assert an attribute value in the ou=system
     * context.
     */
    public void testExample() throws NamingException
    {
        Attributes system = ctx.getAttributes( "" );
        assertNotNull( system );
        assertEquals( "system", system.get( "ou" ).get() );
    }
}
