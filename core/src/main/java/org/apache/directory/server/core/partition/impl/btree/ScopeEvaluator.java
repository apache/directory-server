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

/*
 * $Id$
 *
 * -- (c) LDAPd Group                                                    --
 * -- Please refer to the LICENSE.txt file in the root directory of      --
 * -- any LDAPd project for copyright and distribution information.      --
 *
 * Created on Oct 9, 2003
 */
package org.apache.directory.server.core.partition.impl.btree;


import java.math.BigInteger;

import javax.naming.NamingException;
import javax.naming.directory.SearchControls;

import org.apache.directory.shared.ldap.filter.ExprNode;
import org.apache.directory.shared.ldap.filter.ScopeNode;
import org.apache.directory.shared.ldap.message.DerefAliasesEnum;


/**
 * Evaluates ScopeNode assertions on candidates using a database.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class ScopeEvaluator implements Evaluator
{
    /** Database used to evaluate scope with */
    private BTreePartition db;


    /**
     * Creates a scope node evaluator for search expressions.
     *
     * @param db the database used to evaluate scope node
     */
    public ScopeEvaluator(BTreePartition db)
    {
        this.db = db;
    }


    /**
     * @see org.apache.directory.server.core.partition.impl.btree.Evaluator#evaluate(ExprNode, org.apache.directory.server.core.partition.impl.btree.IndexRecord)
     */
    public boolean evaluate( ExprNode node, IndexRecord record ) throws NamingException
    {
        ScopeNode snode = ( ScopeNode ) node;

        switch ( snode.getScope() )
        {
            case ( SearchControls.OBJECT_SCOPE  ):
                String dn = db.getEntryDn( record.getEntryId() );
                return dn.equals( snode.getBaseDn() );
            case ( SearchControls.ONELEVEL_SCOPE  ):
                return assertOneLevelScope( snode, record.getEntryId() );
            case ( SearchControls.SUBTREE_SCOPE  ):
                return assertSubtreeScope( snode, record.getEntryId() );
            default:
                throw new NamingException( "Unrecognized search scope!" );
        }
    }


    /**
     * Asserts whether or not a candidate has one level scope while taking
     * alias dereferencing into account.
     * 
     * @param node the scope node containing the base and alias handling mode
     * @param id the candidate to assert which can be any db entry's id
     * @return true if the candidate is within one level scope whether or not
     * alias dereferencing is enabled.
     * @throws NamingException if the index lookups fail.
     */
    public boolean assertSubtreeScope( final ScopeNode node, final BigInteger id ) throws NamingException
    {
        String dn = db.getEntryDn( id );
        DerefAliasesEnum mode = node.getDerefAliases();
        Object baseId = db.getEntryId( node.getBaseDn() );
        boolean isDescendant = dn.endsWith( node.getBaseDn() );

        /*
         * The candidate id could be any entry in the db.  If search 
         * dereferencing is not enabled then we return the results of the 
         * descendant test.
         */
        if ( !mode.derefInSearching() )
        {
            return isDescendant;
        }

        /*
         * From here down alias dereferencing is enabled.  We determine if the
         * candidate id is an alias, if so we reject it since aliases should
         * not be returned.
         */
        Index idx = db.getAliasIndex();
        if ( null != idx.reverseLookup( id ) )
        {
            return false;
        }

        /*
         * The candidate is NOT an alias at this point.  So if it is a 
         * descendant we just return it since it is in normal subtree scope.
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
        idx = db.getSubAliasIndex();
        return idx.hasValue( baseId, id );
    }


    /**
     * Asserts whether or not a candidate has one level scope while taking
     * alias dereferencing into account.
     * 
     * @param node the scope node containing the base and alias handling mode
     * @param id the candidate to assert which can be any db entry's id 
     * @return true if the candidate is within one level scope whether or not
     * alias dereferencing is enabled.
     * @throws NamingException if the index lookups fail.
     */
    public boolean assertOneLevelScope( final ScopeNode node, final BigInteger id ) throws NamingException
    {
        DerefAliasesEnum mode = node.getDerefAliases();
        Object baseId = db.getEntryId( node.getBaseDn() );
        Index idx = db.getHierarchyIndex();
        boolean isChild = idx.hasValue( baseId, id );

        /*
         * The candidate id could be any entry in the db.  If search 
         * dereferencing is not enabled then we return the results of the child 
         * test. 
         */
        if ( !mode.derefInSearching() )
        {
            return isChild;
        }

        /*
         * From here down alias dereferencing is enabled.  We determine if the
         * candidate id is an alias, if so we reject it since aliases should
         * not be returned.
         */
        idx = db.getAliasIndex();
        if ( null != idx.reverseLookup( id ) )
        {
            return false;
        }

        /*
         * The candidate is NOT an alias at this point.  So if it is a child we
         * just return it since it is in normal one level scope.
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
        idx = db.getOneAliasIndex();
        return idx.hasValue( baseId, id );
    }
}
