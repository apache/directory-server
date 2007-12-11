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
    /** The administrators group DN */
    public static final String ADMINISTRATORS_GROUP_DN = "cn=Administrators,ou=groups,ou=system";

    /** The system DN */
    public static final String SYSTEM_DN = "ou=system";
    
    /** the default user principal or DN */
    public final static String ADMIN_SYSTEM_DN = "uid=admin,ou=system";
    
    /** the normalized user principal or DN */
    public final static String ADMIN_SYSTEM_DN_NORMALIZED = "0.9.2342.19200300.100.1.1=admin,2.5.4.11=system";

    /** the DN for the global schema subentry */
    public final static String CN_SCHEMA_DN = "cn=schema";
    
    /** The DN for the gloval schema subentry normalized */
    public static final String CN_SCHEMA_DN_NORMALIZED = "2.5.4.3=schema";
   
    /** the DN for the global schema subentry */
    public final static String OU_SCHEMA_DN = "ou=schema";
    
    /** the base dn under which all users reside */
    public final static String USERS_SYSTEM_DN = "ou=users,ou=system";
    
    /** The default change password base DN. */
    public final static String USER_EXAMPLE_COM_DN = "ou=users,dc=example,dc=com";
    
    
    /** the base dn under which all groups reside */
    public final static String GROUPS_SYSTEM_DN = "ou=groups,ou=system";
    
    /** the dn base of the system preference hierarchy */
    public final static String SYSPREFROOT_SYSTEM_DN = "prefNodeName=sysPrefRoot,ou=system";
}
