/*
 *   Copyright 2004 The Apache Software Foundation
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 */
package org.apache.directory.server.core.partition.impl.btree;


import java.math.BigInteger;

import javax.naming.directory.Attributes;


/**
 * An index key value pair based on a tuple which can optionally reference the
 * indexed entry if one has been resusitated.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class IndexRecord
{
    /** The underlying BTree Tuple */
    private final Tuple tuple = new Tuple();
    /** The referenced entry if resusitated */
    private Attributes entry = null;


    /**
     * Sets the key value tuple represented by this IndexRecord optionally 
     * setting the entry if one was resusitated from the master table.
     *
     * @param tuple the tuple for the IndexRecord
     * @param entry the resusitated entry if any
     */
    public void setTuple( Tuple tuple, Attributes entry )
    {
        this.tuple.setKey( tuple.getKey() );
        this.tuple.setValue( tuple.getValue() );
        this.entry = entry;
    }


    /**
     * Sets the key value tuple but swapping the key and the value and 
     * optionally adding the entry if one was resusitated.
     *
     * @param tuple the tuple for the IndexRecord
     * @param entry the resusitated entry if any
     */
    public void setSwapped( Tuple tuple, Attributes entry )
    {
        this.tuple.setKey( tuple.getValue() );
        this.tuple.setValue( tuple.getKey() );
        this.entry = entry;
    }


    /**
     * Gets the entry id for this IndexRecord. 
     *
     * @return the id of the entry indexed
     */
    public BigInteger getEntryId()
    {
        return ( BigInteger ) tuple.getValue();
    }


    /**
     * Gets the index key ( the attribute's value ) for this IndexRecord.
     *
     * @return the key of the entry indexed
     */
    public Object getIndexKey()
    {
        return tuple.getKey();
    }


    /**
     * Sets the entry id.
     *
     * @param id the id of the entry
     */
    public void setEntryId( BigInteger id )
    {
        tuple.setValue( id );
    }


    /**
     * Sets the index key.
     *
     * @param key the key of the IndexRecord
     */
    public void setIndexKey( Object key )
    {
        tuple.setKey( key );
    }


    /**
     * Gets the entry of this IndexRecord if one was resusitated form the master
     * table.
     * 
     * @return the entry's attributes
     */
    public Attributes getAttributes()
    {
        if ( entry == null )
        {
            return null;
        }

        return ( Attributes ) entry.clone();
    }


    /**
     * Sets the entry's attributes.
     * 
     * @param entry the entry's attributes
     */
    public void setAttributes( Attributes entry )
    {
        this.entry = entry;
    }


    /**
     * Clears the tuple key, the tuple value and the entry fields.
     */
    public void clear()
    {
        entry = null;
        tuple.setKey( null );
        tuple.setValue( null );
    }


    /**
     * Utility method used to copy the contents of one IndexRecord into this
     * IndexRecord.
     * 
     * @param record the record whose contents we copy
     */
    public void copy( IndexRecord record )
    {
        entry = record.getAttributes();
        tuple.setKey( record.getIndexKey() );
        tuple.setValue( record.getEntryId() );
    }
}
