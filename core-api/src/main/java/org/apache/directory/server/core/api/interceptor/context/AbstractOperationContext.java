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
package org.apache.directory.server.core.api.interceptor.context;


import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.directory.server.core.api.CoreSession;
import org.apache.directory.server.core.api.LdapPrincipal;
import org.apache.directory.shared.ldap.model.entry.Entry;
import org.apache.directory.shared.ldap.model.exception.LdapException;
import org.apache.directory.shared.ldap.model.message.Control;
import org.apache.directory.shared.ldap.model.name.Dn;


/**
 * This abstract class stores common context elements, like the Dn, which is used
 * in all the contexts.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public abstract class AbstractOperationContext implements OperationContext
{
    protected static final Control[] EMPTY_CONTROLS = new Control[0];

    /** The Dn associated with the context */
    protected Dn dn;

    /** The entry associated with the target entry of this OperationContext */
    protected Entry entry;

    /** The original Entry */
    protected Entry originalEntry;

    /** The associated request's controls */
    protected Map<String, Control> requestControls = new HashMap<String, Control>(4);

    /** The associated response's controls */
    protected Map<String, Control> responseControls = new HashMap<String, Control>(4);

    /** the Interceptors bypassed by this operation */
    protected Collection<String> byPassed;

    /** The interceptors to call for this operation */
    protected List<String> interceptors;

    /** The current interceptor position */
    protected int currentInterceptor;

    protected LdapPrincipal authorizedPrincipal;

    /** The core session */
    protected CoreSession session;

    /** A flag used to tell if we should consider referrals as standard entries */
    protected boolean throwReferral;


    /**
     * Creates a new instance of AbstractOperationContext.
     */
    public AbstractOperationContext( CoreSession session )
    {
        this.session = session;
        currentInterceptor = 0;
    }


    /**
     * Creates a new instance of AbstractOperationContext.
     *
     * @param dn The associated Dn
     */
    public AbstractOperationContext( CoreSession session, Dn dn )
    {
        this.dn = dn;
        this.session = session;

        // The flag is set to ignore, so that the revert operation can act on
        // the entries, even if they are referrals.
        ignoreReferral();
    }


    /**
     * {@inheritDoc}
     */
    public CoreSession getSession()
    {
        return session;
    }


    /**
     * {@inheritDoc}
     */
    public void setSession( CoreSession session )
    {
        this.session = session;
    }


    /**
     * {@inheritDoc}
     */
    protected void setAuthorizedPrincipal( LdapPrincipal authorizedPrincipal )
    {
        this.authorizedPrincipal = authorizedPrincipal;
    }


    /**
     * @return The associated Dn
     */
    public Dn getDn()
    {
        return dn;
    }


    /**
     * Set the context Dn
     *
     * @param dn The Dn to set
     */
    public void setDn( Dn dn )
    {
        this.dn = dn;
    }


    /**
     * {@inheritDoc}
     */
    public void addRequestControl( Control requestControl )
    {
        requestControls.put( requestControl.getOid(), requestControl );
    }


    /**
     * {@inheritDoc}
     */
    public Control getRequestControl( String numericOid )
    {
        return requestControls.get( numericOid );
    }


    /**
     * {@inheritDoc}
     */
    public boolean hasRequestControl( String numericOid )
    {
        return requestControls.containsKey( numericOid );
    }


    /**
     * {@inheritDoc}
     */
    public boolean hasRequestControls()
    {
        return ! requestControls.isEmpty();
    }


    /**
     * {@inheritDoc}
     */
    public void addResponseControl( Control responseControl )
    {
        responseControls.put( responseControl.getOid(), responseControl );
    }


    /**
     * {@inheritDoc}
     */
    public Control getResponseControl( String numericOid )
    {
        return responseControls.get( numericOid );
    }


    /**
     * {@inheritDoc}
     */
    public boolean hasResponseControl( String numericOid )
    {
        return responseControls.containsKey( numericOid );
    }


    /**
     * {@inheritDoc}
     */
    public Control[] getResponseControls()
    {
        if ( responseControls.isEmpty() )
        {
            return EMPTY_CONTROLS;
        }

        return responseControls.values().toArray( EMPTY_CONTROLS );
    }


    /**
     * {@inheritDoc}
     */
    public boolean hasResponseControls()
    {
        return ! responseControls.isEmpty();
    }


    /**
     * {@inheritDoc}
     */
    public int getResponseControlCount()
    {
        return responseControls.size();
    }


    /**
     * {@inheritDoc}
     */
    public void addRequestControls( Control[] requestControls )
    {
        for ( Control c : requestControls )
        {
            this.requestControls.put( c.getOid(), c );
        }
    }


    /**
     * {@inheritDoc}
     */
    public void setRequestControls( Map<String, Control> requestControls )
    {
        this.requestControls = requestControls;
    }


    /**
     * @return the operation name
     */
    public abstract String getName();


    /**
     * @return the originalEntry
     */
    public Entry getOriginalEntry()
    {
        return originalEntry;
    }


    /**
     * @param originalEntry the originalEntry to set
     */
    public void setOriginalEntry( Entry originalEntry )
    {
        this.originalEntry = originalEntry;
    }


    /**
     * {@inheritDoc}
     */
    public final void setInterceptors( List<String> interceptors )
    {
        this.interceptors = interceptors;
    }


    /**
     * {@inheritDoc}
     */
    public final String getNextInterceptor()
    {
        if ( currentInterceptor == interceptors.size() )
        {
            return "FINAL";
        }

        String interceptor = interceptors.get( currentInterceptor );
        currentInterceptor++;

        return interceptor;
    }
    
    
    /**
     * @return The number of the current interceptor in the list
     */
    public int getCurrentInterceptor()
    {
        return currentInterceptor;
    }
    
    
    /**
     * Sets the current interceptor number to a new value.
     * 
     * @param currentInterceptor The new current interceptor value
     */
    public void setCurrentInterceptor( int currentInterceptor )
    {
        this.currentInterceptor = currentInterceptor;
    }


    private void setup( AbstractOperationContext opContext )
    {
        opContext.setAuthorizedPrincipal( authorizedPrincipal );
    }


    /**
     * {@inheritDoc}
     */
    public void delete( Dn dn ) throws LdapException
    {
        DeleteOperationContext deleteContext = new DeleteOperationContext( session, dn );
        setup( deleteContext );
        session.getDirectoryService().getOperationManager().delete( deleteContext );
    }


    /**
     * {@inheritDoc}
     */
    public Entry lookup( LookupOperationContext lookupContext ) throws LdapException
    {
        return session.getDirectoryService().getOperationManager().lookup( lookupContext );
    }


    // TODO - need synchronization here and where we update links
    /**
     * {@inheritDoc}
     */
    public LookupOperationContext newLookupContext( Dn dn )
    {
        LookupOperationContext lookupContext = new LookupOperationContext( session, dn );
        setup( lookupContext );
        
        return lookupContext;
    }

    
    /**
     * {@inheritDoc}
     */
    public LdapPrincipal getEffectivePrincipal()
    {
        if ( authorizedPrincipal != null )
        {
            return authorizedPrincipal;
        }

        return session.getEffectivePrincipal();
    }


    // -----------------------------------------------------------------------
    // OperationContext Linked List Methods
    // -----------------------------------------------------------------------
    /**
     * @param entry the entry to set
     */
    public void setEntry( Entry entry )
    {
        this.entry = entry;
    }


    /**
     * @return the entry
     */
    public Entry getEntry()
    {
        return entry;
    }


    /**
     * Set the throwReferral flag to true
     */
    public void throwReferral()
    {
        throwReferral = true;
    }


    /**
     * @return <code>true</code> if the referrals are thrown
     */
    public boolean isReferralThrown()
    {
        return throwReferral;
    }


    /**
     * Set the throwReferral flag to false
     */
    public void ignoreReferral()
    {
        throwReferral = false;
    }


    /**
     * @return <code>true</code> if the referrals are ignored
     */
    public boolean isReferralIgnored()
    {
        return !throwReferral;
    }
}
