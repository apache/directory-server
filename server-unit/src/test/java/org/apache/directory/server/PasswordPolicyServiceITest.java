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


import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Set;

import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;

import org.apache.directory.server.core.configuration.InterceptorConfiguration;
import org.apache.directory.server.core.configuration.MutableInterceptorConfiguration;
import org.apache.directory.server.core.configuration.MutablePartitionConfiguration;
import org.apache.directory.server.core.configuration.PartitionConfiguration;
import org.apache.directory.server.kerberos.shared.interceptors.PasswordPolicyService;
import org.apache.directory.server.unit.AbstractServerTest;
import org.apache.directory.shared.ldap.message.AttributeImpl;
import org.apache.directory.shared.ldap.message.AttributesImpl;


/**
 * An {@link AbstractServerTest} testing the (@link {@link PasswordPolicyService}.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class PasswordPolicyServiceITest extends AbstractServerTest
{
    private DirContext ctx = null;
    private DirContext users = null;


    /**
     * Set up a partition for EXAMPLE.COM, add the {@link PasswordPolicyService}
     * interceptor, and create a users subcontext.
     */
    public void setUp() throws Exception
    {
        configuration.setAllowAnonymousAccess( false );

        Attributes attrs;
        Set<PartitionConfiguration> pcfgs = new HashSet<PartitionConfiguration>();

        MutablePartitionConfiguration pcfg;

        // Add partition 'example'
        pcfg = new MutablePartitionConfiguration();
        pcfg.setName( "example" );
        pcfg.setSuffix( "dc=example,dc=com" );

        Set<Object> indexedAttrs = new HashSet<Object>();
        indexedAttrs.add( "ou" );
        indexedAttrs.add( "dc" );
        indexedAttrs.add( "objectClass" );
        pcfg.setIndexedAttributes( indexedAttrs );

        attrs = new AttributesImpl( true );
        Attribute attr = new AttributeImpl( "objectClass" );
        attr.add( "top" );
        attr.add( "domain" );
        attrs.put( attr );
        attr = new AttributeImpl( "dc" );
        attr.add( "example" );
        attrs.put( attr );
        pcfg.setContextEntry( attrs );

        pcfgs.add( pcfg );
        configuration.setPartitionConfigurations( pcfgs );

        MutableInterceptorConfiguration interceptorCfg = new MutableInterceptorConfiguration();
        List<InterceptorConfiguration> list = configuration.getInterceptorConfigurations();

        interceptorCfg.setName( "passwordPolicyService" );
        interceptorCfg.setInterceptor( new PasswordPolicyService() );
        list.add( interceptorCfg );
        configuration.setInterceptorConfigurations( list );

        super.setUp();

        Hashtable<String, String> env = new Hashtable<String, String>();
        env.put( "java.naming.factory.initial", "com.sun.jndi.ldap.LdapCtxFactory" );
        env.put( "java.naming.provider.url", "ldap://localhost:" + port + "/dc=example,dc=com" );
        env.put( "java.naming.security.principal", "uid=admin,ou=system" );
        env.put( "java.naming.security.credentials", "secret" );
        env.put( "java.naming.security.authentication", "simple" );
        ctx = new InitialDirContext( env );

        attrs = getOrgUnitAttributes( "users" );
        users = ctx.createSubcontext( "ou=users", attrs );
    }


    /**
     * Tests that passwords that are too short are properly rejected. 
     */
    public void testLength()
    {
        Attributes attrs = getPersonAttributes( "Nelson", "Horatio Nelson", "hnelson", "HN1" );
        try
        {
            users.createSubcontext( "uid=hnelson", attrs );
            fail( "Shouldn't have gotten here." );
        }
        catch ( NamingException ne )
        {
            assertTrue( ne.getMessage().contains( "length too short" ) );
            assertFalse( ne.getMessage().contains( "insufficient character mix" ) );
            assertFalse( ne.getMessage().contains( "contains portions of username" ) );
        }
    }


    /**
     * Tests that passwords with insufficient character mix are properly rejected. 
     */
    public void testCharacterMix()
    {
        Attributes attrs = getPersonAttributes( "Nelson", "Horatio Nelson", "hnelson", "secret" );
        try
        {
            users.createSubcontext( "uid=hnelson", attrs );
            fail( "Shouldn't have gotten here." );
        }
        catch ( NamingException ne )
        {
            assertFalse( ne.getMessage().contains( "length too short" ) );
            assertTrue( ne.getMessage().contains( "insufficient character mix" ) );
            assertFalse( ne.getMessage().contains( "contains portions of username" ) );
        }
    }


    /**
     * Tests that passwords that contain substrings of the username are properly rejected. 
     */
    public void testContainsUsername()
    {
        Attributes attrs = getPersonAttributes( "Nelson", "Horatio Nelson", "hnelson", "A1nelson" );
        try
        {
            users.createSubcontext( "uid=hnelson", attrs );
            fail( "Shouldn't have gotten here." );
        }
        catch ( NamingException ne )
        {
            assertFalse( ne.getMessage().contains( "length too short" ) );
            assertFalse( ne.getMessage().contains( "insufficient character mix" ) );
            assertTrue( ne.getMessage().contains( "contains portions of username" ) );
        }
    }


    /**
     * Tests that passwords with insufficient character mix and that are too
     * short are properly rejected. 
     */
    public void testCharacterMixAndLength()
    {
        Attributes attrs = getPersonAttributes( "Nelson", "Horatio Nelson", "hnelson", "hi" );
        try
        {
            users.createSubcontext( "uid=hnelson", attrs );
            fail( "Shouldn't have gotten here." );
        }
        catch ( NamingException ne )
        {
            assertTrue( ne.getMessage().contains( "length too short" ) );
            assertTrue( ne.getMessage().contains( "insufficient character mix" ) );
            assertFalse( ne.getMessage().contains( "contains portions of username" ) );
        }
    }


    /**
     * Tests that passwords that are too short and that contain substrings of
     * the username are properly rejected.
     */
    public void testLengthAndContainsUsername()
    {
        Attributes attrs = getPersonAttributes( "Bush", "William Bush", "wbush", "bush1" );
        try
        {
            users.createSubcontext( "uid=wbush", attrs );
            fail( "Shouldn't have gotten here." );
        }
        catch ( NamingException ne )
        {
            assertTrue( ne.getMessage().contains( "length too short" ) );
            assertFalse( ne.getMessage().contains( "insufficient character mix" ) );
            assertTrue( ne.getMessage().contains( "contains portions of username" ) );
        }
    }


    /**
     * Tests that passwords with insufficient character mix and that contain substrings of
     * the username are properly rejected.
     */
    public void testCharacterMixAndContainsUsername()
    {
        Attributes attrs = getPersonAttributes( "Nelson", "Horatio Nelson", "hnelson", "hnelson" );
        try
        {
            users.createSubcontext( "uid=hnelson", attrs );
            fail( "Shouldn't have gotten here." );
        }
        catch ( NamingException ne )
        {
            assertFalse( ne.getMessage().contains( "length too short" ) );
            assertTrue( ne.getMessage().contains( "insufficient character mix" ) );
            assertTrue( ne.getMessage().contains( "contains portions of username" ) );
        }
    }


    /**
     * Tests that passwords with insufficient character mix and that are too
     * short and that contain substrings of the username are properly rejected.
     */
    public void testCharacterMixAndLengthAndContainsUsername()
    {
        Attributes attrs = getPersonAttributes( "Bush", "William Bush", "wbush", "bush" );
        try
        {
            users.createSubcontext( "uid=wbush", attrs );
            fail( "Shouldn't have gotten here." );
        }
        catch ( NamingException ne )
        {
            assertTrue( ne.getMessage().contains( "length too short" ) );
            assertTrue( ne.getMessage().contains( "insufficient character mix" ) );
            assertTrue( ne.getMessage().contains( "contains portions of username" ) );
        }
    }


    /**
     * Tear down.
     */
    public void tearDown() throws Exception
    {
        ctx.close();
        ctx = null;
        super.tearDown();
    }


    /**
     * Convenience method for creating a person.
     */
    protected Attributes getPersonAttributes( String sn, String cn, String uid, String userPassword )
    {
        Attributes attrs = new AttributesImpl();
        Attribute ocls = new AttributeImpl( "objectClass" );
        ocls.add( "top" );
        ocls.add( "person" ); // sn $ cn
        ocls.add( "inetOrgPerson" ); // uid
        attrs.put( ocls );
        attrs.put( "cn", cn );
        attrs.put( "sn", sn );
        attrs.put( "uid", uid );
        attrs.put( "userPassword", userPassword );

        return attrs;
    }


    /**
     * Convenience method for creating an organizational unit.
     */
    protected Attributes getOrgUnitAttributes( String ou )
    {
        Attributes attrs = new AttributesImpl();
        Attribute ocls = new AttributeImpl( "objectClass" );
        ocls.add( "top" );
        ocls.add( "organizationalUnit" );
        attrs.put( ocls );
        attrs.put( "ou", ou );

        return attrs;
    }
}
