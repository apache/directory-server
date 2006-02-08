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
import org.apache.directory.shared.asn1.ber.tlv.Length;
import org.apache.directory.shared.asn1.ber.tlv.UniversalTag;
import org.apache.directory.shared.asn1.ber.tlv.Value;
import org.apache.directory.shared.asn1.codec.EncoderException;
import org.apache.directory.shared.asn1.util.Asn1StringUtils;
import org.apache.directory.shared.ldap.codec.util.LdapString;
import org.apache.directory.shared.ldap.codec.util.LdapStringEncodingException;


/**
 * A response control that may be returned by Persistent Search entry 
 * responses.  It contains addition change information to descrive the
 * exact change that occured to an entry.  The exact details of this 
 * control are covered in section 5 of this (yes) expired draft:
 * <a href="http://www3.ietf.org/proceedings/01aug/I-D/draft-ietf-ldapext-psearch-03.txt">
 * Persistent Search Draft v03</a> which is printed out below for 
 * convenience:
 * <pre>
 *   5.  Entry Change Notification Control
 *   
 *   This control provides additional information about the change the caused
 *   a particular entry to be returned as the result of a persistent search.
 *   The controlType is "2.16.840.1.113730.3.4.7".  If the client set the
 *   returnECs boolean to TRUE in the PersistentSearch control, servers MUST
 *   include an EntryChangeNotification control in the Controls portion of
 *   each SearchResultEntry that is returned due to an entry being added,
 *   deleted, or modified.
 *   
 *              EntryChangeNotification ::= SEQUENCE 
 *              {
 *                        changeType ENUMERATED 
 *                        {
 *                                add             (1),
 *                                delete          (2),
 *                                modify          (4),
 *                                modDN           (8)
 *                        },
 *                        previousDN   LDAPDN OPTIONAL,     -- modifyDN ops. only
 *                        changeNumber INTEGER OPTIONAL     -- if supported
 *              }
 *   
 *   changeType indicates what LDAP operation caused the entry to be
 *   returned.
 *   
 *   previousDN is present only for modifyDN operations and gives the DN of
 *   the entry before it was renamed and/or moved.  Servers MUST include this
 *   optional field only when returning change notifications as a result of
 *   modifyDN operations.
 *
 *   changeNumber is the change number [CHANGELOG] assigned by a server for
 *   the change.  If a server supports an LDAP Change Log it SHOULD include
 *   this field.
 *   </pre>
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class EntryChangeControl extends Asn1Object
{
    public static final int UNDEFINED_CHANGE_NUMBER = -1;
    
    private ChangeType changeType = ChangeType.ADD;
    private int changeNumber = UNDEFINED_CHANGE_NUMBER;
    private LdapString previousDn = null;
    

    private transient int eccSeqLength;

    /**
     * Compute the EnryChangeControl length
     * 0x30 L1
     *  |
     *  +--> 0x0A 0x0(1-4) [1|2|4|8] (changeType)
     *  +--> 0x04 L2 previousDN
     *  +--> 0x02 0x0(1-4) [0..2^31-1] (changeNumber)
     */
    public int computeLength()
    {
        int changeTypesLength = 1 + 1 + 1;

        int previousDnLength = 0;
        int changeNumberLength = 0;
        
        if ( previousDn != null ) 
        {
            previousDnLength = 1 + Length.getNbBytes( previousDn.getNbBytes() ) + previousDn.getNbBytes();
        }

        if ( changeNumber != UNDEFINED_CHANGE_NUMBER ) 
        {
            changeNumberLength = 1 + 1 + Value.getNbBytes( changeNumber );
        }

        eccSeqLength = changeTypesLength + previousDnLength + changeNumberLength;
        
        return  1 + Length.getNbBytes( eccSeqLength ) + eccSeqLength;
    }

    /**
     * Encodes the entry change control.
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
        bb.put( Length.getBytes( eccSeqLength ) );

        bb.put( UniversalTag.ENUMERATED_TAG );
        bb.put( (byte)1 );
        bb.put( Value.getBytes( changeType.getValue() ) );
        
        if ( previousDn != null )
        {
            Value.encode( bb, previousDn.getBytes() );
        }
        if ( changeNumber != UNDEFINED_CHANGE_NUMBER )
        {
            Value.encode( bb, changeNumber );
        }
        return bb;
    }


    /**
     * Return a String representing this EntryChangeControl.
     */
    public String toString()
    {
        StringBuffer sb = new StringBuffer();
        
        sb.append( "    Entry Change Control\n" );
        sb.append( "        changeType   : '" ).append( changeType ).append("'\n");
        sb.append( "        previousDN   : '" ).append( previousDn ).append( "'\n");
        if ( changeNumber == UNDEFINED_CHANGE_NUMBER )
        {
            sb.append( "        changeNumber : '" ).append( "UNDEFINED" ).append( "'\n");
        }
        else
        {
            sb.append( "        changeNumber : '" ).append( changeNumber ).append( "'\n");
        }
        return sb.toString();
    }

    
    public ChangeType getChangeType()
    {
        return changeType;
    }
    

    public void setChangeType( ChangeType changeType )
    {
        this.changeType = changeType;
    }

    
    public String getPreviousDn()
    {
    	return previousDn == null ? "" : previousDn.getString();
    }
    
    
    public void setPreviousDn( String previousDn )
    {
        try
        {
            this.previousDn = new LdapString( Asn1StringUtils.getBytesUtf8( previousDn ) );
        }
        catch ( LdapStringEncodingException e )
        {
            e.printStackTrace();
        }
    }
    
    
    public void setPreviousDn( LdapString previousDn )
    {
        this.previousDn = previousDn;
    }
    
    
    public int getChangeNumber()
    {
        return changeNumber;
    }
    
    
    public void setChangeNumber( int changeNumber )
    {
        this.changeNumber = changeNumber;
    }
}
