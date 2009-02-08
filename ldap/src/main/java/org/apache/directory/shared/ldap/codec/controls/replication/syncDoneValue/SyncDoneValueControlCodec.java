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
package org.apache.directory.shared.ldap.codec.controls.replication.syncDoneValue;


import java.nio.ByteBuffer;

import org.apache.directory.shared.asn1.AbstractAsn1Object;
import org.apache.directory.shared.asn1.ber.tlv.TLV;
import org.apache.directory.shared.asn1.ber.tlv.UniversalTag;
import org.apache.directory.shared.asn1.ber.tlv.Value;
import org.apache.directory.shared.asn1.codec.EncoderException;
import org.apache.directory.shared.ldap.util.StringTools;


/**
 * 
 * A syncDoneValue object as described in rfc4533.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class SyncDoneValueControlCodec extends AbstractAsn1Object
{
    /** The Sync cookie */
    private byte[] cookie;

    /** the refreshDeletes flag */
    private boolean refreshDeletes;

    /** The global length for this control */
    private int syncDoneValueLength;


    /**
     * Compute the syncDoneValue length.
     * 0x30 L1
     * |
     * +--> 0x04 L2 xkcd!!!...     (cookie)
     * +--> 0x01 0x01 [0x00|0xFF]  (refreshDeletes)
     */
    @Override
    public int computeLength()
    {
        // cookie's length
        if ( cookie != null )
        {
            syncDoneValueLength = 1 + TLV.getNbBytes( cookie.length ) + cookie.length;
        }

        // the refreshDeletes flag length
        syncDoneValueLength += 1 + 1 + 1;

        return 1 + TLV.getNbBytes( syncDoneValueLength ) + syncDoneValueLength;
    }


    /**
     * Encode the SyncDoneValue control
     * 
     * @param buffer The encoded sink
     * @return A ByteBuffer that contains the encoded PDU
     * @throws EncoderException If anything goes wrong while encoding.
     */
    @Override
    public ByteBuffer encode( ByteBuffer buffer ) throws EncoderException
    {
        ByteBuffer bb = ByteBuffer.allocate( computeLength() );
        bb.put( UniversalTag.SEQUENCE_TAG );
        bb.put( TLV.getBytes( syncDoneValueLength ) );

        if ( cookie != null )
        {
            Value.encode( bb, cookie );
        }

        Value.encode( bb, refreshDeletes );

        return bb;
    }


    /**
     * @return the cookie
     */
    public byte[] getCookie()
    {
        return cookie;
    }


    /**
     * @param cookie cookie to be set
     */
    public void setCookie( byte[] cookie )
    {
        this.cookie = cookie;
    }


    /**
     * @return true, if refreshDeletes flag is set, false otherwise
     */
    public boolean isRefreshDeletes()
    {
        return refreshDeletes;
    }


    /**
     * @param refreshDeletes set the refreshDeletes flag 
     */
    public void setRefreshDeletes( boolean refreshDeletes )
    {
        this.refreshDeletes = refreshDeletes;
    }


    /**
     * @see Object#toString()
     */
    public String toString()
    {
        StringBuilder sb = new StringBuilder();

        sb.append( "    SyncDoneValue control :\n" );
        sb.append( "        cookie            : '" ).append( StringTools.dumpBytes( cookie ) ).append( "'\n" );
        sb.append( "        refreshDeletes : '" ).append( refreshDeletes ).append( "'\n" );

        return sb.toString();
    }
}