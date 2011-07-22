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


import org.apache.directory.shared.ldap.model.entry.Entry;
import org.apache.directory.shared.ldap.model.exception.LdapException;
import org.apache.directory.shared.ldap.model.name.Dn;
import org.apache.directory.shared.ldap.model.schema.SchemaManager;


public interface SchemaService
{

    /**
     * Tells if the given Dn is the schemaSubentry Dn
     * 
     * @param dn The Dn we want to check
     * @return <code>true</code> if the given Dn is the Schema subentry Dn
     * @throws LdapException If the given Dn is not valid
     */
    boolean isSchemaSubentry( Dn dn ) throws LdapException;


    /**
     * @return the schemaManager loaded from schemaPartition
     */
    SchemaManager getSchemaManager();


    SchemaPartition getSchemaPartition();

    /**
     * A seriously unsafe (unsynchronized) means to access the schemaSubentry.
     *
     * @return the schemaSubentry
     * @throws Exception if there is a failure to access schema timestamps
     */
    Entry getSubschemaEntryImmutable() throws LdapException;


    /**
     * A seriously unsafe (unsynchronized) means to access the schemaSubentry.
     *
     * @return the schemaSubentry
     * @throws Exception if there is a failure to access schema timestamps
     */
    Entry getSubschemaEntryCloned() throws LdapException;


    /**
     * Gets the schemaSubentry based on specific search id parameters which
     * include the special '*' and '+' operators.
     *
     * @param ids the ids of the attributes that should be returned from a search
     * @return the subschema entry with the ids provided
     * @throws Exception if there are failures during schema info access
     */
    Entry getSubschemaEntry( String[] ids ) throws LdapException;
}
