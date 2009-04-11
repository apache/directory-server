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
package org.apache.directory.shared.ldap.cursor;


/**
 * An empty Cursor implementation.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class EmptyCursor<E> extends AbstractCursor<E>
{
    /**
     * {@inheritDoc}
     */
    public boolean available()
    {
        return false;
    }

    /**
     * {@inheritDoc}
     */
    public void before( E element ) throws Exception
    {
        checkNotClosed( "before()" );
    }


    /**
     * {@inheritDoc}
     */
    public void after( E element ) throws Exception
    {
        checkNotClosed( "after()" );
    }


    /**
     * {@inheritDoc}
     */
    public void beforeFirst() throws Exception
    {
        checkNotClosed( "beforeFirst()" );
    }


    /**
     * {@inheritDoc}
     */
    public void afterLast() throws Exception
    {
        checkNotClosed( "afterLast()" );
    }


    /**
     * {@inheritDoc}
     */
    public boolean first() throws Exception
    {
        checkNotClosed( "first()" );
        return false;
    }


    /**
     * {@inheritDoc}
     */
    public boolean last() throws Exception
    {
        checkNotClosed( "last()" );
        return false;
    }


    /**
     * {@inheritDoc}
     */
    public boolean previous() throws Exception
    {
        checkNotClosed( "previous()" );
        return false;
    }


    /**
     * {@inheritDoc}
     */
    public boolean next() throws Exception
    {
        checkNotClosed( "next()" );
        return false;
    }


    /**
     * {@inheritDoc}
     */
    public E get() throws Exception
    {
        checkNotClosed( "get()" );
        throw new InvalidCursorPositionException( "This cursor is empty and cannot return elements!" );
    }


    /**
     * {@inheritDoc}
     */
    public boolean isElementReused()
    {
        return false;
    }
}
