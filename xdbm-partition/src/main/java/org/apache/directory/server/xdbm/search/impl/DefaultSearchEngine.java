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


import java.util.HashSet;
import java.util.Set;

import org.apache.directory.api.ldap.model.cursor.Cursor;
import org.apache.directory.api.ldap.model.cursor.CursorException;
import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.directory.api.ldap.model.exception.LdapNoSuchObjectException;
import org.apache.directory.api.ldap.model.exception.LdapOtherException;
import org.apache.directory.api.ldap.model.filter.AndNode;
import org.apache.directory.api.ldap.model.filter.ExprNode;
import org.apache.directory.api.ldap.model.filter.ObjectClassNode;
import org.apache.directory.api.ldap.model.filter.ScopeNode;
import org.apache.directory.api.ldap.model.message.AliasDerefMode;
import org.apache.directory.api.ldap.model.message.SearchScope;
import org.apache.directory.api.ldap.model.name.Dn;
import org.apache.directory.api.ldap.model.schema.SchemaManager;
import org.apache.directory.server.core.api.interceptor.context.SearchOperationContext;
import org.apache.directory.server.core.api.partition.Partition;
import org.apache.directory.server.core.api.partition.PartitionTxn;
import org.apache.directory.server.core.partition.impl.btree.IndexCursorAdaptor;
import org.apache.directory.server.i18n.I18n;
import org.apache.directory.server.xdbm.IndexEntry;
import org.apache.directory.server.xdbm.Store;
import org.apache.directory.server.xdbm.search.Evaluator;
import org.apache.directory.server.xdbm.search.Optimizer;
import org.apache.directory.server.xdbm.search.PartitionSearchResult;
import org.apache.directory.server.xdbm.search.SearchEngine;
import org.apache.directory.server.xdbm.search.evaluator.BaseLevelScopeEvaluator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Given a search filter and a scope the search engine identifies valid
 * candidate entries returning their ids.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class DefaultSearchEngine implements SearchEngine
{
    /** The logger */
    private static final Logger LOG = LoggerFactory.getLogger( DefaultSearchEngine.class );

    /** the Optimizer used by this DefaultSearchEngine */
    private final Optimizer optimizer;

    /** the Database this DefaultSearchEngine operates on */
    private final Store db;

    /** creates Cursors over entries satisfying filter expressions */
    private final CursorBuilder cursorBuilder;

    /** creates evaluators which check to see if candidates satisfy a filter expression */
    private final EvaluatorBuilder evaluatorBuilder;


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
    public DefaultSearchEngine( Store db, CursorBuilder cursorBuilder,
        EvaluatorBuilder evaluatorBuilder, Optimizer optimizer )
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
    @Override
    public Optimizer getOptimizer()
    {
        return optimizer;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public PartitionSearchResult computeResult( PartitionTxn partitionTxn, SchemaManager schemaManager, 
        SearchOperationContext searchContext ) throws LdapException
    {
        SearchScope scope = searchContext.getScope();
        Dn baseDn = searchContext.getDn();
        AliasDerefMode aliasDerefMode = searchContext.getAliasDerefMode();
        ExprNode filter = searchContext.getFilter();

        // Compute the UUID of the baseDN entry
        String baseId = db.getEntryId( partitionTxn, baseDn );

        // Prepare the instance containing the search result
        PartitionSearchResult searchResult = new PartitionSearchResult( schemaManager );
        Set<IndexEntry<String, String>> resultSet = new HashSet<>();

        // Check that we have an entry, otherwise we can immediately get out
        if ( baseId == null )
        {
            if ( ( ( Partition ) db ).getSuffixDn().equals( baseDn ) )
            {
                // The context entry is not created yet, return an empty result
                searchResult.setResultSet( resultSet );

                return searchResult;
            }
            else
            {
                // The search base doesn't exist
                throw new LdapNoSuchObjectException( I18n.err( I18n.ERR_648, baseDn ) );
            }
        }

        // --------------------------------------------------------------------
        // Determine the effective base with aliases
        // --------------------------------------------------------------------
        Dn aliasedBase = null;


        if ( db.getAliasCache() != null )
        {
            aliasedBase = db.getAliasCache().get( baseId );
        }
        else
        {
            aliasedBase = db.getAliasIndex().reverseLookup( partitionTxn, baseId );
        }

        Dn effectiveBase = baseDn;
        String effectiveBaseId = baseId;

        if ( ( aliasedBase != null ) && aliasDerefMode.isDerefFindingBase() )
        {
            /*
             * If the base is an alias and alias dereferencing does occur on
             * finding the base, or always then we set the effective base to the alias target
             * got from the alias index.
             */
            if ( !aliasedBase.isSchemaAware() )
            {
                effectiveBase = new Dn( schemaManager, aliasedBase );
            }
            else
            {
                effectiveBase = aliasedBase;
            }
            
            effectiveBaseId = db.getEntryId( partitionTxn, effectiveBase );
        }

        // --------------------------------------------------------------------
        // Specifically Handle Object Level Scope
        // --------------------------------------------------------------------
        if ( scope == SearchScope.OBJECT )
        {
            IndexEntry<String, String> indexEntry = new IndexEntry<>();
            indexEntry.setId( effectiveBaseId );

            // Fetch the entry, as we have only one
            Entry entry = db.fetch( partitionTxn, indexEntry.getId(), effectiveBase );

            Evaluator<? extends ExprNode> evaluator;

            if ( filter instanceof ObjectClassNode )
            {
                ScopeNode node = new ScopeNode( aliasDerefMode, effectiveBase, effectiveBaseId, scope );
                evaluator = new BaseLevelScopeEvaluator<>( db, node );
            }
            else
            {
                optimizer.annotate( partitionTxn, filter );
                evaluator = evaluatorBuilder.build( partitionTxn, filter );

                // Special case if the filter selects no candidate
                if ( evaluator == null )
                {
                    ScopeNode node = new ScopeNode( aliasDerefMode, effectiveBase, effectiveBaseId, scope );
                    evaluator = new BaseLevelScopeEvaluator<>( db, node );
                }
            }

            indexEntry.setEntry( entry );
            resultSet.add( indexEntry );

            searchResult.setEvaluator( evaluator );
            searchResult.setResultSet( resultSet );

            return searchResult;
        }

        // This is not a BaseObject scope search.

        // Add the scope node using the effective base to the filter
        ExprNode root;

        if ( filter instanceof ObjectClassNode )
        {
            root = new ScopeNode( aliasDerefMode, effectiveBase, effectiveBaseId, scope );
        }
        else
        {
            root = new AndNode();
            ( ( AndNode ) root ).getChildren().add( filter );
            ExprNode node = new ScopeNode( aliasDerefMode, effectiveBase, effectiveBaseId, scope );
            ( ( AndNode ) root ).getChildren().add( node );
        }

        // Annotate the node with the optimizer and return search enumeration.
        optimizer.annotate( partitionTxn, root );
        Evaluator<? extends ExprNode> evaluator = evaluatorBuilder.build( partitionTxn, root );

        Set<String> uuidSet = new HashSet<>();
        searchResult.setAliasDerefMode( aliasDerefMode );
        searchResult.setCandidateSet( uuidSet );

        long nbResults = cursorBuilder.build( partitionTxn, root, searchResult );

        LOG.debug( "Nb results : {} for filter : {}", nbResults, root );

        if ( nbResults < Long.MAX_VALUE )
        {
            for ( String uuid : uuidSet )
            {
                IndexEntry<String, String> indexEntry = new IndexEntry<>();
                indexEntry.setId( uuid );
                resultSet.add( indexEntry );
            }
        }
        else
        {
            // Full scan : use the MasterTable
            Cursor<IndexEntry<String, String>> cursor = new IndexCursorAdaptor( partitionTxn, db.getMasterTable().cursor(), true );

            try
            {
                while ( cursor.next() )
                {
                    IndexEntry<String, String> indexEntry = cursor.get();
    
                    // Here, the indexEntry contains a <UUID, Entry> tuple. Convert it to <UUID, UUID>
                    IndexEntry<String, String> forwardIndexEntry = new IndexEntry<>();
                    forwardIndexEntry.setKey( indexEntry.getKey() );
                    forwardIndexEntry.setId( indexEntry.getKey() );
                    forwardIndexEntry.setEntry( null );
    
                    resultSet.add( forwardIndexEntry );
                }
            }
            catch ( CursorException ce )
            {
                throw new LdapOtherException( ce.getMessage(), ce );
            }
        }

        searchResult.setEvaluator( evaluator );
        searchResult.setResultSet( resultSet );

        return searchResult;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public Evaluator<? extends ExprNode> evaluator( PartitionTxn partitionTxn, ExprNode filter ) throws LdapException
    {
        return evaluatorBuilder.build( partitionTxn, filter );
    }
}
