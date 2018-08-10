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


/**
 * A linked AVL tree node with support to store value along with a key.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class LinkedAvlMapNode<K, V>
{
    /** The key stored in the node */
    K key;

    /** the value stored in the node */
    SingletonOrOrderedSet<V> value;

    /** The left child */
    LinkedAvlMapNode<K, V> left;

    /** The right child */
    LinkedAvlMapNode<K, V> right;

    /** The next node, superior to the current node */
    LinkedAvlMapNode<K, V> next;

    /** The previous node, inferior to the current node */
    LinkedAvlMapNode<K, V> previous;

    int depth;
    int index;

    boolean isLeft;
    int height = 1;


    /**
     * Creates a new instance of LinkedAvlNode, containing a given value.
     *
     * @param theKey the stored key on the topmost node
     * @param theValue The stored value on the topmost node
     */
    public LinkedAvlMapNode( K theKey, V theValue )
    {
        key = theKey;
        value = new SingletonOrOrderedSet<>( theValue );
        left = null;
        right = null;
    }


    public void setLeft( LinkedAvlMapNode<K, V> left )
    {
        this.left = left;
    }


    public void setRight( LinkedAvlMapNode<K, V> right )
    {
        this.right = right;
    }


    public LinkedAvlMapNode<K, V> getNext()
    {
        return next;
    }


    public LinkedAvlMapNode<K, V> getPrevious()
    {
        return previous;
    }


    public LinkedAvlMapNode<K, V> getLeft()
    {
        return left;
    }


    public LinkedAvlMapNode<K, V> getRight()
    {
        return right;
    }


    public K getKey()
    {
        return key;
    }


    public SingletonOrOrderedSet<V> getValue()
    {
        return value;
    }


    public boolean isLeaf()
    {
        return ( right == null && left == null );
    }


    public int getDepth()
    {
        return depth;
    }


    public void setDepth( int depth )
    {
        this.depth = depth;
    }


    public int getHeight()
    {
        return height;
    }


    public void setNext( LinkedAvlMapNode<K, V> next )
    {
        this.next = next;
    }


    public void setPrevious( LinkedAvlMapNode<K, V> previous )
    {
        this.previous = previous;
    }


    public int computeHeight()
    {

        if ( right == null && left == null )
        {
            height = 1;
            return height;
        }

        int lh, rh;

        if ( isLeft )
        {
            lh = ( left == null ? -1 : left.computeHeight() );
            rh = ( right == null ? -1 : right.getHeight() );
        }
        else
        {
            rh = ( right == null ? -1 : right.computeHeight() );
            lh = ( left == null ? -1 : left.getHeight() );
        }

        height = 1 + Math.max( lh, rh );

        return height;
    }


    public int getBalance()
    {
        int lh = ( left == null ? 0 : left.computeHeight() );
        int rh = ( right == null ? 0 : right.computeHeight() );

        return ( rh - lh );
    }


    public int getIndex()
    {
        return index;
    }


    public void setIndex( int index )
    {
        this.index = index;
    }


    @Override
    public String toString()
    {
        return "[" + key + ", [" + value + "]" + "]";
    }

}
