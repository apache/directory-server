/*
 * $Id: EventManager.java,v 1.3 2003/03/13 18:27:26 akarasulu Exp $
 *
 * -- (c) LDAPd Group                                                    --
 * -- Please refer to the LICENSE.txt file in the root directory of      --
 * -- any LDAPd project for copyright and distribution information.      --
 *
 */

package org.apache.eve.event.protocol ;


/**
 * The service interface for a synchronous protocol event manager.
 */
public interface EventManager
{
    /** The ROLE is the fully qualified name of this interface */
    public static final String ROLE = EventManager.class.getName() ;

	/**
     * Synchronously fires a protocol event calling the event handler of all
     * registered listeners for the event type of a_event after the operatation
     * successfully takes place.
     *
     * @param a_event the event to fire after the operation
     */
    void fireAfter(ProtocolEvent a_event) ;

	/**
     * Synchronously fires a protocol event calling the event handler of all
     * registered listeners for the event type of a_event before the operatation
     * takes place.
     *
     * @param a_event the event to fire before the operation
     */
    void fireBefore(ProtocolEvent a_event) ;

    /**
     * Checks to see if a listener is registered for event delivery of this
     * kind specified by a_mask.
     */
    boolean hasListener(int a_mask) ;

    /**
     * Adds a protocol event listener registering it for events that would be
     * accepted by a_mask.
     *
     * @param a_listener the protocol listener to register.
     * @param a_mask used to determine which events the listener will recieve.
     */
    void addListener(ProtocolListener a_listener, int a_mask) ;

    /**
     * Adds a protocol event listener registering it for events that would be
     * accepted by a_mask.
     *
     * @param a_listener the protocol listener to register.
     * @param a_mask used to determine which events the listener will recieve.
     * @param whether or not errors in a_listener's event handler are trapped,
     * trapped errors cannot effect the outcome of the operation.
     */
    void addListener(ProtocolListener a_listener, int a_mask,
        boolean trapErrors) ;
}
