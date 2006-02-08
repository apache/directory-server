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
package org.apache.directory.shared.ldap.filter;


import java.util.Map;
import java.util.HashMap;


/**
 * Abstract implementation of a expression node.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public abstract class AbstractExprNode implements ExprNode
{
    /** equality assertion node */
    public static final int EQUALITY = 0;

    /** presence assertion node */
    public static final int PRESENCE = 1;

    /** substring match assertion node */
    public static final int SUBSTRING = 2;

    /** greater than or equal to assertion node */
    public static final int GREATEREQ = 3;

    /** less than or equal to assertion node */
    public static final int LESSEQ = 4;

    /** approximate assertion node */
    public static final int APPROXIMATE = 5;

    /** extensible match assertion node */
    public static final int EXTENSIBLE = 6;

    /** scope assertion node */
    public static final int SCOPE = 7;

    /** Predicate assertion node */
    public static final int ASSERTION = 8;

    /** OR operator constant */
    public static final int OR = 9;

    /** AND operator constant */
    public static final int AND = 10;

    /** NOT operator constant */
    public static final int NOT = 11;

    /** The map of annotations */
    private Map m_annotations;

    /** node type set to enumation of constants */
    private final int m_assertionType;


    /**
     * Creates an node by setting abstract node type.
     * 
     * @param a_type
     *            the type of this leaf node
     */
    protected AbstractExprNode(int a_type)
    {
        m_assertionType = a_type;

        switch ( m_assertionType )
        {
            case ( APPROXIMATE ):
                break;

            case ( EQUALITY ):
                break;

            case ( EXTENSIBLE ):
                break;

            case ( GREATEREQ ):
                break;

            case ( LESSEQ ):
                break;

            case ( PRESENCE ):
                break;

            case ( SUBSTRING ):
                break;

            case ( SCOPE ):
                break;

            case ( ASSERTION ):
                break;

            case ( OR ):
                break;

            case ( AND ):
                break;

            case ( NOT ):
                break;

            default:
                throw new IllegalArgumentException( "Attribute value assertion type is undefined." );
        }
    }


    /**
     * @see org.apache.directory.shared.ldap.filter.ExprNode#get(java.lang.Object)
     */
    public Object get( Object a_key )
    {
        if ( null == getAnnotations() )
        {
            return null;
        }

        return getAnnotations().get( a_key );
    }


    /**
     * @see org.apache.directory.shared.ldap.filter.ExprNode#set(java.lang.Object,
     *      java.lang.Object)
     */
    public void set( Object a_key, Object a_value )
    {
        if ( null == getAnnotations() )
        {
            m_annotations = new HashMap( 2 );
        }

        getAnnotations().put( a_key, a_value );
    }


    /**
     * Gets the annotations as a Map.
     * 
     * @return the annotation map.
     */
    protected Map getAnnotations()
    {
        return m_annotations;
    }


    /**
     * Gets the assertion type of this leaf node.
     * 
     * @return the assertion or node type
     */
    public final int getAssertionType()
    {
        return m_assertionType;
    }


    /**
     * Returns the filter operator string associated with an assertion type.
     * 
     * @param a_assertionType
     *            the assertion type value
     * @return the string representation
     * @todo Refactor these classes to use an enumeration type
     */
    public static final String getOperationString( int a_assertionType )
    {
        String l_opstr = null;

        switch ( a_assertionType )
        {
            case ( APPROXIMATE ):
                l_opstr = "~=";

                break;

            case ( EQUALITY ):
                l_opstr = "=";

                break;

            case ( EXTENSIBLE ):
                l_opstr = "extensible";

                break;

            case ( GREATEREQ ):
                l_opstr = ">=";

                break;

            case ( LESSEQ ):
                l_opstr = "<=";

                break;

            case ( PRESENCE ):
                l_opstr = "=*";

                break;

            case ( SUBSTRING ):
                l_opstr = "=";

                break;

            default:
                throw new IllegalArgumentException( "Attribute value assertion type is undefined." );
        }

        return l_opstr;
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

        if ( !( other instanceof AbstractExprNode ) )
        {
            return false;
        }

        AbstractExprNode otherExprNode = ( AbstractExprNode ) other;

        if ( otherExprNode.getAssertionType() != m_assertionType )
        {
            return false;
        }

        Map otherAnnotations = otherExprNode.getAnnotations();

        if ( otherAnnotations == m_annotations )
        {
            return true;
        }

        // return true if both are non-null and equals() is true

        return ( ( null != m_annotations && null != otherAnnotations ) && m_annotations.equals( otherAnnotations ) );
    }
}
