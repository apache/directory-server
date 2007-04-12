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

package org.apache.directory.shared.ldap.constants;

/**
 * This class contains all the Ldap specific properties described in the JNDI API.
 * See http://java.sun.com/j2se/1.5.0/docs/guide/jndi/jndi-ldap-gl.html
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class JndiPropertyConstants
{
    // Pure JNDI properties
    public static final String JNDI_BATCHSIZE = "java.naming.batchsize";
    public static final String JNDI_FACTORY_CONTROL = "java.naming.factory.control";
    public static final String JNDI_FACTORY_INITIAL = "java.naming.factory.initial";
    public static final String JNDI_FACTORY_OBJECT = "java.naming.factory.object";
    public static final String JNDI_FACTORY_STATE = "java.naming.factory.state";
    public static final String JNDI_LANGUAGE = "java.naming.language";
    public static final String JNDI_PROVIDER_URL = "java.naming.provider.url";
    public static final String JNDI_REFERRAL = "java.naming.referral";
    public static final String JNDI_SECURITY_AUTHENTICATION = "java.naming.security.authentication";
    public static final String JNDI_SECURITY_CREDENTIALS = "java.naming.security.credentials";
    public static final String JNDI_SECURITY_PRINCIPAL = "java.naming.security.principal";
    public static final String JNDI_SECURITY_PROTOCOL = "java.naming.security.protocol";
    
    // Ldap specific properties
    public static final String JNDI_LDAP_ATTRIBUTES_BINARY = "java.naming.ldap.attributes.binary";
    public static final String JNDI_LDAP_CONTROL_CONNECT = "java.naming.ldap.control.connect";
    public static final String JNDI_LDAP_DELETE_RDN = "java.naming.ldap.deleteRDN";
    public static final String JNDI_LDAP_DAP_DEREF_ALIASES = "java.naming.ldap.derefAliases";
    public static final String JNDI_FACTORY_SOCKET = "java.naming.ldap.factory.socket";
    public static final String JNDI_LDAP_REF_SEPARATOR = "java.naming.ldap.ref.separator";
    public static final String JNDI_LDAP_REFERRAL_LIMIT = "java.naming.ldap.referral.limit";
    public static final String JNDI_LDAP_TYPES_ONLY = "java.naming.ldap.typesOnly";
    public static final String JNDI_LDAP_VERSION = "java.naming.ldap.version";
    
    // SASL properties
    public static final String JNDI_SASL_AUTHORIZATION_ID = "java.naming.security.sasl.authorizationId";
    public static final String JNDI_SASL_REALM = "java.naming.security.sasl.realm";
    public static final String JNDI_SASL_CALLBACK = "java.naming.security.sasl.callback";
    public static final String JNDI_SASL_QOP = "javax.security.sasl.qop";
    public static final String JNDI_SASL_STRENGTH = "javax.security.sasl.strength";
    public static final String JNDI_SASL_MAX_BUFFER = "javax.security.sasl.maxbuffer";
    public static final String JNDI_SASL_AUTHENTICATION = "javax.security.sasl.server.authentication";
    public static final String JNDI_SASL_POLICY_FORWARD = "javax.security.sasl.policy.forward";
    public static final String JNDI_SASL_POLICY_CREDENTIALS = "javax.security.sasl.policy.credentials";
    public static final String JNDI_SASL_POLICY_NO_PLAIN_TEXT = "javax.security.sasl.policy.noplaintext";
    public static final String JNDI_SASL_POLICY_NO_ACTIVE = "javax.security.sasl.policy.noactive";
    public static final String JNDI_SASL_POLICY_NO_DICTIONARY = "javax.security.sasl.policy.nodictionary";
    public static final String JNDI_SASL_POLICY_NO_ANONYMOUS = "javax.security.sasl.policy.noanonymous";
}
