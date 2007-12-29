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
package org.apache.directory.server.kerberos.shared.store;


/**
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class KerberosAttribute
{
    // ------------------------------------------------------------------------
    // Krb5 Schema Attributes
    // ------------------------------------------------------------------------
    /** the krb5kdc schema principal name for a krb5KDCEntry */
    public static final String KRB5_PRINCIPAL_NAME_AT = "krb5PrincipalName";
    public static final String KRB5_PRINCIPAL_NAME_AT_OID = "1.3.6.1.4.1.5322.10.1.1";
    
    /** the krb5kdc schema key for a krb5KDCEntry */
    public static final String KRB5_KEY_AT = "krb5Key";
    public static final String KRB5_KEY_AT_OID = "1.3.6.1.4.1.5322.10.1.10";
    
    /** the krb5kdc schema key version identifier for a krb5KDCEntry */
    public static final String KRB5_KEY_VERSION_NUMBER_AT = "krb5KeyVersionNumber";
    public static final String KRB5_KEY_VERSION_NUMBER_AT_OID = "1.3.6.1.4.1.5322.10.1.2";
    
    /** the disabled boolean LDAP attribute for a Kerberos account */
    public static final String KRB5_ACCOUNT_DISABLED_AT = "krb5AccountDisabled";
    public static final String KRB5_ACCOUNT_DISABLED_AT_OID = "1.3.6.1.4.1.5322.10.1.13";
    
    /** the lockedout boolean LDAP attribute for a Kerberos account */
    public static final String KRB5_ACCOUNT_LOCKEDOUT_AT = "krb5AccountLockedOut";
    public static final String KRB5_ACCOUNT_LOCKEDOUT_AT_OID = "1.3.6.1.4.1.5322.10.1.14";
    
    /** the expiration time attribute LDAP attribute for a Kerberos account */
    public static final String KRB5_ACCOUNT_EXPIRATION_TIME_AT = "krb5AccountExpirationTime";
    public static final String KRB5_ACCOUNT_EXPIRATION_TIME_AT_OID = "1.3.6.1.4.1.5322.10.1.15";


    /** the Apache specific SAM type attribute */
    public static final String APACHE_SAM_TYPE_AT = "apacheSamType";
    public static final String APACHE_SAM_TYPE_AT_OID = "1.3.6.1.4.1.18060.0.4.1.2.9";
    
}
