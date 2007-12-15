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
package org.apache.directory.server.core.cursor;


import java.io.IOException;


/**
 * A Cursor on a single element.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class SingletonCursor<E> extends AbstractCursor
{
    private boolean beforeFirst = true;
    private boolean afterLast;
    private boolean onSingleton;
    private final E singleton;


    public SingletonCursor( E singleton )
    {
        this.singleton = singleton;
    }


    public void beforeFirst() throws IOException
    {
        checkClosed( "()" );
        beforeFirst = true;
        afterLast = false;
        onSingleton = false;
    }


    public void afterLast() throws IOException
    {
        checkClosed( "()" );
        beforeFirst = false;
        afterLast = true;
        onSingleton = false;
    }


    public boolean absolute( int absolutePosition ) throws IOException
    {
        checkClosed( "()" );
        if ( absolutePosition == 0 )
        {
            beforeFirst = false;
            onSingleton = true;
            afterLast = false;
            return true;
        }
        else if ( absolutePosition > 0 )
        {
            beforeFirst = false;
            onSingleton = false;
            afterLast = true;
            return false;
        }
        else
        {
            beforeFirst = true;
            onSingleton = false;
            afterLast = false;
            return false;
        }
    }


    public boolean relative( int relativePosition ) throws IOException
    {
        checkClosed( "()" );

        if ( relativePosition == 0 )
        {
            return true;
        }

        if ( ( relativePosition == -1 && afterLast ) ||
             ( relativePosition == 1 && beforeFirst ) )
        {
            beforeFirst = false;
            onSingleton = true;
            afterLast = false;
            return true;
        }

        if ( relativePosition > 1 )
        {
            beforeFirst = false;
            onSingleton = false;
            afterLast = true;
            return false;
        }

        // below this then relativePosition < 1
        beforeFirst = true;
        onSingleton = false;
        afterLast = false;
        return false;
    }


    public boolean first() throws IOException
    {
        checkClosed( "()" );
        beforeFirst = false;
        onSingleton = true;
        afterLast = false;
        return true;
    }


    public boolean last() throws IOException
    {
        checkClosed( "()" );
        beforeFirst = false;
        onSingleton = true;
        afterLast = false;
        return true;
    }


    public boolean isFirst() throws IOException
    {
        checkClosed( "()" );
        return onSingleton;
    }


    public boolean isLast() throws IOException
    {
        checkClosed( "()" );
        return onSingleton;
    }


    public boolean isAfterLast() throws IOException
    {
        checkClosed( "()" );
        return afterLast;
    }


    public boolean isBeforeFirst() throws IOException
    {
        checkClosed( "()" );
        return beforeFirst;
    }


    public boolean previous() throws IOException
    {
        checkClosed( "()" );
        if ( beforeFirst )
        {
            return false;
        }

        if ( afterLast )
        {
            beforeFirst = false;
            onSingleton = true;
            afterLast = false;
            return true;
        }

        // must be on the singleton
        beforeFirst = true;
        onSingleton = false;
        afterLast = false;
        return false;
    }


    public boolean next() throws IOException
    {
        checkClosed( "()" );
        if ( beforeFirst )
        {
            beforeFirst = false;
            onSingleton = true;
            afterLast = false;
            return true;
        }

        if ( afterLast )
        {
            return false;
        }

        // must be on the singleton
        beforeFirst = false;
        onSingleton = false;
        afterLast = true;
        return false;
    }


    public E get() throws IOException
    {
        checkClosed( "()" );
        if ( onSingleton )
        {
            return singleton;
        }

        if ( beforeFirst )
        {
            throw new InvalidCursorPositionException( "Cannot access element if positioned before first." );
        }
        else
        {
            throw new InvalidCursorPositionException( "Cannot access element if positioned after last." );
        }
    }


    public boolean isElementReused()
    {
        return true;
    }
}
