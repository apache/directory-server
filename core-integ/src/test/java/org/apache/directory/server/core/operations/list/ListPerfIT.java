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
package org.apache.directory.server.core.operations.list;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.apache.directory.server.core.api.interceptor.context.ListOperationContext;
import org.apache.directory.server.core.filtering.EntryFilteringCursor;
import org.apache.directory.server.core.integ.AbstractLdapTestUnit;
import org.apache.directory.server.core.integ.FrameworkRunner;
import org.apache.directory.shared.ldap.model.entry.Entry;
import org.apache.directory.shared.ldap.model.name.Dn;
import org.junit.Test;
import org.junit.runner.RunWith;


/**
 * Test the List operation
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
@RunWith ( FrameworkRunner.class )
public class ListPerfIT extends AbstractLdapTestUnit
{
    /**
     * A List performance test
     */
    @Test
    public void testPerfList() throws Exception
    {
        ListOperationContext listContext = new ListOperationContext( getService().getAdminSession(), new Dn( "ou=system" ) );
        EntryFilteringCursor cursor = getService().getOperationManager().list( listContext );

        assertNotNull( cursor );
        int nb = 0;
        
        while ( cursor.next() )
        {
            Entry entry = cursor.get();
            nb++;
            
            assertNotNull( entry );
        }
        
        cursor.close();
        
        assertEquals( 5, nb );
        
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

            nb = 0;
            cursor = getService().getOperationManager().list( listContext );

            while ( cursor.next() )
            {
                Entry entry = cursor.get();
                nb++;
                
                assertNotNull( entry );
            }
            
            cursor.close();
            
            assertEquals( 5, nb );
        }
            
        long t1 = System.currentTimeMillis();

        Long deltaWarmed = ( t1 - t00 );
        System.out.println( "Delta list: " + deltaWarmed + "( " + ( ( ( nbIterations - 50000 ) * 1000 ) / deltaWarmed ) + " per s ) /" + ( t1 - t0 ) );
    }
}
