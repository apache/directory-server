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


import java.util.ArrayList;
import java.util.List;

import org.apache.directory.server.config.ConfigurationElement;


/**
 * A class used to store the ChangePasswordServer configuration.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class ChangePasswordServerBean extends DSBasedServerBean
{
    /** The allowable clock skew. */
    @ConfigurationElement(attributeType = "ads-krbAllowableClockSkew")
    private long krbAllowableClockSkew;

    /** Whether empty addresses are allowed. */
    @ConfigurationElement(attributeType = "ads-krbEmptyAddressesAllowed")
    private boolean krbEmptyAddressesAllowed;

    /** The encryption types. */
    @ConfigurationElement(attributeType = "ads-krbEncryptionTypes")
    private List<String> krbEncryptionTypes = new ArrayList<String>();

    /** The primary realm. */
    @ConfigurationElement(attributeType = "ads-krbPrimaryRealm")
    private String krbPrimaryRealm;

    /** The policy for category count. */
    @ConfigurationElement(attributeType = "ads-chgPwdPolicyCategoryCount")
    private int chgPwdPolicyCategoryCount;

    /** The policy for password length. */
    @ConfigurationElement(attributeType = "ads-chgPwdPolicyPasswordLength")
    private int chgPwdPolicyPasswordLength;

    /** The policy for token size. */
    @ConfigurationElement(attributeType = "ads-chgPwdPolicyTokenSize")
    private int chgPwdPolicyTokenSize;

    /** The service principal name. */
    @ConfigurationElement(attributeType = "ads-chgPwdServicePrincipal")
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
    public List<String> getKrbEncryptionTypes()
    {
        return krbEncryptionTypes;
    }


    /**
     * @param krbEncryptionTypes the krbEncryptionTypes to set
     */
    public void setKrbEncryptionTypes( List<String> krbEncryptionTypes )
    {
        this.krbEncryptionTypes = krbEncryptionTypes;
    }


    /**
     * Initialize the encryptionTypes set
     * 
     * @param krbEncryptionTypes the encryptionTypes to set
     */
    public void addKrbEncryptionTypes( String... krbEncryptionTypes )
    {
        for ( String encryptionType : krbEncryptionTypes )
        {
            this.krbEncryptionTypes.add( encryptionType );
        }
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


    /**
     * {@inheritDoc}
     */
    public String toString( String tabs )
    {
        StringBuilder sb = new StringBuilder();

        sb.append( tabs ).append( "ChangePasswordServer :\n" );
        sb.append( super.toString( tabs + "  " ) );
        sb.append( toString( tabs, "  change password service principal", chgPwdServicePrincipal ) );
        sb.append( toString( tabs, "  KRB primary realm", krbPrimaryRealm ) );

        if ( ( krbEncryptionTypes != null ) && ( krbEncryptionTypes.size() != 0 ) )
        {
            sb.append( tabs ).append( "  encryption types : \n" );

            for ( String encryptionType : krbEncryptionTypes )
            {
                sb.append( tabs ).append( "    encryptionType : " ).append( encryptionType ).append( '\n' );
            }
        }

        sb.append( toString( tabs, "  change password policy category count", chgPwdPolicyCategoryCount ) );
        sb.append( toString( tabs, "  change password policy password length", chgPwdPolicyPasswordLength ) );
        sb.append( toString( tabs, "  change password policy token size", chgPwdPolicyTokenSize ) );
        sb.append( toString( tabs, "  KRB allowable clock skew", krbAllowableClockSkew ) );
        sb.append( toString( tabs, "  KRB empty addresses allowed", krbEmptyAddressesAllowed ) );

        return sb.toString();
    }


    /**
     * {@inheritDoc}
     */
    public String toString()
    {
        return toString( "" );
    }
}
