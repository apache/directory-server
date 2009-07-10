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


import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;


/**
 * A data structure simulating a tree (ie, a sorted list of elements) using an array.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class ArrayTree<K>
{
    /** The Comparator used for comparing the keys */
    private Comparator<K> comparator;

    /** The array containing the data */
    private K[] array;
    
    /** The current number of elements in the array. May be lower than the array size */
    private int size;
    
    /** The extend size to use when increasing the array size */
    private static final int INCREMENT = 16;
    
    /** The current position in the array */
    private int position;

    /**
     * Creates a new instance of AVLTree.
     *
     * @param comparator the comparator to be used for comparing keys
     */
    public ArrayTree( Comparator<K> comparator)
    {
        this.comparator = comparator;
        array = (K[])new Object[INCREMENT];
        size = 0;
        position = 0;
    }
    
    
    /**
     * @return the comparator associated with this tree 
     */
    public Comparator<K> getComparator()
    {
        return comparator;
    }
    
    
    /**
     * Inserts a key.
     *
     * @param key the item to be inserted
     * @return the replaced key if it already exists
     * Note: Ignores if the given key already exists.
     */
    public K insert( K key )
    {
        if ( size == array.length )
        {
            // The array is full, let's extend it
            K[] newArray = (K[])new Object[size + INCREMENT];
            
            System.arraycopy( array, 0, newArray, 0, size );
            array = newArray;
        }
        
        array[size++] = key;
        Arrays.sort( array, 0, size, comparator );
        
        return key;
    }
    
    
    /**
     * Reduce the array size if neede
     */
    private void reduceArray()
    {
        // We will reduce the array size when the number of elements
        // in it is leaving twice the number of INCREMENT empty slots.
        // We then remove INCREMENT slots
        if ( ( array.length - size ) > (INCREMENT << 1) )
        {
            K[] newArray = (K[])new Object[array.length - INCREMENT];
            System.arraycopy( array, 0, newArray, 0, array.length );
        }
    }
    
    
    /**
     * Removes a key present in the tree
     *
     * @param key the value to be removed
     * @return the removed key, if any, or null if the key does not exist
     */
    public K remove( K key )
    {
        // Search for the key position in the tree
        int pos = findPosition( key );
        
        if ( pos != -1 )
        {
            // Found... 
            if ( pos != size - 1 )
            {
                // If the element is not the last one, we have to
                // move the end of the array one step to the left
                System.arraycopy( array, pos + 1, array, pos, size - pos - 1 );
                
                reduceArray();
            }
            
            size --;
            
            return key;
        }
        else
        {
            return null;
        }
    }
    
    
    /**
     * Tests if the tree is empty.
     * 
     * @return true if the tree is empty, false otherwise
     */
    public boolean isEmpty()
    {
      return size == 0;
    }

    
    /**
     * returns the number of nodes present in this tree.
     * 
     * @return the number of nodes present in this tree
     */
    public int getSize()
    {
        return size;
    }
    
    
    /**
     * @return a list of the stored keys in this tree
     */
    public List<K> getKeys()
    {
        List<K> list = new ArrayList<K>( size );
        
        for ( int i = 0; i < size; i++ )
        {
            list.add( array[i] );
        }
        
        return list;
    }

    /**
     * Prints the contents of AVL tree in pretty format
     */
    public void printTree() 
    {
        if( isEmpty() )
        {
            System.out.println( "Tree is empty" );
            return;
        }
        
        boolean isFirst = false;
        
        for ( K key:array )
        {
            if ( isFirst )
            {
                isFirst = false;
            }
            else
            {
                System.out.print( ", " );
            }
            
            System.out.println( key );
        }
    }
    
    
    /**
     * Get the element at a given position
     * @param position The position in the tree
     * @return The found key, or null if the position is out of scope
     */
    public K get( int position )
    {
        if ( ( position < 0 ) || ( position >= size ) )
        {
            return null;
        }
        
        return array[position];
    }
    
    
    /**
     * Get the element at the current position
     * @return The found key, or null if the position is out of scope
     */
    public K get()
    {
        if ( ( position < 0 ) || ( position >= size ) )
        {
            return null;
        }
        
        return array[position];
    }
    

    /**
     * Get the first element in the tree. It sets the current position to this
     * element.
     * @return The first element of this tree
     */
    public K getFirst()
    {
        position = 0;
        
        if ( size != 0 )
        {
            return array[position];
        }
        else
        {
            return null;
        }
    }
    
    
    /**
     * Get the next element in the tree, from the current position. The position is
     * changed accordingly
     * @return The next element of this tree
     */
    public K getNext()
    {
        if ( ( position < 0 ) || ( position >= size - 1 ) )
        {
            return null;
        }
        
        position++;
        return array[position];
    }
    
    
    /**
     * Get the previous element in the tree, from the current position. The position is
     * changed accordingly
     * @return The previous element of this tree
     */
    public K getPrevious()
    {
        if ( ( position <= 0 ) || ( position > size  - 1 ) )
        {
            return null;
        }
        
        position--;
        return array[position];
    }
    
    
    /**
     * Get the last element in the tree. It sets the current position to this
     * element.
     * @return The last element in this tree
     */
    public K getLast()
    {
        position = size - 1;
        
        if ( size != 0 )
        {
            return array[position];
        }
        else
        {
            return null;
        }
    }

    /**
     * Finds a key higher than the given key. Sets the current position to this
     * element.
     *
     * @param key the key to find
     * @return the LinkedAvlNode<K> whose key is greater than the given key ,<br>
     *         null if there is no node with a higher key than the given key.
     */
    public K findGreater( K key )
    {
        if ( size == 0 )
        {
            return null;
        }
        
        int current = size >> 1;
        int end = size;
        int start = 0;
        int previousCurrent = -1;
        
        while ( previousCurrent != current )
        {
            int res = comparator.compare( array[current], key ) ;
            
            if ( res == 0 )
            {
                if ( current == size - 1 )
                {
                    return null;
                }
                else
                {
                    position = current + 1;
                    return array[position];
                }
            }
            else if ( res < 0 )
            {
                start = current;
                previousCurrent = current;
                current = (start + end ) >> 1;
            }
            else
            {
                end = current;
                previousCurrent = current;
                current = (start + end ) >> 1 ;
            }
        }
        
        // We haven't found the element, so take the next one
        if ( current == size - 1 )
        {
            return null;
        }
        else
        {
            position = current + 1;
            return array[position];
        }
    }


    /**
     * Finds a key higher than the given key.
     *
     * @param key the key
     * @return the key chich is greater than the given key ,<br>
     *         null if there is no higher key than the given key.
     */
    public K findGreaterOrEqual( K key )
    {
        if ( size == 0 )
        {
            return null;
        }
        
        int current = size >> 1;
        int end = size;
        int start = 0;
        int previousCurrent = -1;

        while ( previousCurrent != current )
        {
            int res = comparator.compare( array[current], key ) ;
            
            if ( res == 0 )
            {
                position = current;
                return array[current];
            }
            else if ( res < 0 )
            {
                start = current;
                previousCurrent = current;
                current = (current + end ) >> 1;
            }
            else
            {
                end = current;
                previousCurrent = current;
                current = (current + start ) >> 1 ;
            }
        }
        
        // We haven't found the element, so take the next one
        if ( current == size - 1 )
        {
            return null;
        }
        else
        {
            position = current + 1;
            return array[position];
        }
    }


    /**
     * Finds a key which is lower than the given key.
     *
     * @param key the key
     * @return the key lower than the given key ,<br>
     *         null if there is no node with a lower key than the given key.
     */
    public K findLess( K key )
    {
        if ( size == 0 )
        {
            return null;
        }
        
        int current = size >> 1;
        int end = size;
        int start = 0;
        int previousCurrent = -1;
        
        while ( previousCurrent != current )
        {
            int res = comparator.compare( array[current], key ) ;
            
            if ( res == 0 )
            {
                if ( current == 0 )
                {
                    return null;
                }
                else
                {
                    position = current - 1;
                    return array[position];
                }
            }
            else if ( res < 0 )
            {
                start = current;
                previousCurrent = current;
                current = (current + end ) >> 1;
            }
            else
            {
                end = current;
                previousCurrent = current;
                current = (current + start ) >> 1 ;
            }
        }
        
        // We haven't found the element, so take the previous one
        if ( current == 0 )
        {
            return null;
        }
        else
        {
            position = current;
            return array[current];
        }
    }


    /**
     * Finds a key chich is lower than the given key.
     *
     * @param key the key
     * @return the key which is lower than the given key ,<br>
     *         null if there is no node with a lower key than the given key.
     */
    public K findLessOrEqual( K key )
    {
        if ( size == 0 )
        {
            return null;
        }
        
        int current = size >> 1;
        int end = size;
        int start = 0;
        int previousCurrent = -1;
        
        while ( previousCurrent != current )
        {
            int res = comparator.compare( array[current], key ) ;
            
            if ( res == 0 )
            {
                position = current;
                return array[current];
            }
            else if ( res < 0 )
            {
                start = current;
                previousCurrent = current;
                current = (current + end ) >> 1;
            }
            else
            {
                end = current;
                previousCurrent = current;
                current = (current + start ) >> 1 ;
            }
        }
        
        // We haven't found the element, so take the previous one
        if ( current == 0 )
        {
            return null;
        }
        else
        {
            position = current - 1;
            return array[position];
        }
    }
    
    
    /**
     * Find 
     *
     * @param key the key to find
     * @return the list of traversed LinkedAvlNode.
     */
    public K find( K key )
    {
        if ( size == 0 )
        {
            position = 0;
            return null;
        }
        
        int current = size >> 1;
        int end = size;
        int start = 0;
        int previousCurrent = -1;
        
        while ( previousCurrent != current )
        {
            int res = comparator.compare( array[current], key ) ;
            
            if ( res == 0 )
            {
                position = current;
                return key;
            }
            else if ( res < 0 )
            {
                start = current;
                previousCurrent = current;
                current = (current + end ) >> 1;
            }
            else
            {
                end = current;
                previousCurrent = current;
                current = (current + start ) >> 1 ;
            }
        }
        
        position = 0;
        return null;
    }
    

    private int findPosition( K key )
    {
        if ( size == 0 )
        {
            return -1;
        }
        
        int current = size >> 1;
        int end = size;
        int start = 0;
        int previousCurrent = -1;
        
        while ( previousCurrent != current )
        {
            int res = comparator.compare( array[current], key ) ;
            
            if ( res == 0 )
            {
                position = current;
                return current;
            }
            else if ( res < 0 )
            {
                start = current;
                previousCurrent = current;
                current = (current + end ) >> 1;
            }
            else
            {
                end = current;
                previousCurrent = current;
                current = (current + start ) >> 1 ;
            }
        }
        
        return -1;
    }
    
    
    /**
     * {@inheritDoc}
     */
    public String toString()
    {
        if( isEmpty() )
        {
            return "[]";
        }
        
        StringBuilder sb = new StringBuilder();
        
        boolean isFirst = true;
        
        for ( int i = 0; i < size; i ++ )
        {
            K key = array[i];
            
            if ( isFirst )
            {
                isFirst = false;
            }
            else
            {
                sb.append( ", " );
            }
            
            sb.append( key );
        }
        
        return sb.toString();
    }
}
