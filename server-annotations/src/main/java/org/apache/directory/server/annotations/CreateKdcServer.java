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

import org.apache.directory.server.constants.ServerDNConstants;
import org.apache.directory.server.kerberos.shared.replay.ReplayCache;
import org.apache.directory.server.kerberos.shared.replay.ReplayCacheImpl;


/**
 * A annotation used to define a LdapServer configuration. Many elements can be configured :
 * <ul>
 * <li> The server ID (or name)</li>
 * <li>primary realm</li>
 * <li>service principal</li>
 * <li>maximum ticket lifetime</li>
 * <li>maximum renewable lifetime</li>
 * </ul>
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
@Documented
@Inherited
@Retention(RetentionPolicy.RUNTIME)
@Target(
    { ElementType.METHOD, ElementType.TYPE })
public @interface CreateKdcServer
{
    /** @return The instance name */
    String name() default "DefaultKrbServer";


    /** @return The transports to use, default to LDAP */
    CreateTransport[] transports() default
        {};


    /** @return The default kdc realm */
    String primaryRealm() default "EXAMPLE.COM";


    /** @return The default kdc service principal */
    String kdcPrincipal() default "krbtgt/EXAMPLE.COM@EXAMPLE.COM";


    /** @return The maximum ticket lifetime. */
    long maxTicketLifetime() default 60000 * 1440;


    /** @return The maximum renewable lifetime. */
    long maxRenewableLifetime() default 60000 * 10080;
    
    /** @return the change password server.
     * NOTE: this annotation is declared as an array cause there is no
     * way to define the default value as null for a value in annotation
     * 
     * Only the one declaration of changepassword server is enough and 
     * the first element alone is taken into consideration, rest of the
     * array elements will be ignored*/
    CreateChngPwdServer[] chngPwdServer() default {};
    
    /** @return the DN of the search base for finding users and services */
    String searchBaseDn() default ServerDNConstants.USER_EXAMPLE_COM_DN;

    /** @return the replay cache implementing class */
    Class<? extends ReplayCache> replayCacheType() default ReplayCacheImpl.class;
}