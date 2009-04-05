/*
 *   Licensed to the Apache Software Foundation (ASF) under one
 *   or more contributor license agreements.  See the NOTICE file
 *   distributed with this work for additional information
 *   regarding copyright ownership.  The ASF licenses this file
 *   to you under the Apache License, Version 2.0 (the
 *   "License"); you may not use this file except in compliance
 *   with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing,
 *   software distributed under the License is distributed on an
 *   "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *   KIND, either express or implied.  See the License for the
 *   specific language governing permissions and limitations
 *   under the License.
 *
 */
package org.apache.directory.server.core.partition.ldif;


import java.util.Iterator;

import org.apache.directory.server.core.avltree.AvlTree;
import org.apache.directory.server.core.cursor.ClosureMonitor;
import org.apache.directory.server.core.cursor.Cursor;
import org.apache.directory.server.xdbm.Tuple;


/**
 * A Cursor designed to scan a AvlTree which supports duplicates as if each
 * duplicate key value was a single Tuple.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class DupsAvlCursor<K,V> implements Cursor<Tuple<K,V>>
{
    private final DupsAvlTable<K,V> table;
    private K currentKey;
    private Cursor<V> valueCursor;
    
    
    public DupsAvlCursor( DupsAvlTable<K,V> table )
    {
        this.table = table;
    }
    

    public void after( Tuple<K, V> element ) throws Exception
    {
        table.getAvlTree().findGreater( new Tuple<K,AvlTree<V>>( element.getKey(), null ) );
    }

    
    public void afterLast() throws Exception
    {
        // TODO Auto-generated method stub
    }

    
    public boolean available()
    {
        // TODO Auto-generated method stub
        return false;
    }

    public void before( Tuple<K, V> element ) throws Exception
    {
        // TODO Auto-generated method stub
        
    }

    public void beforeFirst() throws Exception
    {
        // TODO Auto-generated method stub
        
    }

    public void close() throws Exception
    {
        // TODO Auto-generated method stub
        
    }

    public void close( Exception reason ) throws Exception
    {
        // TODO Auto-generated method stub
        
    }

    public boolean first() throws Exception
    {
        // TODO Auto-generated method stub
        return false;
    }

    public Tuple<K, V> get() throws Exception
    {
        // TODO Auto-generated method stub
        return null;
    }

    public boolean isClosed() throws Exception
    {
        // TODO Auto-generated method stub
        return false;
    }

    public boolean isElementReused()
    {
        // TODO Auto-generated method stub
        return false;
    }

    public boolean last() throws Exception
    {
        // TODO Auto-generated method stub
        return false;
    }

    public boolean next() throws Exception
    {
        // TODO Auto-generated method stub
        return false;
    }

    public boolean previous() throws Exception
    {
        // TODO Auto-generated method stub
        return false;
    }

    public void setClosureMonitor( ClosureMonitor monitor )
    {
        // TODO Auto-generated method stub
        
    }

    public Iterator<Tuple<K, V>> iterator()
    {
        // TODO Auto-generated method stub
        return null;
    }

}
