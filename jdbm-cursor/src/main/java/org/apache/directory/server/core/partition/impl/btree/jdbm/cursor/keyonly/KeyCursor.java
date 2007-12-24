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
package org.apache.directory.server.core.partition.impl.btree.jdbm.cursor.keyonly;


import jdbm.btree.BTree;
import jdbm.helper.Tuple;
import jdbm.helper.TupleBrowser;

import org.apache.directory.server.core.cursor.AbstractCursor;
import org.apache.directory.server.core.cursor.InconsistentCursorStateException;
import org.apache.directory.server.core.cursor.InvalidCursorPositionException;
import org.apache.directory.server.core.cursor.CursorState;
import org.apache.directory.shared.ldap.NotImplementedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;


/**
 * Cursor over the keys of a JDBM BTree.  Obviously does not return duplicate
 * keys since JDBM does not natively support multiple values for the same key.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class KeyCursor<E> extends AbstractCursor<E>
{
    private static final Logger LOG = LoggerFactory.getLogger( KeyCursor.class );
    private final Tuple tuple = new Tuple();

    private final CursorState afterInner = new AfterInnerState( this );
    private final CursorState afterLast = new AfterLastState( this );
    private final CursorState beforeFirst = new BeforeFirstState( this );
    private final CursorState beforeInner = new BeforeInnerState( this );
    private final CursorState closed = new ClosedState();
    private final CursorState opened = new OpenedState( this );
    private final CursorState onFirst = new OnFirstState( this );
    private final CursorState onInner = new OnInnerState( this );
    private final CursorState onLast = new OnLastState( this );
    private final CursorState empty = new EmptyCursorState( this );

    private CursorState state = opened;
    private BTree btree;
    private TupleBrowser browser;
    private int size;  // cache the size to prevent needless lookups

    private E first;
    private E last;


    /**
     * Creates a Cursor over the keys of a JDBM BTree.
     *
     * @param btree the JDBM BTree
     * @throws IOException of there are problems accessing the BTree
     */
    KeyCursor( BTree btree ) throws IOException
    {
        this.btree = btree;
        this.size = btree.size();
    }


    public void before( E element ) throws IOException
    {
        browser = btree.browse( element );
        success = false;
        afterLast = false;
        beforeFirst = false;
        tuple.setKey( null );
        tuple.setValue( null );
    }


    public void after( E element ) throws IOException
    {
        throw new NotImplementedException();
    }


    public void beforeFirst() throws IOException
    {
        if ( ! beforeFirst )
        {
            beforeFirst = true;
            afterLast = false;
            success = false;
            size = btree.size();
            browser = btree.browse();
        }
    }


    public void afterLast() throws IOException
    {
        if ( ! afterLast )
        {
            beforeFirst = false;
            afterLast = true;
            success = false;
            size = btree.size();
            browser = btree.browse( null );
        }
    }


    public boolean relative( int relativePosition ) throws IOException
    {
        // -------------------------------------------------------------------
        // Special cases under or above the valid range puts the cursor
        // respectively before the first or after the last position
        // -------------------------------------------------------------------

//        if ( ( relativePosition + pos ) >= size )
//        {
//            afterLast();
//            return false;
//        }
//
//        if ( ( relativePosition + pos ) < 0 )
//        {
//            beforeFirst();
//            return false;
//        }

        // -------------------------------------------------------------------
        // Special case where position is valid and that's the new position
        // -------------------------------------------------------------------

        if ( relativePosition == 0 )
        {
            return success;
        }

        // -------------------------------------------------------------------
        // Cases we have to walk the tree forward or backwards
        // -------------------------------------------------------------------

        if ( relativePosition > 0 )
        {
            for ( ; success && relativePosition > 0; relativePosition-- )
            {
                next();
            }
        }
        else
        {
            for ( ; success && relativePosition < 0; relativePosition++ )
            {
                previous();
            }
        }

        return success;
    }


    public boolean first() throws IOException
    {
        if ( beforeFirst )
        {
            return next();
        }

//        if ( pos == 0 )
//        {
//            return success;
//        }

        beforeFirst();
        return next();
    }


    public boolean last() throws IOException
    {
        if ( afterLast )
        {
            return previous();
        }

//        if ( pos == ( size - 1 ) )
//        {
//            return success;
//        }

        afterLast();
        return previous();
    }


    public boolean isFirst() throws IOException
    {
//        return pos == 0;
        throw new NotImplementedException();
    }


    public boolean isLast() throws IOException
    {
//        return pos == ( size - 1 );
        throw new NotImplementedException();
    }


    public boolean isAfterLast() throws IOException
    {
        return afterLast;
    }


    public boolean isBeforeFirst() throws IOException
    {
        return beforeFirst;
    }


    public boolean previous() throws IOException
    {
        if ( beforeFirst )
        {
            return false;
        }

        if ( afterLast )
        {
            success = browser.getPrevious( tuple );
            if ( success )
            {
                afterLast = false;
                beforeFirst = false;
//                pos = size - 1;
            }
            return success;
        }

//        if ( pos == 0 )
//        {
//            success = false;
//            afterLast = false;
//            beforeFirst = true;
//            pos = BEFORE_FIRST;
//            return false;
//        }

        success = browser.getPrevious( tuple );
        if ( success )
        {
//            pos--;
        }
        return success;
    }


    public boolean next() throws IOException
    {
        if ( afterLast )
        {
            return false;
        }

        if ( beforeFirst )
        {
            success = browser.getNext( tuple );
            if ( success )
            {
                afterLast = false;
                beforeFirst = false;
//                pos = 0;
            }
            return success;
        }

//        if ( pos == size - 1 )
//        {
//            success = false;
//            afterLast = true;
//            beforeFirst = false;
//            pos = size;
//            return false;
//        }

        success = browser.getNext( tuple );
        if ( success )
        {
//            pos++;
        }
        return success;
    }


    private boolean inRangeOnValue()
    {
//        return pos > BEFORE_FIRST && pos < size;
        throw new NotImplementedException();
    }



    public E get() throws IOException
    {
        if ( ! inRangeOnValue() )
        {
            throw new InvalidCursorPositionException();
        }

        if ( success )
        {
            //noinspection unchecked
            return ( E ) tuple.getKey();
        }
        else
        {
            throw new InconsistentCursorStateException( "Seems like the position is in range however the " +
                    "last operation failed to produce a successful result" );
        }
    }


    public boolean isElementReused()
    {
        return false;
    }


    BTree getBtree()
    {
        return btree;
    }


    Tuple getTuple()
    {
        return tuple;
    }


    CursorState getState()
    {
        return state;
    }


    void setState( CursorState state )
    {
        this.state = state;
    }


    TupleBrowser getBrowser()
    {
        return browser;
    }


    void setBrowser( TupleBrowser browser )
    {
        this.browser = browser;
    }


    int size()
    {
        return size;
    }


    E getFirst() throws IOException
    {
        if ( size == 0 )
        {
            return null;
        }

        if ( first != null )
        {
            return first;
        }

        TupleBrowser browser = btree.browse();
        Tuple tuple = new Tuple();
        if ( browser.getNext( tuple ) )
        {
            return first = ( E ) tuple.getKey();
        }

        return first = null;
    }


    E getLast() throws IOException
    {
        if ( size == 0 )
        {
            return null;
        }

        if ( last != null )
        {
            return last;
        }

        TupleBrowser browser = btree.browse( null );
        Tuple tuple = new Tuple();
        if ( browser.getPrevious( tuple ) )
        {
            return last = ( E ) tuple.getKey();
        }
        return last = null;
    }


    public CursorState getAfterInner()
    {
        return afterInner;
    }


    public CursorState getAfterLast()
    {
        return afterLast;
    }


    public CursorState getBeforeFirst()
    {
        return beforeFirst;
    }


    public CursorState getBeforeInner()
    {
        return beforeInner;
    }


    public CursorState getClosed()
    {
        return closed;
    }


    public CursorState getOpened()
    {
        return opened;
    }


    public CursorState getOnFirst()
    {
        return onFirst;
    }


    public CursorState getOnInner()
    {
        return onInner;
    }


    public CursorState getOnLast()
    {
        return onLast;
    }
}
