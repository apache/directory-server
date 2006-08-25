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
package org.apache.directory.server.core.schema;


import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.naming.NamingException;


/**
 * Object identifier registry.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public interface OidRegistry
{
    /**
     * Gets the object identifier for a common name or returns the argument
     * as-is if it is an object identifier.
     * 
     * @param name the name to lookup an OID for
     * @return the OID string associated with a name
     * @throws NamingException if name does not map to an OID
     */
    String getOid( String name ) throws NamingException;


    /**
     * Checks to see if an identifier, oid or name exists within this registry.
     *
     * @param id the oid or name to look for
     * @return true if the id exists false otherwise
     */
    boolean hasOid( String id );


    /**
     * Gets the primary name associated with an OID.  The primary name is the
     * first name specified for the OID.
     * 
     * @param oid the object identifier
     * @return the primary name
     * @throws NamingException if oid does not exist
     */
    String getPrimaryName( String oid ) throws NamingException;


    /**
     * Gets the names associated with an OID.  An OID is unique however it may 
     * have many names used to refer to it.  A good example is the cn and
     * commonName attribute names for OID 2.5.4.3.  Within a server one name 
     * within the set must be chosen as the primary name.  This is used to
     * name certain things within the server internally.  If there is more than
     * one name then the first name is taken to be the primary.
     * 
     * @param oid the OID for which we return the set of common names
     * @return a sorted set of names
     * @throws NamingException if oid does not exist
     */
    List getNameSet( String oid ) throws NamingException;


    /**
     * Lists all the OIDs within the registry.  This may be a really big list.
     * 
     * @return all the OIDs registered
     */
    Iterator list();


    /**
     * Adds an OID name pair to the registry.
     * 
     * @param name the name to associate with the OID
     * @param oid the OID to add or associate a new name with
     */
    void register( String name, String oid );


    /**
     * Get the map of all the oids by their name
     * @return The Map that contains all the oids
     */
    public Map getOidByName();


    /**
     * Get the map of all the oids by their name
     * @return The Map that contains all the oids
     */
    public Map getNameByOid();
}
