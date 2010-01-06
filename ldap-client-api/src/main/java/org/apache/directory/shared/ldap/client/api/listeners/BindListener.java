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
package org.apache.directory.shared.ldap.client.api.listeners;

import org.apache.directory.shared.ldap.client.api.LdapConnection;
import org.apache.directory.shared.ldap.client.api.exception.LdapException;
import org.apache.directory.shared.ldap.client.api.messages.BindResponse;

/**
 * A listener used for asynchronous bind handling. When wanting to handle
 * bind as a non-blocking operation, simply associate a BindListener
 * to the bind operation : When the bind as been completed this listener
 * will be called. 
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public interface BindListener extends OperationResponseListener
{
    /**
     * A callback method called when the bind operation completed.
     *
     * @param connection
     * @param BindResponseCodec The bind response
     */
    void bindCompleted( LdapConnection connection, BindResponse bindResponse ) throws LdapException;
}
