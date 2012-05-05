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

package org.apache.directory.server.ldap.handlers.bind;


/**
 * SASL Constants used to store informations releated to the Challenge/response
 * exchange during the SASL negociation.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class SaslConstants
{
    /**
     * A key constant for storing the SASL Server in the session.
     */
    public static final String SASL_SERVER = "saslServer";

    /**
     * A key constant for storing the SASL host in the session
     */
    public static final String SASL_HOST = "host";

    /**
     * A key constant used when creating a SaslServer
     */
    public static final String LDAP_PROTOCOL = "ldap";

    /**
     * A key constant for storing the place where we are to search for user's pasword
     */
    public static final String SASL_USER_BASE_DN = "userBaseDn";

    /**
     * A key constant for storing the current mechanism
     */
    public static final String SASL_MECH = "saslMech";

    /**
     * A key constant for storing the authenticated user
     */
    public static final String SASL_AUTHENT_USER = "saslAuthentUser";

    /**
     * A key constant for storing the evaluated credentials
     */
    public static final String SASL_CREDS = "saslCreds";

    /**
     * A key constant for storing the Quality Of Protection
     */
    public static final String SASL_QOP = "saslQop";

    /**
     * A key constant for storing the realm
     */
    public static final String SASL_REALM = "saslRealm";

    /**
     * A key constant representing the SASL properties 
     */
    public static final String SASL_PROPS = "saslProps";

    /**
     * A key constant representing the SASL mechanism handler
     */
    public static final String SASL_MECH_HANDLER = "saslmechHandler";

    /**
     * A key constant representing the SASL IoFilter 
     */
    public static final String SASL_FILTER = "SASL_FILTER";
}
