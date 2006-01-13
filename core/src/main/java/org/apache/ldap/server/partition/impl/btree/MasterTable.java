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
package org.apache.ldap.server.partition.impl.btree;


import java.math.BigInteger;

import javax.naming.NamingException;
import javax.naming.directory.Attributes;


/**
 * The master table used to store the Attributes of entries.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public interface MasterTable extends Table
{
    /** the name of the dbf file for this table */
    String DBF = "master";

    /** the sequence key - stores last sequence value in the admin table */
    String SEQPROP_KEY = "__sequence__";

    /**
     * Gets the Attributes of an entry from this MasterTable.
     *
     * @param id the BigInteger id of the entry to retrieve.
     * @return the Attributes of the entry with operational attributes and all.
     * @throws NamingException if there is a read error on the underlying Db.
     */
    Attributes get( BigInteger id ) throws NamingException;
    
    /**
     * Puts the Attributes of an entry into this master table at an index 
     * specified by id.  Used both to create new entries and update existing 
     * ones.
     *
     * @param entry the Attributes of entry w/ operational attributes
     * @param id the BigInteger id of the entry to put
     * @return the newly created entry's Attributes
     * @throws NamingException if there is a write error on the underlying Db.
     */
    Attributes put( Attributes entry, BigInteger id ) throws NamingException;
        
    /**
     * Deletes a entry from the master table at an index specified by id.
     *
     * @param id the BigInteger id of the entry to delete
     * @return the Attributes of the deleted entry
     * @throws NamingException if there is a write error on the underlying Db
     */
    Attributes delete( BigInteger id ) throws NamingException;

    /**
     * Get's the current id value from this master database's sequence without
     * affecting the seq.
     *
     * @return the current value.
     * @throws NamingException if the admin table storing sequences cannot be
     * read.
     */
    BigInteger getCurrentId() throws NamingException;
    
    /**
     * Get's the next value from this SequenceBDb.  This has the side-effect of
     * changing the current sequence values perminantly in memory and on disk.
     *
     * @return the current value incremented by one.
     * @throws NamingException if the admin table storing sequences cannot be
     * read and writen to.
     */
    BigInteger getNextId() throws NamingException;
    
    /**
     * Gets a persistant property stored in the admin table of this MasterTable.
     *
     * @param property the key of the property to get the value of
     * @return the value of the property
     * @throws NamingException when the underlying admin table cannot be read
     */
    String getProperty( String property ) throws NamingException;
        
    /**
     * Sets a persistant property stored in the admin table of this MasterTable.
     *
     * @param property the key of the property to set the value of
     * @param value the value of the property
     * @throws NamingException when the underlying admin table cannot be writen
     */
    void setProperty( String property, String value ) throws NamingException;
}