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
//package org.apache.directory.server.kerberos.shared.messages.value;


/**
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 *
public class PrincipalName
{
    private String nameComponent;
    private int nameType;


    /**
     * Creates a new instance of PrincipalName.
     *
     * @param nameComponent
     * @param nameType
     *
    public PrincipalName( String nameComponent, int nameType )
    {
        this.nameComponent = nameComponent;
        this.nameType = nameType;
    }


    /**
     * Returns the type of the {@link PrincipalName}.
     *
     * @return The type of the {@link PrincipalName}.
     *
    public int getNameType()
    {
        return nameType;
    }


    /**
     * Returns the name component.
     *
     * @return The name component.
     *
    public String getNameComponent()
    {
        return nameComponent;
    }
}*/

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
package org.apache.directory.server.kerberos.shared.messages.value;


import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

import javax.security.auth.kerberos.KerberosPrincipal;

import org.apache.directory.server.kerberos.shared.KerberosUtils;
import org.apache.directory.server.kerberos.shared.messages.value.types.PrincipalNameType;
import org.apache.directory.shared.asn1.AbstractAsn1Object;
import org.apache.directory.shared.asn1.ber.tlv.TLV;
import org.apache.directory.shared.asn1.ber.tlv.UniversalTag;
import org.apache.directory.shared.asn1.ber.tlv.Value;
import org.apache.directory.shared.asn1.codec.EncoderException;
import org.apache.directory.shared.ldap.util.StringTools;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * A principal Name, composed of a type and N names.
 * 
 * PrincipalName   ::= SEQUENCE {
 *        name-type       [0] Int32,
 *        name-string     [1] SEQUENCE OF KerberosString
 * }
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class PrincipalName extends AbstractAsn1Object
{
    /** The logger */
    private static final Logger LOG = LoggerFactory.getLogger( PrincipalName.class );

    /** Speedup for logs */
    private static final boolean IS_DEBUG = LOG.isDebugEnabled();

    /** The type for this principal */
    private PrincipalNameType nameType;

    /** The principal name - we may have more than one - */
    private List<String> nameString;
    
    /** The principal name as a byte[], for encoding purpose */
    private transient List<byte[]> nameBytes;
    
    // Storage for computed lengths
    private transient int principalNameSeqLength;
    private transient int principalTypeTagLength;
    private transient int principalTypeLength;
    private transient int principalStringsTagLength;
    private transient int principalStringsSeqLength;

    /**
     * Creates a new empty instance of PrincipalName.
     *
     * @param principal A Sun kerberosPrincipal instance
     */
    public PrincipalName()
    {
    }

    /**
     * Creates a new instance of PrincipalName, given a KerberosPrincipal.
     * 
     * We assume that a principal has only one type, even if there are
     * more than one name component.
     *
     * @param principal A Sun kerberosPrincipal instance
     */
    public PrincipalName( KerberosPrincipal principal )
    {
        try
        {
            nameString = KerberosUtils.getNames( principal );
        }
        catch ( ParseException pe )
        {
            nameString = KerberosUtils.EMPTY_PRINCIPAL_NAME;
        }

        this.nameType = PrincipalNameType.getTypeByOrdinal( principal.getNameType() );
    }
    
    /**
     * Creates a new instance of PrincipalName given a String and an 
     * prinipal type.
     * 
     * @param nameString The name string, which can contains more than one nameComponent
     * @param nameType The principal name
     */
    public PrincipalName( String nameString, PrincipalNameType nameType )  throws ParseException
    {
        this.nameString = KerberosUtils.getNames( nameString );
        
        this.nameType = nameType;
    }


    /**
     * Creates a new instance of PrincipalName.
     *
     * @param nameComponent
     * @param nameType
     */
    public PrincipalName( String nameString, int nameType ) throws ParseException
    {
        this.nameString = KerberosUtils.getNames( nameString );
        
        this.nameType = PrincipalNameType.getTypeByOrdinal( nameType );
    }


    /**
     * Returns the type of the {@link PrincipalName}.
     *
     * @return The type of the {@link PrincipalName}.
     */
    public PrincipalNameType getNameType()
    {
        return nameType;
    }
                    
    /** 
     * Set the Principal name Type
     * @param nameType the Principal name Type
     */
    public void setNameType( PrincipalNameType nameType )
    {
        this.nameType = nameType;
    }

    /** 
     * Set the Principal name Type
     * @param nameType the Principal name Type
     */
    public void setNameType( int nameType )
    {
        this.nameType = PrincipalNameType.getTypeByOrdinal( nameType );
    }

    /**
     * Returns the name components.
     *
     * @return The name components.
     */
    public List<String> getNames()
    {
        return nameString;
    }


    /**
     * @return A String representing the principal names as a String 
     */
    public String getNameString()
    {
        if ( ( nameString == null ) || ( nameString.size() == 0 ) )
        {
            return "";
        }
        else
        {
            StringBuilder sb = new StringBuilder();
            boolean isFirst = true;

            for ( String name : nameString )
            {
                if ( isFirst )
                {
                    isFirst = false;
                }
                else
                {
                    sb.append( '/' );
                }

                sb.append( name );
            }

            return sb.toString();
        }
    }


    /**
     * Add a new name to the PrincipalName
     * @param name The name to add
     */
    public void addName( String name )
    {
        if ( nameString == null )
        {
            nameString = new ArrayList<String>();
        }

        nameString.add( name );
    }


    /**
     * Compute the PrincipalName length
     * 
     * PrincipalName :
     * 
     * 0x30 L1 PrincipalName sequence
     *  |
     *  +--> 0xA1 L2 name-type tag
     *  |     |
     *  |     +--> 0x02 L2-1 addressType (int)
     *  |
     *  +--> 0xA2 L3 name-string tag
     *        |
     *        +--> 0x30 L3-1 name-string (SEQUENCE OF KerberosString)
     *              |
     *              +--> 0x1B L4[1] value (KerberosString)
     *              |
     *              +--> 0x1B L4[2] value (KerberosString)
     *              |
     *              ...
     *              |
     *              +--> 0x1B L4[n] value (KerberosString)
     */
    public int computeLength()
    {
        // The principalName can't be empty.
        principalTypeLength = Value.getNbBytes( nameType.getOrdinal() );
        principalTypeTagLength = 1 + TLV.getNbBytes( principalTypeLength ) + principalTypeLength;
        
        principalNameSeqLength = 1 + TLV.getNbBytes( principalTypeTagLength ) + principalTypeTagLength;

        // Compute the keyValue
        if ( ( nameString == null ) || ( nameString.size() == 0 ) )
        {
            principalStringsSeqLength = 0;
        }
        else
        {
            principalStringsSeqLength = 0;
            nameBytes = new ArrayList<byte[]>( nameString.size() );

            for ( String name : nameString )
            {
                if ( name != null )
                {
                    byte[] bytes = StringTools.getBytesUtf8( name );
                    nameBytes.add( bytes );
                    principalStringsSeqLength += 1 + TLV.getNbBytes( bytes.length ) + bytes.length;
                }
                else
                {
                    nameBytes.add( StringTools.EMPTY_BYTES );
                    principalStringsSeqLength += 1 + 1;
                }
            }
        }

        principalStringsTagLength = 1 + TLV.getNbBytes( principalStringsSeqLength ) + principalStringsSeqLength;
        principalNameSeqLength += 1 + TLV.getNbBytes( principalStringsTagLength ) + principalStringsTagLength;

        // Compute the whole sequence length
        return 1 + TLV.getNbBytes( principalNameSeqLength ) + principalNameSeqLength;
    }


    /**
     * Encode the PrincipalName message to a PDU. 
     * 
     * PrincipalName :
     * 
     * 0x30 LL
     *   0xA0 LL 
     *     0x02 0x01 name-type (integer)
     *   0xA1 LL 
     *     0x30 LL name-string (SEQUENCE OF KerberosString)
     *       0x1B LL name-string[1]
     *       0x1B LL name-string[2]
     *       ...
     *       0x1B LL name-string[n]
     * 
     * @param buffer The buffer where to put the PDU. It should have been allocated
     * before, with the right size.
     * @return The constructed PDU.
     */
    public ByteBuffer encode( ByteBuffer buffer ) throws EncoderException
    {
        if ( buffer == null )
        {
            throw new EncoderException( "Cannot put a PDU in a null buffer !" );
        }

        try
        {
            // The PrincipalName SEQ Tag
            buffer.put( UniversalTag.SEQUENCE_TAG );
            buffer.put( TLV.getBytes( principalNameSeqLength ) );

            // The name-type, first the tag, then the value
            buffer.put( ( byte ) 0xA0 );
            buffer.put( TLV.getBytes( principalTypeTagLength ) );
            Value.encode( buffer, nameType.getOrdinal() );

            // The name-string tag
            buffer.put( ( byte ) 0xA1 );
            buffer.put( TLV.getBytes( principalStringsTagLength ) );

            // The name-string sequence
            buffer.put( UniversalTag.SEQUENCE_TAG );

            if ( ( nameString == null ) || ( nameString.size() == 0 ) )
            {
                buffer.put( ( byte ) 0x00 );
            }
            else
            {
                buffer.put( TLV.getBytes( principalStringsSeqLength ) );

                // The kerberosStrings
                for ( byte[] name : nameBytes )
                {
                    buffer.put( UniversalTag.GENERALIZED_STRING_TAG );

                    if ( ( name == null ) || ( name.length == 0 ) )
                    {
                        buffer.put( ( byte ) 0x00 );
                    }
                    else
                    {
                        buffer.put( TLV.getBytes( name.length ) );
                        buffer.put( name );
                    }
                }
            }
        }
        catch ( BufferOverflowException boe )
        {
            LOG.error(
                "Cannot encode the principalName object, the PDU size is {} when only {} bytes has been allocated", 1
                    + TLV.getNbBytes( principalNameSeqLength ) + principalNameSeqLength, buffer.capacity() );
            throw new EncoderException( "The PDU buffer size is too small !" );
        }

        if ( IS_DEBUG )
        {
            LOG.debug( "PrinipalName encoding : {}", StringTools.dumpBytes( buffer.array() ) );
            LOG.debug( "PrinipalName initial value : {}", toString() );
        }

        return buffer;
    }


    /**
     * @see Object#toString()
     */
    public String toString()
    {
        StringBuilder sb = new StringBuilder();

        sb.append( "PincipalName : {\n" );

        sb.append( "    name-type: " ).append( nameType ).append( '\n' );

        if ( ( nameString != null ) && ( nameString.size() != 0 ) )
        {
            sb.append( "    name-string : <" );
            boolean isFirst = true;

            for ( String name : nameString )
            {
                if ( isFirst )
                {
                    isFirst = false;
                }
                else
                {
                    sb.append( ", " );
                }

                sb.append( '\'' ).append( name ).append( '\'' );
            }

            sb.append( ">\n}" );
        }
        else
        {
            sb.append( "    no name-string\n}" );
        }

        return sb.toString();
    }
}
