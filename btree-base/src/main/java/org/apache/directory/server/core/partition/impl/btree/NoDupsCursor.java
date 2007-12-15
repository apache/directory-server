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
package org.apache.directory.server.core.partition.impl.btree;


import org.apache.directory.server.core.cursor.AbstractCursor;
import org.apache.directory.server.core.cursor.InvalidCursorPositionException;
import org.apache.directory.server.core.cursor.InconsistentCursorStateException;
import org.apache.directory.shared.ldap.NotImplementedException;

import java.io.IOException;


/**
 * @todo Man you better test this!!!
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class NoDupsCursor extends AbstractCursor<Tuple>
{
    private final Tuple tuple = new Tuple();
    private final TupleBrowserFactory factory;

    private long pos = BEFORE_FIRST;
    private long size;  // cache the size to prevent needless lookups
    private boolean afterLast;
    private boolean beforeFirst;
    private TupleBrowser browser;
    private boolean success;


    public NoDupsCursor( TupleBrowserFactory factory ) throws IOException
    {
        this( factory, false );
    }


    public NoDupsCursor( TupleBrowserFactory factory, boolean afterLast ) throws IOException
    {
        this.factory = factory;

        if ( afterLast )
        {
            afterLast();
        }
        else
        {
            beforeFirst();
        }
    }


    public NoDupsCursor( TupleBrowserFactory factory, int absolute ) throws IOException
    {
        this.factory = factory;
        absolute( absolute );
    }


    public NoDupsCursor( TupleBrowserFactory factory, Object key ) throws IOException
    {
        this.factory = factory;
        beforeKey( key );
    }


    /**
     * @todo
     *
     * This is a little tricky. How do we know where we are positioned?  We could
     * do some check perhaps with getNext() and backup with getPrevious().
     *
     * @param key
     * @throws IOException
     */
    private void beforeKey( Object key ) throws IOException
    {
        browser = factory.beforeKey( key );
        beforeFirst = false;
        afterLast = false;
        size = factory.size();
        pos = BEFORE_FIRST;
        success = true;

        throw new NotImplementedException( "Need to fix the todo on this before going further" );
    }


    public boolean before( Tuple element ) throws IOException
    {
        throw new NotImplementedException();
    }


    public boolean after( Tuple element ) throws IOException
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
            size = factory.size();
            pos = BEFORE_FIRST;
            browser = factory.beforeFirst();
        }
    }


    public void afterLast() throws IOException
    {
        if ( ! afterLast )
        {
            beforeFirst = false;
            afterLast = true;
            success = false;
            size = factory.size();
            pos = size;
            browser = factory.afterLast();
        }
    }


    public boolean absolute( int absolutePosition ) throws IOException
    {
        // -------------------------------------------------------------------
        // Special cases under or above the valid range puts the cursor
        // respectively before the first or after the last position
        // -------------------------------------------------------------------

        if ( absolutePosition >= size )
        {
            afterLast();
            return false;
        }

        if ( absolutePosition < 0 )
        {
            beforeFirst();
            return false;
        }

        // -------------------------------------------------------------------
        // Special case where position is valid and that's the new position
        // -------------------------------------------------------------------

        if ( absolutePosition == pos )
        {
            return success;
        }

        // -------------------------------------------------------------------
        // Special easy to get to cases where we don't have to walk the tree
        // -------------------------------------------------------------------

        if ( absolutePosition == 0 && beforeFirst )
        {
            return next();
        }

        if ( ( absolutePosition == size - 1  ) && afterLast )
        {
            return previous();
        }

        // -------------------------------------------------------------------
        // Cases we have to walk the tree forward or backwards to get to target
        // -------------------------------------------------------------------

        if ( absolutePosition > pos )
        {
            while ( success && pos < absolutePosition )
            {
                next();
            }
        }
        else
        {
            while ( success && pos > absolutePosition )
            {
                previous();
            }
        }

        return success;
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
            success = browser.getPrevious( tuple );
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

        success = browser.getPrevious( tuple );
        if ( success )
        {
            pos--;
        }
        return success;
    }


    public boolean next() throws IOException
    {
        return success = this.browser.getNext( tuple );
    }


    private boolean inRangeOnValue()
    {
        return pos > BEFORE_FIRST && pos < size;
    }



    public Tuple get() throws IOException
    {
        if ( ! inRangeOnValue() )
        {
            throw new InvalidCursorPositionException();
        }

        if ( success )
        {
            return tuple;
        }
        else
        {
            throw new InconsistentCursorStateException( "Seems like the position is in range however the " +
                    "last operation failed to produce a successful result" );
        }
    }


    public boolean isElementReused()
    {
        return true;
    }
}
