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


import java.util.Map;

import org.apache.directory.shared.ldap.client.api.exception.LdapException;
import javax.naming.ldap.Control;


/**
 * Root interface for all LDAP message type interfaces.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public interface Message
{
    /**
     * Get the request messageId
     *
     * @return The request message ID
     */
    int getMessageId();
    
    
    /**
     * Set the request message ID
     *
     * @param messageId The request message ID
     */
    void setMessageId( int messageId );
    
    
    /**
     * Gets the controls associated with this message mapped by OID.
     * 
     * @return Map of OID strings to Control object instances.
     * @see Control
     */
    Map<String, Control> getControls();

    
    /**
     * Gets the control with a specific OID.
     * 
     * @return The Control with the specified OID
     * @see Control
     */
    Control getControl( String oid );

    
    /**
     * Checks whether or not this message has the specified control.
     *
     * @param oid the OID of the control
     * @return true if this message has the control, false if it does not
     */
    boolean hasControl( String oid );
    

    /**
     * Adds controls to this Message.
     * 
     * @param controls the controls to add.
     * @throws MessageException if controls cannot be added to this Message 
     * or the control is not known etc.
     */
    Message add( Control... controls ) throws LdapException;


    /**
     * Deletes controls, removing them from this Message.
     * 
     * @param controls the controls to remove.
     * @throws LdapException if controls cannot be added to this Message 
     * or the control is not known etc.
     */
    Message remove( Control... control ) throws LdapException;
}
