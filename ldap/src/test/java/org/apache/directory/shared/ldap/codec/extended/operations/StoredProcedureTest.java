/*
 *   Copyright 2006 The Apache Software Foundation
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

package org.apache.directory.shared.ldap.codec.extended.operations;


import java.nio.ByteBuffer;

import javax.naming.NamingException;

import junit.framework.Assert;
import junit.framework.TestCase;

import org.apache.directory.shared.asn1.ber.Asn1Decoder;
import org.apache.directory.shared.asn1.ber.IAsn1Container;
import org.apache.directory.shared.asn1.codec.DecoderException;
import org.apache.directory.shared.ldap.codec.extended.operations.StoredProcedure.StoredProcedureParameter;
import org.apache.directory.shared.ldap.util.StringTools;


/*
 * TestCase for a Stored Procedure Extended Operation ASN.1 codec
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class StoredProcedureTest extends TestCase
{
    public void testDecodeStoredProcedure() throws NamingException
    {
        Asn1Decoder storedProcedureDecoder = new StoredProcedureDecoder();

        ByteBuffer stream = ByteBuffer.allocate( 0x3E );

        stream.put( new byte[]
            {
                0x30, 0x3C,
                    0x04, 0x04, 'J', 'a', 'v', 'a',
                    0x04, 0x07, 'e', 'x', 'e', 'c', 'u', 't', 'e',
                    0x30, 0x2B,
                        0x04, 0x03, 'i', 'n', 't', 0x04, 0x01, 0x01,
                        0x04, 0x07, 'b', 'o', 'o', 'l', 'e', 'a', 'n', 0x04, 0x04, 't', 'r', 'u', 'e',
                        0x04, 0x06, 'S', 't', 'r', 'i', 'n', 'g', 0x04, 0x0A, 'p', 'a', 'r', 'a', 'm', 'e', 't', 'e', 'r', '3' 
            } );

        //String decodedPdu = StringTools.dumpBytes( stream.array() );
        stream.flip();

        // Allocate a StoredProcedure Container
        IAsn1Container storedProcedureContainer = new StoredProcedureContainer();

        // Decode a StoredProcedure message
        try
        {
            storedProcedureDecoder.decode( stream, storedProcedureContainer );
        }
        catch ( DecoderException de )
        {
            de.printStackTrace();
            Assert.fail( de.getMessage() );
        }

        StoredProcedure storedProcedure = ( ( StoredProcedureContainer ) storedProcedureContainer ).getStoredProcedure();

        Assert.assertEquals("Java", storedProcedure.getLanguage());
        
        Assert.assertEquals( "execute", StringTools.utf8ToString( storedProcedure.getProcedure() ) );

        Assert.assertNotNull( storedProcedure.getParameters() );
        Assert.assertEquals( 3, storedProcedure.getParameters().size() );

        StoredProcedureParameter param = ( StoredProcedureParameter ) storedProcedure.getParameters().get( 0 );

        Assert.assertEquals( "int", StringTools.utf8ToString( param.getType() ) );
        Assert.assertEquals( 1, param.getValue()[0] );

        param = ( StoredProcedureParameter ) storedProcedure.getParameters().get( 1 );

        Assert.assertEquals( "boolean", StringTools.utf8ToString( param.getType() ) );
        Assert.assertEquals( "true", StringTools.utf8ToString( param.getValue() ) );

        param = ( StoredProcedureParameter ) storedProcedure.getParameters().get( 2 );

        Assert.assertEquals( "String", StringTools.utf8ToString( param.getType() ) );
        Assert.assertEquals( "parameter3", StringTools.utf8ToString( param.getValue() ) );

    }
}
