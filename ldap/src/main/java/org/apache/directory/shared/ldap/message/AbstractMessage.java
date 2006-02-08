/*
 *   Copyright 2004 The Apache Software Foundation
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
package org.apache.directory.shared.ldap.message;


import java.util.*;


/**
 * Abstract message base class.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class AbstractMessage implements Message
{
    static final long serialVersionUID = 7601738291101182094L;

    /** Map of message controls using OID Strings for keys and Control values */
    private final Map controls;

    /** The session unique message sequence identifier */
    private final int id;

    /** The message type enumeration */
    private final MessageTypeEnum type;

    /** Transient Message Parameter Hash */
    private final Map parameters;


    /**
     * Completes the instantiation of a Message.
     * 
     * @param id
     *            the seq id of the message
     * @param type
     *            the type of the message
     */
    protected AbstractMessage(final int id, final MessageTypeEnum type)
    {
        this.id = id;
        this.type = type;
        controls = new HashMap();
        parameters = new HashMap();
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
        return id;
    }


    /**
     * Gets the controls associated with this message mapped by OID.
     * 
     * @return Map of OID strings to Control object instances.
     * @see Control
     */
    public Map getControls()
    {
        return Collections.unmodifiableMap( controls );
    }


    /**
     * Adds a control to this Message.
     * 
     * @param control
     *            the control to add.
     * @throws MessageException
     *             if controls cannot be added to this Message or the control is
     *             not known etc.
     */
    public void add( Control control ) throws MessageException
    {
        controls.put( control.getType(), control );
    }


    /**
     * Deletes a control removing it from this Message.
     * 
     * @param control
     *            the control to remove.
     * @throws MessageException
     *             if controls cannot be added to this Message or the control is
     *             not known etc.
     */
    public void remove( Control control ) throws MessageException
    {
        controls.remove( control.getType() );
    }


    /**
     * Gets the LDAP message type code associated with this Message. Each
     * request and response type has a unique message type code defined by the
     * protocol in <a href="http://www.faqs.org/rfcs/rfc2251.html">RFC 2251</a>.
     * 
     * @return the message type code.
     */
    public MessageTypeEnum getType()
    {
        return type;
    }


    /**
     * Gets a message scope parameter. Message scope parameters are temporary
     * variables associated with a message and are set locally to be used to
     * associate housekeeping information with a request or its processing.
     * These parameters are never transmitted nor recieved, think of them as
     * transient data associated with the message or its processing. These
     * transient parameters are not locked down so modifications can occur
     * without firing LockExceptions even when this Lockable is in the locked
     * state.
     * 
     * @param key
     *            the key used to access a message parameter.
     * @return the transient message parameter value.
     */
    public Object get( Object key )
    {
        return parameters.get( key );
    }


    /**
     * Sets a message scope parameter. These transient parameters are not locked
     * down so modifications can occur without firing LockExceptions even when
     * this Lockable is in the locked state.
     * 
     * @param key
     *            the parameter key
     * @param value
     *            the parameter value
     * @return the old value or null
     */
    public Object put( Object key, Object value )
    {
        return parameters.put( key, value );
    }


    /**
     * Checks to see if two messages are equivalent. Messages equivalence does
     * not factor in parameters accessible through the get() and put()
     * operations, nor do they factor in the Lockable properties of the Message.
     * Only the type, controls, and the messageId are evaluated for equality.
     * 
     * @param obj
     *            the object to compare this Message to for equality
     */
    public boolean equals( Object obj )
    {
        if ( obj == this )
        {
            return true;
        }

        if ( !( obj instanceof Message ) )
        {
            return false;
        }

        Message msg = ( Message ) obj;

        if ( msg.getMessageId() != id )
        {
            return false;
        }

        if ( msg.getType() != type )
        {
            return false;
        }

        Map controls = msg.getControls();
        if ( controls.size() != this.controls.size() )
        {
            return false;
        }

        Iterator list = this.controls.keySet().iterator();
        while ( list.hasNext() )
        {
            if ( !controls.containsKey( list.next() ) )
            {
                return false;
            }
        }

        return true;
    }
}
