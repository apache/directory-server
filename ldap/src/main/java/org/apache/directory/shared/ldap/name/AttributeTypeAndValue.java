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


import java.io.Serializable;
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
 */
public class AttributeTypeAndValue implements Cloneable, Comparable, Serializable
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
    private static Logger log = LoggerFactory.getLogger( AttributeTypeAndValue.class );

    /** The normalized Name type */
    private String normType;

    /** The user provided Name type */
    private String upType;
    

    /** The name value. It can be a String or a byte array */
    private Object value;

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
        value = null;
        upValue = null;
        upName = "";
        start = -1;
        length = 0;
    }


    /**
     * Construct an AttributeTypeAndValue. The type and value are normalized :
     * - the type is trimmed and lowercased
     * - the value is trimmed
     *
     * @param type
     *            The type
     * @param value
     *            the value
     */
    public AttributeTypeAndValue( String upType, String type, Object upValue, Object value ) throws InvalidNameException
    {
        if ( StringTools.isEmpty( type ) || StringTools.isEmpty( type.trim() ) )
        {
            log.error( "The type cannot be empty or null" );
            throw new InvalidNameException( "Null or empty type is not allowed" );
        }

        normType = StringTools.lowerCaseAscii( type.trim() );
        this.upType = upType;
        this.upValue = upValue;

        if ( value instanceof String )
        {
            this.value = StringTools.isEmpty( ( String ) value ) ? "" : value;
        }
        else
        {
            this.value = value;
        }

        upName = upType + '=' + upValue;
        start = 0;
        length = upName.length();
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
     * Store the type
     *
     * @param type
     *            The AttributeTypeAndValue type
     */
    public void setType( String upType, String type ) throws InvalidNameException
    {
        if ( StringTools.isEmpty( type ) || StringTools.isEmpty( type.trim() ) )
        {
            log.error( "The type cannot be empty or null" );
            throw new InvalidNameException( "The AttributeTypeAndValue type cannot be null or empty " );
        }

        normType = type.trim().toLowerCase();
        this.upType = upType;
        upName = upType + upName.substring( upName.indexOf( '=' ) );
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
            log.error( "The type cannot be empty or null" );
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
    public Object getValue()
    {
        return value;
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
     * @param value
     *            The value of the AttributeTypeAndValue
     */
    public void setValue( Object upValue, Object value )
    {
        if ( value instanceof String )
        {
            this.value = StringTools.isEmpty( ( String ) value ) ? "" : ( String ) value;
        }
        else
        {
            this.value = value;
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
            this.value = "";
        }
        else
        {
            this.value = newValue;
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
     * Compares two NameComponents. They are equals if : - types are equals,
     * case insensitive, - values are equals, case sensitive
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
                return compareValue( value, nc.value, CASE_SENSITIVE );
            }
        }
        else
        {
            return 1;
        }
    }


    /**
     * Compares two NameComponents. They are equals if : - types are equals,
     * case insensitive, - values are equals, case insensitive
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
                return compareValue( value, nc.value, CASE_INSENSITIVE );
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
     * @param val1
     *            First String
     * @param val2
     *            Second String
     * @return true if both strings are equals or null.
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
        if ( value instanceof String )
        {
        	// The result will be gathered in a stringBuilder
            StringBuilder sb = new StringBuilder();
            
            // First, store the type and the '=' char
            sb.append( normType ).append( '=' );
            
            String normalizedValue =  ( String ) value;
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
                    if ( ( c < 0) || ( c > 128 ) )
                    {
                    	escaped = true;
                    	break;
                    }
                    else if ( DN_ESCAPED_CHARS[ c ] )
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
	                    
	                    if ( ( c < 0) || ( c > 128 ) )
	                    {
		                    // For chars which are not ASCII, use their hexa value prefixed by an '\'
	                        byte[] bb = StringTools.getBytesUtf8( normalizedValue.substring( i, i + 1 ) );
	                        
	                        for ( byte b:bb )
	                        {
	                            sb.append( '\\' ).
	                                append( StringTools.dumpHex( (byte)(( b & 0x00F0 ) >> 4) ) ).
	                                append( StringTools.dumpHex( b ) );
	                        }
	                    }
	                    else if ( DN_ESCAPED_CHARS[ c ] ) 
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
                + StringTools.dumpHexPairs( ( byte[] ) value );
        }
    }


    /**
     * Gets the hashcode of this object.
     *
     * @see java.lang.Object#hashCode()
     */
    public int hashCode()
    {
        int result = 37;

        result = result*17 + ( normType != null ? normType.hashCode() : 0 );
        result = result*17 + ( value != null ? value.hashCode() : 0 );

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
        
        if ( obj == null )
        {
            return false;
        }
        
        if ( obj.getClass() != this.getClass() )
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
            
        // Compare the value
        return ( value == null ? 
            instance.value == null  :
            value.equals( instance.value ) );
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

        if ( value != null )
        {
            sb.append( value );
        }

        return sb.toString();
    }
}
