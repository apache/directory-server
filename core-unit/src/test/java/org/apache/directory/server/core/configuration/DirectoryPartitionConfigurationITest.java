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


import junit.framework.Assert;
import org.apache.directory.server.core.DirectoryService;
import org.apache.directory.server.core.jndi.CoreContextFactory;
import org.apache.directory.server.core.partition.Partition;
import org.apache.directory.server.core.partition.impl.btree.jdbm.JdbmPartition;
import org.apache.directory.server.core.unit.AbstractAdminTestCase;
import org.apache.directory.shared.ldap.message.AttributesImpl;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NameNotFoundException;
import javax.naming.directory.Attributes;
import java.util.Hashtable;


/**
 * Tests dynamic partition addition and removal.
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
        Partition partition = new JdbmPartition();
        partition.setId( "removable" );
        partition.setSuffix( "ou=removable" );
        Attributes ctxEntry = new AttributesImpl( true );
        ctxEntry.put( "objectClass", "top" );
        ctxEntry.put( "ou", "removable" );
        partition.setContextEntry( ctxEntry );

        // Test AddContextPartition
        service.addPartition( partition );

        Hashtable<String,Object> env = new Hashtable<String,Object>();
        env.put( Context.INITIAL_CONTEXT_FACTORY, CoreContextFactory.class.getName() );
        env.put( DirectoryService.JNDI_KEY, service );
        env.put( Context.SECURITY_CREDENTIALS, "secret" );
        env.put( Context.SECURITY_AUTHENTICATION, "simple" );
        env.put( Context.SECURITY_PRINCIPAL, "uid=admin,ou=system" );
        Context ctx = new InitialContext( env );
        Assert.assertNotNull( ctx.lookup( "ou=removable" ) );

        // Test removeContextPartition
        service.removePartition( partition );
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
