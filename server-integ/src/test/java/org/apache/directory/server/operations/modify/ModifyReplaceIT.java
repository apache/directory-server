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
package org.apache.directory.server.operations.modify;


import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.naming.NamingEnumeration;
import javax.naming.directory.Attribute;
import javax.naming.directory.BasicAttribute;
import javax.naming.directory.DirContext;
import javax.naming.directory.ModificationItem;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;

import org.apache.directory.server.core.DefaultDirectoryService;
import org.apache.directory.server.core.DirectoryService;
import org.apache.directory.server.core.entry.DefaultServerEntry;
import org.apache.directory.server.core.entry.ServerEntry;
import org.apache.directory.server.core.integ.IntegrationUtils;
import org.apache.directory.server.core.integ.Level;
import org.apache.directory.server.core.integ.annotations.ApplyLdifs;
import org.apache.directory.server.core.integ.annotations.CleanupLevel;
import org.apache.directory.server.core.integ.annotations.Factory;
import org.apache.directory.server.core.partition.impl.btree.jdbm.JdbmIndex;
import org.apache.directory.server.core.partition.impl.btree.jdbm.JdbmPartition;
import org.apache.directory.server.integ.LdapServerFactory;
import org.apache.directory.server.integ.SiRunner;
import static org.apache.directory.server.integ.ServerIntegrationUtils.getWiredContext;

import org.apache.directory.server.newldap.LdapServer;
import org.apache.directory.server.newldap.handlers.bind.MechanismHandler;
import org.apache.directory.server.newldap.handlers.bind.SimpleMechanismHandler;
import org.apache.directory.server.newldap.handlers.bind.cramMD5.CramMd5MechanismHandler;
import org.apache.directory.server.newldap.handlers.bind.digestMD5.DigestMd5MechanismHandler;
import org.apache.directory.server.newldap.handlers.bind.gssapi.GssapiMechanismHandler;
import org.apache.directory.server.newldap.handlers.bind.ntlm.NtlmMechanismHandler;
import org.apache.directory.server.newldap.handlers.extended.StartTlsHandler;
import org.apache.directory.server.newldap.handlers.extended.StoredProcedureExtendedOperationHandler;
import org.apache.directory.server.protocol.shared.SocketAcceptor;
import org.apache.directory.server.xdbm.Index;
import org.apache.directory.shared.ldap.constants.SchemaConstants;
import org.apache.directory.shared.ldap.constants.SupportedSaslMechanisms;
import org.apache.directory.shared.ldap.name.LdapDN;
import org.apache.directory.shared.ldap.util.DateUtils;
import org.apache.mina.util.AvailablePortFinder;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertNotNull;


/**
 * Test case for all modify replace operations.
 * 
 * Demonstrates DIRSERVER-646 ("Replacing an unknown attribute with
 * no values (deletion) causes an error").
 */
@RunWith ( SiRunner.class ) 
@CleanupLevel ( Level.SUITE )
@Factory ( ModifyReplaceIT.Factory.class )
@ApplyLdifs( {
    // Entry # 0
    "dn: cn=Kate Bush,ou=system\n" +
    "objectClass: top\n" +
    "objectClass: person\n" +
    "sn: Bush\n" +
    "cn: Kate Bush\n\n" +
    // Entry # 1
    "dn: cn=Kim Wilde,ou=system\n" +
    "objectClass: top\n" +
    "objectClass: person\n" +
    "objectClass: organizationalPerson \n" +
    "objectClass: inetOrgPerson \n" +
    "sn: Wilde\n" +
    "cn: Kim Wilde\n\n" 
    }
)
public class ModifyReplaceIT 
{
    private static final String BASE = "ou=system";

    public static LdapServer ldapServer;
    
    
    public static class Factory implements LdapServerFactory
    {
        public LdapServer newInstance() throws Exception
        {
            DirectoryService service = new DefaultDirectoryService();
            IntegrationUtils.doDelete( service.getWorkingDirectory() );
            service.getChangeLog().setEnabled( true );
            service.setShutdownHookEnabled( false );

            JdbmPartition system = new JdbmPartition();
            system.setId( "system" );

            // @TODO need to make this configurable for the system partition
            system.setCacheSize( 500 );

            system.setSuffix( "ou=system" );

            // Add indexed attributes for system partition
            Set<Index<?,ServerEntry>> indexedAttrs = new HashSet<Index<?,ServerEntry>>();
            indexedAttrs.add( new JdbmIndex<String,ServerEntry>( SchemaConstants.OBJECT_CLASS_AT ) );
            indexedAttrs.add( new JdbmIndex<String,ServerEntry>( SchemaConstants.OU_AT ) );
            system.setIndexedAttributes( indexedAttrs );

            // Add context entry for system partition
            LdapDN systemDn = new LdapDN( "ou=system" );
            ServerEntry systemEntry = new DefaultServerEntry( service.getRegistries(), systemDn );
            systemEntry.put( "objectClass", "top", "organizationalUnit", "extensibleObject", "account" ); 
            systemEntry.put( SchemaConstants.CREATORS_NAME_AT, "uid=admin, ou=system" );
            systemEntry.put( SchemaConstants.CREATE_TIMESTAMP_AT, DateUtils.getGeneralizedTime() );
            systemEntry.put( "ou", "system" );
            systemEntry.put( "uid", "testUid" );
            system.setContextEntry( systemEntry );
            service.setSystemPartition( system );

            // change the working directory to something that is unique
            // on the system and somewhere either under target directory
            // or somewhere in a temp area of the machine.

            LdapServer ldapServer = new LdapServer();
            ldapServer.setDirectoryService( service );
            ldapServer.setSocketAcceptor( new SocketAcceptor( null ) );
            ldapServer.setIpPort( AvailablePortFinder.getNextAvailable( 1024 ) );
            ldapServer.addExtendedOperationHandler( new StartTlsHandler() );
            ldapServer.addExtendedOperationHandler( new StoredProcedureExtendedOperationHandler() );

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

            ldapServer.setSaslMechanismHandlers( mechanismHandlerMap );

            return ldapServer;
        }
    }
    
    /**
     * Create a person entry and try to remove a not present attribute
     */
    @Test
    public void testReplaceNotPresentAttribute() throws Exception 
    {
        DirContext sysRoot = ( DirContext ) getWiredContext( ldapServer ).lookup( BASE );
        
        String rdn = "cn=Kate Bush";

        Attribute attr = new BasicAttribute( "description" );
        ModificationItem item = new ModificationItem( DirContext.REPLACE_ATTRIBUTE, attr );

        sysRoot.modifyAttributes( rdn, new ModificationItem[] { item } );

        SearchControls sctls = new SearchControls();
        sctls.setSearchScope( SearchControls.SUBTREE_SCOPE );
        String filter = "(sn=Bush)";
        String base = "";

        NamingEnumeration<SearchResult> enm = sysRoot.search( base, filter, sctls );
        while ( enm.hasMore() ) 
        {
            SearchResult sr = ( SearchResult ) enm.next();
            Attribute cn = sr.getAttributes().get( "cn" );
            assertNotNull( cn );
            assertTrue( cn.contains("Kate Bush") );
        }

        sysRoot.destroySubcontext( rdn );
    }

    
    /**
     * Create a person entry and try to remove a non existing attribute
     */
    @Test
    public void testReplaceNonExistingAttribute() throws Exception 
    {
        DirContext sysRoot = ( DirContext ) getWiredContext( ldapServer ).lookup( BASE );
        
        String rdn = "cn=Kate Bush";

        Attribute attr = new BasicAttribute( "numberOfOctaves" );
        ModificationItem item = new ModificationItem( DirContext.REPLACE_ATTRIBUTE, attr );

        sysRoot.modifyAttributes(rdn, new ModificationItem[] { item });

        SearchControls sctls = new SearchControls();
        sctls.setSearchScope( SearchControls.SUBTREE_SCOPE );
        String filter = "(sn=Bush)";
        String base = "";

        NamingEnumeration<SearchResult> enm = sysRoot.search( base, filter, sctls );
        while ( enm.hasMore() ) 
        {
            SearchResult sr = enm.next();
            Attribute cn = sr.getAttributes().get( "cn" );
            assertNotNull( cn );
            assertTrue( cn.contains( "Kate Bush" ) );
        }

        sysRoot.destroySubcontext( rdn );
    }


    /**
     * Create a person entry and try to remove a non existing attribute
     */
    @Test
    public void testReplaceNonExistingAttributeManyMods() throws Exception 
    {
        DirContext sysRoot = ( DirContext ) getWiredContext( ldapServer ).lookup( BASE );
        
        String rdn = "cn=Kate Bush";

        Attribute attr = new BasicAttribute( "numberOfOctaves" );
        ModificationItem item = new ModificationItem( DirContext.REPLACE_ATTRIBUTE, attr );
        Attribute attr2 = new BasicAttribute( "description", "blah blah blah" );
        ModificationItem item2 = new ModificationItem( DirContext.ADD_ATTRIBUTE, attr2 );

        sysRoot.modifyAttributes(rdn, new ModificationItem[] { item, item2 });

        SearchControls sctls = new SearchControls();
        sctls.setSearchScope( SearchControls.SUBTREE_SCOPE );
        String filter = "(sn=Bush)";
        String base = "";

        NamingEnumeration<SearchResult> enm = sysRoot.search( base, filter, sctls );
        while ( enm.hasMore() ) 
        {
            SearchResult sr = enm.next();
            Attribute cn = sr.getAttributes().get( "cn" );
            assertNotNull( cn );
            assertTrue( cn.contains( "Kate Bush" ) );
        }

        sysRoot.destroySubcontext( rdn );
    }


    /**
     * Create a person entry and try to replace a non existing indexed attribute
     */
    @Test
    public void testReplaceNonExistingIndexedAttribute() throws Exception 
    {
        DirContext sysRoot = ( DirContext ) getWiredContext( ldapServer ).lookup( BASE );
        
        String rdn = "cn=Kim Wilde";
        //ldapServer.getDirectoryService().getPartitions();

        Attribute attr = new BasicAttribute( "ou", "test" );
        ModificationItem item = new ModificationItem( DirContext.REPLACE_ATTRIBUTE, attr );

        sysRoot.modifyAttributes(rdn, new ModificationItem[] { item });

        SearchControls sctls = new SearchControls();
        sctls.setSearchScope( SearchControls.SUBTREE_SCOPE );
        String filter = "(sn=Wilde)";
        String base = "";

        NamingEnumeration<SearchResult> enm = sysRoot.search( base, filter, sctls );
        
        while ( enm.hasMore() ) 
        {
            SearchResult sr = enm.next();
            Attribute ou = sr.getAttributes().get( "ou" );
            assertNotNull( ou );
            assertTrue( ou.contains( "test" ) );
        }

        sysRoot.destroySubcontext( rdn );
    }
}
