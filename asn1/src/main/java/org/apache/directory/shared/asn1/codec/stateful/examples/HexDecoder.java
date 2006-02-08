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
package org.apache.directory.shared.asn1.codec.stateful.examples;

import java.nio.ByteBuffer;

import org.apache.directory.shared.asn1.codec.DecoderException;
import org.apache.directory.shared.asn1.codec.stateful.AbstractStatefulDecoder;

/**
 * Document me.
 * 
 * @author <a href="mailto:dev@directory.apache.org"> Apache Directory
 *         Project</a> $Rev$
 */
public class HexDecoder extends AbstractStatefulDecoder
{
    private ByteBuffer decoded = ByteBuffer.allocate( 128 ) ;
    private byte lsn ;
    private byte msn ;
    private boolean expectingMsn = true ;


    public void decode( Object chunk ) throws DecoderException
    {
        ByteBuffer encoded = ( ByteBuffer ) chunk;

        if ( encoded == null || !encoded.hasRemaining() )
        {
            return;
        }

        while ( encoded.hasRemaining() )
        {
            if ( ! decoded.hasRemaining() )
            {
                decoded.flip();
                super.decodeOccurred( decoded );
                decoded.clear();
            }

            if ( expectingMsn )
            {
                msn = encoded.get();
                expectingMsn = false;
            }
            else
            {
                lsn = encoded.get();
                expectingMsn = true;
            }

            /* if we've hit the most significant nibble then we have two hex
             * characters as bytes so we need to compute and add the byte to
             * the buffer
             */
            if ( expectingMsn )
            {
                byte bite = getNibble( lsn );
                bite |= ( getNibble( msn ) << 4 );
                decoded.put( bite );
            }
        }

        /* only trigger a decode callback if we have seen an even number of
         * hex character bytes in which case we're in the expectingMsn state
         * this will flush out what's siting in the buffer automatically
         */
        if ( expectingMsn )
        {
            decoded.flip();
            super.decodeOccurred( decoded );
            decoded.clear();
        }
    }



    private byte getNibble( byte ch ) throws DecoderException
    {
        // lowercase the character if it is in upper case
        if ( ch > 64 && ch < 91 )
        {
            ch -= 32;
        }

        switch(ch)
        {
            case 48:
                return 0 ;
            case 49:
                return 1 ;
            case 50:
                return 2 ;
            case 51:
                return 3 ;
            case 52:
                return 4 ;
            case 53:
                return 5 ;
            case 54:
                return 6 ;
            case 55:
                return 7 ;
            case 56:
                return 8 ;
            case 57:
                return 9 ;
            case 97:
                return 10 ;
            case 98:
                return 11 ;
            case 99:
                return 12 ;
            case 100:
                return 13 ;
            case 101:
                return 14 ;
            case 102:
                return 15 ;
            default:
                throw new DecoderException( "non-hex character '" + (char) ch
                    + "' encountered" );
        }
    }
}
