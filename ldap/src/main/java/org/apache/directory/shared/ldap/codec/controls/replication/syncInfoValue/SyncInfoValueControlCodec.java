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
package org.apache.directory.shared.ldap.codec.controls.replication.syncInfoValue;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import org.apache.directory.shared.asn1.AbstractAsn1Object;
import org.apache.directory.shared.asn1.ber.tlv.TLV;
import org.apache.directory.shared.asn1.ber.tlv.UniversalTag;
import org.apache.directory.shared.asn1.ber.tlv.Value;
import org.apache.directory.shared.asn1.codec.EncoderException;
import org.apache.directory.shared.ldap.message.control.replication.SynchronizationInfoEnum;
import org.apache.directory.shared.ldap.util.StringTools;

/**
 * A syncInfoValue object, as defined in RFC 4533
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev:$, $Date: 
 */
public class SyncInfoValueControlCodec extends AbstractAsn1Object
{
    /** The kind of syncInfoValue we are dealing with */
    private SynchronizationInfoEnum type;
    
    /** The cookie */
    private byte[] cookie;
    
    /** The refreshDone flag if we are dealing with refreshXXX syncInfo. Default to true */
    private boolean refreshDone = true;
    
    /** The refreshDeletes flag if we are dealing with syncIdSet syncInfo. Defaluts to false */
    private boolean refreshDeletes = false;
    
    /** The list of UUIDs if we are dealing with syncIdSet syncInfo */
    private List<byte[]> syncUUIDs;
    
    /** The syncUUIDs cumulative lentgh */
    private int syncUUIDsLength;
    
    
    /**
     * The constructor for this codec.
     * @param type The kind of syncInfo we will store. Can be newCookie, 
     * refreshPresent, refreshDelete or syncIdSet
     */
    public SyncInfoValueControlCodec( SynchronizationInfoEnum type )
    {
        this.type = type;
        
        // Initialize the arrayList if needed
        if ( type == SynchronizationInfoEnum.SYNC_ID_SET )
        {
            syncUUIDs = new ArrayList<byte[]>();
        }
    }
    
    
    /** The global length for this control */
    private int syncInfoValueLength;

    /**
     * Get the control type.
     * 
     * @return the type : one of newCookie, refreshDelete, refreshPresent or syncIdSet
     */
    public SynchronizationInfoEnum getType()
    {
        return type;
    }

    
    /**
     * @param syncMode the syncMode to set
     */
    public void setType( SynchronizationInfoEnum type )
    {
        this.type = type;
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
     * @return the refreshDone
     */
    public boolean isRefreshDone()
    {
        return refreshDone;
    }


    /**
     * @param refreshDone the refreshDone to set
     */
    public void setRefreshDone( boolean refreshDone )
    {
        this.refreshDone = refreshDone;
    }


    /**
     * @return the refreshDeletes
     */
    public boolean isRefreshDeletes()
    {
        return refreshDeletes;
    }


    /**
     * @param refreshDeletes the refreshDeletes to set
     */
    public void setRefreshDeletes( boolean refreshDeletes )
    {
        this.refreshDeletes = refreshDeletes;
    }


    /**
     * @return the syncUUIDs
     */
    public List<byte[]> getSyncUUIDs()
    {
        return syncUUIDs;
    }


    /**
     * @param syncUUIDs the syncUUIDs to set
     */
    public void setSyncUUIDs( List<byte[]> syncUUIDs )
    {
        this.syncUUIDs = syncUUIDs;
    }

    
    /**
     * Compute the SyncInfoValue length.
     * 
     * SyncInfoValue :
     * 
     * 0xA0 L1 abcd                   // newCookie
     * 0xA1 L2                        // refreshDelete
     *   |
     *  [+--> 0x04 L3 abcd]           // cookie
     *  [+--> 0x01 0x01 (0x00|0xFF)   // refreshDone
     * 0xA2 L4                        // refreshPresent
     *   |
     *  [+--> 0x04 L5 abcd]           // cookie
     *  [+--> 0x01 0x01 (0x00|0xFF)   // refreshDone
     * 0xA3 L6                        // syncIdSet
     *   |
     *  [+--> 0x04 L7 abcd]           // cookie
     *  [+--> 0x01 0x01 (0x00|0xFF)   // refreshDeletes
     *   +--> 0x31 L8                 // SET OF syncUUIDs
     *          |
     *         [+--> 0x04 L9 abcd]    // syncUUID    public static final int AND_FILTER_TAG = 0xA0;

    public static final int OR_FILTER_TAG = 0xA1;

    public static final int NOT_FILTER_TAG = 0xA2;

    public static final int BIND_REQUEST_SASL_TAG = 0xA3;

     */
    public int computeLength()
    {
        // The mode length
        syncInfoValueLength = 0;
        
        switch ( type )
        {
            case NEW_COOKIE :
                if ( cookie != null )
                {
                    syncInfoValueLength = 1 + TLV.getNbBytes( cookie.length ) + cookie.length;
                }
                else
                {
                    syncInfoValueLength = 1 + 1;
                }
                
                return syncInfoValueLength;
                
            case REFRESH_DELETE :
            case REFRESH_PRESENT :
                if ( cookie != null )
                {
                    syncInfoValueLength = 1 + TLV.getNbBytes( cookie.length ) + cookie.length;
                }
                
                // The refreshDone flag
                syncInfoValueLength += 1 + 1 + 1;
                break;
                
            case SYNC_ID_SET :
                if ( cookie != null )
                {
                    syncInfoValueLength = 1 + TLV.getNbBytes( cookie.length ) + cookie.length;
                }
                
                // The refreshDeletes flag
                syncInfoValueLength += 1 + 1 + 1;

                // The syncUUIDs if any
                syncUUIDsLength = 0;

                if ( syncUUIDs.size() != 0 )
                {
                    for ( byte[] syncUUID:syncUUIDs )
                    {
                        int uuidLength = 1 + TLV.getNbBytes( syncUUID.length ) + syncUUID.length;
                        
                        syncUUIDsLength += 1 + TLV.getNbBytes( uuidLength ) + uuidLength;
                    }
                    
                    // Add the tag and compute the length
                    syncUUIDsLength += 1 + TLV.getNbBytes( syncUUIDsLength ) + syncUUIDsLength;
                }
                
                syncInfoValueLength += 1 + TLV.getNbBytes( syncUUIDsLength ) + syncUUIDsLength;

                break;
        }
        
        return 1 + TLV.getNbBytes( syncInfoValueLength ) + syncInfoValueLength;
    }
    
    
    /**
     * Encode the SyncInfoValue control
     * 
     * @param buffer The encoded sink
     * @return A ByteBuffer that contains the encoded PDU
     * @throws EncoderException If anything goes wrong.
     */
    public ByteBuffer encode( ByteBuffer bb ) throws EncoderException
    {
        // Allocate the bytes buffer.
        if ( bb == null )
        {
            bb = ByteBuffer.allocate( computeLength() );
        }
        
        switch ( type )
        {
            case NEW_COOKIE :
                // The first case : newCookie
                bb.put( (byte)SyncInfoValueTags.NEW_COOKIE_TAG.getValue() );

                // As the OCTET_STRING is absorbed by the Application tag,
                // we have to store the L and V separately
                if ( ( cookie == null ) || ( cookie.length == 0 ) )
                {
                    bb.put( ( byte ) 0 );
                }
                else
                {
                    bb.put( TLV.getBytes( cookie.length ) );
                    bb.put( cookie );
                }

                break;
                
            case REFRESH_DELETE :
                // The second case : refreshDelete
                bb.put( (byte)SyncInfoValueTags.REFRESH_DELETE_TAG.getValue() );
                bb.put( TLV.getBytes( syncInfoValueLength ) );

                // The cookie, if any
                if ( cookie != null )
                {
                    Value.encode( bb, cookie );
                }
                
                // The refreshDone flag
                Value.encode( bb, refreshDone );
                break;
                
            case REFRESH_PRESENT :
                // The third case : refreshPresent
                bb.put( (byte)SyncInfoValueTags.REFRESH_PRESENT_TAG.getValue() );
                bb.put( TLV.getBytes( syncInfoValueLength ) );

                // The cookie, if any
                if ( cookie != null )
                {
                    Value.encode( bb, cookie );
                }
                
                // The refreshDone flag
                Value.encode( bb, refreshDone );
                break;
                
            case SYNC_ID_SET :
                // The last case : syncIdSet
                bb.put( (byte)SyncInfoValueTags.SYNC_ID_SET_TAG.getValue() );
                bb.put( TLV.getBytes( syncInfoValueLength ) );

                // The cookie, if any
                if ( cookie != null )
                {
                    Value.encode( bb, cookie );
                }
                
                // The refreshDeletes flag
                Value.encode( bb, refreshDeletes );
                
                // The syncUUIDs
                bb.put( UniversalTag.SET_TAG );
                bb.put( TLV.getBytes( syncUUIDsLength ) );
                
                // Loop on the UUIDs if any
                if ( syncUUIDs.size() != 0 )
                {
                    for ( byte[] syncUUID:syncUUIDs )
                    {
                        Value.encode( bb , syncUUID );
                    }
                }
        }

        return bb;
    }
    
    
    /**
     * @see Object#toString()
     */
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        
        sb.append( "    SyncInfoValue control :\n" );
        
        switch ( type )
        {
            case NEW_COOKIE :
                sb.append( "        newCookie : '" ).
                    append( StringTools.dumpBytes( cookie ) ).append( "'\n" );
                break;
                
            case REFRESH_DELETE :
                sb.append( "        refreshDelete : \n" );
                
                if ( cookie != null )
                {
                    sb.append( "            cookie : '" ).
                        append( StringTools.dumpBytes( cookie ) ).append( "'\n" );
                }
                
                sb.append( "            refreshDone : " ).append(  refreshDone ).append( '\n' );
                break;
                
            case REFRESH_PRESENT :
                sb.append( "        refreshPresent : \n" );
                
                if ( cookie != null )
                {
                    sb.append( "            cookie : '" ).
                        append( StringTools.dumpBytes( cookie ) ).append( "'\n" );
                }
                
                sb.append( "            refreshDone : " ).append(  refreshDone ).append( '\n' );
                break;
                
            case SYNC_ID_SET :
                sb.append( "        syncIdSet : \n" );
                
                if ( cookie != null )
                {
                    sb.append( "            cookie : '" ).
                        append( StringTools.dumpBytes( cookie ) ).append( "'\n" );
                }
                
                sb.append( "            refreshDeletes : " ).append(  refreshDeletes ).append( '\n' );
                sb.append(  "            syncUUIDS : " );

                if ( syncUUIDs.size() != 0 )
                {
                    boolean isFirst = true;
                    
                    for ( byte[] syncUUID:syncUUIDs )
                    {
                        if ( isFirst )
                        {
                            isFirst = false;
                        }
                        else
                        {
                            sb.append( ", " );
                        }
                        
                        sb.append( syncUUID );
                    }
                    
                    sb.append( '\n' );
                }
                else
                {
                    sb.append(  "empty\n" );
                }
                
                break;
        }
        
        return sb.toString();
    }
}
