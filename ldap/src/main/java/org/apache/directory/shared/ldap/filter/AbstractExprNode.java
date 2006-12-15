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
    /** The map of annotations */
    private Map<String, Object> annotations;

    /** node type set to enumation of constants */
    private final AssertionEnum assertionType;


    /**
     * Creates an node by setting abstract node type.
     * 
     * @param a_type
     *            the type of this leaf node
     */
    protected AbstractExprNode( AssertionEnum type)
    {
        assertionType = type;
    }


    /**
     * @see org.apache.directory.shared.ldap.filter.ExprNode#get(java.lang.Object)
     */
    public Object get( Object key )
    {
        if ( null == annotations )
        {
            return null;
        }

        return annotations.get( key );
    }


    /**
     * @see org.apache.directory.shared.ldap.filter.ExprNode#set(java.lang.Object,
     *      java.lang.Object)
     */
    public void set( String key, Object value )
    {
        if ( null == annotations )
        {
            annotations = new HashMap<String, Object>( 2 );
        }

        annotations.put( key, value );
    }


    /**
     * Gets the annotations as a Map.
     * 
     * @return the annotation map.
     */
    protected Map getAnnotations()
    {
        return annotations;
    }


    /**
     * Gets the assertion type of this leaf node.
     * 
     * @return the assertion or node type
     */
    public AssertionEnum getAssertionType()
    {
        return assertionType;
    }


    /**
     * Returns the filter operator string associated with an assertion type.
     * 
     * @param a_assertionType
     *            the assertion type value
     * @return the string representation
     * TODO Refactor these classes to use an enumeration type
     */
    public static final String getOperationString( AssertionEnum assertionType )
    {
        String opstr = null;

        switch ( assertionType )
        {
            case APPROXIMATE :
                opstr = "~=";

                break;

            case EQUALITY :
                opstr = "=";

                break;

            case EXTENSIBLE :
                opstr = "extensible";

                break;

            case GREATEREQ :
                opstr = ">=";

                break;

            case LESSEQ :
                opstr = "<=";

                break;

            case PRESENCE :
                opstr = "=*";

                break;

            case SUBSTRING :
                opstr = "=";

                break;

            default:
                throw new IllegalArgumentException( "Attribute value assertion type is undefined." );
        }

        return opstr;
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

        if ( otherExprNode.getAssertionType() != assertionType )
        {
            return false;
        }

        Map otherAnnotations = otherExprNode.annotations;

        if ( otherAnnotations == annotations )
        {
            return true;
        }

        // return true if both are non-null and equals() is true

        return ( ( null != annotations ) && ( null != otherAnnotations ) && 
        		annotations.equals( otherAnnotations ) );
    }
}
