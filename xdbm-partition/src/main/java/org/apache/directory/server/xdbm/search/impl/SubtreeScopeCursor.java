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


import java.util.UUID;

import org.apache.directory.server.constants.ApacheSchemaConstants;
import org.apache.directory.server.core.api.partition.Partition;
import org.apache.directory.server.core.api.partition.index.AbstractIndexCursor;
import org.apache.directory.server.core.api.partition.index.Index;
import org.apache.directory.server.core.api.partition.index.IndexCursor;
import org.apache.directory.server.core.api.partition.index.IndexEntry;
import org.apache.directory.server.core.api.txn.TxnLogManager;
import org.apache.directory.server.core.shared.partition.OperationExecutionManagerFactory;
import org.apache.directory.server.core.shared.txn.TxnManagerFactory;
import org.apache.directory.server.i18n.I18n;
import org.apache.directory.shared.ldap.model.cursor.InvalidCursorPositionException;
import org.apache.directory.shared.ldap.model.message.SearchScope;
import org.apache.directory.shared.ldap.model.name.Dn;


/**
 * A Cursor over entries satisfying scope constraints with alias dereferencing
 * considerations.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class SubtreeScopeCursor extends AbstractIndexCursor<UUID>
{
    private static final String UNSUPPORTED_MSG = I18n.err( I18n.ERR_719 );

    /** The Entry database/store */
    private final Partition db;

    /** A ScopeNode Evaluator */
    private final SubtreeScopeEvaluator evaluator;

    /** A Cursor over the entries in the scope of the search base */
    private final IndexCursor<UUID> scopeCursor;

    /** A Cursor over entries brought into scope by alias dereferencing */
    private final IndexCursor<UUID> dereferencedCursor;

    /** Currently active Cursor: we switch between two cursors */
    private IndexCursor<UUID> cursor;

    private UUID contextEntryId;

    /** Alias idx if dereferencing aliases */
    private Index<String> aliasIdx;

    /** Txn and Operation Execution Factories */
    private TxnManagerFactory txnManagerFactory;
    private OperationExecutionManagerFactory executionManagerFactory;


    /**
     * Creates a Cursor over entries satisfying subtree level scope criteria.
     *
     * @param db the entry store
     * @param evaluator an IndexEntry (candidate) evaluator
     * @throws Exception on db access failures
     */
    @SuppressWarnings("unchecked")
    public SubtreeScopeCursor( Partition db, SubtreeScopeEvaluator evaluator, TxnManagerFactory txnManagerFactory,
        OperationExecutionManagerFactory executionManagerFactory )
        throws Exception
    {
        this.txnManagerFactory = txnManagerFactory;
        this.executionManagerFactory = executionManagerFactory;

        TxnLogManager txnLogManager = txnManagerFactory.txnLogManagerInstance();
        this.db = db;
        this.evaluator = evaluator;

        if ( evaluator.getBaseId().compareTo( getContextEntryId() ) == 0 )
        {
            scopeCursor = new AllEntriesCursor( db, txnManagerFactory, executionManagerFactory );
        }
        else
        {
            Index<?> subLevelIdx = db.getSystemIndex( ApacheSchemaConstants.APACHE_SUB_LEVEL_AT_OID );
            subLevelIdx = txnLogManager.wrap( db.getSuffixDn(), subLevelIdx );
            scopeCursor = ( ( Index<UUID> ) subLevelIdx ).forwardCursor( evaluator.getBaseId() );
        }

        if ( evaluator.isDereferencing() )
        {
            Index<?> subAliasIdx = db.getSystemIndex( ApacheSchemaConstants.APACHE_SUB_ALIAS_AT_OID );
            subAliasIdx = txnLogManager.wrap( db.getSuffixDn(), subAliasIdx );
            dereferencedCursor = ( ( Index<UUID> ) subAliasIdx ).forwardCursor( evaluator.getBaseId() );

            aliasIdx = ( Index<String> ) db.getSystemIndex( ApacheSchemaConstants.APACHE_ALIAS_AT_OID );
            aliasIdx = ( Index<String> ) txnLogManager.wrap( db.getSuffixDn(), aliasIdx );
        }
        else
        {
            dereferencedCursor = null;
        }
    }


    /**
     * {@inheritDoc}
     */
    protected String getUnsupportedMessage()
    {
        return UNSUPPORTED_MSG;
    }


    // This will suppress PMD.EmptyCatchBlock warnings in this method
    private UUID getContextEntryId() throws Exception
    {
        if ( contextEntryId == null )
        {
            try
            {
                this.contextEntryId = executionManagerFactory.instance().getEntryId( db, db.getSuffixDn() );
            }
            catch ( Exception e )
            {
                // might not have been created
                // might not have been created
            }
        }

        if ( contextEntryId == null )
        {
            return Partition.defaultID;
        }

        return contextEntryId;
    }


    public void beforeFirst() throws Exception
    {
        checkNotClosed( "beforeFirst()" );
        cursor = scopeCursor;
        cursor.beforeFirst();
        setAvailable( false );
    }


    public void afterLast() throws Exception
    {
        checkNotClosed( "afterLast()" );
        if ( evaluator.isDereferencing() )
        {
            cursor = dereferencedCursor;
        }
        else
        {
            cursor = scopeCursor;
        }

        cursor.afterLast();
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
        checkNotClosed( "previous()" );
        // if the cursor has not been set - position it after last element
        if ( cursor == null )
        {
            afterLast();
        }

        // if we're using the scopeCursor (1st Cursor) then return result as is
        if ( cursor == scopeCursor )
        {
            /*
             * If dereferencing is enabled then we must ignore alias entries, not
             * returning them as part of the results.
             */
            if ( evaluator.isDereferencing() )
            {
                // advance until nothing is available or until we find a non-alias
                do
                {
                    checkNotClosed( "previous()" );
                    setAvailable( cursor.previous() );

                    if ( available() && aliasIdx.reverseLookup( cursor.get().getId() ) == null )
                    {
                        break;
                    }
                }
                while ( available() );
            }
            else
            {
                setAvailable( cursor.previous() );
            }

            return available();
        }

        /*
         * Below here we are using the dereferencedCursor so if nothing is
         * available after an advance backwards we need to switch to the
         * scopeCursor and try a previous call after positioning past it's
         * last element.
         */
        setAvailable( cursor.previous() );

        if ( !available() )
        {
            cursor = scopeCursor;
            cursor.afterLast();

            // advance until nothing is available or until we find a non-alias
            do
            {
                checkNotClosed( "previous()" );
                setAvailable( cursor.previous() );

                if ( available() && aliasIdx.reverseLookup( cursor.get().getId() ) == null )
                {
                    break;
                }
            }
            while ( available() );

            return available();
        }

        return true;
    }


    public boolean next() throws Exception
    {
        checkNotClosed( "next()" );
        // if the cursor hasn't been set position it before the first element
        if ( cursor == null )
        {
            beforeFirst();
        }

        /*
         * If dereferencing is enabled then we must ignore alias entries, not
         * returning them as part of the results.
         */
        if ( evaluator.isDereferencing() )
        {
            // advance until nothing is available or until we find a non-alias
            do
            {
                checkNotClosed( "next()" );
                setAvailable( cursor.next() );

                if ( available() && aliasIdx.reverseLookup( cursor.get().getId() ) == null )
                {
                    break;
                }
            }
            while ( available() );
        }
        else
        {
            setAvailable( cursor.next() );
        }

        // if we're using dereferencedCursor (2nd) then we return the result
        if ( cursor == dereferencedCursor )
        {
            return available();
        }

        /*
         * Below here we are using the scopeCursor so if nothing is
         * available after an advance forward we need to switch to the
         * dereferencedCursor and try a previous call after positioning past
         * it's last element.
         */
        if ( !available() )
        {
            if ( dereferencedCursor != null )
            {
                cursor = dereferencedCursor;
                cursor.beforeFirst();

                return setAvailable( cursor.next() );
            }

            return false;
        }

        return true;
    }


    public IndexEntry<UUID> get() throws Exception
    {
        checkNotClosed( "get()" );

        if ( available() )
        {
            IndexEntry<UUID> indexEntry = cursor.get();

            /*
             *  If the entry is coming from the alias index, then search scope is enlarged
             *  to include the returned entry.
             */

            if ( cursor == dereferencedCursor )
            {
                Dn aliasTargetDn = executionManagerFactory.instance().buildEntryDn( db, indexEntry.getId() );
                txnManagerFactory.txnLogManagerInstance().addRead( aliasTargetDn, SearchScope.OBJECT );
            }

            return indexEntry;
        }

        throw new InvalidCursorPositionException( I18n.err( I18n.ERR_708 ) );
    }


    private void closeCursors() throws Exception
    {
        if ( dereferencedCursor != null )
        {
            dereferencedCursor.close();
        }

        if ( scopeCursor != null )
        {
            scopeCursor.close();
        }
    }


    @Override
    public void close() throws Exception
    {
        closeCursors();
        super.close();
    }


    @Override
    public void close( Exception cause ) throws Exception
    {
        closeCursors();
        super.close( cause );
    }
}
