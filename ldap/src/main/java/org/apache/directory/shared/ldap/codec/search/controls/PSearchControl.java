package org.apache.directory.shared.ldap.codec.search.controls;


import java.nio.ByteBuffer;

import org.apache.directory.shared.asn1.Asn1Object;
import org.apache.directory.shared.asn1.ber.tlv.Length;
import org.apache.directory.shared.asn1.ber.tlv.UniversalTag;
import org.apache.directory.shared.asn1.ber.tlv.Value;
import org.apache.directory.shared.asn1.codec.EncoderException;


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
     * to the client if they match the standard search cri- teria and if the
     * operation that caused the change is included in the changeTypes field.
     * The changeTypes field is the logical OR of one or more of these values:
     * add (1), delete (2), modify (4), modDN (8).
     */
    private int changeTypes;

    private transient int psearchSeqLength;


    /**
     * Compute the PSearchControl length 0x30 L1 | +--> 0x02 0x0(1-4)
     * [0..2^31-1] (changeTypes) +--> 0x01 0x01 [0x00 | 0xFF] (changeOnly) +-->
     * 0x01 0x01 [0x00 | 0xFF] (returnRCs)
     */
    public int computeLength()
    {
        int changeTypesLength = 1 + 1 + Value.getNbBytes( changeTypes );
        int changesOnlyLength = 1 + 1 + 1;
        int returnRCsLength = 1 + 1 + 1;

        psearchSeqLength = changeTypesLength + changesOnlyLength + returnRCsLength;

        return 1 + Length.getNbBytes( psearchSeqLength ) + psearchSeqLength;
    }


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
     * Encodes the persistent search control.
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
        bb.put( UniversalTag.SEQUENCE_TAG );
        bb.put( Length.getBytes( psearchSeqLength ) );

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
