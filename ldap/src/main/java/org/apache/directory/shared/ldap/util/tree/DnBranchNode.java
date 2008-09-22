/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *  
 *    http://www.apache.org/licenses/LICENSE-2.0
 *  
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License. 
 *  
 */
package org.apache.directory.shared.ldap.util.tree;


import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import javax.naming.NamingException;

import org.apache.directory.shared.ldap.name.LdapDN;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * 
 * The Hierarchical Container holds elements ordered by their DN. 
 * 
 * We can see them as directories, where the leaves are the files.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class DnBranchNode<N> implements DnNode<N>
{
    /** The logger for this class */
    private static final Logger LOG = LoggerFactory.getLogger( DnBranchNode.class );

    /** Stores the list of all the descendant */
    private Map<String, DnNode<N>> children;
    
    
    /**
     * Creates a new instance of a DnBranchNode.
     */
    public DnBranchNode()
    {
        children = new HashMap<String, DnNode<N>>(3);
    }

    
    /**
     * @see DnNode#isLeaf()
     */
    public boolean isLeaf()
    {
        return false;
    }
    
    
    /**
     * Recursively adds new nodes to the element lookup tree data structure.  
     * When called it will add an element to the tree in the appropriate leaf 
     * node position based on the DN passed in as an argument.
     *
     * @param current The current node having an element added to it
     * @param dn The DN associated with the added element
     * @param index The index of the current RDN being processed 
     * @param element The associated element to add as a tree node
     * @return The modified tree structure.
     */
    public DnNode<N> recursivelyAddElement( DnBranchNode<N> current, LdapDN dn, int index, N element ) throws NamingException
    {
        String rdnAtIndex = dn.getRdn( index ).toString();
        
        if ( index == dn.size() - 1 )
        {
            return current.addNode( rdnAtIndex, new DnLeafNode<N>( element ) );
        }
        else
        {
            DnNode<N> newNode = ((DnBranchNode<N>)current).getChild( rdnAtIndex );
            
            if ( newNode instanceof DnLeafNode )
            {
                String message = "Overlapping partitions are not allowed";
                LOG.error( message );
                throw new NamingException( message );
            }
        
            if ( newNode == null )
            {
                newNode = new DnBranchNode<N>();
            }

            DnNode<N> child = recursivelyAddElement( (DnBranchNode<N>)newNode, dn, index + 1, element );
            return current.addNode( rdnAtIndex, child );
        }
    }
    
    
    /**
     * Directly adds a new child DnNode to the current DnBranchNode.
     *
     * @param rdn The rdn of the child node to add 
     * @param child The child node to add
     * @return The modified branch node after the insertion
     */
    public DnNode<N> addNode( String rdn, DnNode<N> child )
    {
        children.put( rdn, child );
        return this;
    }
    
    
    /**
     * Tells if the current DnBranchNode contains another node associated 
     * with an rdn.
     *
     * @param rdn The name we are looking for
     * @return <code>true</code> if the tree instance contains this name
     */
    public boolean contains( String rdn )
    {
        return children.containsKey( rdn );
    }

    
    /**
     * Get's a child using an rdn string.
     * 
     * @param rdn the rdn to use as the node key
     * @return the child node corresponding to the rdn.
     */
    public DnNode<N> getChild( String rdn )
    {
        if ( children.containsKey( rdn ) )
        {
            return children.get( rdn );
        }

        return null;
    }
    
    
    /**
     * @see Object#toString()
     */
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append( "{" );
        boolean isFirst = true;
        
        for ( DnNode<N> child:children.values() )
        {
            if ( isFirst )
            {
                isFirst = false;
            }
            else
            {
                sb.append(  ", " );
            }

            if ( child instanceof DnBranchNode )
            {
                sb.append( "Branch: ").append( child.toString() );
            }
            else
            {
                sb.append( "Leaf: " ).append( "'" ).append( child.toString() ).append( "'" );
            }
        }

        sb.append( "}" );
        return sb.toString();
    }
    
    
    /**
     * Get the parent of a given DN, if present in the tree. This parent should be a 
     * subset of the given dn.<br>
     * For instance, if we have stored dc=acme, dc=org into the tree, 
     * the DN: ou=example, dc=acme, dc=org will have a parent, and 
     * dc=acme, dc=org will be returned.
     * <br>For the DN ou=apache, dc=org, there is no parent, so null will be returned.
     *  
     *
     * @param dn the normalized distinguished name to resolve to a parent
     * @return the parent associated with the normalized dn
     */
    public N getParentElement( LdapDN dn )
    {
        Enumeration<String> rdns = dn.getAll();
        
        // This is synchronized so that we can't read the
        // partitionList when it is modified.
        synchronized ( this )
        {
            DnNode<N> currentNode = this;

            // Iterate through all the RDN until we find the associated partition
            while ( rdns.hasMoreElements() )
            {
                String rdn = rdns.nextElement();

                if ( currentNode == null )
                {
                    break;
                }

                if ( currentNode instanceof DnLeafNode )
                {
                    return ( ( DnLeafNode<N> ) currentNode ).getElement();
                }

                DnBranchNode<N> currentBranch = ( DnBranchNode<N> ) currentNode;
                
                if ( currentBranch.contains( rdn ) )
                {
                    currentNode = currentBranch.getChild( rdn );
                    
                    if ( currentNode instanceof DnLeafNode )
                    {
                        return ( ( DnLeafNode<N> ) currentNode ).getElement();
                    }
                }
            }
        }
        
        return null;
    }

    
    /**
     * Tells if the DN contains a parent in the tree. This parent should be a 
     * subset of the given dn.<br>
     * For instance, if we have stored dc=acme, dc=org into the tree, 
     * the DN: ou=example, dc=acme, dc=org will have a parent. 
     *
     * @param dn the normalized distinguished name to resolve to a parent
     * @return the parent associated with the normalized dn
     */
    public boolean hasParentElement( LdapDN dn )
    {
        Enumeration<String> rdns = dn.getAll();
        
        // This is synchronized so that we can't read the
        // partitionList when it is modified.
        synchronized ( this )
        {
            DnNode<N> currentNode = this;

            // Iterate through all the RDN until we find the associated partition
            while ( rdns.hasMoreElements() )
            {
                String rdn = rdns.nextElement();

                if ( currentNode == null )
                {
                    return false;
                }

                if ( currentNode instanceof DnLeafNode )
                {
                    return true;
                }

                DnBranchNode<N> currentBranch = ( DnBranchNode<N> ) currentNode;
                
                if ( currentBranch.contains( rdn ) )
                {
                    currentNode = currentBranch.getChild( rdn );
                    
                    if ( currentNode instanceof DnLeafNode )
                    {
                        return true;
                    }
                }
            }
        }
        
        return false;
    }
}
