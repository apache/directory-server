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


import java.io.IOException;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Set;

import javax.naming.NameNotFoundException;
import javax.naming.NamingException;
import javax.naming.directory.Attributes;
import javax.naming.ldap.InitialLdapContext;
import javax.naming.ldap.LdapContext;

import org.apache.directory.server.ldap.ExtendedOperationHandler;
import org.apache.directory.server.ldap.LdapConfiguration;
import org.apache.directory.server.ldap.support.extended.StoredProcedureExtendedOperationHandler;
import org.apache.directory.server.unit.AbstractServerTest;
import org.apache.directory.shared.ldap.message.AttributesImpl;
import org.apache.directory.shared.ldap.name.LdapDN;
import org.apache.directory.shared.ldap.sp.BaseJavaStoredProcUtils;
import org.apache.directory.shared.ldap.sp.LdapContextParameter;


/**
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev:$
 */
public class StoredProcedureTest extends AbstractServerTest
{
    private LdapContext ctx = null;
    private LdapContext spCtx = null;

    
    public void setUp() throws Exception
    {
        /////////////////////////////////////////////////////////
        // Enable the Stored Procedure Extended Operation Handler
        /////////////////////////////////////////////////////////
        LdapConfiguration ldapCfg = super.configuration.getLdapConfiguration();
        Set<ExtendedOperationHandler> handlers = new HashSet<ExtendedOperationHandler>( ldapCfg.getExtendedOperationHandlers() );
        handlers.add( new StoredProcedureExtendedOperationHandler() );
        ldapCfg.setExtendedOperationHandlers( handlers );

        super.setUp();

        Hashtable<String, Object> env = new Hashtable<String, Object>();
        env.put( "java.naming.factory.initial", "com.sun.jndi.ldap.LdapCtxFactory" );
        env.put( "java.naming.provider.url", "ldap://localhost:" + port + "/ou=system" );
        env.put( "java.naming.security.principal", "uid=admin,ou=system" );
        env.put( "java.naming.security.credentials", "secret" );
        env.put( "java.naming.security.authentication", "simple" );
        ctx = new InitialLdapContext( env, null );
        
        Attributes spContainer = new AttributesImpl( "objectClass", "top", true );
        spContainer.get( "objectClass" ).add( "organizationalUnit" );
        spContainer.put( "ou", "Stored Procedures" );
        spCtx = ( LdapContext ) ctx.createSubcontext( "ou=Stored Procedures", spContainer );
    }


    public void tearDown() throws Exception
    {
        ctx.close();
        ctx = null;
        
        super.tearDown();
    }
    

    public void testExecuteProcedureWithReturnValue() throws NamingException
    {
        String procedureName = HelloWorldProcedure.class.getName() + ":sayHello";
        
        BaseJavaStoredProcUtils.loadStoredProcedureClass( spCtx, HelloWorldProcedure.class );
        
        Object response = BaseJavaStoredProcUtils.callStoredProcedure( ctx, procedureName, new Object[] { } );
        
        assertEquals( "Hello World!", response );
    }
    

    public void testExecuteProcedureWithParametersAndReturnValue() throws NamingException, IOException
    {
        String procedureName = HelloWorldProcedure.class.getName() + ":sayHelloTo";
        
        BaseJavaStoredProcUtils.loadStoredProcedureClass( spCtx, HelloWorldProcedure.class );
        
        Object response = BaseJavaStoredProcUtils.callStoredProcedure( ctx, procedureName, new Object[] { "Ersin" } );
        
        assertEquals( "Hello Ersin!", response );
    }
    
    
    public void testSPDeleteSubtree() throws NamingException
    {
        String ldif =
            "version: 1\n" +
            "\n" +
            "dn: ou=People,ou=system\n" +
            "ou: People\n" +
            "objectclass: organizationalUnit\n" +
            "objectclass: top\n" +
            "\n" + 
            "dn: cn=John,ou=People,ou=system\n" +
            "objectclass: person\n" +
            "objectclass: top\n" +
            "sn: John\n" +
            "cn: John\n" +
            "\n" +
            "dn: cn=Jane,ou=People,ou=system\n" +
            "objectclass: person\n" +
            "objectclass: top\n" +
            "sn: Jane\n" +
            "cn: Jane\n";
        
        injectEntries( ldif );
        
        BaseJavaStoredProcUtils.loadStoredProcedureClass( spCtx, DITUtilitiesSP.class );
        
        String spName = DITUtilitiesSP.class.getName() + ":deleteSubtree";
        Object[] params = new Object[] { new LdapContextParameter( "ou=system" ),
                                         new LdapDN( "ou=People" ) };
        
        BaseJavaStoredProcUtils.callStoredProcedure( ctx, spName, params );
        
        try
        {
            sysRoot.lookup( "cn=Jane,ou=People" );
            sysRoot.lookup( "cn=John,ou=People" );
            sysRoot.lookup( "ou=People" );
            fail( "We should not have come here." );
        }
        catch ( NameNotFoundException e )
        {
            // Expected
        }
    }
     
}
