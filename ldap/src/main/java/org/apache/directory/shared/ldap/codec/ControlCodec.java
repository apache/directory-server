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


import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;

import org.apache.directory.shared.asn1.AbstractAsn1Object;
import org.apache.directory.shared.asn1.Asn1Object;
import org.apache.directory.shared.asn1.ber.tlv.TLV;
import org.apache.directory.shared.asn1.ber.tlv.UniversalTag;
import org.apache.directory.shared.asn1.ber.tlv.Value;
import org.apache.directory.shared.asn1.codec.EncoderException;
import org.apache.directory.shared.ldap.util.StringTools;


/**
 * A Asn1Object to store a Control.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$, 
 */
public class ControlCodec extends AbstractAsn1Object
{
    // ~ Instance fields
    // ----------------------------------------------------------------------------

    /** The control type */
    private String controlType;

    /** The criticality (default value is false) */
    private boolean criticality = false;

    /** Optionnal control value */
    private Object controlValue;

    /** Optionnal control value in encoded form */
    private byte[] encodedValue;

    /** The control length */
    private int controlLength;

    // ~ Methods
    // ------------------------------------------------------------------------------------

    /**
     * Default constructor.
     */
    public ControlCodec()
    {
        super();
    }

    /**
     * Get the control type
     * 
     * @return A string which represent the control type
     */
    public String getControlType()
    {
        return controlType == null ? "" : controlType;
    }


    /**
     * Set the control type
     * 
     * @param controlType The OID to be stored
     */
    public void setControlType( String controlType )
    {
        this.controlType = controlType;
    }


    /**
     * Get the control value
     * 
     * @return The control value
     */
    public Object getControlValue()
    {
        if ( controlValue == null )
        {
            return StringTools.EMPTY_BYTES;
        }
        else if ( controlValue instanceof String )
        {
            return StringTools.getBytesUtf8( ( String ) controlValue );
        }
        else
        {
            return controlValue;
        }
    }


    /**
     * Set the encoded control value
     * 
     * @param encodedValue The encoded control value to store
     */
    public void setEncodedValue( byte[] encodedValue )
    {
        if ( encodedValue != null )
        {
            this.encodedValue = new byte[ encodedValue.length ];
            System.arraycopy( encodedValue, 0, this.encodedValue, 0, encodedValue.length );
        } else {
            this.encodedValue = null;
        }
    }


    /**
     * Get the raw control encoded bytes
     * 
     * @return the encoded bytes for the control
     */
    public byte[] getEncodedValue()
    {
        if ( encodedValue == null )
        {
            return StringTools.EMPTY_BYTES;
        }

        final byte[] copy = new byte[ encodedValue.length ];
        System.arraycopy( encodedValue, 0, copy, 0, encodedValue.length );
        return copy;
    }


    /**
     * Set the control value
     * 
     * @param controlValue The control value to store
     */
    public void setControlValue( Object controlValue )
    {
        this.controlValue = controlValue;
    }


    /**
     * Get the criticality
     * 
     * @return <code>true</code> if the criticality flag is true.
     */
    public boolean getCriticality()
    {
        return criticality;
    }


    /**
     * Set the criticality
     * 
     * @param criticality The criticality value
     */
    public void setCriticality( boolean criticality )
    {
        this.criticality = criticality;
    }


    /**
     * Compute the Control length 
     * Control :
     * 
     * 0x30 L1
     *  |
     *  +--> 0x04 L2 controlType
     * [+--> 0x01 0x01 criticality]
     * [+--> 0x04 L3 controlValue] 
     * 
     * Control length = Length(0x30) + length(L1) 
     *                  + Length(0x04) + Length(L2) + L2
     *                  [+ Length(0x01) + 1 + 1]
     *                  [+ Length(0x04) + Length(L3) + L3]
     */
    public int computeLength()
    {
        // The controlType
        int controlTypeLengh = StringTools.getBytesUtf8( controlType ).length;
        controlLength = 1 + TLV.getNbBytes( controlTypeLengh ) + controlTypeLengh;

        // The criticality, only if true
        if ( criticality )
        {
            controlLength += 1 + 1 + 1; // Always 3 for a boolean
        }

        // The control value, if any
        if ( controlValue != null )
        {
            byte[] controlBytes;
            if ( controlValue instanceof byte[] )
            {
                controlBytes = ( byte[] ) controlValue;
                controlLength += 1 + TLV.getNbBytes( controlBytes.length ) + controlBytes.length;
            }
            else if ( controlValue instanceof String )
            {
                controlBytes = StringTools.getBytesUtf8( ( String ) controlValue );
                controlLength += 1 + TLV.getNbBytes( controlBytes.length ) + controlBytes.length;
            }
            else if ( controlValue instanceof Asn1Object )
            {
                int length = ( ( Asn1Object ) controlValue ).computeLength();
                controlLength += 1 + TLV.getNbBytes( length ) + length;
            }
            else
            {
                throw new IllegalStateException( "Don't know how to handle control value class "
                    + controlValue.getClass() );
            }
        }

        return 1 + TLV.getNbBytes( controlLength ) + controlLength;
    }


    /**
     * Generate the PDU which contains the Control. 
     * Control : 
     * 0x30 LL
     *   0x04 LL type 
     *   [0x01 0x01 criticality]
     *   [0x04 LL value]
     * 
     * @param buffer The encoded PDU
     * @return A ByteBuffer that contaons the PDU
     * @throws EncoderException If anything goes wrong.
     */
    public ByteBuffer encode( ByteBuffer buffer ) throws EncoderException
    {
        try
        {
            // The LdapMessage Sequence
            buffer.put( UniversalTag.SEQUENCE_TAG );

            // The length has been calculated by the computeLength method
            buffer.put( TLV.getBytes( controlLength ) );
        }
        catch ( BufferOverflowException boe )
        {
            throw new EncoderException( "The PDU buffer size is too small !" );
        }

        // The control type
        Value.encode( buffer, getControlType().getBytes() );

        // The control criticality, if true
        if ( criticality )
        {
            Value.encode( buffer, criticality );
        }

        // The control value, if any
        if ( controlValue != null )
        {
            byte[] controlBytes;
            if ( controlValue instanceof byte[] )
            {
                controlBytes = ( byte[] ) controlValue;
                encodedValue = controlBytes;
            }
            else if ( controlValue instanceof String )
            {
                controlBytes = StringTools.getBytesUtf8( ( String ) controlValue );
                encodedValue = controlBytes;
            }
            else if ( controlValue instanceof Asn1Object )
            {
                controlBytes = ( ( Asn1Object ) controlValue ).encode( null ).array();
                encodedValue = controlBytes;
            }
            else
            {
                throw new IllegalStateException( "Don't know how to handle control value class "
                    + controlValue.getClass() );
            }

            Value.encode( buffer, controlBytes );
        }

        return buffer;
    }


    /**
     * Return a String representing a Control
     */
    public String toString()
    {
        StringBuffer sb = new StringBuffer();

        sb.append( "    Control\n" );
        sb.append( "        Control type : '" ).append( controlType ).append(
            "'\n" );
        sb.append( "        Criticality : '" ).append( criticality ).append( "'\n" );

        if ( controlValue != null )
        {
            if ( controlValue instanceof byte[] )
            {
                sb.append( "        Control value : '" ).append( StringTools.dumpBytes( ( byte[] ) controlValue ) )
                    .append( "'\n" );
            }
            else
            {
                sb.append( "        Control value : '" ).append( controlValue ).append( "'\n" );
            }
        }

        return sb.toString();
    }
}
