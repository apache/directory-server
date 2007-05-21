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
package org.apache.directory.server.core.partition.impl.btree.jdbm;


import javax.naming.NamingException;
import javax.naming.directory.Attributes;

import jdbm.RecordManager;
import jdbm.helper.LongSerializer;
import jdbm.helper.StringComparator;

import org.apache.directory.server.core.partition.impl.btree.MasterTable;
import org.apache.directory.server.schema.SerializableComparator;


/**
 * The master table used to store the Attributes of entries.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class JdbmMasterTable extends JdbmTable implements MasterTable
{
    private static final StringComparator STRCOMP = new StringComparator();
    private static final SerializableComparator LONG_COMPARATOR = new SerializableComparator(
        "1.3.6.1.4.1.18060.0.4.1.1.2" )
    {
        private static final long serialVersionUID = 4048791282048841016L;


        public int compare( Object o1, Object o2 )
        {
        	try
        	{
	        	long thisVal = (Long)o1;
	        	long anotherVal = (Long)o2;
	        	return ( thisVal < anotherVal ? -1 : ( thisVal == anotherVal ? 0 : 1 ) );
        	}
        	catch ( NullPointerException npe )
        	{
    	        if ( o1 == null )
    	        {
    	            throw new IllegalArgumentException( "Argument 'obj1' is null" );
    	        }
    	        else
    	        {
    	            throw new IllegalArgumentException( "Argument 'obj2' is null" );
    	        }
        	}
        }
    };
    private static final SerializableComparator STRING_COMPARATOR = new SerializableComparator(
        "1.3.6.1.4.1.18060.0.4.1.1.3" )
    {
        private static final long serialVersionUID = 3258689922792961845L;


        public int compare( Object o1, Object o2 )
        {
            return STRCOMP.compare( o1, o2 );
        }
    };
    /**  */
    private JdbmTable adminTbl = null;


    /**
     * Creates the master entry table using a Berkeley Db for the backing store.
     *
     * @param recMan the jdbm record manager
     * @throws NamingException if there is an error opening the Db file.
     */
    public JdbmMasterTable(RecordManager recMan) throws NamingException
    {
        super( DBF, recMan, LONG_COMPARATOR, LongSerializer.INSTANCE, new AttributesSerializer() );
        adminTbl = new JdbmTable( "admin", recMan, STRING_COMPARATOR, null, null );
        String seqValue = ( String ) adminTbl.get( SEQPROP_KEY );

        if ( null == seqValue )
        {
            adminTbl.put( SEQPROP_KEY, "0" );
        }
    }


    /**
     * Gets the Attributes of an entry from this MasterTable.
     *
     * @param id the BigInteger id of the entry to retrieve.
     * @return the Attributes of the entry with operational attributes and all.
     * @throws NamingException if there is a read error on the underlying Db.
     */
    public Attributes get( Object id ) throws NamingException
    {
        return ( Attributes ) super.get( id );
    }


    /**
     * Puts the Attributes of an entry into this master table at an index 
     * specified by id.  Used both to create new entries and update existing 
     * ones.
     *
     * @param entry the Attributes of entry w/ operational attributes
     * @param id the BigInteger id of the entry to put
     * @return the Attributes of the entry put
     * @throws NamingException if there is a write error on the underlying Db.
     */
    public Attributes put( Attributes entry, Object id ) throws NamingException
    {
        return ( Attributes ) super.put( id, entry );
    }


    /**
     * Deletes a entry from the master table at an index specified by id.
     *
     * @param id the BigInteger id of the entry to delete
     * @return the Attributes of the deleted entry
     * @throws NamingException if there is a write error on the underlying Db
     */
    public Attributes delete( Object id ) throws NamingException
    {
        return ( Attributes ) super.remove( id );
    }


    /**
     * Get's the current id value from this master database's sequence without
     * affecting the seq.
     *
     * @return the current value.
     * @throws NamingException if the admin table storing sequences cannot be
     * read.
     */
    public Long getCurrentId() throws NamingException
    {
        Long id = null;

        synchronized ( adminTbl )
        {
            id = new Long( ( String ) adminTbl.get( SEQPROP_KEY ) );

            if ( null == id )
            {
                adminTbl.put( SEQPROP_KEY, "0" );
                id = 0L;
            }
        }

        return id;
    }


    /**
     * Get's the next value from this SequenceBDb.  This has the side-effect of
     * changing the current sequence values perminantly in memory and on disk.
     * Master table sequence begins at BigInteger.ONE.  The BigInteger.ZERO is
     * used for the fictitious parent of the suffix root entry.
     *
     * @return the current value incremented by one.
     * @throws NamingException if the admin table storing sequences cannot be
     * read and writen to.
     */
    public Long getNextId() throws NamingException
    {
        Long lastVal = null;
        Long nextVal = null;

        synchronized ( adminTbl )
        {
            lastVal = new Long( ( String ) adminTbl.get( SEQPROP_KEY ) );

            if ( null == lastVal )
            {
                adminTbl.put( SEQPROP_KEY, "1" );
                return 1L;
            }
            else
            {
                nextVal = lastVal + 1L;
                adminTbl.put( SEQPROP_KEY, nextVal.toString() );
            }
        }

        return nextVal;
    }


    /**
     * Gets a persistant property stored in the admin table of this MasterTable.
     *
     * @param property the key of the property to get the value of
     * @return the value of the property
     * @throws NamingException when the underlying admin table cannot be read
     */
    public String getProperty( String property ) throws NamingException
    {
        synchronized ( adminTbl )
        {
            return ( String ) adminTbl.get( property );
        }
    }


    /**
     * Sets a persistant property stored in the admin table of this MasterTable.
     *
     * @param property the key of the property to set the value of
     * @param value the value of the property
     * @throws NamingException when the underlying admin table cannot be writen
     */
    public void setProperty( String property, String value ) throws NamingException
    {
        synchronized ( adminTbl )
        {
            adminTbl.put( property, value );
        }
    }
}
