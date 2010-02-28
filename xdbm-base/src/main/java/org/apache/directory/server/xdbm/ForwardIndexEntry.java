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


/**
 * An index id value pair based on a Tuple which can optionally reference the
 * indexed obj if one has already been loaded.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class ForwardIndexEntry<V, O> implements IndexEntry<V, O>
{
    /** The underlying Tuple */
    private final Tuple<V, Long> tuple = new Tuple<V, Long>();

    /** The referenced obj if loaded from the store */
    private O obj;


    /**
     * Sets the key value tuple represented by this ForwardIndexEntry optionally
     * setting the obj associated with the id if one was loaded from the
     * master table.
     *
     * @param tuple the tuple for the ForwardIndexEntry
     * @param entry the resusitated obj if any
     */
    public void setTuple( Tuple<V, Long> tuple, O entry )
    {
        this.tuple.setKey( tuple.getKey() );
        this.tuple.setValue( tuple.getValue() );
        this.obj = entry;
    }


    public Long getId()
    {
        return tuple.getValue();
    }


    public V getValue()
    {
        return tuple.getKey();
    }


    public void setId( Long id )
    {
        tuple.setValue( id );
    }


    public void setValue( V value )
    {
        tuple.setKey( value );
    }


    public O getObject()
    {
        if ( obj == null )
        {
            return null;
        }

        return obj;
    }


    public void setObject( O obj )
    {
        this.obj = obj;
    }


    public Tuple getTuple()
    {
        return tuple;
    }


    public void clear()
    {
        obj = null;
        tuple.setKey( null );
        tuple.setValue( null );
    }


    public void copy( IndexEntry<V, O> entry )
    {
        this.obj = entry.getObject();
        tuple.setKey( entry.getValue() );
        tuple.setValue( entry.getId() );
    }


    public String toString()
    {
        StringBuilder buf = new StringBuilder();
        buf.append( "ForwardIndexEntry[ " );
        buf.append( tuple.getKey() );
        buf.append( ", " );
        buf.append( tuple.getValue() );
        buf.append( " ]" );
        return buf.toString();
    }
}
