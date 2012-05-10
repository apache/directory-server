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


    /**
     * Creates a new instance of AVLTree.
     *
     * @param comparator the comparator to be used for comparing keys
     */
    public ArrayTree( Comparator<K> comparator )
    {
        this.comparator = comparator;
        array = ( K[] ) new Object[INCREMENT];
        size = 0;
    }


    /**
     * Creates a new instance of AVLTree.
     *
     * @param comparator the comparator to be used for comparing keys
     */
    public ArrayTree( Comparator<K> comparator, K[] array )
    {
        this.comparator = comparator;

        if ( array != null )
        {
            size = array.length;
            int arraySize = size;

            if ( size % INCREMENT != 0 )
            {
                arraySize += INCREMENT - size % INCREMENT;
            }

            this.array = ( K[] ) new Object[arraySize];
            System.arraycopy( array, 0, this.array, 0, size );
        }
    }


    /**
     * @return the comparator associated with this tree 
     */
    public Comparator<K> getComparator()
    {
        return comparator;
    }


    /**
     * Inserts a key. Null value insertion is not allowed.
     *
     * @param key the item to be inserted, should not be null
     * @return the replaced key if it already exists
     * Note: Ignores if the given key already exists.
     */
    public K insert( K key )
    {
        if ( key == null )
        {
            // We don't allow null values in the tree
            return null;
        }

        // Check if the key already exists, and if so, return the
        // existing one
        K existing = find( key );

        if ( existing != null )
        {
            return existing;
        }

        if ( size == array.length )
        {
            // The array is full, let's extend it
            K[] newArray = ( K[] ) new Object[size + INCREMENT];

            System.arraycopy( array, 0, newArray, 0, size );
            array = newArray;
        }

        // Currently, just add the element at the end of the array
        // and sort the array. We could be more efficient by inserting the
        // element at the right position by splitting the array in two
        // parts and copying the right part one slot on the right.
        array[size++] = key;
        Arrays.sort( array, 0, size, comparator );

        return null;
    }


    /**q<
     * Reduce the array size if neede
     */
    private void reduceArray()
    {
        // We will reduce the array size when the number of elements
        // in it is leaving twice the number of INCREMENT empty slots.
        // We then remove INCREMENT slots
        if ( ( array.length - size ) > ( INCREMENT << 1 ) )
        {
            K[] newArray = ( K[] ) new Object[array.length - INCREMENT];
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
        int pos = getPosition( key );

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

            size--;

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
    public int size()
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
        if ( isEmpty() )
        {
            System.out.println( "Tree is empty" );
            return;
        }

        boolean isFirst = false;

        for ( K key : array )
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
     * @throws ArrayIndexOutOfBoundsException If the position is not within the array boundaries
     */
    public K get( int position ) throws ArrayIndexOutOfBoundsException
    {
        if ( ( position < 0 ) || ( position >= size ) )
        {
            throw new ArrayIndexOutOfBoundsException();
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
        if ( size != 0 )
        {
            return array[0];
        }
        else
        {
            return null;
        }
    }


    /**
     * Get the last element in the tree. It sets the current position to this
     * element.
     * @return The last element in this tree
     */
    public K getLast()
    {
        if ( size != 0 )
        {
            return array[size - 1];
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
        if ( key == null )
        {
            return null;
        }

        switch ( size )
        {
            case 0:
                return null;

            case 1:
                if ( comparator.compare( array[0], key ) > 0 )
                {
                    return array[0];
                }
                else
                {
                    return null;
                }

            case 2:
                if ( comparator.compare( array[0], key ) > 0 )
                {
                    return array[0];
                }
                else if ( comparator.compare( array[1], key ) > 0 )
                {
                    return array[1];
                }
                else
                {
                    return null;
                }

            default:
                // Split the array in two parts, the left part an the right part
                int current = size >> 1;
                int start = 0;
                int end = size - 1;

                while ( end - start + 1 > 2 )
                {
                    int res = comparator.compare( array[current], key );

                    if ( res == 0 )
                    {
                        // Current can't be equal to zero at this point
                        return array[current + 1];
                    }
                    else if ( res < 0 )
                    {
                        start = current;
                        current = ( current + end + 1 ) >> 1;
                    }
                    else
                    {
                        end = current;
                        current = ( current + start + 1 ) >> 1;
                    }
                }

                switch ( end - start + 1 )
                {
                    case 1:
                        int res = comparator.compare( array[start], key );

                        if ( res <= 0 )
                        {
                            if ( start == size )
                            {
                                return null;
                            }
                            else
                            {
                                return array[start + 1];
                            }
                        }

                        return array[start];

                    case 2:
                        res = comparator.compare( array[start], key );

                        if ( res <= 0 )
                        {
                            res = comparator.compare( array[start + 1], key );

                            if ( res <= 0 )
                            {
                                if ( start == size - 2 )
                                {
                                    return null;
                                }

                                return array[start + 2];
                            }

                            return array[start + 1];
                        }

                        return array[start];
                }
        }

        return null;
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
        if ( key == null )
        {
            return null;
        }

        switch ( size )
        {
            case 0:
                return null;

            case 1:
                if ( comparator.compare( array[0], key ) >= 0 )
                {
                    return array[0];
                }
                else
                {
                    return null;
                }

            case 2:
                if ( comparator.compare( array[0], key ) >= 0 )
                {
                    return array[0];
                }
                else if ( comparator.compare( array[1], key ) >= 0 )
                {
                    return array[1];
                }
                else
                {
                    return null;
                }

            default:
                // Split the array in two parts, the left part an the right part
                int current = size >> 1;
                int start = 0;
                int end = size - 1;

                while ( end - start + 1 > 2 )
                {
                    int res = comparator.compare( array[current], key );

                    if ( res == 0 )
                    {
                        return array[current];
                    }
                    else if ( res < 0 )
                    {
                        start = current;
                        current = ( current + end + 1 ) >> 1;
                    }
                    else
                    {
                        end = current;
                        current = ( current + start + 1 ) >> 1;
                    }
                }

                switch ( end - start + 1 )
                {
                    case 1:
                        int res = comparator.compare( array[start], key );

                        if ( res >= 0 )
                        {
                            return array[start];
                        }
                        else
                        {
                            if ( start == size - 1 )
                            {
                                return null;
                            }
                            else
                            {
                                return array[start + 1];
                            }
                        }

                    case 2:
                        res = comparator.compare( array[start], key );

                        if ( res < 0 )
                        {
                            res = comparator.compare( array[start + 1], key );

                            if ( res < 0 )
                            {
                                if ( start == size - 2 )
                                {
                                    return null;
                                }

                                return array[start + 2];
                            }

                            return array[start + 1];
                        }

                        return array[start];
                }
        }

        return null;
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
        if ( key == null )
        {
            return null;
        }

        switch ( size )
        {
            case 0:
                return null;

            case 1:
                if ( comparator.compare( array[0], key ) >= 0 )
                {
                    return null;
                }
                else
                {
                    return array[0];
                }

            case 2:
                if ( comparator.compare( array[0], key ) >= 0 )
                {
                    return null;
                }
                else if ( comparator.compare( array[1], key ) >= 0 )
                {
                    return array[0];
                }
                else
                {
                    return array[1];
                }

            default:
                // Split the array in two parts, the left part an the right part
                int current = size >> 1;
                int start = 0;
                int end = size - 1;

                while ( end - start + 1 > 2 )
                {
                    int res = comparator.compare( array[current], key );

                    if ( res == 0 )
                    {
                        // Current can't be equal to zero at this point
                        return array[current - 1];
                    }
                    else if ( res < 0 )
                    {
                        start = current;
                        current = ( current + end + 1 ) >> 1;
                    }
                    else
                    {
                        end = current;
                        current = ( current + start + 1 ) >> 1;
                    }
                }

                switch ( end - start + 1 )
                {
                    case 1:
                        // Three cases :
                        // o The value is equal to the current position, or below
                        // the current position :
                        //   - if the current position is at the beginning
                        //     of the array, return null
                        //   - otherwise, return the previous position in the array
                        // o The value is above the current position :
                        //   - return the current position
                        int res = comparator.compare( array[start], key );

                        if ( res >= 0 )
                        {
                            // start can be equal to 0. Check that
                            if ( start == 1 )
                            {
                                return null;
                            }
                            else
                            {
                                return array[start - 1];
                            }
                        }
                        else
                        {
                            return array[start];
                        }

                    case 2:
                        // Four cases :
                        // o the value is equal the current position, or below 
                        //   the first position :
                        //   - if the current position is at the beginning
                        //     of the array, return null
                        //   - otherwise, return the previous element
                        // o the value is above the first position but below
                        //   or equal the second position, return the first position
                        // o otherwise, return the second position
                        res = comparator.compare( array[start], key );

                        if ( res >= 0 )
                        {
                            if ( start == 0 )
                            {
                                return null;
                            }
                            else
                            {
                                return array[start - 1];
                            }
                        }
                        else if ( comparator.compare( array[start + 1], key ) >= 0 )
                        {
                            return array[start];
                        }
                        else
                        {
                            return array[start + 1];
                        }
                }
        }

        return null;
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
        if ( key == null )
        {
            return null;
        }

        switch ( size )
        {
            case 0:
                return null;

            case 1:
                if ( comparator.compare( array[0], key ) <= 0 )
                {
                    return array[0];
                }
                else
                {
                    return null;
                }

            case 2:
                int res = comparator.compare( array[0], key );

                if ( res > 0 )
                {
                    return null;
                }
                else if ( res == 0 )
                {
                    return array[0];
                }

                res = comparator.compare( array[1], key );

                if ( res == 0 )
                {
                    return array[1];
                }
                else if ( comparator.compare( array[1], key ) > 0 )
                {
                    return array[0];
                }
                else
                {
                    return array[1];
                }

            default:
                // Split the array in two parts, the left part an the right part
                int current = size >> 1;
                int start = 0;
                int end = size - 1;

                while ( end - start + 1 > 2 )
                {
                    res = comparator.compare( array[current], key );

                    if ( res == 0 )
                    {
                        return array[current];
                    }
                    else if ( res < 0 )
                    {
                        start = current;
                        current = ( current + end + 1 ) >> 1;
                    }
                    else
                    {
                        end = current;
                        current = ( current + start + 1 ) >> 1;
                    }
                }

                switch ( end - start + 1 )
                {
                    case 1:
                        // Three cases :
                        // o The value is equal to the current position, or below
                        // the current position :
                        //   - if the current position is at the beginning
                        //     of the array, return null
                        //   - otherwise, return the previous position in the array
                        // o The value is above the current position :
                        //   - return the current position
                        res = comparator.compare( array[start], key );

                        if ( res > 0 )
                        {
                            // start can be equal to 0. Check that
                            if ( start == 1 )
                            {
                                return null;
                            }
                            else
                            {
                                return array[start - 1];
                            }
                        }
                        else
                        {
                            return array[start];
                        }

                    case 2:
                        // Four cases :
                        // o the value is equal the current position, or below 
                        //   the first position :
                        //   - if the current position is at the beginning
                        //     of the array, return null
                        //   - otherwise, return the previous element
                        // o the value is above the first position but below
                        //   or equal the second position, return the first position
                        // o otherwise, return the second position
                        res = comparator.compare( array[start], key );

                        if ( res > 0 )
                        {
                            if ( start == 0 )
                            {
                                return null;
                            }
                            else
                            {
                                return array[start - 1];
                            }
                        }

                        res = comparator.compare( array[start + 1], key );

                        if ( res > 0 )
                        {
                            return array[start];
                        }
                        else
                        {
                            return array[start + 1];
                        }
                }
        }

        return null;
    }


    /**
     * Find an element in the array. 
     *
     * @param key the key to find
     * @return the found node, or null
     */
    public K find( K key )
    {
        if ( key == null )
        {
            return null;
        }

        switch ( size )
        {
            case 0:
                return null;

            case 1:
                if ( comparator.compare( array[0], key ) == 0 )
                {
                    return array[0];
                }
                else
                {
                    return null;
                }

            case 2:
                if ( comparator.compare( array[0], key ) == 0 )
                {
                    return array[0];
                }
                else if ( comparator.compare( array[1], key ) == 0 )
                {
                    return array[1];
                }
                else
                {
                    return null;
                }

            default:
                // Split the array in two parts, the left part an the right part
                int current = size >> 1;
                int start = 0;
                int end = size - 1;

                while ( end - start + 1 > 2 )
                {
                    int res = comparator.compare( array[current], key );

                    if ( res == 0 )
                    {
                        return array[current];
                    }
                    else if ( res < 0 )
                    {
                        start = current;
                        current = ( current + end + 1 ) >> 1;
                    }
                    else
                    {
                        end = current;
                        current = ( current + start + 1 ) >> 1;
                    }
                }

                switch ( end - start + 1 )
                {
                    case 1:
                        if ( comparator.compare( array[start], key ) == 0 )
                        {
                            return array[start];
                        }
                        else
                        {
                            return null;
                        }

                    case 2:
                        if ( comparator.compare( array[start], key ) == 0 )
                        {
                            return array[start];
                        }
                        else if ( comparator.compare( array[end], key ) == 0 )
                        {
                            return array[end];
                        }
                        else
                        {
                            return null;
                        }
                }
        }

        return null;
    }


    /**
     * Find the element position in the array. 
     *
     * @param key the key to find
     * @return the position in the array, or -1 if not found
     */
    public int getPosition( K key )
    {
        if ( key == null )
        {
            return -1;
        }

        switch ( size )
        {
            case 0:
                return -1;

            case 1:
                if ( comparator.compare( array[0], key ) == 0 )
                {
                    return 0;
                }
                else
                {
                    return -1;
                }

            case 2:
                if ( comparator.compare( array[0], key ) == 0 )
                {
                    return 0;
                }
                else if ( comparator.compare( array[1], key ) == 0 )
                {
                    return 1;
                }
                else
                {
                    return -1;
                }

            default:
                // Split the array in two parts, the left part an the right part
                int current = size >> 1;
                int start = 0;
                int end = size - 1;

                while ( end - start + 1 > 2 )
                {
                    int res = comparator.compare( array[current], key );

                    if ( res == 0 )
                    {
                        return current;
                    }
                    else if ( res < 0 )
                    {
                        start = current;
                        current = ( current + end + 1 ) >> 1;
                    }
                    else
                    {
                        end = current;
                        current = ( current + start + 1 ) >> 1;
                    }
                }

                switch ( end - start + 1 )
                {
                    case 1:
                        if ( comparator.compare( array[start], key ) == 0 )
                        {
                            return start;
                        }
                        else
                        {
                            return -1;
                        }

                    case 2:
                        if ( comparator.compare( array[start], key ) == 0 )
                        {
                            return start;
                        }
                        else if ( comparator.compare( array[end], key ) == 0 )
                        {
                            return end;
                        }
                        else
                        {
                            return -1;
                        }
                }
        }

        return -1;
    }


    /**
     * Find the element position in the array, or the position of the closest greater element in the array. 
     *
     * @param key the key to find
     * @return the position in the array, or -1 if not found
     */
    public int getAfterPosition( K key )
    {
        if ( key == null )
        {
            return -1;
        }

        switch ( size )
        {
            case 0:
                return -1;

            case 1:
                if ( comparator.compare( array[0], key ) > 0 )
                {
                    return 0;
                }
                else
                {
                    return -1;
                }

            case 2:
                if ( comparator.compare( array[0], key ) > 0 )
                {
                    return 0;
                }

                if ( comparator.compare( array[1], key ) > 0 )
                {
                    return 1;
                }
                else
                {
                    return -1;
                }

            default:
                // Split the array in two parts, the left part an the right part
                int current = size >> 1;
                int start = 0;
                int end = size - 1;

                while ( end - start + 1 > 2 )
                {
                    int res = comparator.compare( array[current], key );

                    if ( res == 0 )
                    {
                        if ( current != size - 1 )
                        {
                            return current + 1;
                        }
                        else
                        {
                            return -1;
                        }
                    }
                    else if ( res < 0 )
                    {
                        start = current;
                        current = ( current + end + 1 ) >> 1;
                    }
                    else
                    {
                        end = current;
                        current = ( current + start + 1 ) >> 1;
                    }
                }

                switch ( end - start + 1 )
                {
                    case 1:
                        if ( comparator.compare( array[start], key ) > 0 )
                        {
                            return start;
                        }
                        else
                        {
                            return -1;
                        }

                    case 2:
                        if ( comparator.compare( array[start], key ) > 0 )
                        {
                            return start;
                        }

                        if ( comparator.compare( array[end], key ) > 0 )
                        {
                            return end;
                        }
                        else
                        {
                            return -1;
                        }
                }
        }

        return -1;
    }


    /**
     * Find the element position in the array, or the position of the closest greater element in the array. 
     *
     * @param key the key to find
     * @return the position in the array, or -1 if not found
     */
    public int getBeforePosition( K key )
    {
        if ( key == null )
        {
            return -1;
        }

        switch ( size )
        {
            case 0:
                return -1;

            case 1:
                if ( comparator.compare( array[0], key ) < 0 )
                {
                    return 0;
                }
                else
                {
                    return -1;
                }

            case 2:
                if ( comparator.compare( array[1], key ) < 0 )
                {
                    return 1;
                }

                if ( comparator.compare( array[0], key ) < 0 )
                {
                    return 0;
                }
                else
                {
                    return -1;
                }

            default:
                // Split the array in two parts, the left part an the right part
                int current = size >> 1;
                int start = 0;
                int end = size - 1;

                while ( end - start + 1 > 2 )
                {
                    int res = comparator.compare( array[current], key );

                    if ( res == 0 )
                    {
                        if ( current == 0 )
                        {
                            return -1;
                        }
                        else
                        {
                            return current - 1;
                        }
                    }
                    else if ( res < 0 )
                    {
                        start = current;
                        current = ( current + end + 1 ) >> 1;
                    }
                    else
                    {
                        end = current;
                        current = ( current + start + 1 ) >> 1;
                    }
                }

                switch ( end - start + 1 )
                {
                    case 1:
                        if ( comparator.compare( array[start], key ) < 0 )
                        {
                            return start;
                        }
                        else
                        {
                            return -1;
                        }

                    case 2:
                        if ( comparator.compare( array[end], key ) < 0 )
                        {
                            return end;
                        }

                        if ( comparator.compare( array[start], key ) < 0 )
                        {
                            return start;
                        }
                        else
                        {
                            return -1;
                        }
                }
        }

        return -1;
    }


    /**
     * Tells if a key exist in the array.
     * 
     * @param key The key to look for
     * @return true if the key exist in the array
     */
    public boolean contains( K key )
    {
        return find( key ) != null;
    }


    /**
     * {@inheritDoc}
     */
    public String toString()
    {
        if ( isEmpty() )
        {
            return "[]";
        }

        StringBuilder sb = new StringBuilder();

        boolean isFirst = true;

        for ( int i = 0; i < size; i++ )
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
