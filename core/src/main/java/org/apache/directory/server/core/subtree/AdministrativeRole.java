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
package org.apache.directory.server.core.subtree;


/**
 * A list of all the Administrative Roles :
 * <ul>
 * <li>SubSchema administration role</li>
 * <li>Access Control administration role</li>
 * <li>Collective Attribute administration role</li>
 * <li>Triggers administration role</li>
 * </ul>
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public enum AdministrativeRole
{
    /** For collective attributes administration */
    COLLECTIVE_ADMIN_ROLE,

    /** For the subschema  administration */
    SUB_SCHEMA_ADMIN_ROLE,
    
    /** For the access control administration */
    ACCESS_CONTROL_ADMIN_ROLE,
    
    /** For triggers administration */
    TRIGGERS_ADMIN_ROLE;
}
