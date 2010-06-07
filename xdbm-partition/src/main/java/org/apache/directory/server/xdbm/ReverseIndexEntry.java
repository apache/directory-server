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

import org.apache.directory.shared.ldap.cursor.Tuple;


/**
 * An index id value pair which can optionally reference the indexed obj
 * if one has already been loaded.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class ReverseIndexEntry<V, O, ID> implements IndexEntry<V, O, ID>
{
    /** The underlying Tuple */
    private final Tuple<ID, V> tuple = new Tuple<ID, V>();

    /** The indexed object if loaded from the store */
    private O obj;


    /**
     * Sets the Tuple value represented by this ReverseIndexEntry optionally
     * setting the obj associated with the id if one was loaded from the
     * master table.
     *
     * @param tuple the tuple for the ReverseIndexEntry
     * @param obj the resusitated object that is indexed if any
     */
    public void setTuple( Tuple<ID, V> tuple, O obj )
    {
        this.tuple.setKey( tuple.getKey() );
        this.tuple.setValue( tuple.getValue() );
        this.obj = obj;
    }


    public ID getId()
    {
        return tuple.getKey();
    }


    public V getValue()
    {
        return tuple.getValue();
    }


    public void setId( ID id )
    {
        tuple.setKey( id );
    }


    public void setValue( V key )
    {
        tuple.setValue( key );
    }


    public O getObject()
    {
        return obj;
    }


    public void setObject( O obj )
    {
        this.obj = obj;
    }


    public Tuple<ID, V> getTuple()
    {
        return tuple;
    }


    public void clear()
    {
        obj = null;
        tuple.setKey( null );
        tuple.setValue( null );
    }


    public void copy( IndexEntry<V, O, ID> entry )
    {
        this.obj = entry.getObject();
        tuple.setKey( entry.getId() );
        tuple.setValue( entry.getValue() );
    }


    public String toString()
    {
        StringBuilder buf = new StringBuilder();
        buf.append( "ReverseIndexEntry[ " );
        buf.append( tuple.getValue() );
        buf.append( ", " );
        buf.append( tuple.getKey() );
        buf.append( " ]" );
        return buf.toString();
    }
}