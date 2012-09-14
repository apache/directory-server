/*
 *   Licensed to the Apache Software Foundation (ASF) under one
 *   or more contributor license agreements.  See the NOTICE file
 *   distributed with this work for additional information
 *   regarding copyright ownership.  The ASF licenses this file
 *   to you under the Apache License, Version 2.0 (the
 *   "License"); you may not use this file except in compliance
 *   with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing,
 *   software distributed under the License is distributed on an
 *   "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *   KIND, either express or implied.  See the License for the
 *   specific language governing permissions and limitations
 *   under the License.
 *
 */
package org.apache.directory.server.xdbm.search;


import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.apache.directory.server.xdbm.IndexEntry;
import org.apache.directory.shared.ldap.model.cursor.SetCursor;
import org.apache.directory.shared.ldap.model.filter.ExprNode;
import org.apache.directory.shared.ldap.model.message.AliasDerefMode;
import org.apache.directory.shared.ldap.model.schema.SchemaManager;


/**
 * A class containing the result of a search :
 * <ul>
 * <li>A set of candidate UUIDs</li>
 * <li>A set of aliased entry if we have any</li>
 * <li>A flag telling if we are dereferencing aliases or not</li>
 * <li>A hierarchy of evaluators to use to validate the candidates</li>
 * </ul>
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class PartitionSearchResult
{
    /** The set of candidate UUIDs selected by the search */
    private SetCursor<IndexEntry<String, String>> resultSet;

    /** The set of candidate UUIDs */
    private Set<String> candidateSet;

    /** The flag indicating if we are dereferencing the aliases. Default to Never. */
    private AliasDerefMode aliasDerefMode = AliasDerefMode.NEVER_DEREF_ALIASES;

    /** The list of aliased entries that still have to be dereferenced */
    private List<String> aliasedIds;

    /** The evaluator to validate the candidates */
    private Evaluator<? extends ExprNode> evaluator;

    /** The SchemaManager */
    private SchemaManager schemaManager;


    /**
     * Create a PartitionSearchResult instance
     */
    public PartitionSearchResult( SchemaManager schemaManager )
    {
        this.schemaManager = schemaManager;
    }


    /**
     * @return the resultSet
     */
    public SetCursor<IndexEntry<String, String>> getResultSet()
    {
        return resultSet;
    }


    /**
     * @param resultSet the resultSet to set
     */
    public void setResultSet( Set<IndexEntry<String, String>> set )
    {
        resultSet = new SetCursor<IndexEntry<String, String>>( set );
    }


    /**
     * @return the candidateSet
     */
    public Set<String> getCandidateSet()
    {
        return candidateSet;
    }


    /**
     * @param candidateSet the candidateSet to set
     */
    public void setCandidateSet( Set<String> set )
    {
        candidateSet = set;
    }


    /**
     * @return the evaluator
     */
    public Evaluator<? extends ExprNode> getEvaluator()
    {
        return evaluator;
    }


    /**
     * @param evaluator the evaluator to set
     */
    public void setEvaluator( Evaluator<? extends ExprNode> evaluator )
    {
        this.evaluator = evaluator;
    }


    /**
     * @return the aliasedIds
     */
    public List<String> getAliasedIds()
    {
        return aliasedIds;
    }


    /**
     * @param aliasedIds the aliasedIds to set
     */
    public void setAliasedIds( List<String> aliasedIds )
    {
        this.aliasedIds = aliasedIds;
    }


    /**
     * @param aliasDerefMode the aliasDerefMode to set
     */
    public void setAliasDerefMode( AliasDerefMode aliasDerefMode )
    {
        this.aliasDerefMode = aliasDerefMode;

        if ( !isNeverDeref() )
        {
            aliasedIds = new ArrayList<String>();
        }
    }


    /**
     * @return True if the alias is never dereferenced
     */
    public boolean isNeverDeref()
    {
        return aliasDerefMode == AliasDerefMode.NEVER_DEREF_ALIASES;
    }


    /**
     * @return True if the alias is always dereferenced
     */
    public boolean isAlwaysDeref()
    {
        return aliasDerefMode == AliasDerefMode.DEREF_ALWAYS;
    }


    /**
     * @return True if the alias is dereferenced while searching
     */
    public boolean isDerefInSearching()
    {
        return aliasDerefMode == AliasDerefMode.DEREF_IN_SEARCHING;
    }


    /**
     * @return True if the alias is dereferenced while finding
     */
    public boolean isDerefFinding()
    {
        return aliasDerefMode == AliasDerefMode.DEREF_FINDING_BASE_OBJ;
    }


    /**
     * @return the schemaManager
     */
    public SchemaManager getSchemaManager()
    {
        return schemaManager;
    }


    /**
     * @param schemaManager the schemaManager to set
     */
    public void setSchemaManager( SchemaManager schemaManager )
    {
        this.schemaManager = schemaManager;
    }


    /**
     * @see Object#toString()
     */
    public String toString()
    {
        StringBuilder sb = new StringBuilder();

        sb.append( "Search result : \n" );
        sb.append( "Alias : " ).append( aliasDerefMode ).append( "\n" );
        sb.append( "Evaluator : " ).append( evaluator ).append( "\n" );

        if ( resultSet == null )
        {
            sb.append( "No UUID found" );
        }
        else
        {
            sb.append( '{' );
            boolean isFirst = true;

            try
            {
                while ( resultSet.next() )
                {
                    if ( isFirst )
                    {
                        isFirst = false;
                    }
                    else
                    {
                        sb.append( ", " );
                    }

                    sb.append( resultSet.get().getId() );
                }

                resultSet.beforeFirst();
            }
            catch ( Exception e )
            {
                // Nothing we can do...
            }

            sb.append( '}' );
        }

        return sb.toString();
    }
}
