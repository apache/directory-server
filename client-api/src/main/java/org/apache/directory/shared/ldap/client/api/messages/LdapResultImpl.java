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

package org.apache.directory.shared.ldap.client.api.messages;

import org.apache.directory.shared.ldap.message.ResultCodeEnum;


/**
 * LdapResult implementation.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Revision: 760984 $
 */
public class LdapResultImpl implements LdapResult
{
    /** Lowest matched entry Dn - defaults to empty string */
    private String matchedDn;

    /** Referral associated with this LdapResult if the errorCode is REFERRAL */
    private Referral referral;

    /** Decriptive error message - defaults to empty string */
    private String errorMessage;

    /** Resultant operation error code - defaults to SUCCESS */
    private ResultCodeEnum resultCode = ResultCodeEnum.SUCCESS;


    // ------------------------------------------------------------------------
    // LdapResult Interface Method Implementations
    // ------------------------------------------------------------------------
    /**
     * {@inheritDoc}
     */
    public String getErrorMessage()
    {
        return errorMessage;
    }


    /**
     * {@inheritDoc}
     */
    public void setErrorMessage( String errorMessage )
    {
        this.errorMessage = errorMessage;
    }


    /**
     * {@inheritDoc}
     */
    public String getMatchedDn()
    {
        return matchedDn;
    }


    /**
     * {@inheritDoc}
     */
    public void setMatchedDn( String matchedDn )
    {
        this.matchedDn = matchedDn;
    }


    /**
     * {@inheritDoc}
     */
    public ResultCodeEnum getResultCode()
    {
        return resultCode;
    }


    /**
     * {@inheritDoc}
     */
    public void setResultCode( ResultCodeEnum resultCode )
    {
        this.resultCode = resultCode;
    }


    /**
     * {@inheritDoc}
     */
    public Referral getReferral()
    {
        return referral;
    }


    /**
     * {@inheritDoc}
     */
    public boolean isReferral()
    {
        return referral != null;
    }


    /**
     * {@inheritDoc}
     */
    public void setReferral( Referral referral )
    {
        this.referral = referral;
    }
}
