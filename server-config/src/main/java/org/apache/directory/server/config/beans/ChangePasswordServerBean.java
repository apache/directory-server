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


/**
 * A class used to store the ChangePasswordServer configuration.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class ChangePasswordServerBean extends DSBasedServerBean
{
    /** The allowable clock skew. */
    private long krballowableclockskew;
    
    /** Whether empty addresses are allowed. */
    private boolean krbemptyaddressesallowed;
    
    /** The encryption types. */
    private List<String> krbencryptiontypes = new ArrayList<String>();
    
    /** The primary realm. */
    private String krbprimaryrealm;
    
    /** The policy for category count. */
    private int chgpwdpolicycategorycount;
    
    /** The policy for password length. */
    private int chgpwdpolicypasswordlength;
    
    /** The policy for token size. */
    private int chgpwdpolicytokensize;
    
    /** The service principal name. */
    private String chgpwdserviceprincipal;

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
        return krballowableclockskew;
    }

    
    /**
     * @param krbAllowableClockSkew the krbAllowableClockSkew to set
     */
    public void setKrbAllowableClockSkew( long krbAllowableClockSkew )
    {
        this.krballowableclockskew = krbAllowableClockSkew;
    }

    
    /**
     * @return the krbEmptyAddressesAllowed
     */
    public boolean isKrbEmptyAddressesAllowed()
    {
        return krbemptyaddressesallowed;
    }

    
    /**
     * @param krbEmptyAddressesAllowed the krbEmptyAddressesAllowed to set
     */
    public void setKrbEmptyAddressesAllowed( boolean krbEmptyAddressesAllowed )
    {
        this.krbemptyaddressesallowed = krbEmptyAddressesAllowed;
    }

    
    /**
     * @return the krbEncryptionTypes
     */
    public List<String> getKrbEncryptionTypes()
    {
        return krbencryptiontypes;
    }

    
    /**
     * @param krbEncryptionTypes the krbEncryptionTypes to set
     */
    public void setKrbEncryptionTypes( List<String> krbEncryptionTypes )
    {
        this.krbencryptiontypes = krbEncryptionTypes;
    }

    
    /**
     * @return the krbPrimaryRealm
     */
    public String getKrbPrimaryRealm()
    {
        return krbprimaryrealm;
    }

    
    /**
     * @param krbPrimaryRealm the krbPrimaryRealm to set
     */
    public void setKrbPrimaryRealm( String krbPrimaryRealm )
    {
        this.krbprimaryrealm = krbPrimaryRealm;
    }

    
    /**
     * @return the chgPwdPolicyCategoryCount
     */
    public int getChgPwdPolicyCategoryCount()
    {
        return chgpwdpolicycategorycount;
    }

    
    /**
     * @param chgPwdPolicyCategoryCount the chgPwdPolicyCategoryCount to set
     */
    public void setChgPwdPolicyCategoryCount( int chgPwdPolicyCategoryCount )
    {
        this.chgpwdpolicycategorycount = chgPwdPolicyCategoryCount;
    }

    
    /**
     * @return the chgPwdPolicyPasswordLength
     */
    public int getChgPwdPolicyPasswordLength()
    {
        return chgpwdpolicypasswordlength;
    }

    
    /**
     * @param chgPwdPolicyPasswordLength the chgPwdPolicyPasswordLength to set
     */
    public void setChgPwdPolicyPasswordLength( int chgPwdPolicyPasswordLength )
    {
        this.chgpwdpolicypasswordlength = chgPwdPolicyPasswordLength;
    }

    
    /**
     * @return the chgPwdPolicyTokenSize
     */
    public int getChgPwdPolicyTokenSize()
    {
        return chgpwdpolicytokensize;
    }

    
    /**
     * @param chgPwdPolicyTokenSize the chgPwdPolicyTokenSize to set
     */
    public void setChgPwdPolicyTokenSize( int chgPwdPolicyTokenSize )
    {
        this.chgpwdpolicytokensize = chgPwdPolicyTokenSize;
    }

    
    /**
     * @return the chgPwdServicePrincipal
     */
    public String getChgPwdServicePrincipal()
    {
        return chgpwdserviceprincipal;
    }

    
    /**
     * @param chgPwdServicePrincipal the chgPwdServicePrincipal to set
     */
    public void setChgPwdServicePrincipal( String chgPwdServicePrincipal )
    {
        this.chgpwdserviceprincipal = chgPwdServicePrincipal;
    }
    
    
    /**
     * {@inheritDoc}
     */
    public String toString( String tabs )
    {
        StringBuilder sb = new StringBuilder();
        
        sb.append( tabs ).append( "ChangePasswordServer :\n" );
        sb.append( super.toString( tabs ) );
        sb.append( tabs ).append( "  change password service principal : " ).append( chgpwdserviceprincipal ).append( '\n' );
        sb.append( tabs ).append( "  KRB primary realm : " ).append( krbprimaryrealm ).append( '\n' );
        
        if ( ( krbencryptiontypes != null ) && ( krbencryptiontypes.size() != 0 ) )
        {
            sb.append( tabs ).append( "  encryption types : \n" );

            for ( String encryptionType : krbencryptiontypes )
            {
                sb.append( tabs ).append( "    encryptionType : " ).append( encryptionType ).append( '\n' );
            }
        }
        
        sb.append( tabs ).append( "  change password policy category count : " ).append( chgpwdpolicycategorycount ).append( '\n' );
        sb.append( tabs ).append( "  change password policy password length : " ).append( chgpwdpolicypasswordlength ).append( '\n' );
        sb.append( tabs ).append( "  change password policy token size : " ).append( chgpwdpolicytokensize ).append( '\n' );
        sb.append( tabs ).append( "  KRB allowable clock skew : " ).append( krballowableclockskew ).append( '\n' );
        sb.append( toString( tabs, "  KRB empty addresses allowed", krbemptyaddressesallowed ) );

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
