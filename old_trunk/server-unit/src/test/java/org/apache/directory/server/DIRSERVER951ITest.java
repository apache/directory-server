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
import java.util.Set;

import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;
import javax.naming.ldap.LdapContext;

import org.apache.directory.server.core.partition.Oid;
import org.apache.directory.server.core.partition.PartitionNexus;
import org.apache.directory.server.core.partition.impl.btree.MutableBTreePartitionConfiguration;
import org.apache.directory.server.unit.AbstractServerTest;
import org.apache.directory.shared.ldap.message.AttributeImpl;
import org.apache.directory.shared.ldap.message.AttributesImpl;
import org.apache.directory.shared.ldap.util.DateUtils;
import org.apache.directory.shared.ldap.util.NamespaceTools;


/**
 * A set of tests to make sure the negation operator is working 
 * properly when included in search filters. Created in response
 * to JIRA issue 
 * <a href="https://issues.apache.org/jira/browse/DIRSERVER-951">DIRSERVER-951</a>.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class DIRSERVER951ITest extends AbstractServerTest
{
    private LdapContext ctx = null;


    /**
     * Create context and entries for tests.
     */
    public void setUp() throws Exception
    {
        MutableBTreePartitionConfiguration systemCfg = new MutableBTreePartitionConfiguration();
        systemCfg.setId( "system" );
        
        // @TODO need to make this configurable for the system partition
        systemCfg.setCacheSize( 500 );
        
        systemCfg.setSuffix( PartitionNexus.SYSTEM_PARTITION_SUFFIX );

        // Add indexed attributes for system partition
        Set<Object> indexedAttrs = new HashSet<Object>();
        indexedAttrs.add( Oid.ALIAS );
        indexedAttrs.add( Oid.EXISTANCE );
        indexedAttrs.add( Oid.HIERARCHY );
        indexedAttrs.add( Oid.NDN );
        indexedAttrs.add( Oid.ONEALIAS );
        indexedAttrs.add( Oid.SUBALIAS );
        indexedAttrs.add( Oid.UPDN );
        indexedAttrs.add( "objectClass" );
        indexedAttrs.add( "ou" );
        systemCfg.setIndexedAttributes( indexedAttrs );

        // Add context entry for system partition
        Attributes systemEntry = new AttributesImpl();
        Attribute objectClassAttr = new AttributeImpl( "objectClass" );
        objectClassAttr.add( "top" );
        objectClassAttr.add( "account" );
        //objectClassAttr.add( "extensibleObject" );
        systemEntry.put( objectClassAttr );
        systemEntry.put( "creatorsName", "uid=admin,ou=system" );
        systemEntry.put( "createTimestamp", DateUtils.getGeneralizedTime() );
        systemEntry.put( NamespaceTools.getRdnAttribute( "ou=system" ),
            NamespaceTools.getRdnValue( "ou=system" ) );
        systemEntry.put( "uid", "testUid" );
        systemCfg.setContextEntry( systemEntry );
        
        configuration.setSystemPartitionConfiguration( systemCfg );
        
        super.setUp();
        super.loadTestLdif( true );
        ctx = getWiredContext();
        assertNotNull( ctx );
    }


    /**
     * Closes context and destroys server.
     */
    public void tearDown() throws Exception
    {
        ctx.close();
        ctx = null;
        super.tearDown();
    }
    

    /**
     * Tests to make sure a negated search for OU of "test1" returns
     * those entries that do not have the OU attribute or do not have
     * a "test1" value for OU if the attribute exists.
     */
    public void testSearchNotOU() throws Exception
    {
        Set<SearchResult> results = getResults( "(!(ou=test1))" );
        assertFalse( contains( "uid=test1,ou=test,ou=system", results ) );
        assertTrue( contains( "uid=test2,ou=test,ou=system", results ) );
        assertTrue( contains( "uid=testNoOU,ou=test,ou=system", results ) );
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
    
    
    Set<SearchResult> getResults( String filter ) throws NamingException
    {
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
