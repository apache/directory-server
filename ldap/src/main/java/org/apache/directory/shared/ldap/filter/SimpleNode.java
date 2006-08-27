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
package org.apache.directory.shared.ldap.filter;


/**
 * A simple assertion value node.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Revision$
 */
public class SimpleNode extends LeafNode
{
    /** the value */
    private Object value;


    /**
     * Creates a new SimpleNode object.
     * 
     * @param attribute
     *            the attribute name
     * @param value
     *            the value to test for
     * @param type
     *            the type of the assertion
     */
    public SimpleNode( String attribute, byte[] value, int type )
    {
//        this( attribute, StringTools.utf8ToString( value ), type );
        super( attribute, type );
        this.value = value;

        switch ( type )
        {
            case ( APPROXIMATE ):
                break;

            case ( EQUALITY ):
                break;

            case ( EXTENSIBLE ):
                throw new IllegalArgumentException( "Assertion type supplied is "
                    + "extensible.  Use ExtensibleNode instead." );

            case ( GREATEREQ ):
                break;

            case ( LESSEQ ):
                break;

            case ( PRESENCE ):
                throw new IllegalArgumentException( "Assertion type supplied is "
                    + "presence.  Use PresenceNode instead." );

            case ( SUBSTRING ):
                throw new IllegalArgumentException( "Assertion type supplied is "
                    + "substring.  Use SubstringNode instead." );

            default:
                throw new IllegalArgumentException( "Attribute value assertion type is undefined." );
        }
    }


    /**
     * Creates a new SimpleNode object.
     * 
     * @param attribute
     *            the attribute name
     * @param value
     *            the value to test for
     * @param type
     *            the type of the assertion
     */
    public SimpleNode( String attribute, String value, int type )
    {
        super( attribute, type );
        this.value = value;

        switch ( type )
        {
            case ( APPROXIMATE ):
                break;

            case ( EQUALITY ):
                break;

            case ( EXTENSIBLE ):
                throw new IllegalArgumentException( "Assertion type supplied is "
                    + "extensible.  Use ExtensibleNode instead." );

            case ( GREATEREQ ):
                break;

            case ( LESSEQ ):
                break;

            case ( PRESENCE ):
                throw new IllegalArgumentException( "Assertion type supplied is "
                    + "presence.  Use PresenceNode instead." );

            case ( SUBSTRING ):
                throw new IllegalArgumentException( "Assertion type supplied is "
                    + "substring.  Use SubstringNode instead." );

            default:
                throw new IllegalArgumentException( "Attribute value assertion type is undefined." );
        }
    }


    /**
     * Gets the value.
     * 
     * @return the value
     */
    public final Object getValue()
    {
        return value;
    }


    /**
     * Sets the value of this node.
     * 
     * @param value the value for this node
     */
    public void setValue( Object value )
    {
        this.value = value;
    }


    /**
     * @see org.apache.directory.shared.ldap.filter.ExprNode#printToBuffer(
     *      java.lang.StringBuffer)
     */
    public StringBuffer printToBuffer( StringBuffer buf )
    {
        buf.append( '(' ).append( getAttribute() );

        switch ( getAssertionType() )
        {
            case ( APPROXIMATE ):
                buf.append( "~=" );
                break;

            case ( EQUALITY ):
                buf.append( "=" );
                break;

            case ( GREATEREQ ):
                buf.append( ">=" );
                break;

            case ( LESSEQ ):
                buf.append( "<=" );
                break;

            default:
                buf.append( "UNKNOWN" );
        }

        buf.append( value );
        buf.append( ')' );

        if ( ( null != getAnnotations() ) && getAnnotations().containsKey( "count" ) )
        {
            buf.append( '[' );
            buf.append( getAnnotations().get( "count" ).toString() );
            buf.append( "] " );
        }
        else
        {
            buf.append( ' ' );
        }

        return buf;
    }


    /**
     * @see java.lang.Object#toString()
     */
    public String toString()
    {
        StringBuffer buf = new StringBuffer();
        printToBuffer( buf );
        return ( buf.toString() );
    }


    /**
     * @see org.apache.directory.shared.ldap.filter.ExprNode#accept(
     *      org.apache.directory.shared.ldap.filter.FilterVisitor)
     */
    public void accept( FilterVisitor visitor )
    {
        if ( visitor.canVisit( this ) )
        {
            visitor.visit( this );
        }
    }


    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#equals(java.lang.Object)
     */
    public boolean equals( Object other )
    {
        if ( null == other )
        {
            return false;
        }

        if ( this == other )
        {
            return true;
        }

        if ( !( other instanceof SimpleNode ) )
        {
            return false;
        }

        if ( !super.equals( other ) )
        {
            return false;
        }

        return value.equals( ( ( SimpleNode ) other ).getValue() );
    }
}
