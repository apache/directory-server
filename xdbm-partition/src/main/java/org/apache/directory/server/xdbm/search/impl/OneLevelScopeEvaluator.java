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


import org.apache.directory.shared.ldap.model.filter.ScopeNode;
import org.apache.directory.shared.ldap.model.message.SearchScope;
import org.apache.directory.server.i18n.I18n;
import org.apache.directory.server.xdbm.IndexEntry;
import org.apache.directory.server.xdbm.Store;
import org.apache.directory.server.xdbm.search.Evaluator;


/**
 * Evaluates one level scope assertions on candidates using an entry database.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class OneLevelScopeEvaluator<E, ID extends Comparable<ID>> implements Evaluator<ScopeNode, E, ID>
{
    /** The ScopeNode containing initial search scope constraints */
    private final ScopeNode node;

    /** The entry identifier of the scope base */
    private final ID baseId;

    /** True if the scope requires alias dereferencing while searching */
    private final boolean dereferencing;

    /** the entry db storing entries */
    private final Store<E, ID> db;


    /**
     * Creates a one level scope node Evaluator for search expressions.
     *
     * @param node the scope node
     * @param db the database used to evaluate scope node
     * @throws Exception on db access failure
     */
    public OneLevelScopeEvaluator( Store<E, ID> db, ScopeNode node ) throws Exception
    {
        this.node = node;

        if ( node.getScope() != SearchScope.ONELEVEL )
        {
            throw new IllegalStateException( I18n.err( I18n.ERR_720 ) );
        }

        this.db = db;
        baseId = db.getEntryId( node.getBaseDn() );
        dereferencing = node.getDerefAliases().isDerefInSearching() || node.getDerefAliases().isDerefAlways();
    }


    /**
     * Asserts whether or not a candidate has one level scope while taking
     * alias dereferencing into account.
     *
     * @param candidate the candidate to assert
     * @return true if the candidate is within one level scope
     * @throws Exception if db lookups fail
     * @see org.apache.directory.server.xdbm.search.Evaluator#evaluate(IndexEntry)
     */
    public boolean evaluateId( ID candidate ) throws Exception
    {
        boolean isChild = db.getRdnIndexHelper().isDirectDescendantOf( baseId, candidate );

        /*
         * The candidate id could be any entry in the db.  If search
         * dereferencing is not enabled then we return the results of the child
         * test.
         */
        if ( !dereferencing )
        {
            return isChild;
        }

        /*
         * From here down alias dereferencing is enabled.  We determine if the
         * candidate id is an alias, if so we reject it since aliases should
         * not be returned.
         */
        if ( null != db.getAliasIndex().reverseLookup( candidate ) )
        {
            return false;
        }

        /*
         * The candidate is NOT an alias at this point.  So if it is a child we
         * just return true since it is in normal one level scope.
         */
        if ( isChild )
        {
            return true;
        }

        /*
         * At this point the candidate is not a child and it is not an alias.
         * We need to check if the candidate is in extended one level scope by
         * performing a lookup on the one level alias index.  This index stores
         * a tuple mapping the baseId to the id of objects brought into the
         * one level scope of the base by an alias: ( baseId, aliasedObjId )
         * If the candidate id is an object brought into one level scope then
         * the lookup returns true accepting the candidate.  Otherwise the
         * candidate is rejected with a false return because it is not in scope.
         */
        return db.getOneAliasIndex().forward( baseId, candidate );
    }


    /**
     * Asserts whether or not a candidate has one level scope while taking
     * alias dereferencing into account.
     *
     * TODO - terribly inefficient - would benefit from exposing the id of an
     * entry within the Entry
     *
     * {@inheritDoc}
     */
    public boolean evaluateEntry( E candidate ) throws Exception
    {
        throw new UnsupportedOperationException( I18n.err( I18n.ERR_721 ) );
    }


    /**
     * Asserts whether or not a candidate has one level scope while taking
     * alias dereferencing into account.
     *
     * @param candidate the candidate to assert
     * @return true if the candidate is within one level scope
     * @throws Exception if db lookups fail
     * @see org.apache.directory.server.xdbm.search.Evaluator#evaluate(IndexEntry)
     */
    public boolean evaluate( IndexEntry<?, E, ID> candidate ) throws Exception
    {
        boolean isChild = db.getRdnIndexHelper().isDirectDescendantOf( baseId, candidate.getId() );

        /*
         * The candidate id could be any entry in the db.  If search
         * dereferencing is not enabled then we return the results of the child
         * test.
         */
        if ( !dereferencing )
        {
            return isChild;
        }

        /*
         * From here down alias dereferencing is enabled.  We determine if the
         * candidate id is an alias, if so we reject it since aliases should
         * not be returned.
         */
        if ( null != db.getAliasIndex().reverseLookup( candidate.getId() ) )
        {
            return false;
        }

        /*
         * The candidate is NOT an alias at this point.  So if it is a child we
         * just return true since it is in normal one level scope.
         */
        if ( isChild )
        {
            return true;
        }

        /*
         * At this point the candidate is not a child and it is not an alias.
         * We need to check if the candidate is in extended one level scope by
         * performing a lookup on the one level alias index.  This index stores
         * a tuple mapping the baseId to the id of objects brought into the
         * one level scope of the base by an alias: ( baseId, aliasedObjId )
         * If the candidate id is an object brought into one level scope then
         * the lookup returns true accepting the candidate.  Otherwise the
         * candidate is rejected with a false return because it is not in scope.
         */
        return db.getOneAliasIndex().forward( baseId, candidate.getId() );
    }


    public ScopeNode getExpression()
    {
        return node;
    }


    /**
     * Gets the id of the search base associated with the ScopeNode expression.
     *
     * @return identifier of the search base
     */
    public ID getBaseId()
    {
        return baseId;
    }


    /**
     * Gets whether or not dereferencing is enabled for this evaluator.
     *
     * @return true if dereferencing is enabled, false otherwise
     */
    public boolean isDereferencing()
    {
        return dereferencing;
    }
}