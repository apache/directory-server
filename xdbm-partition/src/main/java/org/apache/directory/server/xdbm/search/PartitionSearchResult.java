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


import java.util.Set;

import org.apache.directory.server.xdbm.IndexEntry;
import org.apache.directory.shared.ldap.model.cursor.SetCursor;
import org.apache.directory.shared.ldap.model.filter.ExprNode;


/**
 * A class containing the result of a search :
 * <ul>
 * <li>A set of cadidate UUIDs</li>
 * <li>A hierarchy of evaualtors to use to validate the candidates</li>
 * </ul>
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class PartitionSearchResult
{
    /** The set of candidate UUIDs selected by the search */
    private SetCursor<IndexEntry<String, String>> resultSet;

    /** The evaluator to validate the candidates */
    private Evaluator<? extends ExprNode> evaluator;


    /**
     * Create a PartitionSearchResult instance
     */
    public PartitionSearchResult()
    {
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
     * @see Object#toString()
     */
    public String toString()
    {
        StringBuilder sb = new StringBuilder();

        sb.append( "Search result : \n" );
        sb.append( evaluator );

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
