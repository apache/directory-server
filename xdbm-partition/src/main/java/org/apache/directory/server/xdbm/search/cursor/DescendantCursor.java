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
package org.apache.directory.server.xdbm.search.cursor;


import java.util.UUID;

import org.apache.commons.collections.ArrayStack;
import org.apache.directory.server.i18n.I18n;
import org.apache.directory.server.xdbm.AbstractIndexCursor;
import org.apache.directory.server.xdbm.ForwardIndexEntry;
import org.apache.directory.server.xdbm.IndexEntry;
import org.apache.directory.server.xdbm.ParentIdAndRdn;
import org.apache.directory.server.xdbm.Store;
import org.apache.directory.shared.ldap.model.cursor.Cursor;
import org.apache.directory.shared.ldap.model.entry.Entry;
import org.apache.directory.shared.ldap.model.name.Rdn;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * A Cursor over entries satisfying one level scope constraints with alias
 * dereferencing considerations when enabled during search.
 * We use the Rdn index to fetch all the descendants of a given entry.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class DescendantCursor extends AbstractIndexCursor<UUID>
{
    /** A dedicated log for cursors */
    private static final Logger LOG_CURSOR = LoggerFactory.getLogger( "CURSOR" );

    /** Error message for unsupported operations */
    private static final String UNSUPPORTED_MSG = I18n.err( I18n.ERR_719 );

    /** The entry database/store */
    private final Store<Entry> db;

    /** The prefetched element */
    private IndexEntry prefetched;

    /** The current Cursor over the entries in the scope of the search base */
    private Cursor<IndexEntry<ParentIdAndRdn, UUID>> currentCursor;

    /** The current Parent ID */
    private UUID currentParentId;

    /** The stack of cursors used to process the depth-first traversal */
    private ArrayStack cursorStack;

    /** The stack of parentIds used to process the depth-first traversal */
    private ArrayStack parentIdStack;

    /** The initial entry ID we are looking descendants for */
    private UUID baseId;

    /** A flag to tell that we are in the top level cursor or not */
    private boolean topLevel;

    protected static final boolean TOP_LEVEL = true;
    protected static final boolean INNER = false;


    /**
     * Creates a Cursor over entries satisfying one level scope criteria.
     *
     * @param db the entry store
     * @param evaluator an IndexEntry (candidate) evaluator
     * @throws Exception on db access failures
     */
    public DescendantCursor( Store<Entry> db, UUID baseId, UUID parentId,
        Cursor<IndexEntry<ParentIdAndRdn, UUID>> cursor )
        throws Exception
    {
        this( db, baseId, parentId, cursor, TOP_LEVEL );
    }


    /**
     * Creates a Cursor over entries satisfying one level scope criteria.
     *
     * @param db the entry store
     * @param evaluator an IndexEntry (candidate) evaluator
     * @throws Exception on db access failures
     */
    public DescendantCursor( Store<Entry> db, UUID baseId, UUID parentId,
        Cursor<IndexEntry<ParentIdAndRdn, UUID>> cursor,
        boolean topLevel )
        throws Exception
    {
        LOG_CURSOR.debug( "Creating ChildrenCursor {}", this );
        this.db = db;
        currentParentId = parentId;
        currentCursor = cursor;
        cursorStack = new ArrayStack();
        parentIdStack = new ArrayStack();
        this.baseId = baseId;
        this.topLevel = topLevel;
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
        setAvailable( false );
    }


    public void afterLast() throws Exception
    {
        throw new UnsupportedOperationException( getUnsupportedMessage() );
    }


    public boolean first() throws Exception
    {
        beforeFirst();

        return next();
    }


    public boolean last() throws Exception
    {
        throw new UnsupportedOperationException( getUnsupportedMessage() );
    }


    public boolean previous() throws Exception
    {
        checkNotClosed( "next()" );

        boolean hasPrevious = currentCursor.previous();

        if ( hasPrevious )
        {
            IndexEntry entry = currentCursor.get();

            if ( ( ( ParentIdAndRdn ) entry.getTuple().getKey() ).getParentId().equals( currentParentId ) )
            {
                prefetched = entry;
                return true;
            }
        }

        return false;
    }


    /**
     * {@inheritDoc}
     */
    public boolean next() throws Exception
    {
        checkNotClosed( "next()" );
        boolean finished = false;

        while ( !finished )
        {
            boolean hasNext = currentCursor.next();

            // We will use a depth first approach. The alternative (Breadth-first) would be
            // too memory consuming. 
            // The idea is to use a ChildrenCursor each time we have an entry with chidren, 
            // and process recursively.
            if ( hasNext )
            {
                IndexEntry cursorEntry = currentCursor.get();
                ParentIdAndRdn parentIdAndRdn = ( ( ParentIdAndRdn ) ( cursorEntry.getKey() ) );

                // Check that we aren't out of the cursor's limit
                if ( !parentIdAndRdn.getParentId().equals( currentParentId ) )
                {
                    // Ok, we went too far. Unstack the cursor and return
                    finished = cursorStack.size() == 0;

                    if ( !finished )
                    {
                        currentCursor.close();
                        currentCursor = ( Cursor<IndexEntry<ParentIdAndRdn, UUID>> ) cursorStack.pop();
                        currentParentId = ( UUID ) parentIdStack.pop();
                    }

                    // And continue...
                }
                else
                {
                    // We have a candidate, it will be returned.
                    if ( topLevel )
                    {
                        prefetched = new ForwardIndexEntry();
                        prefetched.setId( cursorEntry.getId() );
                        prefetched.setKey( baseId );
                    }
                    else
                    {
                        prefetched = cursorEntry;
                    }

                    // Check if the current entry has children or not.
                    if ( parentIdAndRdn.getNbDescendants() > 0 )
                    {
                        UUID newParentId = ( UUID ) cursorEntry.getId();

                        // Yes, then create a new cursor and go down one level
                        Cursor<IndexEntry<ParentIdAndRdn, UUID>> cursor = db.getRdnIndex().forwardCursor();

                        IndexEntry<ParentIdAndRdn, UUID> startingPos = new ForwardIndexEntry<ParentIdAndRdn, UUID>();
                        startingPos.setKey( new ParentIdAndRdn( newParentId, ( Rdn[] ) null ) );
                        cursor.before( startingPos );

                        cursorStack.push( currentCursor );
                        parentIdStack.push( currentParentId );

                        currentCursor = cursor;
                        currentParentId = newParentId;
                    }

                    return true;
                }
            }
            else
            {
                // The current cursor has been exhausted. Get back to the parent's cursor.
                finished = cursorStack.size() == 0;

                if ( !finished )
                {
                    currentCursor.close();
                    currentCursor = ( Cursor<IndexEntry<ParentIdAndRdn, UUID>> ) cursorStack.pop();
                    currentParentId = ( UUID ) parentIdStack.pop();
                }
                // and continue...
            }
        }

        return false;
    }


    public IndexEntry<UUID, UUID> get() throws Exception
    {
        checkNotClosed( "get()" );

        return prefetched;
    }


    @Override
    public void close() throws Exception
    {
        LOG_CURSOR.debug( "Closing ChildrenCursor {}", this );

        // Close the cursors stored in the stack, if we have some
        for ( Object cursor : cursorStack )
        {
            ( ( Cursor<IndexEntry<?, ?>> ) cursor ).close();
        }

        // And finally, close the current cursor
        currentCursor.close();

        super.close();
    }


    @Override
    public void close( Exception cause ) throws Exception
    {
        LOG_CURSOR.debug( "Closing ChildrenCursor {}", this );

        // Close the cursors stored in the stack, if we have some
        for ( Object cursor : cursorStack )
        {
            ( ( Cursor<IndexEntry<?, ?>> ) cursor ).close( cause );
        }

        // And finally, close the current cursor
        currentCursor.close( cause );

        super.close( cause );
    }
}