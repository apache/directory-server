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
package org.apache.directory.server.core.partition;


import java.util.HashMap;

import org.apache.directory.server.core.DefaultDirectoryService;
import org.apache.directory.server.core.DirectoryService;
import org.apache.directory.server.core.entry.DefaultServerEntry;
import org.apache.directory.server.core.entry.ServerEntry;
import org.apache.directory.server.core.integ.CiRunner;
import org.apache.directory.server.core.integ.DirectoryServiceFactory;
import org.apache.directory.server.core.integ.annotations.Factory;
import org.apache.directory.server.core.partition.impl.btree.jdbm.JdbmPartition;

import static org.apache.directory.server.core.integ.IntegrationUtils.getRootContext;
import org.apache.directory.shared.ldap.message.AttributesImpl;
import org.apache.directory.shared.ldap.name.LdapDN;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.ldap.LdapContext;



/**
 * Test cases for partition handling.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
@RunWith ( CiRunner.class )
@Factory ( PartitionIT.Factory.class )
public final class PartitionIT
{
    private static final Logger LOG = LoggerFactory.getLogger( PartitionIT.class );
    public static DirectoryService service;

    
    /**
     * Creates a DirectoryService configured with two separate dc=com based 
     * domains to test multiple partitions.
     */
    public static class Factory implements DirectoryServiceFactory
    {
		public DirectoryService newInstance() throws Exception 
		{
            DirectoryService service = new DefaultDirectoryService();
            service.getChangeLog().setEnabled( true );
            
            Partition foo = new JdbmPartition();
            foo.setId( "foo" );
            foo.setSuffix( "dc=foo,dc=com" );
            LdapDN contextDn = new LdapDN( "dc=foo,dc=com" );
            contextDn.normalize( service.getRegistries().getAttributeTypeRegistry().getNormalizerMapping() );
            ServerEntry contextEntry = new DefaultServerEntry( service.getRegistries(), contextDn );
            contextEntry.add( "objectClass", "top", "domain" );
            contextEntry.add( "dc", "foo" );
            foo.setContextEntry( contextEntry );
            service.addPartition( foo );
            
            Partition bar = new JdbmPartition();
            bar.setId( "bar" );
            bar.setSuffix( "dc=bar,dc=com" );
            contextDn = new LdapDN( "dc=bar,dc=com" );
            contextDn.normalize( service.getRegistries().getAttributeTypeRegistry().getNormalizerMapping() );
            contextEntry = new DefaultServerEntry( service.getRegistries(), contextDn );
            contextEntry.add( "objectClass", "top", "domain" );
            contextEntry.add( "dc", "bar" );
            bar.setContextEntry( contextEntry );
            service.addPartition( bar );
            
            return service;
		}
    }
    

    /**
     * Test case to weed out issue in DIRSERVER-1118.
     *
     * @see https://issues.apache.org/jira/browse/DIRSERVER-1118
     */
    @Test
    public void testDIRSERVER_1118() throws Exception
    {
        /*
         * Confirm the presence of the partitions foo and bar through DS API
         */
        HashMap<String, Partition> partitionMap = new HashMap<String, Partition>();
        
        for ( Partition partition : service.getPartitions() )
        {
            LOG.debug( "partition id = {}", partition.getId() );
            partitionMap.put( partition.getId(), partition );
        }
        
        assertNotNull( partitionMap.containsKey( "foo" ) );
        assertNotNull( partitionMap.containsKey( "bar" ) );

        /*
         * Confirm presence and publishing of foo and bar partitions as 
         * namingContexts as values innamingContexts attribute of the rootDSE
         */
        LdapContext rootDSE = getRootContext( service );
        Attribute namingContexts = rootDSE.getAttributes( "", 
            new String[] { "namingContexts" } ).get( "namingContexts" );
        assertTrue( namingContexts.contains( "dc=foo,dc=com" ) );
        assertTrue( namingContexts.contains( "dc=bar,dc=com" ) );
        LOG.debug( "Found both dc=foo,dc=com and dc=bar,dc=com in namingContexts" );
        
        /*
         * Add, lookup, then delete entry in both foo and bar partitions
         */
        addLookupDelete( "dc=foo,dc=com" );
        addLookupDelete( "dc=bar,dc=com" );
    }
    

    /**
     * Given the suffix DN of a partition this method will add an entry, look 
     * it up, then delete it making sure all checks out.
     *
     * @param partitionSuffix the DN of the partition suffix
     */
    public void addLookupDelete( String partitionSuffix ) throws Exception
    {
        LdapContext rootDSE = getRootContext( service );
        Attributes attrs = new AttributesImpl( "objectClass", "organizationalUnit", true );
        attrs.put( "ou", "people" );
        String entryDn = "ou=people," + partitionSuffix;
        rootDSE.createSubcontext( entryDn, attrs );
        LOG.debug( "added entry {} to partition {}", entryDn, partitionSuffix );
        
        Attributes reloaded = rootDSE.getAttributes( entryDn );
        assertNotNull( reloaded );
        assertTrue( reloaded.get( "ou" ).contains( "people" ) );
        LOG.debug( "looked up entry {} from partition {}", entryDn, partitionSuffix );
        
        rootDSE.destroySubcontext( entryDn );
        try
        {
            rootDSE.getAttributes( entryDn );
            fail( "should never get here" );
        }
        catch ( Exception e )
        {
            LOG.debug( "Successfully deleted entry {} from partition {}", entryDn, partitionSuffix );
        }
    }
}
