/*
 * $Id: ASTNode.java,v 1.2 2003/03/13 18:27:19 akarasulu Exp $
 *
 * -- (c) LDAPd Group                                                    --
 * -- Please refer to the LICENSE.txt file in the root directory of      --
 * -- any LDAPd project for copyright and distribution information.      --
 *
 */

package org.apache.eve.backend.jdbm.gui ;


import java.util.ArrayList ;
import java.util.Enumeration ;
import javax.swing.tree.TreeNode ;

import org.apache.eve.backend.jdbm.Database ;
import org.apache.eve.backend.jdbm.LdapEntryImpl ;
import org.apache.eve.backend.jdbm.index.IndexRecord;
import java.util.Collections;
import org.apache.eve.backend.Cursor;
import java.util.Iterator;
import org.apache.ldap.common.filter.ExprNode;
import org.apache.ldap.common.filter.BranchNode;
import org.apache.ldap.common.filter.LeafNode;
import org.apache.ldap.common.filter.PresenceNode;
import org.apache.ldap.common.filter.SubstringNode;


/**
 * A node representing an entry.
 */
public class ASTNode
	implements TreeNode
{
    private final ASTNode m_parent ;
	private final ExprNode m_exprNode ;
    private final ArrayList m_children ;


    public ASTNode(ASTNode a_parent, ExprNode a_exprNode)
    {
        m_children = new ArrayList(2) ;
        m_exprNode = a_exprNode ;

        if(a_parent == null) {
            m_parent = this ;
        } else {
            m_parent = a_parent ;
        }

		try {
            if(m_exprNode.isLeaf()) {
                return ;
            }

            BranchNode l_branch = (BranchNode) m_exprNode ;
            ArrayList l_exprNodes = l_branch.getChildren() ;
            for(int ii = 0 ; ii < l_exprNodes.size(); ii++) {
                ExprNode l_child = (ExprNode) l_exprNodes.get(ii) ;
                m_children.add(new ASTNode(this, l_child)) ;
            }
        } catch(Exception e) {
            e.printStackTrace() ;
        }
    }


    public Enumeration children()
    {
        return Collections.enumeration(m_children) ;
    }


    public boolean getAllowsChildren()
    {
        return !m_exprNode.isLeaf() ;
    }


    public TreeNode getChildAt(int a_childIndex)
    {
		return (TreeNode) m_children.get(a_childIndex) ;
    }


    public int getChildCount()
    {
        return m_children.size() ;
    }


    public int getIndex(TreeNode a_child)
    {
        return m_children.indexOf(a_child) ;
    }


    public TreeNode getParent()
    {
        return m_parent ;
    }


    public boolean isLeaf()
    {
        return m_children.size() <= 0 ;
    }


    public String toString()
    {
        return m_exprNode.toString() ;
    }


    public ExprNode getExprNode()
    {
        return m_exprNode ;
    }
}
