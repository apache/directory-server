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


import java.util.Comparator;
import java.util.List;


/**
 * The interface for an AVL Tree.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public interface AvlTree<K>
{

    /**
     * @return the comparator associated with this tree 
     */
    abstract Comparator<K> getComparator();


    /**
     * Inserts a LinkedAvlNode with the given key.
     *
     * @param key the item to be inserted
     * @return the replaced key if it already exists
     * Note: Ignores if a node with the given key already exists.
     */
    abstract K insert( K key );


    /**
     * Removes the LinkedAvlNode present in the tree with the given key value
     *
     * @param key the value of the node to be removed
     * @return the removed key, if any, or null if the key does not exist
     */
    abstract K remove( K key );


    /**
     * Tests if the tree is logically empty.
     * 
     * @return true if the tree is empty, false otherwise
     */
    abstract boolean isEmpty();


    /**
     * returns the number of nodes present in this tree.
     * 
     * @return the number of nodes present in this tree
     */
    //NOTE: This method is internally used by AVLTreeMarshaller
    abstract int getSize();


    /**
     * @return the root element of this tree (ie, not the first, but the
     * topmost element)
     */
    abstract LinkedAvlNode<K> getRoot();


    /**
     * @return a list of the stored keys in this tree
     */
    abstract List<K> getKeys();


    /**
     * Prints the contents of AVL tree in pretty format
     */
    abstract void printTree();


    /**
     * @return The first element of this tree
     */
    abstract LinkedAvlNode<K> getFirst();


    /**
     * @return The last element in this tree
     */
    abstract LinkedAvlNode<K> getLast();


    /**
     * Finds a LinkedAvlNode<K> whose key is higher than the given key.
     *
     * @param key the key
     * @return the LinkedAvlNode<K> whose key is greater than the given key ,<br>
     *         null if there is no node with a higher key than the given key.
     */
    abstract LinkedAvlNode<K> findGreater( K key );


    /**
     * Finds a LinkedAvlNode<K> whose key is higher than the given key.
     *
     * @param key the key
     * @return the LinkedAvlNode<K> whose key is greater than the given key ,<br>
     *         null if there is no node with a higher key than the given key.
     */
    abstract LinkedAvlNode<K> findGreaterOrEqual( K key );


    /**
     * Finds a LinkedAvlNode<K> whose key is lower than the given key.
     *
     * @param key the key
     * @return the LinkedAvlNode<K> whose key is lower than the given key ,<br>
     *         null if there is no node with a lower key than the given key.
     */
    abstract LinkedAvlNode<K> findLess( K key );


    /**
     * Finds a LinkedAvlNode<K> whose key is lower than the given key.
     *
     * @param key the key
     * @return the LinkedAvlNode<K> whose key is lower than the given key ,<br>
     *         null if there is no node with a lower key than the given key.
     */
    abstract LinkedAvlNode<K> findLessOrEqual( K key );


    /**
     * 
     * Find a LinkedAvlNode with the given key value in the tree.
     *
     * @param key the key to find
     * @return the list of traversed LinkedAvlNode.
     */
    abstract LinkedAvlNode<K> find( K key );
}