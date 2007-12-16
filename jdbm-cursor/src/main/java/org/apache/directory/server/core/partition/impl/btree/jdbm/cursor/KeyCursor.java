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
package org.apache.directory.server.core.partition.impl.btree.jdbm.cursor;


import jdbm.btree.BTree;
import jdbm.helper.Tuple;
import jdbm.helper.TupleBrowser;

import org.apache.directory.server.core.cursor.AbstractCursor;
import org.apache.directory.server.core.cursor.InconsistentCursorStateException;
import org.apache.directory.server.core.cursor.InvalidCursorPositionException;
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
    private final Tuple jdbmTuple = new Tuple();

    private BTree btree;
    private TupleBrowser browser;
    private int pos = BEFORE_FIRST;
    private int size;  // cache the size to prevent needless lookups
    private boolean afterLast;
    private boolean beforeFirst;
    private boolean success;


    /**
     * Creates a Cursor over the keys of a JDBM BTree.
     *
     * @param btree the JDBM BTree
     * @throws IOException of there are problems accessing the BTree
     */
    KeyCursor( BTree btree ) throws IOException
    {
        this.btree = btree;
        beforeFirst();
    }


    public void before( E element ) throws IOException
    {
        browser = btree.browse( element );
        pos = UNDEFINED;
        success = false;
        afterLast = false;
        beforeFirst = false;
        jdbmTuple.setKey( null );
        jdbmTuple.setValue( null );
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
            pos = BEFORE_FIRST;
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
            pos = size;
            browser = btree.browse( null );
        }
    }


    public boolean relative( int relativePosition ) throws IOException
    {
        // -------------------------------------------------------------------
        // Special cases under or above the valid range puts the cursor
        // respectively before the first or after the last position
        // -------------------------------------------------------------------

        if ( ( relativePosition + pos ) >= size )
        {
            afterLast();
            return false;
        }

        if ( ( relativePosition + pos ) < 0 )
        {
            beforeFirst();
            return false;
        }

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

        if ( pos == 0 )
        {
            return success;
        }

        beforeFirst();
        return next();
    }


    public boolean last() throws IOException
    {
        if ( afterLast )
        {
            return previous();
        }

        if ( pos == ( size - 1 ) )
        {
            return success;
        }

        afterLast();
        return previous();
    }


    public boolean isFirst() throws IOException
    {
        return pos == 0;
    }


    public boolean isLast() throws IOException
    {
        return pos == ( size - 1 );
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
            success = browser.getPrevious( jdbmTuple );
            if ( success )
            {
                afterLast = false;
                beforeFirst = false;
                pos = size - 1;
            }
            return success;
        }

        if ( pos == 0 )
        {
            success = false;
            afterLast = false;
            beforeFirst = true;
            pos = BEFORE_FIRST;
            return false;
        }

        success = browser.getPrevious( jdbmTuple );
        if ( success )
        {
            pos--;
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
            success = browser.getNext( jdbmTuple );
            if ( success )
            {
                afterLast = false;
                beforeFirst = false;
                pos = 0;
            }
            return success;
        }

        if ( pos == size - 1 )
        {
            success = false;
            afterLast = true;
            beforeFirst = false;
            pos = size;
            return false;
        }

        success = browser.getNext( jdbmTuple );
        if ( success )
        {
            pos++;
        }
        return success;
    }


    private boolean inRangeOnValue()
    {
        return pos > BEFORE_FIRST && pos < size;
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
            return ( E ) jdbmTuple.getKey();
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
}
