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
    NORMALIZATION_INTERCEPTOR("normalizationInterceptor"),
    AUTHENTICATION_INTERCEPTOR("authenticationInterceptor"),
    REFERRAL_INTERCEPTOR("referralInterceptor"),
    ACI_AUTHORIZATION_INTERCEPTOR("aciAuthorizationInterceptor"),
    DEFAULT_AUTHORIZATION_INTERCEPTOR("defaultAuthorizationInterceptor"),
    ADMINISTRATIVE_POINT_INTERCEPTOR("administrativePointInterceptor"),
    EXCEPTION_INTERCEPTOR("exceptionInterceptor"),
    OPERATIONAL_ATTRIBUTE_INTERCEPTOR("operationalAttributeInterceptor"),
    SCHEMA_INTERCEPTOR("schemaInterceptor"),
    SUBENTRY_INTERCEPTOR("subentryInterceptor"),
    EVENT_INTERCEPTOR("eventInterceptor"),
    TRIGGER_INTERCEPTOR("triggerInterceptor"),
    CHANGE_LOG_INTERCEPTOR("changeLogInterceptor"),
    COLLECTIVE_ATTRIBUTE_INTERCEPTOR("collectiveAttributeInterceptor"),
    JOURNAL_INTERCEPTOR("journalInterceptor");

    /** The associated interceptor name */
    private String name;


    /**
     * The private constructor
     * @param methodName The associated interceptor name
     */
    private InterceptorEnum( String name )
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
