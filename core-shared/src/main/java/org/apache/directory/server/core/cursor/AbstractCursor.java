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


/**
 * Simple class that contains often used Cursor code.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public abstract class AbstractCursor<E> implements Cursor<E>
{
    protected static final int BEFORE_FIRST = -1;
    private boolean closed;


    protected void checkClosed( String operation ) throws CursorClosedException
    {
        if ( isClosed() )
        {
            throw new CursorClosedException( "Attempting " + operation + " operation on a closed Cursor." );
        }
    }


    public boolean isClosed()
    {
        return closed;
    }


    public void close()
    {
        closed = true;
    }
}
