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


import java.util.Iterator;

import javax.naming.NamingException;

import org.apache.directory.shared.ldap.schema.NameForm;


/**
 * An NameForm registry service interface.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public interface NameFormRegistry
{
    /**
     * Registers a NameForm with this registry.
     * 
     * @param schema the name of the schema the NameForm is associated with
     * @param nameForm the nameForm to register
     * @throws NamingException if the NameForm is already registered or the
     * registration operation is not supported
     */
    void register( String schema, NameForm nameForm ) throws NamingException;


    /**
     * Looks up a nameForm by its unique Object Identifier or by name.
     * 
     * @param id the object identifier or name
     * @return the NameForm instance for the id
     * @throws NamingException if the NameForm does not exist
     */
    NameForm lookup( String id ) throws NamingException;


    /**
     * Gets the name of the schema this schema object is associated with.
     *
     * @param id the object identifier or the name
     * @return the schema name
     * @throws NamingException if the schema object does not exist
     */
    String getSchemaName( String id ) throws NamingException;


    /**
     * Checks to see if an nameForm exists.
     * 
     * @param id the object identifier or name
     * @return true if an nameForm definition exists for the oid, false
     * otherwise
     */
    boolean hasNameForm( String id );


    /**
     * Lists all the NameForms within this registry.
     *
     * @return an Iterator over all the NameForms within this registry
     */
    Iterator list();
}
