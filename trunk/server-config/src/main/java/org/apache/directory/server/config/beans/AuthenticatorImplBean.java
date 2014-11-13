/*
 *   Licensed to the Apache Software Foundation (ASF) under one
 *   or more contributor license agreements.  See the NOTICE file
 *   distributed with this work for additional information
 *   regarding copyright ownership.  The ASF licenses this file
 *   to you under the Apache License, Version 2.0 (the
 *   "License"); you may not use this file except in compliance
 *   with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing,
 *   software distributed under the License is distributed on an
 *   "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *   KIND, either express or implied.  See the License for the
 *   specific language governing permissions and limitations
 *   under the License.
 *
 */

package org.apache.directory.server.config.beans;


import org.apache.directory.server.config.ConfigurationElement;


/**
 * Configuration bean of an authenticator implementation.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class AuthenticatorImplBean extends AuthenticatorBean
{
    /** the fully qualified class name of the authenticator implementation */
    @ConfigurationElement(attributeType = "ads-authenticatorClass")
    private String authenticatorClass;


    /**
     * Creates a new instance of AuthenticatorImplBean.
     */
    public AuthenticatorImplBean()
    {
    }


    /**
     * @return the authenticatorClass
     */
    public String getAuthenticatorClass()
    {
        return authenticatorClass;
    }


    /**
     * @param authenticatorClass the authenticatorClass to set
     */
    public void setAuthenticatorClass( String authenticatorClass )
    {
        this.authenticatorClass = authenticatorClass;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public String toString()
    {
        return "AuthenticatorImplBean [authenticatorClass=" + authenticatorClass + "]";
    }
}
