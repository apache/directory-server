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


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.apache.directory.shared.kerberos.flags.AbstractKerberosFlags;
import org.apache.directory.shared.kerberos.flags.TicketFlag;
import org.junit.jupiter.api.Test;



/**
 * Test for AbstractKerberosFlags
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
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
        };

        // unset flag 4
        akf.clearFlag( TicketFlag.PROXY );
        assertEquals(
            (
            ( 1 << ( 31 - TicketFlag.FORWARDABLE.getValue() ) )
                + ( 1 << ( 31 - TicketFlag.FORWARDED.getValue() ) )
                + ( 1 << ( 31 - TicketFlag.RENEWABLE.getValue() ) ) ),
            akf.getIntValue(),
            "clear(KerberosFlag)" );

        // unset flag 2
        akf.clearFlag( TicketFlag.FORWARDED.getValue() );
        assertEquals(
            ( ( 1 << ( 31 - TicketFlag.FORWARDABLE.getValue() ) )
            + ( 1 << ( 31 - TicketFlag.RENEWABLE.getValue() ) ) ), akf.getIntValue(),
            "clear(KerberosFlag)" );
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
        };

        // No flags set
        AbstractKerberosFlags akfEmptyConstructor = new AbstractKerberosFlags()
        {
        };

        assertEquals( 0, akfEmptyConstructor.getIntValue(), "intValue" );
        assertEquals(
            ( ( 1 << ( 31 - TicketFlag.FORWARDABLE.getValue() ) ) )
                + ( 1 << ( 31 - TicketFlag.FORWARDED.getValue() ) )
                + ( 1 << ( 31 - TicketFlag.PROXY.getValue() ) )
                + ( 1 << ( 31 - TicketFlag.RENEWABLE.getValue() ) ), akfIntConstructor.getIntValue(),
                "intValue" );
    }


    @Test
    public void testFlagGetterSetter() throws Exception
    {
        AbstractKerberosFlags akf = new AbstractKerberosFlags()
        {
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
                assertTrue( akf.isFlagSet( ticketFlag ), "isFlagSet(TicketFlag): " + ticketFlag.toString() );
                assertTrue( akf.isFlagSet( ticketFlag.getValue() ), "isFlagSet(int): " + ticketFlag.toString() );
                assertTrue( 
                    AbstractKerberosFlags.isFlagSet( flagsValue, ticketFlag.getValue() ),
                    "isFlagSet(int,int): " + ticketFlag.toString() );
            }
            else
            {
                assertFalse( akf.isFlagSet( ticketFlag ), "isFlagSet(TicketFlag): " + ticketFlag.toString() );
                assertFalse( akf.isFlagSet( ticketFlag.getValue() ), "isFlagSet(int): " + ticketFlag.toString() );
                assertFalse( 
                    AbstractKerberosFlags.isFlagSet( flagsValue, ticketFlag.getValue() ),
                    "isFlagSet(int,int): " + ticketFlag.toString() );
            }

            setFlag = !setFlag;
        }
    }
}
