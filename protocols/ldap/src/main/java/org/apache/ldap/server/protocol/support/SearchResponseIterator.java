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
package org.apache.ldap.server.protocol.support;


import java.util.Iterator;
import java.util.NoSuchElementException;

import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.SearchResult;

import org.apache.ldap.common.exception.LdapException;
import org.apache.ldap.common.message.ManageDsaITControl;
import org.apache.ldap.common.message.ReferralImpl;
import org.apache.ldap.common.message.ResultCodeEnum;
import org.apache.ldap.common.message.SearchRequest;
import org.apache.ldap.common.message.SearchResponseDone;
import org.apache.ldap.common.message.SearchResponseEntry;
import org.apache.ldap.common.message.SearchResponseEntryImpl;
import org.apache.ldap.common.message.SearchResponseReference;
import org.apache.ldap.common.message.SearchResponseReferenceImpl;
import org.apache.ldap.common.util.ExceptionUtils;

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
    private final NamingEnumeration underlying;
    private SearchResponseDone respDone;
    private boolean done = false;
    private Object prefetched;


    /**
     * Creates a search response iterator for the resulting enumeration
     * over a search request.
     *
     * @param req the search request to generate responses to
     * @param underlying the underlying JNDI enumeration containing SearchResults
     */
    public SearchResponseIterator( SearchRequest req, NamingEnumeration underlying )
    {
        this.req = req;
        this.underlying = underlying;

        try
        {
            if( underlying.hasMore() )
            {
                SearchResult result = ( SearchResult ) underlying.next();

                /*
                 * Now we have to build the prefetched object from the 'result'
                 * local variable for the following call to next()
                 */
                Attribute ref = result.getAttributes().get( "ref" );
                if( ref == null || ref.size() == 0 || req.getControls().containsKey( ManageDsaITControl.CONTROL_OID ) )
                {
                    SearchResponseEntry respEntry;
                    respEntry = new SearchResponseEntryImpl( req.getMessageId() );
                    respEntry.setAttributes( result.getAttributes() );
                    respEntry.setObjectName( result.getName() );
                    prefetched = respEntry;
                }
                else
                {
                    SearchResponseReference respRef;
                    respRef = new SearchResponseReferenceImpl( req.getMessageId() );
                    respRef.setReferral( new ReferralImpl() );

                    for( int ii = 0; ii < ref.size(); ii ++ )
                    {
                        String url;

                        try
                        {
                            url = ( String ) ref.get( ii );
                            respRef.getReferral().addLdapUrl( url );
                        }
                        catch( NamingException e )
                        {
                            try
                            {
                                underlying.close();
                            }
                            catch( Throwable t )
                            {
                            }

                            prefetched = null;
                            respDone = getResponse( req, e );
                        }
                    }

                    prefetched = respRef;
                }
            }
        }
        catch( NamingException e )
        {
            try
            {
                this.underlying.close();
            }
            catch( Exception e2 )
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
        if( done )
        {
            throw new NoSuchElementException();
        }

        // if respDone has been assembled this is our last object to return
        if( respDone != null )
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
            if( underlying.hasMore() )
            {
                result = ( SearchResult ) underlying.next();
            }
            else
            {
                try
                {
                    underlying.close();
                }
                catch( Throwable t )
                {
                }

                respDone = ( SearchResponseDone ) req.getResultResponse();
                respDone.getLdapResult().setResultCode( ResultCodeEnum.SUCCESS );
                prefetched = null;
                return next;
            }
        }
        catch( NamingException e )
        {
            try
            {
                underlying.close();
            }
            catch( Throwable t )
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

        if( ref == null || ref.size() > 0 )
        {
            SearchResponseEntry respEntry = new SearchResponseEntryImpl( req.getMessageId() );
            respEntry.setAttributes( result.getAttributes() );
            respEntry.setObjectName( result.getName() );
            prefetched = respEntry;
        }
        else
        {
            SearchResponseReference respRef = new SearchResponseReferenceImpl( req.getMessageId() );
            respRef.setReferral( new ReferralImpl() );

            for( int ii = 0; ii < ref.size(); ii ++ )
            {
                String url;

                try
                {
                    url = ( String ) ref.get( ii );
                    respRef.getReferral().addLdapUrl( url );
                }
                catch( NamingException e )
                {
                    try
                    {
                        underlying.close();
                    }
                    catch( Throwable t )
                    {
                    }

                    prefetched = null;
                    respDone = getResponse( req, e );
                    return next;
                }
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


    SearchResponseDone getResponse( SearchRequest req, NamingException e )
    {
        String msg = "failed on search operation";
        if ( log.isDebugEnabled() )
        {
            msg += ":\n" + req + ":\n" + ExceptionUtils.getStackTrace( e );
        }

        SearchResponseDone resp = ( SearchResponseDone ) req.getResultResponse();
        ResultCodeEnum code = null;
        if( e instanceof LdapException )
        {
            code = ( ( LdapException ) e ).getResultCode();
        }
        else
        {
            code = ResultCodeEnum.getBestEstimate( e, req.getType() );
        }

        resp.getLdapResult().setResultCode( code );
        resp.getLdapResult().setErrorMessage( msg );
        if ( ( e.getResolvedName() != null ) &&
                ( ( code == ResultCodeEnum.NOSUCHOBJECT ) ||
                  ( code == ResultCodeEnum.ALIASPROBLEM ) ||
                  ( code == ResultCodeEnum.INVALIDDNSYNTAX ) ||
                  ( code == ResultCodeEnum.ALIASDEREFERENCINGPROBLEM ) ) )
        {
            resp.getLdapResult().setMatchedDn( e.getResolvedName().toString() );
        }
        return resp;
    }
}