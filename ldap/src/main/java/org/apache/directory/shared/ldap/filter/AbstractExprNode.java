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


import java.util.HashMap;
import java.util.Map;


/**
 * Abstract implementation of a expression node.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public abstract class AbstractExprNode implements ExprNode
{
    /** The map of annotations */
    protected Map<String, Object> annotations;

    /** The node type */
    protected final AssertionType assertionType;
    
    
    /**
     * Creates a node by setting abstract node type.
     * 
     * @param assertionType The node's type
     */
    protected AbstractExprNode( AssertionType assertionType )
    {
        this.assertionType = assertionType;
    }


    /**
     * @see ExprNode#getAssertionType()
     * 
     * @return the node's type
     */
    public AssertionType getAssertionType()
    {
        return assertionType;
    }


    /**
     * Tests to see if this node is a leaf or branch node.
     * 
     * @return true if the node is a leaf,false otherwise
     */
    public abstract boolean isLeaf();

    
    /**
     * @see Object#equals(Object)
     *@return <code>true</code> if both objects are equal 
     */
    public boolean equals( Object o )
    {
        // Shortcut for equals object
        if ( this == o )
        {
            return true;
        }
        
        if ( !( o instanceof AbstractExprNode ) )
        {
            return false;
        }
        
        AbstractExprNode that = (AbstractExprNode)o;
        
        // Check the node type
        if ( this.assertionType != that.assertionType )
        {
            return false;
        }
        
        if ( annotations == null )
        {
            return that.annotations == null;
        }
        else if ( that.annotations == null )
        {
            return false;
        }
        
        // Check all the annotation
        for ( String key:annotations.keySet() )
        {
            if ( !that.annotations.containsKey( key ) )
            {
                return false;
            }
            
            Object thisAnnotation = annotations.get( key ); 
            Object thatAnnotation = that.annotations.get( key );
            
            if ( thisAnnotation == null )
            {
                if ( thatAnnotation != null )
                {
                    return false;
                }
            }
            else
            {
                if ( !thisAnnotation.equals( thatAnnotation ) )
                {
                    return false;
                }
            }
        }
        
        return true;
    }


    /**
     * @see Object#hashCode()
     * @return the instance's hash code 
     */
    public int hashCode()
    {
        int h = 37;
        
        if ( annotations != null )
        {
            for ( String key:annotations.keySet() )
            {
                Object value = annotations.get( key );
                
                h = h*17 + key.hashCode();
                h = h*17 + ( value == null ? 0 : value.hashCode() );
            }
        }
        
        return h;
    }


    /**
     * @see org.apache.directory.shared.ldap.filter.ExprNode#get(java.lang.Object)
     * 
     * @return the annotation value.
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
    protected Map<String, Object> getAnnotations()
    {
        return annotations;
    }


    /**
     * Default implementation for this method : just throw an exception.
     * 
     * @param buf the buffer to append to.
     * @return The buffer in which the refinement has been appended
     * @throws UnsupportedOperationException if this node isn't a part of a refinement.
     */
    public StringBuilder printRefinementToBuffer( StringBuilder buf )
    {
        throw new UnsupportedOperationException( "ScopeNode can't be part of a refinement" );
    }


    public String toString()
    {
        if ( ( null != getAnnotations() ) && getAnnotations().containsKey( "count" ) )
        {
            return ":[" + getAnnotations().get( "count" ) + "]";
        }
        else 
        {
            return "";
        }
    }
}
