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
package org.apache.directory.shared.ldap.codec.controls.replication.syncRequestValue;

import java.nio.ByteBuffer;

import org.apache.directory.shared.asn1.AbstractAsn1Object;
import org.apache.directory.shared.asn1.ber.tlv.TLV;
import org.apache.directory.shared.asn1.ber.tlv.UniversalTag;
import org.apache.directory.shared.asn1.ber.tlv.Value;
import org.apache.directory.shared.asn1.codec.EncoderException;
import org.apache.directory.shared.ldap.message.control.replication.SynchronizationModeEnum;
import org.apache.directory.shared.ldap.util.StringTools;

/**
 * A syncRequestValue object, as defined in RFC 4533
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev:$, $Date: 
 */
public class SyncRequestValueControlCodec extends AbstractAsn1Object
{
    /** The synchronization type */
    private SynchronizationModeEnum mode;
    
    /** The Sync cookie */
    private byte[] cookie;
    
    /** The reloadHint flag */
    private boolean reloadHint;
    
    /** The global length for this control */
    private int syncRequestValueLength;

    /**
     * @return the mode
     */
    public SynchronizationModeEnum getMode()
    {
        return mode;
    }

    
    /**
     * @param syncMode the syncMode to set
     */
    public void setMode( SynchronizationModeEnum mode )
    {
        this.mode = mode;
    }

    
    /**
     * @return the cookie
     */
    public byte[] getCookie()
    {
        return cookie;
    }

    
    /**
     * @param cookie the cookie to set
     */
    public void setCookie( byte[] cookie )
    {
        this.cookie = cookie;
    }

    
    /**
     * @return the reloadHint
     */
    public boolean isReloadHint()
    {
        return reloadHint;
    }

    
    /**
     * @param reloadHint the reloadHint to set
     */
    public void setReloadHint( boolean reloadHint )
    {
        this.reloadHint = reloadHint;
    }

    /**
     * Compute the SyncRequestValue length.
     * 
     * SyncRequestValue :
     * 0x30 L1
     *  | 
     *  +--> 0x0A 0x01 [0x00|0x01|0x02|0x03] (mode)
     * [+--> 0x04 L2 abcd...                 (cookie)
     *  +--> 0x01 0x01 [0x00|0xFF]           (reloadHint)
     *   
     */
    public int computeLength()
    {
        // The mode length
        syncRequestValueLength = 1 + 1 + 1;
        
        // The cookie length, if we have a cookie
        if ( cookie != null )
        {
            syncRequestValueLength += 1 + TLV.getNbBytes( cookie.length ) + cookie.length;
        }
        
        // The reloadHint length
        syncRequestValueLength += 1 + 1 + 1;

        return 1 + TLV.getNbBytes( syncRequestValueLength ) + syncRequestValueLength;
    }
    
    
    /**
     * Encode the SyncRequestValue control
     * 
     * @param buffer The encoded sink
     * @return A ByteBuffer that contains the encoded PDU
     * @throws EncoderException If anything goes wrong.
     */
    public ByteBuffer encode( ByteBuffer buffer ) throws EncoderException
    {
        // Allocate the bytes buffer.
        ByteBuffer bb = ByteBuffer.allocate( computeLength() );
        bb.put( UniversalTag.SEQUENCE_TAG );
        bb.put( TLV.getBytes( syncRequestValueLength ) );

        Value.encode( bb, mode.getValue() );
        
        if ( cookie != null )
        {
            Value.encode( bb, cookie );
        }
        
        Value.encode( bb, reloadHint );
        
        return bb;
    }
    
    
    /**
     * @see Object#toString()
     */
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        
        sb.append( "    SyncRequestValue control :\n" );
        sb.append( "        mode              : '" ).append( mode ).append( "'\n" );
        sb.append( "        cookie            : '" ).
            append( StringTools.dumpBytes( cookie ) ).append( "'\n" );
        sb.append( "        refreshAndPersist : '" ).append( reloadHint ).append( "'\n" );

        return sb.toString();
    }
}
