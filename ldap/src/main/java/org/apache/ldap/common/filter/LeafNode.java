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
package org.apache.ldap.common.filter;


/**
 * Abstract base class for leaf nodes within the expression filter tree. 
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public abstract class LeafNode extends AbstractExprNode
{
    /** attribute on which this leaf is based */
    private final String m_attribute ;

    /**
     * Creates a leaf node.
     *
     * @param a_attribute the attribute this node is based on
     * @param a_type the type of this leaf node
     */
    protected LeafNode( String a_attribute, int a_type )
    {
        super ( a_type ) ;

        m_attribute = a_attribute ;
    }


    /**
     * Gets whether this node is a leaf - the answer is always true here.
     *
     * @return true always
     */
    public final boolean isLeaf(  )
    {
        return true ;
    }


    /**
     * Gets the attribute this leaf node is based on.
     *
     * @return the attribute asserted
     */
    public final String getAttribute(  )
    {
        return m_attribute ;
    }


    /* (non-Javadoc)
     * @see java.lang.Object#equals(java.lang.Object)
     */
    public boolean equals(Object other)
    {
        if ( null == other )
        {
            return false;
        }

        if ( this == other )
        {
            return true;
        }
        
        if ( ! ( other instanceof LeafNode ) )
        {
            return false;
        }
        
        if ( ! super.equals( other ) )
        {
            return false;
        }

        return m_attribute.equals( ( ( LeafNode ) other ).getAttribute() );
    }
}
