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

import org.apache.directory.server.core.api.partition.Partition;
import org.apache.directory.server.i18n.I18n;
import org.apache.directory.server.xdbm.EmptyIndexCursor;
import org.apache.directory.server.xdbm.ForwardIndexEntry;
import org.apache.directory.server.xdbm.IndexCursor;
import org.apache.directory.server.xdbm.IndexEntry;
import org.apache.directory.server.xdbm.SingletonIndexCursor;
import org.apache.directory.server.xdbm.Store;
import org.apache.directory.server.xdbm.search.Evaluator;
import org.apache.directory.server.xdbm.search.Optimizer;
import org.apache.directory.server.xdbm.search.SearchEngine;
import org.apache.directory.shared.ldap.model.entry.Entry;
import org.apache.directory.shared.ldap.model.exception.LdapNoSuchObjectException;
import org.apache.directory.shared.ldap.model.filter.AndNode;
import org.apache.directory.shared.ldap.model.filter.BranchNode;
import org.apache.directory.shared.ldap.model.filter.ExprNode;
import org.apache.directory.shared.ldap.model.filter.ScopeNode;
import org.apache.directory.shared.ldap.model.message.AliasDerefMode;
import org.apache.directory.shared.ldap.model.message.SearchScope;
import org.apache.directory.shared.ldap.model.name.Dn;


/**
 * Given a search filter and a scope the search engine identifies valid
 * candidate entries returning their ids.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class DefaultSearchEngine<ID extends Comparable<ID>> implements SearchEngine<Entry, ID>
{
    /** the Optimizer used by this DefaultSearchEngine */
    private final Optimizer optimizer;
    /** the Database this DefaultSearchEngine operates on */
    private final Store<Entry, ID> db;
    /** creates Cursors over entries satisfying filter expressions */
    private final CursorBuilder<ID> cursorBuilder;
    /** creates evaluators which check to see if candidates satisfy a filter expression */
    private final EvaluatorBuilder<ID> evaluatorBuilder;


    // ------------------------------------------------------------------------
    // C O N S T R U C T O R S
    // ------------------------------------------------------------------------

    /**
     * Creates a DefaultSearchEngine for searching a Database without setting
     * up the database.
     * @param db the btree based partition
     * @param cursorBuilder an expression cursor builder
     * @param evaluatorBuilder an expression evaluator builder
     * @param optimizer an optimizer to use during search
     */
    public DefaultSearchEngine( Store<Entry, ID> db, CursorBuilder<ID> cursorBuilder,
        EvaluatorBuilder<ID> evaluatorBuilder, Optimizer optimizer )
    {
        this.db = db;
        this.optimizer = optimizer;
        this.cursorBuilder = cursorBuilder;
        this.evaluatorBuilder = evaluatorBuilder;
    }


    /**
     * Gets the optimizer for this DefaultSearchEngine.
     *
     * @return the optimizer
     */
    public Optimizer getOptimizer()
    {
        return optimizer;
    }


    /**
     * {@inheritDoc}
     */
    public IndexCursor<ID, Entry, ID> cursor( Dn base, AliasDerefMode aliasDerefMode, ExprNode filter,
        SearchControls searchCtls ) throws Exception
    {
        Dn effectiveBase;
        ID baseId = db.getEntryId( base );

        // Check that we have an entry, otherwise we can immediately get out
        if ( baseId == null )
        {
            if ( ( ( Partition ) db ).getSuffixDn().equals( base ) )
            {
                // The context entry is not created yet, return an empty cursor
                return new EmptyIndexCursor<ID, Entry, ID>();
            }
            else
            {
                // The search base doesn't exist
                throw new LdapNoSuchObjectException( I18n.err( I18n.ERR_648, base ) );
            }
        }

        String aliasedBase = db.getAliasIndex().reverseLookup( baseId );

        // --------------------------------------------------------------------
        // Determine the effective base with aliases
        // --------------------------------------------------------------------

        /*
         * If the base is not an alias or if alias dereferencing does not
         * occur on finding the base then we set the effective base to the
         * given base.
         */
        if ( ( null == aliasedBase ) || !aliasDerefMode.isDerefFindingBase() )
        {
            effectiveBase = base;
        }
        /*
         * If the base is an alias and alias dereferencing does occur on
         * finding the base then we set the effective base to the alias target
         * got from the alias index.
         */
        else
        {
            effectiveBase = new Dn( aliasedBase );
        }

        // --------------------------------------------------------------------
        // Specifically Handle Object Level Scope
        // --------------------------------------------------------------------

        if ( searchCtls.getSearchScope() == SearchControls.OBJECT_SCOPE )
        {
            ID effectiveBaseId = baseId;
            
            if ( effectiveBase != base )
            {
                effectiveBaseId = db.getEntryId( effectiveBase );
            }

            IndexEntry<ID, ID> indexEntry = new ForwardIndexEntry<ID, ID>();
            indexEntry.setId( effectiveBaseId );
            optimizer.annotate( filter );
            Evaluator<? extends ExprNode, Entry, ID> evaluator = evaluatorBuilder.build( filter );

            if ( evaluator.evaluate( indexEntry ) )
            {
                return new SingletonIndexCursor<ID, ID>( indexEntry );
            }
            else
            {
                return new EmptyIndexCursor<ID, Entry, ID>();
            }
        }

        // Add the scope node using the effective base to the filter
        BranchNode root = new AndNode();
        ExprNode node = new ScopeNode( aliasDerefMode, effectiveBase, SearchScope.getSearchScope( searchCtls
            .getSearchScope() ) );
        root.getChildren().add( node );
        root.getChildren().add( filter );

        // Annotate the node with the optimizer and return search enumeration.
        optimizer.annotate( root );
        return ( IndexCursor<ID, Entry, ID> ) cursorBuilder.build( root );
    }


    /**
     * @see SearchEngine#evaluator(ExprNode)
     */
    public Evaluator<? extends ExprNode, Entry, ID> evaluator( ExprNode filter ) throws Exception
    {
        return evaluatorBuilder.build( filter );
    }
}
