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


import javax.naming.ldap.Control;

import org.apache.directory.shared.ldap.name.LdapDN;


/**
 * This interface represent the context passed as an argument to each interceptor.
 * It will contain data used by all the operations.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public interface OperationContext
{
    /**
     * @return The associated DN
     */
    LdapDN getDn();
    
    
    /**
     * Set the context DN
     *
     * @param dn The DN to set
     */
    void setDn( LdapDN dn );

    
    /**
     * Adds a response control to this operation.
     *
     * @param responseControl the response control to add to this operation.
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
     * @param requestControl the request control to add to this operation.
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
     * Gets a request control if present for this request.
     * 
     * @param numericOid the numeric OID of the control also known as it's type OID
     * @return the control if present
     */
    Control getRequestControl( String numericOid );


    /**
     * Adds many request controls to this operation.
     *
     * @param requestControls the request controls to add to this operation.
     */
    void addRequestControls( Control[] requestControls );
}
