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
import java.util.Iterator;


/**
 * A limited Cursor over an Iterator of elements.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class IteratorCursor<E> extends AbstractCursor<E>
{
    private final Iterator<E> values;
    private E current;


    public IteratorCursor( Iterator<E> values )
    {
        this.values = values;
    }


    public void before( E element ) throws IOException
    {
        throw new UnsupportedOperationException( "Cannot advance before an element on the underlying Iterator." );
    }


    public void after( E element ) throws IOException
    {
        throw new UnsupportedOperationException( "Cannot advance after an element on the underlying Iterator." );
    }


    public void beforeFirst() throws IOException
    {
        throw new UnsupportedOperationException( "Cannot advance before first on the underlying Iterator." );
    }


    public void afterLast() throws IOException
    {
        throw new UnsupportedOperationException( "Cannot adanvce after last on the underlying Iterator." );
    }


    public boolean first() throws IOException
    {
        throw new UnsupportedOperationException( "Cannot advance to first position on the underlying Iterator." );
    }


    public boolean last() throws IOException
    {
        throw new UnsupportedOperationException( "Cannot advance to last position on the underlying Iterator." );
    }


    public boolean previous() throws IOException
    {
        throw new UnsupportedOperationException( "Cannot back up on the underlying Iterator." );
    }


    public boolean next() throws IOException
    {
        checkClosed( "next()" );
        if ( values.hasNext() )
        {
            current = values.next();
            return true;
        }

        return false;
    }


    public E get() throws IOException
    {
        checkClosed( "get()" );
        return current;
    }


    public boolean isElementReused()
    {
        return false;
    }
}
