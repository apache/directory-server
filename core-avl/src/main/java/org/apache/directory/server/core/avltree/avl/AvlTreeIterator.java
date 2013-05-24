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
package org.apache.directory.server.core.avltree.avl;


import java.util.Iterator;


/**
 * AVL Tree Set iterator
 * 
 * @author Vladimir Lysyy (http://bobah.net)
 *
 */
final class AvlTreeIterator<T extends Comparable<T>> implements Iterator<T>
{
    private AvlNode<T> root;
    private AvlNode<T> next = null;
    private boolean initial = true;


    public AvlTreeIterator( AvlNode<T> root )
    {
        this.root = root;
        find_next();
    }


    @Override
    public boolean hasNext()
    {
        return next != null || ( initial && root != null );
    }


    @Override
    public T next()
    {
        T value = next == null ? null : next.value;
        find_next();
        return value;
    }


    public void find_next()
    {
        if ( next == null )
        {
            if ( root == null || !initial )
            {
                return;
            }

            initial = false;
            next = root;

            while ( next.left != null )
            {
                next = next.left;
            }
        }
        else
        {
            if ( next.right != null )
            {
                next = next.right;

                while ( next.left != null )
                {
                    next = next.left;
                }
            }
            else
            {
                AvlNode<T> parent = next.parent;

                while ( parent != null && parent.left != next )
                {
                    next = parent;
                    parent = next.parent;
                }

                next = parent;
            }
        }
    }


    @Override
    public void remove()
    {
        assert false : "not supported";
    }
}