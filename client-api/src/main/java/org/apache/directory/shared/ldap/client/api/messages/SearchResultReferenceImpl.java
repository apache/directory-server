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

import org.apache.directory.shared.ldap.util.LdapURL;


/**
 * Search reference protocol response message used to return referrals to the
 * client in response to a search request message.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Revision: 760984 $
 */
public class SearchResultReferenceImpl extends AbstractMessage implements SearchResultReference
{
    /** The list of LdapURL referrals */
    private Referral referral;
    
    /**
     * Creates a new instance of SearchResultReferenceImpl.
     */
    public SearchResultReferenceImpl()
    {
        super();
    }

    
    /**
     * {@inheritDoc}
     */
    public Referral getReferrals()
    {
        return referral;
    }


    /**
     * {@inheritDoc}
     */
    public void setReferral( Referral referral )
    {
        this.referral = referral;
    }


    /**
     * {@inheritDoc}
     */
    public void addReferrals( LdapURL... urls )
    {
        if ( referral == null )
        {
            referral = new ReferralImpl();
        }
        
        referral.addLdapUrls( urls );
    }


    /**
     * {@inheritDoc}
     */
    public void addReferrals( String... urls )
    {
        if ( referral == null )
        {
            referral = new ReferralImpl();
        }
        
        referral.addLdapUrls( urls );
    }


    /**
     * {@inheritDoc}
     */
    public void removeReferrals( LdapURL... urls )
    {
        if ( referral == null )
        {
            referral = new ReferralImpl();
        }
        
        referral.removeLdapUrl( urls );
    }


    /**
     * {@inheritDoc}
     */
    public void removeReferrals( String... urls )
    {
        if ( referral == null )
        {
            referral = new ReferralImpl();
        }
        
        referral.removeLdapUrl( urls );
    }
}
