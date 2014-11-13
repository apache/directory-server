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
package org.apache.directory.server.core.api.administrative;


import org.apache.directory.api.ldap.model.name.Dn;
import org.apache.directory.api.ldap.model.subtree.AdministrativeRole;


/**
 * An abstract class used for TriggerExecution APs
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public abstract class TriggerExecutionAdministrativePoint extends AbstractAdministrativePoint
{

    /**
     * Create an instance of TriggerExecutionAdministrativePoint
     *
     * @param dn The AdministrativePoint Dn
     * @param uuid The AdministrativePoint UUID
     * @param role The AdministrativePoint role
     */
    protected TriggerExecutionAdministrativePoint( Dn dn, String uuid, AdministrativeRole role )
    {
        super( dn, uuid, role );
    }
}
