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
package org.apache.directory.server.constants;

/**
 * A utility class where we declare all the statically defined DN used in the server.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev:$
 */
public class ServerDNConstants 
{
	public static final String ADMINISTRATORS_GROUP_DN = "cn=Administrators,ou=groups,ou=system";

    public static final String SYSTEM_DN = "ou=system";
    
    /** the default user principal or DN */
    public static final String ADMIN_SYSTEM_DN = "uid=admin,ou=system";
    
    /** the DN for the global schema subentry */
    public static final String SCHEMA_DN = "cn=schema";

    /** the normalized DN for the global schema subentry */
    public static final String SCHEMA_DN_NORMALIZED = "2.5.4.3=schema";

    /** the normalized user principal or DN */
    public static final String ADMIN_SYSTEM_DN_NORMALIZED = "0.9.2342.19200300.100.1.1=admin,2.5.4.11=system";
    
    /** the base dn under which all users reside */
    public static final String USERS_SYSTEM_DN = "ou=users,ou=system";
    
    /** the base dn under which all groups reside */
    public static final String GROUPS_SYSTEM_DN = "ou=groups,ou=system";
}
