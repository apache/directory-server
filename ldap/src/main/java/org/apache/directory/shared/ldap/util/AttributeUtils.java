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
package org.apache.directory.shared.ldap.util;


import org.apache.directory.shared.ldap.common.ServerAttribute;
import org.apache.directory.shared.ldap.common.ServerAttributeImpl;
import org.apache.directory.shared.ldap.common.ServerEntry;
import org.apache.directory.shared.ldap.common.ServerEntryImpl;
import org.apache.directory.shared.ldap.message.AttributeImpl;
import org.apache.directory.shared.ldap.message.AttributesImpl;
import org.apache.directory.shared.ldap.message.ModificationItemImpl;
import org.apache.directory.shared.ldap.name.LdapDN;
import org.apache.directory.shared.ldap.schema.AttributeType;
import org.apache.directory.shared.ldap.schema.MatchingRule;
import org.apache.directory.shared.ldap.schema.NoOpNormalizer;
import org.apache.directory.shared.ldap.schema.Normalizer;

import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.BasicAttributes;
import javax.naming.directory.ModificationItem;
import java.text.ParseException;
import java.util.Arrays;
import java.util.List;


/**
 * A set of utility fuctions for working with Attributes.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class AttributeUtils
{
    /**
     * Correctly removes an attribute from an entry using it's attributeType information.
     * 
     * @param type the attributeType of the attribute to remove
     * @param entry the entry to remove the attribute from 
     * @return the Attribute that is removed
     * @throws NamingException if there are problems accessing the attribute
     */
    public static Attribute removeAttribute( AttributeType type, Attributes entry )
    {
        Attribute attr = entry.get( type.getOid() );
        if ( attr == null )
        {
            String[] aliases = type.getNames();
            for ( int ii = 0; ii < aliases.length; ii++ )
            {
                attr = entry.get( aliases[ii] );
                if ( attr != null )
                {
                    return entry.remove( attr.getID() );
                }
            }
        }
        
        if ( attr == null )
        {
            return null;
        }
        
        return entry.remove( attr.getID() );
    }
    
    
    /**
     * Compare two values and return true if they are equal.
     * 
     * @param value1 The first value
     * @param value2 The second value
     * @return true if both value are null or if they are equal.
     */
    public final static boolean equals( Object value1, Object value2 )
    {
        if ( value1 == value2 )
        {
            return true;
        }
        
        if ( value1 instanceof byte[] )
        {
            if ( value2 instanceof byte[] )
            {
                return Arrays.equals( (byte[])value1, (byte[])value2 );
            }
            else
            {
                return false;
            }
        }
        else
        {
            return value1.equals( value2 );
        }
    }
    
    /**
     * Clone the value. An attribute value is supposed to be either a String
     * or a byte array. If it's a String, then we just return it ( as String
     * is immutable, we don't need to copy it). If it's a bu=yte array, we
     * create a new byte array and copy the bytes into it.
     * 
     * @param value The value to clone
     * @return The cloned value
     */
    public final static Object cloneValue( Object value )
    {
        // First copy the value
        Object newValue = null;
        
        if ( value instanceof byte[] )
        {
            newValue = ((byte[])value).clone();
        }
        else
        {
            newValue = value;
        }
        
        return newValue;
    }

    /**
     * Switch from a BasicAttribute to a AttributeImpl. This is
     * necessary to allow cloning to be correctly handled.
     * 
     * @param attribute The attribute to transform
     * @return A instance of AttributeImpl
     */
    public final static Attribute toAttributeImpl( Attribute attribute )
    {
        if ( attribute instanceof AttributeImpl )
        {
            // Just return the attribute
            return attribute;
        }
        else
        {
            // Create a new AttributeImpl from the original attribute
            AttributeImpl newAttribute = new AttributeImpl( attribute.getID() );
            
            try
            {
                NamingEnumeration values = attribute.getAll();
                
                while ( values.hasMoreElements() )
                {
                    newAttribute.add( cloneValue( values.next() ) );
                }
                
                return newAttribute;
            }
            catch ( NamingException ne )
            {
                return newAttribute;
            }
        }
    }
    
    /**
     * Switch from a BasicAttributes to a AttributesImpl. This is
     * necessary to allow cloning to be correctly handled.
     * 
     * @param attributes The attributes to transform
     * @return A instance of AttributesImpl
     */
    public final static Attributes toAttributesImpl( Attributes attributes )
    {
        if ( attributes instanceof AttributesImpl )
        {
            // Just return the attribute
            return attributes;
        }
        else
        {
            // Create a new AttributesImpl from the original attribute
            AttributesImpl newAttributes = new AttributesImpl( attributes.isCaseIgnored() );
            
            try
            {
                NamingEnumeration values = attributes.getAll();
                
                while ( values.hasMoreElements() )
                {
                    Attribute attribute = (Attribute)values.next();
                    
                    newAttributes.put( toAttributeImpl( attribute ) );
                }
                
                return newAttributes;
            }
            catch ( NamingException ne )
            {
                return newAttributes;
            }
        }
    }
    
    /**
     * Utility method to extract an attribute from Attributes object using
     * all combinationos of the name including aliases.
     * 
     * @param attrs the Attributes to get the Attribute object from
     * @param type the attribute type specification
     * @return an Attribute with matching the attributeType spec or null
     */
    public final static Attribute getAttribute( Attributes attrs, AttributeType type )
    {
        // check if the attribute's OID is used
        Attribute attr = attrs.get( type.getOid() );
        
        if ( attr != null )
        {
            return attr;
        }

        // optimization bypass to avoid cost of the loop below
        if ( type.getNames().length == 1 )
        {
            attr = attrs.get( type.getNames()[0] );
            
            if ( attr != null )
            {
                return attr;
            }
        }
        
        // iterate through aliases
        for ( String alias:type.getNames() )
        {
            attr = attrs.get( alias );
            
            if ( attr != null )
            {
                return attr;
            }
        }
        
        return null;
    }
    
    
    /**
     * Utility method to extract a modification item from an array of modifications.
     * 
     * @param mods the array of ModificationItems to extract the Attribute from.
     * @param type the attributeType spec of the Attribute to extract
     * @return the modification item on the attributeType specified
     */
    public final static ModificationItem getModificationItem( ModificationItem[] mods, AttributeType type )
    {
        // optimization bypass to avoid cost of the loop below
        if ( type.getNames().length == 1 )
        {
            for ( int jj = 0; jj < mods.length; jj++ )
            {
                if ( mods[jj].getAttribute().getID().equalsIgnoreCase( type.getNames()[0] ) )
                {
                    return mods[jj];
                }
            }
        }
        
        // check if the attribute's OID is used
        for ( int jj = 0; jj < mods.length; jj++ )
        {
            if ( mods[jj].getAttribute().getID().equals( type.getOid() ) )
            {
                return mods[jj];
            }
        }
        
        // iterate through aliases
        for ( int ii = 0; ii < type.getNames().length; ii++ )
        {
            for ( int jj = 0; jj < mods.length; jj++ )
            {
                if ( mods[jj].getAttribute().getID().equalsIgnoreCase( type.getNames()[ii] ) )
                {
                    return mods[jj];
                }
            }
        }
        
        return null;
    }
    
    
    /**
     * Utility method to extract a modification item from an array of modifications.
     * 
     * @param mods the array of ModificationItems to extract the Attribute from.
     * @param type the attributeType spec of the Attribute to extract
     * @return the modification item on the attributeType specified
     */
    public final static ModificationItem getModificationItem( List<ModificationItemImpl> mods, AttributeType type )
    {
        // optimization bypass to avoid cost of the loop below
        if ( type.getNames().length == 1 )
        {
            for ( ModificationItem mod:mods )
            {
                if ( mod.getAttribute().getID().equalsIgnoreCase( type.getNames()[0] ) )
                {
                    return mod;
                }
            }
        }
        
        // check if the attribute's OID is used
        for ( ModificationItem mod:mods )
        {
            if ( mod.getAttribute().getID().equals( type.getOid() ) )
            {
                return mod;
            }
        }
        
        // iterate through aliases
        for ( int ii = 0; ii < type.getNames().length; ii++ )
        {
            for ( ModificationItem mod:mods )
            {
                if ( mod.getAttribute().getID().equalsIgnoreCase( type.getNames()[ii] ) )
                {
                    return mod;
                }
            }
        }
        
        return null;
    }

    
    /**
     * Utility method to extract an attribute from an array of modifications.
     * 
     * @param mods the array of ModificationItems to extract the Attribute from.
     * @param type the attributeType spec of the Attribute to extract
     * @return the extract Attribute or null if no such attribute exists
     */
    public final static Attribute getAttribute( ModificationItem[] mods, AttributeType type )
    {
        ModificationItem mod = getModificationItem( mods, type );
        
        if ( mod != null )
        {
            return mod.getAttribute();
        }
        
        return null;
    }
    

    /**
     * Utility method to extract an attribute from a list of modifications.
     * 
     * @param mods the list of ModificationItems to extract the Attribute from.
     * @param type the attributeType spec of the Attribute to extract
     * @return the extract Attribute or null if no such attribute exists
     */
    public final static Attribute getAttribute( List<ModificationItemImpl> mods, AttributeType type )
    {
        ModificationItem mod = getModificationItem( mods, type );
        
        if ( mod != null )
        {
            return mod.getAttribute();
        }
        
        return null;
    }
    

    /**
     * Check if an attribute contains a specific value, using the associated matchingRule for that
     *
     * @param attr The attribute we are searching in
     * @param compared The object we are looking for
     * @param type The attribute type
     * @return <code>true</code> if the value exists in the attribute</code>
     * @throws NamingException If something went wrong while accessing the data
     */
    public final static boolean containsValue( Attribute attr, Object compared, AttributeType type ) throws NamingException
    {
        // quick bypass test
        if ( attr.contains( compared ) )
        {
            return true;
        }
        
        MatchingRule matchingRule = type.getEquality();
        
        Normalizer normalizer = null;
        
        if ( matchingRule != null )
        {
            normalizer = type.getEquality().getNormalizer();
        }
        else
        {
            normalizer = new NoOpNormalizer();
        }

        if ( type.getSyntax().isHumanReadable() )
        {
            String comparedStr = ( String ) normalizer.normalize( compared );
            
            for ( NamingEnumeration values = attr.getAll(); values.hasMoreElements(); /**/ )
            //for ( int ii = attr.size() - 1; ii >= 0; ii-- )
            {
                String value = (String)values.nextElement();
                //String value = ( String ) attr.get( ii );
                if ( comparedStr.equals( normalizer.normalize( value ) ) )
                {
                    return true;
                }
            }
        }
        else
        {
            byte[] comparedBytes = null;
            
            if ( compared instanceof String )
            {
                if ( ((String)compared).length() < 3 )
                {
                    return false;
                }
                
                // Tansform the String to a byte array
                int state = 1;
                comparedBytes = new byte[((String)compared).length()/3];
                int pos = 0;
                
                for ( char c:((String)compared).toCharArray() )
                {
                    switch ( state )
                    {
                        case 1 :
                            if ( c != '\\' )
                            {
                                return false;
                            }

                            state++;
                            break;
                            
                        case 2 :
                            int high = StringTools.getHexValue( c );
                            
                            if ( high == -1 )
                            {
                                return false;
                            }
                            
                            comparedBytes[pos] = (byte)(high << 4);
                            
                            state++;
                            break;
                            
                        case 3 :
                            int low = StringTools.getHexValue( c );
                            
                            if ( low == -1 )
                            {
                                return false;
                            }
                            
                            comparedBytes[pos] += (byte)low;
                            pos++;
                            
                            state = 1;
                    }
                }
            }
            else
            {
                comparedBytes = ( byte[] ) compared;
            }
            
            for ( NamingEnumeration values = attr.getAll(); values.hasMoreElements(); /**/ )
            //for ( int ii = attr.size() - 1; ii >= 0; ii-- )
            {
                Object value = values.nextElement();
                
                if ( value instanceof byte[] )
                {
                    if ( ArrayUtils.isEquals( comparedBytes, value ) )
                    {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    /**
     * Check if an attribute contains a value. The test is case insensitive,
     * and the value is supposed to be a String. If the value is a byte[],
     * then the case sensitivity is useless.
     *
     * @param attr The attribute to check
     * @param value The value to look for
     * @return true if the value is present in the attribute
     * @throws NamingException
     */
    public final static boolean containsValueCaseIgnore( Attribute attr, Object value )
    {
        // quick bypass test
        if ( attr.contains( value ) )
        {
            return true;
        }

        try
        {
            if ( value instanceof String )
            {
                String strVal = (String)value;

                NamingEnumeration attrVals = attr.getAll();

                while ( attrVals.hasMoreElements() )
                {
                    Object attrVal = attrVals.nextElement();

                    if ( attrVal instanceof String )
                    {
                        if ( strVal.equalsIgnoreCase( (String)attrVal ) )
                        {
                            return true;
                        }
                    }
                }
            }
            else
            {
                byte[] valueBytes = ( byte[] )value;

                NamingEnumeration attrVals = attr.getAll();

                while ( attrVals.hasMoreElements() )
                {
                    Object attrVal = attrVals.nextElement();

                    if ( attrVal instanceof byte[] )
                    {
                        if ( Arrays.equals( (byte[])attrVal, valueBytes ) )
                        {
                            return true;
                        }

                    }
                }
            }
        }
        catch (NamingException ne )
        {
            return false;
        }

        return false;
    }


    public static boolean containsAnyValues( Attribute attr, Object[] compared, AttributeType type )
        throws NamingException
    {
        // quick bypass test
        for ( int ii = 0; ii < compared.length; ii++ )
        {
            if ( attr.contains( compared ) )
            {
                return true;
            }
        }
        
        Normalizer normalizer = type.getEquality().getNormalizer();

        if ( type.getSyntax().isHumanReadable() )
        {
            for ( int jj = 0; jj < compared.length; jj++ )
            {
                String comparedStr = ( String ) normalizer.normalize( compared[jj] );
                for ( int ii = attr.size(); ii >= 0; ii-- )
                {
                    String value = ( String ) attr.get( ii );
                    if ( comparedStr.equals( normalizer.normalize( value ) ) )
                    {
                        return true;
                    }
                }
            }
        }
        else
        {
            for ( int jj = 0; jj < compared.length; jj++ )
            {
                byte[] comparedBytes = ( byte[] ) compared[jj];
                for ( int ii = attr.size(); ii >= 0; ii-- )
                {
                    if ( ArrayUtils.isEquals( comparedBytes, attr.get( ii ) ) )
                    {
                        return true;
                    }
                }
            }
        }

        return false;
    }


    /**
     * Creates a new attribute which contains the values representing the
     * difference of two attributes. If both attributes are null then we cannot
     * determine the attribute ID and an {@link IllegalArgumentException} is
     * raised. Note that the order of arguments makes a difference.
     * 
     * @param attr0
     *            the first attribute
     * @param attr1
     *            the second attribute
     * @return a new attribute with the difference of values from both attribute
     *         arguments
     * @throws NamingException
     *             if there are problems accessing attribute values
     */
    public static Attribute getDifference( Attribute attr0, Attribute attr1 ) throws NamingException
    {
        String id;

        if ( attr0 == null && attr1 == null )
        {
            throw new IllegalArgumentException( "Cannot figure out attribute ID if both args are null" );
        }
        else if ( attr0 == null )
        {
            return new AttributeImpl( attr1.getID() );
        }
        else if ( attr1 == null )
        {
            return ( Attribute ) attr0.clone();
        }
        else if ( !attr0.getID().equalsIgnoreCase( attr1.getID() ) )
        {
            throw new IllegalArgumentException( "Cannot take difference of attributes with different IDs!" );
        }
        else
        {
            id = attr0.getID();
        }

        Attribute attr = new AttributeImpl( id );

        if ( attr0 != null )
        {
            for ( int ii = 0; ii < attr0.size(); ii++ )
            {
                attr.add( attr0.get( ii ) );
            }
        }

        if ( attr1 != null )
        {
            for ( int ii = 0; ii < attr1.size(); ii++ )
            {
                attr.remove( attr1.get( ii ) );
            }
        }

        return attr;
    }


    /**
     * Creates a new attribute which contains the values representing the union
     * of two attributes. If one attribute is null then the resultant attribute
     * returned is a copy of the non-null attribute. If both are null then we
     * cannot determine the attribute ID and an {@link IllegalArgumentException}
     * is raised.
     * 
     * @param attr0
     *            the first attribute
     * @param attr1
     *            the second attribute
     * @return a new attribute with the union of values from both attribute
     *         arguments
     * @throws NamingException
     *             if there are problems accessing attribute values
     */
    public static Attribute getUnion( Attribute attr0, Attribute attr1 ) throws NamingException
    {
        String id;

        if ( attr0 == null && attr1 == null )
        {
            throw new IllegalArgumentException( "Cannot figure out attribute ID if both args are null" );
        }
        else if ( attr0 == null )
        {
            id = attr1.getID();
        }
        else if ( attr1 == null )
        {
            id = attr0.getID();
        }
        else if ( !attr0.getID().equalsIgnoreCase( attr1.getID() ) )
        {
            throw new IllegalArgumentException( "Cannot take union of attributes with different IDs!" );
        }
        else
        {
            id = attr0.getID();
        }

        Attribute attr = new AttributeImpl( id );

        if ( attr0 != null )
        {
            for ( int ii = 0; ii < attr0.size(); ii++ )
            {
                attr.add( attr0.get( ii ) );
            }
        }

        if ( attr1 != null )
        {
            for ( int ii = 0; ii < attr1.size(); ii++ )
            {
                attr.add( attr1.get( ii ) );
            }
        }

        return attr;
    }

    /**
     * Check if the attributes is a BasicAttributes, and if so, switch
     * the case sensitivity to false to avoid tricky problems in the server.
     * (Ldap attributeTypes are *always* case insensitive)
     * 
     * @param attributes The Attributes to check
     */
    public static Attributes toCaseInsensitive( Attributes attributes )
    {
        if ( attributes == null )
        {
            return attributes;
        }
        
        if ( attributes instanceof BasicAttributes )
        {
            if ( attributes.isCaseIgnored() )
            {
                // Just do nothing if the Attributes is already case insensitive
                return attributes;
            }
            else
            {
                // Ok, bad news : we have to create a new BasicAttributes
                // whiwh will be case insensitive
                Attributes newAttrs = new BasicAttributes( true );
                
                NamingEnumeration attrs = attributes.getAll();
                
                if ( attrs != null )
                {
                    // Iterate through the attributes now
                    while ( attrs.hasMoreElements() )
                    {
                        newAttrs.put( (Attribute)attrs.nextElement() );
                    }
                }
                
                return newAttrs;
            }
        }
        else
        {
            // we can safely return the attributes if it's not a BasicAttributes
            return attributes;
        }
    }
    

    /**
     * Return a string representing the attributes with tabs in front of the
     * string
     * 
     * @param tabs
     *            Spaces to be added before the string
     * @param attribute
     *            The attribute to print
     * @return A string
     */
    public static String toString( String tabs, Attribute attribute )
    {
        StringBuffer sb = new StringBuffer();

        sb.append( tabs ).append( "Attribute\n" );

        if ( attribute != null )
        {
            sb.append( tabs ).append( "    Type : '" ).append( attribute.getID() ).append( "'\n" );

            for ( int j = 0; j < attribute.size(); j++ )
            {

                try
                {
                    Object attr = attribute.get( j );

                    if ( attr != null )
                    {
                        if ( attr instanceof String )
                        {
                            sb.append( tabs ).append( "        Val[" ).append( j ).append( "] : " ).append( attr ).append(
                                " \n" );
                        }
                        else if ( attr instanceof byte[] )
                        {
                            String string = StringTools.utf8ToString( ( byte[] ) attr );
    
                            sb.append( tabs ).append( "        Val[" ).append( j ).append( "] : " );
                            sb.append( string ).append( '/' );
                            sb.append( StringTools.dumpBytes( ( byte[] ) attr ) );
                            sb.append( " \n" );
                        }
                        else
                        {
                            sb.append( tabs ).append( "        Val[" ).append( j ).append( "] : " ).append( attr ).append(
                                " \n" );
                        }
                    }
                }
                catch ( NamingException ne )
                {
                    sb.append( "Bad attribute : " ).append( ne.getMessage() );
                }
            }
        }
        
        return sb.toString();
    }

    /**
     * Return a string representing the attribute
     * 
     * @param attribute
     *            The attribute to print
     * @return A string
     */
    public static String toString( Attribute attribute )
    {
        return toString( "", attribute );
    }

    /**
     * Return a string representing the attributes with tabs in front of the
     * string
     * 
     * @param tabs
     *            Spaces to be added before the string
     * @param attributes
     *            The attributes to print
     * @return A string
     */
    public static String toString( String tabs, Attributes attributes )
    {
        StringBuffer sb = new StringBuffer();

        sb.append( tabs ).append( "Attributes\n" );

        if ( attributes != null )
        {
            NamingEnumeration attributesIterator = attributes.getAll();
    
            while ( attributesIterator.hasMoreElements() )
            {
                Attribute attribute = ( Attribute ) attributesIterator.nextElement();
    
                sb.append( tabs ).append( attribute.toString() );
            }
        }
        
        return sb.toString();
    }
    
    /**
     * Parse attribute's options :
     * 
     * options = *( ';' option )
     * option = 1*keychar
     * keychar = 'a'-z' | 'A'-'Z' / '0'-'9' / '-'
     */
    private static void parseOptions( String str, Position pos ) throws ParseException
    {
        while ( StringTools.isCharASCII( str, pos.start, ';' ) )
        {
            pos.start++;
            
            // We have an option
            if ( !StringTools.isAlphaDigitMinus( str, pos.start ) )
            {
                // We must have at least one keychar
                throw new ParseException( "An empty option is not allowed", pos.start );
            }
            
            pos.start++;
            
            while ( StringTools.isAlphaDigitMinus( str, pos.start ) )
            {
                pos.start++;
            }
        }
    }
    
    /**
     * Parse a number :
     * 
     * number = '0' | '1'..'9' digits
     * digits = '0'..'9'*
     * 
     * @return true if a number has been found
     */
    private static boolean parseNumber( String filter, Position pos )
    {
        char c = StringTools.charAt( filter, pos.start );
        
        switch ( c )
        {
            case '0' :
                // If we get a starting '0', we should get out
                pos.start++;
                return true;
                
            case '1' : 
            case '2' : 
            case '3' : 
            case '4' : 
            case '5' : 
            case '6' : 
            case '7' : 
            case '8' : 
            case '9' : 
                pos.start++;
                break;
                
            default :
                // Not a number.
                return false;
        }
        
        while ( StringTools.isDigit( filter, pos.start ) )
        {
            pos.start++;
        }
        
        return true;
    }

    
    /**
     * 
     * Parse an OID.
     *
     * numericoid = number 1*( '.' number )
     * number = '0'-'9' / ( '1'-'9' 1*'0'-'9' )
     *
     * @param str The OID to parse
     * @param pos The current position in the string
     * @return A valid OID
     * @throws ParseException If we don't have a valid OID
     */
    public static void parseOID( String str, Position pos ) throws ParseException
    {
        // We have an OID
        parseNumber( str, pos );
        
        // We must have at least one '.' number
        if ( StringTools.isCharASCII( str, pos.start, '.' ) == false )
        {
            throw new ParseException( "Invalid OID, missing '.'", pos.start );
        }
        
        pos.start++;
        
        if ( parseNumber( str, pos ) == false )
        {
            throw new ParseException( "Invalid OID, missing a number after a '.'", pos.start );
        }
        
        while ( true )
        {
            // Break if we get something which is not a '.'
            if ( StringTools.isCharASCII( str, pos.start, '.' ) == false )
            {
                break;
            }
            
            pos.start++;
            
            if ( parseNumber( str, pos ) == false )
            {
                throw new ParseException( "Invalid OID, missing a number after a '.'", pos.start );
            }
        }
    }
    
    /**
     * Parse an attribute. The grammar is :
     * attributedescription = attributetype options
     * attributetype = oid
     * oid = descr / numericoid
     * descr = keystring
     * numericoid = number 1*( '.' number )
     * options = *( ';' option )
     * option = 1*keychar
     * keystring = leadkeychar *keychar
     * leadkeychar = 'a'-z' | 'A'-'Z'
     * keychar = 'a'-z' | 'A'-'Z' / '0'-'9' / '-'
     * number = '0'-'9' / ( '1'-'9' 1*'0'-'9' )
     *
     * @param str The parsed attribute,
     * @param pos The position of the attribute in the current string
     * @return The parsed attribute if valid
     */
    public static String parseAttribute( String str, Position pos, boolean withOption ) throws ParseException
    {
        // We must have an OID or an DESCR first
        char c = StringTools.charAt( str, pos.start );
        
        if ( c == '\0' )
        {
            throw new ParseException( "Empty attributes", pos.start );
        }
        
        int start = pos.start;

        if ( StringTools.isAlpha( c ) )
        {
            // A DESCR
            pos.start++;
            
            while ( StringTools.isAlphaDigitMinus( str, pos.start ) )
            {
                pos.start++;
            }

            // Parse the options if needed
            if ( withOption )
            {
                parseOptions( str, pos );
            }
            
            return str.substring( start, pos.start );
        }
        else if ( StringTools.isDigit( c ) )
        {
            // An OID
            pos.start++;
            
            // Parse the OID
            parseOID( str, pos );
            
            // Parse the options
            if ( withOption )
            {
                parseOptions( str, pos );
            }
            
            return str.substring( start, pos.start );
        }
        else
        {
            throw new ParseException( "Bad char in attribute", pos.start );
        }
    }


    /**
     * Return a string representing the attributes
     * 
     * @param attributes
     *            The attributes to print
     * @return A string
     */
    public static String toString( Attributes attributes )
    {
        return toString( "", attributes );
    }
    
    
    /**
     * Convert a BasicAttribute or an AttributeImpl instance to a
     * ServerAttribute instance
     *
     * @param attribute The attribute to convert
     * @return A ServerAttributeImpl instance 
     */
    public static ServerAttribute convertAttribute( Attribute attribute ) throws NamingException
    {
        assert( attribute != null );
        
        ServerAttribute serverAttribute = new ServerAttributeImpl( attribute.getID() );
        NamingEnumeration<?> values = attribute.getAll();
        
        while ( values.hasMoreElements() )
        {
            Object value = values.nextElement();
            
            if ( value instanceof String )
            {
                serverAttribute.add( (String)value );
            }
            else if ( value instanceof byte[] )
            {
                serverAttribute.add( (byte[])value );
            }
            else
            {
                serverAttribute.add( (String)null );
            }
        }
        
        return serverAttribute;
    }
    
    
    /**
     * Convert an instance of Attributes to an instance of ServerEntry
     *
     * @param attributes The instance to convert
     * @return An instance of ServerEntryImpl
     */
    public static ServerEntry convertEntry( LdapDN dn, Attributes attributes ) throws NamingException
    {
        assert( dn != null );
        assert( attributes != null );
        
        ServerEntry serverEntry = new ServerEntryImpl( dn );
        
        NamingEnumeration<? extends Attribute> attrs = attributes.getAll();
        
        while ( attrs.hasMoreElements() )
        {
            Attribute attribute = attrs.nextElement();
            serverEntry.put( convertAttribute( attribute ) );
        }
        
        return serverEntry;
    }
}
