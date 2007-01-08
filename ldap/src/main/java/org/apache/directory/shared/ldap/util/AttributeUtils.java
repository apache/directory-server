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


import java.util.Arrays;

import org.apache.directory.shared.ldap.message.AttributeImpl;
import org.apache.directory.shared.ldap.message.AttributesImpl;
import org.apache.directory.shared.ldap.message.ModificationItemImpl;
import org.apache.directory.shared.ldap.schema.AttributeType;
import org.apache.directory.shared.ldap.schema.Normalizer;

import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;


/**
 * A set of utility fuctions for working with Attributes.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class AttributeUtils
{
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
        // optimization bypass to avoid cost of the loop below
        if ( type.getNames().length == 1 )
        {
            return attrs.get( type.getNames()[0] );
        }
        
        // check if the attribute's OID is used
        if ( attrs.get( type.getOid() ) != null )
        {
            return attrs.get( type.getOid() );
        }
        
        // iterate through aliases
        for ( int ii = 0; ii < type.getNames().length; ii++ )
        {
            if ( attrs.get( type.getNames()[ii] ) != null )
            {
                return attrs.get( type.getNames()[ii] );
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
    public final static Attribute getAttribute( ModificationItemImpl[] mods, AttributeType type )
    {
        // optimization bypass to avoid cost of the loop below
        if ( type.getNames().length == 1 )
        {
            for ( int jj = 0; jj < mods.length; jj++ )
            {
                if ( mods[jj].getAttribute().getID().equalsIgnoreCase( type.getNames()[0] ) )
                {
                    return mods[jj].getAttribute();
                }
            }
        }
        
        // check if the attribute's OID is used
        for ( int jj = 0; jj < mods.length; jj++ )
        {
            if ( mods[jj].getAttribute().getID().equals( type.getOid() ) )
            {
                return mods[jj].getAttribute();
            }
        }
        
        // iterate through aliases
        for ( int ii = 0; ii < type.getNames().length; ii++ )
        {
            for ( int jj = 0; jj < mods.length; jj++ )
            {
                if ( mods[jj].getAttribute().getID().equalsIgnoreCase( type.getNames()[ii] ) )
                {
                    return mods[jj].getAttribute();
                }
            }
        }
        
        return null;
    }
    
    
    public final static boolean containsValue( Attribute attr, Object compared, AttributeType type ) throws NamingException
    {
        // quick bypass test
        if ( attr.contains( compared ) )
        {
            return true;
        }
        
        Normalizer normalizer = type.getEquality().getNormalizer();

        if ( type.getSyntax().isHumanReadible() )
        {
            String comparedStr = ( String ) normalizer.normalize( compared );
            for ( int ii = attr.size() - 1; ii >= 0; ii-- )
            {
                String value = ( String ) attr.get( ii );
                if ( comparedStr.equals( normalizer.normalize( value ) ) )
                {
                    return true;
                }
            }
        }
        else
        {
            byte[] comparedBytes = ( byte[] ) compared;
            for ( int ii = attr.size() - 1; ii >= 0; ii-- )
            {
                if ( ArrayUtils.isEquals( comparedBytes, attr.get( ii ) ) )
                {
                    return true;
                }
            }
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

        if ( type.getSyntax().isHumanReadible() )
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
     * Return a string representing the attributes with tabs in front of the
     * string
     * 
     * @param tabs
     *            Spaces to be added before the string
     * @param attributes
     *            The attributes to print
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
     * Return a string representing the attributes
     * 
     * @param attributes
     *            The attributes to print
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
}
