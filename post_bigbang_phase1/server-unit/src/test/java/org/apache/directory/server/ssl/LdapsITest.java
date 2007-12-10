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
package org.apache.directory.server.ssl;


import org.apache.directory.server.ssl.support.SSLSocketFactory;
import org.apache.directory.server.unit.AbstractServerTest;
import org.apache.directory.shared.ldap.message.AttributeImpl;
import org.apache.directory.shared.ldap.message.AttributesImpl;

import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.IOException;
import java.util.Hashtable;


/**
 * Test case to verify DIREVE-216.  Starts up the server binds via SUN JNDI provider
 * to perform add modify operations on entries.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class LdapsITest extends AbstractServerTest
{
    private static final String RDN = "cn=The Person";

    private DirContext ctx;


    /**
     * Create an entry for a person.
     */
    public void setUp() throws Exception
    {
        super.setUp();

//        int ldapsPort = AvailablePortFinder.getNextAvailable( 8192 );
//
//        LdapServer ldapsServer = new LdapServer( socketAcceptor, directoryService );


        Hashtable<String, String> env = new Hashtable<String, String>();
        env.put( "java.naming.factory.initial", "com.sun.jndi.ldap.LdapCtxFactory" );
        env.put( "java.naming.provider.url", "ldap://localhost:" + port + "/ou=system" );
        env.put( "java.naming.ldap.factory.socket", SSLSocketFactory.class.getName() );
        env.put( "java.naming.security.principal", "uid=admin,ou=system" );
        env.put( "java.naming.security.credentials", "secret" );
        env.put( "java.naming.security.authentication", "simple" );
        ctx = new InitialDirContext( env );
    }


    @Override
    protected void configureLdapServer()
    {
        ldapServer.setEnableLdaps( true );
        ldapServer.setLdapsCertificatePassword( "boguspw" );
//        ldapServer.setIpPort( ldapsPort );

        // Copy the bogus certificate to the certificates directory.
        InputStream in = getClass().getResourceAsStream( "/bogus.cert" );
        ldapServer.getLdapsCertificateFile().getParentFile().mkdirs();

        try
        {
            FileOutputStream out = new FileOutputStream( ldapServer.getLdapsCertificateFile() );

            for ( ;; )
            {
                int c = in.read();
                if ( c < 0 )
                {
                    break;
                }
                out.write( c );
            }

            in.close();
            out.close();
        } catch ( IOException e )
        {
            throw new RuntimeException( e );
        }

    }

    /**
     * Remove the person.
     */
    public void tearDown() throws Exception
    {
        ctx.unbind( RDN );
        ctx.close();
        ctx = null;
        super.tearDown();
    }


    /**
     * Just a little test to check if the connection is made successfully.
     * 
     * @throws NamingException cannot create person
     */
    public void testSetUpTearDown() throws NamingException
    {
        // Create a person
        Attributes attributes = new AttributesImpl( true );
        Attribute attribute = new AttributeImpl( "objectClass" );
        attribute.add( "top" );
        attribute.add( "person" );
        attributes.put( attribute );
        attributes.put( "cn", "The Person" );
        attributes.put( "sn", "Person" );
        attributes.put( "description", "this is a person" );
        DirContext person = ctx.createSubcontext( RDN, attributes );

        assertNotNull( person );
    }
}
