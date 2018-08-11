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

import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.directory.api.ldap.model.message.Control;
import org.apache.directory.api.ldap.model.name.Dn;
import org.apache.directory.server.core.api.CoreSession;
import org.apache.directory.server.core.api.LdapPrincipal;
import org.apache.directory.server.core.api.partition.Partition;
import org.apache.directory.server.core.api.partition.PartitionTxn;


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
    protected Map<String, Control> requestControls = new HashMap<>( 4 );

    /** The associated response's controls */
    protected Map<String, Control> responseControls = new HashMap<>( 4 );

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
    
    /** The transaction this operation is ran into */
    protected PartitionTxn transaction;
    
    /** The partition this operation will be applied on */
    protected Partition partition;


    /**
     * Creates a new instance of AbstractOperationContext.
     * 
     * @param session The session to use
     */
    public AbstractOperationContext( CoreSession session )
    {
        this.session = session;
        currentInterceptor = 0;
    }


    /**
     * Creates a new instance of AbstractOperationContext.
     *
     * @param session The session to use
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
    @Override
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
    @Override
    public Dn getDn()
    {
        return dn;
    }


    /**
     * Set the context Dn
     *
     * @param dn The Dn to set
     */
    @Override
    public void setDn( Dn dn )
    {
        this.dn = dn;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void addRequestControl( Control requestControl )
    {
        requestControls.put( requestControl.getOid(), requestControl );
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public Control getRequestControl( String numericOid )
    {
        return requestControls.get( numericOid );
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public boolean hasRequestControl( String numericOid )
    {
        return requestControls.containsKey( numericOid );
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public boolean hasRequestControls()
    {
        return !requestControls.isEmpty();
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void addResponseControl( Control responseControl )
    {
        responseControls.put( responseControl.getOid(), responseControl );
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public Control getResponseControl( String numericOid )
    {
        return responseControls.get( numericOid );
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public boolean hasResponseControl( String numericOid )
    {
        return responseControls.containsKey( numericOid );
    }


    /**
     * {@inheritDoc}
     */
    @Override
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
    @Override
    public boolean hasResponseControls()
    {
        return !responseControls.isEmpty();
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public int getResponseControlCount()
    {
        return responseControls.size();
    }


    /**
     * {@inheritDoc}
     */
    @Override
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
    @Override
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
    @Override
    public final void setInterceptors( List<String> interceptors )
    {
        this.interceptors = interceptors;
    }


    /**
     * {@inheritDoc}
     */
    @Override
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
    @Override
    public int getCurrentInterceptor()
    {
        return currentInterceptor;
    }


    /**
     * Sets the current interceptor number to a new value.
     * 
     * @param currentInterceptor The new current interceptor value
     */
    @Override
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
    @Override
    public void delete( Dn dn ) throws LdapException
    {
        DeleteOperationContext deleteContext = new DeleteOperationContext( session, dn );
        setup( deleteContext );
        session.getDirectoryService().getOperationManager().delete( deleteContext );
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public Entry lookup( LookupOperationContext lookupContext ) throws LdapException
    {
        return session.getDirectoryService().getOperationManager().lookup( lookupContext );
    }


    // TODO - need synchronization here and where we update links
    /**
     * {@inheritDoc}
     */
    @Override
    public LookupOperationContext newLookupContext( Dn dn, String... attributes )
    {
        LookupOperationContext lookupContext = new LookupOperationContext( session, dn, attributes );
        setup( lookupContext );

        return lookupContext;
    }


    /**
     * {@inheritDoc}
     */
    @Override
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
    @Override
    public void setEntry( Entry entry )
    {
        this.entry = entry;
    }


    /**
     * @return the entry
     */
    @Override
    public Entry getEntry()
    {
        return entry;
    }


    /**
     * Set the throwReferral flag to true
     */
    @Override
    public void throwReferral()
    {
        throwReferral = true;
    }


    /**
     * @return <code>true</code> if the referrals are thrown
     */
    @Override
    public boolean isReferralThrown()
    {
        return throwReferral;
    }


    /**
     * Set the throwReferral flag to false
     */
    @Override
    public void ignoreReferral()
    {
        throwReferral = false;
    }


    /**
     * @return <code>true</code> if the referrals are ignored
     */
    @Override
    public boolean isReferralIgnored()
    {
        return !throwReferral;
    }


    /**
     * @return the transaction
     */
    @Override
    public PartitionTxn getTransaction()
    {
        return transaction;
    }


    /**
     * @param transaction the transaction to set
     */
    @Override
    public void setTransaction( PartitionTxn transaction )
    {
        this.transaction = transaction;
    }
    
    
    /**
     * {@inheritDoc}
     */
    @Override
    public Partition getPartition()
    {
        return partition;
    }
    
    
    /**
     * {@inheritDoc}
     */
    @Override
    public void setPartition( Partition partition )
    {
        this.partition = partition;
    }
}
