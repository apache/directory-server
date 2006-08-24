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


import javax.naming.directory.*;
import javax.naming.NamingException;

import org.apache.directory.server.unit.AbstractServerTest;

import java.util.Hashtable;


/**
 * Verify the use of our new schema and test that we can create entries using it.  
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class SchemaTest extends AbstractServerTest
{
    private DirContext ctx = null;


    /**
     * Make sure embedded instance uses our schema and get a connection to 
     * the embedded server using the SUN JNDI LDAP provider over the wire.
     */
    public void setUp() throws Exception
    {
        // -------------------------------------------------------------------
        // Setup our schema to load       
        // -------------------------------------------------------------------

        // todo 

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
     * Let's test to make sure our schema elements have been installed and are
     * ready to be used.
     */
    public void testSchemaPresence() throws NamingException
    {
    }


    /**
     * Adds a Car objectClass to the DIT under ou=system.
     */
    public void testAddCar() throws NamingException
    {
    }
}
