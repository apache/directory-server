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


import java.util.List;

import org.apache.directory.server.core.api.CoreSession;
import org.apache.directory.server.core.api.LdapPrincipal;
import org.apache.directory.server.core.api.entry.ClonedServerEntry;
import org.apache.directory.shared.ldap.model.entry.Entry;
import org.apache.directory.shared.ldap.model.exception.LdapException;
import org.apache.directory.shared.ldap.model.message.Control;
import org.apache.directory.shared.ldap.model.name.Dn;


/**
 * This interface represent the context passed as an argument to each interceptor.
 * It will contain data used by all the operations.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public interface OperationContext
{
    /**
     * @return The number of the current interceptor in the list
     */
    int getCurrentInterceptor();


    /**
     * Sets the current interceptor number to a new value.
     * 
     * @param currentInterceptor The new current interceptor value
     */
    void setCurrentInterceptor( int currentInterceptor );


    /**
     * Gets the effective principal for this operation which may not be the
     * same as the authenticated principal when the session for this context
     * has an explicit authorization id, or this operation was applied with
     * the proxy authorization control.
     * 
     * @see CoreSession#getAuthenticatedPrincipal()
     * @see CoreSession#getEffectivePrincipal()
     * @return the effective principal for this operation
     */
    LdapPrincipal getEffectivePrincipal();


    /**
     * @return The associated Dn
     */
    Dn getDn();


    /**
     * Set the context Dn
     *
     * @param dn The Dn to set
     */
    void setDn( Dn dn );


    /**
     * Gets the server entry associated with the target Dn of this
     * OperationContext.  The entry associated with the Dn may be altered
     * during the course of processing an LDAP operation through the
     * InterceptorChain.  This place holder is put here to prevent the need
     * for repetitive lookups of the target entry.  Furthermore the returned
     * entry may be altered by any Interceptor in the chain and this is why a
     * ClonedServerEntry is returned instead of a Entry.  A
     * ClonedServerEntry has an immutable reference to the original state of
     * the target entry.  The original state can be accessed via a call to
     * {@link ClonedServerEntry#getOriginalEntry()}.  The return value may be
     * null in which case any lookup performed to access it may set it to
     * prevent the need for subsequent lookups.
     * 
     * Also note that during the course of handling some operations such as
     * those that rename, move or rename and move the entry, may alter the Dn
     * of this entry.  Interceptor implementors should not presume the Dn or
     * the values contained in this entry are currently what is present in the
     * DIT.  The original entry contained in the ClonedServerEntry shoudl be
     * used as the definitive source of information about the state of the
     * entry in the DIT before returning from the Partition subsystem.
     * 
     * @return target entry associated with the Dn of this OperationContext
     */
    Entry getEntry();


    /**
     * Sets the server entry associated with the target Dn of this
     * OperationContext.
     *
     * @param entry the entry whose Dn is associated with this OperationContext.
     */
    void setEntry( Entry entry );


    /**
     * Adds a response control to this operation.
     *
     * @param responseControl the response control to add to this operation
     */
    void addResponseControl( Control responseControl );


    /**
     * Checks to see if a response control is present on this operation.
     *
     * @param numericOid the numeric OID of the control also known as it's type OID
     * @return true if the control is associated with this operation, false otherwise
     */
    boolean hasResponseControl( String numericOid );


    /**
     * Gets a response control if present for this request.
     * 
     * @param numericOid the numeric OID of the control also known as it's type OID
     * @return the control if present
     */
    Control getResponseControl( String numericOid );


    /**
     * Gets all the response controls producted during this operation.
     *
     * @return an array over all the response controls
     */
    Control[] getResponseControls();


    /**
     * Checks if any response controls have been generated for this operation.
     *
     * @return true if any response controls have been generated, false otherwise
     */
    boolean hasResponseControls();


    /**
     * Checks the number of response controls have been generated for this operation.
     *
     * @return the number of response controls that have been generated
     */
    int getResponseControlCount();


    /**
     * Adds a request control to this operation.
     *
     * @param requestControl the request control to add to this operation
     */
    void addRequestControl( Control requestControl );


    /**
     * Checks to see if a request control is present on this request.
     *
     * @param numericOid the numeric OID of the control also known as it's type OID
     * @return true if the control is associated with this operation, false otherwise
     */
    boolean hasRequestControl( String numericOid );


    /**
     * Checks if any request controls exists for this operation.
     *
     * @return true if any request controls exist, false otherwise
     */
    boolean hasRequestControls();


    /**
     * Gets a request control if present for this request.
     * 
     * @param numericOid the numeric OID of the control also known as it's type OID
     * @return the control if present
     */
    Control getRequestControl( String numericOid );


    /**
     * Adds many request controls to this operation.
     *
     * @param requestControls the request controls to add to this operation
     */
    void addRequestControls( Control[] requestControls );


    /**
     * @return the operation's name
     */
    String getName();


    /**
     * Gets the next interceptor in the list of interceptors. The
     * position in the list will be incremented.
     * 
     * @return The next interceptor from the list of interceptors
     */
    String getNextInterceptor();


    /**
     * Sets the list of interceptors to go through for an operation
     * 
     * @param interceptors The list of interceptors
     */
    void setInterceptors( List<String> interceptors );


    /**
     * Gets the session associated with this operation.
     *
     * @return the session associated with this operation
     */
    CoreSession getSession();


    // -----------------------------------------------------------------------
    // Utility Factory Methods to Create New OperationContexts
    // -----------------------------------------------------------------------
    LookupOperationContext newLookupContext( Dn dn, String... attributes );


    Entry lookup( LookupOperationContext lookupContext ) throws LdapException;


    /**
     * Process the delete for inner operations. This is only valid for SubschemaSubentry
     * operations, and will most certainly be removed later.
     * 
     * @param dn
     * @throws LdapException
     */
    void delete( Dn dn ) throws LdapException;


    /**
     * Set the throwReferral flag to true
     */
    void throwReferral();


    /**
     * @return <code>true</code> if the referrals are thrown
     */
    boolean isReferralThrown();


    /**
     * Set the throwReferral flag to false
     */
    void ignoreReferral();


    /**
     * @return <code>true</code> if the referrals are ignored
     */
    boolean isReferralIgnored();
}
