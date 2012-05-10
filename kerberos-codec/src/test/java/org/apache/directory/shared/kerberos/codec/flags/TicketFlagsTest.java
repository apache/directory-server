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
package org.apache.directory.shared.kerberos.codec.flags;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;

import org.apache.directory.shared.kerberos.flags.TicketFlag;
import org.apache.directory.shared.kerberos.flags.TicketFlags;
import org.junit.Test;


/**
 * A clss used to test the TicketFlags class
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class TicketFlagsTest
{
    @Test
    public void testTicketFlags()
    {
        TicketFlags flags = new TicketFlags();

        assertFalse( flags.isForwardable() );
        flags.setFlag( TicketFlag.FORWARDABLE );

        assertTrue( flags.isForwardable() );
        assertTrue( flags.toString().startsWith( TicketFlag.FORWARDABLE.toString() ) );

        assertFalse( flags.isRenewable() );
        flags.setFlag( TicketFlag.RENEWABLE );
        assertTrue( flags.isRenewable() );
        assertTrue( flags.isForwardable() );

        int flagValue = flags.getIntValue();
        assertEquals( 0x40800000, flagValue );

        flags.clearFlag( TicketFlag.FORWARDABLE );
        assertTrue( flags.isRenewable() );
        assertFalse( flags.isForwardable() );

        byte[] bytes = flags.getData();
        assertTrue( Arrays.equals( new byte[]
            { 0x00, 0x00, ( byte ) 0x80, 0x00, 0x00 }, bytes ) );
    }
}
