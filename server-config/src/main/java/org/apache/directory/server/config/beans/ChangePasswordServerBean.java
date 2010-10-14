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

import java.util.HashSet;
import java.util.Set;


/**
 * A class used to store the ChangePasswordServer configuration.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class ChangePasswordServerBean extends CatalogBasedServerBean
{
    /** The allowable clock skew. */
    private long krbAllowableClockSkew;
    
    /** Whether empty addresses are allowed. */
    private boolean krbEmptyAddressesAllowed;
    
    /** The encryption types. */
    private Set<String> krbEncryptionTypes = new HashSet<String>();
    
    /** The primary realm. */
    private String krbPrimaryRealm;
    
    /** The policy for category count. */
    private int chgPwdPolicyCategoryCount;
    
    /** The policy for password length. */
    private int chgPwdPolicyPasswordLength;
    
    /** The policy for token size. */
    private int chgPwdPolicyTokenSize;
    
    /** The service principal name. */
    private String chgPwdServicePrincipal;

    /**
     * Create a new ChangePasswordServer instance
     */
    public ChangePasswordServerBean()
    {
        super();
    }

    /**
     * @return the krbAllowableClockSkew
     */
    public long getKrbAllowableClockSkew()
    {
        return krbAllowableClockSkew;
    }

    
    /**
     * @param krbAllowableClockSkew the krbAllowableClockSkew to set
     */
    public void setKrbAllowableClockSkew( long krbAllowableClockSkew )
    {
        this.krbAllowableClockSkew = krbAllowableClockSkew;
    }

    
    /**
     * @return the krbEmptyAddressesAllowed
     */
    public boolean isKrbEmptyAddressesAllowed()
    {
        return krbEmptyAddressesAllowed;
    }

    
    /**
     * @param krbEmptyAddressesAllowed the krbEmptyAddressesAllowed to set
     */
    public void setKrbEmptyAddressesAllowed( boolean krbEmptyAddressesAllowed )
    {
        this.krbEmptyAddressesAllowed = krbEmptyAddressesAllowed;
    }

    
    /**
     * @return the krbEncryptionTypes
     */
    public Set<String> getKrbEncryptionTypes()
    {
        return krbEncryptionTypes;
    }

    
    /**
     * @param krbEncryptionTypes the krbEncryptionTypes to set
     */
    public void setKrbEncryptionTypes( Set<String> krbEncryptionTypes )
    {
        this.krbEncryptionTypes = krbEncryptionTypes;
    }

    
    /**
     * @return the krbPrimaryRealm
     */
    public String getKrbPrimaryRealm()
    {
        return krbPrimaryRealm;
    }

    
    /**
     * @param krbPrimaryRealm the krbPrimaryRealm to set
     */
    public void setKrbPrimaryRealm( String krbPrimaryRealm )
    {
        this.krbPrimaryRealm = krbPrimaryRealm;
    }

    
    /**
     * @return the chgPwdPolicyCategoryCount
     */
    public int getChgPwdPolicyCategoryCount()
    {
        return chgPwdPolicyCategoryCount;
    }

    
    /**
     * @param chgPwdPolicyCategoryCount the chgPwdPolicyCategoryCount to set
     */
    public void setChgPwdPolicyCategoryCount( int chgPwdPolicyCategoryCount )
    {
        this.chgPwdPolicyCategoryCount = chgPwdPolicyCategoryCount;
    }

    
    /**
     * @return the chgPwdPolicyPasswordLength
     */
    public int getChgPwdPolicyPasswordLength()
    {
        return chgPwdPolicyPasswordLength;
    }

    
    /**
     * @param chgPwdPolicyPasswordLength the chgPwdPolicyPasswordLength to set
     */
    public void setChgPwdPolicyPasswordLength( int chgPwdPolicyPasswordLength )
    {
        this.chgPwdPolicyPasswordLength = chgPwdPolicyPasswordLength;
    }

    
    /**
     * @return the chgPwdPolicyTokenSize
     */
    public int getChgPwdPolicyTokenSize()
    {
        return chgPwdPolicyTokenSize;
    }

    
    /**
     * @param chgPwdPolicyTokenSize the chgPwdPolicyTokenSize to set
     */
    public void setChgPwdPolicyTokenSize( int chgPwdPolicyTokenSize )
    {
        this.chgPwdPolicyTokenSize = chgPwdPolicyTokenSize;
    }

    
    /**
     * @return the chgPwdServicePrincipal
     */
    public String getChgPwdServicePrincipal()
    {
        return chgPwdServicePrincipal;
    }

    
    /**
     * @param chgPwdServicePrincipal the chgPwdServicePrincipal to set
     */
    public void setChgPwdServicePrincipal( String chgPwdServicePrincipal )
    {
        this.chgPwdServicePrincipal = chgPwdServicePrincipal;
    }
}
