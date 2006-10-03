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

import org.apache.directory.shared.asn1.Asn1Object;
import org.apache.directory.shared.asn1.ber.tlv.TLV;
import org.apache.directory.shared.asn1.ber.tlv.UniversalTag;
import org.apache.directory.shared.asn1.ber.tlv.Value;
import org.apache.directory.shared.asn1.codec.EncoderException;


/**
 * A persistence search object
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class PSearchControl extends Asn1Object
{
    /**
     * If changesOnly is TRUE, the server MUST NOT return any existing entries
     * that match the search criteria. Entries are only returned when they are
     * changed (added, modified, deleted, or subject to a modifyDN operation).
     */
    private boolean changesOnly;

    /**
     * If returnECs is TRUE, the server MUST return an Entry Change Notification
     * control with each entry returned as the result of changes.
     */
    private boolean returnECs;

    /**
     * As changes are made to the server, the effected entries MUST be returned
     * to the client if they match the standard search criteria and if the
     * operation that caused the change is included in the changeTypes field.
     * The changeTypes field is the logical OR of one or more of these values:
     * add    (1), 
     * delete (2), 
     * modify (4), 
     * modDN  (8).
     */
    private int changeTypes;
    
    /** Definition of the change types */
    public static final int CHANGE_TYPE_ADD     = 1;
    public static final int CHANGE_TYPE_DELETE  = 2;
    public static final int CHANGE_TYPE_MODIFY  = 4;
    public static final int CHANGE_TYPE_MODDN   = 8;
    
    /** Min and Max values for the possible combined change types */
    public static final int CHANGE_TYPES_MIN = CHANGE_TYPE_ADD;
    public static final int CHANGE_TYPES_MAX = CHANGE_TYPE_ADD | CHANGE_TYPE_DELETE | CHANGE_TYPE_MODIFY | CHANGE_TYPE_MODDN;

    /** A temporary storage for a psearch length */
    private transient int psearchSeqLength;


    public void setChangesOnly( boolean changesOnly )
    {
        this.changesOnly = changesOnly;
    }


    public boolean isChangesOnly()
    {
        return changesOnly;
    }


    public void setReturnECs( boolean returnECs )
    {
        this.returnECs = returnECs;
    }


    public boolean isReturnECs()
    {
        return returnECs;
    }


    public void setChangeTypes( int changeTypes )
    {
        this.changeTypes = changeTypes;
    }


    public int getChangeTypes()
    {
        return changeTypes;
    }

    /**
     * Compute the PSearchControl length 
     * 0x30 L1 
     *   | 
     *   +--> 0x02 0x0(1-4) [0..2^31-1] (changeTypes) 
     *   +--> 0x01 0x01 [0x00 | 0xFF] (changeOnly) 
     *   +--> 0x01 0x01 [0x00 | 0xFF] (returnRCs)
     */
    public int computeLength()
    {
        int changeTypesLength = 1 + 1 + Value.getNbBytes( changeTypes );
        int changesOnlyLength = 1 + 1 + 1;
        int returnRCsLength = 1 + 1 + 1;

        psearchSeqLength = changeTypesLength + changesOnlyLength + returnRCsLength;

        return 1 + TLV.getNbBytes( psearchSeqLength ) + psearchSeqLength;
    }


    /**
     * Encodes the persistent search control.
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
        bb.put( TLV.getBytes( psearchSeqLength ) );

        Value.encode( bb, changeTypes );
        Value.encode( bb, changesOnly );
        Value.encode( bb, returnECs );
        return bb;
    }


    /**
     * Return a String representing this PSearchControl.
     */
    public String toString()
    {
        StringBuffer sb = new StringBuffer();

        sb.append( "    Persistant Search Control\n" );
        sb.append( "        changeTypes : '" ).append( changeTypes ).append( "'\n" );
        sb.append( "        changesOnly : '" ).append( changesOnly ).append( "'\n" );
        sb.append( "        returnECs   : '" ).append( returnECs ).append( "'\n" );

        return sb.toString();
    }
}
