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


import org.apache.directory.server.i18n.I18n;
import org.apache.directory.server.core.api.partition.Partition;
import org.apache.directory.server.core.api.partition.index.AbstractIndexCursor;
import org.apache.directory.server.core.api.partition.index.Index;
import org.apache.directory.server.core.api.partition.index.IndexCursor;
import org.apache.directory.server.core.api.partition.index.IndexEntry;
import org.apache.directory.server.core.api.txn.TxnLogManager;
import org.apache.directory.server.core.shared.partition.OperationExecutionManagerFactory;
import org.apache.directory.server.core.shared.txn.TxnManagerFactory;
import org.apache.directory.server.xdbm.search.Evaluator;
import org.apache.directory.shared.ldap.model.constants.SchemaConstants;
import org.apache.directory.shared.ldap.model.cursor.InvalidCursorPositionException;
import org.apache.directory.shared.ldap.model.filter.ExprNode;


/**
 * A Cursor returning candidates satisfying a logical negation expression.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class NotCursor<V> extends AbstractIndexCursor<V>
{
    private static final String UNSUPPORTED_MSG = I18n.err( I18n.ERR_718 );
    private final IndexCursor<String> uuidCursor;
    private final Evaluator<? extends ExprNode> childEvaluator;
    
    /** Txn and Operation Execution Factories */
    private TxnManagerFactory txnManagerFactory;
    private OperationExecutionManagerFactory executionManagerFactory;


    @SuppressWarnings("unchecked")
    public NotCursor( Partition store, Evaluator<? extends ExprNode> childEvaluator, TxnManagerFactory txnManagerFactory,
        OperationExecutionManagerFactory executionManagerFactory )
        throws Exception
    {
        this.txnManagerFactory = txnManagerFactory;
        this.executionManagerFactory = executionManagerFactory;
        
        this.childEvaluator = childEvaluator;
 
        TxnLogManager txnLogManager = txnManagerFactory.txnLogManagerInstance();
        Index<?> entryUuidIdx = store.getSystemIndex( SchemaConstants.ENTRY_UUID_AT_OID );
        entryUuidIdx = txnLogManager.wrap( store.getSuffixDn(), entryUuidIdx );
        uuidCursor = ( ( Index<String> )entryUuidIdx ).forwardCursor();
    }


    /**
     * {@inheritDoc}
     */
    protected String getUnsupportedMessage()
    {
        return UNSUPPORTED_MSG;
    }

    
    public void beforeFirst() throws Exception
    {
        checkNotClosed( "beforeFirst()" );
        uuidCursor.beforeFirst();
        setAvailable( false );
    }


    public void afterLast() throws Exception
    {
        checkNotClosed( "afterLast()" );
        uuidCursor.afterLast();
        setAvailable( false );
    }


    public boolean first() throws Exception
    {
        beforeFirst();
        
        return next();
    }


    public boolean last() throws Exception
    {
        afterLast();
        
        return previous();
    }


    public boolean previous() throws Exception
    {
        while ( uuidCursor.previous() )
        {
            checkNotClosed( "previous()" );
            IndexEntry<?> candidate = uuidCursor.get();
            
            if ( !childEvaluator.evaluate( candidate ) )
            {
                return setAvailable( true );
            }
        }

        return setAvailable( false );
    }


    public boolean next() throws Exception
    {
        while ( uuidCursor.next() )
        {
            checkNotClosed( "next()" );
            IndexEntry<?> candidate = uuidCursor.get();
            
            if ( !childEvaluator.evaluate( candidate ) )
            {
                return setAvailable( true );
            }
        }

        return setAvailable( false );
    }


    public IndexEntry<V> get() throws Exception
    {
        checkNotClosed( "get()" );
        
        if ( available() )
        {
            return ( IndexEntry<V> ) uuidCursor.get();
        }

        throw new InvalidCursorPositionException( I18n.err( I18n.ERR_708 ) );
    }


    public void close() throws Exception
    {
        super.close();
        uuidCursor.close();
    }
}
