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
package org.apache.directory.server.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.apache.directory.ldap.client.api.NoVerificationTrustManager;
import org.apache.directory.shared.ldap.model.message.AliasDerefMode;
import org.apache.directory.shared.ldap.model.message.SearchScope;

/**
 * A annotation used to define a replication consumer configuration. Many elements can be configured :
 * <ul>
 *   <li>remoteHost : the remote server's name, defaults to 'localhost'</li>
 *   <li>remotePort : the remote server's LDAP port, defaults to 389</li>
 *   <li>replUserDn : The replication User's DN</li>
 *   <li>replUserPassword : The replication User's password</li>
 *   <li>refreshNPersist : the replication mode, defaults to 'true'</li>
 *   <li>refreshInterval : the interval between replications when in refreshOnly mode, defaults to 60s</li>
 *   <li>baseDn : the base from which to fetch entries on the remote server</li>
 *   <li>filter : the filter to select entries,defaults to (ObjectClass=*)</li>
 *   <li>attributes : the list of attributes to replicate, defaults to all</li>
 *   <li>searchSizeLimit : the maximum number of entries to fetch, defaults to no limit</li>
 *   <li>searchTimeout : the maximum delay to wait for entries, defaults to no limit</li>
 *   <li>searchScope : the scope, defaults to SUBTREE</li>
 *   <li>aliasDerefMode : set the aliss derefence policy, defaults to NEVER </li>
 *   <li>cookie : the replication cookie</li>
 *   <li>replicaId : the replica identifier</li>
 *   <li>configEntryDn : the configuration entry's DN</li>
 *   <li>chaseReferrals : tells if we chase referrals, defaults to false</li>
 *   <li>useTls : the connection uses TLS, defaults to true</li>
 *   <li>strictCertVerification : strictly verify the certificate, defaults to true</li>
 *   <li>trustManager : the trustManager to use, defaults to @link{NoVerificationTrustManager}</li>
 * </ul>
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
@Documented
@Inherited
@Retention ( RetentionPolicy.RUNTIME )
@Target ( { ElementType.METHOD, ElementType.TYPE } )
public @interface CreateConsumer
{
    /** host name of the syncrepl remote server, default value is localhost */
    String remoteHost() default "localhost";

    /** port number of the syncrepl provider server, default is 389 */
    int remotePort() default 389;

    /** replication user's Dn */
    String replUserDn();

    /** password for binding with replication user dn */
    String replUserPassword();

    /** flag to represent refresh and persist or refresh only mode, defaults to true */
    boolean refreshNPersist() default true;

    /** time interval for successive sync requests, default is 60 seconds */
    long refreshInterval() default 60 * 1000;

    /** the base Dn whose content will be searched for replicating */
    String baseDn();

    /** the ldap filter for fetching the entries, default value is (objectClass=*) */
    String filter() default "(objectClass=*)";

    /** names of attributes to be replicated, default value is all user attributes */
    String[] attributes() default ""; 

    /** the maximum number of search results to be fetched
     * default value is 0 (i.e no limit) */
    int searchSizeLimit() default 0;

    /** the timeout value to be used while doing a search 
     * default value is 0 (i.e no limit)*/
    int searchTimeout() default 0;

    /** the search scope, default is sub tree level */
    SearchScope searchScope() default SearchScope.SUBTREE;

    /** alias dereferencing mode, default is set to 'never deref aliases' */
    AliasDerefMode aliasDerefMode() default AliasDerefMode.NEVER_DEREF_ALIASES;

    /** the replica's id */
    int replicaId();

    /** The configuration entry DN */
    String configEntryDn() default "";

    /** flag to indicate whether to chase referrals or not, default is false hence passes ManageDsaITControl with syncsearch request*/
    boolean chaseReferrals() default false;

    /** flag to indicate the use of TLS, default is true */
    boolean useTls() default true;

    /** flag to indicate the use of strict certificate verification, default is true */
    boolean strictCertVerification() default true;

    /** the X509 certificate trust manager used, default value set to {@link NoVerificationTrustManager} */
    Class<?> trustManager() default NoVerificationTrustManager.class;
}
