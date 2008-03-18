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
package org.apache.directory.server.core.partition.impl.btree;


import javax.naming.Name;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.SearchControls;

import org.apache.directory.shared.ldap.filter.AndNode;
import org.apache.directory.shared.ldap.filter.BranchNode;
import org.apache.directory.shared.ldap.filter.ExprNode;
import org.apache.directory.shared.ldap.filter.ScopeNode;
import org.apache.directory.shared.ldap.message.AliasDerefMode;
import org.apache.directory.shared.ldap.name.LdapDN;


/**
 * Given a search filter and a scope the search engine identifies valid
 * candidate entries returning their ids.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class DefaultSearchEngine implements SearchEngine
{
    /** the Optimizer used by this DefaultSearchEngine */
    private final Optimizer optimizer;
    /** the Database this DefaultSearchEngine operates on */
    private BTreePartition db;
    /** Evaluator flyweight used for filter expression assertions */
    private ExpressionEvaluator evaluator;
    /** Enumerator flyweight that creates enumerations on filter expressions */
    private ExpressionEnumerator enumerator;


    // ------------------------------------------------------------------------
    // C O N S T R U C T O R S
    // ------------------------------------------------------------------------

    /**
     * Creates a DefaultSearchEngine for searching a Database without setting
     * up the database.
     * @param db the btree based partition
     * @param enumerator an expression enumerator
     * @param evaluator an expression evaluator
     * @param optimizer an optimizer to use during search
     */
    public DefaultSearchEngine( BTreePartition db, ExpressionEvaluator evaluator,
        ExpressionEnumerator enumerator, Optimizer optimizer )
    {
        this.db = db;
        this.evaluator = evaluator;
        this.enumerator = enumerator;
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


    public NamingEnumeration<IndexRecord> search( Name base, AliasDerefMode aliasDerefMode, ExprNode filter, SearchControls searchCtls )
        throws Exception
    {
        Name effectiveBase;
        Long baseId = db.getEntryId( base.toString() );
        String aliasedBase = ( String ) db.getAliasIndex().reverseLookup( baseId );

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
        return enumerator.enumerate( root );
    }


    /**
     * @see SearchEngine#evaluate(ExprNode, Long)
     */
    public boolean evaluate( ExprNode ilter, Long id ) throws NamingException
    {
        IndexEntry rec = new ForwardIndexEntry();
        rec.setId( id );
        return evaluator.evaluate( ilter, rec );
    }
}
