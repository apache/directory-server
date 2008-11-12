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
package org.apache.directory.server.operations.bind;


import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

import javax.naming.Context;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.NoPermissionException;
import javax.naming.OperationNotSupportedException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.BasicAttribute;
import javax.naming.directory.BasicAttributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;
import javax.naming.ldap.InitialLdapContext;

import org.apache.directory.server.core.DefaultDirectoryService;
import org.apache.directory.server.core.DirectoryService;
import org.apache.directory.server.core.integ.IntegrationUtils;
import org.apache.directory.server.core.integ.Level;
import org.apache.directory.server.core.integ.annotations.ApplyLdifs;
import org.apache.directory.server.core.integ.annotations.CleanupLevel;
import org.apache.directory.server.core.integ.annotations.Factory;
import org.apache.directory.server.integ.LdapServerFactory;
import org.apache.directory.server.integ.SiRunner;
import org.apache.directory.server.ldap.LdapService;
import org.apache.directory.server.ldap.handlers.bind.MechanismHandler;
import org.apache.directory.server.ldap.handlers.bind.SimpleMechanismHandler;
import org.apache.directory.server.ldap.handlers.bind.cramMD5.CramMd5MechanismHandler;
import org.apache.directory.server.ldap.handlers.bind.digestMD5.DigestMd5MechanismHandler;
import org.apache.directory.server.ldap.handlers.bind.gssapi.GssapiMechanismHandler;
import org.apache.directory.server.ldap.handlers.bind.ntlm.NtlmMechanismHandler;
import org.apache.directory.server.ldap.handlers.extended.StoredProcedureExtendedOperationHandler;
import org.apache.directory.server.core.partition.impl.btree.jdbm.JdbmPartition;
import org.apache.directory.shared.asn1.util.Asn1StringUtils;
import org.apache.directory.shared.ldap.constants.SupportedSaslMechanisms;
import org.apache.directory.shared.ldap.message.MutableControl;
import org.apache.directory.shared.ldap.util.ArrayUtils;
import org.apache.mina.transport.socket.nio.NioSocketAcceptor;
import org.apache.mina.util.AvailablePortFinder;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.fail;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;


/**
 * A set of miscellaneous tests.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev: 682556 $
 */
@RunWith ( SiRunner.class ) 
@CleanupLevel ( Level.CLASS )
@Factory ( MiscBindIT.Factory.class )
@ApplyLdifs( {
    // Entry #0
    "dn: dc=aPache,dc=org\n" +
    "dc: aPache\n" +
    "objectClass: top\n" +
    "objectClass: domain\n\n"
    }
)
public class MiscBindIT
{
    public static LdapService ldapService;

    
    public static class Factory implements LdapServerFactory
    {
        public LdapService newInstance() throws Exception
        {
            DirectoryService service = new DefaultDirectoryService();
            IntegrationUtils.doDelete( service.getWorkingDirectory() );
            service.getChangeLog().setEnabled( true );
            service.setAllowAnonymousAccess( true );
            service.setShutdownHookEnabled( false );

            JdbmPartition apache = new JdbmPartition();
            apache.setId( "apache" );

            // @TODO need to make this configurable for the system partition
            apache.setCacheSize( 500 );
            apache.setSuffix( "dc=aPache,dc=org" );
            apache.setId( "apache" );
            service.addPartition( apache );

            // change the working directory to something that is unique
            // on the system and somewhere either under target directory
            // or somewhere in a temp area of the machine.

            LdapService ldapService = new LdapService();
            ldapService.setDirectoryService( service );
            ldapService.setSocketAcceptor( new NioSocketAcceptor() );
            ldapService.setIpPort( AvailablePortFinder.getNextAvailable( 1024 ) );
            ldapService.setAllowAnonymousAccess( true );
            ldapService.addExtendedOperationHandler( new StoredProcedureExtendedOperationHandler() );

            // Setup SASL Mechanisms
            
            Map<String, MechanismHandler> mechanismHandlerMap = new HashMap<String,MechanismHandler>();
            mechanismHandlerMap.put( SupportedSaslMechanisms.PLAIN, new SimpleMechanismHandler() );

            CramMd5MechanismHandler cramMd5MechanismHandler = new CramMd5MechanismHandler();
            mechanismHandlerMap.put( SupportedSaslMechanisms.CRAM_MD5, cramMd5MechanismHandler );

            DigestMd5MechanismHandler digestMd5MechanismHandler = new DigestMd5MechanismHandler();
            mechanismHandlerMap.put( SupportedSaslMechanisms.DIGEST_MD5, digestMd5MechanismHandler );

            GssapiMechanismHandler gssapiMechanismHandler = new GssapiMechanismHandler();
            mechanismHandlerMap.put( SupportedSaslMechanisms.GSSAPI, gssapiMechanismHandler );

            NtlmMechanismHandler ntlmMechanismHandler = new NtlmMechanismHandler();
            mechanismHandlerMap.put( SupportedSaslMechanisms.NTLM, ntlmMechanismHandler );
            mechanismHandlerMap.put( SupportedSaslMechanisms.GSS_SPNEGO, ntlmMechanismHandler );

            ldapService.setSaslMechanismHandlers( mechanismHandlerMap );

            return ldapService;
        }
    }
    
    
    
    private boolean oldAnnonymousAccess;
    
    
    @Before
    public void recordAnnonymous() throws NamingException
    {
        oldAnnonymousAccess = ldapService.getDirectoryService().isAllowAnonymousAccess();
    }
    
    
    @After
    public void revertAnonnymous()
    {
        ldapService.getDirectoryService().setAllowAnonymousAccess( oldAnnonymousAccess );
        ldapService.setAllowAnonymousAccess( oldAnnonymousAccess );
    }

    
    /**
     * Test to make sure anonymous binds are disabled when going through
     * the wire protocol.
     *
     * @throws Exception if anything goes wrong
     */
    @Test
    public void testDisableAnonymousBinds() throws Exception
    {
        ldapService.getDirectoryService().setAllowAnonymousAccess( false );
        ldapService.setAllowAnonymousAccess( false );
        
        // Use the SUN JNDI provider to hit server port and bind as anonymous
        InitialDirContext ic = null;
        final Hashtable<String, Object> env = new Hashtable<String, Object>();

        env.put( Context.PROVIDER_URL, "ldap://localhost:" + ldapService.getIpPort() + "/ou=system" );
        env.put( Context.SECURITY_AUTHENTICATION, "none" );
        env.put( Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory" );

        boolean connected = false;
        while ( !connected )
        {
            try
            {
                ic = new InitialDirContext( env );
                connected = true;
            }
            catch ( Exception e )
            {
            	// We should not get here
            	fail();
            }
        }

        ldapService.getDirectoryService().setAllowAnonymousAccess( false );
        
        try
        {
            ic.search( "", "(objectClass=*)", new SearchControls() );
            fail( "If anonymous binds are disabled we should never get here!" );
        }
        catch ( NoPermissionException e )
        {
        }

        Attributes attrs = new BasicAttributes( true );
        Attribute oc = new BasicAttribute( "objectClass" );
        attrs.put( oc );
        oc.add( "top" );
        oc.add( "organizationalUnit" );

        try
        {
            ic.createSubcontext( "ou=blah", attrs );
        }
        catch ( NoPermissionException e )
        {
        }
    }


    /**
     * Test to make sure anonymous binds are allowed on the RootDSE even when disabled
     * in general when going through the wire protocol.
     *
     * @throws Exception if anything goes wrong
     */
    @Test
    public void testEnableAnonymousBindsOnRootDSE() throws Exception
    {
        ldapService.getDirectoryService().setAllowAnonymousAccess( true );

        // Use the SUN JNDI provider to hit server port and bind as anonymous
        Hashtable<String, Object> env = new Hashtable<String, Object>();

        env.put( Context.PROVIDER_URL, "ldap://localhost:" + ldapService.getIpPort() + "/" );
        env.put( Context.SECURITY_AUTHENTICATION, "none" );
        env.put( Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory" );

        InitialDirContext ctx = new InitialDirContext( env );
        SearchControls cons = new SearchControls();
        cons.setSearchScope( SearchControls.OBJECT_SCOPE );
        NamingEnumeration<SearchResult> list = ctx.search( "", "(objectClass=*)", cons );
        
        SearchResult result = null;
        
        if ( list.hasMore() )
        {
            result = list.next();
        }
        
        assertFalse( list.hasMore() );
        list.close();

        assertNotNull( result );
        assertEquals( "", result.getName().trim() );
    }


    /**
     * Test to make sure that if anonymous binds are allowed a user may search
     * within a a partition.
     *
     * @throws Exception if anything goes wrong
     */
    @Test
    public void testAnonymousBindsEnabledBaseSearch() throws Exception
    {
        ldapService.getDirectoryService().setAllowAnonymousAccess( true );

        // Use the SUN JNDI provider to hit server port and bind as anonymous
        Hashtable<String, Object> env = new Hashtable<String, Object>();

        env.put( Context.PROVIDER_URL, "ldap://localhost:" + ldapService.getIpPort() + "/" );
        env.put( Context.SECURITY_AUTHENTICATION, "none" );
        env.put( Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory" );

        InitialDirContext ctx = new InitialDirContext( env );
        SearchControls cons = new SearchControls();
        cons.setSearchScope( SearchControls.OBJECT_SCOPE );
        NamingEnumeration<SearchResult> list = ctx.search( "dc=apache,dc=org", "(objectClass=*)", cons );
        SearchResult result = null;
        
        if ( list.hasMore() )
        {
            result = list.next();
        }
        
        assertFalse( list.hasMore() );
        list.close();

        assertNotNull( result );
        assertNotNull( result.getAttributes().get( "dc" ) );
    }


    /**
     * Reproduces the problem with
     * <a href="http://issues.apache.org/jira/browse/DIREVE-239">DIREVE-239</a>.
     *
     * @throws Exception if anything goes wrong
     */
    @Test
    public void testAdminAccessBug() throws Exception
    {
        ldapService.getDirectoryService().setAllowAnonymousAccess( true );

        // Use the SUN JNDI provider to hit server port and bind as anonymous

        final Hashtable<String, Object> env = new Hashtable<String, Object>();

        env.put( Context.PROVIDER_URL, "ldap://localhost:" + ldapService.getIpPort() );
        env.put( "java.naming.ldap.version", "3" );
        env.put( Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory" );

        Attributes attributes = new BasicAttributes( true );
        Attribute objectClass = new BasicAttribute( "objectClass" );
        objectClass.add( "top" );
        objectClass.add( "organizationalUnit" );
        attributes.put( objectClass );
        attributes.put( "ou", "blah" );
        InitialDirContext ctx = new InitialDirContext( env );
        ctx.createSubcontext( "ou=blah,ou=system", attributes );
        SearchControls controls = new SearchControls();
        controls.setSearchScope( SearchControls.OBJECT_SCOPE );
        controls.setReturningAttributes( new String[]
                {"+"} );
        NamingEnumeration<SearchResult> list = ctx.search( "ou=blah,ou=system", "(objectClass=*)", controls );
        SearchResult result = list.next();
        list.close();
        Attribute creatorsName = result.getAttributes().get( "creatorsName" );
        assertEquals( "", creatorsName.get() );
    }


    /**
     * Test case for <a href="http://issues.apache.org/jira/browse/DIREVE-284" where users in
     * mixed case partitions were not able to authenticate properly.  This test case creates
     * a new partition under dc=aPache,dc=org, it then creates the example user in the JIRA
     * issue and attempts to authenticate as that user.
     *
     * @throws Exception if the user cannot authenticate or test fails
     */
    @Test
    public void testUserAuthOnMixedCaseSuffix() throws Exception
    {
        ldapService.getDirectoryService().setAllowAnonymousAccess( true );

        Hashtable<String, Object> env = new Hashtable<String, Object>();

        env.put( Context.PROVIDER_URL, "ldap://localhost:" + ldapService.getIpPort() + "/dc=aPache,dc=org" );
        env.put( "java.naming.ldap.version", "3" );
        env.put( Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory" );
        InitialDirContext ctx = new InitialDirContext( env );
        Attributes attrs = ctx.getAttributes( "" );
        assertTrue( attrs.get( "dc" ).get().equals( "aPache" ) );

        Attributes user = new BasicAttributes( "cn", "Kate Bush", true );
        Attribute oc = new BasicAttribute( "objectClass" );
        oc.add( "top" );
        oc.add( "person" );
        oc.add( "organizationalPerson" );
        oc.add( "inetOrgPerson" );
        user.put( oc );
        user.put( "sn", "Bush" );
        user.put( "userPassword", "Aerial" );
        ctx.createSubcontext( "cn=Kate Bush", user );

        env.put( Context.SECURITY_AUTHENTICATION, "simple" );
        env.put( Context.SECURITY_CREDENTIALS, "Aerial" );
        env.put( Context.SECURITY_PRINCIPAL, "cn=Kate Bush,dc=aPache,dc=org" );

        InitialDirContext userCtx = new InitialDirContext( env );
        assertNotNull( userCtx );
    }


    @Test
    public void testFailureWithUnsupportedControl() throws Exception
    {
        MutableControl unsupported = new MutableControl()
        {
            boolean isCritical = true;
            private static final long serialVersionUID = 1L;


            @SuppressWarnings("unused")
            public String getType()
            {
                return "1.1.1.1";
            }


            public void setID( String oid )
            {
            }


            @SuppressWarnings("unused")
            public byte[] getValue()
            {
                return new byte[0];
            }


            @SuppressWarnings("unused")
            public void setValue( byte[] value )
            {
            }


            public boolean isCritical()
            {
                return isCritical;
            }


            public void setCritical( boolean isCritical )
            {
                this.isCritical = isCritical;
            }


            public String getID()
            {
                return "1.1.1.1";
            }


            public byte[] getEncodedValue()
            {
                return new byte[0];
            }
        };
        
        ldapService.getDirectoryService().setAllowAnonymousAccess( true );
        
        Hashtable<String, Object> env = new Hashtable<String, Object>();

        env.put( Context.PROVIDER_URL, "ldap://localhost:" + ldapService.getIpPort() + "/ou=system" );
        env.put( "java.naming.ldap.version", "3" );
        env.put( Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory" );
        env.put( Context.SECURITY_AUTHENTICATION, "simple" );
        env.put( Context.SECURITY_CREDENTIALS, "secret" );
        env.put( Context.SECURITY_PRINCIPAL, "uid=admin,ou=system" );
        InitialLdapContext ctx = new InitialLdapContext( env, null );

        Attributes user = new BasicAttributes( "cn", "Kate Bush", true );
        Attribute oc = new BasicAttribute( "objectClass" );
        oc.add( "top" );
        oc.add( "person" );
        oc.add( "organizationalPerson" );
        oc.add( "inetOrgPerson" );
        user.put( oc );
        user.put( "sn", "Bush" );
        user.put( "userPassword", "Aerial" );
        ctx.setRequestControls( new MutableControl[]
                {unsupported} );

        try
        {
            ctx.createSubcontext( "cn=Kate Bush", user );
        }
        catch ( OperationNotSupportedException e )
        {
        }

        unsupported.setCritical( false );
        DirContext kate = ctx.createSubcontext( "cn=Kate Bush", user );
        assertNotNull( kate );
        assertTrue( ArrayUtils.isEquals( Asn1StringUtils.getBytesUtf8( "Aerial" ), kate.getAttributes( "" ).get(
                "userPassword" ).get() ) );
    }
}
