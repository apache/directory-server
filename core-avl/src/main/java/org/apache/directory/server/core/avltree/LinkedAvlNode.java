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
 * A linked AVL tree node.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class LinkedAvlNode<T> 
{
    /** The data stored in the node */
    T key;
    
    /** The left child */
    LinkedAvlNode<T> left;
    
    /** The right child */
    LinkedAvlNode<T> right;
    
    /** The next node, superior to the current node */
    LinkedAvlNode<T> next;

    /** The previous node, inferior to the current node */
    LinkedAvlNode<T> previous;
    
    int depth;
    int index;
    
    boolean isLeft;
    int height = 1;
    
    
    /**
     * Creates a new instance of LinkedAvlNode, containing a given value.
     *
     * @param theKey the stored value on the topmost node
     */
    public LinkedAvlNode( T theKey )
    {
        key = theKey;
        left = null;
        right = null;
    }


    public void setLeft( LinkedAvlNode<T> left )
    {
        this.left = left;
    }


    public void setRight( LinkedAvlNode<T> right )
    {
        this.right = right;
    }


    public LinkedAvlNode<T> getNext()
    {
        return next;
    }


    public LinkedAvlNode<T> getPrevious()
    {
        return previous;
    }


    public LinkedAvlNode<T> getLeft() {
        return left;
    }


    public LinkedAvlNode<T> getRight() {
        return right;
    }

    public T getKey() {
        return key;
    }

    public boolean isLeaf()
    {
        return ( right == null && left == null );
    }
    
    public int getDepth() {
        return depth;
    }

    public void setDepth( int depth ) {
        this.depth = depth;
    }

    public int getHeight()
    {
        return height;
    }
    
    
   public void setNext( LinkedAvlNode<T> next )
   {
      this.next = next;
   }

   
   public void setPrevious( LinkedAvlNode<T> previous )
   {
      this.previous = previous;
   }    
   
   
    public int computeHeight()
    {

        if(right == null && left == null)
        {
            height = 1;
            return height;
        }
        
        int lh,rh;
        
        if( isLeft )
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

    public void setIndex(int index)
    {
        this.index = index;
    }


    @Override
    public String toString() {
        return "[" + key + "]";
    }
    
}
