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

import java.util.HashMap;
import java.util.Map;
import javax.naming.ldap.Control;

import org.apache.directory.shared.ldap.client.api.exception.LdapException;


/**
 * Abstract message base class.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev: 760984 $
 */
public abstract class AbstractMessage implements Message
{
    /** Map of message controls using OID Strings for keys and Control values */
    private Map<String, Control> controls;

    /** The session unique message sequence identifier */
    private int messageId = -1;

    /**
     * Completes the instanciation of a Message.
     * 
     * @param messageId the seq id of the message
     */
    protected AbstractMessage()
    {
        controls = new HashMap<String, Control>();
    }


    /**
     * Gets the session unique message sequence id for this message. Requests
     * and their responses if any have the same message id. Clients at the
     * initialization of a session start with the first message's id set to 1
     * and increment it with each transaction.
     * 
     * @return the session unique message id.
     */
    public int getMessageId()
    {
        return messageId;
    }


    /**
     * {@inheritDoc}
     *
     * @param messageId
     */
    public void setMessageId( int messageId )
    {
        this.messageId = messageId;
    }


    /**
     * {@inheritDoc}
     */
    public Message add( Control... controls ) throws LdapException
    {
        if ( this.controls == null )
        {
            this.controls = new HashMap<String, Control>();
        }
        
        if ( controls != null )
        {
            for ( Control control:controls )
            {
                this.controls.put( control.getID(), control );
            }
        }
        
        return this;
    }


    /**
     * {@inheritDoc}
     */
    public Map<String, Control> getControls()
    {
        return controls;
    }


    /**
     * Gets the control with a specific OID.
     * 
     * @return The Control with the specified OID
     */
    public Control getControl( String oid )
    {
        return controls.get( oid );
    }

    
    /**
     * {@inheritDoc}
     */
    public boolean hasControl( String oid )
    {
        return ( controls != null ) && ( controls.size() > 0 );
    }


    /**
     * {@inheritDoc}
     */
    public Message remove( Control... controls ) throws LdapException
    {
        if ( this.controls == null )
        {
            // We don't have any controls, so we can just exit
            return this;
        }
        
        if ( controls != null )
        {
            for ( Control ctrl:controls )
            {
                this.controls.remove( ctrl.getID() );
            }
        }
        
        return this;
    }
    
    
    /**
     * Get a String representation of a LdapMessage
     * 
     * @return A LdapMessage String
     */
    public String toString()
    {
        StringBuffer sb = new StringBuffer();

        sb.append( "LdapMessage\n" );
        sb.append( "    message Id : " ).append( messageId ).append( '\n' );

        if ( controls != null )
        {
            for ( Control control:controls.values() )
            {
                sb.append( control );
            }
        }

        return sb.toString();
    }
}
