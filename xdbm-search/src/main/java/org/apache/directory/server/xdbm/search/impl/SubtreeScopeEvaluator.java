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


import org.apache.directory.shared.ldap.filter.ScopeNode;
import org.apache.directory.server.xdbm.IndexEntry;
import org.apache.directory.server.xdbm.Store;
import org.apache.directory.server.xdbm.Index;

import javax.naming.directory.SearchControls;


/**
 * Evaluates ScopeNode assertions with subtree scope on candidates using an
 * entry database.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class SubtreeScopeEvaluator<E> implements Evaluator<ScopeNode,E>
{
    /** The ScopeNode containing initial search scope constraints */
    private final ScopeNode node;

    /** The entry identifier of the scope base */
    private final Long baseId;

    /** True if the scope requires alias dereferencing while searching */
    private final boolean dereferencing;

    /** The alias index used for subtree scope expansion */
    private final Index<Long,E> aliasIndex;

    /** The subtree scope index for parent-descendant mappings */
    private final Index<Long,E> scopeIndex;


    /**
     * Creates a subtree scope node evaluator for search expressions.
     *
     * @param node the scope node
     * @param db the database used to evaluate scope node
     * @throws Exception on db access failure
     */
    public SubtreeScopeEvaluator( Store<E> db, ScopeNode node ) throws Exception
    {
        this.node = node;

        if ( node.getScope() != SearchControls.SUBTREE_SCOPE )
        {
            throw new IllegalStateException( "ScopeNode is not of subtree scope." );
        }

        baseId = db.getEntryId( node.getBaseDn() );
        scopeIndex = db.getSubLevelIndex();
        dereferencing = node.getDerefAliases().isDerefInSearching() ||
            node.getDerefAliases().isDerefAlways();
        aliasIndex = db.getSubAliasIndex();
    }


    /**
     * Asserts whether or not a candidate has one level scope while taking
     * alias dereferencing into account.
     *
     * @param candidate the entry tested to see if it is in subtree scope
     * @return true if the candidate is within one level scope whether or not
     * alias dereferencing is enabled.
     * @throws Exception if the index lookups fail.
     * @see Evaluator#evaluate(org.apache.directory.server.xdbm.IndexEntry)
     */
    public boolean evaluate( IndexEntry<?,E> candidate ) throws Exception
    {
        boolean isDescendant = scopeIndex.has( baseId, candidate.getId() );

        /*
         * The candidate id could be any entry in the db.  If search
         * dereferencing is not enabled then we return the results of the
         * descendant test.
         */
        if ( ! isDereferencing() )
        {
            return isDescendant;
        }

        /*
         * From here down alias dereferencing is enabled.  We determine if the
         * candidate id is an alias, if so we reject it since aliases should
         * not be returned.
         */
        if ( null != aliasIndex.reverseLookup( candidate.getId() ) )
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
        return aliasIndex.has( baseId, candidate.getId() );
    }


    public ScopeNode getExpression()
    {
        return node;
    }


    public Long getBaseId()
    {
        return baseId;
    }


    public boolean isDereferencing()
    {
        return dereferencing;
    }
}
