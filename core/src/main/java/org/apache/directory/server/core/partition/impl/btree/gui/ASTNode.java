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
package org.apache.directory.server.core.partition.impl.btree.gui;


import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;

import javax.swing.tree.TreeNode;

import org.apache.directory.shared.ldap.filter.BranchNode;
import org.apache.directory.shared.ldap.filter.ExprNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * A node representing an entry.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class ASTNode implements TreeNode
{
    private static final Logger log = LoggerFactory.getLogger( ASTNode.class );

    private final ASTNode parent;
    private final ExprNode exprNode;
    private final ArrayList children;


    public ASTNode(ASTNode parent, ExprNode exprNode)
    {
        this.children = new ArrayList( 2 );
        this.exprNode = exprNode;

        if ( parent == null )
        {
            this.parent = this;
        }
        else
        {
            this.parent = parent;
        }

        try
        {
            if ( exprNode.isLeaf() )
            {
                return;
            }

            BranchNode branch = ( BranchNode ) exprNode;
            ArrayList exprNodes = branch.getChildren();
            for ( int ii = 0; ii < exprNodes.size(); ii++ )
            {
                ExprNode child = ( ExprNode ) exprNodes.get( ii );
                children.add( new ASTNode( this, child ) );
            }
        }
        catch ( Exception e )
        {
            // FIXME What exception could be thrown here?
            log.warn( "Unexpected exception: parent=" + parent + ", exprNode=" + exprNode, e );
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
