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
package org.apache.directory.server.xdbm.search.impl;


import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.directory.api.ldap.model.constants.SchemaConstants;
import org.apache.directory.api.ldap.model.cursor.Cursor;
import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.directory.api.ldap.model.exception.LdapOtherException;
import org.apache.directory.api.ldap.model.filter.AndNode;
import org.apache.directory.api.ldap.model.filter.ApproximateNode;
import org.apache.directory.api.ldap.model.filter.AssertionNode;
import org.apache.directory.api.ldap.model.filter.BranchNode;
import org.apache.directory.api.ldap.model.filter.EqualityNode;
import org.apache.directory.api.ldap.model.filter.ExprNode;
import org.apache.directory.api.ldap.model.filter.ExtensibleNode;
import org.apache.directory.api.ldap.model.filter.GreaterEqNode;
import org.apache.directory.api.ldap.model.filter.LeafNode;
import org.apache.directory.api.ldap.model.filter.LessEqNode;
import org.apache.directory.api.ldap.model.filter.NotNode;
import org.apache.directory.api.ldap.model.filter.OrNode;
import org.apache.directory.api.ldap.model.filter.PresenceNode;
import org.apache.directory.api.ldap.model.filter.ScopeNode;
import org.apache.directory.api.ldap.model.filter.SimpleNode;
import org.apache.directory.api.ldap.model.filter.SubstringNode;
import org.apache.directory.api.util.Strings;
import org.apache.directory.server.core.api.partition.Partition;
import org.apache.directory.server.core.api.partition.PartitionTxn;
import org.apache.directory.server.i18n.I18n;
import org.apache.directory.server.xdbm.Index;
import org.apache.directory.server.xdbm.IndexNotFoundException;
import org.apache.directory.server.xdbm.Store;
import org.apache.directory.server.xdbm.search.Optimizer;


/**
 * Optimizer that annotates the filter using scan counts.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class DefaultOptimizer implements Optimizer
{
    /* Package protected*/ static final String CANDIDATES_ANNOTATION_KEY = "candidates";
    
    /* Package protected*/ static final String COUNT_ANNOTATION = "count"; 

    /** the database this optimizer operates on */
    private final Store db;
    private String contextEntryId;


    /**
     * Creates an optimizer on a database.
     *
     * @param db the database this optimizer works for.
     */
    public DefaultOptimizer( Store db )
    {
        this.db = db;
    }


    // This will suppress PMD.EmptyCatchBlock warnings in this method
    @SuppressWarnings("PMD.EmptyCatchBlock")
    private String getContextEntryId( PartitionTxn partitionTxn ) throws LdapException
    {
        if ( contextEntryId == null )
        {
            try
            {
                this.contextEntryId = db.getEntryId( partitionTxn, ( ( Partition ) db ).getSuffixDn() );
            }
            catch ( Exception e )
            {
                // might not have been created
            }
        }

        if ( contextEntryId == null )
        {
            return Partition.DEFAULT_ID;
        }

        return contextEntryId;
    }


    /**
     * Annotates the expression tree to determine optimal evaluation order based
     * on the scan count for indices that exist for each expression node.  If an
     * index on the attribute does not exist an IndexNotFoundException will be
     * thrown.
     *
     * {@inheritDoc}
     */
    @Override
    @SuppressWarnings("unchecked")
    public Long annotate( PartitionTxn partitionTxn, ExprNode node ) throws LdapException
    {
        // Start off with the worst case unless scan count says otherwise.
        Long count = Long.MAX_VALUE;

        /* --------------------------------------------------------------------
         *                 H A N D L E   L E A F   N O D E S          
         * --------------------------------------------------------------------
         * 
         * Each leaf node is based on an attribute and it represents a condition
         * that needs to be statisfied.  We ask the index (if one exists) for 
         * the attribute to give us a scan count of all the candidates that 
         * would satisfy the attribute assertion represented by the leaf node.
         * 
         * This is conducted differently based on the type of the leaf node.
         * Comments on each node type explain how each scan count is arrived at.
         */

        if ( node instanceof ScopeNode )
        {
            count = getScopeScan( partitionTxn, ( ScopeNode ) node );
        }
        else if ( node instanceof AssertionNode )
        {
            /* 
             * Leave it up to the assertion node to determine just how much it
             * will cost us.  Anyway it defaults to a maximum scan count if a
             * scan count is not specified by the implementation.
             */
        }
        else if ( node.isLeaf() )
        {
            LeafNode leaf = ( LeafNode ) node;

            try
            {  
                if ( node instanceof PresenceNode )
                {
                    count = getPresenceScan( partitionTxn, ( PresenceNode ) leaf );
                }
                else if ( node instanceof EqualityNode )
                {
                    count = getEqualityScan( partitionTxn, ( EqualityNode ) leaf );
                }
                else if ( node instanceof GreaterEqNode )
                {
                    count = getGreaterLessScan( partitionTxn, ( GreaterEqNode ) leaf, SimpleNode.EVAL_GREATER );
                }
                else if ( node instanceof LessEqNode )
                {
                    count = getGreaterLessScan( partitionTxn, ( SimpleNode ) leaf, SimpleNode.EVAL_LESSER );
                }
                else if ( node instanceof SubstringNode )
                {
                    /** Cannot really say so we presume the total index count */
                    count = getSubstringScan( partitionTxn, ( SubstringNode ) leaf );
                }
                else if ( node instanceof ExtensibleNode )
                {
                    /** Cannot really say so we presume the total index count */
                    count = getFullScan( partitionTxn, leaf );
                }
                else if ( node instanceof ApproximateNode )
                {
                    /** Feature not implemented so we just use equality matching */
                    count = getEqualityScan( partitionTxn, ( ApproximateNode ) leaf );
                }
                else
                {
                    throw new IllegalArgumentException( I18n.err( I18n.ERR_711 ) );
                }
            }
            catch ( IndexNotFoundException | IOException e )
            {
                throw new LdapOtherException( e.getMessage(), e );
            }
        }
        // --------------------------------------------------------------------
        //                 H A N D L E   B R A N C H   N O D E S       
        // --------------------------------------------------------------------
        else
        {
            if ( node instanceof AndNode )
            {
                count = getConjunctionScan( partitionTxn, ( AndNode ) node );
            }
            else if ( node instanceof OrNode )
            {
                count = getDisjunctionScan( partitionTxn, ( OrNode ) node );
            }
            else if ( node instanceof NotNode )
            {
                annotate( partitionTxn, ( ( NotNode ) node ).getFirstChild() );

                /*
                 * A negation filter is always worst case since we will have
                 * to retrieve all entries from the master table then test
                 * each one against the negated child filter.  There is no way
                 * to use the indices.
                 */
                count = Long.MAX_VALUE;
            }
            else
            {
                count = Long.MAX_VALUE;
            }
        }

        // Protect against overflow when counting.
        if ( count < 0L )
        {
            count = Long.MAX_VALUE;
        }

        node.set( COUNT_ANNOTATION, count );

        return count;
    }


    /**
     * ANDs or Conjunctions take the count of the smallest child as their count.
     * This is the best that a conjunction can do and should be used rather than
     * the worst case. Notice that we annotate the child node with a recursive 
     * call before accessing its count parameter making the chain recursion 
     * depth first.
     *
     * @param node a AND (Conjunction) BranchNode
     * @return the calculated scan count
     * @throws Exception if there is an error
     */
    private long getConjunctionScan( PartitionTxn partitionTxn, BranchNode node ) throws LdapException
    {
        long count = Long.MAX_VALUE;
        List<ExprNode> children = node.getChildren();

        for ( ExprNode child : children )
        {
            if ( ( count == 1 ) && ( child instanceof ScopeNode ) )
            {
                // We can stop here
                break;
            }

            annotate( partitionTxn, child );
            count = Math.min( ( ( Long ) child.get( COUNT_ANNOTATION ) ), count );

            if ( count == 0 )
            {
                // No need to continue
                break;
            }
        }

        return count;
    }


    /**
     * Disjunctions (OR) are the union of candidates across all subexpressions 
     * so we add all the counts of the child nodes. Notice that we annotate the 
     * child node with a recursive call.
     *
     * @param node the OR branch node
     * @return the scan count on the OR node
     * @throws Exception if there is an error
     */
    private long getDisjunctionScan( PartitionTxn partitionTxn, BranchNode node ) throws LdapException
    {
        List<ExprNode> children = node.getChildren();
        long total = 0L;

        for ( ExprNode child : children )
        {
            annotate( partitionTxn, child );
            total += ( Long ) child.get( COUNT_ANNOTATION );

            if ( total == Long.MAX_VALUE )
            {
                // We can stop here withoit evaluating the following filters
                break;
            }
        }

        return total;
    }


    /**
     * Gets the worst case scan count for all entries that satisfy the equality
     * assertion in the SimpleNode argument.  
     *
     * @param node the node to get a scan count for 
     * @return the worst case
     * @throws Exception if there is an error accessing an index
     */
    @SuppressWarnings("unchecked")
    private <V> long getEqualityScan( PartitionTxn partitionTxn, SimpleNode<V> node ) throws LdapException, IndexNotFoundException, IOException
    {
        if ( db.hasIndexOn( node.getAttributeType() ) )
        {
            Index<V, String> idx = ( Index<V, String> ) db.getIndex( node.getAttributeType() );

            String normalizedKey;
            
            if ( node.getValue().isSchemaAware() )
            {
                normalizedKey = node.getValue().getNormalized();
            }
            else
            {
                normalizedKey = node.getAttributeType().getEquality().getNormalizer().normalize( node.getValue().getString() );
            }
            
            Cursor<String> result = idx.forwardValueCursor( partitionTxn, ( V ) normalizedKey );
            Set<String> values = new HashSet<>();
            int nbFound = 0;

            for ( String value : result )
            {
                values.add( value );
                nbFound++;

                // Arbitrary stop gathering the candidates if we have more than 100
                if ( nbFound == 100 )
                {
                    break;
                }
            }

            result.close();

            if ( nbFound < 100 )
            {
                // Store the found candidates in the node
                node.set( CANDIDATES_ANNOTATION_KEY, values );

                return values.size();
            }
            else
            {
                // Reset the candidates annotation
                node.set( CANDIDATES_ANNOTATION_KEY, null );

                return idx.count( partitionTxn, ( V ) node.getValue().getNormalized() );
            }
        }

        // count for non-indexed attribute is unknown so we presume da worst
        return Long.MAX_VALUE;
    }


    /**
     * Gets a scan count of the nodes that satisfy the greater or less than test
     * specified by the node.
     *
     * @param node the greater or less than node to get a count for 
     * @param isGreaterThan if true test is for >=, otherwise <=
     * @return the scan count of all nodes satisfying the Ava
     * @throws Exception if there is an error accessing an index
     */
    @SuppressWarnings("unchecked")
    private <V> long getGreaterLessScan( PartitionTxn partitionTxn, SimpleNode<V> node, boolean isGreaterThan ) throws LdapException, IndexNotFoundException
    {
        if ( db.hasIndexOn( node.getAttributeType() ) )
        {
            Index<V, String> idx = ( Index<V, String> ) db.getIndex( node.getAttributeType() );

            if ( isGreaterThan )
            {
                return idx.greaterThanCount( partitionTxn, ( V ) node.getValue().getString() );
            }
            else
            {
                return idx.lessThanCount( partitionTxn, ( V ) node.getValue().getString() );
            }
        }

        // count for non-indexed attribute is unknown so we presume da worst
        return Long.MAX_VALUE;
    }


    /**
     * Get a scan count based on a Substring node : we will count the entries that are greater
     * than ABC where the filter is (attr=ABC*). Any other filter won't be evaluated (for instance,
     * a filter like (attr=*ABC) will resolve to a full scan atm - we could have created a reverted
     * index for such a case -, and filters like (attr=*ABC*) also esolve to a full scan).
     * 
     * @param node The substring node
     * @return The number of candidates
     * @throws Exception If there is an error accessing an index
     */
    private long getSubstringScan( PartitionTxn partitionTxn, SubstringNode node ) throws LdapException, IndexNotFoundException
    {
        if ( db.hasIndexOn( node.getAttributeType() ) )
        {
            Index<String, String> idx = ( Index<String, String> ) db.getIndex( node.getAttributeType() );

            String initial = node.getInitial();

            if ( Strings.isEmpty( initial ) )
            {
                // Not a (attr=ABC*) filter : full index scan
                return idx.count( partitionTxn );
            }
            else
            {
                return idx.greaterThanCount( partitionTxn, initial );
            }
        }
        else
        {
            // count for non-indexed attribute is unknown so we presume da worst
            return Long.MAX_VALUE;
        }
    }


    /**
     * Gets the total number of entries within the database index if one is 
     * available otherwise the count of all the entries within the database is
     * returned.
     *
     * @param node the leaf node to get a full scan count for 
     * @return the worst case full scan count
     * @throws Exception if there is an error access database indices
     */
    private long getFullScan( PartitionTxn partitionTxn, LeafNode node ) throws LdapException, IndexNotFoundException
    {
        if ( db.hasIndexOn( node.getAttributeType() ) )
        {
            Index<?, ?> idx = db.getIndex( node.getAttributeType() );
            return idx.count( partitionTxn );
        }

        return Long.MAX_VALUE;
    }


    /**
     * Gets the number of entries that would be returned by a presence node
     * assertion.  Leverages the presence system index for scan counts.
     *
     * @param node the presence node
     * @return the number of entries matched for the presence of an attribute
     * @throws Exception if errors result
     */
    private long getPresenceScan( PartitionTxn partitionTxn, PresenceNode node ) throws LdapException
    {
        if ( db.hasUserIndexOn( node.getAttributeType() )
             || node.getAttributeType().getOid().equals( SchemaConstants.ADMINISTRATIVE_ROLE_AT_OID ) )
        {
            Index<String, String> presenceIndex = db.getPresenceIndex();

            return presenceIndex.count( partitionTxn, node.getAttributeType().getOid() );
        }
        else if ( db.hasSystemIndexOn( node.getAttributeType() )
            || ( node.getAttributeType().getOid() == SchemaConstants.ENTRY_UUID_AT_OID ) )
        {
            // the system indices (objectClass, entryUUID and entryCSN) are maintained for
            // each entry, so we could just return the database count
            return db.count( partitionTxn );
        }

        return Long.MAX_VALUE;
    }


    /**
     * Gets the scan count for the scope node attached to this filter.
     *
     * @param node the ScopeNode
     * @return the scan count for scope
     * @throws Exception if any errors result
     */
    private long getScopeScan( PartitionTxn partitionTxn, ScopeNode node ) throws LdapException
    {
        String id = node.getBaseId();

        switch ( node.getScope() )
        {
            case OBJECT:
                return 1L;

            case ONELEVEL:
                return db.getChildCount( partitionTxn, id );

            case SUBTREE:
                if ( Strings.equals( id, getContextEntryId( partitionTxn ) ) )
                {
                    return db.count( partitionTxn );
                }
                else
                {
                    return db.getRdnIndex().reverseLookup( partitionTxn, id ).getNbDescendants() + 1L;
                }

            default:
                throw new IllegalArgumentException( I18n.err( I18n.ERR_713 ) );
        }
    }
}
