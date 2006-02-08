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
package org.apache.directory.shared.ldap.codec.modifyDn;

import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;

import javax.naming.Name;

import org.apache.directory.shared.asn1.ber.tlv.Length;
import org.apache.directory.shared.asn1.ber.tlv.Value;
import org.apache.directory.shared.asn1.codec.EncoderException;
import org.apache.directory.shared.ldap.codec.LdapConstants;
import org.apache.directory.shared.ldap.codec.LdapMessage;
import org.apache.directory.shared.ldap.name.LdapDN;
import org.apache.directory.shared.ldap.name.Rdn;
import org.apache.directory.shared.ldap.util.StringTools;


/**
 * A ModifyDNRequest Message. Its syntax is :
 * ModifyDNRequest ::= [APPLICATION 12] SEQUENCE {
 *                 entry           LDAPDN,
 *                 newrdn          RelativeLDAPDN,
 *                 deleteoldrdn    BOOLEAN,
 *                 newSuperior     [0] LDAPDN OPTIONAL }
 *  
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class ModifyDNRequest extends LdapMessage
{
    //~ Instance fields ----------------------------------------------------------------------------

    /** The DN to be modified. */
    private Name entry;

    /** The new RDN to be added to the RDN or to the new superior, if present */
    private Rdn newRDN;

    /** If the previous RDN is to be deleted, this flag will be set to true */
    private boolean deleteOldRDN;

    /** The optional superior, which will be concatened to the newRdn */
    private Name newSuperior;

    /** The modify DN request length */
    private transient int modifyDNRequestLength;

    //~ Constructors -------------------------------------------------------------------------------

    /**
     * Creates a new ModifyDNRequest object.
     */
    public ModifyDNRequest()
    {
        super( );
    }

    //~ Methods ------------------------------------------------------------------------------------

    /**
     * Get the message type
     *
     * @return Returns the type.
     */
    public int getMessageType()
    {
        return LdapConstants.MODIFYDN_REQUEST;
    }

    /**
     * Get the modification's DN
     * @return Returns the entry.
     */
    public String getEntry()
    {
        return ( ( entry == null ) ? "" : entry.toString() );
    }

    /**
     * Set the modification DN.
     * @param entry The entry to set.
     */
    public void setEntry( Name entry )
    {
        this.entry = entry;
    }

    /**
     * Tells if the old RDN is to be deleted
     *
     * @return Returns the deleteOldRDN.
     */
    public boolean isDeleteOldRDN()
    {
        return deleteOldRDN;
    }

    /**
     * Set the flag to delete the old RDN
     *
     * @param deleteOldRDN The deleteOldRDN to set.
     */
    public void setDeleteOldRDN( boolean deleteOldRDN )
    {
        this.deleteOldRDN = deleteOldRDN;
    }

    /**
     * Get the new RDN
     *
     * @return Returns the newRDN.
     */
    public String getNewRDN()
    {
        return ( ( newRDN == null ) ? "" : newRDN.toString() );
    }

    /**
     * Set the new RDN
     *
     * @param newRDN The newRDN to set.
     */
    public void setNewRDN( Rdn newRDN )
    {
        this.newRDN = newRDN;
    }

    /**
     * Get the newSuperior
     *
     * @return Returns the newSuperior.
     */
    public String getNewSuperior()
    {
        return ( ( newSuperior == null ) ? "" : newSuperior.toString() );
    }

    /**
     * Set the new superior
     *
     * @param newSuperior The newSuperior to set.
     */
    public void setNewSuperior( Name newSuperior )
    {
        this.newSuperior = newSuperior;
    }

    /**
     * Compute the ModifyDNRequest length
     * 
     * ModifyDNRequest :
     * 
     * 0x6C L1
     *  |
     *  +--> 0x04 L2 entry
     *  +--> 0x04 L3 newRDN
     *  +--> 0x01 0x01 (true/false) deleteOldRDN (3 bytes)
     * [+--> 0x80 L4 newSuperior ] 
     * 
     * L2 = Length(0x04) + Length(Length(entry)) + Length(entry) 
     * L3 = Length(0x04) + Length(Length(newRDN)) + Length(newRDN) 
     * L4 = Length(0x80) + Length(Length(newSuperior)) + Length(newSuperior)
     * L1 = L2 + L3 + 3 [+ L4] 
     * 
     * Length(ModifyDNRequest) = Length(0x6C) + Length(L1) + L1
     * @return DOCUMENT ME!
    */
    public int computeLength()
    {
    	int newRdnlength = StringTools.getBytesUtf8( newRDN.toString() ).length;
        modifyDNRequestLength =
            1 + Length.getNbBytes( LdapDN.getNbBytes( entry ) ) + LdapDN.getNbBytes( entry ) +
            1 + Length.getNbBytes( newRdnlength ) + newRdnlength +
            1 + 1 + 1; // deleteOldRDN

        if ( newSuperior != null )
        {
            modifyDNRequestLength += 1 + Length.getNbBytes( LdapDN.getNbBytes( newSuperior ) ) +
            	LdapDN.getNbBytes( newSuperior );
        }

        return 1 + Length.getNbBytes( modifyDNRequestLength ) + modifyDNRequestLength;
    }

    /**
     * Encode the ModifyDNRequest message to a PDU.
     * 
     * ModifyDNRequest :
     * 
     * 0x6C LL
     *   0x04 LL entry
     *   0x04 LL newRDN
     *   0x01 0x01 deleteOldRDN
     *   [0x80 LL newSuperior]
     * 
     * @param buffer The buffer where to put the PDU
     * @return The PDU.
     */
    public ByteBuffer encode( ByteBuffer buffer ) throws EncoderException
    {
        if ( buffer == null )
        {
            throw new EncoderException( "Cannot put a PDU in a null buffer !" );
        }

        try
        {
            // The ModifyDNRequest Tag
            buffer.put( LdapConstants.MODIFY_DN_REQUEST_TAG );
            buffer.put( Length.getBytes( modifyDNRequestLength ) ) ;

            // The entry
            
            Value.encode( buffer, LdapDN.getBytes( entry ) );

            // The newRDN
            Value.encode( buffer, newRDN.toString() );

            // The flag deleteOldRdn
            Value.encode( buffer, deleteOldRDN );

            // The new superior, if any
            if ( newSuperior != null )
            {
                // Encode the reference
                buffer.put( (byte) LdapConstants.MODIFY_DN_REQUEST_NEW_SUPERIOR_TAG );
                
                int newSuperiorLength = LdapDN.getNbBytes( newSuperior ); 
                
                buffer.put( Length.getBytes( newSuperiorLength ) );

                if ( newSuperiorLength != 0 )
                {
                    buffer.put( LdapDN.getBytes( newSuperior ) );
                }
            }
        }
        catch ( BufferOverflowException boe )
        {
            throw new EncoderException("The PDU buffer size is too small !");
        }

        return buffer;
    }

    /**
     * Get a String representation of a ModifyDNRequest
     *
     * @return A ModifyDNRequest String 
     */
    public String toString()
    {

        StringBuffer sb = new StringBuffer();

        sb.append( "    ModifyDN Response\n" );
        sb.append( "        Entry : '" ).append( entry ).append( "'\n" );
        sb.append( "        New RDN : '" ).append( newRDN.toString() ).append( "'\n" );
        sb.append( "        Delete old RDN : " ).append( deleteOldRDN ).append( "\n" );

        if ( newSuperior != null )
        {
            sb.append( "        New superior : '" ).append( newSuperior.toString() ).append(
                "'\n" );
        }

        return sb.toString();
    }
}
