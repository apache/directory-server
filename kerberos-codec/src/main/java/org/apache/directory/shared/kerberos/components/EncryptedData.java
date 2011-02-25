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
package org.apache.directory.shared.kerberos.components;


import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;
import java.util.Arrays;

import org.apache.directory.server.i18n.I18n;
import org.apache.directory.shared.asn1.AbstractAsn1Object;
import org.apache.directory.shared.asn1.EncoderException;
import org.apache.directory.shared.asn1.ber.tlv.TLV;
import org.apache.directory.shared.asn1.ber.tlv.UniversalTag;
import org.apache.directory.shared.asn1.ber.tlv.Value;
import org.apache.directory.shared.kerberos.codec.types.EncryptionType;
import org.apache.directory.shared.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * A structure storing an encrypted data element. The ASN.1 grammar is :
 * <pre>
 * EncryptedData   ::= SEQUENCE {
 *        etype   [0] Int32 -- EncryptionType --,
 *        kvno    [1] UInt32 OPTIONAL,
 *        cipher  [2] OCTET STRING -- ciphertext
 * }
 *</pre>
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class EncryptedData extends AbstractAsn1Object
{
    /** The logger */
    private static final Logger log = LoggerFactory.getLogger( EncryptedData.class );

    /** Speedup for logs */
    private static final boolean IS_DEBUG = log.isDebugEnabled();

    /** The used encryption algorithm */
    private EncryptionType eType;

    /** Version number of the key under which data is encrypted */
    private int kvno;

    /** A flag used to tell if a kvno has been added, as the kvno is optional. */
    private boolean hasKvno;

    /** The field containing the enciphered text */
    private byte[] cipher;

    /** A constant used when the key is not present */
    public static final boolean HAS_KVNO = true;

    // Storage for computed lengths
    private int eTypeTagLength;
    private int kvnoTagLength;
    private int cipherTagLength;
    private int encryptedDataSeqLength;

    /**
     * Creates a new instance of EncryptedData.
     */
    public EncryptedData()
    {
        hasKvno = !HAS_KVNO;
    }
    
    /**
     * Creates a new instance of EncryptedData.
     *
     * @param eType The encription algorithm
     * @param kvno The key version
     * @param cipher the encrypted text
     */
    public EncryptedData( EncryptionType eType, int kvno, byte[] cipher )
    {
        this.eType = eType;
        this.hasKvno = kvno > 0;
        this.kvno = kvno;
        this.cipher = cipher;
    }


    /**
     * Creates a new instance of EncryptedData.
     *
     * @param eType The encription algorithm
     * @param cipher the encrypted text
     */
    public EncryptedData( EncryptionType eType, byte[] cipher )
    {
        this.eType = eType;
        this.hasKvno = !HAS_KVNO;
        kvno = -1;
        this.cipher = cipher;
    }


    /**
     * Returns the {@link EncryptionType}.
     *
     * @return The {@link EncryptionType}.
     */
    public EncryptionType getEType()
    {
        return eType;
    }


    /**
     * Set the EncryptionType
     * @param eType the EncryptionType
     */
    public void setEType( EncryptionType eType )
    {
        this.eType = eType;
    }

    /**
     * Returns the key version.
     *
     * @return The key version.
     */
    public int getKvno()
    {
        return hasKvno ? kvno : -1;
    }

    /**
     * Set the key version
     * @param kvno The key version
     */
    public void setKvno( int kvno )
    {
        this.kvno = kvno;
        hasKvno = true;
    }

    /**
     * Tells if there is a key version.
     *
     * @return <code>true</code> if there is a key version.
     */
    public boolean hasKvno()
    {
        return hasKvno;
    }


    /**
     * Returns the raw cipher text.
     *
     * @return The raw cipher text.
     */
    public byte[] getCipher()
    {
        return cipher;
    }

    /**
     * Set the cipher text
     * @param cipher The cipher text
     */
    public void setCipher( byte[] cipher )
    {
        this.cipher = cipher;
    }
    

    /**
     * Compute the EncryptedData length
     * <pre>
     * EncryptedData :
     * 
     * 0x30 L1 EncryptedData sequence
     *  |
     *  +--> 0xA1 L2 etype tag
     *  |     |
     *  |     +--> 0x02 L2-1 etype (int)
     *  |
     *  +--> [0xA2 L3 kvno tag
     *  |     |
     *  |     +--> 0x30 L3-1 kvno (int)] (optional)
     *  |
     *  +--> 0xA2 L4 cipher tag
     *        |
     *        +--> 0x04 L4-1 cipher (OCTET STRING)
     * </pre>
     */
    public int computeLength()
    {
        encryptedDataSeqLength = 0;

        // Compute the encryption Type length
        int eTypeLength = Value.getNbBytes( eType.getValue() );
        eTypeTagLength = 1 + TLV.getNbBytes( eTypeLength ) + eTypeLength;
        encryptedDataSeqLength = 1 + TLV.getNbBytes( eTypeTagLength ) + eTypeTagLength; 


        // Compute the kvno length if any
        if ( hasKvno )
        {
            int kvnoLength = Value.getNbBytes( kvno );
            kvnoTagLength = 1 + TLV.getNbBytes( kvnoLength ) + kvnoLength;
            encryptedDataSeqLength += 1 + TLV.getNbBytes( kvnoTagLength ) + kvnoTagLength;
        }
        else
        {
            kvnoTagLength = 0;
        }

        // Compute the cipher
        if ( ( cipher == null ) || ( cipher.length == 0 ) )
        {
            cipherTagLength = 1 + 1;
        }
        else
        {
            cipherTagLength = 1 + TLV.getNbBytes( cipher.length ) + cipher.length;
        }

        encryptedDataSeqLength += 1 + TLV.getNbBytes( cipherTagLength ) + cipherTagLength;

        // Compute the whole sequence length
        return 1 + TLV.getNbBytes( encryptedDataSeqLength ) + encryptedDataSeqLength;
    }


    /**
     * Encode the EncryptedData message to a PDU. 
     * <pre>
     * EncryptedData :
     * 
     * 0x30 LL
     *   0xA0 LL 
     *     0x02 0x01 etype (integer)
     *   [0xA1 LL 
     *     0x02 0x01 kvno (integer)] (optional)
     *   0xA2 LL 
     *     0x04 LL cipher (OCTET STRING)
     * </pre>
     * @param buffer The buffer where to put the PDU. It should have been allocated
     * before, with the right size.
     * @return The constructed PDU.
     */
    public ByteBuffer encode( ByteBuffer buffer ) throws EncoderException
    {
        if ( buffer == null )
        {
            throw new EncoderException( I18n.err( I18n.ERR_148 ) );
        }

        try
        {
            // The EncryptedData SEQ Tag
            buffer.put( UniversalTag.SEQUENCE.getValue() );
            buffer.put( TLV.getBytes( encryptedDataSeqLength ) );

            // The etype, first the tag, then the value
            buffer.put( ( byte ) 0xA0 );
            buffer.put( TLV.getBytes( eTypeTagLength ) );

            Value.encode( buffer, eType.getValue() );

            // The kvno, if any, first the tag, then the value
            if ( hasKvno )
            {
                buffer.put( ( byte ) 0xA1 );
                buffer.put( TLV.getBytes( kvnoTagLength ) );

                Value.encode( buffer, kvno );
            }

            // The cipher tag
            buffer.put( ( byte ) 0xA2 );
            buffer.put( TLV.getBytes( cipherTagLength ) );
            Value.encode( buffer, cipher );
        }
        catch ( BufferOverflowException boe )
        {
            log.error( I18n.err( I18n.ERR_141, 1 + TLV.getNbBytes( encryptedDataSeqLength ) 
                + encryptedDataSeqLength, buffer.capacity() ) );
            throw new EncoderException( I18n.err( I18n.ERR_138 ) );
        }

        if ( IS_DEBUG )
        {
            log.debug( "EncryptedData encoding : {}", Strings.dumpBytes(buffer.array()) );
            log.debug( "EncryptedData initial value : {}", toString() );
        }

        return buffer;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = 1;
        result = prime * result + Arrays.hashCode( cipher );
        result = prime * result + ( ( eType == null ) ? 0 : eType.hashCode() );
        result = prime * result + kvno;
        return result;
    }

    
    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals( Object obj )
    {
        if ( this == obj )
        {
            return true;
        }
        
        if ( obj == null )
        {
            return false;
        }
        
        EncryptedData other = ( EncryptedData ) obj;
        
        if ( !Arrays.equals( cipher, other.cipher ) )
        {
            return false;
        }
        
        if ( eType != other.eType )
        {
            return false;
        }
        
        if ( kvno != other.kvno )
        {
            return false;
        }
        
        return true;
    }

    
    /**
     * @see Object#toString()
     */
    public String toString()
    {
        return toString( "" );
    }
    
    
    /**
     * @see Object#toString()
     */
    public String toString( String tabs )
    {
        StringBuilder sb = new StringBuilder();

        sb.append( tabs ).append( "EncryptedData : {\n" );
        sb.append( tabs ).append( "    etype: " ).append( eType ).append( '\n' );

        if ( hasKvno )
        {
            sb.append( tabs ).append( "    kvno: " ).append( kvno ).append( '\n' );
        }

        sb.append( tabs ).append( "    cipher: " ).append( Strings.dumpBytes(cipher) ).append( "\n}\n" );

        return sb.toString();
    }
}
