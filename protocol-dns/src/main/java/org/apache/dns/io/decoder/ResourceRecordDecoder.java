/*
 *   Copyright 2005 The Apache Software Foundation
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

package org.apache.dns.io.decoder;

import java.nio.ByteBuffer;

public class ResourceRecordDecoder
{
    protected short getUnsignedByte( ByteBuffer byteBuffer )
    {
        return ( (short) ( byteBuffer.get() & 0xff ) );
    }

    protected short getUnsignedByte( ByteBuffer byteBuffer, int position )
    {
        return ( (short) ( byteBuffer.get( position ) & (short) 0xff ) );
    }

    protected int getUnsignedShort( ByteBuffer byteBuffer )
    {
        return ( byteBuffer.getShort() & 0xffff );
    }

    protected int getUnsignedShort( ByteBuffer byteBuffer, int position )
    {
        return ( byteBuffer.getShort( position ) & 0xffff );
    }

    protected long getUnsignedInt( ByteBuffer byteBuffer )
    {
        return ( byteBuffer.getInt() & 0xffffffffL );
    }

    protected long getUnsignedInt( ByteBuffer byteBuffer, int position )
    {
        return ( byteBuffer.getInt( position ) & 0xffffffffL );
    }
}
