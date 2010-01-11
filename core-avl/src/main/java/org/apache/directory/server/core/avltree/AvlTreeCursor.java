/*
 *   Licensed to the Apache Software Foundation (ASF) under one
 *   or more contributor license agreements.  See the NOTICE file
 *   distributed with this work for additional information
 *   regarding copyright ownership.  The ASF licenses this file
 *   to you under the Apache License, Version 2.0 (the
 *   "License"); you may not use this file except in compliance
 *   with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing,
 *   software distributed under the License is distributed on an
 *   "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *   KIND, either express or implied.  See the License for the
 *   specific language governing permissions and limitations
 *   under the License.
 *
 */
package org.apache.directory.server.core.avltree;


import org.apache.directory.shared.ldap.cursor.AbstractCursor;
import org.apache.directory.shared.ldap.cursor.InvalidCursorPositionException;


/**
 * A Cursor for an AvlTree.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class AvlTreeCursor<K> extends AbstractCursor<K>
{
    private AvlTree<K> tree;
    private LinkedAvlNode<K> node;
    private boolean onNode = false;
    private boolean isAfterLast = false;
    private boolean isBeforeFirst = true;
 
    
    public AvlTreeCursor( AvlTree<K> tree )
    {
        this.tree = tree;
    }

    
    public void after( K element ) throws Exception 
    {
        checkNotClosed( "after" );

        if ( element == null )
        {
            afterLast();
            return;
        }

        LinkedAvlNode<K> found = tree.findGreater( element );
        
        if ( found == null )
        {
            node = tree.getLast();
            onNode = false;
            isAfterLast = true;
            isBeforeFirst = false;
            return;
        }

        node = found;
        isAfterLast = false;
        isBeforeFirst = false;
        onNode = false;
    }


    public void afterLast() throws Exception 
    {
        checkNotClosed( "afterLast" );
        node = tree.getLast();
        isBeforeFirst = false;
        isAfterLast = true;
        onNode = false;
    }


    public boolean available()
    {
        return onNode;
    }


    public void before( K element ) throws Exception 
    {
        checkNotClosed( "before" );

        if ( element == null )
        {
            beforeFirst();
            return;
        }

        LinkedAvlNode<K> found = tree.findLess( element );
        if ( found == null )
        {
            node = tree.getFirst();
            isAfterLast = false;
            isBeforeFirst = true;
        }
        else
        {
            node = found.next;
            isAfterLast = false;
            isBeforeFirst = false;
        }
        onNode = false;
    }


    public void beforeFirst() throws Exception 
    {
        checkNotClosed( "beforeFirst" );
        node = tree.getFirst();
        isBeforeFirst = true;
        isAfterLast = false;
        onNode = false;
    }


    public boolean first() throws Exception 
    {
        checkNotClosed( "first" );
        node = tree.getFirst();
        isBeforeFirst = false;
        isAfterLast = false;
        return onNode = node != null;
    }


    public K get() throws Exception 
    {
        checkNotClosed( "get" );
        if ( onNode )
        {
            return node.getKey();
        }
        
        throw new InvalidCursorPositionException();
    }


    public boolean isElementReused()
    {
        return true;
    }


    public boolean last() throws Exception 
    {
        checkNotClosed( "last" );
        node = tree.getLast();
        isBeforeFirst = false;
        isAfterLast = false;
        return onNode = node != null;
    }


    public boolean next() throws Exception 
    {
        checkNotClosed( "next" );
        
        if ( isBeforeFirst )
        {
            node = tree.getFirst();
            isBeforeFirst = false;
            isAfterLast = false;
            return onNode = node != null;
        }

        if ( isAfterLast )
        {
            return false;
        }
        else if ( onNode )
        {
            if ( node == null )
            {
                node = tree.getFirst();
                return true;
            }
            
            if ( node.next == null )
            {
                onNode = false;
                isAfterLast = true;
                isBeforeFirst = false;
                return false;
            }
            
            node = node.next;
            return true;
        }

        return node != null && ( onNode = true );
    }


    public boolean previous() throws Exception
    {
        checkNotClosed( "previous" );

        if ( isBeforeFirst )
        {
            return false;
        }

        if ( isAfterLast )
        {
            node = tree.getLast();
            isBeforeFirst = false;
            isAfterLast = false;
            return onNode = node != null;
        }

        if ( onNode )
        {
            if ( node == null )
            {
                node = tree.getLast();
                return true;
            }
            if ( node.previous == null )
            {
                onNode = false;
                isAfterLast = false;
                isBeforeFirst = true;
                return false;
            }
            
            node = node.previous;
            return true;
        }
        
        return false;
    }
}
