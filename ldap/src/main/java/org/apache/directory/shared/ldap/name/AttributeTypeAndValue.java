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
package org.apache.directory.shared.ldap.name;


import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Arrays;

import javax.naming.InvalidNameException;

import org.apache.directory.shared.ldap.util.StringTools;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * A Attribute Type And Value, which is the basis of all RDN. It contains a
 * type, and a value. The type must not be case sensitive. Superfluous leading
 * and trailing spaces MUST have been trimmed before. The value MUST be in UTF8
 * format, according to RFC 2253. If the type is in OID form, then the value
 * must be a hexadecimal string prefixed by a '#' character. Otherwise, the
 * string must respect the RC 2253 grammar. No further normalization will be
 * done, because we don't have any knowledge of the Schema definition in the
 * parser.
 *
 * We will also keep a User Provided form of the atav (Attribute Type And Value),
 * called upName.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class AttributeTypeAndValue implements Cloneable, Comparable, Externalizable
{
    /**
     * Declares the Serial Version Uid.
     *
     * @see <a
     *      href="http://c2.com/cgi/wiki?AlwaysDeclareSerialVersionUid">Always
     *      Declare Serial Version Uid</a>
     */
    private static final long serialVersionUID = 1L;

    /** The LoggerFactory used by this class */
    private static Logger LOG = LoggerFactory.getLogger( AttributeTypeAndValue.class );

    /** The normalized Name type */
    private String normType;

    /** The user provided Name type */
    private String upType;

    /** The name value. It can be a String or a byte array */
    private Object normValue;

    /** The name user provided value. It can be a String or a byte array */
    private Object upValue;

    /** The user provided atav */
    private String upName;

    /** The starting position of this atav in the given string from which
     * we have extracted the upName */
    private int start;

    /** The length of this atav upName */
    private int length;

    /** Two values used for comparizon */
    private static final boolean CASE_SENSITIVE = true;

    private static final boolean CASE_INSENSITIVE = false;


    /**
     * Construct an empty AttributeTypeAndValue
     */
    public AttributeTypeAndValue()
    {
        normType = null;
        upType = null;
        normValue = null;
        upValue = null;
        upName = "";
        start = -1;
        length = 0;
    }


    /**
     * Construct an AttributeTypeAndValue. The type and value are normalized :
     * <li> the type is trimmed and lowercased </li>
     * <li> the value is trimmed </li>
     * <p>
     * Note that the upValue should <b>not</b> be null or empty, or resolved
     * to an empty string after having trimmed it. 
     *
     * @param upType The Usrr Provided type
     * @param normType The normalized type
     * @param upValue The User Provided value
     * @param normValue The normalized value
     */
    public AttributeTypeAndValue( String upType, String normType, Object upValue, Object normValue ) throws InvalidNameException
    {
        String upTypeTrimmed = StringTools.trim( upType );
        String normTypeTrimmed = StringTools.trim( normType );
        
        if ( StringTools.isEmpty( upTypeTrimmed ) )
        {
            if ( StringTools.isEmpty( normTypeTrimmed ) )
            {
                String message =  "The type cannot be empty or null";
                LOG.error( message );
                throw new InvalidNameException( message );
            }
            else
            {
                // In this case, we will use the normType instead
                this.normType = StringTools.lowerCaseAscii( normTypeTrimmed );
                this.upType = normType;
            }
        }
        else if ( StringTools.isEmpty( normTypeTrimmed ) )
        {
            // In this case, we will use the upType instead
            this.normType = StringTools.lowerCaseAscii( upTypeTrimmed );
            this.upType = upType;
        }
        else
        {
            this.normType = StringTools.lowerCaseAscii( normTypeTrimmed );
            this.upType = upType;
            
        }
            

        if ( ( normValue == null ) || ( upValue == null ) )
        {
            if ( normValue instanceof String )
            {
                this.normValue = StringTools.isEmpty( ( String ) normValue ) ? "" : normValue;
            }
            else
            {
                this.normValue = normValue;
            }

            if ( upValue instanceof String )
            {
                this.upValue = StringTools.isEmpty( ( String ) upValue ) ? "" : upValue;
            }
            else
            {
                this.upValue = upValue;
            }
        }
        else
        {
    
            this.upValue = upValue;
    
            if ( normValue instanceof String )
            {
                this.normValue = StringTools.isEmpty( ( String ) normValue ) ? "" : normValue;
            }
            else
            {
                this.normValue = normValue;
            }
        }

        upName = this.upType + '=' + ( this.upValue == null ? "" : this.upValue );
        start = 0;
        length = upName.length();
    }


    /**
     * Construct an AttributeTypeAndValue. The type and value are normalized :
     * <li> the type is trimmed and lowercased </li>
     * <li> the value is trimmed </li>
     * <p>
     * Note that the upValue should <b>not</b> be null or empty, or resolved
     * to an empty string after having trimmed it. 
     *
     * @param upType The User Provided type
     * @param normType The normalized type
     * @param upValue The User Provided value
     * @param normValue The normalized value
     * @param start Start of this ATAV in the RDN
     * @param length Length of this ATAV
     * @param upName The user provided name
     */
    /**No protection*/ AttributeTypeAndValue( 
                            String upType, 
                            String normType, 
                            Object upValue, 
                            Object normValue,
                            int start, 
                            int length, 
                            String upName )
    {
        this.upType = upType;
        this.normType = normType;
        this.upValue = upValue;
        this.normValue = normValue;
        this.start = start;
        this.length = length;
        this.upName = upName;
    }

    
    /**
     * Get the normalized type of a AttributeTypeAndValue
     *
     * @return The normalized type
     */
    public String getNormType()
    {
        return normType;
    }

    /**
     * Get the user provided type of a AttributeTypeAndValue
     *
     * @return The user provided type
     */
    public String getUpType()
    {
        return upType;
    }


    /**
     * Store a new type
     *
     * @param upType The AttributeTypeAndValue User Provided type
     * @param type The AttributeTypeAndValue type
     * 
     * @throws InvalidNameException if the type or upType are empty or null.
     * If the upName is invalid.
     */
    public void setType( String upType, String type ) throws InvalidNameException
    {
        if ( StringTools.isEmpty( type ) || StringTools.isEmpty( type.trim() ) )
        {
            String message = "The type cannot be empty or null";
            LOG.error( message );
            throw new InvalidNameException( message );
        }
        
        if ( StringTools.isEmpty( upType ) || StringTools.isEmpty( upType.trim() ) )
        {
            String message = "The User Provided type cannot be empty or null";
            LOG.error( message );
            throw new InvalidNameException( message );
        }
        
        int equalPosition = upName.indexOf( '=' );
        
        if ( equalPosition <= 1 )
        {
            String message = "The User provided name does not contains an '='"; 
            LOG.error( message );
            throw new InvalidNameException( message );
        }

        normType = type.trim().toLowerCase();
        this.upType = upType;
        upName = upType + upName.substring( equalPosition );
        start = -1;
        length = upName.length();
    }


    /**
     * Store the type, after having trimmed and lowercased it.
     *
     * @param type
     *            The AttributeTypeAndValue type
     */
    public void setTypeNormalized( String type ) throws InvalidNameException
    {
        if ( StringTools.isEmpty( type ) || StringTools.isEmpty( type.trim() ) )
        {
            LOG.error( "The type cannot be empty or null" );
            throw new InvalidNameException( "The AttributeTypeAndValue type cannot be null or empty " );
        }

        normType = type.trim().toLowerCase();
        upType = type;
        upName = type + upName.substring( upName.indexOf( '=' ) );
        start = -1;
        length = upName.length();
    }


    /**
     * Get the Value of a AttributeTypeAndValue
     *
     * @return The value
     */
    public Object getNormValue()
    {
        return normValue;
    }

    /**
     * Get the User Provided Value of a AttributeTypeAndValue
     *
     * @return The value
     */
    public Object getUpValue()
    {
        return upValue;
    }

    /**
     * Get the normalized Value of a AttributeTypeAndValue
     *
     * @return The value
     */
    public String getNormalizedValue()
    {
        return normalize();
    }


    /**
     * Store the value of a AttributeTypeAndValue.
     *
     * @param value The user provided value of the AttributeTypeAndValue
     * @param normValue The normalized value
     */
    public void setValue( Object upValue, Object normValue )
    {
        if ( normValue instanceof String )
        {
            this.normValue = StringTools.isEmpty( ( String ) normValue ) ? "" : ( String ) normValue;
        }
        else
        {
            this.normValue = normValue;
        }

        this.upValue = upValue;
        upName = upName.substring( 0, upName.indexOf( '=' ) + 1 ) + upValue;
        start = -1;
        length = upName.length();
    }


    /**
     * Get the upName length
     *
     * @return the upName length
     */
    public int getLength()
    {
        return length;
    }


    /**
     * get the position in the original upName where this atav starts.
     *
     * @return The starting position of this atav
     */
    public int getStart()
    {
        return start;
    }


    /**
     * Get the user provided form of this attribute type and value
     *
     * @return The user provided form of this atav
     */
    public String getUpName()
    {
        return upName;
    }


    /**
     * Store the value of a AttributeTypeAndValue, after having trimmed it.
     *
     * @param value
     *            The value of the AttributeTypeAndValue
     */
    public void setValueNormalized( String value )
    {
        String newValue = StringTools.trim( value );

        if ( StringTools.isEmpty( newValue ) )
        {
            this.normValue = "";
        }
        else
        {
            this.normValue = newValue;
        }

        upName = upName.substring( 0, upName.indexOf( '=' ) + 1 ) + value;
        start = -1;
        length = upName.length();
    }


    /**
     * Implements the cloning.
     *
     * @return a clone of this object
     */
    public Object clone()
    {
        try
        {
            return super.clone();
        }
        catch ( CloneNotSupportedException cnse )
        {
            throw new Error( "Assertion failure" );
        }
    }


    /**
     * Compares two NameComponents. They are equals if : 
     * - types are equals, case insensitive, 
     * - values are equals, case sensitive
     *
     * @param object
     * @return 0 if both NC are equals, otherwise a positive value if the
     *         original NC is superior to the second one, a negative value if
     *         the second NC is superior.
     */
    public int compareTo( Object object )
    {
        if ( object instanceof AttributeTypeAndValue )
        {
            AttributeTypeAndValue nc = ( AttributeTypeAndValue ) object;

            int res = compareType( normType, nc.normType );

            if ( res != 0 )
            {
                return res;
            }
            else
            {
                return compareValue( normValue, nc.normValue, CASE_SENSITIVE );
            }
        }
        else
        {
            return 1;
        }
    }


    /**
     * Compares two NameComponents. They are equals if : 
     * - types are equals, case insensitive, 
     * - values are equals, case insensitive
     *
     * @param object
     * @return 0 if both NC are equals, otherwise a positive value if the
     *         original NC is superior to the second one, a negative value if
     *         the second NC is superior.
     */
    public int compareToIgnoreCase( Object object )
    {
        if ( object instanceof AttributeTypeAndValue )
        {
            AttributeTypeAndValue nc = ( AttributeTypeAndValue ) object;

            int res = compareType( normType, nc.normType );

            if ( res != 0 )
            {
                return res;
            }
            else
            {
                return compareValue( normValue, nc.normValue, CASE_INSENSITIVE );
            }
        }
        else
        {
            return 1;
        }
    }


    /**
     * Compare two types, trimed and case insensitive
     *
     * @param val1
     *            First String
     * @param val2
     *            Second String
     * @return true if both strings are equals or null.
     */
    private int compareType( String val1, String val2 )
    {
        if ( StringTools.isEmpty( val1 ) )
        {
            return StringTools.isEmpty( val2 ) ? 0 : -1;
        }
        else if ( StringTools.isEmpty( val2 ) )
        {
            return 1;
        }
        else
        {
            return ( StringTools.trim( val1 ) ).compareToIgnoreCase( StringTools.trim( val2 ) );
        }
    }


    /**
     * Compare two values
     *
     * @param val1 First value
     * @param val2 Second value
     * @param sensitivity A flag to define the case sensitivity
     * @return -1 if the first value is inferior to the second one, +1 if
     * its superior, 0 if both values are equal
     */
    private int compareValue( Object val1, Object val2, boolean sensitivity )
    {
        if ( val1 instanceof String )
        {
            if ( val2 instanceof String )
            {
                int val = ( sensitivity == CASE_SENSITIVE ) ? ( ( String ) val1 ).compareTo( ( String ) val2 )
                    : ( ( String ) val1 ).compareToIgnoreCase( ( String ) val2 );

                return ( val < 0 ? -1 : ( val > 0 ? 1 : val ) );
            }
            else
            {
                return 1;
            }
        }
        else if ( val1 instanceof byte[] )
        {
            if ( Arrays.equals( ( byte[] ) val1, ( byte[] ) val2 ) )
            {
                return 0;
            }
            else
            {
                return 1;
            }
        }
        else
        {
            return 1;
        }
    }

    private static final boolean[] DN_ESCAPED_CHARS = new boolean[]
        {
        true,  true,  true,  true,  true,  true,  true,  true,  // 0x00 -> 0x07
        true,  true,  true,  true,  true,  true,  true,  true,  // 0x08 -> 0x0F
        true,  true,  true,  true,  true,  true,  true,  true,  // 0x10 -> 0x17
        true,  true,  true,  true,  true,  true,  true,  true,  // 0x18 -> 0x1F
        true,  false, true,  true,  false, false, false, false, // 0x20 -> 0x27 ' ', '"', '#'
        false, false, false, true,  true,  false, false, false, // 0x28 -> 0x2F '+', ','
        false, false, false, false, false, false, false, false, // 0x30 -> 0x37 
        false, false, false, true,  true,  false, true,  false, // 0x38 -> 0x3F ';', '<', '>'
        false, false, false, false, false, false, false, false, // 0x40 -> 0x47
        false, false, false, false, false, false, false, false, // 0x48 -> 0x4F
        false, false, false, false, false, false, false, false, // 0x50 -> 0x57
        false, false, false, false, true,  false, false, false, // 0x58 -> 0x5F
        false, false, false, false, false, false, false, false, // 0x60 -> 0x67
        false, false, false, false, false, false, false, false, // 0x68 -> 0x6F
        false, false, false, false, false, false, false, false, // 0x70 -> 0x77
        false, false, false, false, false, false, false, false, // 0x78 -> 0x7F
        };

    /**
     * A Normalized String representation of a AttributeTypeAndValue : - type is
     * trimed and lowercased - value is trimed and lowercased, and special characters
     * are escaped if needed.
     *
     * @return A normalized string representing a AttributeTypeAndValue
     */
    public String normalize()
    {
        if ( normValue instanceof String )
        {
            // The result will be gathered in a stringBuilder
            StringBuilder sb = new StringBuilder();
            
            // First, store the type and the '=' char
            sb.append( normType ).append( '=' );
            
            String normalizedValue =  ( String ) normValue;
            int valueLength = normalizedValue.length();
            boolean escaped = false;
            
            if ( normalizedValue.length() > 0 )
            {
                char[] chars = normalizedValue.toCharArray();

                // Loop first assuming the DN won't contain any
                // char needing to be escaped. This is the case
                // for 99.99% of all DN (blind bet, of course ...) 
                for ( char c:chars )
                {
                    if ( ( c >= 0 ) && ( c < DN_ESCAPED_CHARS.length ) && DN_ESCAPED_CHARS[ c ] )
                    {
                        escaped = true;
                        break;
                    }
                }

                // Here, we have a char to escape. Start again the loop...
                if ( escaped )
                {
                    for ( int i = 0; i < valueLength; i++ )
                    {
                        char c = chars[i];

                        if ( ( c >= 0 ) && ( c < DN_ESCAPED_CHARS.length ) && DN_ESCAPED_CHARS[ c ] ) 
                        {
                            // Some chars need to be escaped even if they are US ASCII
                            // Just prefix them with a '\'
                            // Special cases are ' ' (space), '#') which need a special
                            // treatment.
                            if ( c == ' ' )
                            {
                                if ( ( i == 0 ) || ( i == valueLength - 1 ) )
                                {
                                    sb.append( '\\' ).append(  c  );
                                }
                                else
                                {
                                    sb.append( ' ' );
                                }
    
                                continue;
                            }
                            else if ( c == '#' )
                            {
                                if ( i == 0 )
                                {
                                    sb.append( "\\#" );
                                    continue;
                                }
                                else
                                {
                                    sb.append( '#' );
                                }
                                
                                continue;
                            }
    
                            sb.append( '\\' ).append( c );
                        }
                        else
                        {
                            // Standard ASCII chars are just appended
                            sb.append( c );
                        }
                    }
                }
                else
                {
                    // The String does not contain any escaped char : 
                    // just append it. 
                    sb.append( normalizedValue );
                }
            }
            
            return sb.toString();
        }
        else
        {
            return normType + "=#"
                + StringTools.dumpHexPairs( ( byte[] ) normValue );
        }
    }


    /**
     * Gets the hashcode of this object.
     *
     * @see java.lang.Object#hashCode()
     * @return The instance hash code
     */
    public int hashCode()
    {
        int result = 37;

        result = result*17 + ( normType != null ? normType.hashCode() : 0 );
        result = result*17 + ( normValue != null ? normValue.hashCode() : 0 );

        return result;
    }

    /**
     * @see Object#equals(Object)
     */
    public boolean equals( Object obj )
    {
        if ( this == obj )
        {
            return true;
        }
        
        if ( !( obj instanceof AttributeTypeAndValue ) )
        {
            return false;
        }
        
        AttributeTypeAndValue instance = (AttributeTypeAndValue)obj;
     
        // Compare the type
        if ( normType == null )
        {
            if ( instance.normType != null )
            {
                return false;
            }
        }
        else 
        {
            if ( !normType.equals( instance.normType ) )
            {
                return false;
            }
        }
            
        // Compare the values
        if ( normValue == null )
        {
            return instance.normValue == null;
        }
        else if ( normValue instanceof String )
        {
            if ( instance.normValue instanceof String )
            {
                return normValue.equals( instance.normValue );
            }
            else
            {
                return false;
            }
        }
        else if ( normValue instanceof byte[] )
        {
            if ( instance.normValue instanceof byte[] )
            {
                return Arrays.equals( (byte[])normValue, (byte[])instance.normValue );
            }
            else
            {
                return false;
            }
        }
        else
        {
            return false;
        }
    }

    
    /**
     * @see Externalizable#readExternal(ObjectInput)<p>
     * 
     * An AttributeTypeAndValue is composed of  a type and a value.
     * The data are stored following the structure :
     * 
     * <li>upName</li> The User provided ATAV
     * <li>start</li> The position of this ATAV in the DN
     * <li>length</li> The ATAV length
     * <li>upType</li> The user Provided Type
     * <li>normType</li> The normalized AttributeType
     * <li>isHR<li> Tells if the value is a String or not
     * <p>
     * if the value is a String :
     * <li>upValue</li> The User Provided value.
     * <li>value</li> The normalized value.
     * <p>
     * if the value is binary :
     * <li>upValueLength</li>
     * <li>upValue</li> The User Provided value.
     * <li>valueLength</li>
     * <li>value</li> The normalized value.
     */
    public void writeExternal( ObjectOutput out ) throws IOException
    {
        if ( StringTools.isEmpty( upName ) || 
             StringTools.isEmpty( upType ) ||
             StringTools.isEmpty( normType ) ||
             ( start < 0 ) ||
             ( length < 2 ) ||             // At least a type and '='
             ( upValue == null ) ||
             ( normValue == null ) )
        {
            String message = "Cannot serialize an wrong ATAV, ";
            
            if ( StringTools.isEmpty( upName ) )
            {
                message += "the upName should not be null or empty";
            }
            else if ( StringTools.isEmpty( upType ) )
            {
                message += "the upType should not be null or empty";
            }
            else if ( StringTools.isEmpty( normType ) )
            {
                message += "the normType should not be null or empty";
            }
            else if ( start < 0 )
            {
                message += "the start should not be < 0";
            }
            else if ( length < 2 )
            {
                message += "the length should not be < 2";
            }
            else if ( upValue == null )
            {
                message += "the upValue should not be null";
            }
            else if ( normValue == null )
            {
                message += "the value should not be null";
            }
                
            LOG.error( message );
            throw new IOException( message );
        }
        
        out.writeUTF( upName );
        out.writeInt( start );
        out.writeInt( length );
        out.writeUTF( upType );
        out.writeUTF( normType );
        
        boolean isHR = ( normValue instanceof String );
        
        out.writeBoolean( isHR );
        
        if ( isHR )
        {
            out.writeUTF( (String)upValue );
            out.writeUTF( (String)normValue );
        }
        else
        {
            out.writeInt( ((byte[])upValue).length );
            out.write( (byte[])upValue );
            out.writeInt( ((byte[])normValue).length );
            out.write( (byte[])normValue );
        }
    }
    
    
    /**
     * @see Externalizable#readExternal(ObjectInput)
     * 
     * We read back the data to create a new ATAV. The structure 
     * read is exposed in the {@link AttributeTypeAndValue#writeExternal(ObjectOutput)} 
     * method<p>
     */
    public void readExternal( ObjectInput in ) throws IOException, ClassNotFoundException
    {
        upName = in.readUTF();
        start = in.readInt();
        length = in.readInt();
        upType = in.readUTF();
        normType = in.readUTF();
        
        boolean isHR = in.readBoolean();
        
        if ( isHR )
        {
            upValue = in.readUTF();
            normValue = in.readUTF();
        }
        else
        {
            int upValueLength = in.readInt();
            upValue = new byte[upValueLength];
            in.readFully( (byte[])upValue );

            int valueLength = in.readInt();
            normValue = new byte[valueLength];
            in.readFully( (byte[])normValue );
        }
    }
    
    
    /**
     * A String representation of a AttributeTypeAndValue.
     *
     * @return A string representing a AttributeTypeAndValue
     */
    public String toString()
    {
        StringBuffer sb = new StringBuffer();

        if ( StringTools.isEmpty( normType ) || StringTools.isEmpty( normType.trim() ) )
        {
            return "";
        }

        sb.append( normType ).append( "=" );

        if ( normValue != null )
        {
            sb.append( normValue );
        }

        return sb.toString();
    }
}
