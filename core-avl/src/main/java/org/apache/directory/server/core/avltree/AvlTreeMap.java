package org.apache.directory.server.core.avltree;


import java.util.Comparator;
import java.util.List;


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
     * @return the replaced key if it already exists
     * Note: Replaces a nodes value if duplicate keys are not allowed.
     */
    V insert( K key, V value );


    /**
     * Removes the LinkedAvlMapNode present in the tree with the given key and value
     *
     * @param key the key of the node to be removed
     * @param value the value of the node, if null the entire node will be removed 
     *              including any values having the same key
     * @return the removed key, if any, or null if the key does not exist
     */
    V remove( K key, V value );

    
    // ------ NEW ------
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
     * Finds a LinkedAvlMapNode<K,V> whose key is higher than the given key.
     *
     * @param key the key
     * @return the LinkedAvlMapNode<K,V> whose key is greater than the given key ,<br>
     *         null if there is no node with a higher key than the given key.
     */
    LinkedAvlMapNode<K, V> findGreater( K key );


    /**
     * Finds a LinkedAvlMapNode<K,V> whose key is higher than the given key.
     *
     * @param key the key
     * @return the LinkedAvlMapNode<K,V> whose key is greater than the given key ,<br>
     *         null if there is no node with a higher key than the given key.
     */
    LinkedAvlMapNode<K, V> findGreaterOrEqual( K key );


    /**
     * Finds a LinkedAvlMapNode<K,V> whose key is lower than the given key.
     *
     * @param key the key
     * @return the LinkedAvlMapNode<K,V> whose key is lower than the given key ,<br>
     *         null if there is no node with a lower key than the given key.
     */
    LinkedAvlMapNode<K, V> findLess( K key );


    /**
     * Finds a LinkedAvlMapNode<K,V> whose key is lower than the given key.
     *
     * @param key the key
     * @return the LinkedAvlMapNode<K,V> whose key is lower than the given key ,<br>
     *         null if there is no node with a lower key than the given key.
     */
    LinkedAvlMapNode<K, V> findLessOrEqual( K key );


    /**
     * 
     * Find a LinkedAvlNode with the given key value in the tree.
     *
     * @param key the key to find
     * @return the list of traversed LinkedAvlNode.
     */
    LinkedAvlMapNode<K, V> find( K key );


    LinkedAvlMapNode<K, V> find( K key, V value );


    boolean isDupsAllowed();

}