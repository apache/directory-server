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
package org.apache.directory.shared.asn1.ber;


import org.apache.directory.shared.asn1.ber.grammar.IStates;
import org.apache.directory.shared.asn1.ber.tlv.ITLVBerDecoderMBean;
import org.apache.directory.shared.asn1.ber.tlv.TLV;
import org.apache.directory.shared.asn1.ber.tlv.TLVStateEnum;
import org.apache.directory.shared.asn1.ber.tlv.Value;
import org.apache.directory.shared.asn1.codec.DecoderException;
import org.apache.directory.shared.asn1.util.Asn1StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;

import javax.naming.NamingException;


/**
 * A BER TLV Tag component decoder. This decoder instanciate a Tag. The tag
 * won't be implementations should not copy the handle to the Tag object
 * delivered but should copy the data if they need it over the long term.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class Asn1Decoder implements ITLVBerDecoderMBean
{
    // ~ Static fields/initializers
    // -----------------------------------------------------------------

    /** The logger */
    private static final Logger log = LoggerFactory.getLogger( Asn1Decoder.class );
    
    /** A speedup for logger */
    private static final boolean IS_DEBUG = log.isDebugEnabled();
    
    /** This flag is used to indicate that there are more bytes in the stream */
    private static final boolean MORE = true;

    /** This flag is used to indicate that there are no more bytes in the stream */
    private static final boolean END = false;

    // ~ Instance fields
    // ----------------------------------------------------------------------------

    /** Flag that is used to allow/disallow the indefinite form of Length */
    private boolean indefiniteLengthAllowed;

    /** The maximum number of bytes that could be used to encode the Length */
    private int maxLengthLength;

    /** The maximum number of bytes that could be used to encode the Tag */
    private int maxTagLength;


    // ~ Constructors
    // -------------------------------------------------------------------------------

    /**
     * A public constructor of an Asn1 Decoder.
     */
    public Asn1Decoder()
    {
        indefiniteLengthAllowed = false;
        maxLengthLength = 1;
        maxTagLength = 1;
    }


    // ~ Methods
    // ------------------------------------------------------------------------------------

    /**
     * Treat the start of a TLV. It reads the tag and get its value.
     * 
     * @param stream The ByteBuffer containing the PDU to decode
     * @param container The container that stores the current state, 
     * the result and other informations.
     * @return <code>true</code> if there are more bytes to read, <code>false 
     * </code> otherwise
     * @throws DecoderException If something went wrong.
     */
    private boolean treatTagStartState( ByteBuffer stream, IAsn1Container container )
    {

        if ( stream.hasRemaining() )
        {

            byte octet = stream.get();

            TLV tlv = new TLV();
            tlv.setTag( octet );

            // Store the current TLV in the container.
            container.setCurrentTLV( tlv );

            // Create a link between the current TLV with its parent
            tlv.setParent( container.getParentTLV() );

            // Switch to the next state, which is the Length decoding
            container.setState( TLVStateEnum.LENGTH_STATE_START );

            if ( IS_DEBUG )
            {
                byte tag = container.getCurrentTLV().getTag();
                log.debug( "Tag {} has been decoded", Asn1StringUtils.dumpByte( tag ) );
            }

            return MORE;
        }
        else
        {

            // The stream has been exhausted
            return END;
        }
    }

    /**
     * Dump the current TLV tree
     * 
     * @param container The container
     */
    private void dumpTLVTree( IAsn1Container container )
    {
        StringBuffer sb = new StringBuffer();
        TLV current = container.getCurrentTLV();

        sb.append( "TLV" ).append( Asn1StringUtils.dumpByte( current.getTag() ) ).append( "(" )
            .append( current.getExpectedLength() ).append( ")" );

        current = current.getParent();

        while ( current != null )
        {
            sb.append( "-TLV" ).append( Asn1StringUtils.dumpByte( current.getTag() ) ).append( "(" )
                .append( current.getExpectedLength() ).append( ")" );
            current = current.getParent();
        }

        if ( IS_DEBUG ) 
        {
            log.debug( "TLV Tree : {}", sb.toString() );
        }
    }


    /**
     * Check if the TLV tree is fully decoded
     * 
     * @param container The container
     */
    private boolean isTLVDecoded( IAsn1Container container )
    {
        TLV current = container.getCurrentTLV();

        TLV parent = current.getParent();

        while ( parent != null )
        {
            if ( parent.getExpectedLength() != 0 )
            {
                return false;
            }

            parent = parent.getParent();
        }

        Value value = current.getValue();

        if ( ( value != null ) && ( value.getData() != null ) )
        {
            return ( current.getExpectedLength() == value.getData().length );
        }
        else
        {
            return current.getExpectedLength() == 0;
        }
    }

    /**
     * Treat the Length start. The tag has been decoded, so we have to deal with
     * the LENGTH, which can be multi-bytes.
     * 
     * @param stream  The ByteBuffer containing the PDU to decode
     * @param container The container that stores the current state, 
     * the result and other informations.
     * @return <code>true</code> if there are more bytes to read, <code>false 
     * </code> otherwise
     * @throws DecoderException Thrown if anything went wrong
     */
    private boolean treatLengthStartState( ByteBuffer stream, IAsn1Container container ) throws DecoderException
    {

        if ( stream.hasRemaining() )
        {
            byte octet = stream.get();
            TLV tlv = container.getCurrentTLV();

            if ( ( octet & TLV.LENGTH_LONG_FORM ) == 0 )
            {

                // We don't have a long form. The Length of the Value part is
                // given by this byte.
                tlv.setLength( octet );
                tlv.setLengthNbBytes(  1 );

                container.setState( TLVStateEnum.LENGTH_STATE_END );
            }
            else if ( ( octet & TLV.LENGTH_EXTENSION_RESERVED ) != TLV.LENGTH_EXTENSION_RESERVED )
            {

                int expectedLength = octet & TLV.LENGTH_SHORT_MASK;

                if ( expectedLength > 4 )
                {
                    log.error( "Overflow : can't have more than 4 bytes long length" );
                    throw new DecoderException( "Overflow : can't have more than 4 bytes long length" );
                }

                tlv.setLength( 0 );
                tlv.setLengthNbBytes( 1 + expectedLength );
                tlv.setLengthBytesRead( 1 );
                container.setState( TLVStateEnum.LENGTH_STATE_PENDING );
            }
            else
            {
                log.error( "Length reserved extension used" );
                throw new DecoderException( "Length reserved extension used" );
            }

            return MORE;
        }
        else
        {
            return END;
        }
    }


    /**
     * This function is called when a Length is in the process of being decoded,
     * but the lack of bytes in the buffer stopped the process.
     * 
     * @param stream The ByteBuffer containing the PDU to decode
     * @param container The container that stores the current state, 
     * the result and other informations.
     * @return <code>true</code> if there are more bytes to read, <code>false 
     * </code> otherwise
     * @throws DecoderException Thrown if anything went wrong
     */
    private boolean treatLengthPendingState( ByteBuffer stream, IAsn1Container container )
    {

        if ( stream.hasRemaining() )
        {

            TLV tlv = container.getCurrentTLV();
            int length = tlv.getLength();

            while ( tlv.getLengthBytesRead() < tlv.getLengthNbBytes() )
            {

                byte octet = stream.get();

                if ( IS_DEBUG )
                {
                    log.debug( "  current byte : {}", Asn1StringUtils.dumpByte( octet ) );
                }

                tlv.incLengthBytesRead();
                length = ( length << 8 ) | ( octet & 0x00FF );
                
                if ( stream.hasRemaining() == false )
                {
                    tlv.setLength( length );
                    
                    if ( tlv.getLengthBytesRead() < tlv.getLengthNbBytes() )
                    {
                        container.setState( TLVStateEnum.LENGTH_STATE_PENDING );
                    }
                    else
                    {
                        container.setState( TLVStateEnum.LENGTH_STATE_END );
                    }
                    
                    return END;
                }
            }

            tlv.setLength( length );
            container.setState( TLVStateEnum.LENGTH_STATE_END );

            return MORE;
        }
        else
        {

            return END;
        }
    }


    /**
     * A debug function used to dump the expected length stack.
     * 
     * @param tlv The current TLV.
     * @return A string which represent the expected length stack.
     */
    private String getParentLength( TLV tlv )
    {
        StringBuffer buffer = new StringBuffer();

        buffer.append( "TLV expected length stack : " );

        while ( true )
        {
            if ( tlv == null )
            {
                buffer.append( " - null" );
                break;
            }
            else
            {
                buffer.append( " - " ).append( tlv.getExpectedLength() );
            }

            tlv = tlv.getParent();
        }

        return buffer.toString();
    }


    /**
     * The Length is fully decoded. We have to call an action to check the size.
     * 
     * @param container The container that stores the current state, 
     * the result and other informations.
     * @throws DecoderException Thrown if anything went wrong
     */
    private void treatLengthEndState( IAsn1Container container ) throws DecoderException
    {
        TLV tlv = container.getCurrentTLV();
        int length = tlv.getLength();

        // We will check the length here. What we must control is
        // that the enclosing constructed TLV expected length is not
        // exceeded by the current TLV.
        TLV parentTLV = container.getParentTLV();

        if ( IS_DEBUG )
        {
            log.debug( "Parent length : {}", getParentLength( parentTLV ) );
        }

        if ( parentTLV == null )
        {
            // This is the first TLV, so we can't check anything. We will
            // just store this TLV as the root of the PDU
            tlv.setExpectedLength( length );
            container.setParentTLV( tlv );

            if ( IS_DEBUG )
            {
                if  ( tlv != null )
                {
                    log.debug( "Root TLV[{}]", Integer.valueOf( length ) );
                }
                else
                {
                    log.debug( "Root TLV[ null ]");
                }
            }
        }
        else
        {
            // We have a parent, so we will check that its expected length is
            // not exceeded.
            int expectedLength = parentTLV.getExpectedLength();
            int currentLength = tlv.getSize();

            if ( expectedLength < currentLength )
            {
                // The expected length is lower than the Value length of the
                // current TLV. This is an error...
                log.error( "tlv[{}, {}]", Integer.valueOf( expectedLength ), Integer.valueOf( currentLength ) );
                throw new DecoderException( "The current Value length is above the expected length" );
            }

            // deal with the particular case where expected length equal
            // the current length, which means that the parentTLV has been
            // completed.
            if ( expectedLength == currentLength )
            {
                parentTLV.setExpectedLength( 0 );

                // We also have to check that the current TLV is a constructed
                // one.
                // In this case, we have to switch from this parent TLV
                // to the parent's parent TLV.
                if ( tlv.isConstructed() )
                {
                    // here, we also have another special case : a
                    // zero length TLV. We must then unstack all
                    // the parents which length is null.
                    if ( length == 0 )
                    {
                        // We will set the parent to the first parentTLV which
                        // expectedLength
                        // is not null, and it will become the new parent TLV
                        while ( parentTLV != null )
                        {
                            if ( parentTLV.getExpectedLength() != 0 )
                            {
                                // ok, we have an incomplete parent. we will
                                // stop the recursion right here
                                break;
                            }
                            else
                            {
                                parentTLV = parentTLV.getParent();
                            }
                        }

                        container.setParentTLV( parentTLV );
                    }
                    else
                    {
                        // The new Parent TLV is this Constructed TLV
                        container.setParentTLV( tlv );
                    }

                    tlv.setParent( parentTLV );
                    tlv.setExpectedLength( length );
                }
                else
                {
                    tlv.setExpectedLength( length );
                    
                    // It's over, the parent TLV has been completed.
                    // Go back to the parent's parent TLV until we find
                    // a tlv which is not complete.
                    while ( parentTLV != null )
                    {
                        if ( parentTLV.getExpectedLength() != 0 )
                        {
                            // ok, we have an incomplete parent. we will
                            // stop the recursion right here
                            break;
                        }
                        else
                        {
                            parentTLV = parentTLV.getParent();
                        }
                    }

                    container.setParentTLV( parentTLV );
                }
            }
            else
            {
                // Renew the expected Length.
                parentTLV.setExpectedLength( expectedLength - currentLength );
                tlv.setExpectedLength( length );

                if ( tlv.isConstructed() )
                {
                    // We have a constructed tag, so we must switch the
                    // parentTLV
                    tlv.setParent( parentTLV );
                    container.setParentTLV( tlv );
                }
            }

        }

        if ( IS_DEBUG )
        {
            log.debug( "Length {} has been decoded", Integer.valueOf( length ) );
        }

        if ( length == 0 )
        {
            // The length is 0, so we can't expect a value.
            container.setState( TLVStateEnum.TLV_STATE_DONE );
        }
        else
        {
            // Go ahead and decode the value part
            container.setState( TLVStateEnum.VALUE_STATE_START );
        }
    }


    /**
     * Treat the Value part. We will distinguish two cases : - if the Tag is a
     * Primitive one, we will get the value. - if the Tag is a Constructed one,
     * nothing will be done.
     * 
     * @param stream The ByteBuffer containing the PDU to decode
     * @param container The container that stores the current state, 
     * the result and other informations.
     * @return <code>true</code> if there are more bytes to read, <code>false 
     * </code> otherwise
     * @throws DecoderException Thrown if anything went wrong
     */
    private boolean treatValueStartState( ByteBuffer stream, IAsn1Container container )
    {

        TLV currentTlv = container.getCurrentTLV();

        if ( TLV.isConstructed( currentTlv.getTag() ) )
        {
            container.setState( TLVStateEnum.TLV_STATE_DONE );

            return MORE;
        }
        else
        {

            int length = currentTlv.getLength();
            int nbBytes = stream.remaining();

            if ( nbBytes < length )
            {
                currentTlv.getValue().init( length );
                currentTlv.getValue().setData( stream );
                container.setState( TLVStateEnum.VALUE_STATE_PENDING );

                return END;
            }
            else
            {
                currentTlv.getValue().init( length );
                stream.get( currentTlv.getValue().getData(), 0, length );
                container.setState( TLVStateEnum.TLV_STATE_DONE );

                return MORE;
            }
        }
    }


    /**
     * Treat a pending Value when we get more bytes in the buffer.
     * 
     * @param stream The ByteBuffer containing the PDU to decode
     * @param container The container that stores the current state, 
     * the result and other informations.
     * @return <code>MORE</code> if some bytes remain in the buffer when the
     * value has been decoded, <code>END</code> if whe still need to get some 
     * more bytes.
     * @throws DecoderException Thrown if anything went wrong
     */
    private boolean treatValuePendingState( ByteBuffer stream, IAsn1Container container )
    {

        TLV currentTlv = container.getCurrentTLV();

        int length = currentTlv.getLength();
        int currentLength = currentTlv.getValue().getCurrentLength();
        int nbBytes = stream.remaining();

        if ( ( currentLength + nbBytes ) < length )
        {
            currentTlv.getValue().addData( stream );
            container.setState( TLVStateEnum.VALUE_STATE_PENDING );

            return END;
        }
        else
        {

            int remaining = length - currentLength;
            byte[] data = new byte[remaining];
            stream.get( data, 0, remaining );
            currentTlv.getValue().addData( data );
            container.setState( TLVStateEnum.TLV_STATE_DONE );

            return MORE;
        }
    }


    /**
     * When the TLV has been fully decoded, we have to execute the associated
     * action and switch to the next TLV, which will start with a Tag.
     * 
     * @param stream The ByteBuffer containing the PDU to decode
     * @param container The container that stores the current state, 
     * the result and other informations.
     * @return <code>true</code> if there are more bytes to read, <code>false 
     * </code> otherwise
     * @throws DecoderException Thrown if anything went wrong
     */
    private boolean treatTLVDoneState( ByteBuffer stream, IAsn1Container container ) throws DecoderException, NamingException
    {
        if ( IS_DEBUG )
        {
            dumpTLVTree( container );
        }

        // First, we have to execute the associated action
        container.getGrammar().executeAction( container );

        // Check if the PDU has been fully decoded.
        if ( isTLVDecoded( container ) )
        {
            if ( container.getState() == IStates.GRAMMAR_END )
            {
                // Change the state to DECODED
                container.setState( TLVStateEnum.PDU_DECODED );
            }
            else
            {
                if ( container.isGrammarEndAllowed() )
                {
                    // Change the state to DECODED
                    container.setState( TLVStateEnum.PDU_DECODED );
                }
                else
                {
                    log.error( "The PDU is decoded, but we should have had more TLVs" );
                    throw new DecoderException( "Truncated PDU. Some elements are lacking, accordingly to the grammar" );
                }
            }

        }
        else
        {
            // Then we switch to the Start tag state and free the current TLV
            container.setState( TLVStateEnum.TAG_STATE_START );
        }

        return stream.hasRemaining();
    }


    /**
     * An helper function that return a string representing the current state
     * for debuging purpose.
     * 
     * @param state The state
     * @return A string representation of the state
     */
    private String stateToString( int state )
    {

        switch ( state )
        {

            case TLVStateEnum.TAG_STATE_START:
                return "TAG_STATE_START";

            case TLVStateEnum.TAG_STATE_END:
                return "TAG_STATE_END";

            case TLVStateEnum.TAG_STATE_OVERFLOW:
                return "TAG_STATE_OVERFLOW";

            case TLVStateEnum.LENGTH_STATE_START:
                return "LENGTH_STATE_START";

            case TLVStateEnum.LENGTH_STATE_PENDING:
                return "LENGTH_STATE_PENDING";

            case TLVStateEnum.LENGTH_STATE_END:
                return "LENGTH_STATE_END";

            case TLVStateEnum.VALUE_STATE_START:
                return "VALUE_STATE_START";

            case TLVStateEnum.VALUE_STATE_PENDING:
                return "VALUE_STATE_PENDING";

            case TLVStateEnum.TLV_STATE_DONE:
                return "TLV_STATE_DONE";

            default:
                return "UNKNOWN_STATE";
        }
    }


    /**
     * The decoder main function. This is where we read bytes from the stream
     * and go through the automaton. It's an inifnite loop which stop when no
     * more bytes are to be read. It can occurs if the ByteBuffer is exhausted
     * or if the PDU has been fully decoded.
     * 
     * @param stream The ByteBuffer containing the PDU to decode
     * @param container The container that store the state, the result 
     * and other elements.
     * @throws DecoderException Thrown if anything went wrong!
     */
    public void decode( ByteBuffer stream, IAsn1Container container ) throws DecoderException, NamingException
    {

        /*
         * We have to deal with the current state. This is an infinite loop,
         * which will stop for any of these reasons : 
         * - STATE_END has been reached (hopefully, the most frequent case) 
         * - buffer is empty (it could happen) 
         * - STATE_OVERFLOW : bad situation ! The PDU may be a
         * malevolous hand crafted ones, that try to "kill" our decoder. Whe
         * must log it with all information to track back this case, and punish
         * the guilty !
         */

        boolean hasRemaining = stream.hasRemaining();

        if ( IS_DEBUG )
        {
            log.debug( ">>>==========================================" );
            log.debug( "--> Decoding a PDU" );
            log.debug( ">>>------------------------------------------" );
        }

        while ( hasRemaining )
        {

            if ( IS_DEBUG )
            {
                log.debug( "--- State = {} ---", stateToString( container.getState() ) );

                if ( stream.hasRemaining() )
                {
                    byte octet = stream.get( stream.position() );

                    log.debug( "  current byte : {}", Asn1StringUtils.dumpByte( octet ) );
                }
                else
                {
                    log.debug( "  no more byte to decode in the stream" );
                }
            }

            switch ( container.getState() )
            {

                case TLVStateEnum.TAG_STATE_START:
                    // Reset the GrammarEnd flag first
                    container.grammarEndAllowed( false );
                    hasRemaining = treatTagStartState( stream, container );

                    break;

                case TLVStateEnum.LENGTH_STATE_START:
                    hasRemaining = treatLengthStartState( stream, container );

                    break;

                case TLVStateEnum.LENGTH_STATE_PENDING:
                    hasRemaining = treatLengthPendingState( stream, container );

                    break;

                case TLVStateEnum.LENGTH_STATE_END:
                    treatLengthEndState( container );

                    break;

                case TLVStateEnum.VALUE_STATE_START:
                    hasRemaining = treatValueStartState( stream, container );

                    break;

                case TLVStateEnum.VALUE_STATE_PENDING:
                    hasRemaining = treatValuePendingState( stream, container );

                    break;

                case TLVStateEnum.VALUE_STATE_END:
                    hasRemaining = stream.hasRemaining();

                    // Nothing to do. We will never reach this state
                    break;

                case TLVStateEnum.TLV_STATE_DONE:
                    hasRemaining = treatTLVDoneState( stream, container );

                    break;

                case TLVStateEnum.PDU_DECODED:
                    // We have to deal with the case where there are
                    // more bytes in the buffer, but the PDU has been decoded.
                    log.warn( "The PDU has been fully decoded but there are still bytes in the buffer." );

                    hasRemaining = false;

                    break;
            }
        }

        if ( IS_DEBUG )
        {
            log.debug( "<<<------------------------------------------" );

            if ( container.getState() == TLVStateEnum.PDU_DECODED )
            {
                if ( container.getCurrentTLV() != null )
                {
                    log.debug( "<-- Stop decoding : {}", container.getCurrentTLV().toString() );
                }
                else
                {
                    log.debug( "<-- Stop decoding : null current TLV" );
                }
            }
            else
            {
                if ( container.getCurrentTLV() != null )
                {
                    log.debug( "<-- End decoding : {}", container.getCurrentTLV().toString() );
                }
                else
                {
                    log.debug( "<-- End decoding : null current TLV" );
                }
            }

            log.debug( "<<<==========================================" );
        }

        return;
    }


    /**
     * Get the length's Length.
     * 
     * @return Returns the length's Length.
     */
    public int getMaxLengthLength()
    {
        return maxLengthLength;
    }


    /**
     * Get the maximum Tag's length
     * 
     * @return Returns the maximum tag Length.
     */
    public int getMaxTagLength()
    {
        return maxTagLength;
    }


    /**
     * Disallow indefinite length.
     */
    public void disallowIndefiniteLength()
    {
        this.indefiniteLengthAllowed = false;
    }


    /**
     * Allow indefinite length.
     */
    public void allowIndefiniteLength()
    {
        this.indefiniteLengthAllowed = true;
    }


    /**
     * Tells if indefinite length form could be used for Length
     * 
     * @return Returns <code>true</code> if the current decoder support
     *         indefinite length
     */
    public boolean isIndefiniteLengthAllowed()
    {

        return indefiniteLengthAllowed;
    }


    /**
     * Set the maximul length for a Length
     * 
     * @param maxLengthLength The lengthLength to set.
     * @throws DecoderException Thrown if the indefinite length is 
     * allowed or if the length's Length is above 126 bytes
     */
    public void setMaxLengthLength( int maxLengthLength ) throws DecoderException
    {

        if ( ( this.indefiniteLengthAllowed ) && ( maxLengthLength > 126 ) )
        {
            throw new DecoderException( "Length above 126 bytes are not allowed for a definite form Length" );
        }

        this.maxLengthLength = maxLengthLength;
    }


    /**
     * Set the maximum Tag length
     * 
     * @param maxTagLength The tagLength to set.
     */
    public void setMaxTagLength( int maxTagLength )
    {
        this.maxTagLength = maxTagLength;
    }
}
