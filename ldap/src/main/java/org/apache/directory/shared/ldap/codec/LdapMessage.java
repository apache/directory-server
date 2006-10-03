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
package org.apache.directory.shared.ldap.codec;


import org.apache.directory.shared.asn1.Asn1Object;
import org.apache.directory.shared.asn1.ber.tlv.TLV;
import org.apache.directory.shared.asn1.ber.tlv.UniversalTag;
import org.apache.directory.shared.asn1.ber.tlv.Value;
import org.apache.directory.shared.asn1.codec.EncoderException;
import org.apache.directory.shared.ldap.codec.abandon.AbandonRequest;
import org.apache.directory.shared.ldap.codec.add.AddRequest;
import org.apache.directory.shared.ldap.codec.add.AddResponse;
import org.apache.directory.shared.ldap.codec.bind.BindRequest;
import org.apache.directory.shared.ldap.codec.bind.BindResponse;
import org.apache.directory.shared.ldap.codec.compare.CompareRequest;
import org.apache.directory.shared.ldap.codec.compare.CompareResponse;
import org.apache.directory.shared.ldap.codec.del.DelRequest;
import org.apache.directory.shared.ldap.codec.del.DelResponse;
import org.apache.directory.shared.ldap.codec.extended.ExtendedRequest;
import org.apache.directory.shared.ldap.codec.extended.ExtendedResponse;
import org.apache.directory.shared.ldap.codec.modify.ModifyRequest;
import org.apache.directory.shared.ldap.codec.modify.ModifyResponse;
import org.apache.directory.shared.ldap.codec.modifyDn.ModifyDNRequest;
import org.apache.directory.shared.ldap.codec.modifyDn.ModifyDNResponse;
import org.apache.directory.shared.ldap.codec.search.SearchRequest;
import org.apache.directory.shared.ldap.codec.search.SearchResultDone;
import org.apache.directory.shared.ldap.codec.search.SearchResultEntry;
import org.apache.directory.shared.ldap.codec.search.SearchResultReference;
import org.apache.directory.shared.ldap.codec.unbind.UnBindRequest;

import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;

import java.util.ArrayList;
import java.util.Iterator;


/**
 * The main ldapObject : every Ldap Message are encapsulated in it. It contains
 * a message Id, a operation (protocolOp) and one ore more Controls.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class LdapMessage extends Asn1Object
{
    // ~ Instance fields
    // ----------------------------------------------------------------------------

    /** The message ID */
    private int messageId;

    /** The request or response being carried by the message */
    private Asn1Object protocolOp;

    /** The controls */
    private ArrayList controls;

    /** The current control */
    private transient Control currentControl;

    /** The LdapMessage length */
    private transient int ldapMessageLength;

    /** The controls length */
    private transient int controlsLength;

    /** The controls sequence length */
    private transient int controlsSequenceLength;


    // ~ Constructors
    // -------------------------------------------------------------------------------

    /**
     * Creates a new LdapMessage object.
     */
    public LdapMessage()
    {
        // We should not create this kind of object directly
    }


    // ~ Methods
    // ------------------------------------------------------------------------------------

    /**
     * Get the Control Object at a specific index
     * 
     * @param i The index of the Control Object to get
     * @return The selected Control Object
     */
    public Control getControls( int i )
    {
        return ( Control ) controls.get( i );
    }


    /**
     * Get the Control Objects
     * 
     * @return The Control Objects
     */
    public ArrayList getControls()
    {
        return controls;
    }


    /**
     * Get the current Control Object
     * 
     * @return The current Control Object
     */
    public Control getCurrentControl()
    {
        return currentControl;
    }


    /**
     * Add a control to the Controls array
     * 
     * @param control The Control to add
     */
    public void addControl( Control control )
    {
        currentControl = control;
        if ( this.controls == null )
        {
            this.controls = new ArrayList();
        }
        this.controls.add( control );
    }


    /**
     * Init the controls array
     */
    public void initControls()
    {
        controls = new ArrayList();
    }


    /**
     * Get the message ID
     * 
     * @return The message ID
     */
    public int getMessageId()
    {
        return messageId;
    }


    /**
     * Set the message ID
     * 
     * @param messageId The message ID
     */
    public void setMessageId( int messageId )
    {
        this.messageId = messageId;
    }


    /**
     * Get the message type
     * 
     * @return The message type
     */
    public int getMessageType()
    {
        return ( ( LdapMessage ) protocolOp ).getMessageType();
    }


    /**
     * Get the message type Name
     * 
     * @return The message type name
     */
    public String getMessageTypeName()
    {
        switch ( ( ( LdapMessage ) protocolOp ).getMessageType() )
        {
            case LdapConstants.ABANDON_REQUEST:
                return "ABANDON_REQUEST";
            case LdapConstants.ADD_REQUEST:
                return "ADD_REQUEST";
            case LdapConstants.ADD_RESPONSE:
                return "ADD_RESPONSE";
            case LdapConstants.BIND_REQUEST:
                return "BIND_REQUEST";
            case LdapConstants.BIND_RESPONSE:
                return "BIND_RESPONSE";
            case LdapConstants.COMPARE_REQUEST:
                return "COMPARE_REQUEST";
            case LdapConstants.COMPARE_RESPONSE:
                return "COMPARE_REQUEST";
            case LdapConstants.DEL_REQUEST:
                return "DEL_REQUEST";
            case LdapConstants.DEL_RESPONSE:
                return "DEL_RESPONSE";
            case LdapConstants.EXTENDED_REQUEST:
                return "EXTENDED_REQUEST";
            case LdapConstants.EXTENDED_RESPONSE:
                return "EXTENDED_RESPONSE";
            case LdapConstants.MODIFYDN_REQUEST:
                return "MODIFYDN_REQUEST";
            case LdapConstants.MODIFYDN_RESPONSE:
                return "MODIFYDN_RESPONSE";
            case LdapConstants.MODIFY_REQUEST:
                return "MODIFY_REQUEST";
            case LdapConstants.MODIFY_RESPONSE:
                return "MODIFY_RESPONSE";
            case LdapConstants.SEARCH_REQUEST:
                return "SEARCH_REQUEST";
            case LdapConstants.SEARCH_RESULT_DONE:
                return "SEARCH_RESULT_DONE";
            case LdapConstants.SEARCH_RESULT_ENTRY:
                return "SEARCH_RESULT_ENTRY";
            case LdapConstants.SEARCH_RESULT_REFERENCE:
                return "SEARCH_RESULT_REFERENCE";
            case LdapConstants.UNBIND_REQUEST:
                return "UNBIND_REQUEST";
            default:
                return "UNKNOWN";
        }
    }


    /**
     * Get the encapsulated Ldap response.
     * 
     * @return Returns the Ldap response.
     */
    public LdapResponse getLdapResponse()
    {
        return ( LdapResponse ) protocolOp;
    }


    /**
     * Get a AbandonRequest ldapObject, assuming that the caller knows that it
     * is the LdapMessage exact type.
     * 
     * @return Returns the AbandonRequest ldapObject.
     */
    public AbandonRequest getAbandonRequest()
    {
        return ( AbandonRequest ) protocolOp;
    }


    /**
     * Get a AddRequest ldapObject, assuming that the caller knows that it is
     * the LdapMessage exact type.
     * 
     * @return Returns the AddRequest ldapObject.
     */
    public AddRequest getAddRequest()
    {
        return ( AddRequest ) protocolOp;
    }


    /**
     * Get a AddResponse ldapObject, assuming that the caller knows that it is
     * the LdapMessage exact type.
     * 
     * @return Returns the AddResponse ldapObject.
     */
    public AddResponse getAddResponse()
    {
        return ( AddResponse ) protocolOp;
    }


    /**
     * Get a BindRequest ldapObject, assuming that the caller knows that it is
     * the LdapMessage exact type.
     * 
     * @return Returns the BindRequest ldapObject.
     */
    public BindRequest getBindRequest()
    {
        return ( BindRequest ) protocolOp;
    }


    /**
     * Get a BindResponse ldapObject, assuming that the caller knows that it is
     * the LdapMessage exact type.
     * 
     * @return Returns the BindResponse ldapObject.
     */
    public BindResponse getBindResponse()
    {
        return ( BindResponse ) protocolOp;
    }


    /**
     * Get a CompareRequest ldapObject, assuming that the caller knows that it
     * is the LdapMessage exact type.
     * 
     * @return Returns the CompareRequest ldapObject.
     */
    public CompareRequest getCompareRequest()
    {
        return ( CompareRequest ) protocolOp;
    }


    /**
     * Get a CompareResponse ldapObject, assuming that the caller knows that it
     * is the LdapMessage exact type.
     * 
     * @return Returns the CompareResponse ldapObject.
     */
    public CompareResponse getCompareResponse()
    {
        return ( CompareResponse ) protocolOp;
    }


    /**
     * Get a DelRequest ldapObject, assuming that the caller knows that it is
     * the LdapMessage exact type.
     * 
     * @return Returns the DelRequest ldapObject.
     */
    public DelRequest getDelRequest()
    {
        return ( DelRequest ) protocolOp;
    }


    /**
     * Get a DelResponse ldapObject, assuming that the caller knows that it is
     * the LdapMessage exact type.
     * 
     * @return Returns the DelResponse ldapObject.
     */
    public DelResponse getDelResponse()
    {
        return ( DelResponse ) protocolOp;
    }


    /**
     * Get a ExtendedRequest ldapObject, assuming that the caller knows that it
     * is the LdapMessage exact type.
     * 
     * @return Returns the ExtendedRequest ldapObject.
     */
    public ExtendedRequest getExtendedRequest()
    {
        return ( ExtendedRequest ) protocolOp;
    }


    /**
     * Get a ExtendedResponse ldapObject, assuming that the caller knows that it
     * is the LdapMessage exact type.
     * 
     * @return Returns the ExtendedResponse ldapObject.
     */
    public ExtendedResponse getExtendedResponse()
    {
        return ( ExtendedResponse ) protocolOp;
    }


    /**
     * Get a ModifyDNRequest ldapObject, assuming that the caller knows that it
     * is the LdapMessage exact type.
     * 
     * @return Returns the ModifyDNRequest ldapObject.
     */
    public ModifyDNRequest getModifyDNRequest()
    {
        return ( ModifyDNRequest ) protocolOp;
    }


    /**
     * Get a ModifyDNResponse ldapObject, assuming that the caller knows that it
     * is the LdapMessage exact type.
     * 
     * @return Returns the ModifyDNResponse ldapObject.
     */
    public ModifyDNResponse getModifyDNResponse()
    {
        return ( ModifyDNResponse ) protocolOp;
    }


    /**
     * Get a ModifyRequest ldapObject, assuming that the caller knows that it is
     * the LdapMessage exact type.
     * 
     * @return Returns the ModifyRequest ldapObject.
     */
    public ModifyRequest getModifyRequest()
    {
        return ( ModifyRequest ) protocolOp;
    }


    /**
     * Get a ModifyResponse ldapObject, assuming that the caller knows that it
     * is the LdapMessage exact type.
     * 
     * @return Returns the ModifyResponse ldapObject.
     */
    public ModifyResponse getModifyResponse()
    {
        return ( ModifyResponse ) protocolOp;
    }


    /**
     * Get a SearchRequest ldapObject, assuming that the caller knows that it is
     * the LdapMessage exact type.
     * 
     * @return Returns the SearchRequest ldapObject.
     */
    public SearchRequest getSearchRequest()
    {
        return ( SearchRequest ) protocolOp;
    }


    /**
     * Get a SearchResultDone ldapObject, assuming that the caller knows that it
     * is the LdapMessage exact type.
     * 
     * @return Returns the SearchRequestDone ldapObject.
     */
    public SearchResultDone getSearchResultDone()
    {
        return ( SearchResultDone ) protocolOp;
    }


    /**
     * Get a SearchResultEntry ldapObject, assuming that the caller knows that
     * it is the LdapMessage exact type.
     * 
     * @return Returns the SearchResultEntry ldapObject.
     */
    public SearchResultEntry getSearchResultEntry()
    {
        return ( SearchResultEntry ) protocolOp;
    }


    /**
     * Get a SearchResultReference ldapObject, assuming that the caller knows
     * that it is the LdapMessage exact type.
     * 
     * @return Returns the SearchResultReference ldapObject.
     */
    public SearchResultReference getSearchResultReference()
    {
        return ( SearchResultReference ) protocolOp;
    }


    /**
     * Get a UnBindRequest ldapObject, assuming that the caller knows that it is
     * the LdapMessage exact type.
     * 
     * @return Returns the UnBindRequest ldapObject.
     */
    public UnBindRequest getUnBindRequest()
    {
        return ( UnBindRequest ) protocolOp;
    }


    /**
     * Set the ProtocolOP
     * 
     * @param protocolOp The protocolOp to set.
     */
    public void setProtocolOP( Asn1Object protocolOp )
    {
        this.protocolOp = protocolOp;
    }


    /**
     * Compute the LdapMessage length LdapMessage : 
     * 0x30 L1 
     *   | 
     *   +--> 0x02 0x0(1-4) [0..2^31-1] (MessageId) 
     *   +--> protocolOp 
     *   [+--> Controls] 
     *   
     * MessageId length = Length(0x02) + length(MessageId) + MessageId.length 
     * L1 = length(ProtocolOp) 
     * LdapMessage length = Length(0x30) + Length(L1) + MessageId length + L1
     */
    public int computeLength()
    {
        // The length of the MessageId. It's the sum of
        // - the tag (0x02), 1 byte
        // - the length of the Id length, 1 byte
        // - the Id length, 1 to 4 bytes
        ldapMessageLength = 1 + 1 + Value.getNbBytes( messageId );

        // Get the protocolOp length
        int protocolOpLength = protocolOp.computeLength();

        // Add the protocol length to the message length
        ldapMessageLength += protocolOpLength;

        // Do the same thing for Controls, if any.
        if ( controls != null )
        {
            // Controls :
            // 0xA0 L3
            //   |
            //   +--> 0x30 L4
            //   +--> 0x30 L5
            //   +--> ...
            //   +--> 0x30 Li
            //   +--> ...
            //   +--> 0x30 Ln
            //
            // L3 = Length(0x30) + Length(L5) + L5
            // + Length(0x30) + Length(L6) + L6
            // + ...
            // + Length(0x30) + Length(Li) + Li
            // + ...
            // + Length(0x30) + Length(Ln) + Ln
            //
            // LdapMessageLength = LdapMessageLength + Length(0x90)
            // + Length(L3) + L3
            controlsSequenceLength = 0;

            Iterator controlIterator = controls.iterator();

            // We may have more than one control. ControlsLength is L4.
            while ( controlIterator.hasNext() )
            {
                controlsSequenceLength += ( ( Control ) controlIterator.next() ).computeLength();
            }

            // Computes the controls length
            controlsLength = controlsSequenceLength; // 1 + Length.getNbBytes(
                                                     // controlsSequenceLength
                                                     // ) + controlsSequenceLength;

            // Now, add the tag and the length of the controls length
            ldapMessageLength += 1 + TLV.getNbBytes( controlsSequenceLength ) + controlsSequenceLength;
        }

        // finally, calculate the global message size :
        // length(Tag) + Length(length) + length

        return 1 + ldapMessageLength + TLV.getNbBytes( ldapMessageLength );
    }


    /**
     * Generate the PDU which contains the encoded object. 
     * 
     * The generation is done in two phases : 
     * - first, we compute the length of each part and the
     * global PDU length 
     * - second, we produce the PDU. 
     * 
     * 0x30 L1 
     *   | 
     *   +--> 0x02 L2 MessageId  
     *   +--> ProtocolOp 
     *   +--> Controls 
     *   
     * L2 = Length(MessageId)
     * L1 = Length(0x02) + Length(L2) + L2 + Length(ProtocolOp) + Length(Controls)
     * LdapMessageLength = Length(0x30) + Length(L1) + L1
     * 
     * @param buffer The encoded PDU
     * @return A ByteBuffer that contaons the PDU
     * @throws EncoderException If anything goes wrong.
     */
    public ByteBuffer encode( ByteBuffer buffer ) throws EncoderException
    {
        // Allocate the bytes buffer.
        ByteBuffer bb = ByteBuffer.allocate( computeLength() );

        try
        {
            // The LdapMessage Sequence
            bb.put( UniversalTag.SEQUENCE_TAG );

            // The length has been calculated by the computeLength method
            bb.put( TLV.getBytes( ldapMessageLength ) );
        }
        catch ( BufferOverflowException boe )
        {
            throw new EncoderException( "The PDU buffer size is too small !" );
        }

        // The message Id
        Value.encode( bb, messageId );

        // Add the protocolOp part
        protocolOp.encode( bb );

        // Do the same thing for Controls, if any.
        if ( controls != null )
        {
            // Encode the controls
            bb.put( ( byte ) LdapConstants.CONTROLS_TAG );
            bb.put( TLV.getBytes( controlsLength ) );

            // Encode each control
            Iterator controlIterator = controls.iterator();

            while ( controlIterator.hasNext() )
            {
                ( ( Control ) controlIterator.next() ).encode( bb );
            }
        }

        return bb;
    }


    /**
     * Get a String representation of a LdapMessage
     * 
     * @return A LdapMessage String
     */
    public String toString()
    {
        StringBuffer sb = new StringBuffer();

        sb.append( "LdapMessage\n" );
        sb.append( "    message Id : " ).append( messageId ).append( '\n' );
        sb.append( protocolOp.toString() );

        if ( controls != null )
        {
            for ( int i = 0; i < controls.size(); i++ )
            {
                sb.append( ( ( Control ) controls.get( i ) ).toString() );
            }
        }

        return sb.toString();
    }
}
