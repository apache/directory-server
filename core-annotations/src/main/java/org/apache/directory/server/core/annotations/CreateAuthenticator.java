/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.directory.server.core.annotations;


import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.apache.directory.server.core.authn.AnonymousAuthenticator;
import org.apache.directory.server.core.authn.Authenticator;


/**
 * An authenticator creation
 * a name and a suffix, plus some other characteristics. Here is an example :
 * <pre>
 * @CreateAuthenticator(
 *     type = "org.apache.directory.server.core.authn.StrongAuthenticator"
 *     )
 * )
 * </pre>
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(
    { ElementType.METHOD, ElementType.TYPE })
public @interface CreateAuthenticator
{
    /** The authenticator implementation class */
    Class<? extends Authenticator> type() default AnonymousAuthenticator.class;


    /** Delegate host, use for testing DelegatingAuthenticator */
    String delegateHost() default "localhost";


    /** Delegate port, use for testing DelegatingAuthenticator */
    int delegatePort() default -1;


    /** The base DN from which we will delegate authentication */
    String delegateBaseDn() default "";


    /** Tells if we use SSL to connect */
    boolean delegateSsl() default false;


    /** Tells if we use startTls to connect */
    boolean delegateTls() default true;


    /** The SSL TrustManager FQCN */
    String delegateSslTrustManagerFQCN() default "org.apache.directory.ldap.client.api.NoVerificationTrustManager";


    /** The startTls TrustManager FQCN */
    String delegateTlsTrustManagerFQCN() default "org.apache.directory.ldap.client.api.NoVerificationTrustManager";
}
