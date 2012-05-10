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
package org.apache.directory.shared.kerberos.codec.options;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Test;


/**
 * Test the Options class
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class OptionsTest
{
    @SuppressWarnings("serial")
    private class MyOptions extends Options
    {
        private MyOptions()
        {
            super( 22 );
        }
    }


    @Test
    public void testAddBytes()
    {
        MyOptions myOptions = new MyOptions();

        // Set the bits 10-0100 1010-1100 0000-0110
        myOptions.setBytes( new byte[]
            { 0x02, ( byte ) 0x92, ( byte ) 0xB0, 0x18 } );

        assertEquals( "1001001010110000000110", myOptions.toString() );
        assertFalse( myOptions.get( 21 ) );
        assertTrue( myOptions.get( 20 ) );

        try
        {
            myOptions.get( 22 );
            fail();
        }
        catch ( ArrayIndexOutOfBoundsException e )
        {
            assertTrue( true );
        }
    }
}
