/*
 *   Copyright 2004 The Apache Software Foundation
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 */
package org.apache.directory.server.kerberos.messages.value;

import java.util.Arrays;

import org.apache.kerberos.messages.value.KdcOptions;
import org.apache.kerberos.messages.value.TicketFlags;

import junit.framework.TestCase;

public class OptionsTest extends TestCase
{
    private byte[] fpriOptions = { (byte) 0x50, (byte) 0x00, (byte) 0x00, (byte) 0x10 };

    public void testToString()
    {
        TicketFlags flags = new TicketFlags();
        flags.set( TicketFlags.FORWARDABLE );
        flags.set( TicketFlags.PROXIABLE );
        flags.set( TicketFlags.RENEWABLE );
        flags.set( TicketFlags.INITIAL );
        assertEquals( flags.toString(), "FORWARDABLE PROXIABLE RENEWABLE INITIAL" );
    }

    public void testDuplicateSetting()
    {
        TicketFlags flags = new TicketFlags();
        flags.set( TicketFlags.MAY_POSTDATE );
        flags.set( TicketFlags.FORWARDABLE );
        flags.set( TicketFlags.PROXIABLE );
        flags.set( TicketFlags.MAY_POSTDATE );
        flags.set( TicketFlags.RENEWABLE );
        assertEquals( flags.toString(), "FORWARDABLE PROXIABLE MAY_POSTDATE RENEWABLE" );
    }

    public void testConstruction()
    {
        KdcOptions options = new KdcOptions( fpriOptions );
        assertTrue( Arrays.equals( options.getBytes(), fpriOptions ) );
    }
}
