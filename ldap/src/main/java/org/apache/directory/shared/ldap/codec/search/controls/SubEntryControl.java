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
package org.apache.directory.shared.ldap.codec.search.controls;


import java.nio.ByteBuffer;

import org.apache.directory.shared.asn1.Asn1Object;
import org.apache.directory.shared.asn1.ber.tlv.Value;
import org.apache.directory.shared.asn1.codec.EncoderException;


/**
 * A searchRequest control.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class SubEntryControl extends Asn1Object
{
    private boolean visibility = false;


    /**
     * Check if the subEntry is visible
     * 
     * @return true or false.
     */
    public boolean isVisible()
    {
        return visibility;
    }


    /**
     * Set the visibility flag
     * 
     * @param visibility
     *            The visibility flag : true or false
     */
    public void setVisibility( boolean visibility )
    {
        this.visibility = visibility;
    }


    /**
     * Compute the SubEntryControl length 0x01 0x01 [0x00|0xFF]
     */
    public int computeLength()
    {
        return 1 + 1 + 1;
    }


    /**
     * Encodes the subEntry control.
     * 
     * @param buffer
     *            The encoded sink
     * @return A ByteBuffer that contains the encoded PDU
     * @throws EncoderException
     *             If anything goes wrong.
     */
    public ByteBuffer encode( ByteBuffer buffer ) throws EncoderException
    {
        // Allocate the bytes buffer.
        ByteBuffer bb = ByteBuffer.allocate( computeLength() );
        Value.encode( bb, visibility );

        return bb;
    }


    /**
     * Return a String representing this EntryChangeControl.
     */
    public String toString()
    {
        StringBuffer sb = new StringBuffer();

        sb.append( "    SubEntry Control\n" );
        sb.append( "        Visibility   : '" ).append( visibility ).append( "'\n" );

        return sb.toString();
    }
}
