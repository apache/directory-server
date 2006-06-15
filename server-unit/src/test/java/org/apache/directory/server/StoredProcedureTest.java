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


import javax.naming.directory.*;
import javax.naming.ldap.InitialLdapContext;
import javax.naming.ldap.LdapContext;
import javax.naming.NamingException;

import org.apache.directory.server.ldap.support.extended.StoredProcedureExtendedOperationHandler;
import org.apache.directory.server.unit.AbstractServerTest;
import org.apache.directory.shared.ldap.message.extended.StoredProcedureRequest;
import org.apache.directory.shared.ldap.message.extended.StoredProcedureResponse;

import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Set;


/**
 * Test case to verify DIREVE-216.  Starts up the server binds via SUN JNDI provider
 * to perform add modify operations on entries.
 * 
 * @author szoerner
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev: 379013 $
 */
public class StoredProcedureTest extends AbstractServerTest
{
    private LdapContext ctx = null;


    /**
     * Create an entry for a person.
     */
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

        URL url = getClass().getResource( "HelloWorldProcedure.class" );
        InputStream in = getClass().getResourceAsStream( "HelloWorldProcedure.class" );
        File file = new File( url.getFile() );
        int size = ( int ) file.length();
        byte[] buf = new byte[size];
        in.read( buf );
        in.close();
        
        // set up
        Attributes attributes = new BasicAttributes( "objectClass", "top", true );
        attributes.get( "objectClass" ).add( "javaClass" );
        attributes.put( "fullyQualifiedClassName", HelloWorldProcedure.class.getName() );
        attributes.put( "byteCode", buf );
        ctx.createSubcontext( "fullyQualifiedClassName=" + HelloWorldProcedure.class.getName(), attributes );
    }


    public void tearDown() throws Exception
    {
        ctx.close();
        ctx = null;
    }


    /**
     */
    public void testExecuteProcedure() throws NamingException
    {
        String language = "java";
        String procedure = "org.apache.directory.server.HelloWorldProcedure.sayHello";
        StoredProcedureRequest req = new StoredProcedureRequest( 0, procedure, language );
        StoredProcedureResponse resp = ( StoredProcedureResponse ) ctx.extendedOperation( req );
        assertNotNull( resp );
    }
}
