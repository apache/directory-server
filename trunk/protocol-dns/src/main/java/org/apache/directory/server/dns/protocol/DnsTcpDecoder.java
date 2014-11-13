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

package org.apache.directory.server.dns.protocol;


import org.apache.directory.server.dns.io.decoder.DnsMessageDecoder;
import org.apache.directory.server.i18n.I18n;
import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.CumulativeProtocolDecoder;
import org.apache.mina.filter.codec.ProtocolDecoderOutput;


/**
 * A {@link CumulativeProtocolDecoder} which supports DNS operation over TCP,
 * by reassembling split packets prior to decoding.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class DnsTcpDecoder extends CumulativeProtocolDecoder
{
    private DnsMessageDecoder decoder = new DnsMessageDecoder();

    private int maxObjectSize = 16384; // 16KB


    /**
     * Returns the allowed maximum size of the object to be decoded.
     * 
     * @return The max object size.
     */
    public int getMaxObjectSize()
    {
        return maxObjectSize;
    }


    /**
     * Sets the allowed maximum size of the object to be decoded.
     * If the size of the object to be decoded exceeds this value, this
     * decoder will throw a {@link IllegalArgumentException}.  The default
     * value is <tt>16384</tt> (16KB).
     * 
     * @param maxObjectSize 
     */
    public void setMaxObjectSize( int maxObjectSize )
    {
        if ( maxObjectSize <= 0 )
        {
            throw new IllegalArgumentException( I18n.err( I18n.ERR_634, maxObjectSize ) );
        }

        this.maxObjectSize = maxObjectSize;
    }


    @Override
    protected boolean doDecode( IoSession session, IoBuffer in, ProtocolDecoderOutput out ) throws Exception
    {
        if ( !in.prefixedDataAvailable( 2, maxObjectSize ) )
        {
            return false;
        }

        in.getShort();

        out.write( decoder.decode( in ) );

        return true;
    }
}
