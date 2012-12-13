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


import org.apache.directory.server.xdbm.IndexEntry;
import org.apache.directory.server.xdbm.Store;
import org.apache.directory.server.xdbm.search.Evaluator;
import org.apache.directory.shared.ldap.model.entry.Entry;
import org.apache.directory.shared.ldap.model.filter.UndefinedNode;


/**
 * An Evaluator that always validate all the submitted values
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class PassThroughEvaluator implements Evaluator<UndefinedNode>
{
    /** The backend */
    private final Store db;


    /**
     * Create a new instance of the PassThroughEvaluator
     * @throws Exception
     */
    public PassThroughEvaluator( Store db )
    {
        this.db = db;
    }


    /**
     * {@inheritDoc}
     */
    public boolean evaluate( IndexEntry<?, String> indexEntry ) throws Exception
    {
        Entry entry = indexEntry.getEntry();

        // resuscitate the entry if it has not been and set entry in IndexEntry
        if ( null == entry )
        {
            entry = db.fetch( indexEntry.getId() );

            if ( null == entry )
            {
                // The entry is not anymore present : get out
                return false;
            }

            indexEntry.setEntry( entry );
        }

        return true;
    }


    /**
     * {@inheritDoc}
     */
    public boolean evaluate( Entry entry ) throws Exception
    {
        return true;
    }


    /**
     * Gets the expression used by this expression Evaluator.
     *
     * @return the AST for the expression
     */
    public UndefinedNode getExpression()
    {
        return null;
    }


    /**
     * @see Object#toString()
     */
    public String toString( String tabs )
    {
        StringBuilder sb = new StringBuilder();

        sb.append( tabs ).append( "PassthroughEvaluator\n" );

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