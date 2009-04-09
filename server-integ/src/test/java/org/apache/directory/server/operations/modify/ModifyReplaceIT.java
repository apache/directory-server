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


import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.naming.NamingEnumeration;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.BasicAttribute;
import javax.naming.directory.BasicAttributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.ModificationItem;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;

import org.apache.directory.server.core.DirectoryService;
import org.apache.directory.server.core.entry.ServerEntry;
import org.apache.directory.server.core.impl.DefaultDirectoryService;
import org.apache.directory.server.core.integ.IntegrationUtils;
import org.apache.directory.server.core.integ.Level;
import org.apache.directory.server.core.integ.annotations.ApplyLdifs;
import org.apache.directory.server.core.integ.annotations.CleanupLevel;
import org.apache.directory.server.core.integ.annotations.Factory;
import org.apache.directory.server.core.partition.jdbm.JdbmIndex;
import org.apache.directory.server.core.partition.jdbm.JdbmPartition;
import org.apache.directory.server.integ.LdapServerFactory;
import org.apache.directory.server.integ.SiRunner;
import static org.apache.directory.server.integ.ServerIntegrationUtils.getWiredContext;

import org.apache.directory.server.ldap.LdapService;
import org.apache.directory.server.ldap.handlers.bind.MechanismHandler;
import org.apache.directory.server.ldap.handlers.bind.SimpleMechanismHandler;
import org.apache.directory.server.ldap.handlers.bind.cramMD5.CramMd5MechanismHandler;
import org.apache.directory.server.ldap.handlers.bind.digestMD5.DigestMd5MechanismHandler;
import org.apache.directory.server.ldap.handlers.bind.gssapi.GssapiMechanismHandler;
import org.apache.directory.server.ldap.handlers.bind.ntlm.NtlmMechanismHandler;
import org.apache.directory.server.ldap.handlers.extended.StartTlsHandler;
import org.apache.directory.server.ldap.handlers.extended.StoredProcedureExtendedOperationHandler;
import org.apache.directory.server.protocol.shared.transport.TcpTransport;
import org.apache.directory.shared.ldap.constants.SchemaConstants;
import org.apache.directory.shared.ldap.constants.SupportedSaslMechanisms;
import org.apache.mina.util.AvailablePortFinder;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertEquals;


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
    // Entry # 1
    "dn: cn=Kate Bush,ou=system\n" +
    "objectClass: top\n" +
    "objectClass: person\n" +
    "sn: Bush\n" +
    "cn: Kate Bush\n\n" +

    // Entry # 2
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

    public static LdapService ldapService;
    
    
    public static class Factory implements LdapServerFactory
    {
        public LdapService newInstance() throws Exception
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
            system.setWorkingDirectory( new File( service.getWorkingDirectory(), "system" ) );

            // Add indexed attributes for system partition
            Set<JdbmIndex<?,ServerEntry>> indexedAttrs = new HashSet<JdbmIndex<?,ServerEntry>>();
            indexedAttrs.add( new JdbmIndex<String,ServerEntry>( SchemaConstants.OBJECT_CLASS_AT ) );
            indexedAttrs.add( new JdbmIndex<String,ServerEntry>( SchemaConstants.OU_AT ) );
            system.setIndexedAttributes( indexedAttrs );
            service.setSystemPartition( system );

            // change the working directory to something that is unique
            // on the system and somewhere either under target directory
            // or somewhere in a temp area of the machine.

            LdapService ldapService = new LdapService();
            ldapService.setDirectoryService( service );
            int port = AvailablePortFinder.getNextAvailable( 1024 );
            ldapService.setTcpTransport( new TcpTransport( port ) );
            ldapService.addExtendedOperationHandler( new StartTlsHandler() );
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
    
    /**
     * Create a person entry and try to remove a not present attribute
     */
    @Test
    public void testReplaceToRemoveNotPresentAttribute() throws Exception
    {
        DirContext sysRoot = ( DirContext ) getWiredContext( ldapService ).lookup( BASE );
        
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
            assertTrue( cn.contains( "Kate Bush") );
            Attribute desc = sr.getAttributes().get( "description" );
            assertNull( desc );
        }

        sysRoot.destroySubcontext( rdn );
    }
    
    
    /**
     * Create a person entry and try to add a not present attribute via a REPLACE
     */
    @Test
    public void testReplaceToAddNotPresentAttribute() throws Exception 
    {
        DirContext sysRoot = ( DirContext ) getWiredContext( ldapService ).lookup( BASE );
        
        String rdn = "cn=Kate Bush";

        Attribute attr = new BasicAttribute( "description", "added description" );
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
            assertTrue( cn.contains( "Kate Bush") );
            Attribute desc = sr.getAttributes().get( "description" );
            assertNotNull( desc );
            assertTrue( desc.contains( "added description") );
            assertEquals( 1, desc.size() );
        }

        sysRoot.destroySubcontext( rdn );
    }
    
    
    /**
     * Create a person entry and try to remove a non existing attribute
     */
    @Test
    public void testReplaceNonExistingAttribute() throws Exception 
    {
        DirContext sysRoot = ( DirContext ) getWiredContext( ldapService ).lookup( BASE );
        
        String rdn = "cn=Kate Bush";

        Attribute attr = new BasicAttribute( "numberOfOctaves" );
        ModificationItem item = new ModificationItem( DirContext.REPLACE_ATTRIBUTE, attr );

        sysRoot.modifyAttributes( rdn, new ModificationItem[] { item } );

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
        DirContext sysRoot = ( DirContext ) getWiredContext( ldapService ).lookup( BASE );
        
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
        DirContext sysRoot = ( DirContext ) getWiredContext( ldapService ).lookup( BASE );
        
        String rdn = "cn=Kim Wilde";
        //ldapService.getDirectoryService().getPartitions();

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
    
    
    /**
     * Create a person entry, replace telephoneNumber, verify the 
     * case of the attribute description attribute.
     */
    @Test
    public void testReplaceCaseOfAttributeDescription() throws Exception
    {
        DirContext ctx = ( DirContext ) getWiredContext( ldapService ).lookup( BASE );
        String rdn = "cn=Kate Bush";

        // Replace telephoneNumber
        String newValue = "2345678901";
        Attributes attrs = new BasicAttributes( "telephoneNumber", newValue, false );
        ctx.modifyAttributes( rdn, DirContext.REPLACE_ATTRIBUTE, attrs );

        // Verify, that
        // - case of attribute description is correct
        // - attribute value is added 
        attrs = ctx.getAttributes( rdn );
        Attribute attr = attrs.get( "telephoneNumber" );
        assertNotNull( attr );
        assertEquals( "telephoneNumber", attr.getID() );
        assertTrue( attr.contains( newValue ) );
        assertEquals( 1, attr.size() );
    }
}
