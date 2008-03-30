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


import javax.naming.directory.SearchControls;

import org.apache.directory.shared.ldap.filter.ScopeNode;
import org.apache.directory.server.xdbm.IndexEntry;
import org.apache.directory.server.xdbm.Store;


/**
 * Evaluates ScopeNode assertions on candidates using an entry database.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class ScopeEvaluator<E> implements Evaluator<ScopeNode,E>
{
    /** Database used to evaluate scope with */
    private final Store<E> db;

    /** The ScopeNode containing initial search scope constraints */
    private final ScopeNode node;

    /** The entry identifier of the scope base */
    private final Long baseId;


    /**
     * Creates a scope node evaluator for search expressions.
     *
     * @param node the scope node
     * @param db the database used to evaluate scope node
     * @throws Exception on db access failure
     */
    public ScopeEvaluator( Store<E> db, ScopeNode node ) throws Exception
    {
        this.db = db;
        this.node = node;

        baseId = db.getEntryId( node.getBaseDn() );
    }


    /**
     * @see Evaluator#evaluate(org.apache.directory.server.xdbm.IndexEntry)
     */
    public boolean evaluate( IndexEntry<?,E> entry ) throws Exception
    {
        switch ( node.getScope() )
        {
            case ( SearchControls.OBJECT_SCOPE  ):
                return entry.getId().longValue() == baseId.longValue();
            case ( SearchControls.ONELEVEL_SCOPE  ):
                return isInOneLevelScope( entry.getId() );
            case ( SearchControls.SUBTREE_SCOPE  ):
                return isInSubtreeScope( entry.getId() );
            default:
                throw new IllegalStateException( "Unrecognized search scope!" );
        }
    }


    public ScopeNode getExpression()
    {
        return node;
    }


    /**
     * Asserts whether or not a candidate has one level scope while taking
     * alias dereferencing into account.
     * 
     * @param id the candidate to assert which can be any db entry's id
     * @return true if the candidate is within one level scope whether or not
     * alias dereferencing is enabled.
     * @throws Exception if the index lookups fail.
     */
    public boolean isInSubtreeScope( final Long id ) throws Exception
    {
        boolean isDescendant = db.getSubLevelIndex().has( baseId, id );

        /*
         * The candidate id could be any entry in the db.  If search 
         * dereferencing is not enabled then we return the results of the 
         * descendant test.
         */
        if ( !node.getDerefAliases().isDerefInSearching() )
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
        return db.getSubAliasIndex().has( baseId, id );
    }


    /**
     * Asserts whether or not a candidate has one level scope while taking
     * alias dereferencing into account.
     * 
     * @param id the candidate to assert which can be any db entry's id 
     * @return true if the candidate is within one level scope whether or not
     * alias dereferencing is enabled.
     * @throws Exception if the index lookups fail.
     */
    public boolean isInOneLevelScope( final Long id ) throws Exception
    {
        boolean isChild = db.getOneLevelIndex().has( baseId, id );

        /*
         * The candidate id could be any entry in the db.  If search 
         * dereferencing is not enabled then we return the results of the child 
         * test. 
         */
        if ( !node.getDerefAliases().isDerefInSearching() )
        {
            return isChild;
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
        return db.getOneAliasIndex().has( baseId, id );
    }
}
