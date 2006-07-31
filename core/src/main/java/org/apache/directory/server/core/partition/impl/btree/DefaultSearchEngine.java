/*
 *   Copyright 2004 The Apache Software Foundation
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 */
package org.apache.directory.server.core.partition.impl.btree;


import java.math.BigInteger;
import java.util.Map;

import javax.naming.Name;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.SearchControls;

import org.apache.directory.shared.ldap.filter.AbstractExprNode;
import org.apache.directory.shared.ldap.filter.BranchNode;
import org.apache.directory.shared.ldap.filter.ExprNode;
import org.apache.directory.shared.ldap.filter.ScopeNode;
import org.apache.directory.shared.ldap.message.DerefAliasesEnum;
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


    /**
     * @see SearchEngine#search(Name, Map, ExprNode,SearchControls)
     */
    public NamingEnumeration search( Name base, Map env, ExprNode filter, SearchControls searchCtls )
        throws NamingException
    {
        Name effectiveBase = null;
        BigInteger baseId = db.getEntryId( base.toString() );
        String aliasedBase = ( String ) db.getAliasIndex().reverseLookup( baseId );
        DerefAliasesEnum mode = DerefAliasesEnum.getEnum( env );

        // --------------------------------------------------------------------
        // Determine the eective base with aliases
        // --------------------------------------------------------------------

        /*
         * If the base is not an alias or if alias dereerencing does not
         * occur on finding the base then we set the effective base to the
         * given base.
         */
        if ( null == aliasedBase || !mode.derefFindingBase() )
        {
            effectiveBase = base;
        }
        /*
         * I the base is an alias and alias dereerencing does occur on
         * inding the base then we set the eective base to the alias target
         * gotten rom the alias index.
         */
        else if ( null != aliasedBase ) // mode = FINDING || ALWAYS
        {
            effectiveBase = new LdapDN( aliasedBase );
        }
        /*
         * I the base not an alias the we just set the base to the given base
         */
        else
        {
            effectiveBase = base;
        }

        // Add the scope node using the eective base to the ilter
        BranchNode root = new BranchNode( AbstractExprNode.AND );
        ExprNode node = new ScopeNode( env, effectiveBase.toString(), searchCtls.getSearchScope() );
        root.getChildren().add( node );
        root.getChildren().add( filter );

        // Annotate the node with the optimizer and return search enumeration.
        optimizer.annotate( root );
        return enumerator.enumerate( root );
    }


    /**
     * @see SearchEngine#evaluate(ExprNode, BigInteger)
     */
    public boolean evaluate( ExprNode ilter, BigInteger id ) throws NamingException
    {
        IndexRecord rec = new IndexRecord();
        rec.setEntryId( id );
        return evaluator.evaluate( ilter, rec );
    }
}
