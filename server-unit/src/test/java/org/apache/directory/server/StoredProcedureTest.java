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
package org.apache.directory.server;


import java.io.IOException;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Set;

import javax.naming.NamingException;
import javax.naming.ldap.InitialLdapContext;
import javax.naming.ldap.LdapContext;

import org.apache.directory.server.ldap.support.extended.StoredProcedureExtendedOperationHandler;
import org.apache.directory.server.unit.AbstractServerTest;
import org.apache.directory.shared.ldap.sp.JavaStoredProcedureUtils;


/**
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev:$
 */
public class StoredProcedureTest extends AbstractServerTest
{
    private LdapContext ctx = null;

    
    public void setUp() throws Exception
    {
        Set handlers = new HashSet( super.configuration.getExtendedOperationHandlers() );
        handlers.add( new StoredProcedureExtendedOperationHandler() );
        super.configuration.setExtendedOperationHandlers( handlers );
        
        super.setUp();

        Hashtable env = new Hashtable();
        env.put( "java.naming.factory.initial", "com.sun.jndi.ldap.LdapCtxFactory" );
        env.put( "java.naming.provider.url", "ldap://localhost:" + port + "/ou=system" );
        env.put( "java.naming.security.principal", "uid=admin,ou=system" );
        env.put( "java.naming.security.credentials", "secret" );
        env.put( "java.naming.security.authentication", "simple" );
        ctx = new InitialLdapContext( env, null );

        JavaStoredProcedureUtils.loadStoredProcedureClass( ctx, HelloWorldProcedure.class.getName(), getClass() );
    }


    public void tearDown() throws Exception
    {
        ctx.close();
        ctx = null;
        
        super.tearDown();
    }
    

    public void testExecuteProcedure() throws NamingException
    {
        String procedureName = HelloWorldProcedure.class.getName() + ".sayHello";
        
        Object response = JavaStoredProcedureUtils.callStoredProcedure( ctx, procedureName, new Object[] { } );
        
        assertEquals( "Hello World!", response );
    }
    

    public void testExecuteProcedureWithParameters() throws NamingException, IOException
    {
        String procedureName = HelloWorldProcedure.class.getName() + ".sayHelloTo";
        
        Object response = JavaStoredProcedureUtils.callStoredProcedure( ctx, procedureName, new Object[] { "Ersin" } );
        
        assertEquals( "Hello Ersin!", response );
    }
    
}
