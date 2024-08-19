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
package org.apache.directory.server.core.api;


import javax.naming.Context;

import org.apache.directory.api.util.Strings;
import org.apache.directory.server.i18n.I18n;


/**
 * Enumeration for referral handling modes.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public enum ReferralHandlingMode
{
    THROW("throw"),
    FOLLOW("follow"),
    IGNORE("ignore"),
    THROW_FINDING_BASE("throw-finding-base");

    /** 
     * The JNDI Context.REFERRAL key's value.
     * 
     * @see Context#REFERRAL
     */
    private final String jndiValue;


    /**
     * Creates a new instance of ReferralHandlingMode.
     *
     * @see Context#REFERRAL
     * @param jndiValue the JNDI Context.REFERRAL key's value
     */
    ReferralHandlingMode( String jndiValue )
    {
        this.jndiValue = jndiValue;
    }


    /**
     * Gets the equivalent JNDI Context.REFERRAL key's value for this enumeration constant.
     *
     * @see Context#REFERRAL
     * @return the equivalent JNDI Context.REFERRAL key's value
     */
    public String getJndiValue()
    {
        return jndiValue;
    }


    /**
     * Gets the enumeration constant for the JNDI Context.REFERRAL key's value.
     *
     * @see Context#REFERRAL
     * @param jndiValue the JNDI Context.REFERRAL key's value
     * @return the referral handling mode enumeration constant
     * @throws IllegalArgumentException if the value is not a recognized value
     */
    public static ReferralHandlingMode getModeFromJndi( String jndiValue )
    {
        jndiValue = Strings.toLowerCaseAscii( Strings.trim( jndiValue ) );

        if ( jndiValue.equals( "throw" ) )
        {
            return THROW;
        }

        if ( jndiValue.equals( "follow" ) )
        {
            return FOLLOW;
        }

        if ( jndiValue.equals( "ignore" ) )
        {
            return IGNORE;
        }

        if ( jndiValue.equals( "throw-finding-base" ) )
        {
            return THROW_FINDING_BASE;
        }

        throw new IllegalArgumentException( I18n.err( I18n.ERR_02037_UNKNOWN_JNDI_CONTEXT_REFERRAL, jndiValue ) );
    }
}
