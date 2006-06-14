/*
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
package org.apache.directory.server.ldap.support;


import java.util.Iterator;
import java.util.NoSuchElementException;

import javax.naming.InvalidNameException;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;

import org.apache.directory.server.core.jndi.ServerLdapContext;
import org.apache.directory.server.ldap.SessionRegistry;
import org.apache.directory.shared.ldap.codec.util.LdapURL;
import org.apache.directory.shared.ldap.codec.util.LdapURLEncodingException;
import org.apache.directory.shared.ldap.exception.LdapException;
import org.apache.directory.shared.ldap.message.ManageDsaITControl;
import org.apache.directory.shared.ldap.message.ReferralImpl;
import org.apache.directory.shared.ldap.message.ResultCodeEnum;
import org.apache.directory.shared.ldap.message.SearchRequest;
import org.apache.directory.shared.ldap.message.SearchResponseDone;
import org.apache.directory.shared.ldap.message.SearchResponseEntry;
import org.apache.directory.shared.ldap.message.SearchResponseEntryImpl;
import org.apache.directory.shared.ldap.message.SearchResponseReference;
import org.apache.directory.shared.ldap.message.SearchResponseReferenceImpl;
import org.apache.directory.shared.ldap.name.LdapDN;
import org.apache.directory.shared.ldap.util.ExceptionUtils;
import org.apache.mina.common.IoSession;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * A iterator which wraps a search result returning naming enumeration to return 
 * search responses.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
class SearchResponseIterator implements Iterator
{
    private static final Logger log = LoggerFactory.getLogger( SearchResponseIterator.class );
    private final SearchRequest req;
    private final ServerLdapContext ctx;
    private final NamingEnumeration underlying;
    private final IoSession session;
    private SearchResponseDone respDone;
    private boolean done = false;
    private Object prefetched;
    private final int scope;


    /**
     * Creates a search response iterator for the resulting enumeration
     * over a search request.
     *
     * @param req the search request to generate responses to
     * @param underlying the underlying JNDI enumeration containing SearchResults
     */
    public SearchResponseIterator( SearchRequest req, ServerLdapContext ctx, NamingEnumeration underlying, int scope,
        IoSession session )
    {
        this.req = req;
        this.ctx = ctx;
        this.scope = scope;
        this.underlying = underlying;
        this.session = session;

        try
        {
            if ( underlying.hasMore() )
            {
                SearchResult result = ( SearchResult ) underlying.next();

                /*
                 * Now we have to build the prefetched object from the 'result'
                 * local variable for the following call to next()
                 */
                Attribute ref = result.getAttributes().get( "ref" );
                if ( !ctx.isReferral( result.getName() )
                    || req.getControls().containsKey( ManageDsaITControl.CONTROL_OID ) )
                {
                    SearchResponseEntry respEntry;
                    respEntry = new SearchResponseEntryImpl( req.getMessageId() );
                    respEntry.setAttributes( result.getAttributes() );
                    try
                    {
                        respEntry.setObjectName( new LdapDN( result.getName() ) );
                    }
                    catch ( InvalidNameException ine )
                    {
                        log.error( "Invalid object name : " + result.getName(), ine);
                        throw new RuntimeException( ine );
                    }
                    
                    prefetched = respEntry;
                }
                else
                {
                    SearchResponseReference respRef;
                    respRef = new SearchResponseReferenceImpl( req.getMessageId() );
                    respRef.setReferral( new ReferralImpl() );

                    for ( int ii = 0; ii < ref.size(); ii++ )
                    {
                        String url;

                        try
                        {
                            url = ( String ) ref.get( ii );
                            respRef.getReferral().addLdapUrl( url );
                        }
                        catch ( NamingException e )
                        {
                            try
                            {
                                underlying.close();
                            }
                            catch ( Throwable t )
                            {
                            }

                            prefetched = null;
                            respDone = getResponse( req, e );
                        }
                    }

                    prefetched = respRef;
                }
            }
            else
            {
                SessionRegistry.getSingleton().removeOutstandingRequest( session, req.getMessageId() );
            }
        }
        catch ( NamingException e )
        {
            try
            {
                this.underlying.close();
            }
            catch ( Exception e2 )
            {
            }

            respDone = getResponse( req, e );
        }
    }


    public boolean hasNext()
    {
        return !done;
    }


    public Object next()
    {
        Object next = prefetched;
        SearchResult result = null;

        // if we're done we got nothing to give back
        if ( done )
        {
            throw new NoSuchElementException();
        }

        // if respDone has been assembled this is our last object to return
        if ( respDone != null )
        {
            done = true;
            return respDone;
        }

        /*
         * If we have gotten this far then we have a valid next entry
         * or referral to return from this call in the 'next' variable.
         */
        try
        {
            /*
             * If we have more results from the underlying cursorr then
             * we just set the result and build the response object below.
             */
            if ( underlying.hasMore() )
            {
                result = ( SearchResult ) underlying.next();
            }
            else
            {
                try
                {
                    underlying.close();
                }
                catch ( Throwable t )
                {
                }

                respDone = ( SearchResponseDone ) req.getResultResponse();
                respDone.getLdapResult().setResultCode( ResultCodeEnum.SUCCESS );
                prefetched = null;
                SessionRegistry.getSingleton().removeOutstandingRequest( session, req.getMessageId() );
                return next;
            }
        }
        catch ( NamingException e )
        {
            try
            {
                underlying.close();
            }
            catch ( Throwable t )
            {
            }

            prefetched = null;
            respDone = getResponse( req, e );
            return next;
        }

        /*
         * Now we have to build the prefetched object from the 'result'
         * local variable for the following call to next()
         */
        Attribute ref = result.getAttributes().get( "ref" );
        boolean isReferral = false;

        try
        {
            isReferral = ctx.isReferral( result.getName() );
        }
        catch ( NamingException e )
        {
            log.error( "failed to determine if " + result.getName() + " is a referral", e );
            throw new RuntimeException( e );
        }

        // we may need to lookup the object again if the ref attribute was filtered out
        if ( isReferral && ref == null )
        {
            try
            {
                ref = ctx.getAttributes( result.getName(), new String[]
                    { "ref" } ).get( "ref" );
            }
            catch ( NamingException e )
            {
                log.error( "failed to lookup ref attribute for " + result.getName(), e );
                throw new RuntimeException( e );
            }
        }

        if ( !isReferral || req.getControls().containsKey( ManageDsaITControl.CONTROL_OID ) )
        {
            SearchResponseEntry respEntry = new SearchResponseEntryImpl( req.getMessageId() );
            respEntry.setAttributes( result.getAttributes() );
            
            try
            {
                respEntry.setObjectName( new LdapDN( result.getName() ) );
            }
            catch ( InvalidNameException ine )
            {
                log.error( "Invalid object name : " + result.getName(), ine);
                throw new RuntimeException( ine );
            }
            
            prefetched = respEntry;
        }
        else
        {
            SearchResponseReference respRef = new SearchResponseReferenceImpl( req.getMessageId() );
            respRef.setReferral( new ReferralImpl() );

            for ( int ii = 0; ii < ref.size(); ii++ )
            {
                String val;
                try
                {
                    val = ( String ) ref.get( ii );
                }
                catch ( NamingException e1 )
                {
                    log.error( "failed to access referral url." );
                    try
                    {
                        underlying.close();
                    }
                    catch ( Throwable t )
                    {
                    }

                    prefetched = null;
                    respDone = getResponse( req, e1 );
                    return next;
                }

                // need to add non-ldap URLs as-is
                if ( !val.startsWith( "ldap" ) )
                {
                    respRef.getReferral().addLdapUrl( val );
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
                        .error( "Bad URL (" + val + ") for ref in " + result.getName()
                            + ".  Reference will be ignored." );
                    try
                    {
                        underlying.close();
                    }
                    catch ( Throwable t )
                    {
                    }

                    prefetched = null;
                    respDone = getResponse( req, e );
                    return next;
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

                respRef.getReferral().addLdapUrl( buf.toString() );
            }

            prefetched = respRef;
        }

        return next;
    }


    /**
     * Unsupported so it throws an exception.
     *
     * @throws UnsupportedOperationException
     */
    public void remove()
    {
        throw new UnsupportedOperationException();
    }


    SearchResponseDone getResponse( SearchRequest req, Exception e )
    {
        String msg = "failed on search operation";
        
        if ( log.isDebugEnabled() )
        {
            msg += ":\n" + req + ":\n" + ExceptionUtils.getStackTrace( e );
        }

        SearchResponseDone resp = ( SearchResponseDone ) req.getResultResponse();
        ResultCodeEnum code = null;
        
        if ( e instanceof LdapException )
        {
            code = ( ( LdapException ) e ).getResultCode();
        }
        else
        {
            code = ResultCodeEnum.getBestEstimate( e, req.getType() );
        }

        resp.getLdapResult().setResultCode( code );
        resp.getLdapResult().setErrorMessage( msg );

        if ( e instanceof NamingException )
        {
            NamingException ne = ( NamingException ) e;
            
            if ( ( ne.getResolvedName() != null )
                && ( ( code == ResultCodeEnum.NOSUCHOBJECT ) || ( code == ResultCodeEnum.ALIASPROBLEM )
                    || ( code == ResultCodeEnum.INVALIDDNSYNTAX ) || ( code == ResultCodeEnum.ALIASDEREFERENCINGPROBLEM ) ) )
            {
                resp.getLdapResult().setMatchedDn( (LdapDN)ne.getResolvedName() );
            }
        }
        
        SessionRegistry.getSingleton().removeOutstandingRequest( session, req.getMessageId() );
        return resp;
    }
}