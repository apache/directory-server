/*
 *   Copyright 2004 The Apache Software Foundation
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
package org.apache.directory.shared.ldap.util;


import org.apache.directory.shared.ldap.message.LockableAttributeImpl;
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
    public static boolean containsValue( Attribute attr, Object compared, AttributeType type ) throws NamingException
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
            return new LockableAttributeImpl( attr1.getID() );
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

        Attribute attr = new LockableAttributeImpl( id );

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

        Attribute attr = new LockableAttributeImpl( id );

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
    public static String toString( String tabs, Attributes attributes )
    {
        StringBuffer sb = new StringBuffer();

        sb.append( tabs ).append( "Attributes\n" );

        NamingEnumeration attributesIterator = attributes.getAll();

        while ( attributesIterator.hasMoreElements() )
        {
            Attribute attribute = ( Attribute ) attributesIterator.nextElement();

            sb.append( tabs ).append( "    Type : '" ).append( attribute.getID() ).append( "'\n" );

            for ( int j = 0; j < attribute.size(); j++ )
            {

                try
                {
                    Object attr = attribute.get( j );

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
    public static String toString( Attributes attributes )
    {
        return toString( "", attributes );
    }
}
