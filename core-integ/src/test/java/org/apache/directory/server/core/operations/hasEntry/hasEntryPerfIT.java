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

import org.apache.directory.server.core.api.OperationEnum;
import org.apache.directory.server.core.api.interceptor.context.HasEntryOperationContext;
import org.apache.directory.server.core.integ.AbstractLdapTestUnit;
import org.apache.directory.server.core.integ.FrameworkRunner;
import org.apache.directory.shared.ldap.model.name.Dn;
import org.junit.Test;
import org.junit.runner.RunWith;


/**
 * Test the hasEntry operation
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
@RunWith(FrameworkRunner.class)
public class hasEntryPerfIT extends AbstractLdapTestUnit
{
    /**
     * A hasEntry performance test
     */
    @Test
    public void testPerfHasEntry() throws Exception
    {
        Dn adminDn = new Dn( "uid=admin, ou=system" );
        HasEntryOperationContext hasEntryContext = new HasEntryOperationContext( getService().getAdminSession(),
            adminDn );
        hasEntryContext.setInterceptors( getService().getInterceptors( OperationEnum.HAS_ENTRY ) );
        boolean hasEntry = getService().getOperationManager().hasEntry( hasEntryContext );

        assertTrue( hasEntry );
        int nbIterations = 150000;

        long t0 = System.currentTimeMillis();
        long t00 = 0L;
        long tt0 = System.currentTimeMillis();

        for ( int i = 0; i < nbIterations; i++ )
        {
            if ( i % 1000 == 0 )
            {
                long tt1 = System.currentTimeMillis();

                System.out.println( i + ", " + ( tt1 - tt0 ) );
                tt0 = tt1;
            }

            if ( i == 50000 )
            {
                t00 = System.currentTimeMillis();
            }

            hasEntryContext.setCurrentInterceptor( 0 );
            hasEntry = getService().getOperationManager().hasEntry( hasEntryContext );
        }

        long t1 = System.currentTimeMillis();

        Long deltaWarmed = ( t1 - t00 );
        System.out.println( "Delta hasEntry: " + deltaWarmed + "( "
            + ( ( ( nbIterations - 50000 ) * 1000 ) / deltaWarmed ) + " per s ) /" + ( t1 - t0 ) );
    }
}
