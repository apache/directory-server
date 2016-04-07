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


import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import org.apache.directory.api.ldap.model.cursor.Cursor;
import org.apache.directory.api.ldap.model.entry.Value;
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
import org.apache.directory.api.ldap.model.schema.normalizers.NoOpNormalizer;
import org.apache.directory.api.util.exception.NotImplementedException;
import org.apache.directory.server.core.api.partition.Partition;
import org.apache.directory.server.i18n.I18n;
import org.apache.directory.server.xdbm.Index;
import org.apache.directory.server.xdbm.IndexEntry;
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


    public <T> long build( ExprNode node, PartitionSearchResult searchResult ) throws Exception
    {
        Object count = node.get( "count" );

        if ( ( count != null ) && ( ( Long ) count ) == 0L )
        {
            return 0;
        }

        switch ( node.getAssertionType() )
        {
        /* ---------- LEAF NODE HANDLING ---------- */

            case APPROXIMATE:
                return computeApproximate( ( ApproximateNode<T> ) node, searchResult );

            case EQUALITY:
                return computeEquality( ( EqualityNode<T> ) node, searchResult );

            case GREATEREQ:
                return computeGreaterEq( ( GreaterEqNode<T> ) node, searchResult );

            case LESSEQ:
                return computeLessEq( ( LessEqNode<T> ) node, searchResult );

            case PRESENCE:
                return computePresence( ( PresenceNode ) node, searchResult );

            case SCOPE:
                if ( ( ( ScopeNode ) node ).getScope() == SearchScope.ONELEVEL )
                {
                    return computeOneLevelScope( ( ScopeNode ) node, searchResult );
                }
                else
                {
                    return computeSubLevelScope( ( ScopeNode ) node, searchResult );
                }

            case SUBSTRING:
                return computeSubstring( ( SubstringNode ) node, searchResult );

                /* ---------- LOGICAL OPERATORS ---------- */

            case AND:
                return computeAnd( ( AndNode ) node, searchResult );

            case NOT:
                // Always return infinite, except if the resulting eva 
                return computeNot( ( NotNode ) node, searchResult );

            case OR:
                return computeOr( ( OrNode ) node, searchResult );

                /* ----------  NOT IMPLEMENTED  ---------- */

            case ASSERTION:
            case EXTENSIBLE:
                throw new NotImplementedException();

            default:
                throw new IllegalStateException( I18n.err( I18n.ERR_260, node.getAssertionType() ) );
        }
    }


    /**
     * Computes the set of candidates for an Approximate filter. We will feed the set only if
     * we have an index for the AT.
     */

    private <T> long computeApproximate( ApproximateNode<T> node, PartitionSearchResult searchResult )
        throws Exception
    {
        ApproximateCursor<T> cursor = new ApproximateCursor<T>( db,
            ( ApproximateEvaluator<T> ) evaluatorBuilder
                .build( node ) );

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
    private <T> long computeEquality( EqualityNode<T> node, PartitionSearchResult searchResult )
        throws Exception
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
        Value<T> value = node.getValue();
        int nbResults = 0;

        // Fetch all the UUIDs if we have an index
        if ( db.hasIndexOn( attributeType ) )
        {
            // Get the cursor using the index
            Index<T, String> userIndex = ( Index<T, String> ) db.getIndex( attributeType );
            Cursor<IndexEntry<T, String>> userIdxCursor = userIndex.forwardCursor( value.getValue() );
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
    private <T> long computeGreaterEq( GreaterEqNode<T> node, PartitionSearchResult searchResult )
        throws Exception
    {
        AttributeType attributeType = node.getAttributeType();
        Value<T> value = node.getValue();
        int nbResults = 0;

        // Fetch all the UUIDs if we have an index
        if ( db.hasIndexOn( attributeType ) )
        {
            // Get the cursor using the index
            Index<T, String> userIndex = ( Index<T, String> ) db.getIndex( attributeType );
            Cursor<IndexEntry<T, String>> userIdxCursor = userIndex.forwardCursor();

            // Position the index on the element we should start from
            IndexEntry<T, String> indexEntry = new IndexEntry<T, String>();
            indexEntry.setKey( value.getValue() );

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
    private <T> long computeLessEq( LessEqNode<T> node, PartitionSearchResult searchResult )
        throws Exception
    {
        AttributeType attributeType = node.getAttributeType();
        Value<T> value = node.getValue();
        int nbResults = 0;

        // Fetch all the UUIDs if we have an index
        if ( db.hasIndexOn( attributeType ) )
        {
            // Get the cursor using the index
            Index<T, String> userIndex = ( Index<T, String> ) db.getIndex( attributeType );
            Cursor<IndexEntry<T, String>> userIdxCursor = userIndex.forwardCursor();

            // Position the index on the element we should start from
            IndexEntry<T, String> indexEntry = new IndexEntry<T, String>();
            indexEntry.setKey( value.getValue() );

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
    private <T> long computePresence( PresenceNode node, PartitionSearchResult searchResult )
        throws Exception
    {
        AttributeType attributeType = node.getAttributeType();
        int nbResults = 0;

        // Fetch all the UUIDs if we have an index
        if ( db.hasIndexOn( attributeType ) )
        {
            // Get the cursor using the index
            Cursor<IndexEntry<String, String>> presenceCursor = db.getPresenceIndex().forwardCursor(
                attributeType.getOid() );

            // Position the index on the element we should start from
            IndexEntry<String, String> indexEntry = new IndexEntry<String, String>();
            Set<String> uuidSet = searchResult.getCandidateSet();

            // And loop on it
            while ( presenceCursor.next() )
            {
                indexEntry = presenceCursor.get();

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
    private long computeOneLevelScope( ScopeNode node, PartitionSearchResult searchResult )
        throws Exception
    {
        int nbResults = 0;

        // We use the RdnIndex to get all the entries from a starting point
        // and below up to the number of children
        Cursor<IndexEntry<ParentIdAndRdn, String>> rdnCursor = db.getRdnIndex().forwardCursor();

        IndexEntry<ParentIdAndRdn, String> startingPos = new IndexEntry<ParentIdAndRdn, String>();
        startingPos.setKey( new ParentIdAndRdn( node.getBaseId(), ( Rdn[] ) null ) );
        rdnCursor.before( startingPos );

        Cursor<IndexEntry<String, String>> scopeCursor = new ChildrenCursor( db, node.getBaseId(), rdnCursor );
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
                Dn aliasedDn = db.getAliasIndex().reverseLookup( uuid );

                if ( aliasedDn != null )
                {
                    aliasedDn.apply( evaluatorBuilder.getSchemaManager() );
                    String aliasedId = db.getEntryId( aliasedDn );

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
    private long computeSubLevelScope( ScopeNode node, PartitionSearchResult searchResult )
        throws Exception
    {
        // If we are searching from the partition DN, better get out.
        String contextEntryId = db.getEntryId( ( ( Partition ) db ).getSuffixDn() );

        if ( node.getBaseId() == contextEntryId )
        {
            return Long.MAX_VALUE;
        }

        int nbResults = 0;

        // We use the RdnIndex to get all the entries from a starting point
        // and below up to the number of descendant
        String baseId = node.getBaseId();
        ParentIdAndRdn parentIdAndRdn = db.getRdnIndex().reverseLookup( baseId );
        IndexEntry<ParentIdAndRdn, String> startingPos = new IndexEntry<ParentIdAndRdn, String>();

        startingPos.setKey( parentIdAndRdn );
        startingPos.setId( baseId );

        Cursor<IndexEntry<ParentIdAndRdn, String>> rdnCursor = new SingletonIndexCursor<ParentIdAndRdn>(
            startingPos );
        String parentId = parentIdAndRdn.getParentId();

        Cursor<IndexEntry<String, String>> scopeCursor = new DescendantCursor( db, baseId, parentId, rdnCursor );
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
                Dn aliasedDn = db.getAliasIndex().reverseLookup( uuid );

                if ( aliasedDn != null )
                {
                    aliasedDn.apply( evaluatorBuilder.getSchemaManager() );
                    String aliasedId = db.getEntryId( aliasedDn );

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

                        nbResults += computeSubLevelScope( newScopeNode, searchResult );
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
    private long computeSubstring( SubstringNode node, PartitionSearchResult searchResult )
        throws Exception
    {
        AttributeType attributeType = node.getAttributeType();

        // Fetch all the UUIDs if we have an index
        if ( db.hasIndexOn( attributeType ) )
        {
            Index<String, String> userIndex = ( ( Index<String, String> ) db.getIndex( attributeType ) );
            Cursor<IndexEntry<String, String>> cursor = userIndex.forwardCursor();

            // Position the index on the element we should start from
            IndexEntry<String, String> indexEntry = new IndexEntry<String, String>();
            String initial = node.getInitial();
            
            boolean fullIndexScan = false;
            
            if ( initial == null )
            {
                fullIndexScan = true;
                cursor.beforeFirst();
            }
            else
            {
                indexEntry.setKey( initial );
                
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

            // And loop on it
            while ( cursor.next() )
            {
                indexEntry = cursor.get();

                String key = indexEntry.getKey();

                boolean matched = regexp.matcher( key ).matches();
                
                if ( !fullIndexScan & !matched )
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
    private <T> long computeOr( OrNode node, PartitionSearchResult searchResult ) throws Exception
    {
        List<ExprNode> children = node.getChildren();

        long nbOrResults = 0;

        // Recursively create Cursors and Evaluators for each child expression node
        for ( ExprNode child : children )
        {
            Object count = child.get( "count" );

            if ( ( count != null ) && ( ( Long ) count == 0L ) )
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

            long nbResults = build( child, searchResult );

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
    private long computeAnd( AndNode node, PartitionSearchResult searchResult ) throws Exception
    {
        int minIndex = 0;
        long minValue = Long.MAX_VALUE;
        long value = Long.MAX_VALUE;

        /*
         * We scan the child nodes of a branch node searching for the child
         * expression node with the smallest scan count.  This is the child
         * we will use for iteration
         */
        final List<ExprNode> children = node.getChildren();

        for ( int i = 0; i < children.size(); i++ )
        {
            ExprNode child = children.get( i );
            Object count = child.get( "count" );

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
        long nbResults = build( minChild, searchResult );

        return nbResults;
    }


    /**
     * Creates an AndCursor over a conjunction expression branch node.
     *
     * @param node a conjunction expression branch node
     * @return Cursor over the conjunction expression
     * @throws Exception on db access failures
     */
    private long computeNot( NotNode node, PartitionSearchResult searchResult ) throws Exception
    {
        final List<ExprNode> children = node.getChildren();

        ExprNode child = children.get( 0 );
        Object count = child.get( "count" );

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
