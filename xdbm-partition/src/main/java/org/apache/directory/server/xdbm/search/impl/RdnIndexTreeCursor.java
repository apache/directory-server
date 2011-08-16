/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.directory.server.xdbm.search.impl;


import java.util.Deque;
import java.util.LinkedList;

import org.apache.directory.server.xdbm.AbstractIndexCursor;
import org.apache.directory.server.xdbm.ForwardIndexEntry;
import org.apache.directory.server.xdbm.Index;
import org.apache.directory.server.xdbm.IndexCursor;
import org.apache.directory.server.xdbm.IndexEntry;
import org.apache.directory.server.xdbm.ParentIdAndRdn;
import org.apache.directory.shared.ldap.model.exception.LdapInvalidDnException;
import org.apache.directory.shared.ldap.model.name.Rdn;


/**
 * A Cursor that traverses the RDN index as a tree.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class RdnIndexTreeCursor<E, ID extends Comparable<ID>> extends AbstractIndexCursor<ID, E, ID>
{

    /** Special RDN used as end marker of a tree level. Also used as marker afterLast()/last() operations */
    private static final Rdn STOP_ELEMENT;
    static
    {
        try
        {
            STOP_ELEMENT = new Rdn( "zzz=zzz" );
        }
        catch ( LdapInvalidDnException e )
        {
            throw new RuntimeException( "Error initializing stop element", e );
        }
    }

    private final Index<ParentIdAndRdn<ID>, E, ID> rdnIndex;
    private final ID startId;
    private final boolean oneLevel;

    /** 
     * A stack containing all cursors. On construction the root cursor is added. 
     * While the tree is traversed in depth-first order more cursors are added and removed.
     * The root cursor is never removed.
     */
    private final CursorStack cursors;

    /** Whether or not this Cursor is positioned so an entry is available */
    private boolean available;


    public RdnIndexTreeCursor( Index<ParentIdAndRdn<ID>, E, ID> rdnIndex, ID startId, boolean oneLevel )
        throws Exception
    {
        this.rdnIndex = rdnIndex;
        this.startId = startId;
        this.oneLevel = oneLevel;

        this.cursors = new CursorStack();

        if ( oneLevel )
        {
            this.cursors.createChildCursor( startId );
        }
        else
        {
            this.cursors.createChildCursor( rdnIndex.reverseLookup( startId ) );
        }

        beforeFirst();
    }


    public boolean available()
    {
        return available;
    }


    public void before( IndexEntry<ID, E, ID> element ) throws Exception
    {
        throw new UnsupportedOperationException();
    }


    public void beforeValue( ID id, ID value ) throws Exception
    {
        throw new UnsupportedOperationException();
    }


    public void afterValue( ID id, ID value ) throws Exception
    {
        throw new UnsupportedOperationException();
    }


    public void after( IndexEntry<ID, E, ID> element ) throws Exception
    {
        throw new UnsupportedOperationException();
    }


    public void beforeFirst() throws Exception
    {
        checkNotClosed( "beforeFirst()" );

        // remove and close all but the root cursors in the stack
        while ( cursors.size() > 1 )
        {
            cursors.closeCurrentCursor();
        }

        // position the root cursor
        cursors.getCurrentCursor().before( cursors.getCurrentCursorStartElement() );
        available = false;
    }


    public void afterLast() throws Exception
    {
        checkNotClosed( "afterLast()" );

        // remove and close all but the root cursors in the stack
        while ( cursors.size() > 1 )
        {
            cursors.closeCurrentCursor();
        }

        // position the root cursor
        cursors.getCurrentCursor().after( cursors.getCurrentCursorStopElement() );
        available = false;
    }


    public boolean first() throws Exception
    {
        checkNotClosed( "first()" );
        beforeFirst();
        return next();
    }


    public boolean last() throws Exception
    {
        checkNotClosed( "last()" );
        afterLast();
        return previous();
    }


    public boolean previous() throws Exception
    {
        checkNotClosed( "previous()" );
        if ( oneLevel )
        {
            return previousOneLevel();
        }
        else
        {
            return previousSubLevel();
        }
    }


    private boolean previousOneLevel() throws Exception
    {
        return previousCurrentLevel();
    }


    private boolean previousSubLevel() throws Exception
    {
        if ( previousCurrentLevel() )
        {
            // depth first traversal: check if previous index entry has children
            do
            {
                ID id = cursors.getCurrentCursor().get().getId();
                cursors.createChildCursor( id );
                cursors.getCurrentCursor().after( cursors.getCurrentCursorStopElement() );
            }
            while ( previousCurrentLevel() );

            cursors.closeCurrentCursor();

            available = true;
            return true;
        }

        // back one level
        if ( cursors.size() > 1 )
        {
            cursors.closeCurrentCursor();
            available = true;
            return true;
        }

        return false;
    }


    private boolean previousCurrentLevel() throws Exception
    {
        if ( cursors.getCurrentCursor().previous() )
        {
            // Compare the previous index entry's parent with the start element
            IndexEntry<ParentIdAndRdn<ID>, E, ID> indexEntry = cursors.getCurrentCursor().get();
            ParentIdAndRdn<ID> currentParentIdAndRdn = indexEntry.getValue();
            ParentIdAndRdn<ID> startParentIdAndRdn = cursors.getCurrentCursorStartElement().getValue();
            if ( currentParentIdAndRdn.getParentId().equals( startParentIdAndRdn.getParentId() )
                && currentParentIdAndRdn.compareTo( startParentIdAndRdn ) >= 0 )
            {
                available = true;
                return true;
            }
            else
            {
                cursors.getCurrentCursor().next();
                available = false;
                return false;
            }
        }
        else
        {
            available = false;
            return false;
        }
    }


    public boolean next() throws Exception
    {
        checkNotClosed( "next()" );
        if ( oneLevel )
        {
            return nextOneLevel();
        }
        else
        {
            return nextSubLevel();
        }
    }


    private boolean nextOneLevel() throws Exception
    {
        return nextCurrentLevel();
    }


    private boolean nextSubLevel() throws Exception
    {
        // depth first traversal: check if current index entry has children
        // TODO: use one-level count for optimization
        if ( available )
        {
            ID id = cursors.getCurrentCursor().get().getId();
            cursors.createChildCursor( id );
            cursors.getCurrentCursor().before( cursors.getCurrentCursorStartElement() );
            if ( nextCurrentLevel() )
            {
                return true;
            }
            else
            {
                cursors.closeCurrentCursor();
            }
        }

        // next element at current level
        if ( nextCurrentLevel() )
        {
            return true;
        }

        // back one level
        while ( cursors.size() > 1 )
        {
            cursors.closeCurrentCursor();
            if ( nextCurrentLevel() )
            {
                return true;
            }
        }

        return false;
    }


    private boolean nextCurrentLevel() throws Exception
    {
        if ( cursors.getCurrentCursor().next() )
        {
            // Compare the next index entry's parent with the stop element
            IndexEntry<ParentIdAndRdn<ID>, E, ID> indexEntry = cursors.getCurrentCursor().get();
            ParentIdAndRdn<ID> currentParentIdAndRdn = indexEntry.getValue();
            ParentIdAndRdn<ID> stopParentIdAndRdn = cursors.getCurrentCursorStopElement().getValue();
            if ( currentParentIdAndRdn.getParentId().equals( stopParentIdAndRdn.getParentId() )
                && currentParentIdAndRdn.compareTo( stopParentIdAndRdn ) <= 0 )
            {
                available = true;
                return true;
            }
            else
            {
                cursors.getCurrentCursor().previous();
                available = false;
                return false;
            }
        }
        else
        {
            available = false;
            return false;
        }
    }


    @Override
    public IndexEntry<ID, E, ID> get() throws Exception
    {
        IndexEntry<ParentIdAndRdn<ID>, E, ID> wrappedEntry = cursors.getCurrentCursor().get();
        IndexEntry<ID, E, ID> entry = new ForwardIndexEntry<ID, E, ID>();
        entry.setValue( startId );
        //entry.setValue( wrappedEntry.getValue().getParentId() );
        entry.setId( wrappedEntry.getId() );
        return entry;
    }

    private class CursorStack
    {
        private final Deque<CursorStackEntry> cursorStack;


        public CursorStack()
        {
            this.cursorStack = new LinkedList<CursorStackEntry>();
        }


        public void createChildCursor( ID startId ) throws Exception
        {
            CursorStackEntry entry = new CursorStackEntry( startId );
            cursorStack.addFirst( entry );
        }


        public void createChildCursor( ParentIdAndRdn<ID> parentIdAndRdn ) throws Exception
        {
            CursorStackEntry entry = new CursorStackEntry( parentIdAndRdn );
            cursorStack.addFirst( entry );
        }


        /**
         * @return the current cursor from the cursor stack
         */
        public IndexCursor<ParentIdAndRdn<ID>, E, ID> getCurrentCursor()
        {
            IndexCursor<ParentIdAndRdn<ID>, E, ID> cursor = cursorStack.getFirst().cursor;
            return cursor;
        }


        /**
         * @return the current cursor's start element from the cursor stack
         */
        public IndexEntry<ParentIdAndRdn<ID>, E, ID> getCurrentCursorStartElement()
        {
            IndexEntry<ParentIdAndRdn<ID>, E, ID> startElement = cursorStack.getFirst().startElement;
            return startElement;
        }


        /**
         * @return the current cursor's stop element from the cursor stack
         */
        public IndexEntry<ParentIdAndRdn<ID>, E, ID> getCurrentCursorStopElement()
        {
            IndexEntry<ParentIdAndRdn<ID>, E, ID> startElement = cursorStack.getFirst().stopElement;
            return startElement;
        }


        public void closeCurrentCursor() throws Exception
        {
            cursorStack.removeFirst().cursor.close();
        }


        public int size()
        {
            return cursorStack.size();
        }


        @Override
        public String toString()
        {
            return "CursorStack [cursorStack=" + cursorStack + "]";
        }

        private class CursorStackEntry
        {
            private final IndexCursor<ParentIdAndRdn<ID>, E, ID> cursor;
            private final IndexEntry<ParentIdAndRdn<ID>, E, ID> startElement;
            private final IndexEntry<ParentIdAndRdn<ID>, E, ID> stopElement;


            public CursorStackEntry( ID startId ) throws Exception
            {
                this.cursor = rdnIndex.forwardCursor();
                this.startElement = createElement( new ParentIdAndRdn<ID>( startId ) );
                this.stopElement = createElement( new ParentIdAndRdn<ID>( startId, STOP_ELEMENT ) );
            }


            public CursorStackEntry( ParentIdAndRdn<ID> parentIdAndRdn ) throws Exception
            {
                this.cursor = rdnIndex.forwardCursor();
                this.startElement = createElement( parentIdAndRdn );
                this.stopElement = createElement( parentIdAndRdn );
            }


            private IndexEntry<ParentIdAndRdn<ID>, E, ID> createElement( ParentIdAndRdn<ID> parentIdAndRdn )
            {
                IndexEntry<ParentIdAndRdn<ID>, E, ID> startElement = new ForwardIndexEntry<ParentIdAndRdn<ID>, E, ID>();
                startElement.setValue( parentIdAndRdn );
                return startElement;
            }


            @Override
            public String toString()
            {
                //return "CSE:" + startElement.getValue().getParentId();
                return "CSE:" + startElement;
            }
        }
    }
}
