package org.apache.directory.server.core.avltree;


import java.util.Comparator;
import java.util.List;


public interface AvlTreeMap<K, V>
{

    /**
     * @return the key comparator associated with this tree 
     */
    public abstract Comparator<K> getKeyComparator();


    /**
     * @return the value comparator associated with this tree 
     */
    public abstract Comparator<V> getValueComparator();


    /**
     * Inserts a LinkedAvlMapNode with the given key and value.
     *
     * @param key the item to be inserted
     * @return the replaced key if it already exists
     * Note: Replaces a nodes value if duplicate keys are not allowed.
     */
    public abstract K insert( K key, V value );


    /**
     * Removes the LinkedAvlMapNode present in the tree with the given key and value
     *
     * @param key the key of the node to be removed
     * @param value the value of the node, if null the entire node will be removed 
     *              including any values having the same key
     * @return the removed key, if any, or null if the key does not exist
     */
    public abstract K remove( K key, V value );


    /**
     * Tests if the tree is logically empty.
     * 
     * @return true if the tree is empty, false otherwise
     */
    public abstract boolean isEmpty();


    /**
     * returns the number of nodes present in this tree.
     * 
     * @return the number of nodes present in this tree
     */
    //NOTE: This method is internally used by AVLTreeMarshaller
    public abstract int getSize();


    /**
     * @return the root element of this tree (ie, not the first, but the
     * topmost element)
     */
    public abstract LinkedAvlMapNode<K, V> getRoot();


    /**
     * @return a list of the stored keys in this tree
     */
    public abstract List<K> getKeys();


    /**
     * Prints the contents of AVL tree in pretty format
     */
    public abstract void printTree();


    /**
     * @return The first element of this tree
     */
    public abstract LinkedAvlMapNode<K, V> getFirst();


    /**
     * @return The last element in this tree
     */
    public abstract LinkedAvlMapNode<K, V> getLast();


    /**
     * Finds a LinkedAvlMapNode<K,V> whose key is higher than the given key.
     *
     * @param key the key
     * @return the LinkedAvlMapNode<K,V> whose key is greater than the given key ,<br>
     *         null if there is no node with a higher key than the given key.
     */
    public abstract LinkedAvlMapNode<K, V> findGreater( K key );


    /**
     * Finds a LinkedAvlMapNode<K,V> whose key is higher than the given key.
     *
     * @param key the key
     * @return the LinkedAvlMapNode<K,V> whose key is greater than the given key ,<br>
     *         null if there is no node with a higher key than the given key.
     */
    public abstract LinkedAvlMapNode<K, V> findGreaterOrEqual( K key );


    /**
     * Finds a LinkedAvlMapNode<K,V> whose key is lower than the given key.
     *
     * @param key the key
     * @return the LinkedAvlMapNode<K,V> whose key is lower than the given key ,<br>
     *         null if there is no node with a lower key than the given key.
     */
    public abstract LinkedAvlMapNode<K, V> findLess( K key );


    /**
     * Finds a LinkedAvlMapNode<K,V> whose key is lower than the given key.
     *
     * @param key the key
     * @return the LinkedAvlMapNode<K,V> whose key is lower than the given key ,<br>
     *         null if there is no node with a lower key than the given key.
     */
    public abstract LinkedAvlMapNode<K, V> findLessOrEqual( K key );


    /**
     * 
     * Find a LinkedAvlNode with the given key value in the tree.
     *
     * @param key the key to find
     * @return the list of traversed LinkedAvlNode.
     */
    public abstract LinkedAvlMapNode<K, V> find( K key );


    public abstract LinkedAvlMapNode<K, V> find( K key, V value );


    public abstract boolean isDupsAllowed();

}