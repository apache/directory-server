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


import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.Hashtable;

import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;

import org.apache.directory.server.ssl.support.SSLSocketFactory;
import org.apache.directory.server.unit.AbstractServerTest;
import org.apache.directory.shared.ldap.message.AttributeImpl;
import org.apache.directory.shared.ldap.message.AttributesImpl;
import org.apache.mina.util.AvailablePortFinder;


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

    private DirContext ctx = null;


    /**
     * Create an entry for a person.
     */
    public void setUp() throws Exception
    {
        doDelete( configuration.getWorkingDirectory() );

        int ldapsPort = AvailablePortFinder.getNextAvailable( 8192 );
        configuration.setEnableLdaps( true );
        configuration.setLdapsCertificatePassword( "boguspw" );
        configuration.setLdapsPort( ldapsPort );

        // Copy the bogus certificate to the certificates directory.
        InputStream in = getClass().getResourceAsStream( "/bogus.cert" );
        configuration.getLdapsCertificateFile().getParentFile().mkdirs();

        FileOutputStream out = new FileOutputStream( configuration.getLdapsCertificateFile() );

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

        doDelete = false;
        super.setUp();
        doDelete = true;

        Hashtable env = new Hashtable();
        env.put( "java.naming.factory.initial", "com.sun.jndi.ldap.LdapCtxFactory" );
        env.put( "java.naming.provider.url", "ldap://localhost:" + ldapsPort + "/ou=system" );
        env.put( "java.naming.ldap.factory.socket", SSLSocketFactory.class.getName() );
        env.put( "java.naming.security.principal", "uid=admin,ou=system" );
        env.put( "java.naming.security.credentials", "secret" );
        env.put( "java.naming.security.authentication", "simple" );
        ctx = new InitialDirContext( env );
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
