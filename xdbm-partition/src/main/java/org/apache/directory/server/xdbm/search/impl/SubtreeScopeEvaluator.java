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


import java.util.UUID;

import org.apache.directory.server.core.api.partition.Partition;
import org.apache.directory.server.i18n.I18n;
import org.apache.directory.server.core.api.partition.index.IndexEntry;
import org.apache.directory.server.xdbm.Store;
import org.apache.directory.server.xdbm.search.Evaluator;
import org.apache.directory.shared.ldap.model.entry.Entry;
import org.apache.directory.shared.ldap.model.filter.ScopeNode;
import org.apache.directory.shared.ldap.model.message.SearchScope;


/**
 * Evaluates ScopeNode assertions with subtree scope on candidates using an
 * entry database.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class SubtreeScopeEvaluator implements Evaluator<ScopeNode>
{
    /** The ScopeNode containing initial search scope constraints */
    private final ScopeNode node;

    /** The entry identifier of the scope base */
    private final UUID baseId;

    /** 
     * Whether or not to accept all candidates.  If this evaluator's baseId is
     * set to the context entry's id, then obviously all candidates will be 
     * subordinate to this root ancestor or in subtree scope.  This check is 
     * done on  initialization and used there after.  One reason we need do 
     * this is because the subtree scope index (sub level index) does not map 
     * the values for the context entry id to it's subordinates since it would 
     * have to include all entries.  This is a waste of space and lookup time
     * since we know all entries will be subordinates in this case.
     */
    private final boolean baseIsContextEntry;

    /** True if the scope requires alias dereferencing while searching */
    private final boolean dereferencing;

    /** The entry database/store */
    private final Store db;


    /**
     * Creates a subtree scope node evaluator for search expressions.
     *
     * @param node the scope node
     * @param db the database used to evaluate scope node
     * @throws Exception on db access failure
     */
    public SubtreeScopeEvaluator( Store db, ScopeNode node ) throws Exception
    {
        this.db = db;
        this.node = node;

        if ( node.getScope() != SearchScope.SUBTREE )
        {
            throw new IllegalStateException( I18n.err( I18n.ERR_727 ) );
        }

        baseId = db.getEntryId( node.getBaseDn() );
        baseIsContextEntry = ( getContextEntryId().compareTo( baseId ) == 0 );
        dereferencing = node.getDerefAliases().isDerefInSearching() || node.getDerefAliases().isDerefAlways();
    }

    private UUID contextEntryId;


    // This will suppress PMD.EmptyCatchBlock warnings in this method
    @SuppressWarnings("PMD.EmptyCatchBlock")
    private UUID getContextEntryId() throws Exception
    {
        if ( contextEntryId == null )
        {
            try
            {
                this.contextEntryId = db.getEntryId( ((Partition)db).getSuffixDn() );
            }
            catch ( Exception e )
            {
                // might not have been created
                // might not have been created
            }
        }

        if ( contextEntryId == null )
        {
            return db.getDefaultId();
        }

        return contextEntryId;
    }


    /**
     * Asserts whether or not a candidate has sub level scope while taking
     * alias dereferencing into account.
     *
     * @param candidate the entry tested to see if it is in subtree scope
     * @return true if the candidate is within one level scope whether or not
     * alias dereferencing is enabled.
     * @throws Exception if the index lookups fail.
     * @see Evaluator#evaluate(org.apache.directory.server.xdbm.IndexEntry)
     */
    public boolean evaluate( IndexEntry<?> candidate ) throws Exception
    {
        UUID id = candidate.getId();
        
        /*
         * This condition catches situations where the candidate is equal to 
         * the base entry and when the base entry is the context entry.  Note
         * we do not store a mapping in the subtree index of the context entry
         * to all it's subordinates since that would be the entire set of 
         * entries in the db.
         */
        boolean isDescendant = baseIsContextEntry || baseId.equals( id ) || db.getSubLevelIndex().forward( baseId, id );

        /*
         * The candidate id could be any entry in the db.  If search
         * dereferencing is not enabled then we return the results of the
         * descendant test.
         */
        if ( !isDereferencing() )
        {
            return isDescendant;
        }

        /*
         * From here down alias dereferencing is enabled.  We determine if the
         * candidate id is an alias, if so we reject it since aliases should
         * not be returned.
         */
        if ( null != db.getAliasIndex().reverseLookup( id ) )
        {
            return false;
        }

        /*
         * The candidate is NOT an alias at this point.  So if it is a
         * descendant we just return true since it is in normal subtree scope.
         */
        if ( isDescendant )
        {
            return true;
        }

        /*
         * At this point the candidate is not a descendant and it is not an
         * alias.  We need to check if the candidate is in extended subtree
         * scope by performing a lookup on the subtree alias index.  This index
         * stores a tuple mapping the baseId to the ids of objects brought
         * into subtree scope of the base by an alias:
         *
         * ( baseId, aliasedObjId )
         *
         * If the candidate id is an object brought into subtree scope then
         * the lookup returns true accepting the candidate.  Otherwise the
         * candidate is rejected with a false return because it is not in scope.
         */
        return db.getSubAliasIndex().forward( baseId, id );
    }


    /**
     * Asserts whether or not a candidate has sub level scope while taking
     * alias dereferencing into account.
     *
     * @param candidate the entry tested to see if it is in subtree scope
     * @return true if the candidate is within one level scope whether or not
     * alias dereferencing is enabled.
     * @throws Exception if the index lookups fail.
     * @see Evaluator#evaluate(org.apache.directory.server.xdbm.IndexEntry)
     */
    public boolean evaluateEntry( Entry candidate ) throws Exception
    {
        throw new UnsupportedOperationException( I18n.err( I18n.ERR_721 ) );
    }


    public ScopeNode getExpression()
    {
        return node;
    }


    public UUID getBaseId()
    {
        return baseId;
    }


    public boolean isDereferencing()
    {
        return dereferencing;
    }
}
