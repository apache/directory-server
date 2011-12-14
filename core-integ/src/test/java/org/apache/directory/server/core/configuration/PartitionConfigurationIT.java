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
package org.apache.directory.server.core.configuration;


import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertNull;

import java.util.UUID;

import org.apache.directory.ldap.client.api.LdapConnection;
import org.apache.directory.server.core.DefaultDirectoryService;
import org.apache.directory.server.core.annotations.CreateDS;
import org.apache.directory.server.core.api.DirectoryService;
import org.apache.directory.server.core.api.interceptor.context.AddOperationContext;
import org.apache.directory.server.core.api.partition.Partition;
import org.apache.directory.server.core.factory.DefaultDirectoryServiceFactory;
import org.apache.directory.server.core.factory.DirectoryServiceFactory;
import org.apache.directory.server.core.factory.PartitionFactory;
import org.apache.directory.server.core.integ.AbstractLdapTestUnit;
import org.apache.directory.server.core.integ.FrameworkRunner;
import org.apache.directory.server.core.integ.IntegrationUtils;
import org.apache.directory.server.core.shared.partition.OperationExecutionManagerFactory;
import org.apache.directory.server.core.shared.txn.TxnManagerFactory;
import org.apache.directory.shared.ldap.model.csn.CsnFactory;
import org.apache.directory.shared.ldap.model.entry.DefaultEntry;
import org.apache.directory.shared.ldap.model.entry.Entry;
import org.apache.directory.shared.ldap.model.name.Dn;
import org.junit.Test;
import org.junit.runner.RunWith;


/**
 * Tests dynamic partition addition and removal.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
@RunWith(FrameworkRunner.class)
@CreateDS(name = "PartitionConfigurationIT")
public class PartitionConfigurationIT extends AbstractLdapTestUnit
{
    public TxnManagerFactory getTxnManagerFactory()
    {
        return ( ( DefaultDirectoryService )getService() ).getTxnManagerFactory();
    }
    
    
    public OperationExecutionManagerFactory getOperationExecutionManagerFactory()
    {
        return ( ( DefaultDirectoryService )getService() ).getOperationExecutionManagerFactory();
    }

    @Test
    public void testAddAndRemove() throws Exception
    {
        DirectoryService service = getService();
        DirectoryServiceFactory dsFactory = DefaultDirectoryServiceFactory.class.newInstance();
        PartitionFactory partitionFactory = dsFactory.getPartitionFactory();
        Partition partition = partitionFactory.createPartition( 
            service.getSchemaManager(), 
            "removable", 
            "ou=removable", 
            100, 
            service.getInstanceLayout().getPartitionsDirectory(),
            getTxnManagerFactory(), 
            getOperationExecutionManagerFactory());

        // Test AddContextPartition
        service.addPartition( partition );

        Dn suffixDn = new Dn( service.getSchemaManager(), "ou=removable" );

        Entry ctxEntry = new DefaultEntry( 
            service.getSchemaManager(), 
            suffixDn.toString(),
            "objectClass: top",
            "objectClass: organizationalUnit",
            "ou: removable",
            "entryCSN", new CsnFactory( 1 ).newInstance().toString(),
            "entryUUID", UUID.randomUUID().toString() );

        service.getPartitionNexus().add( new AddOperationContext( service.getAdminSession(), ctxEntry ) );

        LdapConnection connection = IntegrationUtils.getAdminConnection( service );

        assertNotNull( connection.lookup( "ou=removable" ) );

        // Test removeContextPartition
        service.removePartition( partition );

        assertNull( connection.lookup( "ou=removable" ) );
    }
}
