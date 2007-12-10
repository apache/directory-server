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
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attributes;
import javax.swing.tree.TreeNode;

import org.apache.directory.server.core.partition.impl.btree.BTreePartition;
import org.apache.directory.server.core.partition.impl.btree.IndexRecord;
import org.apache.directory.server.core.partition.impl.btree.SearchEngine;
import org.apache.directory.shared.ldap.filter.ExprNode;
import org.apache.directory.shared.ldap.name.LdapDN;


/**
 * A node representing an entry.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class EntryNode implements TreeNode
{
    private final BTreePartition partition;
    private final EntryNode parent;
    private final Attributes entry;
    private final ArrayList<TreeNode> children;
    private final Long id;


    public EntryNode(Long id, EntryNode parent, BTreePartition partition, Attributes entry, Map<Long, EntryNode> map)
    {
        this( id, parent, partition, entry, map, null, null );
    }


    public EntryNode( Long id, EntryNode parent, BTreePartition db, Attributes entry, Map<Long, EntryNode> map,
        ExprNode exprNode, SearchEngine engine )
    {
        this.partition = db;
        this.id = id;
        this.entry = entry;
        children = new ArrayList<TreeNode>();

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
            List<IndexRecord> records = new ArrayList<IndexRecord>();
            NamingEnumeration childList = db.list( id );
            
            while ( childList.hasMore() )
            {
                IndexRecord old = ( IndexRecord ) childList.next();
                IndexRecord newRec = new IndexRecord();
                newRec.copy( old );
                records.add( newRec );
            }
            
            childList.close();

            Iterator list = records.iterator();

            while ( list.hasNext() )
            {
                IndexRecord rec = ( IndexRecord ) list.next();

                if ( engine != null && exprNode != null )
                {
                    if ( db.getChildCount( (Long)rec.getEntryId() ) == 0 )
                    {
                        if ( engine.evaluate( exprNode, (Long)rec.getEntryId() ) )
                        {
                            Attributes newEntry = db.lookup( (Long)rec.getEntryId() );
                            EntryNode child = new EntryNode( (Long)rec.getEntryId(), this, db, newEntry, map, exprNode,
                                engine );
                            children.add( child );
                        }
                        else
                        {
                            continue;
                        }
                    }
                    else
                    {
                        Attributes newEntry = db.lookup( (Long)rec.getEntryId() );
                        EntryNode child = new EntryNode( (Long)rec.getEntryId(), this, db, newEntry, map, exprNode, engine );
                        children.add( child );
                    }
                }
                else
                {
                    Attributes newEntry = db.lookup( (Long)rec.getEntryId() );
                    EntryNode child = new EntryNode( (Long)rec.getEntryId(), this, db, newEntry, map );
                    children.add( child );
                }
            }
        }
        catch ( Exception e )
        {
            e.printStackTrace();
        }

        map.put( id, this );
    }


    public Enumeration children()
    {
        return Collections.enumeration( children );
    }


    public boolean getAllowsChildren()
    {
        return true;
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


    public String getEntryDn() throws NamingException
    {
        return partition.getEntryDn( id );
    }


    public String toString()
    {
        StringBuffer buf = new StringBuffer();

        try
        {
            LdapDN dn = new LdapDN( partition.getEntryDn( id ) );
            buf.append( "(" ).append( id ).append( ") " );
            buf.append( dn.getRdn() );
        }
        catch ( NamingException e )
        {
            e.printStackTrace();
            buf.append( "ERROR: " + e.getMessage() );
        }

        if ( children.size() > 0 )
        {
            buf.append( " [" ).append( children.size() ).append( "]" );
        }

        return buf.toString();
    }


    public Attributes getLdapEntry()
    {
        return entry;
    }


    public Long getEntryId()
    {
        return id;
    }
}
