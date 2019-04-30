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
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import org.apache.directory.api.ldap.model.cursor.Cursor;
import org.apache.directory.api.ldap.model.cursor.CursorException;
import org.apache.directory.api.ldap.model.entry.Value;
import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.directory.api.ldap.model.exception.LdapOtherException;
import org.apache.directory.api.ldap.model.filter.AndNode;
import org.apache.directory.api.ldap.model.filter.ApproximateNode;
import org.apache.directory.api.ldap.model.filter.EqualityNode;
import org.apache.directory.api.ldap.model.filter.ExprNode;
import org.apache.directory.api.ldap.model.filter.GreaterEqNode;
import org.apache.directory.api.ldap.model.filter.LessEqNode;
import org.apache.directory.api.ldap.model.filter.NotNode;
import org.apache.directory.api.ldap.model.filter.OrNode;
import org.apache.directory.api.ldap.model.filter.PresenceNode;
import org.apache.directory.api.ldap.model.filter.ScopeNode;
import org.apache.directory.api.ldap.model.filter.SubstringNode;
import org.apache.directory.api.ldap.model.message.SearchScope;
import org.apache.directory.api.ldap.model.name.Dn;
import org.apache.directory.api.ldap.model.name.Rdn;
import org.apache.directory.api.ldap.model.schema.AttributeType;
import org.apache.directory.api.ldap.model.schema.MatchingRule;
import org.apache.directory.api.ldap.model.schema.Normalizer;
import org.apache.directory.api.ldap.model.schema.PrepareString;
import org.apache.directory.api.ldap.model.schema.normalizers.NoOpNormalizer;
import org.apache.directory.api.util.exception.NotImplementedException;
import org.apache.directory.server.core.api.partition.Partition;
import org.apache.directory.server.core.api.partition.PartitionTxn;
import org.apache.directory.server.i18n.I18n;
import org.apache.directory.server.xdbm.Index;
import org.apache.directory.server.xdbm.IndexEntry;
import org.apache.directory.server.xdbm.IndexNotFoundException;
import org.apache.directory.server.xdbm.ParentIdAndRdn;
import org.apache.directory.server.xdbm.SingletonIndexCursor;
import org.apache.directory.server.xdbm.Store;
import org.apache.directory.server.xdbm.search.PartitionSearchResult;
import org.apache.directory.server.xdbm.search.cursor.ApproximateCursor;
import org.apache.directory.server.xdbm.search.cursor.ChildrenCursor;
import org.apache.directory.server.xdbm.search.cursor.DescendantCursor;
import org.apache.directory.server.xdbm.search.evaluator.ApproximateEvaluator;


/**
 * Builds Cursors over candidates that satisfy a filter expression.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class CursorBuilder
{
    /** The database used by this builder */
    private Store db = null;

    /** Evaluator dependency on a EvaluatorBuilder */
    private EvaluatorBuilder evaluatorBuilder;


    /**
     * Creates an expression tree enumerator.
     *
     * @param db database used by this enumerator
     * @param evaluatorBuilder the evaluator builder
     */
    public CursorBuilder( Store db, EvaluatorBuilder evaluatorBuilder )
    {
        this.db = db;
        this.evaluatorBuilder = evaluatorBuilder;
    }


    public <T> long build( PartitionTxn partitionTxn, ExprNode node, PartitionSearchResult searchResult ) throws LdapException
    {
        Object count = node.get( DefaultOptimizer.COUNT_ANNOTATION );

        if ( ( count != null ) && ( ( Long ) count ) == 0L )
        {
            return 0;
        }

        try
        {
            switch ( node.getAssertionType() )
            {
            /* ---------- LEAF NODE HANDLING ---------- */
    
                case APPROXIMATE:
                    return computeApproximate( partitionTxn, ( ApproximateNode<T> ) node, searchResult );
    
                case EQUALITY:
                    return computeEquality( partitionTxn, ( EqualityNode<T> ) node, searchResult );
    
                case GREATEREQ:
                    return computeGreaterEq( partitionTxn, ( GreaterEqNode<T> ) node, searchResult );
    
                case LESSEQ:
                    return computeLessEq( partitionTxn, ( LessEqNode<T> ) node, searchResult );
    
                case PRESENCE:
                    return computePresence( partitionTxn, ( PresenceNode ) node, searchResult );
    
                case SCOPE:
                    if ( ( ( ScopeNode ) node ).getScope() == SearchScope.ONELEVEL )
                    {
                        return computeOneLevelScope( partitionTxn, ( ScopeNode ) node, searchResult );
                    }
                    else
                    {
                        return computeSubLevelScope( partitionTxn, ( ScopeNode ) node, searchResult );
                    }
    
                case SUBSTRING:
                    return computeSubstring( partitionTxn, ( SubstringNode ) node, searchResult );
    
                    /* ---------- LOGICAL OPERATORS ---------- */
    
                case AND:
                    return computeAnd( partitionTxn, ( AndNode ) node, searchResult );
    
                case NOT:
                    // Always return infinite, except if the resulting eva 
                    return computeNot( ( NotNode ) node, searchResult );
    
                case OR:
                    return computeOr( partitionTxn, ( OrNode ) node, searchResult );
    
                    /* ----------  NOT IMPLEMENTED  ---------- */
    
                case ASSERTION:
                case EXTENSIBLE:
                    throw new NotImplementedException();
    
                default:
                    throw new IllegalStateException( I18n.err( I18n.ERR_260, node.getAssertionType() ) );
            }
        }
        catch ( IndexNotFoundException | CursorException | IOException e )
        {
            throw new LdapOtherException( e.getMessage(), e );
        }
    }


    /**
     * Computes the set of candidates for an Approximate filter. We will feed the set only if
     * we have an index for the AT.
     */

    private <T> long computeApproximate( PartitionTxn partitionTxn, ApproximateNode<T> node, PartitionSearchResult searchResult )
        throws LdapException, IndexNotFoundException, CursorException, IOException
    {
        ApproximateCursor<T> cursor = new ApproximateCursor<>( partitionTxn, db,
            ( ApproximateEvaluator<T> ) evaluatorBuilder
                .build( partitionTxn, node ) );

        int nbResults = 0;
        Set<String> uuidSet = searchResult.getCandidateSet();

        while ( cursor.next() )
        {
            IndexEntry<T, String> indexEntry = cursor.get();

            String uuid = indexEntry.getId();
            boolean added = uuidSet.add( uuid );

            // if the UUID was added increment the result count
            if ( added )
            {
                nbResults++;
            }
        }

        cursor.close();

        return nbResults;
    }


    /**
     * Computes the set of candidates for an Equality filter. We will feed the set only if
     * we have an index for the AT.
     */
    private <T> long computeEquality( PartitionTxn partitionTxn, EqualityNode<T> node, PartitionSearchResult searchResult )
        throws LdapException, IndexNotFoundException, CursorException, IOException
    {
        Set<String> thisCandidates = ( Set<String> ) node.get( DefaultOptimizer.CANDIDATES_ANNOTATION_KEY );

        if ( thisCandidates != null )
        {
            Set<String> candidates = searchResult.getCandidateSet();

            for ( String candidate : thisCandidates )
            {
                candidates.add( candidate );
            }

            return thisCandidates.size();
        }

        AttributeType attributeType = node.getAttributeType();
        Value value = node.getValue();
        int nbResults = 0;

        // Fetch all the UUIDs if we have an index
        if ( db.hasIndexOn( attributeType ) )
        {
            // Get the cursor using the index
            Index<T, String> userIndex = ( Index<T, String> ) db.getIndex( attributeType );
            Cursor<IndexEntry<T, String>> userIdxCursor = userIndex.forwardCursor( partitionTxn, ( T ) value.getNormalized() );
            Set<String> uuidSet = searchResult.getCandidateSet();

            // And loop on it
            while ( userIdxCursor.next() )
            {
                IndexEntry<T, String> indexEntry = userIdxCursor.get();

                String uuid = indexEntry.getId();
                boolean added = uuidSet.add( uuid );
                
                // if the UUID was added increment the result count
                if ( added )
                {
                    nbResults++;
                }
            }

            userIdxCursor.close();
        }
        else
        {
            // No index, we will have to do a full scan
            return Long.MAX_VALUE;
        }

        return nbResults;
    }


    /**
     * Computes the set of candidates for an GreateEq filter. We will feed the set only if
     * we have an index for the AT.
     */
    private <T> long computeGreaterEq( PartitionTxn partitionTxn, GreaterEqNode<T> node, PartitionSearchResult searchResult )
        throws LdapException, IndexNotFoundException, CursorException, IOException
    {
        AttributeType attributeType = node.getAttributeType();
        Value value = node.getValue();
        int nbResults = 0;

        // Fetch all the UUIDs if we have an index
        if ( db.hasIndexOn( attributeType ) )
        {
            // Get the cursor using the index
            Index<T, String> userIndex = ( Index<T, String> ) db.getIndex( attributeType );
            Cursor<IndexEntry<T, String>> userIdxCursor = userIndex.forwardCursor( partitionTxn );

            // Position the index on the element we should start from
            IndexEntry<T, String> indexEntry = new IndexEntry<>();
            indexEntry.setKey( ( T ) value.getString() );

            userIdxCursor.before( indexEntry );
            Set<String> uuidSet = searchResult.getCandidateSet();

            // And loop on it
            while ( userIdxCursor.next() )
            {
                indexEntry = userIdxCursor.get();

                String uuid = indexEntry.getId();
                boolean added = uuidSet.add( uuid );

                // if the UUID was added increment the result count
                if ( added )
                {
                    nbResults++;
                }
            }

            userIdxCursor.close();
        }
        else
        {
            // No index, we will have to do a full scan
            return Long.MAX_VALUE;
        }

        return nbResults;
    }


    /**
     * Computes the set of candidates for an LessEq filter. We will feed the set only if
     * we have an index for the AT.
     */
    private <T> long computeLessEq( PartitionTxn partitionTxn, LessEqNode<T> node, PartitionSearchResult searchResult )
        throws LdapException, IndexNotFoundException, CursorException, IOException
    {
        AttributeType attributeType = node.getAttributeType();
        Value value = node.getValue();
        int nbResults = 0;

        // Fetch all the UUIDs if we have an index
        if ( db.hasIndexOn( attributeType ) )
        {
            // Get the cursor using the index
            Index<T, String> userIndex = ( Index<T, String> ) db.getIndex( attributeType );
            Cursor<IndexEntry<T, String>> userIdxCursor = userIndex.forwardCursor( partitionTxn );

            // Position the index on the element we should start from
            IndexEntry<T, String> indexEntry = new IndexEntry<>();
            indexEntry.setKey( ( T ) value.getString() );

            userIdxCursor.after( indexEntry );
            Set<String> uuidSet = searchResult.getCandidateSet();

            // And loop on it
            while ( userIdxCursor.previous() )
            {
                indexEntry = userIdxCursor.get();

                String uuid = indexEntry.getId();
                boolean added = uuidSet.add( uuid );

                // if the UUID was added increment the result count
                if ( added )
                {
                    nbResults++;
                }
            }

            userIdxCursor.close();
        }
        else
        {
            // No index, we will have to do a full scan
            return Long.MAX_VALUE;
        }

        return nbResults;
    }


    /**
     * Computes the set of candidates for a Presence filter. We will feed the set only if
     * we have an index for the AT.
     */
    private long computePresence( PartitionTxn partitionTxn, PresenceNode node, PartitionSearchResult searchResult )
        throws LdapException, CursorException, IOException
    {
        AttributeType attributeType = node.getAttributeType();
        int nbResults = 0;

        // Fetch all the UUIDs if we have an index
        if ( db.hasIndexOn( attributeType ) )
        {
            // Get the cursor using the index
            Cursor<IndexEntry<String, String>> presenceCursor = db.getPresenceIndex().forwardCursor(
                partitionTxn, attributeType.getOid() );

            // Position the index on the element we should start from
            Set<String> uuidSet = searchResult.getCandidateSet();

            // And loop on it
            while ( presenceCursor.next() )
            {
                IndexEntry<String, String> indexEntry = presenceCursor.get();

                String uuid = indexEntry.getId();
                boolean added = uuidSet.add( uuid );

                // if the UUID was added increment the result count
                if ( added )
                {
                    nbResults++;
                }
            }

            presenceCursor.close();
        }
        else
        {
            // No index, we will have to do a full scan
            return Long.MAX_VALUE;
        }

        return nbResults;
    }


    /**
     * Computes the set of candidates for a OneLevelScope filter. We will feed the set only if
     * we have an index for the AT.
     */
    private long computeOneLevelScope( PartitionTxn partitionTxn, ScopeNode node, PartitionSearchResult searchResult )
        throws LdapException, CursorException, IOException
    {
        int nbResults = 0;

        // We use the RdnIndex to get all the entries from a starting point
        // and below up to the number of children
        Cursor<IndexEntry<ParentIdAndRdn, String>> rdnCursor = db.getRdnIndex().forwardCursor( partitionTxn );

        IndexEntry<ParentIdAndRdn, String> startingPos = new IndexEntry<>();
        startingPos.setKey( new ParentIdAndRdn( node.getBaseId(), ( Rdn[] ) null ) );
        rdnCursor.before( startingPos );

        Cursor<IndexEntry<String, String>> scopeCursor = new ChildrenCursor( partitionTxn, db, node.getBaseId(), rdnCursor );
        Set<String> candidateSet = searchResult.getCandidateSet();

        // Fetch all the UUIDs if we have an index
        // And loop on it
        while ( scopeCursor.next() )
        {
            IndexEntry<String, String> indexEntry = scopeCursor.get();

            String uuid = indexEntry.getId();

            // If the entry is an alias, and we asked for it to be dereferenced,
            // we will dereference the alias
            if ( searchResult.isDerefAlways() || searchResult.isDerefInSearching() )
            {
                Dn aliasedDn = db.getAliasIndex().reverseLookup( partitionTxn, uuid );

                if ( aliasedDn != null )
                {
                    if ( !aliasedDn.isSchemaAware() )
                    {
                        aliasedDn = new Dn( evaluatorBuilder.getSchemaManager(), aliasedDn );
                    }

                    String aliasedId = db.getEntryId( partitionTxn, aliasedDn );

                    // This is an alias. Add it to the set of candidates to process, if it's not already
                    // present in the candidate set 
                    boolean added = candidateSet.add( aliasedId );
                    
                    if ( added )
                    {
                        nbResults++;
                    }
                }
                else
                {
                    // The UUID is not present in the Set, we add it
                    boolean added = candidateSet.add( uuid );
                    
                    // This is not an alias
                    if ( added )
                    {
                        nbResults++;
                    }
                }
            }
            else
            {
                // The UUID is not present in the Set, we add it
                boolean added = candidateSet.add( uuid );
                
                // This is not an alias
                if ( added )
                {
                    nbResults++;
                }
            }
        }

        scopeCursor.close();

        return nbResults;
    }


    /**
     * Computes the set of candidates for a SubLevelScope filter. We will feed the set only if
     * we have an index for the AT.
     */
    private long computeSubLevelScope( PartitionTxn partitionTxn, ScopeNode node, PartitionSearchResult searchResult )
        throws LdapException, IOException, CursorException
    {
        // If we are searching from the partition DN, better get out.
        String contextEntryId = db.getEntryId( partitionTxn, ( ( Partition ) db ).getSuffixDn() );

        if ( node.getBaseId() == contextEntryId )
        {
            return Long.MAX_VALUE;
        }

        int nbResults = 0;

        // We use the RdnIndex to get all the entries from a starting point
        // and below up to the number of descendant
        String baseId = node.getBaseId();
        ParentIdAndRdn parentIdAndRdn = db.getRdnIndex().reverseLookup( partitionTxn, baseId );
        IndexEntry<ParentIdAndRdn, String> startingPos = new IndexEntry<>();

        startingPos.setKey( parentIdAndRdn );
        startingPos.setId( baseId );

        Cursor<IndexEntry<ParentIdAndRdn, String>> rdnCursor = new SingletonIndexCursor<>( partitionTxn, 
            startingPos );
        String parentId = parentIdAndRdn.getParentId();

        Cursor<IndexEntry<String, String>> scopeCursor = new DescendantCursor( partitionTxn, db, baseId, parentId, rdnCursor );
        Set<String> candidateSet = searchResult.getCandidateSet();

        // Fetch all the UUIDs if we have an index
        // And loop on it
        while ( scopeCursor.next() )
        {
            IndexEntry<String, String> indexEntry = scopeCursor.get();

            String uuid = indexEntry.getId();

            // If the entry is an alias, and we asked for it to be dereferenced,
            // we will dereference the alias
            if ( searchResult.isDerefAlways() || searchResult.isDerefInSearching() )
            {
                Dn aliasedDn = db.getAliasIndex().reverseLookup( partitionTxn, uuid );

                if ( aliasedDn != null )
                {
                    if ( !aliasedDn.isSchemaAware() )
                    {
                        aliasedDn = new Dn( evaluatorBuilder.getSchemaManager(), aliasedDn );
                    }

                    String aliasedId = db.getEntryId( partitionTxn, aliasedDn );

                    // This is an alias. Add it to the set of candidates to process, if it's not already
                    // present in the candidate set 
                    boolean added = candidateSet.add( aliasedId );
                    
                    if ( added )
                    {
                        nbResults++;

                        ScopeNode newScopeNode = new ScopeNode(
                            node.getDerefAliases(),
                            aliasedDn,
                            aliasedId,
                            node.getScope() );

                        nbResults += computeSubLevelScope( partitionTxn, newScopeNode, searchResult );
                    }
                }
                else
                {
                    // This is not an alias
                    // The UUID is not present in the Set, we add it
                    boolean added = candidateSet.add( uuid );
                    
                    if ( added )
                    {
                        nbResults++;
                    }
                }
            }
            else
            {
                // The UUID is not present in the Set, we add it
                boolean added = candidateSet.add( uuid );
                
                if ( added )
                {
                    nbResults++;
                }
            }
        }

        scopeCursor.close();

        return nbResults;
    }


    /**
     * Computes the set of candidates for an Substring filter. We will feed the set only if
     * we have an index for the AT.
     */
    private long computeSubstring( PartitionTxn partitionTxn, SubstringNode node, PartitionSearchResult searchResult )
        throws LdapException, IndexNotFoundException, CursorException, IOException
    {
        AttributeType attributeType = node.getAttributeType();
        
        // Check if the AttributeType has a SubstringMatchingRule
        if ( attributeType.getSubstring() == null )
        {
            // No SUBSTRING matching rule : return 0
            return 0L;
        }

        // Fetch all the UUIDs if we have an index
        if ( db.hasIndexOn( attributeType ) )
        {
            Index<String, String> userIndex = ( Index<String, String> ) db.getIndex( attributeType );
            Cursor<IndexEntry<String, String>> cursor = userIndex.forwardCursor( partitionTxn );

            // Position the index on the element we should start from
            IndexEntry<String, String> indexEntry = new IndexEntry<>();
            String initial = node.getInitial();
            
            boolean fullIndexScan = false;
            
            if ( initial == null )
            {
                fullIndexScan = true;
                cursor.beforeFirst();
            }
            else
            {
                indexEntry.setKey( attributeType.getEquality().getNormalizer().normalize( initial, PrepareString.AssertionType.SUBSTRING_INITIAL ) );
                
                cursor.before( indexEntry );
            }
            
            int nbResults = 0;

            MatchingRule rule = attributeType.getSubstring();

            if ( rule == null )
            {
                rule = attributeType.getEquality();
            }

            Normalizer normalizer;
            Pattern regexp;

            if ( rule != null )
            {
                normalizer = rule.getNormalizer();
            }
            else
            {
                normalizer = new NoOpNormalizer( attributeType.getSyntaxOid() );
            }

            // compile the regular expression to search for a matching attribute
            // if the attributeType is humanReadable
            if ( attributeType.getSyntax().isHumanReadable() )
            {
                regexp = node.getRegex( normalizer );
            }
            else
            {
                regexp = null;
            }

            Set<String> uuidSet = searchResult.getCandidateSet();

            if ( regexp == null )
            {
                return nbResults;
            }
            
            // And loop on it
            while ( cursor.next() )
            {
                indexEntry = cursor.get();

                String key = indexEntry.getKey();

                boolean matched = regexp.matcher( key ).matches();
                
                if ( !fullIndexScan && !matched )
                {
                    cursor.close();

                    return nbResults;
                }

                if ( !matched )
                {
                    continue;
                }
                
                String uuid = indexEntry.getId();

                boolean added = uuidSet.add( uuid );
                
                // if the UUID was added increment the result count
                if ( added )
                {
                    nbResults++;
                }
            }

            cursor.close();

            return nbResults;
        }
        else
        {
            // No index, we will have to do a full scan
            return Long.MAX_VALUE;
        }
    }


    /**
     * Creates a OrCursor over a disjunction expression branch node.
     *
     * @param node the disjunction expression branch node
     * @return Cursor over candidates satisfying disjunction expression
     * @throws Exception on db access failures
     */
    private long computeOr( PartitionTxn partitionTxn, OrNode node, PartitionSearchResult searchResult ) 
        throws LdapException
    {
        List<ExprNode> children = node.getChildren();

        long nbOrResults = 0;

        // Recursively create Cursors and Evaluators for each child expression node
        for ( ExprNode child : children )
        {
            Object count = child.get( DefaultOptimizer.COUNT_ANNOTATION );

            if ( count != null )
            {
                long countLong = ( Long ) count;

                if ( countLong == 0 )
                {
                    // We can skip the cursor, it will not return any candidate
                    continue;
                }
                else if ( countLong == Long.MAX_VALUE )
                {
                    // We can stop here, we will anyway do a full scan
                    return countLong;
                }
            }

            long nbResults = build( partitionTxn, child, searchResult );

            if ( nbResults == Long.MAX_VALUE )
            {
                // We can stop here, we will anyway do a full scan
                return nbResults;
            }
            else
            {
                nbOrResults += nbResults;
            }
        }

        return nbOrResults;
    }


    /**
     * Creates an AndCursor over a conjunction expression branch node.
     *
     * @param node a conjunction expression branch node
     * @return Cursor over the conjunction expression
     * @throws Exception on db access failures
     */
    private long computeAnd( PartitionTxn partitionTxn, AndNode node, PartitionSearchResult searchResult ) 
        throws LdapException
    {
        int minIndex = 0;
        long minValue = Long.MAX_VALUE;
        long value;

        /*
         * We scan the child nodes of a branch node searching for the child
         * expression node with the smallest scan count.  This is the child
         * we will use for iteration
         */
        final List<ExprNode> children = node.getChildren();

        for ( int i = 0; i < children.size(); i++ )
        {
            ExprNode child = children.get( i );
            Object count = child.get( DefaultOptimizer.COUNT_ANNOTATION );

            if ( count == null )
            {
                continue;
            }

            value = ( Long ) count;

            if ( value == 0L )
            {
                // No need to go any further : we won't have matching candidates anyway
                return 0L;
            }

            if ( value < minValue )
            {
                minValue = value;
                minIndex = i;
            }
        }

        // Once found we return the number of candidates for this child
        ExprNode minChild = children.get( minIndex );

        return build( partitionTxn, minChild, searchResult );
    }


    /**
     * Creates an AndCursor over a conjunction expression branch node.
     *
     * @param node a conjunction expression branch node
     * @return Cursor over the conjunction expression
     * @throws Exception on db access failures
     */
    private long computeNot( NotNode node, PartitionSearchResult searchResult )
    {
        final List<ExprNode> children = node.getChildren();

        ExprNode child = children.get( 0 );
        Object count = child.get( DefaultOptimizer.COUNT_ANNOTATION );

        if ( count == null )
        {
            return Long.MAX_VALUE;
        }

        long value = ( Long ) count;

        if ( value == Long.MAX_VALUE )
        {
            // No need to go any further : we won't have matching candidates anyway
            return 0L;
        }

        return Long.MAX_VALUE;
    }
}
