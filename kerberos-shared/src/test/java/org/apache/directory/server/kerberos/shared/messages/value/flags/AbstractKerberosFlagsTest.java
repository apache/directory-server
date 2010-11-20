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

import org.apache.directory.junit.tools.Concurrent;
import org.apache.directory.junit.tools.ConcurrentJunitRunner;
import org.junit.Test;
import org.junit.runner.RunWith;


/**
 * Test for AbstractKerberosFlags
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
@RunWith(ConcurrentJunitRunner.class)
@Concurrent()
public class AbstractKerberosFlagsTest
{
    @Test
    public void testClearFlag() throws Exception
    {
        // Flags 1, 2, 4, 8 set
        AbstractKerberosFlags akf = new AbstractKerberosFlags(
            getBytes( ( int ) ( Math.pow( 2, TicketFlag.FORWARDABLE.getOrdinal() )
                + Math.pow( 2, TicketFlag.FORWARDED.getOrdinal() ) + Math.pow( 2, TicketFlag.PROXY.getOrdinal() ) + Math
                .pow( 2, TicketFlag.RENEWABLE.getOrdinal() ) ) ) )
        {
            private static final long serialVersionUID = 1L;
        };

        // unset flag 4
        akf.clearFlag( TicketFlag.PROXY );
        assertEquals(
            "clear(KerberosFlag)",
            ( int ) ( Math.pow( 2, TicketFlag.FORWARDABLE.getOrdinal() )
                + Math.pow( 2, TicketFlag.FORWARDED.getOrdinal() ) + Math.pow( 2, TicketFlag.RENEWABLE.getOrdinal() ) ),
            akf.getIntValue() );

        // unset flag 2
        akf.clearFlag( TicketFlag.FORWARDED.getOrdinal() );
        assertEquals(
            "clear(int)",
            ( int ) ( Math.pow( 2, TicketFlag.FORWARDABLE.getOrdinal() ) + Math.pow( 2,
                TicketFlag.RENEWABLE.getOrdinal() ) ), akf.getIntValue() );
    }


    @Test
    public void testValue() throws Exception
    {
        // Flags 1, 2, 4, 8 set
        AbstractKerberosFlags akfIntConstructor = new AbstractKerberosFlags( getBytes( ( int ) ( Math.pow( 2,
            TicketFlag.FORWARDABLE.getOrdinal() )
            + Math.pow( 2, TicketFlag.FORWARDED.getOrdinal() )
            + Math.pow( 2, TicketFlag.PROXY.getOrdinal() ) + Math.pow( 2, TicketFlag.RENEWABLE.getOrdinal() ) ) ) )
        {
            private static final long serialVersionUID = 1L;
        };

        // No flags set
        AbstractKerberosFlags akfEmptyConstructor = new AbstractKerberosFlags()
        {
            private static final long serialVersionUID = 1L;
        };

        // intValue
        assertEquals( "intValue", 0, akfEmptyConstructor.getIntValue() );
        assertEquals(
            "intValue",
            ( int ) ( Math.pow( 2, TicketFlag.FORWARDABLE.getOrdinal() )
                + Math.pow( 2, TicketFlag.FORWARDED.getOrdinal() ) + Math.pow( 2, TicketFlag.PROXY.getOrdinal() ) + Math
                .pow( 2, TicketFlag.RENEWABLE.getOrdinal() ) ), akfIntConstructor.getIntValue() );

        // hexValue
        // TODO The method getHexValue() is a bit confusing WRT its comment and naming and what the method really
        // does. Ever seen a 'toHex' method returning an 'int'?
        //        assertEquals( "hexValue", Integer.toHexString( 0 ), Integer.toHexString( akfEmptyConstructor.getHexValue() ) );
        //        assertEquals( "hexValue", Integer.toHexString( ( int ) ( Math.pow( 2, TicketFlag.FORWARDABLE.getOrdinal() )
        //            + Math.pow( 2, TicketFlag.FORWARDED.getOrdinal() ) + Math.pow( 2, TicketFlag.PROXY.getOrdinal() ) + Math
        //            .pow( 2, TicketFlag.RENEWABLE.getOrdinal() ) ) ), Integer.toHexString( akfIntConstructor.getHexValue() ) );
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
            if ( !tf.equals( TicketFlag.MAX_VALUE ) )
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
                akf.setFlag( ticketFlag.getOrdinal() );
                flagsValue += Math.pow( 2, ticketFlag.getOrdinal() );
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
                assertTrue( "isFlagSet(int): " + ticketFlag.toString(), akf.isFlagSet( ticketFlag.getOrdinal() ) );
                assertTrue( "isFlagSet(int,int): " + ticketFlag.toString(),
                    AbstractKerberosFlags.isFlagSet( flagsValue, ticketFlag.getOrdinal() ) );
            }
            else
            {
                assertFalse( "isFlagSet(TicketFlag): " + ticketFlag.toString(), akf.isFlagSet( ticketFlag ) );
                assertFalse( "isFlagSet(int): " + ticketFlag.toString(), akf.isFlagSet( ticketFlag.getOrdinal() ) );
                assertFalse( "isFlagSet(int,int): " + ticketFlag.toString(),
                    AbstractKerberosFlags.isFlagSet( flagsValue, ticketFlag.getOrdinal() ) );
            }
            setFlag = !setFlag;
        }

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
            {
                ( byte ) ( 0 ), // unused bits
                ( byte ) ( flags >>> 24 ), ( byte ) ( ( flags >> 16 ) & 0x00ff ), ( byte ) ( ( flags >> 8 ) & 0x00ff ),
                ( byte ) ( flags & 0x00ff ) };
    }
}
