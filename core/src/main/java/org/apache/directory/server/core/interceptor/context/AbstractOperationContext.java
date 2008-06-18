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
package org.apache.directory.server.core.interceptor.context;


import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.naming.ldap.Control;

import org.apache.directory.server.core.CoreSession;
import org.apache.directory.server.core.ReferralHandlingMode;
import org.apache.directory.server.core.authn.LdapPrincipal;
import org.apache.directory.server.core.entry.ClonedServerEntry;
import org.apache.directory.server.core.entry.ServerEntry;
import org.apache.directory.shared.ldap.entry.Modification;
import org.apache.directory.shared.ldap.message.ManageDsaITControl;
import org.apache.directory.shared.ldap.message.Request;
import org.apache.directory.shared.ldap.name.LdapDN;


/**
 * This abstract class stores common context elements, like the DN, which is used
 * in all the contexts.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public abstract class AbstractOperationContext implements OperationContext
{
    protected static final Control[] EMPTY_CONTROLS = new Control[0];

    /** The DN associated with the context */
    protected LdapDN dn;
    
    /** The entry associated with the target entry of this OperationContext */
    private ClonedServerEntry entry;
    
    /** The associated request's controls */
    protected Map<String, Control> requestControls = new HashMap<String, Control>(4);

    /** The associated response's controls */
    protected Map<String, Control> responseControls = new HashMap<String, Control>(4);

    /** the Interceptors bypassed by this operation */
    protected Collection<String> byPassed;
    
    protected LdapPrincipal authorizedPrincipal;
    
    /** an operation can override the session's referral handling mode */
    protected ReferralHandlingMode referralHandlingMode;
    
    protected CoreSession session;
    
    protected OperationContext next;
    
    protected OperationContext previous;


    /**
     * Creates a new instance of AbstractOperationContext.
     */
    public AbstractOperationContext( CoreSession session )
    {
        this.session = session;
    }
    
    
    /**
     * Creates a new instance of AbstractOperationContext.
     *
     * @param dn The associated DN
     */
    public AbstractOperationContext( CoreSession session, LdapDN dn )
    {
        this.dn = dn;
        this.session = session;
    }


    public CoreSession getSession()
    {
        return session;
    }
    
    
    protected void setSession( CoreSession session )
    {
        this.session = session;
    }
    
    
    protected void setAuthorizedPrincipal( LdapPrincipal authorizedPrincipal )
    {
        this.authorizedPrincipal = authorizedPrincipal;
    }


    /**
     * @return The associated DN
     */
    public LdapDN getDn()
    {
        return dn;
    }

    
    /**
     * Set the context DN
     *
     * @param dn The DN to set
     */
    public void setDn( LdapDN dn )
    {
        this.dn = dn;
    }

    
    public void addRequestControl( Control requestControl )
    {
        requestControls.put( requestControl.getID(), requestControl );
    }

    
    public Control getRequestControl( String numericOid )
    {
        return requestControls.get( numericOid );
    }

    
    public boolean hasRequestControl( String numericOid )
    {
        return requestControls.containsKey( numericOid );
    }

    
    public boolean hasRequestControls()
    {
        return ! requestControls.isEmpty();
    }


    public void addResponseControl( Control responseControl )
    {
        responseControls.put( responseControl.getID(), responseControl );
    }


    public Control getResponseControl( String numericOid )
    {
        return responseControls.get( numericOid );
    }


    public boolean hasResponseControl( String numericOid )
    {
        return responseControls.containsKey( numericOid );
    }


    public Control[] getResponseControls()
    {
        if ( responseControls.isEmpty() )
        {
            return EMPTY_CONTROLS;
        }
        
        return responseControls.values().toArray( EMPTY_CONTROLS );
    }


    public boolean hasResponseControls()
    {
        return ! responseControls.isEmpty();
    }


    public int getResponseControlCount()
    {
        return responseControls.size();
    }


    public void addRequestControls( Control[] requestControls )
    {
        for ( Control c : requestControls )
        {
            this.requestControls.put( c.getID(), c );
        }
    }

    
    public void setRequestControls( Map<String, Control> requestControls )
    {
        this.requestControls = requestControls;
    }

    
    /**
     * @return the operation name
     */
    public abstract String getName();


    /**
     * Gets the set of bypassed Interceptors.
     *
     * @return the set of bypassed Interceptors
     */
    public Collection<String> getByPassed()
    {
        if ( byPassed == null )
        {
            return Collections.emptyList();
        }
        
        return Collections.unmodifiableCollection( byPassed );
    }
    
    
    /**
     * Sets the set of bypassed Interceptors.
     * 
     * @param byPassed the set of bypassed Interceptors
     */
    public void setByPassed( Collection<String> byPassed )
    {
        this.byPassed = byPassed;
    }

    
    /**
     * Checks to see if an Interceptor is bypassed for this operation.
     *
     * @param interceptorName the interceptorName of the Interceptor to check for bypass
     * @return true if the Interceptor should be bypassed, false otherwise
     */
    public boolean isBypassed( String interceptorName )
    {
        return byPassed != null && byPassed.contains( interceptorName );
    }


    /**
     * Checks to see if any Interceptors are bypassed by this operation.
     *
     * @return true if at least one bypass exists
     */
    public boolean hasBypass()
    {
        return byPassed != null && !byPassed.isEmpty();
    }

    
    private void setup( AbstractOperationContext opContext )
    {
        opContext.setPreviousOperation( this );
        next = opContext;
        opContext.setByPassed( byPassed );
        opContext.setAuthorizedPrincipal( authorizedPrincipal );
    }
    
    
    public boolean hasEntry( LdapDN dn, Collection<String> byPassed ) throws Exception
    {
        EntryOperationContext opContext = new EntryOperationContext( session, dn );
        setup( opContext );
        opContext.setByPassed( byPassed );
        return session.getDirectoryService().getOperationManager().hasEntry( opContext );
    }
    
    
    public void add( ServerEntry entry, Collection<String> byPassed ) throws Exception
    {
        AddOperationContext opContext = new AddOperationContext( session, entry );
        setup( opContext );
        opContext.setByPassed( byPassed );
        session.getDirectoryService().getOperationManager().add( opContext );
    }
    
    
    public void delete( LdapDN dn, Collection<String> byPassed ) throws Exception
    {
        DeleteOperationContext opContext = new DeleteOperationContext( session, dn );
        setup( opContext );
        opContext.setByPassed( byPassed );
        session.getDirectoryService().getOperationManager().delete( opContext );
    }
    
    
    public void modify( LdapDN dn, List<Modification> mods, Collection<String> byPassed ) throws Exception
    {
        ModifyOperationContext opContext = new ModifyOperationContext( session, dn, mods );
        setup( opContext );
        opContext.setByPassed( byPassed );
        session.getDirectoryService().getOperationManager().modify( opContext );
    }
    
    
    // TODO - need synchronization here and where we update links
    public LookupOperationContext newLookupContext( LdapDN dn )
    {
        LookupOperationContext opContext = new LookupOperationContext( session, dn );
        setup( opContext );
        return opContext;
    }


    public ClonedServerEntry lookup( LookupOperationContext opContext ) throws Exception
    {
        if ( opContext != next )
        {
            throw new IllegalStateException( "Cannot execute indirect lookup if it is not the next operation." );
        }
        return session.getDirectoryService().getOperationManager().lookup( opContext );
    }


    public ClonedServerEntry lookup( LdapDN dn, Collection<String> byPassed ) throws Exception
    {
        LookupOperationContext opContext = newLookupContext( dn );
        opContext.setByPassed( byPassed );
        return session.getDirectoryService().getOperationManager().lookup( opContext );
    }
    

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
    
    
    public boolean isFirstOperation()
    {
        return previous == null;
    }
    
    
    public OperationContext getFirstOperation()
    {
        if ( previous == null )
        {
            return this;
        }
        
        return previous.getFirstOperation();
    }
    
    
    public OperationContext getLastOperation()
    {
        if ( next == null )
        {
            return this;
        }
        
        return next.getLastOperation();
    }
    
    
    public OperationContext getNextOperation()
    {
        return next;
    }
    
    
    protected void setNextOperation( OperationContext next )
    {
        this.next = next;
    }
    
    
    public OperationContext getPreviousOperation()
    {
        return previous;
    }
    
    
    protected void setPreviousOperation( OperationContext previous )
    {
        this.previous = previous;
    }


    /**
     * @param referralHandlingMode the referralHandlingMode to set
     */
    public void setReferralHandlingMode( ReferralHandlingMode referralHandlingMode )
    {
        this.referralHandlingMode = referralHandlingMode;
    }


    /**
     * @return the referralHandlingMode
     */
    public ReferralHandlingMode getReferralHandlingMode()
    {
        return referralHandlingMode;
    }
    
    
    protected void setReferralHandlingMode( Request req )
    {
        if ( req.hasControl( ManageDsaITControl.CONTROL_OID ) )
        {
            this.referralHandlingMode = ReferralHandlingMode.IGNORE;
        }
        else
        {
            this.referralHandlingMode = ReferralHandlingMode.THROW;
        }
    }


    /**
     * @param entry the entry to set
     */
    public void setEntry( ClonedServerEntry entry )
    {
        this.entry = entry;
    }


    /**
     * @return the entry
     */
    public ClonedServerEntry getEntry()
    {
        return entry;
    }
}
