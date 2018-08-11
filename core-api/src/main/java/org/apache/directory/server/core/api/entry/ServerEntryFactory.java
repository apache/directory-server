/*
 *   Licensed to the Apache Software Foundation (ASF) under one
 *   or more contributor license agreements.  See the NOTICE file
 *   distributed with this work for additional information
 *   regarding copyright ownership.  The ASF licenses this file
 *   to you under the Apache License, Version 2.0 (the
 *   "License"); you may not use this file except in compliance
 *   with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing,
 *   software distributed under the License is distributed on an
 *   "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *   KIND, either express or implied.  See the License for the
 *   specific language governing permissions and limitations
 *   under the License.
 *
 */
package org.apache.directory.server.core.api.entry;


import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.directory.api.ldap.model.name.Dn;


/**
 * A factory which produces ServerEntry objects.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public interface ServerEntryFactory
{
    /*
     * NOTE: 
     * 
     * May want to add more newEntry() overrides, some with variable arguments 
     * to create entries in one method call.
     */

    /**
     * Creates a new ServerEntry which has not yet been added to the 
     * directory.
     * 
     * @param dn The entry Dn
     * @return The created entry
     * @throws LdapException If the new entry cannot be created
     */
    Entry newEntry( Dn dn ) throws LdapException;
}
