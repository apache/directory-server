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
package org.apache.ldap.common.name;

import java.io.Serializable;

import javax.naming.InvalidNameException;

import org.apache.ldap.common.util.StringTools;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A Attribute Type And Value, which is the basis of all RDN.
 * It contains a type, and a value.
 * 
 * The type must not be case sensitive. Superfluous leading
 * and trailing spaces MUST have been trimmed before.
 * 
 * The value MUST be in UTF8 format, according to RFC 2253. If the type 
 * is in OID form, then the value must be a hexadecimal string prefixed 
 * by a '#' character. Otherwise, the string must respect the RC 2253
 * grammar. No further normalization will be done, because we don't
 * have any knowledge of the Schema definition in the parser. 
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class AttributeTypeAndValue implements Cloneable, Comparable, Serializable
{
    /**
     * Declares the Serial Version Uid.
     * 
     * @see <a href="http://c2.com/cgi/wiki?AlwaysDeclareSerialVersionUid">Always Declare Serial Version Uid</a>
     */
    private static final long serialVersionUID = 1L;

    /** The LoggerFactory used by this class */
    private static Logger log = LoggerFactory.getLogger( AttributeTypeAndValue.class );

    /** The Name type */
    private String type;
    
    /** The name value */
    private String value;
    
    /** Two values used for comparizon */
    private static final boolean CASE_SENSITIVE = true;
    private static final boolean CASE_INSENSITIVE = false;
    
    /**
     * Construct an empty AttributeTypeAndValue
     */ 
    public AttributeTypeAndValue()
    {
        type = null;
        value = null;
    }
    
    /**
     * Construct an AttributeTypeAndValue. The type and value are normalized :
     * - the type is trimmed and lowercased
     * - the value is trimmed
     * 
     * @param type The type
     * @param value the value
     */ 
    public AttributeTypeAndValue( String type, String value ) throws InvalidNameException
    {
    	if ( StringTools.isEmpty( type ) || StringTools.isEmpty( type.trim() ) )
    	{
    		log.error( "The type cannot be empty or null" );
    		throw new InvalidNameException( "Null or empty type is not allowed" );
    	}
    	
        this.type = type;
        this.value = StringTools.isEmpty( value ) ? "" : value;
    }
    
    /**
     * Get the type of a AttributeTypeAndValue
     * 
     * @return The type
     */
    public String getType()
    {
        return type;
    }
    
    /**
     * Store the type 
     * 
     * @param type The AttributeTypeAndValue type 
     */
    public void setType( String type ) throws InvalidNameException
    {
        if ( StringTools.isEmpty( type ) )
        {
            throw new InvalidNameException( "The AttributeTypeAndValue type cannot be null : " );
        }
        
        this.type = type;
    }
    
    /**
     * Store the type, after having trimmed and lowercased it.
     * 
     * @param type The AttributeTypeAndValue type 
     */
    public void setTypeNormalized( String type ) throws InvalidNameException
    {
        this.type = StringTools.lowerCase( StringTools.trim( type ) );

        if ( StringTools.isEmpty( this.type ) )
        {
            throw new InvalidNameException( "The AttributeTypeAndValue type cannot be null : " );
        }
    }
    
    /**
     * Get the Value of a AttributeTypeAndValue
     * 
     * @return The value
     */
    public String getValue()
    {
        return value;
    }
    
    /**
     * Store the value of a AttributeTypeAndValue.
     * 
     * @param value The value of the AttributeTypeAndValue
     */
    public void setValue( String value )
    {
        this.value = StringTools.isEmpty( value ) ? "" : value;
    }

    /**
     * Store the value of a AttributeTypeAndValue, after having trimmed it. 
     * 
     * @param value The value of the AttributeTypeAndValue
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
     * @return 0 if both NC are equals, otherwise a positive value if
     * the original NC is superior to the second one, a negative value 
     * if the second NC is superior.
     */
    public int compareTo( Object object )
    {
        if ( object instanceof AttributeTypeAndValue )
        {
            AttributeTypeAndValue nc = (AttributeTypeAndValue)object;
            
            int res = compareType( type, nc.type );
            
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
     * Compares two NameComponents. They are equals if :
     * - types are equals, case insensitive,
     * - values are equals, case insensitive
     * 
     * @param object
     * @return 0 if both NC are equals, otherwise a positive value if
     * the original NC is superior to the second one, a negative value 
     * if the second NC is superior.
     */
    public int compareToIgnoreCase( Object object )
    {
        if ( object instanceof AttributeTypeAndValue )
        {
            AttributeTypeAndValue nc = (AttributeTypeAndValue)object;
            
            int res = compareType( type, nc.type );
            
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
     * @param val1 First String
     * @param val2 Second String
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
     * @param val1 First String
     * @param val2 Second String
     * @return true if both strings are equals or null.
     */
    private int compareValue( String val1, String val2, boolean sensitivity )
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
        	int res = 0;
        	
        	if ( sensitivity == CASE_SENSITIVE )
        	{
        		res = ( StringTools.trim( val1 ) ).compareTo( StringTools.trim( val2 ) );
        	}
        	else
        	{
        		res = ( StringTools.trim( val1 ) ).compareToIgnoreCase( StringTools.trim( val2 ) );
        	}
        	
    		return (res < 0 ? -1 : res > 0 ? 1 : 0 );
        }
    }

    /**
     * A Normalized String representation of a AttributeTypeAndValue :
     * - type is trimed and lowercased
     * - value is trimed and lowercased
     * 
     * @return A normalized string representing a AttributeTypeAndValue
     */
    public String normalize()
    {
        return StringTools.lowerCase( StringTools.trim( type ) ) + '=' +
        StringTools.trim( value );
    }

    /**
     * A String representation of a AttributeTypeAndValue.
     * 
     * @return A string representing a AttributeTypeAndValue
     */
    public String toString()
    {
        StringBuffer sb = new StringBuffer();
        
        if ( StringTools.isEmpty( type ) || StringTools.isEmpty( type.trim() ) )
        {
        	return "";
        }
        
        sb.append( type ).append( "=" );
        
        if ( value != null )
        {
        	sb.append( value );
        }
        
        return sb.toString();
    }
}

