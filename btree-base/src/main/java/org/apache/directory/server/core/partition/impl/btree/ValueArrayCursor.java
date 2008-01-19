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


import org.apache.directory.server.core.cursor.Cursor;
import org.apache.directory.server.core.cursor.CursorClosedException;
import org.apache.directory.server.core.cursor.InvalidCursorPositionException;
import org.apache.directory.shared.ldap.NotImplementedException;

import java.util.Arrays;
import java.util.List;


/**
 * A Cursor which returns the values of a single key as Tuples.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class ValueArrayCursor<K,V> implements Cursor<Tuple>
{
    private static final int BEFORE_FIRST = -1;

    private final K key;
    private final List<V> values;
    private final Tuple<K,V> tuple = new Tuple<K,V>();

    private boolean closed;
    private int pos = BEFORE_FIRST;


    public ValueArrayCursor( final K key, final V[] values )
    {
        this.key = key;
        this.tuple.setKey( key );
        this.values = Arrays.asList( values );
    }


    public ValueArrayCursor( final K key, final List<V> values )
    {
        this.key = key;
        this.tuple.setKey( key );
        this.values = values;
    }


    protected void checkClosed( String operation ) throws Exception
    {
        if ( closed )
        {
            throw new CursorClosedException( "Attempting " + operation
                    + " operation on a closed Cursor." );
        }
    }


    public boolean available()
    {
        return inRangeOnValue();
    }


    public void before( Tuple element ) throws Exception
    {
        throw new NotImplementedException();
    }


    public void after( Tuple element ) throws Exception
    {
        throw new NotImplementedException();
    }


    public void beforeFirst() throws Exception
    {
        checkClosed( "beforeFirst()" );
        pos = BEFORE_FIRST;
    }


    public void afterLast() throws Exception
    {
        checkClosed( "afterLast()" );
        pos = values.size();
    }


    public boolean absolute( int absolutePosition ) throws Exception
    {
        checkClosed( "absolute()" );
        if ( absolutePosition >= values.size() )
        {
            pos = values.size();
            return false;
        }

        if ( absolutePosition < 0 )
        {
            pos = BEFORE_FIRST;
            return false;
        }

        pos = absolutePosition;
        return true;
    }


    public boolean relative( int relativePosition ) throws Exception
    {
        checkClosed( "relative()" );
        if ( ( relativePosition + pos ) >= values.size() )
        {
            pos = values.size();
            return false;
        }

        if ( ( relativePosition + pos ) < 0 )
        {
            pos = BEFORE_FIRST;
            return false;
        }

        pos += relativePosition;
        return true;
    }


    public boolean first() throws Exception
    {
        checkClosed( "first()" );
        pos = 0;
        return true;
    }


    public boolean last() throws Exception
    {
        checkClosed( "last()" );
        pos = values.size() - 1;
        return true;
    }


    public boolean isFirst() throws Exception
    {
        checkClosed( "isFirst()" );
        return pos == 0;
    }


    public boolean isLast() throws Exception
    {
        checkClosed( "isLast()" );
        return pos == values.size() - 1;
    }


    public boolean isAfterLast() throws Exception
    {
        checkClosed( "isAfterLast()" );
        return pos == values.size();
    }


    public boolean isBeforeFirst() throws Exception
    {
        checkClosed( "isBeforeFirst()" );
        return pos == BEFORE_FIRST;
    }


    public boolean isClosed() throws Exception
    {
        return closed;
    }


    public boolean previous() throws Exception
    {
        checkClosed( "previous()" );
        if ( pos <= BEFORE_FIRST )
        {
            return false;
        }

        pos--;
        return inRangeOnValue();
    }


    private boolean inRangeOnValue()
    {
        return pos > BEFORE_FIRST && pos < values.size();
    }


    public boolean next() throws Exception
    {
        checkClosed( "next()" );
        if ( pos >= values.size() )
        {
            return false;
        }

        pos++;
        return inRangeOnValue();
    }


    public Tuple get() throws Exception
    {
        checkClosed( "get()" );
        if ( inRangeOnValue() )
        {
            return tuple.setBoth( key, values.get( pos ) );
        }

        throw new InvalidCursorPositionException( "Cursor pos (" + pos
                + ") not in value range [0-" + ( values.size() - 1 )+ "]" );
    }


    public boolean isElementReused()
    {
        return true;
    }


    public void close() throws Exception
    {
        closed = true;
    }
}
