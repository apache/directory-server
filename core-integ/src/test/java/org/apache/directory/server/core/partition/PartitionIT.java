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


import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;

import org.apache.directory.api.ldap.model.entry.DefaultEntry;
import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.ldap.client.api.LdapConnection;
import org.apache.directory.server.core.annotations.ContextEntry;
import org.apache.directory.server.core.annotations.CreateDS;
import org.apache.directory.server.core.annotations.CreateIndex;
import org.apache.directory.server.core.annotations.CreatePartition;
import org.apache.directory.server.core.api.partition.Partition;
import org.apache.directory.server.core.integ.AbstractLdapTestUnit;
import org.apache.directory.server.core.integ.ApacheDSTestExtension;
import org.apache.directory.server.core.integ.IntegrationUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Test cases for partition handling.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
@ExtendWith( ApacheDSTestExtension.class )
/*
 * Creates a DirectoryService configured with two separate dc=com based 
 * domains to test multiple partitions.
 */
@CreateDS(name = "PartitionIT-class",
    partitions =
        {
            @CreatePartition(
                name = "foo",
                suffix = "dc=foo,dc=com",
                contextEntry = @ContextEntry(
                    entryLdif =
                    "dn: dc=foo,dc=com\n" +
                        "dc: foo\n" +
                        "objectClass: top\n" +
                        "objectClass: domain\n\n"),
                indexes =
                    {
                        @CreateIndex(attribute = "objectClass"),
                        @CreateIndex(attribute = "dc"),
                        @CreateIndex(attribute = "ou")
                }),
            @CreatePartition(
                name = "bar",
                suffix = "dc=bar,dc=com",
                contextEntry = @ContextEntry(
                    entryLdif =
                    "dn: dc=bar,dc=com\n" +
                        "dc: bar\n" +
                        "objectClass: top\n" +
                        "objectClass: domain\n\n"),
                indexes =
                    {
                        @CreateIndex(attribute = "objectClass"),
                        @CreateIndex(attribute = "dc"),
                        @CreateIndex(attribute = "ou")
                })
    })
public final class PartitionIT extends AbstractLdapTestUnit
{
    private static final Logger LOG = LoggerFactory.getLogger( PartitionIT.class );


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

        for ( Partition partition : getService().getPartitions() )
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
        try ( LdapConnection conn = IntegrationUtils.getAdminConnection( getService() ) )
        {
            assertTrue( conn.getRootDse( "*", "+" ).contains( "namingContexts", "dc=foo,dc=com", "dc=bar,dc=com" ) );
            
            LOG.debug( "Found both dc=foo,dc=com and dc=bar,dc=com in namingContexts" );

            /*
             * Add, lookup, then delete entry in both foo and bar partitions
             */
            addLookupDelete( "dc=foo,dc=com" );
            addLookupDelete( "dc=bar,dc=com" );
        }
    }


    /**
     * Given the suffix Dn of a partition this method will add an entry, look
     * it up, then delete it making sure all checks out.
     *
     * @param partitionSuffix the Dn of the partition suffix
     */
    public void addLookupDelete( String partitionSuffix ) throws Exception
    {
        try ( LdapConnection conn = IntegrationUtils.getAdminConnection( getService() ) )
        {
            String entryDn = "ou=people," + partitionSuffix;

            conn.add( new DefaultEntry( 
                entryDn,
                "objectClass", "organizationalUnit",
                "ou", "people"
                ) );
            
            LOG.debug( "added entry {} to partition {}", entryDn, partitionSuffix );
            
            Entry reloaded = conn.lookup( entryDn );
            
            assertNotNull( reloaded );
            assertTrue( reloaded.contains( "ou", "people" ) );
            LOG.debug( "looked up entry {} from partition {}", entryDn, partitionSuffix );
            
            conn.delete( entryDn );

            // The entry should not be founc
            assertNull( conn.lookup( entryDn ) );
        }
    }
}
