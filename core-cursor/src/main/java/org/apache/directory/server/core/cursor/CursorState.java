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
 * A Cursor's state: cursor states leverage the State Pattern to isolate state
 * specific transition logic with certain operations.  Not every Cursor is
 * that complex so the implementor should decide whether or not using the
 * State Pattern is over kill on a per Cursor implementation basis.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public interface CursorState<E>
{
    void before( E element ) throws IOException;

    void after( E element ) throws IOException;

    void beforeFirst() throws IOException;

    void afterLast() throws IOException;

    boolean relative( int offset ) throws IOException;

    boolean first() throws IOException;

    boolean last() throws IOException;

    boolean previous() throws IOException;

    boolean next() throws IOException;
}
