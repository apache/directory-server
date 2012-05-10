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
    /** The instance name */
    String name() default "DefaultKrbServer";


    /** The transports to use, default to LDAP */
    CreateTransport[] transports() default
        {};


    /** The default kdc realm */
    String primaryRealm() default "EXAMPLE.COM";


    /** The default kdc service principal */
    String kdcPrincipal() default "krbtgt/EXAMPLE.COM@EXAMPLE.COM";


    /** The maximum ticket lifetime. */
    long maxTicketLifetime() default 60000 * 1440;


    /** The maximum renewable lifetime. */
    long maxRenewableLifetime() default 60000 * 10080;
}