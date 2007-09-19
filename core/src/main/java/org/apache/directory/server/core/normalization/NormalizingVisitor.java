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
package org.apache.directory.server.core.normalization;


import org.apache.directory.server.schema.registries.OidRegistry;
import org.apache.directory.shared.asn1.primitives.OID;
import org.apache.directory.shared.ldap.filter.AndNode;
import org.apache.directory.shared.ldap.filter.BranchNode;
import org.apache.directory.shared.ldap.filter.ExprNode;
import org.apache.directory.shared.ldap.filter.ExtensibleNode;
import org.apache.directory.shared.ldap.filter.FilterVisitor;
import org.apache.directory.shared.ldap.filter.LeafNode;
import org.apache.directory.shared.ldap.filter.NotNode;
import org.apache.directory.shared.ldap.filter.PresenceNode;
import org.apache.directory.shared.ldap.filter.SimpleNode;
import org.apache.directory.shared.ldap.filter.SubstringNode;
import org.apache.directory.shared.ldap.name.NameComponentNormalizer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.naming.NamingException;

import java.util.ArrayList;
import java.util.List;


/**
 * A filter visitor which normalizes leaf node values as it visits them.  It also removes
 * leaf nodes from branches whose attributeType is undefined.  It obviously cannot remove
 * a leaf node from a filter which is only a leaf node.  Checks to see if a filter is a
 * leaf node with undefined attributeTypes should be done outside this visitor.
 *
 * Since this visitor may remove filter nodes it may produce negative results on filters,
 * like NOT branch nodes without a child or AND and OR nodes with one or less children.  This
 * might make some partition implementations choke.  To avoid this problem we clean up branch
 * nodes that don't make sense.  For example all BranchNodes without children are just
 * removed.  An AND and OR BranchNode with a single child is replaced with it's child for
 * all but the topmost branchnode which we cannot replace.  So again the top most branch
 * node must be inspected by code outside of this visitor.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class NormalizingVisitor implements FilterVisitor
{
    /** logger used by this class */
    private final static Logger log = LoggerFactory.getLogger( NormalizingVisitor.class );
    
    /** the name component normalizer used by this visitor */
    private final NameComponentNormalizer ncn;
    
    /** the oid registry used to resolve OIDs for attributeType ids */
    private final OidRegistry registry;


    public NormalizingVisitor( NameComponentNormalizer ncn, OidRegistry registry )
    {
        this.ncn = ncn;
        this.registry = registry;
    }
    
    /**
     * A private method used to normalize a value
     * @return
     */
    private Object normalizeValue( String attribute, Object value )
    {
    	try
    	{
	    	Object normalized;
	    	
	        if ( OID.isOID( attribute ) )
	        {
	            if ( value instanceof String )
	            {
	                normalized = ncn.normalizeByOid( attribute, ( String ) value );
	            }
	            else if ( value instanceof byte [] )
	            {
	                normalized = ncn.normalizeByOid( attribute, ( byte[] ) value );
	            }
	            else
	            {
	                normalized = ncn.normalizeByOid( attribute, value.toString() );
	            }
	        }
	        else
	        {
	            if ( value instanceof String )
	            {
	                normalized = ncn.normalizeByName( attribute, ( String ) value );
	            }
	            else if ( value instanceof byte [] )
	            {
	                normalized = ncn.normalizeByName( attribute, ( byte[] ) value );
	            }
	            else
	            {
	                normalized = ncn.normalizeByName( attribute, value.toString() );
	            }
	        }
	        
	        return normalized;
	    }
	    catch ( NamingException ne )
	    {
	        log.warn( "Failed to normalize filter value: {}", ne.getMessage(), ne );
	        return null;
	    }
    	
    }
    
    /**
     * Visit a PresenceNode. If the attribute exists, the node is returned, otherwise
     * null is returned.
     */
    private ExprNode visitPresenceNode( PresenceNode node )
    {
        try
        {
            node.setAttribute( registry.getOid( node.getAttribute() ) );
            return node;
        }
        catch ( NamingException ne )
        {
            log.warn( "Failed to normalize filter node attribute: {}, error: {}", node.getAttribute(), ne.getMessage() );
            return null;
        }
    }

    /**
     * Visit a SimpleNode. If the attribute exists, the node is returned, otherwise
     * null is returned. SimpleNodes are :
     *  - ApproximateNode
     *  - EqualityNode
     *  - GreaterEqNode
     *  - LesserEqNode
     */
    private ExprNode visitSimpleNode( SimpleNode node )
    {
        // still need this check here in case the top level is a leaf node
        // with an undefined attributeType for its attribute
        if ( !ncn.isDefined( node.getAttribute() ) )
        {
            return null;
        }

       	Object normalized = normalizeValue( node.getAttribute(), node.getValue() );
        
        if ( normalized == null )
        {
        	return null;
        }

        try
        {
            node.setAttribute( registry.getOid( node.getAttribute() ) );
            node.setValue( normalized );
            return node;
        }
        catch ( NamingException ne )
        {
            log.warn( "Failed to normalize filter node attribute: {}, error: {}", node.getAttribute(), ne.getMessage() );
            return null;
        }
    }

    /**
     * Visit a SubstringNode. If the attribute exists, the node is returned, otherwise
     * null is returned. 
     * 
     * Normalizing substring value is pretty complex. It's not currently implemented...
     */
    private ExprNode visitSubstringNode( SubstringNode node )
    {
        // still need this check here in case the top level is a leaf node
        // with an undefined attributeType for its attribute
        if ( !ncn.isDefined( node.getAttribute() ) )
        {
        	return null;
        }

        Object normInitial = null;
        
        if ( node.getInitial() != null )
        {
	        normInitial = normalizeValue( node.getAttribute(), node.getInitial() );
	        
	        if ( normInitial == null )
	        {
	        	return null;
	        }
        }
        
        List<String> normAnys = null;
        
        if ( ( node.getAny() != null ) && ( node.getAny().size() != 0 ) )
        {
	        normAnys = new ArrayList<String>( node.getAny().size() );
	        
	        for ( String any:node.getAny() )
	        {
	        	Object normAny = normalizeValue( node.getAttribute(), any );
	        	
	        	if ( normAny != null )
	        	{
	        		normAnys.add( (String)normAny );
	        	}
	        }
        
	        if ( normAnys.size() == 0 )
	        {
	        	return null;
	        }
        }
        
        Object normFinal = null;
        
        if ( node.getFinal() != null )
        {
	        normFinal = normalizeValue( node.getAttribute(), node.getFinal() );
	        
	        if ( normFinal == null )
	        {
	        	return null;
	        }
        }
        
        
        try
        {
            node.setAttribute( registry.getOid( node.getAttribute() ) );
            node.setInitial( (String)normInitial );
            node.setAny( normAnys );
            node.setFinal( (String)normFinal );
            return node;
        }
        catch ( NamingException ne )
        {
            log.warn( "Failed to normalize filter node attribute: {}, error: {}", node.getAttribute(), ne.getMessage() );
            return null;
        }
    }

    /**
     * Visit a ExtensibleNode. If the attribute exists, the node is returned, otherwise
     * null is returned. 
     * 
     * TODO implement the logic for ExtensibleNode
     */
    private ExprNode visitExtensibleNode( ExtensibleNode node )
    {
        try
        {
            node.setAttribute( registry.getOid( node.getAttribute() ) );
            return node;
        }
        catch ( NamingException ne )
        {
            log.warn( "Failed to normalize filter node attribute: {}, error: {}", node.getAttribute(), ne.getMessage() );
            return null;
        }
    }

    /**
     * Visit a BranchNode. BranchNodes are :
     *  - AndNode
     *  - NotNode
     *  - OrNode
     */
    private ExprNode visitBranchNode( BranchNode node )
    {
    	// Two differente cases :
    	// - AND or OR
    	// - NOT
    	
    	if ( node instanceof NotNode )
    	{
        	// Manage the NOT
    		ExprNode child = node.getFirstChild();
    		
    		ExprNode result = (ExprNode)visit( child );
    		
    		if ( result == null )
    		{
    			return result;
    		}
    		else if ( result instanceof BranchNode )
    		{
    			node.setChildren( ((BranchNode)result).getChildren() );
    			return node;
    		}
    		else if ( result instanceof LeafNode )
    		{
    			List<ExprNode> newChildren = new ArrayList<ExprNode>(1); 
    			newChildren.add( result );
    			node.setChildren( newChildren );
    			return node;
    		}
    	}
    	else
    	{
    		// Manage AND and OR nodes.
    		BranchNode branchNode = (BranchNode)node;
            List<ExprNode> children = node.getChildren();
    		
    		// For AND and OR, we may have more than one children.
    		// We may have to remove some of them, so let's create
    		// a new handler to store the correct nodes.
    		List<ExprNode> newChildren = new ArrayList<ExprNode>( 
    				children.size() );
    		
    		// Now, iterate through all the children
    		for ( int i = 0; i < children.size(); i++ )
    		{
    			ExprNode child = children.get( i );
    			
    			ExprNode result = (ExprNode)visit( child );
    			
    			if ( result != null )
    			{
    				// As the node is correct, add it to the children 
    				// list.
    				newChildren.add( result );
    			}
    		}
            
            if ( ( branchNode instanceof AndNode ) && ( newChildren.size() != children.size() ) )
            {
                return null;
            }
    		
    		if ( newChildren.size() == 0 )
    		{
    			// No more children, return null
    			return null;
    		}
    		else if ( newChildren.size() == 1 )
    		{
    			// As we only have one child, return it
    			// to the caller.
    			return newChildren.get( 0 );
    		}
    		else
    		{
    			branchNode.setChildren( newChildren );
    		}
    	}
    	
    	return node;
    }
    
    /**
     * Visit the tree, normalizing the leaves and recusrsively visit the branches.
     * 
     * Here are the leaves we are visiting :
     * - PresenceNode ( attr =* )
     * - ExtensibleNode ( ? )
     * - SubStringNode ( attr = *X*Y* )
     * - ApproximateNode ( attr ~= value )
     * - EqualityNode ( attr = value )
     * - GreaterEqNode ( attr >= value )
     * - LessEqNode ( attr <= value )
     * 
     * The PresencNode is managed differently from other nodes, as it just check
     * for the attribute, not the value.
     */
    public Object visit( ExprNode node )
    {
        // -------------------------------------------------------------------
        // Handle PresenceNodes
        // -------------------------------------------------------------------
        
        if ( node instanceof PresenceNode )
        {
        	return visitPresenceNode( (PresenceNode)node );
        }

        // -------------------------------------------------------------------
        // Handle BranchNodes (AndNode, NotNode and OrNode)
        // -------------------------------------------------------------------
        
        else if ( node instanceof BranchNode )
        {
        	return visitBranchNode( (BranchNode)node );
        }

        // -------------------------------------------------------------------
        // Handle SimpleNodes (ApproximateNode, EqualityNode, GreaterEqNode,
        // and LesserEqNode) 
        // -------------------------------------------------------------------
        
        else if ( node instanceof SimpleNode )
        {
        	return visitSimpleNode( (SimpleNode)node );
        }
        else if ( node instanceof ExtensibleNode )
        {
        	return visitExtensibleNode( (ExtensibleNode)node );
        }
        else if ( node instanceof SubstringNode )
        {
        	return visitSubstringNode( (SubstringNode)node );
        }
        else
        {
        	return null;
        }
    }


    public boolean canVisit( ExprNode node )
    {
        return true;
    }


    public boolean isPrefix()
    {
        return false;
    }


    public List<ExprNode> getOrder( BranchNode node, List<ExprNode> children )
    {
        return children;
    }
}
