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

import org.apache.directory.server.factory.DefaultLdapServerFactory;


/**
 * A annotation used to define a LdapServer configuration. Many elements can be configured :
 * <ul>
 * <li> The server ID (or name)</li>
 * <li> The max time limit</li>
 * <li> the max size limit</li>
 * <li> Should it allow anonymous access</li>
 * <li> The keyStore file</li>
 * <li> The certificate password</li>
 * </ul>
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
@Documented
@Inherited
@Retention(RetentionPolicy.RUNTIME)
@Target(
    { ElementType.METHOD, ElementType.TYPE })
public @interface CreateLdapServer
{
    /** @return The instance name */
    String name() default "DefaultLdapServer";


    /** @return The transports to use, default to LDAP */
    CreateTransport[] transports() default
        {};


    /** @return The LdapServer factory */
    Class<?> factory() default DefaultLdapServerFactory.class;


    /** @return The maximum size limit.*/
    long maxSizeLimit() default 1000;


    /** @return The maximum time limit. */
    int maxTimeLimit() default 1000;


    /** @return Tells if anonymous access are allowed or not. */
    boolean allowAnonymousAccess() default false;


    /** @return The external keyStore file to use, default to the empty string */
    String keyStore() default "";


    /** @return The certificate password in base64, default to the empty string */
    String certificatePassword() default "";


    /** @return name of the classes implementing extended operations */
    Class<?>[] extendedOpHandlers() default
        {};


    /** @return supported set of SASL mechanisms */
    SaslMechanism[] saslMechanisms() default
        {};


    /** @return NTLM provider class, default value is a invalid class */
    Class<?> ntlmProvider() default Object.class;


    /** @return The name of this host, validated during SASL negotiation. */
    String saslHost() default "ldap.example.com";
    
    
    /** @return The name of this host, validated during SASL negotiation. */
    String[] saslRealms() default {"example.com"};
    
    
    /** @return The service principal, used by GSSAPI. */
    String saslPrincipal() default "ldap/ldap.example.com@EXAMPLE.COM";

    /**
     * The X509 certificate trust managers used
     *
     *  @return The trust manager classes
     */
    Class<?>[] trustManagers() default {};
}