/*
 * $Id: EntryNode.java,v 1.3 2003/03/13 18:27:22 akarasulu Exp $
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
import java.util.HashMap;
import org.apache.ldap.common.filter.ExprNode ;
import org.apache.eve.backend.jdbm.search.SearchEngine;


/**
 * A node representing an entry.
 */
public class EntryNode
	implements TreeNode
{
    private final Database m_db ;
    private final EntryNode m_parent ;
	private final LdapEntryImpl m_entry ;
    private final ArrayList m_children ;


    public EntryNode(EntryNode a_parent, Database a_db, LdapEntryImpl a_entry,
        HashMap a_map)
    {
        this(a_parent, a_db, a_entry, a_map, null, null) ;
    }


    public EntryNode(EntryNode a_parent, Database a_db, LdapEntryImpl a_entry,
        HashMap a_map, ExprNode a_exprNode, SearchEngine a_engine)
    {
        m_db = a_db ;
		m_entry = a_entry ;
        m_children = new ArrayList() ;

        if(a_parent == null) {
            m_parent = this ;
        } else {
            m_parent = a_parent ;
        }

		try {
            ArrayList l_records = new ArrayList() ;
            Cursor l_cursor = m_db.getChildren(m_entry.getEntryID()) ;
            while(l_cursor.hasMore()) {
                IndexRecord l_old = (IndexRecord) l_cursor.next() ;
                IndexRecord l_new = new IndexRecord() ;
                l_new.setEntryId(l_old.getEntryId()) ;
                l_new.setIndexKey(l_old.getIndexKey()) ;
                l_records.add(l_new) ;
            }
            l_cursor.close() ;

            Iterator l_list = l_records.iterator() ;
            while(l_list.hasNext()) {
                IndexRecord l_rec = (IndexRecord) l_list.next() ;

                // Avoids root node which is both a parent and child of itself.
                if(l_rec.getEntryId().equals(m_entry.getEntryID())) {
                    continue ;
                }

                if(a_engine != null && a_exprNode != null) {
                    if(m_db.getChildCount(l_rec.getEntryId()) == 0) {
                        if(a_engine.assertExpression(m_db, a_exprNode,
                            l_rec.getEntryId()))
                        {
                            LdapEntryImpl l_entry = m_db.read(l_rec.getEntryId()) ;
                            EntryNode l_child =
                                new EntryNode(this, m_db, l_entry, a_map,
                                a_exprNode, a_engine) ;
                            m_children.add(l_child) ;
                        } else {
                            continue ;
                        }
                    } else {
                        LdapEntryImpl l_entry = m_db.read(l_rec.getEntryId()) ;
                        EntryNode l_child = new EntryNode(this, m_db, l_entry,
                            a_map, a_exprNode, a_engine) ;
                        m_children.add(l_child) ;
                    }
                } else {
                    LdapEntryImpl l_entry = m_db.read(l_rec.getEntryId()) ;
                    EntryNode l_child =
                        new EntryNode(this, m_db, l_entry, a_map) ;
                    m_children.add(l_child) ;
                }
            }
        } catch(Exception e) {
            e.printStackTrace() ;
        }

        a_map.put(a_entry.getEntryID(), this) ;
    }


    public Enumeration children()
    {
        return Collections.enumeration(m_children) ;
    }


    public boolean getAllowsChildren()
    {
        return true ;
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
        StringBuffer l_buf = new StringBuffer() ;
        l_buf.append(m_entry.getEntryDN()) ;
        if(m_children.size() > 0) {
            l_buf.append(" [").append(m_children.size()).append(']') ;
        }
        return l_buf.toString() ;
    }


    public LdapEntryImpl getLdapEntry()
    {
        return m_entry ;
    }
}
