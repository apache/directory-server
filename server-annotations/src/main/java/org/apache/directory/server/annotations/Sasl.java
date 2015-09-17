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

import org.apache.directory.server.ldap.handlers.sasl.SimpleMechanismHandler;
import org.apache.directory.server.ldap.handlers.sasl.cramMD5.CramMd5MechanismHandler;
import org.apache.directory.server.ldap.handlers.sasl.digestMD5.DigestMd5MechanismHandler;
import org.apache.directory.server.ldap.handlers.sasl.gssapi.GssapiMechanismHandler;
import org.apache.directory.server.ldap.handlers.sasl.ntlm.NtlmMechanismHandler;


/**
 * A annotation used to define the SASL configuration. Many elements can be configured :
 * <ul>
 * <li> The host</li>
 * <li> The principal</li>
 * <li> The SASL Qop</li>
 * <li> The SASL Realms</li>
 * <li> The SASL mechanisms</li>
 * </ul>
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
@Documented
@Inherited
@Retention(RetentionPolicy.RUNTIME)
@Target(
    { ElementType.METHOD, ElementType.TYPE })
public @interface Sasl
{
    /** The SASL host, default to "" */
    String host() default "";


    /** The principal */
    String principal();


    /** The SASL QOP list */
    String[] qop() default
        { "auth", "auth-int", "auth-conf" };


    /** The SASL realms */
    String[] realms() default
        {};


    /** The mechanism handlers.*/
    Class<?>[] mechanismHandler() default
        {
            SimpleMechanismHandler.class,
            CramMd5MechanismHandler.class,
            DigestMd5MechanismHandler.class,
            GssapiMechanismHandler.class,
            NtlmMechanismHandler.class
    };
}