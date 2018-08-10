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
 * An interface to the AVL tree based map. The implementations
 * should hold a value(s) along with a key  
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public interface AvlTreeMap<K, V>
{

    /**
     * @return the key comparator associated with this tree 
     */
    Comparator<K> getKeyComparator();


    /**
     * @return the value comparator associated with this tree 
     */
    Comparator<V> getValueComparator();


    /**
     * Inserts a LinkedAvlMapNode with the given key and value.
     *
     * @param key the item to be inserted
     * @param value the value associated with the key
     * @return the replaced value if any exists else null
     * Note: Replaces a nodes value if duplicate keys are not allowed and the new value is
     *       not equal to the existing value.
     */
    V insert( K key, V value );


    /**
     * Removes the LinkedAvlMapNode present in the tree with the given key and value
     *
     * @param key the key of the node to be removed
     * @param value the value of the node
     * @return the removed value, if any, or null if the key or value does not exist
     * @throws IllegalArgumentException if key or value is null
     */
    V remove( K key, V value );


    /**
     * Removes a node associated with the given key
     * The entire node will be removed irrespective of whether duplicate keys
     * are enabled or not
     * 
     * @param key the key of the node to be removed
     * @return a SingletonOrOrderedSet
     * @throws IllegalArgumentException if key is null
     */
    SingletonOrOrderedSet<V> remove( K key );


    /**
     * Tests if the tree is logically empty.
     * 
     * @return true if the tree is empty, false otherwise
     */
    boolean isEmpty();


    /**
     * returns the number of nodes present in this tree.
     * 
     * @return the number of nodes present in this tree
     */
    int getSize();


    /**
     * @return the root element of this tree (i.e., not the first, but the
     * topmost element)
     */
    LinkedAvlMapNode<K, V> getRoot();


    /**
     * @return a list of the stored keys in this tree
     */
    List<K> getKeys();


    /**
     * Prints the contents of AVL tree in pretty format
     */
    void printTree();


    /**
     * @return The first element of this tree
     */
    LinkedAvlMapNode<K, V> getFirst();


    /**
     * @return The last element in this tree
     */
    LinkedAvlMapNode<K, V> getLast();


    /**
     * Finds a LinkedAvlMapNode whose key is higher than the given key.
     *
     * @param key the key
     * @return the LinkedAvlMapNode whose key is greater than the given key ,<br>
     *         null if there is no node with a higher key than the given key.
     */
    LinkedAvlMapNode<K, V> findGreater( K key );


    /**
     * Finds a LinkedAvlMapNode whose key is higher than the given key.
     *
     * @param key the key
     * @return the LinkedAvlMapNode whose key is greater than the given key ,<br>
     *         null if there is no node with a higher key than the given key.
     */
    LinkedAvlMapNode<K, V> findGreaterOrEqual( K key );


    /**
     * Finds a LinkedAvlMapNode whose key is lower than the given key.
     *
     * @param key the key
     * @return the LinkedAvlMapNode whose key is lower than the given key ,<br>
     *         null if there is no node with a lower key than the given key.
     */
    LinkedAvlMapNode<K, V> findLess( K key );


    /**
     * Finds a LinkedAvlMapNode whose key is lower than the given key.
     *
     * @param key the key
     * @return the LinkedAvlMapNode whose key is lower than the given key ,<br>
     *         null if there is no node with a lower key than the given key.
     */
    LinkedAvlMapNode<K, V> findLessOrEqual( K key );


    /**
     * 
     * Find a LinkedAvlMapNode with the given key value in the tree.
     *
     * @param key the key to find
     * @return the list of traversed LinkedAvlMapNode.
     */
    LinkedAvlMapNode<K, V> find( K key );


    /**
     * 
     * Find a LinkedAvlMapNode with the given key and value in the tree.
     *
     * @param key the key of the node
     * @param value the value of the node
     * @return LinkedAvlMapNode having the given key and value
     */
    LinkedAvlMapNode<K, V> find( K key, V value );


    /**
     * tells if the duplicate keys are supported or not. 
     *
     * @return true if duplicate keys are allowed, false otherwise
     */
    boolean isDupsAllowed();

}