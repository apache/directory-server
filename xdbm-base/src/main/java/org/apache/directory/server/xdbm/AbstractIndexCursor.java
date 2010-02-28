/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *  
 *    http://www.apache.org/licenses/LICENSE-2.0
 *  
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License. 
 *  
 */
package org.apache.directory.server.xdbm;


import java.util.Iterator;

import org.apache.directory.shared.ldap.cursor.ClosureMonitor;
import org.apache.directory.shared.ldap.cursor.CursorIterator;
import org.apache.directory.shared.ldap.cursor.DefaultClosureMonitor;


/**
 * An abstract TupleCursor.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public abstract class AbstractIndexCursor<K, E, ID> implements IndexCursor<K, E, ID>
{
    private ClosureMonitor monitor = new DefaultClosureMonitor();


    public final void setClosureMonitor( ClosureMonitor monitor )
    {
        if ( monitor == null )
        {
            throw new NullPointerException( "monitor" );
        }

        this.monitor = monitor;
    }


    protected final void checkNotClosed( String operation ) throws Exception
    {
        monitor.checkNotClosed();
    }


    public final boolean isClosed()
    {
        return monitor.isClosed();
    }


    public void close() throws Exception
    {
        monitor.close();
    }


    public void close( Exception cause ) throws Exception
    {
        monitor.close( cause );
    }


    public Iterator<IndexEntry<K, E, ID>> iterator()
    {
        return new CursorIterator<IndexEntry<K, E, ID>>( this );
    }
}