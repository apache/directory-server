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


import java.util.Hashtable;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NameNotFoundException;
import javax.naming.directory.Attributes;

import junit.framework.Assert;

import org.apache.directory.server.core.configuration.AddPartitionConfiguration;
import org.apache.directory.server.core.configuration.MutablePartitionConfiguration;
import org.apache.directory.server.core.configuration.RemovePartitionConfiguration;
import org.apache.directory.server.core.jndi.CoreContextFactory;
import org.apache.directory.server.core.partition.impl.btree.jdbm.JdbmPartition;
import org.apache.directory.server.core.unit.AbstractAdminTestCase;
import org.apache.directory.shared.ldap.message.AttributesImpl;


/**
 * Tests {@link AddPartitionConfiguration} and
 * {@link RemovePartitionConfiguration} works correctly.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class DirectoryPartitionConfigurationITest extends AbstractAdminTestCase
{
    public DirectoryPartitionConfigurationITest()
    {
    }


    public void testAddAndRemove() throws Exception
    {
        MutablePartitionConfiguration partitionCfg = new MutablePartitionConfiguration();
        partitionCfg.setName( "removable" );
        partitionCfg.setSuffix( "ou=removable" );
        Attributes ctxEntry = new AttributesImpl( true );
        ctxEntry.put( "objectClass", "top" );
        ctxEntry.put( "ou", "removable" );
        partitionCfg.setContextEntry( ctxEntry );
        partitionCfg.setContextPartition( new JdbmPartition() );

        // Test AddContextPartition
        AddPartitionConfiguration addCfg = new AddPartitionConfiguration( partitionCfg );

        Hashtable env = new Hashtable();
        env.put( Context.INITIAL_CONTEXT_FACTORY, CoreContextFactory.class.getName() );
        env.putAll( addCfg.toJndiEnvironment() );

        Context ctx = new InitialContext( env );
        Assert.assertNotNull( ctx.lookup( "ou=removable" ) );

        // Test removeContextPartition
        RemovePartitionConfiguration removeCfg = new RemovePartitionConfiguration( "ou=removable" );
        env.putAll( removeCfg.toJndiEnvironment() );

        ctx = new InitialContext( env );
        try
        {
            ctx.lookup( "ou=removable" );
            Assert.fail( "NameNotFoundException should be thrown." );
        }
        catch ( NameNotFoundException e )
        {
            // Partition is removed.
        }
    }
}
