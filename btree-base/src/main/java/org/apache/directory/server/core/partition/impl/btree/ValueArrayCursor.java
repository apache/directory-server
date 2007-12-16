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

import java.io.IOException;
import java.util.Arrays;
import java.util.List;


/**
 * A Cursor which returns the values of a single key as Tuples.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class ValueArrayCursor implements Cursor<Tuple>
{
    private static final int BEFORE_FIRST = -1;

    private final Object key;
    private final List<Object> values;
    private final Tuple tuple = new Tuple();

    private boolean closed;
    private int pos = BEFORE_FIRST;


    public ValueArrayCursor( final Object key, final Object[] values )
    {
        this.key = key;
        this.tuple.setKey( key );
        this.values = Arrays.asList( values );
    }


    public ValueArrayCursor( final Object key, final List<Object> values )
    {
        this.key = key;
        this.tuple.setKey( key );
        this.values = values;
    }


    protected void checkClosed( String operation ) throws IOException
    {
        if ( closed )
        {
            throw new CursorClosedException( "Attempting " + operation
                    + " operation on a closed Cursor." );
        }
    }


    public void before( Tuple element ) throws IOException
    {
        throw new NotImplementedException();
    }


    public boolean after( Tuple element ) throws IOException
    {
        throw new NotImplementedException();
    }


    public void beforeFirst() throws IOException
    {
        checkClosed( "beforeFirst()" );
        pos = BEFORE_FIRST;
    }


    public void afterLast() throws IOException
    {
        checkClosed( "afterLast()" );
        pos = values.size();
    }


    public boolean absolute( int absolutePosition ) throws IOException
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


    public boolean relative( int relativePosition ) throws IOException
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


    public boolean first() throws IOException
    {
        checkClosed( "first()" );
        pos = 0;
        return true;
    }


    public boolean last() throws IOException
    {
        checkClosed( "last()" );
        pos = values.size() - 1;
        return true;
    }


    public boolean isFirst() throws IOException
    {
        checkClosed( "isFirst()" );
        return pos == 0;
    }


    public boolean isLast() throws IOException
    {
        checkClosed( "isLast()" );
        return pos == values.size() - 1;
    }


    public boolean isAfterLast() throws IOException
    {
        checkClosed( "isAfterLast()" );
        return pos == values.size();
    }


    public boolean isBeforeFirst() throws IOException
    {
        checkClosed( "isBeforeFirst()" );
        return pos == BEFORE_FIRST;
    }


    public boolean isClosed() throws IOException
    {
        return closed;
    }


    public boolean previous() throws IOException
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


    public boolean next() throws IOException
    {
        checkClosed( "next()" );
        if ( pos >= values.size() )
        {
            return false;
        }

        pos++;
        return inRangeOnValue();
    }


    public Tuple get() throws IOException
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


    public void close() throws IOException
    {
        closed = true;
    }
}
