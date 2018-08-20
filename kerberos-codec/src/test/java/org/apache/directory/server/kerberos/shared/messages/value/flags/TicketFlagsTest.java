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


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.mycila.junit.concurrent.Concurrency;
import com.mycila.junit.concurrent.ConcurrentJunitRunner;
import org.apache.directory.shared.kerberos.flags.TicketFlag;
import org.apache.directory.shared.kerberos.flags.TicketFlags;
import org.junit.Test;
import org.junit.runner.RunWith;


/**
 * Test for TicketFlags
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
@RunWith(ConcurrentJunitRunner.class)
@Concurrency()
public class TicketFlagsTest
{

    @Test
    public void testEmptyConstructor() throws Exception
    {
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
    }


    @Test
    public void testGivenIntConstructor() throws Exception
    {
        // Flags 1, 2, 4, 8 set
        TicketFlags tf = new TicketFlags( ( int ) ( Math.pow( 2, 31 - 1 ) + Math.pow( 2, 31 - 2 )
            + Math.pow( 2, 31 - 4 ) + Math.pow(
            2, 31 - 8 ) ) );
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


    @Test
    public void testGivenByteArrayConstructor() throws Exception
    {
        // Flags 1, 2, 4, 8 set
        TicketFlags tf = new TicketFlags(
            getBytes( ( int ) ( ( 1 << ( 31 - 1 ) ) |
                ( 1 << ( 31 - 2 ) ) |
                ( 1 << ( 31 - 4 ) ) |
            ( 1 << 31 - 8 ) ) ) );
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


    @Test
    public void testSetFlag() throws Exception
    {
        TicketFlags tf = new TicketFlags();

        for ( TicketFlag t : TicketFlag.values() )
        {
            if ( !t.equals( TicketFlag.MAX_VALUE ) )
            {
                tf.setFlag( t );
            }
        }

        assertTrue( tf.isReserved() ); // 0
        assertTrue( tf.isForwardable() ); // 1
        assertTrue( tf.isForwarded() ); // 2
        assertTrue( tf.isProxiable() ); // 3
        assertTrue( tf.isProxy() ); // 4
        assertTrue( tf.isMayPosdate() ); // 5
        assertTrue( tf.isPostdated() ); // 6
        assertTrue( tf.isInvalid() ); // 7
        assertTrue( tf.isRenewable() ); // 8
        assertTrue( tf.isInitial() ); // 9
        assertTrue( tf.isPreAuth() ); // 10
        assertTrue( tf.isHwAuthent() ); // 11
        assertTrue( tf.isTransitedPolicyChecked() ); // 12
        assertTrue( tf.isOkAsDelegate() ); // 13
    }


    /**
     * Tests converting the ticket flags to a descriptive String.
     */
    @Test
    public void testToString() throws Exception
    {
        TicketFlags tf = new TicketFlags();
        assertEquals( "toString()", "", tf.toString() );

        int i = 0;
        for ( TicketFlag t : TicketFlag.values() )
        {
            if ( t != TicketFlag.MAX_VALUE )
            {
                i |= 1 << ( 31 - t.getValue() );
            }
        }

        tf = new TicketFlags( i );
        assertEquals( "toString()", "RESERVED(0) FORWARDABLE(1) FORWARDED(2) PROXIABLE(3) PROXY(4) "
            + "MAY_POSTDATE(5) POSTDATED(6) INVALID(7) RENEWABLE(8) INITIAL(9) PRE_AUTHENT(10) "
            + "HW_AUTHENT(11) TRANSITED_POLICY_CHECKED(12) OK_AS_DELEGATE(13)", tf.toString() );
    }


    /**
     * Tests that setting flags is idempotent.
     */
    @Test
    public void testDuplicateSetting()
    {
        TicketFlags flags = new TicketFlags();
        flags.setFlag( TicketFlag.MAY_POSTDATE );
        flags.setFlag( TicketFlag.FORWARDABLE );
        flags.setFlag( TicketFlag.PROXIABLE );
        flags.setFlag( TicketFlag.MAY_POSTDATE );
        flags.setFlag( TicketFlag.RENEWABLE );
        assertEquals( "FORWARDABLE(1) PROXIABLE(3) MAY_POSTDATE(5) RENEWABLE(8)", flags.toString() );
    }


    /**
     * 
     * Get the byte array representation of an int
     *
     * @param flags The flags as int
     * @return The Flags as byte array
     */
    private byte[] getBytes( int flags )
    {
        return new byte[]
            { 0x00,
                ( byte ) ( flags >>> 24 ),
                ( byte ) ( ( flags >> 16 ) & 0x00ff ),
                ( byte ) ( ( flags >> 8 ) & 0x00ff ),
                ( byte ) ( flags & 0x00ff ) };
    }
}
