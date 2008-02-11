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


/**
 * A linked binary tree node.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class LinkedBinaryNode<T> 
{
    T key; // The data in the node
    LinkedBinaryNode<T> left; // Left child
    LinkedBinaryNode<T> right; // Right child
    LinkedBinaryNode<T> next;
    LinkedBinaryNode<T> previous;
    
    transient int depth;
    
    LinkedBinaryNode( T theKey )
    {
        key = theKey;
        left = right = null;
    }


	public LinkedBinaryNode<T> getLeft() {
		return left;
	}


	public LinkedBinaryNode<T> getRight() {
		return right;
	}

	public T getKey() {
		return key;
	}

	public boolean isLeaf()
	{
		return ( right == null && left == null );
	}
	
	/**
	 * This method is used for internal purpose only while pretty printing the tree.<br>
	 * @return the depth at the this node
	 */
	public int getDepth() {
		return depth;
	}

    /**
     * This method is used for internal purpose only while pretty printing the tree.<br>
     * @param depth value representing the depth of the this node
     */
	public void setDepth( int depth ) {
		this.depth = depth;
	}

	@Override
	public String toString() {
	    return "[" + key + "]";
	}
    
    
}
