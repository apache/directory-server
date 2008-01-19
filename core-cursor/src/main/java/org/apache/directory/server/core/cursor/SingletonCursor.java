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


import java.util.Comparator;


/**
 * A Cursor over a single element.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class SingletonCursor<E> extends AbstractCursor<E>
{
    private boolean beforeFirst = true;
    private boolean afterLast;
    private boolean onSingleton;
    private final Comparator<E> comparator;
    private final E singleton;


    public SingletonCursor( E singleton )
    {
        this( singleton, null );
    }


    public SingletonCursor( E singleton, Comparator<E> comparator )
    {
        this.singleton = singleton;
        this.comparator = comparator;
    }


    public boolean available()
    {
        return onSingleton;
    }
    

    public void before( E element ) throws Exception
    {
        checkClosed( "before()" );

        if ( comparator == null )
        {
            throw new UnsupportedOperationException(
                    "Without a comparator I cannot advance to just before the specified element." );
        }

        int comparison = comparator.compare( singleton, element );

        if ( comparison < 0 )
        {
            first();
        }
        else
        {
            beforeFirst();
        }
    }


    public void after( E element ) throws Exception
    {
        checkClosed( "after()" );

        if ( comparator == null )
        {
            throw new UnsupportedOperationException(
                    "Without a comparator I cannot advance to just after the specified element." );
        }

        int comparison = comparator.compare( singleton, element );

        if ( comparison > 0 )
        {
            first();
        }
        else
        {
            afterLast();
        }
    }


    public void beforeFirst() throws Exception
    {
        checkClosed( "()" );
        beforeFirst = true;
        afterLast = false;
        onSingleton = false;
    }


    public void afterLast() throws Exception
    {
        checkClosed( "()" );
        beforeFirst = false;
        afterLast = true;
        onSingleton = false;
    }


    public boolean first() throws Exception
    {
        checkClosed( "()" );
        beforeFirst = false;
        onSingleton = true;
        afterLast = false;
        return true;
    }


    public boolean last() throws Exception
    {
        checkClosed( "()" );
        beforeFirst = false;
        onSingleton = true;
        afterLast = false;
        return true;
    }


    public boolean isFirst() throws Exception
    {
        checkClosed( "()" );
        return onSingleton;
    }


    public boolean isLast() throws Exception
    {
        checkClosed( "()" );
        return onSingleton;
    }


    public boolean isAfterLast() throws Exception
    {
        checkClosed( "()" );
        return afterLast;
    }


    public boolean isBeforeFirst() throws Exception
    {
        checkClosed( "()" );
        return beforeFirst;
    }


    public boolean previous() throws Exception
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


    public boolean next() throws Exception
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


    public E get() throws Exception
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
