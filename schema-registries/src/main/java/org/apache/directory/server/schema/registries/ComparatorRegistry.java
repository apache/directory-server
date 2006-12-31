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
package org.apache.directory.server.schema.registries;


import java.util.Comparator;
import java.util.Iterator;

import javax.naming.NamingException;


/**
 * Comparator registry component's service interface.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public interface ComparatorRegistry
{
    /**
     * Gets the name of the schema this schema object is associated with.
     *
     * @param oid the object identifier
     * @return the schema name
     * @throws NamingException if the schema object does not exist 
     */
    String getSchemaName( String oid ) throws NamingException;


    /**
     * Registers a Comparator with this registry.
     * 
     * @param schema the name of the schema the comparator is associated with
     * @param oid the object identifier
     * @param comparator the Comparator to register
     * @throws NamingException if the Comparator is already registered or the 
     *      registration operation is not supported
     */
    void register( String schema, String oid, Comparator comparator ) throws NamingException;


    /**
     * Looks up a Comparator by its unique Object Identifier.
     * 
     * @param oid the object identifier
     * @return the Comparator for the oid
     * @throws NamingException if there is a backing store failure or the 
     *      Comparator does not exist.
     */
    Comparator lookup( String oid ) throws NamingException;


    /**
     * Checks to see if a Comparator exists.  Backing store failures simply 
     * return false.
     * 
     * @param oid the object identifier
     * @return true if a Comparator definition exists for the oid, false 
     *      otherwise
     */
    boolean hasComparator( String oid );


    /**
     * Iterates over the numeric OID strings of this registry.
     * 
     * @return Iterator of numeric OID strings 
     */
    Iterator<String> oidIterator();
}
