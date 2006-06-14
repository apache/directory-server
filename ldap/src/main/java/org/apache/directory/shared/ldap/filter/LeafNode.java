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


/**
 * Abstract base class for leaf nodes within the expression filter tree.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public abstract class LeafNode extends AbstractExprNode
{
    /** attribute on which this leaf is based */
    private String attribute;


    /**
     * Creates a leaf node.
     * 
     * @param attribute the attribute this node is based on
     * @param type the type of this leaf node
     */
    protected LeafNode( String attribute, int type )
    {
        super( type );
        this.attribute = attribute;
    }


    /**
     * Gets whether this node is a leaf - the answer is always true here.
     * 
     * @return true always
     */
    public final boolean isLeaf()
    {
        return true;
    }


    /**
     * Gets the attribute this leaf node is based on.
     * 
     * @return the attribute asserted
     */
    public final String getAttribute()
    {
        return attribute;
    }
    
    
    /**
     * Sets the attribute this leaf node is based on.
     * 
     * @param attribute the attribute that is asserted by this filter node
     */
    public void setAttribute( String attribute )
    {
        this.attribute = attribute;
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

        if ( !( other instanceof LeafNode ) )
        {
            return false;
        }

        if ( !super.equals( other ) )
        {
            return false;
        }

        return attribute.equals( ( ( LeafNode ) other ).getAttribute() );
    }
}
