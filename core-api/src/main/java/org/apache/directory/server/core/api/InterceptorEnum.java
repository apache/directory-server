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
package org.apache.directory.server.core.api;


/**
 * The list of mandatory interceptors we use in the server.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public enum InterceptorEnum
{
    ACI_AUTHORIZATION_INTERCEPTOR("aciAuthorizationInterceptor"),
    ADMINISTRATIVE_POINT_INTERCEPTOR("administrativePointInterceptor"),
    AUTHENTICATION_INTERCEPTOR("authenticationInterceptor"),
    CHANGE_LOG_INTERCEPTOR("changeLogInterceptor"),
    COLLECTIVE_ATTRIBUTE_INTERCEPTOR("collectiveAttributeInterceptor"),
    DEFAULT_AUTHORIZATION_INTERCEPTOR("defaultAuthorizationInterceptor"),
    EVENT_INTERCEPTOR("eventInterceptor"),
    EXCEPTION_INTERCEPTOR("exceptionInterceptor"),
    JOURNAL_INTERCEPTOR("journalInterceptor"),
    NORMALIZATION_INTERCEPTOR("normalizationInterceptor"),
    OPERATIONAL_ATTRIBUTE_INTERCEPTOR("operationalAttributeInterceptor"),
    PASSWORD_POLICY_INTERCEPTOR("ppolicyInterceptor"),
    REFERRAL_INTERCEPTOR("referralInterceptor"),
    SCHEMA_INTERCEPTOR("schemaInterceptor"),
    SUBENTRY_INTERCEPTOR("subentryInterceptor"),
    TRIGGER_INTERCEPTOR("triggerInterceptor");

    /** The associated interceptor name */
    private String name;


    /**
     * The private constructor
     * @param methodName The associated interceptor name
     */
    InterceptorEnum( String name )
    {
        this.name = name;
    }


    /**
     * @return The associated interceptor name
     */
    public String getName()
    {
        return name;
    }
}
