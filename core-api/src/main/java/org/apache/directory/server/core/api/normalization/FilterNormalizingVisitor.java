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
package org.apache.directory.server.core.api.normalization;


import java.util.ArrayList;
import java.util.List;

import org.apache.directory.api.ldap.model.entry.Value;
import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.directory.api.ldap.model.filter.AndNode;
import org.apache.directory.api.ldap.model.filter.BranchNode;
import org.apache.directory.api.ldap.model.filter.ExprNode;
import org.apache.directory.api.ldap.model.filter.ExtensibleNode;
import org.apache.directory.api.ldap.model.filter.FilterVisitor;
import org.apache.directory.api.ldap.model.filter.LeafNode;
import org.apache.directory.api.ldap.model.filter.NotNode;
import org.apache.directory.api.ldap.model.filter.PresenceNode;
import org.apache.directory.api.ldap.model.filter.SimpleNode;
import org.apache.directory.api.ldap.model.filter.SubstringNode;
import org.apache.directory.api.ldap.model.filter.UndefinedNode;
import org.apache.directory.api.ldap.model.schema.AttributeType;
import org.apache.directory.api.ldap.model.schema.MatchingRule;
import org.apache.directory.api.ldap.model.schema.Normalizer;
import org.apache.directory.api.ldap.model.schema.PrepareString.AssertionType;
import org.apache.directory.api.ldap.model.schema.SchemaManager;
import org.apache.directory.api.ldap.model.schema.normalizers.NameComponentNormalizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


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
 * all but the topmost branch node which we cannot replace.  So again the top most branch
 * node must be inspected by code outside of this visitor.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class FilterNormalizingVisitor implements FilterVisitor
{
    /** logger used by this class */
    private static final Logger LOG = LoggerFactory.getLogger( FilterNormalizingVisitor.class );

    /** the name component normalizer used by this visitor */
    private final NameComponentNormalizer ncn;

    /** the SchemaManager instance used to resolve OIDs for attributeType ids */
    private final SchemaManager schemaManager;

    /**
     * Chars which need to be escaped in a filter
     * '\0' | '(' | ')' | '*' | '\'
     */
    private static final boolean[] FILTER_CHAR =
        { 
            true,  false, false, false, false, false, false, false, // 00 -> 07 NULL
            false, false, false, false, false, false, false, false, // 08 -> 0F
            false, false, false, false, false, false, false, false, // 10 -> 17
            false, false, false, false, false, false, false, false, // 18 -> 1F
            false, false, false, false, false, false, false, false, // 20 -> 27
            true,  true,  true,  false, false, false, false, false, // 28 -> 2F '(', ')', '*'
            false, false, false, false, false, false, false, false, // 30 -> 37
            false, false, false, false, false, false, false, false, // 38 -> 3F 
            false, false, false, false, false, false, false, false, // 40 -> 47
            false, false, false, false, false, false, false, false, // 48 -> 4F
            false, false, false, false, false, false, false, false, // 50 -> 57
            false, false, false, false, true,  false, false, false, // 58 -> 5F '\'
            false, false, false, false, false, false, false, false, // 60 -> 67
            false, false, false, false, false, false, false, false, // 68 -> 6F
            false, false, false, false, false, false, false, false, // 70 -> 77
            false, false, false, false, false, false, false, false  // 78 -> 7F
    };


    /**
     * 
     * Creates a new instance of NormalizingVisitor.
     *
     * @param ncn The name component normalizer to use
     * @param schemaManager The schemaManager
     */
    public FilterNormalizingVisitor( NameComponentNormalizer ncn, SchemaManager schemaManager )
    {
        this.ncn = ncn;
        this.schemaManager = schemaManager;
    }


    /**
     * Check if the given char is a filter escaped char
     * &lt;filterEscapedChars&gt; ::= '\0' | '(' | ')' | '*' | '\'
     *
     * @param c the char we want to test
     * @return true if the char is a pair char only
     */
    public static boolean isFilterChar( char c )
    {
        return ( ( c | 0x7F ) == 0x7F ) && FILTER_CHAR[c & 0x7f];
    }


    /**
     * A private method used to normalize a value. At this point, the value
     * is a Value<byte[]>, we have to translate it to a Value<String> if its
     * AttributeType is H-R. Then we have to normalize the value accordingly
     * to the AttributeType Normalizer.
     * 
     * @param attribute The attribute's ID
     * @param value The value to normalize
     * @return the normalized value
     */
    private Value normalizeValue( AttributeType attributeType, Value value )
    {
        try
        {
            Value normalized;

            if ( attributeType.getSyntax().isHumanReadable() )
            {
                normalized = new Value( attributeType, value.getString() );
            }
            else
            {
                normalized = ( Value ) ncn.normalizeByName( attributeType.getOid(), value.getBytes() );
            }

            return normalized;
        }
        catch ( LdapException ne )
        {
            LOG.warn( "Failed to normalize filter value: {}", ne.getLocalizedMessage(), ne );
            return null;
        }
    }


    /**
     * Visit a PresenceNode. If the attribute exists, the node is returned, otherwise
     * null is returned.
     * 
     * @param node the node to visit
     * @return The visited node
     */
    private ExprNode visitPresenceNode( PresenceNode node ) throws LdapException
    {
        // still need this check here in case the top level is a leaf node
        // with an undefined attributeType for its attribute
        if ( !ncn.isDefined( node.getAttribute() ) )
        {
            return null;
        }

        node.setAttributeType( schemaManager.lookupAttributeTypeRegistry( node.getAttribute() ) );

        return node;
    }


    /**
     * Visit a SimpleNode. If the attribute exists, the node is returned, otherwise
     * null is returned. SimpleNodes are :
     *  - ApproximateNode
     *  - EqualityNode
     *  - GreaterEqNode
     *  - LesserEqNode
     *  
     * @param node the node to visit
     * @return the visited node
     */
    private ExprNode visitSimpleNode( SimpleNode node ) throws LdapException
    {
        if ( node.getAttributeType() == null )
        {
            // still need this check here in case the top level is a leaf node
            // with an undefined attributeType for its attribute
            if ( !ncn.isDefined( node.getAttribute() ) )
            {
                return null;
            }

            AttributeType attributeType = schemaManager.lookupAttributeTypeRegistry( node.getAttribute() );
            node.setAttributeType( attributeType );
        }

        Value normalized = normalizeValue( node.getAttributeType(), node.getValue() );

        if ( normalized == null )
        {
            return null;
        }

        node.setValue( normalized );

        return node;
    }


    /**
     * Visit a SubstringNode. If the attribute exists, the node is returned, otherwise
     * null is returned. 
     * 
     * Normalizing substring value is pretty complex. It's not currently implemented...
     * 
     * @param node the node to visit
     * @return the visited node
     */
    private ExprNode visitSubstringNode( SubstringNode node ) throws LdapException
    {
        AttributeType attributeType = schemaManager.lookupAttributeTypeRegistry( node.getAttribute() );
        MatchingRule substringMR = attributeType.getSubstring();
        
        if ( ( substringMR == null ) || ( substringMR.getNormalizer() == null ) )
        {
            // No normalizer for a Substring filter
            return new UndefinedNode( node.getAttribute() );
        }
        
        Normalizer normalizer = substringMR.getNormalizer();
        node.setAttributeType( attributeType );

        if ( node.getInitial() != null )
        {
            String normalizedInitial = normalizer.normalize( node.getInitial(), AssertionType.SUBSTRING_INITIAL );

            node.setInitial( normalizedInitial );
        }

        List<String> normAnys = null;

        if ( ( node.getAny() != null ) && ( !node.getAny().isEmpty() ) )
        {
            normAnys = new ArrayList<>( node.getAny().size() );

            for ( String any : node.getAny() )
            {
                String normalizedAny = normalizer.normalize( any, AssertionType.SUBSTRING_ANY );

                if ( normalizedAny != null )
                {
                    normAnys.add( normalizedAny );
                }
            }

            if ( normAnys.isEmpty() )
            {
                return null;
            }
        }

        if ( node.getFinal() != null )
        {
            String normalizedFinal = normalizer.normalize( node.getFinal(), AssertionType.SUBSTRING_FINAL );

            if ( normalizedFinal != null )
            {
                node.setFinal( normalizedFinal );
            }
        }

        node.setAny( normAnys );

        return node;
    }


    /**
     * Visit a ExtensibleNode. If the attribute exists, the node is returned, otherwise
     * null is returned. 
     * 
     * TODO implement the logic for ExtensibleNode
     * 
     * @param node the node to visit
     * @return the visited node
     */
    private ExprNode visitExtensibleNode( ExtensibleNode node ) throws LdapException
    {
        // still need this check here in case the top level is a leaf node
        // with an undefined attributeType for its attribute
        if ( !ncn.isDefined( node.getAttribute() ) )
        {
            return null;
        }

        node.setAttributeType( schemaManager.lookupAttributeTypeRegistry( node.getAttribute() ) );

        return node;
    }


    /**
     * Visit a BranchNode. BranchNodes are :
     *  - AndNode
     *  - NotNode
     *  - OrNode
     *  
     * @param node the node to visit
     * @return the visited node
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

            ExprNode result = ( ExprNode ) visit( child );

            if ( result == null )
            {
                return null;
            }
            else if ( result instanceof BranchNode )
            {
                List<ExprNode> newChildren = new ArrayList<>( 1 );
                newChildren.add( result );
                node.setChildren( newChildren );
                
                return node;
            }
            else if ( result instanceof LeafNode )
            {
                List<ExprNode> newChildren = new ArrayList<>( 1 );
                newChildren.add( result );
                node.setChildren( newChildren );
                
                return node;
            }
        }
        else
        {
            // Manage AND and OR nodes.
            BranchNode branchNode = node;
            List<ExprNode> children = node.getChildren();

            // For AND and OR, we may have more than one children.
            // We may have to remove some of them, so let's create
            // a new handler to store the correct nodes.
            List<ExprNode> newChildren = new ArrayList<>( children.size() );

            // Now, iterate through all the children
            for ( int i = 0; i < children.size(); i++ )
            {
                ExprNode child = children.get( i );

                ExprNode result = ( ExprNode ) visit( child );

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

            if ( newChildren.isEmpty() )
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
     * - GreaterEqNode ( attr &gt;= value )
     * - LessEqNode ( attr &lt;= value )
     * 
     * The PresencNode is managed differently from other nodes, as it just check
     * for the attribute, not the value.
     * 
     * @param node the node to visit
     * @return the visited node
     */
    @Override
    public Object visit( ExprNode node )
    {
        try
        {
            // -------------------------------------------------------------------
            // Handle PresenceNodes
            // -------------------------------------------------------------------

            if ( node instanceof PresenceNode )
            {
                return visitPresenceNode( ( PresenceNode ) node );
            }

            // -------------------------------------------------------------------
            // Handle BranchNodes (AndNode, NotNode and OrNode)
            // -------------------------------------------------------------------

            else if ( node instanceof BranchNode )
            {
                return visitBranchNode( ( BranchNode ) node );
            }

            // -------------------------------------------------------------------
            // Handle SimpleNodes (ApproximateNode, EqualityNode, GreaterEqNode,
            // and LesserEqNode) 
            // -------------------------------------------------------------------

            else if ( node instanceof SimpleNode )
            {
                return visitSimpleNode( ( SimpleNode ) node );
            }
            else if ( node instanceof ExtensibleNode )
            {
                return visitExtensibleNode( ( ExtensibleNode ) node );
            }
            else if ( node instanceof SubstringNode )
            {
                return visitSubstringNode( ( SubstringNode ) node );
            }
            else
            {
                return null;
            }
        }
        catch ( LdapException e )
        {
            throw new RuntimeException( e );
        }
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public boolean canVisit( ExprNode node )
    {
        return true;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isPrefix()
    {
        return false;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public List<ExprNode> getOrder( BranchNode node, List<ExprNode> children )
    {
        return children;
    }
}
