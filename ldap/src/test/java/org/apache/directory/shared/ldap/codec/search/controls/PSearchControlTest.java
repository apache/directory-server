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
package org.apache.directory.shared.ldap.codec.search.controls;


import java.nio.ByteBuffer;

import javax.naming.NamingException;

import org.apache.directory.shared.asn1.ber.Asn1Decoder;
import org.apache.directory.shared.asn1.codec.DecoderException;
import org.apache.directory.shared.ldap.codec.search.controls.PSearchControl;
import org.apache.directory.shared.ldap.codec.search.controls.PSearchControlContainer;
import org.apache.directory.shared.ldap.codec.search.controls.PSearchControlDecoder;
import org.apache.directory.shared.ldap.util.StringTools;

import junit.framework.Assert;
import junit.framework.TestCase;


/**
 * Test the PSearchControlTest codec
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class PSearchControlTest extends TestCase
{
    /**
     * Test the decoding of a PSearchControl
     */
    public void testDecodeModifyDNRequestSuccess() throws NamingException
    {
        Asn1Decoder decoder = new PSearchControlDecoder();
        ByteBuffer bb = ByteBuffer.allocate( 0x0b );
        bb.put( new byte[]
            { 0x30, 0x09, // PersistentSearch ::= SEQUENCE {
                0x02, 0x01, 0x01, // changeTypes INTEGER,
                0x01, 0x01, 0x00, // changesOnly BOOLEAN,
                0x01, 0x01, 0x00 // returnECs BOOLEAN
            // }
            } );
        bb.flip();

        PSearchControlContainer container = new PSearchControlContainer();
        try
        {
            decoder.decode( bb, container );
        }
        catch ( DecoderException de )
        {
            de.printStackTrace();
            Assert.fail( de.getMessage() );
        }

        PSearchControl control = container.getPSearchControl();
        assertEquals( 1, control.getChangeTypes() );
        assertEquals( false, control.isChangesOnly() );
        assertEquals( false, control.isReturnECs() );
    }


    /**
     * Test encoding of a PSearchControl.
     */
    public void testEncodePSearchControl() throws Exception
    {
        ByteBuffer bb = ByteBuffer.allocate( 0x0b );
        bb.put( new byte[]
            { 0x30, 0x09, // PersistentSearch ::= SEQUENCE {
                0x02, 0x01, 0x01, // changeTypes INTEGER,
                0x01, 0x01, 0x00, // changesOnly BOOLEAN,
                0x01, 0x01, 0x00 // returnECs BOOLEAN
            // }
            } );

        String expected = StringTools.dumpBytes( bb.array() );
        bb.flip();

        PSearchControl ctrl = new PSearchControl();
        ctrl.setChangesOnly( false );
        ctrl.setReturnECs( false );
        ctrl.setChangeTypes( 1 );
        bb = ctrl.encode( null );
        String decoded = StringTools.dumpBytes( bb.array() );
        assertEquals( expected, decoded );
    }
}
