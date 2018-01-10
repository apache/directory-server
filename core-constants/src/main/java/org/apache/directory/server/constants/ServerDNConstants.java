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
 * DN constants used in the server.
 * Final reference -&gt; class shouldn't be extended
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public final class ServerDNConstants
{
    /**
     *  Ensures no construction of this class, also ensures there is no need for final keyword above
     *  (Implicit super constructor is not visible for default constructor),
     *  but is still self documenting.
     */
    private ServerDNConstants()
    {
    }

    /** The administrators group DN */
    public static final String ADMINISTRATORS_GROUP_DN = "cn=Administrators,ou=groups,ou=system";

    /** The system DN */
    public static final String SYSTEM_DN = "ou=system";

    /** the default user principal or DN */
    public static final String ADMIN_SYSTEM_DN = "uid=admin,ou=system";

    /** the normalized user principal or DN */
    public static final String ADMIN_SYSTEM_DN_NORMALIZED = "0.9.2342.19200300.100.1.1= admin ,2.5.4.11= system ";

    /** the DN for the global schema subentry */
    public static final String CN_SCHEMA_DN = "cn=schema";

    /** The DN for the gloval schema subentry normalized */
    public static final String CN_SCHEMA_DN_NORMALIZED = "2.5.4.3= schema ";

    /** the base dn under which all users reside */
    public static final String USERS_SYSTEM_DN = "ou=users,ou=system";

    /** The default change password base DN. */
    public static final String USER_EXAMPLE_COM_DN = "ou=users,dc=example,dc=com";

    /** the base dn under which all groups reside */
    public static final String GROUPS_SYSTEM_DN = "ou=groups,ou=system";

    /** the dn base of the system preference hierarchy */
    public static final String SYSPREFROOT_SYSTEM_DN = "prefNodeName=sysPrefRoot,ou=system";

    /** The ldifDile base which stores the name of the loaded ldif files */
    public static final String LDIF_FILES_DN = "ou=loadedLdifFiles,ou=configuration,ou=system";

    /** the config partition's dn */
    public static final String CONFIG_DN = "ou=config";

    /** The replication consumer container DN */
    public static final String REPL_CONSUMER_DN_STR = "ou=consumers,ou=system";

    /** the default DirectoryService DN */
    public static final String DEFAULT_DS_CONFIG_DN = "ads-directoryServiceId=default,ou=config";

    /** the replication consumer configuration DN */
    public static final String REPL_CONSUMER_CONFIG_DN = "ou=replConsumers,ads-serverId=ldapServer,ou=servers,"
        + DEFAULT_DS_CONFIG_DN;
}
