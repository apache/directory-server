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


/**
 * An index id value pair based on a Tuple which can optionally reference the
 * indexed Entry if one has already been loaded.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @param <V> The key stored in the Tuple, associated key for the object
 * @param <ID> The ID of the object
 */
public class ForwardIndexEntry<K, ID> extends AbstractIndexEntry<K, ID>
{
    /** The underlying Tuple */
    private final Tuple<K, ID> tuple = new Tuple<K, ID>();


    /**
     * Creates a ForwardIndexEntry instance
     */
    public ForwardIndexEntry()
    {
        super();
    }


    /**
     * Sets the key value tuple represented by this ForwardIndexEntry, after having 
     * reset the IndexEntry content (the Entry will now be null)
     *
     * @param tuple the tuple for the ForwardIndexEntry
     */
    public void setTuple( Tuple<K, ID> tuple )
    {
        // Clear the entry
        super.clear();

        // And inject the tuple key and value 
        this.tuple.setKey( tuple.getKey() );
        this.tuple.setValue( tuple.getValue() );
    }


    /**
     * {@inheritDoc}
     */
    public ID getId()
    {
        return tuple.getValue();
    }


    /**
     * {@inheritDoc}
     */
    public K getKey()
    {
        return tuple.getKey();
    }


    /**
     * {@inheritDoc}
     */
    public void setId( ID id )
    {
        tuple.setValue( id );
    }


    /**
     * {@inheritDoc}
     */
    public void setKey( K value )
    {
        tuple.setKey( value );
    }


    /**
     * {@inheritDoc}
     */
    public Tuple<K, ID> getTuple()
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
    public void copy( IndexEntry<K, ID> entry )
    {
        super.copy( entry );
        tuple.setKey( entry.getKey() );
        tuple.setValue( entry.getId() );
    }


    public int hashCode()
    {
        if ( getId() == null )
        {
            return 0;
        }

        return getId().hashCode();
    }


    public boolean equals( IndexEntry<K, ID> that )
    {
        if ( that == this )
        {
            return true;
        }

        if ( !( that instanceof ForwardIndexEntry ) )
        {
            return false;
        }

        ForwardIndexEntry<K, ID> thatIndexEntry = ( ForwardIndexEntry<K, ID> ) that;

        if ( thatIndexEntry.getId() == null )
        {
            return getId() == null;
        }

        if ( thatIndexEntry.getId().equals( getId() ) )
        {
            return true;
        }

        return false;
    }


    /**
     * {@inheritDoc}
     */
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
