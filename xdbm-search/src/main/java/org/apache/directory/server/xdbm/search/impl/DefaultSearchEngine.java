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

import org.apache.directory.shared.ldap.filter.AndNode;
import org.apache.directory.shared.ldap.filter.BranchNode;
import org.apache.directory.shared.ldap.filter.ExprNode;
import org.apache.directory.shared.ldap.filter.ScopeNode;
import org.apache.directory.shared.ldap.message.AliasDerefMode;
import org.apache.directory.shared.ldap.name.LdapDN;
import org.apache.directory.server.xdbm.*;
import org.apache.directory.server.xdbm.search.Optimizer;
import org.apache.directory.server.xdbm.search.SearchEngine;
import org.apache.directory.server.core.cursor.Cursor;
import org.apache.directory.server.core.entry.ServerEntry;


/**
 * Given a search filter and a scope the search engine identifies valid
 * candidate entries returning their ids.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class DefaultSearchEngine implements SearchEngine<ServerEntry>
{
    /** the Optimizer used by this DefaultSearchEngine */
    private final Optimizer optimizer;
    /** the Database this DefaultSearchEngine operates on */
    private Store<ServerEntry> db;
    /** Evaluator flyweight used for filter expression assertions */
    private Evaluator<? extends ExprNode, ServerEntry> evaluator;
    /** CursorBuilder flyweight that creates enumerations on filter expressions */
    private CursorBuilder cursorBuilder;


    // ------------------------------------------------------------------------
    // C O N S T R U C T O R S
    // ------------------------------------------------------------------------

    /**
     * Creates a DefaultSearchEngine for searching a Database without setting
     * up the database.
     * @param db the btree based partition
     * @param cursorBuilder an expression cursorBuilder
     * @param evaluator an expression evaluator
     * @param optimizer an optimizer to use during search
     */
    public DefaultSearchEngine( Store<ServerEntry> db,
                                Evaluator<? extends ExprNode, ServerEntry> evaluator,
                                CursorBuilder cursorBuilder, Optimizer optimizer )
    {
        this.db = db;
        this.evaluator = evaluator;
        this.cursorBuilder = cursorBuilder;
        this.optimizer = optimizer;
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


    public Cursor<IndexEntry<?,ServerEntry>> search( LdapDN base, AliasDerefMode aliasDerefMode, ExprNode filter, SearchControls searchCtls )
        throws Exception
    {
        LdapDN effectiveBase;
        Long baseId = db.getEntryId( base.toString() );
        String aliasedBase = db.getAliasIndex().reverseLookup( baseId );

        // --------------------------------------------------------------------
        // Determine the effective base with aliases
        // --------------------------------------------------------------------

        /*
         * If the base is not an alias or if alias dereferencing does not
         * occur on finding the base then we set the effective base to the
         * given base.
         */
        if ( ( null == aliasedBase ) || ! aliasDerefMode.isDerefFindingBase() )
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
            effectiveBase = new LdapDN( aliasedBase );
        }
        
        // Add the scope node using the effective base to the filter
        BranchNode root = new AndNode();
        ExprNode node = new ScopeNode( aliasDerefMode, effectiveBase.toString(), searchCtls.getSearchScope() );
        root.getChildren().add( node );
        root.getChildren().add( filter );

        // Annotate the node with the optimizer and return search enumeration.
        optimizer.annotate( root );
        return cursorBuilder.build( root );
    }


    /**
     * @see SearchEngine#evaluate(ExprNode, Long)
     */
    public boolean evaluate( ExprNode filter, Long id ) throws Exception
    {
        IndexEntry<?,ServerEntry> rec = new ForwardIndexEntry<Object,ServerEntry>();
        rec.setId( id );
        return evaluator.evaluate( rec );
    }
}
