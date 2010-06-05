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
package org.apache.directory.server.core.operations.getRootDse;

import static org.junit.Assert.assertNotNull;

import org.apache.directory.ldap.client.api.LdapConnection;
import org.apache.directory.server.core.integ.AbstractLdapTestUnit;
import org.apache.directory.server.core.integ.FrameworkRunner;
import org.apache.directory.server.core.integ.IntegrationUtils;
import org.apache.directory.server.core.interceptor.context.GetRootDSEOperationContext;
import org.apache.directory.shared.ldap.entry.Entry;
import org.junit.Test;
import org.junit.runner.RunWith;


/**
 * Test the GetRootDSE operation
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
@RunWith ( FrameworkRunner.class )
public class GetRootDsePerfIT extends AbstractLdapTestUnit
{
    /**
     * A GetRootDSE performance test
     */
    @Test
    public void testPerfGetRootDSE() throws Exception
    {
        LdapConnection connection = IntegrationUtils.getAdminConnection( service );

        GetRootDSEOperationContext opContext = new GetRootDSEOperationContext( service.getAdminSession() );
        Entry rootDSE = service.getOperationManager().getRootDSE( opContext );

        assertNotNull( rootDSE );
        
        long t0 = System.currentTimeMillis();
        
        for ( int i = 0; i < 100; i++ )
        {
            for ( int j = 0; j < 5000; j++ )
            {
                rootDSE = service.getOperationManager().getRootDSE( opContext );
            }
            
            System.out.print( "." );
        }
        
        long t1 = System.currentTimeMillis();
        
        System.out.println( "Delta : " + ( t1 - t0 ) );
        connection.close();
    }
}
