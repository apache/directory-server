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


import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.io.IOException;
import java.util.UUID;

import org.apache.directory.api.ldap.model.csn.CsnFactory;
import org.apache.directory.api.ldap.model.entry.DefaultEntry;
import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.api.ldap.model.name.Dn;
import org.apache.directory.ldap.client.api.LdapConnection;
import org.apache.directory.server.core.annotations.CreateDS;
import org.apache.directory.server.core.api.interceptor.context.AddOperationContext;
import org.apache.directory.server.core.api.partition.Partition;
import org.apache.directory.server.core.api.partition.PartitionTxn;
import org.apache.directory.server.core.factory.DefaultDirectoryServiceFactory;
import org.apache.directory.server.core.factory.DirectoryServiceFactory;
import org.apache.directory.server.core.factory.PartitionFactory;
import org.apache.directory.server.core.integ.AbstractLdapTestUnit;
import org.apache.directory.server.core.integ.ApacheDSTestExtension;
import org.apache.directory.server.core.integ.IntegrationUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;


/**
 * Tests dynamic partition addition and removal.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
@ExtendWith( ApacheDSTestExtension.class )
@CreateDS(name = "PartitionConfigurationIT")
public class PartitionConfigurationIT extends AbstractLdapTestUnit
{

    @Test
    public void testAddAndRemove() throws Exception
    {
        DirectoryServiceFactory dsFactory = DefaultDirectoryServiceFactory.class.newInstance();
        PartitionFactory partitionFactory = dsFactory.getPartitionFactory();
        Partition partition = partitionFactory.createPartition( getService().getSchemaManager(),
            getService().getDnFactory(), "removable",
            "ou=removable", 100, getService()
                .getInstanceLayout().getPartitionsDirectory() );

        // Test AddContextPartition
        getService().addPartition( partition );

        Dn suffixDn = new Dn( getService().getSchemaManager(), "ou=removable" );

        Entry ctxEntry = new DefaultEntry(
            getService().getSchemaManager(),
            suffixDn.toString(),
            "objectClass: top",
            "objectClass: organizationalUnit",
            "ou: removable",
            "entryCSN", new CsnFactory( 1 ).newInstance().toString(),
            "entryUUID", UUID.randomUUID().toString() );

        AddOperationContext addContext = new AddOperationContext( getService().getAdminSession(), ctxEntry );
        addContext.setPartition( partition );
        PartitionTxn partitionTxn = null;
        
        try
        {
            partitionTxn = partition.beginWriteTransaction();
            addContext.setTransaction( partitionTxn );
            partition.add( addContext );
            partitionTxn.commit();
        }
        catch ( IOException ioe )
        {
            partitionTxn.abort();
        }

        LdapConnection connection = IntegrationUtils.getAdminConnection( getService() );

        assertNotNull( connection.lookup( "ou=removable" ) );

        // Test removeContextPartition
        getService().removePartition( partition );

        assertNull( connection.lookup( "ou=removable" ) );
    }
}
