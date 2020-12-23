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
import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.api.ldap.model.filter.ExprNode;
import org.apache.directory.api.ldap.model.message.SearchScope;
import org.apache.directory.api.ldap.model.name.Dn;
import org.apache.directory.api.ldap.model.schema.SchemaManager;
import org.apache.directory.server.core.api.CoreSession;
import org.apache.directory.server.core.api.DirectoryService;
import org.apache.directory.server.core.api.filtering.EntryFilteringCursorImpl;
import org.apache.directory.server.core.api.interceptor.context.SearchOperationContext;
import org.apache.directory.server.core.api.partition.PartitionTxn;
import org.apache.directory.server.core.partition.impl.btree.AbstractBTreePartition;
import org.apache.directory.server.core.partition.impl.btree.EntryCursorAdaptor;
import org.apache.directory.server.core.partition.impl.btree.IndexCursorAdaptor;
import org.apache.directory.server.xdbm.IndexEntry;
import org.apache.directory.server.xdbm.Store;
import org.apache.directory.server.xdbm.search.Evaluator;
import org.apache.directory.server.xdbm.search.PartitionSearchResult;


/**
 * A class containing common method and fields for Cursor tests.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class AbstractCursorTest
{
    protected EvaluatorBuilder evaluatorBuilder;
    protected CursorBuilder cursorBuilder;
    protected Store store;
    protected static SchemaManager schemaManager;
    protected DirectoryService directoryService;;
    protected CoreSession session;


    /**
     * Creates a cursor from a filter
     * 
     * @param root The filter we are using for the cursor construction
     * @return The constructed cursor
     * @throws Exception If anything went wrong
     */
    protected Cursor<Entry> buildCursor( PartitionTxn partitionTxn, ExprNode root ) throws Exception
    {
        Evaluator<? extends ExprNode> evaluator = evaluatorBuilder.build( partitionTxn, root );

        PartitionSearchResult searchResult = new PartitionSearchResult( schemaManager );
        Set<IndexEntry<String, String>> resultSet = new HashSet<IndexEntry<String, String>>();

        Set<String> uuids = new HashSet<String>();
        searchResult.setCandidateSet( uuids );

        long candidates = cursorBuilder.build( partitionTxn, root, searchResult );

        if ( candidates < Long.MAX_VALUE )
        {
            for ( String uuid : uuids )
            {
                IndexEntry<String, String> indexEntry = new IndexEntry<String, String>();
                indexEntry.setId( uuid );
                resultSet.add( indexEntry );
            }
        }
        else
        {
            // Full scan : use the MasterTable
            Cursor<IndexEntry<String, String>> cursor = new IndexCursorAdaptor( partitionTxn, store.getMasterTable().cursor(), true );

            while ( cursor.next() )
            {
                IndexEntry<String, String> indexEntry = cursor.get();

                // Here, the indexEntry contains a <UUID, Entry> tuple. Convert it to <UUID, UUID> 
                IndexEntry<String, String> forwardIndexEntry = new IndexEntry<String, String>();
                forwardIndexEntry.setKey( indexEntry.getKey() );
                forwardIndexEntry.setId( indexEntry.getKey() );
                forwardIndexEntry.setEntry( indexEntry.getEntry() );

                resultSet.add( forwardIndexEntry );
            }
        }

        searchResult.setResultSet( resultSet );
        searchResult.setEvaluator( evaluator );

        // We want all the user attributes plus the entryUUID
        SearchOperationContext operationContext = 
            new SearchOperationContext( session, Dn.ROOT_DSE, SearchScope.ONELEVEL, null, "*", "EntryUUID" );
        
        return new EntryFilteringCursorImpl( new EntryCursorAdaptor( partitionTxn, ( AbstractBTreePartition ) store, searchResult ),
            operationContext, directoryService.getSchemaManager() );
    }
}
