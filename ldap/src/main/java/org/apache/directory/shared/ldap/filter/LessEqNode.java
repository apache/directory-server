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
 * A assertion value node for LessOrEqual.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Revision: 519266 $
 */
public class LessEqNode extends SimpleNode
{
    /**
     * Creates a new LessEqNode object.
     * 
     * @param attribute the attribute name
     * @param value the value to test for
     */
    public LessEqNode( String attribute, byte[] value )
    {
        super( attribute, value );
    }


    /**
     * Creates a new LessEqNode object.
     * 
     * @param attribute the attribute name
     * @param value the value to test for
     */
    public LessEqNode( String attribute, String value )
    {
        super( attribute, value );
    }

    /**
     * @see org.apache.directory.shared.ldap.filter.ExprNode#printToBuffer(
     *      java.lang.StringBuilder)
     */
    public StringBuilder printToBuffer( StringBuilder buf )
    {
        buf.append( '(' ).append( getAttribute() ).append( "<=" ).append( value ).append( ')' );

        return super.printToBuffer( buf );
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
        if ( this == other )
        {
            return true;
        }

        if ( !( other instanceof LessEqNode ) )
        {
            return false;
        }

        LessEqNode otherNode = (LessEqNode) other; 
        
        return ( value == null ? otherNode.value == null : value.equals( otherNode.value ) );
    }
}
