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
 * @version $Rev$, $Date$
 */
public class LinkedAvlNode<T> 
{
    T key; // The data in the node
    LinkedAvlNode<T> left; // Left child
    LinkedAvlNode<T> right; // Right child
    LinkedAvlNode<T> next;
    LinkedAvlNode<T> previous;
    
    transient int depth;
    
    public LinkedAvlNode( T theKey )
    {
        key = theKey;
        left = right = null;
    }


	public void setLeft( LinkedAvlNode<T> left )
    {
        this.left = left;
    }


    public void setRight( LinkedAvlNode<T> right )
    {
        this.right = right;
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

	
	public void setNext( LinkedAvlNode<T> next )
    {
        this.next = next;
    }


    public void setPrevious( LinkedAvlNode<T> previous )
    {
        this.previous = previous;
    }


    public int getHeight()
    {
	    if(right == null && left == null)
	    {
	        return 1;
	    }
	    
	    int lh = ( left == null ? -1 : left.getHeight() );
	    int rh = ( right == null ? -1 : right.getHeight() );
	    
        return 1 + Math.max( lh, rh );
    }

	public int getIndex()
	{
	    if( previous == null )
	    {
	        return 0;
	    }
	    
	  return previous.getIndex() + 1;
	}

    @Override
	public String toString() {
	    return "[" + key + "]";
	}
    
    
}
