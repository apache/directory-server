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
import org.apache.directory.server.core.annotations.CreateDS;
import org.apache.directory.server.core.factory.DefaultDirectoryServiceFactory;
import org.apache.directory.server.core.factory.PartitionFactory;
import org.apache.directory.server.core.integ.AbstractLdapTestUnit;
import org.apache.directory.server.core.integ.FrameworkRunner;
import org.apache.directory.server.core.integ.IntegrationUtils;
import org.apache.directory.server.core.interceptor.context.AddOperationContext;
import org.apache.directory.server.core.partition.Partition;
import org.apache.directory.shared.ldap.csn.CsnFactory;
import org.apache.directory.shared.ldap.model.entry.Entry;
import org.apache.directory.shared.ldap.model.ldif.LdifUtils;
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

    @Test
    public void testAddAndRemove() throws Exception
    {
        PartitionFactory partitionFactory = DefaultDirectoryServiceFactory.DEFAULT.getPartitionFactory();
        Partition partition = partitionFactory.createPartition( "removable", "ou=removable", 100, service
            .getInstanceLayout().getPartitionsDirectory() );

        // Test AddContextPartition
        service.addPartition( partition );

        Dn suffixDn = new Dn( "ou=removable", service.getSchemaManager() );

        Entry ctxEntry = LdifUtils.createEntry( service.getSchemaManager(), suffixDn,
            "objectClass: top",
            "objectClass: organizationalUnit",
            "ou: removable",
            "entryCSN", new CsnFactory( 1 ).newInstance().toString(),
            "entryUUID", UUID.randomUUID().toString() );

        partition.add( new AddOperationContext( service.getAdminSession(), ctxEntry ) );

        LdapConnection connection = IntegrationUtils.getAdminConnection( service );

        assertNotNull( connection.lookup( "ou=removable" ) );

        // Test removeContextPartition
        service.removePartition( partition );

        assertNull( connection.lookup( "ou=removable" ) );
    }
}
