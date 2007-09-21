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
package org.apache.directory.server.core.changelog;

import javax.naming.NamingException;

import org.apache.directory.server.core.authn.LdapPrincipal;
import org.apache.directory.shared.ldap.ldif.Entry;


/**
 * A store for change events on the directory which exposes methods for 
 * managing, querying and in general performing legal operations on the log.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public interface ChangeLogStore 
{
    /**
     * Get's the current revision of the server (a.k.a. the HEAD revision).
     *
     * @return the current revision of the server
     */
    long getCurrentRevision();
    
    /**
     * Records a change as a forward LDIF, a reverse change to revert the change and
     * the authorized principal triggering the revertable change event.
     *
     * @param principal the authorized LDAP principal triggering the change
     * @param forward LDIF of the change going to the next state
     * @param reverse LDIF (anti-operation): the change required to revert this change
     * @return the commit id or revision representing the change within the log
     * @throws NamingException
     */
    long log( LdapPrincipal principal, Entry forward, Entry reverse ) throws NamingException;
}
