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
package org.apache.directory.server.operations.search;


import org.apache.directory.server.core.DefaultDirectoryService;
import org.apache.directory.server.core.DirectoryService;
import org.apache.directory.server.core.entry.ServerEntry;
import org.apache.directory.server.core.integ.IntegrationUtils;
import org.apache.directory.server.core.integ.Level;
import org.apache.directory.server.core.integ.annotations.ApplyLdifs;
import org.apache.directory.server.core.integ.annotations.CleanupLevel;
import org.apache.directory.server.core.integ.annotations.Factory;
import org.apache.directory.server.xdbm.Index;
import org.apache.directory.server.core.partition.impl.btree.jdbm.JdbmIndex;
import org.apache.directory.server.core.partition.impl.btree.jdbm.JdbmPartition;
import org.apache.directory.server.integ.LdapServerFactory;
import org.apache.directory.server.integ.SiRunner;
import static org.apache.directory.server.integ.ServerIntegrationUtils.getWiredContext;

import org.apache.directory.server.ldap.LdapServer;
import org.apache.directory.server.ldap.handlers.bind.MechanismHandler;
import org.apache.directory.server.ldap.handlers.bind.SimpleMechanismHandler;
import org.apache.directory.server.ldap.handlers.bind.cramMD5.CramMd5MechanismHandler;
import org.apache.directory.server.ldap.handlers.bind.digestMD5.DigestMd5MechanismHandler;
import org.apache.directory.server.ldap.handlers.bind.gssapi.GssapiMechanismHandler;
import org.apache.directory.server.ldap.handlers.bind.ntlm.NtlmMechanismHandler;
import org.apache.directory.server.ldap.handlers.extended.StartTlsHandler;
import org.apache.directory.server.ldap.handlers.extended.StoredProcedureExtendedOperationHandler;
import org.apache.directory.server.protocol.shared.SocketAcceptor;
import org.apache.directory.shared.ldap.constants.SchemaConstants;
import org.apache.directory.shared.ldap.constants.SupportedSaslMechanisms;
import org.apache.mina.util.AvailablePortFinder;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.naming.NamingEnumeration;
import javax.naming.directory.DirContext;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;


/**
 * A set of tests to make sure the negation operator is working 
 * properly when included in search filters on indexed attributes.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
@RunWith ( SiRunner.class ) 
@CleanupLevel ( Level.CLASS )
@Factory ( IndexedNegationSearchIT.Factory.class )
@ApplyLdifs( {
    "dn: ou=test,ou=system\n" +
    "objectClass: top\n" +
    "objectClass: organizationalUnit\n" +
    "ou: test\n\n" +

    "dn: uid=test1,ou=test,ou=system\n" +
    "objectClass: top\n" +
    "objectClass: account\n" +
    "uid: test1\n" +
    "ou: test1\n\n" +

    "dn: uid=test2,ou=test,ou=system\n" +
    "objectClass: top\n" +
    "objectClass: account\n" +
    "uid: test2\n" +
    "ou: test2\n\n" +

    "dn: uid=testNoOU,ou=test,ou=system\n" +
    "objectClass: top\n" +
    "objectClass: account\n" +
    "uid: testNoOU\n\n" +
    
    "dn: ou=actors,ou=system\n" +
    "objectClass: top\n" +
    "objectClass: organizationalUnit\n" +
    "ou: actors\n\n" +

    "dn: uid=jblack,ou=actors,ou=system\n" +
    "objectClass: top\n" +
    "objectClass: person\n" +
    "objectClass: organizationalPerson\n" +
    "objectClass: uidObject\n" +
    "uid: jblack\n" +
    "ou: comedy\n" +
    "ou: adventure\n" +
    "cn: Jack Black\n" +
    "sn: Black\n\n" +

    "dn: uid=bpitt,ou=actors,ou=system\n" +
    "objectClass: top\n" +
    "objectClass: person\n" +
    "objectClass: organizationalPerson\n" +
    "objectClass: uidObject\n" +
    "uid: bpitt\n" +
    "ou: drama\n" +
    "ou: adventure\n" +
    "cn: Brad Pitt\n" +
    "sn: Pitt\n\n" +

    "dn: uid=gcloony,ou=actors,ou=system\n" +
    "objectClass: top\n" +
    "objectClass: person\n" +
    "objectClass: organizationalPerson\n" +
    "objectClass: uidObject\n" +
    "uid: gcloony\n" +
    "ou: drama\n" +
    "cn: Goerge Cloony\n" +
    "sn: Cloony\n\n" +

    "dn: uid=jnewbie,ou=actors,ou=system\n" +
    "objectClass: top\n" +
    "objectClass: person\n" +
    "objectClass: organizationalPerson\n" +
    "objectClass: uidObject\n" +
    "uid: jnewbie\n" +
    "cn: Joe Newbie\n" +
    "sn: Newbie\n\n" 

    }
)
public class IndexedNegationSearchIT 
{
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
     * Tests to make sure a negated search for OU of "test1" returns
     * those entries that do not have the OU attribute or do not have
     * a "test1" value for OU if the attribute exists.
     */
    @Test
    public void testSearchNotOUIndexed() throws Exception
    {
        Set<SearchResult> results = getResults( "(!(ou=test1))" );
        assertFalse( contains( "uid=test1,ou=test,ou=system", results ) );
        assertTrue( contains( "uid=test2,ou=test,ou=system", results ) );
        assertTrue( contains( "uid=testNoOU,ou=test,ou=system", results ) );
    }

    
    /**
     * Tests to make sure a negated search for actors without ou
     * with value 'drama' returns those that do not have the attribute
     * and do not have a 'drama' value for ou if the attribute still
     * exists.  This test DOES build an index on ou for the system
     * partition and should have failed if the bug in DIRSERVER-951
     * was present and reproducable.
     */
    @Test
    public void testSearchNotDramaIndexed() throws Exception
    {
        // jack black has ou but not drama, and joe newbie has no ou what so ever
        Set<SearchResult> results = getActorResults( "(!(ou=drama))" );
        assertTrue( contains( "uid=jblack,ou=actors,ou=system", results ) );
        assertTrue( contains( "uid=jnewbie,ou=actors,ou=system", results ) );
        assertEquals( 2, results.size() );
    }

    
    boolean contains( String dn, Set<SearchResult> results )
    {
        for ( SearchResult result : results )
        {
            if ( result.getNameInNamespace().equals( dn ) )
            {
                return true;
            }
        }
        
        return false;
    }
    
    
    /**
     * Tests to make sure a negated search for actors without ou
     * with value 'drama' returns those that do not have the attribute
     * and do not have a 'drama' value for ou if the attribute still
     * exists.  This test DOES build an index on ou for the system
     * partition and should have failed if the bug in DIRSERVER-951
     * was present and reproducable.
     */
    @Test
    public void testSearchNotDramaNotNewbieIndexed() throws Exception
    {
        // jack black has ou but not drama, and joe newbie has no ou what so ever
        Set<SearchResult> results = getActorResults( "(& (!(uid=jnewbie)) (!(ou=drama)) )" );
        assertTrue( contains( "uid=jblack,ou=actors,ou=system", results ) );
        assertFalse( contains( "uid=jnewbie,ou=actors,ou=system", results ) );
        assertEquals( 1, results.size() );
    }

    
    Set<SearchResult> getActorResults( String filter ) throws Exception
    {
        DirContext ctx = getWiredContext( ldapServer );
        Set<SearchResult> results = new HashSet<SearchResult>();
        SearchControls controls = new SearchControls();
        controls.setSearchScope( SearchControls.ONELEVEL_SCOPE );
        NamingEnumeration<SearchResult> namingEnumeration = ctx.search( "ou=actors,ou=system", filter, controls );
        while( namingEnumeration.hasMore() )
        {
            results.add( namingEnumeration.next() );
        }
        
        return results;
    }

    
    Set<SearchResult> getResults( String filter ) throws Exception
    {
        DirContext ctx = getWiredContext( ldapServer );
        Set<SearchResult> results = new HashSet<SearchResult>();
        SearchControls controls = new SearchControls();
        controls.setSearchScope( SearchControls.SUBTREE_SCOPE );
        NamingEnumeration<SearchResult> namingEnumeration = ctx.search( "ou=system", filter, controls );
        while( namingEnumeration.hasMore() )
        {
            results.add( namingEnumeration.next() );
        }
        
        return results;
    }
}
