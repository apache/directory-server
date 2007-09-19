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
    protected Map<String, Object> annotations;

    /**
     * Creates a node by setting abstract node type.
     */
    protected AbstractExprNode()
    {
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
    protected Map<String, Object> getAnnotations()
    {
        return annotations;
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
