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
package org.apache.directory.server.core.referral;


import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.naming.NamingException;

import org.apache.directory.server.core.cursor.CursorIterator;
import org.apache.directory.server.core.cursor.InvalidCursorPositionException;
import org.apache.directory.server.core.entry.ClonedServerEntry;
import org.apache.directory.server.core.filtering.EntryFilter;
import org.apache.directory.server.core.filtering.EntryFilteringCursor;
import org.apache.directory.server.core.interceptor.context.OperationContext;
import org.apache.directory.server.core.interceptor.context.SearchingOperationContext;
import org.apache.directory.shared.ldap.codec.util.LdapURL;
import org.apache.directory.shared.ldap.codec.util.LdapURLEncodingException;
import org.apache.directory.shared.ldap.constants.SchemaConstants;
import org.apache.directory.shared.ldap.entry.EntryAttribute;
import org.apache.directory.shared.ldap.entry.Value;
import org.apache.directory.shared.ldap.exception.LdapReferralException;
import org.apache.directory.shared.ldap.filter.SearchScope;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * A EntryFilteringCursor implementation wrapping another underlying 
 * EntryFilterCursor but saving the return of referral entries to be returned
 * last after returning regular entries.  These Cursors can only be started
 * at one end of a scan and terminated at another.  They cannot be positioned
 * anywhere in between because then tracking of encountered referral entries
 * would be invalidated.  This Cursor does not allow direction changes during
 * traversal.  A direction change will reposition the cursor at the respective
 * end of the Cursor.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class ReferralHandlingCursor implements EntryFilteringCursor
{
    private static final Logger LOG = LoggerFactory.getLogger( ReferralHandlingCursor.class );
    private static final int PARKED_INDEX = -1;
    
    private final EntryFilteringCursor wrapped;
    private final ReferralLut lut;
    private final List<ClonedServerEntry> referrals = new ArrayList<ClonedServerEntry>();
    private final boolean doThrow;
    
    private ClonedServerEntry entry;
    private int referralsIndex = PARKED_INDEX;
    private Boolean forward;
    
    
    public ReferralHandlingCursor( EntryFilteringCursor wrapped, ReferralLut lut, boolean doThrow )
    {
        this.lut = lut;
        this.doThrow = doThrow;
        this.wrapped = wrapped;
    }
    
    
    // -----------------------------------------------------------------------
    // EntryFilteringCursor Interface Methods
    // -----------------------------------------------------------------------
    

    /* 
     * @see org.apache.directory.server.core.filtering.EntryFilteringCursor#addEntryFilter(
     * org.apache.directory.server.core.filtering.EntryFilter)
     */
    public boolean addEntryFilter( EntryFilter filter )
    {
        return wrapped.addEntryFilter( filter );
    }


    /* 
     * @see org.apache.directory.server.core.filtering.EntryFilteringCursor#getEntryFilters()
     */
    public List<EntryFilter> getEntryFilters()
    {
        return wrapped.getEntryFilters();
    }


    /* 
     * @see org.apache.directory.server.core.filtering.EntryFilteringCursor#getOperationContext()
     */
    public SearchingOperationContext getOperationContext()
    {
        return wrapped.getOperationContext();
    }


    /* 
     * @see org.apache.directory.server.core.filtering.EntryFilteringCursor#isAbandoned()
     */
    public boolean isAbandoned()
    {
        return wrapped.isAbandoned();
    }


    /* 
     * @see org.apache.directory.server.core.filtering.EntryFilteringCursor#removeEntryFilter(
     * org.apache.directory.server.core.filtering.EntryFilter)
     */
    public boolean removeEntryFilter( EntryFilter filter )
    {
        return wrapped.removeEntryFilter( filter );
    }


    /* 
     * @see org.apache.directory.server.core.filtering.EntryFilteringCursor#setAbandoned(boolean)
     */
    public void setAbandoned( boolean abandoned )
    {
        wrapped.setAbandoned( abandoned );
    }


    // -----------------------------------------------------------------------
    // Unsupported Cursor Interface Methods
    // -----------------------------------------------------------------------
    

    /**
     * Unsupported operation since these Cursors do not preserve order due to
     * extraction and saving of referral entries to be returned at their end.
     * 
     * @see org.apache.directory.server.core.cursor.Cursor#after(java.lang.Object)
     */
    public void after( ClonedServerEntry element ) throws Exception
    {
        throw new UnsupportedOperationException();
    }


    /**
     * Unsupported operation since these Cursors do not preserve order due to
     * extraction and saving of referral entries to be returned at their end.
     * 
     * @see org.apache.directory.server.core.cursor.Cursor#before(java.lang.Object)
     */
    public void before( ClonedServerEntry element ) throws Exception
    {
        throw new UnsupportedOperationException();
    }


    // -----------------------------------------------------------------------
    // Supported Cursor Interface Methods
    // -----------------------------------------------------------------------
    
    
    /*
     * @see org.apache.directory.server.core.cursor.Cursor#afterLast()
     */
    public void afterLast() throws Exception
    {
        clearState();
        wrapped.afterLast();
        forward = false;
    }


    /*
     * @see org.apache.directory.server.core.cursor.Cursor#beforeFirst()
     */
    public void beforeFirst() throws Exception
    {
        clearState();
        wrapped.beforeFirst();
        forward = true;
    }


    /* 
     * @see org.apache.directory.server.core.cursor.Cursor#first()
     */
    public boolean first() throws Exception
    {
        beforeFirst();
        return next();
    }


    /* 
     * @see org.apache.directory.server.core.cursor.Cursor#last()
     */
    public boolean last() throws Exception
    {
        afterLast();
        return previous();
    }


    /* 
     * @see org.apache.directory.server.core.cursor.Cursor#next()
     */
    public boolean next() throws Exception
    {
        /* 
         * if our direction has not yet been set then implicitly with this
         * call the user has made the decision to continue traversal forward
         */ 

        if ( forward == null )
        {
            forward = true;
        }
        
        /*
         * if we've been going in the reverse direction we must position 
         * before the first element then step forward one entry clearing state
         */
        
        if ( ! forward )
        {
            beforeFirst();
        }
        
        while ( wrapped.next() )
        {
            ClonedServerEntry tempEntry = wrapped.get();
            
            if ( lut.isReferral( tempEntry.getDn() ) )
            {
                referrals.add( tempEntry );
                continue;
            }
            
            entry = tempEntry;
            return true;
        }
        
        if ( referralsIndex == PARKED_INDEX )
        {
            referralsIndex = 0;
        }
        else
        {
            referralsIndex++;
        }
        
        if ( referralsIndex <= referrals.size() )
        {
            entry = referrals.get( referralsIndex );
            
            if ( doThrow )
            {
                doReferralExceptionOnSearchBase( wrapped.getOperationContext() );
            }
                
            return true;
        }
        else
        {
            entry = null;
            return false;
        }
    }


    /* 
     * @see org.apache.directory.server.core.cursor.Cursor#previous()
     */
    public boolean previous() throws Exception
    {
        /* 
         * if our direction has not yet been set then implicitly with this
         * call the user has made the decision to continue traversal reverse
         */ 

        if ( forward == null )
        {
            forward = false;
        }
        
        /*
         * if we've been going in the reverse direction we must position 
         * before the first element then step forward one entry clearing state
         */
        
        if ( forward )
        {
            afterLast();
        }
        
        while ( wrapped.previous() )
        {
            ClonedServerEntry tempEntry = wrapped.get();
            
            if ( lut.isReferral( tempEntry.getDn() ) )
            {
                referrals.add( tempEntry );
                continue;
            }
            
            entry = tempEntry;
            return true;
        }
        
        if ( referralsIndex == PARKED_INDEX )
        {
            referralsIndex = referrals.size() - 1;
        }
        else
        {
            referralsIndex--;
        }
        
        if ( referralsIndex >= 0 )
        {
            entry = referrals.get( referralsIndex );
            return true;
        }
        else
        {
            entry = null;
            return false;
        }
    }


    /* 
     * @see org.apache.directory.server.core.cursor.Cursor#available()
     */
    public boolean available()
    {
        return entry != null;
    }


    /* 
     * @see org.apache.directory.server.core.cursor.Cursor#get()
     */
    public ClonedServerEntry get() throws Exception
    {
        if ( available() )
        {
            return entry;
        }
        
        throw new InvalidCursorPositionException();
    }


    /* 
     * @see org.apache.directory.server.core.cursor.Cursor#isClosed()
     */
    public boolean isClosed() throws Exception
    {
        return wrapped.isClosed();
    }


    /* 
     * @see org.apache.directory.server.core.cursor.Cursor#close()
     */
    public void close() throws Exception
    {
        wrapped.close();
        clearState();
    }


    /* 
     * @see org.apache.directory.server.core.cursor.Cursor#isElementReused()
     */
    public boolean isElementReused()
    {
        return wrapped.isElementReused();
    }


    /* 
     * @see java.lang.Iterable#iterator()
     */
    public Iterator<ClonedServerEntry> iterator()
    {
        return new CursorIterator<ClonedServerEntry>( this );
    }


    // -----------------------------------------------------------------------
    // Private Methods
    // -----------------------------------------------------------------------
    
    
    private void clearState()
    {
        referrals.clear();
        referralsIndex = PARKED_INDEX;
        entry = null;
        forward = null;
    }
    

    private void doReferralExceptionOnSearchBase( OperationContext opContext ) throws NamingException
    {
        // the refs attribute may be filtered out so we might need to lookup the entry
        EntryAttribute refs = entry.getOriginalEntry().get( SchemaConstants.REF_AT );
        
        if ( refs == null )
        {
            throw new IllegalStateException( entry.getDn()
                + " does not seem like a referral but we're trying to handle it as one." );
        }

        List<String> list = new ArrayList<String>( refs.size() );
        
        for ( Value<?> value:refs )
        {
            String val = ( String ) value.get();

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
                LOG.error( "Bad URL ({}) for ref in {}.  Reference will be ignored.", val, entry.getDn() );
            }

            StringBuilder buf = new StringBuilder();
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

            SearchScope scope = wrapped.getOperationContext().getScope();
            
            switch ( scope )
            {
                case SUBTREE:
                    buf.append( "sub" );
                    break;

                // if we search for one level and encounter a referral then search
                // must be continued at that node using base level search scope
                case ONELEVEL:
                    buf.append( "base" );
                    break;
                    
                case OBJECT:
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
