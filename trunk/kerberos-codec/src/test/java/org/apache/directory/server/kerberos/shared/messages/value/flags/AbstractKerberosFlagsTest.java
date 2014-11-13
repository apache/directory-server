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

import org.apache.directory.shared.kerberos.flags.AbstractKerberosFlags;
import org.apache.directory.shared.kerberos.flags.TicketFlag;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.mycila.junit.concurrent.Concurrency;
import com.mycila.junit.concurrent.ConcurrentJunitRunner;


/**
 * Test for AbstractKerberosFlags
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
@RunWith(ConcurrentJunitRunner.class)
@Concurrency()
public class AbstractKerberosFlagsTest
{
    @Test
    public void testClearFlag() throws Exception
    {
        // Flags 1, 2, 4, 8 set
        AbstractKerberosFlags akf = new AbstractKerberosFlags(
            ( 1 << ( 31 - TicketFlag.FORWARDABLE.getValue() ) )
                + ( 1 << ( 31 - TicketFlag.FORWARDED.getValue() ) )
                + ( 1 << ( 31 - TicketFlag.PROXY.getValue() ) )
                + ( 1 << ( 31 - TicketFlag.RENEWABLE.getValue() ) ) )
        {
            private static final long serialVersionUID = 1L;
        };

        // unset flag 4
        akf.clearFlag( TicketFlag.PROXY );
        assertEquals(
            "clear(KerberosFlag)",
            (
            ( 1 << ( 31 - TicketFlag.FORWARDABLE.getValue() ) )
                + ( 1 << ( 31 - TicketFlag.FORWARDED.getValue() ) )
                + ( 1 << ( 31 - TicketFlag.RENEWABLE.getValue() ) ) ),
            akf.getIntValue() );

        // unset flag 2
        akf.clearFlag( TicketFlag.FORWARDED.getValue() );
        assertEquals(
            "clear(int)",
            ( ( 1 << ( 31 - TicketFlag.FORWARDABLE.getValue() ) )
            + ( 1 << ( 31 - TicketFlag.RENEWABLE.getValue() ) ) ), akf.getIntValue() );
    }


    @Test
    public void testValue() throws Exception
    {
        // Flags 1, 2, 4, 8 set
        AbstractKerberosFlags akfIntConstructor = new AbstractKerberosFlags(
            ( ( 1 << ( 31 - TicketFlag.FORWARDABLE.getValue() ) )
                + ( 1 << ( 31 - TicketFlag.FORWARDED.getValue() ) )
                + ( 1 << ( 31 - TicketFlag.PROXY.getValue() ) )
                + ( 1 << ( 31 - TicketFlag.RENEWABLE.getValue() ) ) ) )
        {
            private static final long serialVersionUID = 1L;
        };

        // No flags set
        AbstractKerberosFlags akfEmptyConstructor = new AbstractKerberosFlags()
        {
            private static final long serialVersionUID = 1L;
        };

        assertEquals( "intValue", 0, akfEmptyConstructor.getIntValue() );
        assertEquals(
            "intValue",
            ( ( 1 << ( 31 - TicketFlag.FORWARDABLE.getValue() ) ) )
                + ( 1 << ( 31 - TicketFlag.FORWARDED.getValue() ) )
                + ( 1 << ( 31 - TicketFlag.PROXY.getValue() ) )
                + ( 1 << ( 31 - TicketFlag.RENEWABLE.getValue() ) ), akfIntConstructor.getIntValue() );
    }


    @Test
    public void testFlagGetterSetter() throws Exception
    {
        AbstractKerberosFlags akf = new AbstractKerberosFlags()
        {
            private static final long serialVersionUID = 1L;
        };

        // MAX_VALUE is not a real ticket flag and will cause an IndexOutOfBoundsException,
        // so skip this
        TicketFlag[] ticketFlags = new TicketFlag[TicketFlag.values().length - 1];
        int i = 0;
        int flagsValue = 0;

        for ( TicketFlag tf : TicketFlag.values() )
        {
            if ( tf != TicketFlag.MAX_VALUE )
            {
                ticketFlags[i] = tf;
            }

            i++;
        }

        boolean setFlag = true;

        for ( TicketFlag ticketFlag : ticketFlags )
        {
            // Only set every 2nd ticket flag
            if ( setFlag )
            {
                akf.setFlag( ticketFlag.getValue() );
                flagsValue += ( 1 << ( 31 - ticketFlag.getValue() ) );
            }

            setFlag = !setFlag;
        }

        setFlag = true;

        for ( TicketFlag ticketFlag : ticketFlags )
        {
            // Only every 2nd ticket flag is set
            if ( setFlag )
            {
                assertTrue( "isFlagSet(TicketFlag): " + ticketFlag.toString(), akf.isFlagSet( ticketFlag ) );
                assertTrue( "isFlagSet(int): " + ticketFlag.toString(), akf.isFlagSet( ticketFlag.getValue() ) );
                assertTrue( "isFlagSet(int,int): " + ticketFlag.toString(),
                    AbstractKerberosFlags.isFlagSet( flagsValue, ticketFlag.getValue() ) );
            }
            else
            {
                assertFalse( "isFlagSet(TicketFlag): " + ticketFlag.toString(), akf.isFlagSet( ticketFlag ) );
                assertFalse( "isFlagSet(int): " + ticketFlag.toString(), akf.isFlagSet( ticketFlag.getValue() ) );
                assertFalse( "isFlagSet(int,int): " + ticketFlag.toString(),
                    AbstractKerberosFlags.isFlagSet( flagsValue, ticketFlag.getValue() ) );
            }

            setFlag = !setFlag;
        }
    }
}
