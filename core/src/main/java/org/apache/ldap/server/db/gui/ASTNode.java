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
package org.apache.ldap.server.db.gui;


import java.util.ArrayList;
import java.util.Enumeration;
import javax.swing.tree.TreeNode;

import java.util.Collections;
import org.apache.ldap.common.filter.ExprNode;
import org.apache.ldap.common.filter.BranchNode;


/**
 * A node representing an entry.
 *
 * @author <a href="mailto:directory-dev@incubator.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class ASTNode implements TreeNode
{
    private final ASTNode parent;
	private final ExprNode exprNode;
    private final ArrayList children;


    public ASTNode( ASTNode parent, ExprNode exprNode )
    {
        this.children = new ArrayList(2);
        this.exprNode = exprNode;

        if( parent == null )
        {
            this.parent = this;
        }
        else
        {
            this.parent = parent;
        }

		try
        {
            if( exprNode.isLeaf() )
            {
                return;
            }

            BranchNode branch = ( BranchNode ) exprNode;
            ArrayList exprNodes = branch.getChildren();
            for ( int ii = 0; ii < exprNodes.size(); ii++ )
            {
                ExprNode child = ( ExprNode ) exprNodes.get(ii);
                children.add( new ASTNode( this, child ) );
            }
        }
        catch( Exception e )
        {
            e.printStackTrace();
        }
    }


    public Enumeration children()
    {
        return Collections.enumeration( children );
    }


    public boolean getAllowsChildren()
    {
        return !exprNode.isLeaf();
    }


    public TreeNode getChildAt( int childIndex )
    {
		return ( TreeNode ) children.get( childIndex );
    }


    public int getChildCount()
    {
        return children.size();
    }


    public int getIndex( TreeNode child )
    {
        return children.indexOf( child );
    }


    public TreeNode getParent()
    {
        return parent;
    }


    public boolean isLeaf()
    {
        return children.size() <= 0;
    }


    public String toString()
    {
        return exprNode.toString();
    }


    public ExprNode getExprNode()
    {
        return exprNode;
    }
}
