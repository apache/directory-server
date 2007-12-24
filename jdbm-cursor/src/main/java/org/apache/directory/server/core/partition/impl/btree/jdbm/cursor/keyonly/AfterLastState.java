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

import org.apache.directory.server.core.cursor.CursorState;

import java.io.IOException;


/**
 * The KeyCursor's after last state.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class AfterLastState<E> implements CursorState<E>
{
    private final KeyCursor cursor;


    public AfterLastState( KeyCursor cursor )
    {
        this.cursor = cursor;
    }


    public void before( E element ) throws IOException
    {

    }


    public void after( E element ) throws IOException
    {

    }


    public void beforeFirst() throws IOException
    {

    }


    public void afterLast() throws IOException
    {

    }


    public boolean relative( int offset ) throws IOException
    {
        return false;
    }


    public boolean first() throws IOException
    {
        return false;
    }


    public boolean last() throws IOException
    {
        return false;
    }


    public boolean previous() throws IOException
    {
        return false;
    }


    public boolean next() throws IOException
    {
        return false;
    }
}
