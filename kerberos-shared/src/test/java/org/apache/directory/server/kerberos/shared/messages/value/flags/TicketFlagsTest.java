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
package org.apache.directory.server.kerberos.shared.messages.value.flags;


import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;


/**
 * Test for TicketFlags
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class TicketFlagsTest
{

    @Test
    public void constructorsTest() throws Exception
    {
        // Empty c'tor -> no flags set
        TicketFlags tf = new TicketFlags();
        for ( TicketFlag t : TicketFlag.values() )
        {
            if ( !t.equals( TicketFlags.MAX_SIZE ) )
            {
                assertFalse( tf.isFlagSet( t ) );
            }
        }
        assertFalse( tf.isForwardable() );
        assertFalse( tf.isForwarded() );
        assertFalse( tf.isHwAuthent() );
        assertFalse( tf.isInitial() );
        assertFalse( tf.isInvalid() );
        assertFalse( tf.isMayPosdate() );
        assertFalse( tf.isOkAsDelegate() );
        assertFalse( tf.isPostdated() );
        assertFalse( tf.isPreAuth() );
        assertFalse( tf.isProxiable() );
        assertFalse( tf.isProxy() );
        assertFalse( tf.isRenewable() );
        assertFalse( tf.isReserved() );
        assertFalse( tf.isTransitedPolicyChecked() );

        // Flags 1, 2, 4, 8 set
        tf = new TicketFlags( 278 );
        assertFalse( tf.isReserved() ); // 0
        assertTrue( tf.isForwardable() ); // 1
        assertTrue( tf.isForwarded() ); // 2
        assertFalse( tf.isProxiable() ); // 3
        assertTrue( tf.isProxy() ); // 4
        assertFalse( tf.isMayPosdate() ); // 5
        assertFalse( tf.isPostdated() ); // 6
        assertFalse( tf.isInvalid() ); // 7
        assertTrue( tf.isRenewable() ); // 8
        assertFalse( tf.isInitial() ); // 9
        assertFalse( tf.isPreAuth() ); // 10
        assertFalse( tf.isHwAuthent() ); // 11
        assertFalse( tf.isTransitedPolicyChecked() ); // 12
        assertFalse( tf.isOkAsDelegate() ); // 13
    }

}
