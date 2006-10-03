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
package org.apache.directory.shared.ldap.codec;


import java.util.HashSet;
import java.util.Set;

import org.apache.directory.shared.asn1.ber.AbstractContainer;
import org.apache.directory.shared.asn1.ber.IAsn1Container;
import org.apache.directory.shared.ldap.util.StringTools;


/**
 * The LdapMessage container stores all the messages decoded by the Asn1Decoder.
 * When dealing whith an incoding PDU, we will obtain a LdapMessage in the
 * ILdapContainer.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class LdapMessageContainer extends AbstractContainer implements IAsn1Container
{
    // ~ Instance fields
    // ----------------------------------------------------------------------------

    /** The ldap message */
    private LdapMessage ldapMessage;

    /** A HashSet which contaons the binary attributes */
    private Set binaries;
    
    /** The message ID */
    private int messageId;
    
    /** The current control */
    private Control currentControl;

    // ~ Constructors
    // -------------------------------------------------------------------------------

    /**
     * Creates a new LdapMessageContainer object. We will store ten grammars,
     * it's enough ...
     */
    public LdapMessageContainer()
    {
        this( new HashSet() );
    }


    /**
     * Creates a new LdapMessageContainer object. We will store ten grammars,
     * it's enough ...
     */
    public LdapMessageContainer( Set binaries )
    {
        super();
        stateStack = new int[10];
        grammar = LdapMessageGrammar.getInstance();
        states = LdapStatesEnum.getInstance();

        this.binaries = binaries;
    }


    // ~ Methods
    // ------------------------------------------------------------------------------------
    /**
     * @return Returns the ldapMessage.
     */
    public LdapMessage getLdapMessage()
    {
        return ldapMessage;
    }


    /**
     * Set a ldapMessage Object into the container. It will be completed by the
     * ldapDecoder .
     * 
     * @param ldapMessage The message to set.
     */
    public void setLdapMessage( LdapMessage ldapMessage )
    {
        this.ldapMessage = ldapMessage;
    }


    public void clean()
    {
        super.clean();

        ldapMessage = null;
        messageId = 0;
        currentControl = null;
    }


    /**
     * @return Returns true if the attribute is binary.
     */
    public boolean isBinary( String id )
    {
        return binaries.contains( StringTools.lowerCase( StringTools.trim( id ) ) );
    }

    /**
     * @return The message ID
     */
    public int getMessageId()
    {
        return messageId;
    }

    /**
     * Set the message ID
     */
    public void setMessageId( int messageId )
    {
        this.messageId = messageId;
    }

    /**
     * @return the current control being created
     */
    public Control getCurrentControl()
    {
        return currentControl;
    }

    /**
     * Store a newly created control
     * @param currentControl The control to store
     */
    public void setCurrentControl( Control currentControl )
    {
        this.currentControl = currentControl;
    }
}
