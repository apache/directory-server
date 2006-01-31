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
package org.apache.ldap.server.dumptool;


import java.util.Hashtable;
import javax.naming.NamingException;
import javax.naming.ldap.InitialLdapContext;
import javax.naming.ldap.LdapContext;

import org.apache.ldap.common.message.extended.GracefulShutdownRequest;


/**
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class GracefulShutdown
{
    private int port = 10389;
    private String host = "localhost";
    private String password = "secret";

    
    public void execute() throws NamingException
    {
        Hashtable env = new Hashtable();
        env.put( "java.naming.factory.initial", "com.sun.jndi.ldap.LdapCtxFactory" );
        env.put( "java.naming.provider.url", "ldap://" + host + ":" + port );
        env.put( "java.naming.security.principal", "uid=admin,ou=system" );
        env.put( "java.naming.security.credentials", password );
        env.put( "java.naming.security.authentication", "simple" );

        LdapContext ctx = new InitialLdapContext( env, null );
        ctx.extendedOperation( new GracefulShutdownRequest( 0, 10, 10 ) );
        ctx.close();
    }


    public static void main( String[] args ) throws Exception
    {
        GracefulShutdown command = new GracefulShutdown();
        command.execute();
    }
}
