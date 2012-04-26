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


import java.util.List;

import org.apache.directory.server.core.api.partition.Partition;
import org.apache.directory.server.i18n.I18n;
import org.apache.directory.server.xdbm.AbstractIndexCursor;
import org.apache.directory.server.xdbm.ForwardIndexEntry;
import org.apache.directory.server.xdbm.IndexCursor;
import org.apache.directory.server.xdbm.IndexEntry;
import org.apache.directory.server.xdbm.ParentIdAndRdn;
import org.apache.directory.server.xdbm.SingletonIndexCursor;
import org.apache.directory.server.xdbm.Store;
import org.apache.directory.shared.ldap.model.cursor.InvalidCursorPositionException;
import org.apache.directory.shared.ldap.model.cursor.SingletonCursor;
import org.apache.directory.shared.ldap.model.entry.Entry;
import org.apache.directory.shared.ldap.model.name.Rdn;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * A Cursor over entries satisfying scope constraints with alias dereferencing
 * considerations.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class SubtreeScopeCursor<ID extends Comparable<ID>> extends AbstractIndexCursor<ID, Entry, ID>
{
    /** A dedicated log for cursors */
    private static final Logger LOG_CURSOR = LoggerFactory.getLogger( "CURSOR" );

    private static final String UNSUPPORTED_MSG = I18n.err( I18n.ERR_719 );

    /** The Entry database/store */
    private final Store<Entry, ID> db;

    /** A ScopeNode Evaluator */
    private final SubtreeScopeEvaluator<Entry, ID> evaluator;

    /** A Cursor over the entries in the scope of the search base */
    private final IndexCursor<ID, Entry, ID> scopeCursor;

    /** A Cursor over entries brought into scope by alias dereferencing */
    private final IndexCursor<ID, Entry, ID> dereferencedCursor;

    /** Currently active Cursor: we switch between two cursors */
    private IndexCursor<ID, Entry, ID> cursor;

    private ID contextEntryId;


    /**
     * Creates a Cursor over entries satisfying subtree level scope criteria.
     *
     * @param db the entry store
     * @param evaluator an IndexEntry (candidate) evaluator
     * @throws Exception on db access failures
     */
    public SubtreeScopeCursor( Store<Entry, ID> db, SubtreeScopeEvaluator<Entry, ID> evaluator )
        throws Exception
    {
        LOG_CURSOR.debug( "Creating SubtreeScopeCursor {}", this );
        this.db = db;
        this.evaluator = evaluator;

        if ( evaluator.getBaseId() == getContextEntryId() )
        {
            scopeCursor = new AllEntriesCursor<ID>( db );
        }
        else
        {
            // We use the RdnIndex to get all the entries from a starting point
            // and below up to the number of children
            ID baseId = evaluator.getBaseId();
            ParentIdAndRdn<ID> parentIdAndRdn = db.getRdnIndex().reverseLookup( baseId ); 
            IndexEntry indexEntry = new ForwardIndexEntry();
            
            indexEntry.setId( baseId );
            indexEntry.setKey( parentIdAndRdn );

            IndexCursor<ParentIdAndRdn<ID>,Entry, ID> cursor = new SingletonIndexCursor<ParentIdAndRdn<ID>, ID>( indexEntry );
            ID parentId = parentIdAndRdn.getParentId();

            scopeCursor = new DescendantCursor( db, baseId, parentId, cursor );
        }

        if ( evaluator.isDereferencing() )
        {
            dereferencedCursor = db.getSubAliasIndex().forwardCursor( evaluator.getBaseId() );
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
    private ID getContextEntryId() throws Exception
    {
        if ( contextEntryId == null )
        {
            try
            {
                this.contextEntryId = db.getEntryId( ( ( Partition ) db ).getSuffixDn() );
            }
            catch ( Exception e )
            {
                // might not have been created
                // might not have been created
            }
        }

        if ( contextEntryId == null )
        {
            return db.getDefaultId();
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

                    if ( available() && db.getAliasIndex().reverseLookup( cursor.get().getId() ) == null )
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

                if ( available() && db.getAliasIndex().reverseLookup( cursor.get().getId() ) == null )
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

                if ( available() && db.getAliasIndex().reverseLookup( cursor.get().getId() ) == null )
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


    public IndexEntry<ID, ID> get() throws Exception
    {
        checkNotClosed( "get()" );

        if ( available() )
        {
            return cursor.get();
        }

        throw new InvalidCursorPositionException( I18n.err( I18n.ERR_708 ) );
    }


    /**
     * {@inheritDoc}
     */
    public void close() throws Exception
    {
        LOG_CURSOR.debug( "Closing SubtreeScopeCursor {}", this );
        
        if ( dereferencedCursor != null )
        {
            dereferencedCursor.close();
        }

        if ( scopeCursor != null )
        {
            scopeCursor.close();
        }
        
        if ( cursor != null )
        {
            cursor.close();
        }

        super.close();
    }


    /**
     * {@inheritDoc}
     */
    public void close( Exception cause ) throws Exception
    {
        LOG_CURSOR.debug( "Closing SubtreeScopeCursor {}", this );
        
        if ( dereferencedCursor != null )
        {
            dereferencedCursor.close( cause );
        }

        if ( scopeCursor != null )
        {
            scopeCursor.close( cause );
        }
        
        if ( cursor != null )
        {
            cursor.close( cause );
        }

        super.close( cause );
    }
}
