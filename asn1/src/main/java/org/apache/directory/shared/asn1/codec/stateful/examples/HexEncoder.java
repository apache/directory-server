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

import org.apache.directory.shared.asn1.codec.EncoderException;
import org.apache.directory.shared.asn1.codec.stateful.EncoderCallback;
import org.apache.directory.shared.asn1.codec.stateful.EncoderMonitor;
import org.apache.directory.shared.asn1.codec.stateful.EncoderMonitorAdapter;
import org.apache.directory.shared.asn1.codec.stateful.StatefulEncoder;


/**
 * Document me.
 * 
 * @author <a href="mailto:dev@directory.apache.org"> Apache Directory
 *         Project</a> $Rev$
 */
public class HexEncoder implements StatefulEncoder
{
    private static final int CHUNK_SZ = 128;
    private ByteBuffer buf = ByteBuffer.allocate( CHUNK_SZ );
    private EncoderMonitor monitor = new EncoderMonitorAdapter();
    private EncoderCallback cb = new EncoderCallback() {
        public void encodeOccurred( StatefulEncoder encoder, Object encoded )
        {
        }
    };
    private final byte[] HEXCHAR_LUT = {
        (byte)'0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
        'a', 'b', 'c', 'd', 'e', 'f'
    };


    /**
     * Transforms a decoded ByteArray of binary data into a stream of ASCII hex
     * characters.
     *
     * @param obj
     * @throws org.apache.directory.shared.asn1.codec.EncoderException
     */
    public void encode( Object obj ) throws EncoderException
    {
        ByteBuffer raw = ( ByteBuffer ) obj;

        if ( raw == null || ! raw.hasRemaining() )
        {
            return;
        }

        /*
         * Keep encoding one byte at a time if we have remaining bytes in the
         * raw buffer and there's space for 2 hex character bytes in the
         * resultant hex encoded buffer.
         */
        while( raw.hasRemaining() )
        {
            if ( ! buf.hasRemaining() )
            {
                buf.flip();
                cb.encodeOccurred( this, buf );
                monitor.callbackOccured( this, cb, buf );
                buf.clear();
            }

            byte bite = raw.get();
            buf.put( HEXCHAR_LUT[( bite >> 4 ) & 0x0000000F] );
            buf.put( HEXCHAR_LUT[bite & 0x0000000F] );
        }

        buf.flip();
        cb.encodeOccurred( this, buf );
        monitor.callbackOccured( this, cb, buf );
        buf.clear();
    }


    public void setCallback( EncoderCallback cb )
    {
        EncoderCallback old = this.cb;
        this.cb = cb;
        monitor.callbackSet( this, old, cb );
    }


    public void setEncoderMonitor( EncoderMonitor monitor )
    {
        this.monitor = monitor;
    }
}
