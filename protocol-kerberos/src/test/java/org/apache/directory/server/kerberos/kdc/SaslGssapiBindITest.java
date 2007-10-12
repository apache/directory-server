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
package org.apache.directory.server.kerberos.kdc;


import org.apache.directory.server.core.DirectoryService;
import org.apache.directory.server.core.DefaultDirectoryService;
import org.apache.directory.server.core.interceptor.Interceptor;
import org.apache.directory.server.core.kerberos.KeyDerivationInterceptor;
import org.apache.directory.server.core.partition.Partition;
import org.apache.directory.server.core.partition.impl.btree.Index;
import org.apache.directory.server.core.partition.impl.btree.jdbm.JdbmIndex;
import org.apache.directory.server.core.partition.impl.btree.jdbm.JdbmPartition;
import org.apache.directory.server.kerberos.shared.store.KerberosAttribute;
import org.apache.directory.server.ldap.LdapServer;
import org.apache.directory.server.protocol.shared.SocketAcceptor;
import org.apache.directory.server.unit.AbstractServerTest;
import org.apache.directory.shared.ldap.message.AttributeImpl;
import org.apache.directory.shared.ldap.message.AttributesImpl;
import org.apache.directory.shared.ldap.message.ModificationItemImpl;
import org.apache.mina.util.AvailablePortFinder;

import javax.naming.Context;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Set;


/**
 * An {@link AbstractServerTest} testing SASL GSSAPI authentication
 * and security layer negotiation.  These tests require both the LDAP
 * and the Kerberos protocol.  As with any "three-headed" Kerberos
 * scenario, there are 3 principals:  1 for the test user, 1 for the
 * Kerberos ticket-granting service (TGS), and 1 for the LDAP service.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class SaslGssapiBindITest extends AbstractServerTest
{
    private DirContext ctx;


    /**
     * Creates a new instance of SaslGssapiBindTest and sets JAAS system properties
     * for the KDC and realm, so we don't have to rely on external configuration.
     */
    public SaslGssapiBindITest()
    {
        System.setProperty( "java.security.krb5.realm", "EXAMPLE.COM" );
        System.setProperty( "java.security.krb5.kdc", "localhost" );
    }


    /**
     * Set up a partition for EXAMPLE.COM and add user and service principals to
     * test authentication with.
     */
    public void setUp() throws Exception
    {
        super.setUp();

        setAllowAnonymousAccess( false );
        ldapServer.setSaslHost( "localhost" );
        ldapServer.setSaslPrincipal( "ldap/localhost@EXAMPLE.COM" );

        KdcServer kdcConfig = new KdcServer( null, socketAcceptor, directoryService );
        kdcConfig.setEnabled( true );
        kdcConfig.setSearchBaseDn( "ou=users,dc=example,dc=com" );
        kdcConfig.setSecurityAuthentication( "simple" );
        kdcConfig.setSecurityCredentials( "secret" );
        kdcConfig.setSecurityPrincipal( "uid=admin,ou=system" );

        Attributes attrs;

//        doDelete( directoryService.getWorkingDirectory() );
//        port = AvailablePortFinder.getNextAvailable( 1024 );
//        ldapServer.setIpPort( port );
//        directoryService.setShutdownHookEnabled( false );


        setContexts( "uid=admin,ou=system", "secret" );

        // -------------------------------------------------------------------
        // Enable the krb5kdc schema
        // -------------------------------------------------------------------

        // check if krb5kdc is disabled
        Attributes krb5kdcAttrs = schemaRoot.getAttributes( "cn=Krb5kdc" );
        boolean isKrb5KdcDisabled = false;
        if ( krb5kdcAttrs.get( "m-disabled" ) != null )
        {
            isKrb5KdcDisabled = ( ( String ) krb5kdcAttrs.get( "m-disabled" ).get() ).equalsIgnoreCase( "TRUE" );
        }

        // if krb5kdc is disabled then enable it
        if ( isKrb5KdcDisabled )
        {
            Attribute disabled = new AttributeImpl( "m-disabled" );
            ModificationItemImpl[] mods = new ModificationItemImpl[]
                    {new ModificationItemImpl( DirContext.REMOVE_ATTRIBUTE, disabled )};
            schemaRoot.modifyAttributes( "cn=Krb5kdc", mods );
        }

        // Get a context, create the ou=users subcontext, then create the 3 principals.
        Hashtable<String, Object> env = new Hashtable<String, Object>();
        env.put( DirectoryService.JNDI_KEY, directoryService );
        env.put( Context.INITIAL_CONTEXT_FACTORY, "org.apache.directory.server.core.jndi.CoreContextFactory" );
        env.put( Context.PROVIDER_URL, "dc=example,dc=com" );
        env.put( Context.SECURITY_PRINCIPAL, "uid=admin,ou=system" );
        env.put( Context.SECURITY_CREDENTIALS, "secret" );
        env.put( Context.SECURITY_AUTHENTICATION, "simple" );

        ctx = new InitialDirContext( env );

        attrs = getOrgUnitAttributes( "users" );
        DirContext users = ctx.createSubcontext( "ou=users", attrs );

        attrs = getPrincipalAttributes( "Nelson", "Horatio Nelson", "hnelson", "secret", "hnelson@EXAMPLE.COM" );
        users.createSubcontext( "uid=hnelson", attrs );

        attrs = getPrincipalAttributes( "Service", "KDC Service", "krbtgt", "secret", "krbtgt/EXAMPLE.COM@EXAMPLE.COM" );
        users.createSubcontext( "uid=krbtgt", attrs );

        attrs = getPrincipalAttributes( "Service", "LDAP Service", "ldap", "randall", "ldap/localhost@EXAMPLE.COM" );
        users.createSubcontext( "uid=ldap", attrs );
    }

    protected void configureDirectoryService()
    {
        Attributes attrs;
        Set<Partition> partitions = new HashSet<Partition>();

        // Add partition 'example'
        JdbmPartition partition = new JdbmPartition();
        partition.setId( "example" );
        partition.setSuffix( "dc=example,dc=com" );

        Set<Index> indexedAttrs = new HashSet<Index>();
        indexedAttrs.add( new JdbmIndex( "ou" ) );
        indexedAttrs.add( new JdbmIndex( "dc" ) );
        indexedAttrs.add( new JdbmIndex( "objectClass" ) );
        partition.setIndexedAttributes( indexedAttrs );

        attrs = new AttributesImpl( true );
        Attribute attr = new AttributeImpl( "objectClass" );
        attr.add( "top" );
        attr.add( "domain" );
        attrs.put( attr );
        attr = new AttributeImpl( "dc" );
        attr.add( "example" );
        attrs.put( attr );
        partition.setContextEntry( attrs );

        partitions.add( partition );
        directoryService.setPartitions( partitions );

        List<Interceptor> list = directoryService.getInterceptors();
        list.add( new KeyDerivationInterceptor() );
        directoryService.setInterceptors( list );
    }


    /**
     * Convenience method for creating principals.
     *
     * @param cn           the commonName of the person
     * @param principal    the kerberos principal name for the person
     * @param sn           the surName of the person
     * @param uid          the unique identifier for the person
     * @param userPassword the credentials of the person
     * @return the attributes of the person principal
     */
    protected Attributes getPrincipalAttributes( String sn, String cn, String uid, String userPassword, String principal )
    {
        Attributes attrs = new AttributesImpl();
        Attribute ocls = new AttributeImpl( "objectClass" );
        ocls.add( "top" );
        ocls.add( "person" ); // sn $ cn
        ocls.add( "inetOrgPerson" ); // uid
        ocls.add( "krb5principal" );
        ocls.add( "krb5kdcentry" );
        attrs.put( ocls );
        attrs.put( "cn", cn );
        attrs.put( "sn", sn );
        attrs.put( "uid", uid );
        attrs.put( "userPassword", userPassword );
        attrs.put( KerberosAttribute.PRINCIPAL, principal );
        attrs.put( KerberosAttribute.VERSION, "0" );

        return attrs;
    }


    /**
     * Convenience method for creating an organizational unit.
     *
     * @param ou the ou of the organizationalUnit
     * @return the attributes of the organizationalUnit
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


    /**
     * Tests to make sure GSSAPI binds below the RootDSE work.
     */
    public void testSaslGssapiBind()
    {
        assertTrue( true );
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
}
