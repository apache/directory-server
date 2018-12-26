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
package org.apache.directory.server.xdbm.search.evaluator;


import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.directory.api.ldap.model.filter.ScopeNode;
import org.apache.directory.api.ldap.model.message.SearchScope;
import org.apache.directory.api.ldap.model.name.Dn;
import org.apache.directory.server.core.api.partition.Partition;
import org.apache.directory.server.core.api.partition.PartitionTxn;
import org.apache.directory.server.i18n.I18n;
import org.apache.directory.server.xdbm.IndexEntry;
import org.apache.directory.server.xdbm.ParentIdAndRdn;
import org.apache.directory.server.xdbm.Store;
import org.apache.directory.server.xdbm.search.Evaluator;


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
    private final String baseId;

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
     * @param partitionTxn The transaction to use
     * @param db the database used to evaluate scope node
     * @param node the scope node
     * @throws LdapException on db access failure
     */
    public SubtreeScopeEvaluator( PartitionTxn partitionTxn, Store db, ScopeNode node ) throws LdapException
    {
        this.db = db;
        this.node = node;

        if ( node.getScope() != SearchScope.SUBTREE )
        {
            throw new IllegalStateException( I18n.err( I18n.ERR_727 ) );
        }

        baseId = node.getBaseId();
        
        baseIsContextEntry = db.getSuffixId( partitionTxn ) == baseId;

        dereferencing = node.getDerefAliases().isDerefInSearching() || node.getDerefAliases().isDerefAlways();
    }


    /**
     * Tells if a candidate is a descendant of the base ID. We have to fetch all 
     * the parentIdAndRdn up to the baseId. If we terminate on the context entry without 
     * having found the baseId, then the candidate is not a descendant.
     */
    private boolean isDescendant( PartitionTxn partitionTxn, String candidateId ) throws LdapException
    {
        String tmp = candidateId;

        while ( true )
        {
            ParentIdAndRdn parentIdAndRdn = db.getRdnIndex().reverseLookup( partitionTxn, tmp );

            if ( parentIdAndRdn == null )
            {
                return false;
            }

            tmp = parentIdAndRdn.getParentId();

            if ( tmp.equals( Partition.ROOT_ID ) )
            {
                return false;
            }

            if ( tmp.equals( baseId ) )
            {
                return true;
            }
        }
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public boolean evaluate( PartitionTxn partitionTxn, IndexEntry<?, String> indexEntry ) throws LdapException
    {
        String id = indexEntry.getId();
        Entry entry = indexEntry.getEntry();

        // Fetch the entry
        if ( null == entry )
        {
            entry = db.fetch( partitionTxn, indexEntry.getId() );

            if ( null == entry )
            {
                // The entry is not anymore present : get out
                return false;
            }

            indexEntry.setEntry( entry );
        }

        /*
         * This condition catches situations where the candidate is equal to 
         * the base entry and when the base entry is the context entry.  Note
         * we do not store a mapping in the subtree index of the context entry
         * to all it's subordinates since that would be the entire set of 
         * entries in the db.
         */
        boolean isDescendant = baseIsContextEntry || baseId.equals( id ) || entry.getDn().isDescendantOf( node.getBaseDn() );

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
        if ( db.getAliasCache() != null )
        {
            Dn dn = db.getAliasCache().get( id );
            
            if ( dn != null )
            {
                return false;
            }
        }
        else if ( null != db.getAliasIndex().reverseLookup( partitionTxn, id ) )
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
        return db.getSubAliasIndex().forward( partitionTxn, baseId, id );
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public boolean evaluate( Entry candidate ) throws LdapException
    {
        throw new UnsupportedOperationException( I18n.err( I18n.ERR_721 ) );
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public ScopeNode getExpression()
    {
        return node;
    }


    /**
     * @return The base ID
     */
    public String getBaseId()
    {
        return baseId;
    }


    /**
     * @return <tt>true</tt> if dereferencing
     */
    public boolean isDereferencing()
    {
        return dereferencing;
    }


    /**
     * @see Object#toString()
     */
    public String toString( String tabs )
    {
        StringBuilder sb = new StringBuilder();

        sb.append( tabs ).append( "SubstreeScopeEvaluator : " ).append( node ).append( '\n' );

        return sb.toString();
    }


    /**
     * @see Object#toString()
     */
    public String toString()
    {
        return toString( "" );
    }
}
