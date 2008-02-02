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
package org.apache.directory.server.core.splay;


import java.util.Comparator;


/**
 * A generified, top-down, linked, in memory, splay tree implementation. This 
 * is an adaptation of the <a href="mailto:sleator@cs.cmu.edu">Danny Sleator's
 * </a> non-linked implementation <b><a href="http://tinyurl.com/2xs3fl">here
 * </a></b>.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev: 602753 $
 */
public class SplayTree<K>
{
    private LinkedBinaryNode<K> header = new LinkedBinaryNode<K>( null ); // For splay

    private LinkedBinaryNode<K> root;
    private Comparator<K> keyComparator;


    public SplayTree( Comparator<K> keyComparator )
    {
        this.keyComparator = keyComparator;
        root = null;
    }


    /**
     * Insert into the tree.
     * @param x the item to insert.
     * @throws DuplicateItemException if x is already present.
     */
    public void insert( K key )
    {
        LinkedBinaryNode<K> n;
        int c;
        if ( root == null )
        {
            root = new LinkedBinaryNode<K>( key );
            return;
        }
        splay( key );
        if ( ( c = keyComparator.compare( key, root.key ) ) == 0 )
        {
            //      throw new DuplicateItemException( x.toString() );     
            return;
        }
        n = new LinkedBinaryNode<K>( key );
        if ( c < 0 )
        {
            n.left = root.left;
            n.right = root;
            root.left = null;
        }
        else
        {
            n.right = root.right;
            n.left = root;
            root.right = null;
        }
        root = n;
    }


    /**
     * Remove from the tree.
     * @param x the item to remove.
     * @throws ItemNotFoundException if x is not found.
     */
    public void remove( K key )
    {
        LinkedBinaryNode<K> x;
        splay( key );
        if ( keyComparator.compare( key, root.key ) != 0 )
        {
            //            throw new ItemNotFoundException(x.toString());
            return;
        }
        // Now delete the root
        if ( root.left == null )
        {
            root = root.right;
        }
        else
        {
            x = root.right;
            root = root.left;
            splay( key );
            root.right = x;
        }
    }


    /**
     * Find the smallest item in the tree.
     */
    public K findMin()
    {
        LinkedBinaryNode<K> x = root;
        if ( root == null )
            return null;
        while ( x.left != null )
            x = x.left;
        splay( x.key );
        return x.key;
    }


    /**
     * Find the largest item in the tree.
     */
    public K findMax()
    {
        LinkedBinaryNode<K> x = root;
        if ( root == null )
            return null;
        while ( x.right != null )
            x = x.right;
        splay( x.key );
        return x.key;
    }


    /**
     * Find an item in the tree.
     */
    public K find( K key )
    {
        if ( root == null )
        {
            return null;
        }
        
        splay( key );
        
        if ( keyComparator.compare( root.key, key ) != 0 )
        {
            return null;
        }
        
        return root.key;
    }


    /**
     * Test if the tree is logically empty.
     * @return true if empty, false otherwise.
     */
    public boolean isEmpty()
    {
        return root == null;
    }


    /** this method just illustrates the top-down method of
     * implementing the move-to-root operation 
     */
    @SuppressWarnings("unused")
    private void moveToRoot( K key )
    {
        LinkedBinaryNode<K> l, r, t;
        l = r = header;
        t = root;
        header.left = header.right = null;
        for ( ;; )
        {
            if ( keyComparator.compare( key, t.key ) < 0 )
            {
                if ( t.left == null )
                    break;
                r.left = t; /* link right */
                r = t;
                t = t.left;
            }
            else if ( keyComparator.compare( key, t.key ) > 0 )
            {
                if ( t.right == null )
                    break;
                l.right = t; /* link left */
                l = t;
                t = t.right;
            }
            else
            {
                break;
            }
        }
        l.right = t.left; /* assemble */
        r.left = t.right;
        t.left = header.right;
        t.right = header.left;
        root = t;
    }


    /**
     * Internal method to perform a top-down splay.
     * 
     *   splay(key) does the splay operation on the given key.
     *   If key is in the tree, then the LinkedBinaryNode containing
     *   that key becomes the root.  If key is not in the tree,
     *   then after the splay, key.root is either the greatest key
     *   < key in the tree, or the lest key > key in the tree.
     *
     *   This means, among other things, that if you splay with
     *   a key that's larger than any in the tree, the rightmost
     *   node of the tree becomes the root.  This property is used
     *   in the delete() method.
     */

    private void splay( K key )
    {
        LinkedBinaryNode<K> l, r, t, y;
        l = r = header;
        t = root;
        header.left = header.right = null;
        for ( ;; )
        {
            if ( keyComparator.compare( key, t.key ) < 0 )
            {
                if ( t.left == null )
                    break;
                if ( keyComparator.compare( key, t.left.key ) < 0 )
                {
                    y = t.left; /* rotate right */
                    t.left = y.right;
                    y.right = t;
                    t = y;
                    if ( t.left == null )
                        break;
                }
                r.left = t; /* link right */
                r = t;
                t = t.left;
            }
            else if ( keyComparator.compare( key, t.key ) > 0 )
            {
                if ( t.right == null )
                    break;
                if ( keyComparator.compare( key, t.right.key ) > 0 )
                {
                    y = t.right; /* rotate left */
                    t.right = y.left;
                    y.left = t;
                    t = y;
                    if ( t.right == null )
                        break;
                }
                l.right = t; /* link left */
                l = t;
                t = t.right;
            }
            else
            {
                break;
            }
        }
        l.right = t.left; /* assemble */
        r.left = t.right;
        t.left = header.right;
        t.right = header.left;
        root = t;
    }


    // test code stolen from Weiss
    public static void main( String[] args )
    {
        SplayTree<Integer> t = new SplayTree<Integer>( new Comparator<Integer>() 
        {
            public int compare( Integer i1, Integer i2 )
            {
                return i1.compareTo( i2 );
            }
        });
        final int NUMS = 40000;
        final int GAP = 307;

        System.out.println( "Checking... (no bad output means success)" );

        for ( int i = GAP; i != 0; i = ( i + GAP ) % NUMS )
            t.insert( new Integer( i ) );
        System.out.println( "Inserts complete" );

        for ( int i = 1; i < NUMS; i += 2 )
            t.remove( new Integer( i ) );
        System.out.println( "Removes complete" );

        if ( ( ( Integer ) ( t.findMin() ) ).intValue() != 2 || ( ( Integer ) ( t.findMax() ) ).intValue() != NUMS - 2 )
            System.out.println( "FindMin or FindMax error!" );

        for ( int i = 2; i < NUMS; i += 2 )
            if ( ( ( Integer ) t.find( new Integer( i ) ) ).intValue() != i )
                System.out.println( "Error: find fails for " + i );

        for ( int i = 1; i < NUMS; i += 2 )
            if ( t.find( new Integer( i ) ) != null )
                System.out.println( "Error: Found deleted item " + i );
    }

}