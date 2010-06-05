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
package org.apache.directory.server.core.operations.hasEntry;

import static org.junit.Assert.assertTrue;

import org.apache.directory.server.core.integ.AbstractLdapTestUnit;
import org.apache.directory.server.core.integ.FrameworkRunner;
import org.apache.directory.server.core.interceptor.context.EntryOperationContext;
import org.apache.directory.shared.ldap.name.DN;
import org.junit.Test;
import org.junit.runner.RunWith;


/**
 * Test the hasEntry operation
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
@RunWith ( FrameworkRunner.class )
public class hasEntryPerfIT extends AbstractLdapTestUnit
{
    /**
     * A hasEntry performance test
     */
    @Test
    public void testPerfHasEntry() throws Exception
    {
        DN adminDn = new DN( "uid=admin, ou=system" );
        EntryOperationContext opContext = new EntryOperationContext( service.getAdminSession(), adminDn );
        boolean hasEntry = service.getOperationManager().hasEntry( opContext );

        assertTrue( hasEntry );
        
        long t0 = System.currentTimeMillis();
        
        for ( int i = 0; i < 100; i++ )
        {
            for ( int j = 0; j < 5000; j++ )
            {
                hasEntry = service.getOperationManager().hasEntry( opContext );
            }
            
            System.out.print( "." );
        }
        
        long t1 = System.currentTimeMillis();
        
        System.out.println( "Delta : " + ( t1 - t0 ) );
    }
}
