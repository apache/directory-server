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


import org.apache.directory.shared.ldap.model.cursor.Tuple;
import org.apache.directory.shared.ldap.model.entry.Entry;


/**
 * An index id value pair which can optionally reference the indexed Entry
 * if one has already been loaded.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @param <V> The value stored in the Tuple, associated key for the Entry
 * @param <ID> The ID of the Entry
 */
public class ReverseIndexEntry<V, ID> extends AbstractIndexEntry<V, ID>
{
    /** The underlying Tuple */
    private final Tuple<ID, V> tuple = new Tuple<ID, V>();


    /**
     * Creates a ForwardIndexEntry instance
     */
    public ReverseIndexEntry()
    {
        super( null );
    }


    /**
     * Sets the Tuple value represented by this ReverseIndexEntry optionally
     * setting the Entry associated with the id if one was loaded from the
     * master table.
     *
     * @param tuple the tuple for the ReverseIndexEntry
     * @param obj the resusitated Entry that is indexed if any
     */
    public void setTuple( Tuple<ID, V> tuple, Entry entry )
    {
        setEntry( entry );
        this.tuple.setKey( tuple.getKey() );
        this.tuple.setValue( tuple.getValue() );
    }


    /**
     * {@inheritDoc}
     */
    public ID getId()
    {
        return tuple.getKey();
    }


    /**
     * {@inheritDoc}
     */
    public V getValue()
    {
        return tuple.getValue();
    }


    /**
     * {@inheritDoc}
     */
    public void setId( ID id )
    {
        tuple.setKey( id );
    }


    /**
     * {@inheritDoc}
     */
    public void setValue( V key )
    {
        tuple.setValue( key );
    }


    /**
     * {@inheritDoc}
     */
    public Tuple<?, ?> getTuple()
    {
        return tuple;
    }


    /**
     * {@inheritDoc}
     */
    public void clear()
    {
        super.clear();
        tuple.setKey( null );
        tuple.setValue( null );
    }


    /**
     * {@inheritDoc}
     */
    public void copy( IndexEntry<V, ID> entry )
    {
        setEntry( entry.getEntry() );
        tuple.setKey( entry.getId() );
        tuple.setValue( entry.getValue() );
    }


    /**
     * {@inheritDoc}
     */
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