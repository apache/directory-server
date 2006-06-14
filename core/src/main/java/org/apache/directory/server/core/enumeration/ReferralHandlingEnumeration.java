/*
 *   Copyright 2006 The Apache Software Foundation
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 */
package org.apache.directory.server.core.enumeration;


import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;

import org.apache.directory.server.core.partition.DirectoryPartitionNexus;
import org.apache.directory.server.core.referral.ReferralLut;
import org.apache.directory.server.core.schema.AttributeTypeRegistry;
import org.apache.directory.shared.ldap.codec.util.LdapURL;
import org.apache.directory.shared.ldap.codec.util.LdapURLEncodingException;
import org.apache.directory.shared.ldap.exception.LdapReferralException;
import org.apache.directory.shared.ldap.name.LdapDN;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * A wrapper enumeration which saves referral entries to be returned last.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class ReferralHandlingEnumeration implements NamingEnumeration
{
    private static final String REF_ATTR = "ref";
    private final Logger log = LoggerFactory.getLogger( ReferralHandlingEnumeration.class );
    private final List referrals = new ArrayList();
    private final NamingEnumeration underlying;
    private final ReferralLut lut;
    private final DirectoryPartitionNexus nexus;
    private final boolean doThrow;
    private final int scope;
    private SearchResult prefetched;
    private int refIndex = -1;


    public ReferralHandlingEnumeration( NamingEnumeration underlying, ReferralLut lut, AttributeTypeRegistry registry,
        DirectoryPartitionNexus nexus, int scope, boolean doThrow ) throws NamingException
    {
        this.underlying = underlying;
        this.doThrow = doThrow;
        this.lut = lut;
        this.scope = scope;
        this.nexus = nexus;
        prefetch();
    }


    public void prefetch() throws NamingException
    {
        while ( underlying.hasMore() )
        {
            SearchResult result = ( SearchResult ) underlying.next();
            LdapDN dn = new LdapDN( result.getName() );
            dn.normalize();
            
            if ( lut.isReferral( dn ) )
            {
                referrals.add( result );
                continue;
            }
            prefetched = result;
            return;
        }

        refIndex++;
        prefetched = ( SearchResult ) referrals.get( refIndex );
        if ( doThrow )
        {
            doReferralExceptionOnSearchBase();
        }
    }


    public Object next() throws NamingException
    {
        SearchResult retval = prefetched;
        prefetch();
        return retval;
    }


    public boolean hasMore() throws NamingException
    {
        return underlying.hasMore() || refIndex < referrals.size();
    }


    public void close() throws NamingException
    {
        underlying.close();
        referrals.clear();
        prefetched = null;
        refIndex = Integer.MAX_VALUE;
    }


    public boolean hasMoreElements()
    {
        try
        {
            return hasMore();
        }
        catch ( NamingException e )
        {
            log.error( "Naming enumeration failure.  Closing enumeration early!", e );
            try
            {
                close();
            }
            catch ( NamingException e1 )
            {
                log.error( "Naming enumeration failure.  Failed to properly close enumeration!", e1 );
            }
        }

        return false;
    }


    public Object nextElement()
    {
        try
        {
            return next();
        }
        catch ( NamingException e )
        {
            log.error( "NamingEnumeration closed prematurely without returning elements.", e );
        }

        throw new NoSuchElementException( "NamingEnumeration closed prematurely without returning elements." );
    }


    public void doReferralExceptionOnSearchBase() throws NamingException
    {
        // the refs attribute may be filtered out so we might need to lookup the entry
        Attribute refs = prefetched.getAttributes().get( REF_ATTR );
        if ( refs == null )
        {
            LdapDN prefetchedDn = new LdapDN( prefetched.getName() );
            prefetchedDn.normalize();
            refs = nexus.lookup( prefetchedDn ).get( REF_ATTR );
        }

        if ( refs == null )
        {
            throw new IllegalStateException( prefetched.getName()
                + " does not seem like a referral but we're trying to handle it as one." );
        }

        List list = new ArrayList( refs.size() );
        for ( int ii = 0; ii < refs.size(); ii++ )
        {
            String val = ( String ) refs.get( ii );

            // need to add non-ldap URLs as-is
            if ( !val.startsWith( "ldap" ) )
            {
                list.add( val );
                continue;
            }

            // parse the ref value and normalize the DN according to schema 
            LdapURL ldapUrl = new LdapURL();
            try
            {
                ldapUrl.parse( val.toCharArray() );
            }
            catch ( LdapURLEncodingException e )
            {
                log
                    .error( "Bad URL (" + val + ") for ref in " + prefetched.getName()
                        + ".  Reference will be ignored." );
            }

            StringBuffer buf = new StringBuffer();
            buf.append( ldapUrl.getScheme() );
            buf.append( ldapUrl.getHost() );
            if ( ldapUrl.getPort() > 0 )
            {
                buf.append( ":" );
                buf.append( ldapUrl.getPort() );
            }
            buf.append( "/" );
            buf.append( ldapUrl.getDn() );
            buf.append( "??" );

            switch ( scope )
            {
                case ( SearchControls.SUBTREE_SCOPE  ):
                    buf.append( "sub" );
                    break;

                // if we search for one level and encounter a referral then search
                // must be continued at that node using base level search scope
                case ( SearchControls.ONELEVEL_SCOPE  ):
                    buf.append( "base" );
                    break;
                case ( SearchControls.OBJECT_SCOPE  ):
                    buf.append( "base" );
                    break;
                default:
                    throw new IllegalStateException( "Unknown recognized search scope: " + scope );
            }

            list.add( buf.toString() );
        }
        LdapReferralException lre = new LdapReferralException( list );
        throw lre;
    }
}
