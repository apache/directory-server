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


import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.directory.shared.ldap.codec.util.LdapURLEncodingException;
import org.apache.directory.shared.ldap.util.LdapURL;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Represents a referral which is a set of alternative locations where an entry
 * can be found. Here's what <a href="http://www.faqs.org/rfcs/rfc2251.html">
 * RFC 2251 </a> has to say about it:
 * 
 * <pre>
 *  4.1.11. Referral
 * 
 *   The referral error indicates that the contacted server does not hold
 *   the target entry of the request.  The referral field is present in an
 *   LDAPResult if the LDAPResult.resultCode field value is referral, and
 *   absent with all other result codes.  It contains a reference to
 *   another server (or set of servers) which may be accessed via LDAP or
 *   other protocols.  Referrals can be returned in response to any
 *   operation request (except unbind and abandon which do not have
 *   responses). At least one URL MUST be present in the Referral.
 * 
 *   The referral is not returned for a singleLevel or wholeSubtree search
 *   in which the search scope spans multiple naming contexts, and several
 *   different servers would need to be contacted to complete the
 *   operation. Instead, continuation references, described in section
 *   4.5.3, are returned.
 * 
 *        Referral ::= SEQUENCE OF LDAPURL  -- one or more
 * 
 *        LDAPURL ::= LDAPString -- limited to characters permitted in URLs
 * 
 *   If the client wishes to progress the operation, it MUST follow the
 *   referral by contacting any one of servers.  All the URLs MUST be
 *   equally capable of being used to progress the operation.  (The
 *   mechanisms for how this is achieved by multiple servers are outside
 *   the scope of this document.)
 * 
 *   URLs for servers implementing the LDAP protocol are written according
 *   to &lt;a href=&quot;http://www.faqs.org/rfcs/rfc2255.html&quot;&gt;[9]&lt;/a&gt;.  If an alias
 *   was dereferenced, the &lt;dn&gt; part of the URL MUST be present, with the new
 *   target object name.  If the &lt;dn&gt; part is present, the client MUST use this
 *   name in its next request to progress the operation, and if it is not present
 *   the client will use the same name as in the original request.  Some servers
 *   (e.g. participating in distributed indexing) may provide a different filter
 *   in a referral for a search operation.  If the filter part of the URL
 *    is present in an LDAPURL, the client MUST use this filter in its next
 *   request to progress this search, and if it is not present the client
 *   MUST use the same filter as it used for that search.  Other aspects
 *   of the new request may be the same or different as the request which
 *   generated the referral.
 * 
 *   Note that UTF-8 characters appearing in a DN or search filter may not
 *   be legal for URLs (e.g. spaces) and MUST be escaped using the %
 *   method in RFC 1738 &lt;a href=&quot;http://www.faqs.org/rfcs/rfc1738.html&quot;&gt;[7]&lt;/a&gt;.
 * 
 *   Other kinds of URLs may be returned, so long as the operation could
 *   be performed using that protocol.
 * </pre>
 * 
 * @author <a href="mailto:dev@directory.apache.org"> Apache Directory Project</a>
 * @version $Rev: 761643 $
 * TODO This interface should be located in a url package under common to be
 *       constructed from a LDAPv3 URL parser. The interface will eventually
 *       look very different once url support is added: for one it will add and
 *       remove LdapUrl objects instead of strings or provide both string and
 *       LdapUrl add/remove methods.
 */
public class Referral
{
    /** The logger */
    static final Logger LOG = LoggerFactory.getLogger( Referral.class );

    /** The list of LDAPUrls */
    private List<LdapURL> referrals;
    
    /**
     * Gets an unmodifiable set of alternative referral urls.
     * 
     * @return the alternative url objects.
     */
    public Collection<String> getLdapUrls()
    {
        if ( referrals == null )
        {
            return new ArrayList<String>();
        }
        
        List<String> urls = new ArrayList<String>( referrals.size() );
        
        for ( LdapURL referral:referrals )
        {
            urls.add( referral.toString() );
        }
        
        return urls;
    }


    /**
     * Adds an LDAPv3 URL to this Referral.
     * 
     * @param urls the LDAPv3 URLs to add
     */
    public void addLdapUrls( String... urls )
    {
        if ( ( urls == null ) || ( urls.length == 0 ) )
        {
            return;
        }
        
        if ( referrals == null )
        {
            referrals = new ArrayList<LdapURL>( urls.length );
        }
        
        for ( String url:urls )
        {
            try
            {
                LdapURL ldapUrl = new LdapURL( url );
                
                referrals.add( ldapUrl );
            }
            catch ( LdapURLEncodingException luee )
            {
                LOG.warn( "The given URL '{}' is invalid", url );
            }
        }
    }


    /**
     * Adds an LDAPv3 URL to this Referral.
     * 
     * @param urls the LDAPv3 URLs to add
     */
    public void addLdapUrls( LdapURL... urls )
    {
        if ( ( urls == null ) || ( urls.length == 0 ) )
        {
            return;
        }
        
        if ( referrals == null )
        {
            referrals = new ArrayList<LdapURL>( urls.length );
        }
        
        for ( LdapURL url:urls )
        {
            referrals.add( url );
        }
    }


    /**
     * Removes an LDAPv3 URL to this Referral.
     * 
     * @param urls the LDAPv3 URLs to remove
     */
    public void removeLdapUrl( String... urls )
    {
        if ( ( urls == null ) || ( urls.length == 0 ) )
        {
            return;
        }
        
        if ( ( referrals == null ) || ( referrals.size() == 0 ) )
        {
            return;
        }
        
        for ( String url:urls )
        {
            try
            {
                LdapURL ldapUrl = new LdapURL( url );
                
                referrals.remove( ldapUrl );
            }
            catch ( LdapURLEncodingException luee )
            {
                LOG.warn( "The given URL '{}' is invalid", url );
            }
        }
    }


    /**
     * Removes an LDAPv3 URL to this Referral.
     * 
     * @param urls the LDAPv3 URLs to remove
     */
    public void removeLdapUrl( LdapURL... urls )
    {
        if ( ( urls == null ) || ( urls.length == 0 ) )
        {
            return;
        }
        
        if ( ( referrals == null ) || ( referrals.size() == 0 ) )
        {
            return;
        }
        
        for ( LdapURL url:urls )
        {
            referrals.remove( url );
        }
    }
}
