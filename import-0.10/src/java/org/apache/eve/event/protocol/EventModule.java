/*
 * $Id: EventModule.java,v 1.5 2003/03/13 18:27:26 akarasulu Exp $
 *
 * -- (c) LDAPd Group                                                    --
 * -- Please refer to the LICENSE.txt file in the root directory of      --
 * -- any LDAPd project for copyright and distribution information.      --
 *
 */

package org.apache.eve.event.protocol ;


import java.util.HashMap ;
import java.util.Iterator ;

import org.apache.eve.AbstractModule ;

import org.apache.avalon.framework.configuration.Configuration ;


/**
 * Default synchronous event manager implementation for the server.
 * 
 * @phoenix:block
 * @phoenix:service name="org.apache.eve.event.protocol.EventManager"
 * @phoenix:mx-topic name="backend-nexus"
 */
public class EventModule
    extends AbstractModule
    implements EventManager
{
    /** Map of listeners to their Listener spec. */
	private HashMap m_listeners = new HashMap() ;
    /**
     * Current mask used to determine if an event listener of a specific type
     * is registered to recieve events.  This mask is constructed by doing a
     * comulative bitwise OR on it and the registered event masks of a listener.
     */
    private int m_mask = 0 ;


    /**
     * Adds a protocol event listener registering it for events that would be
     * accepted by a_mask.
     *
     * @param a_listener the protocol listener to register.
     * @param a_mask used to determine which events the listener will recieve.
     * @param whether or not errors in a_listener's event handler are trapped,
     * trapped errors cannot effect the outcome of the operation.
     */
    public void addListener(ProtocolListener a_listener, int a_mask,
        boolean trapErrors)
    {
		if(!ProtocolEvent.isValid(a_mask)) {
			throw new IllegalArgumentException("Invalid mask parameter.") ;
        }

		ListenerSpec l_spec = new ListenerSpec() ;
		l_spec.mask = a_mask ;
		l_spec.trapErrors = trapErrors ;

        synchronized(m_listeners) {
            m_listeners.put(a_listener, l_spec) ;
            m_mask |= a_mask ; 
        }
    }


    /**
     * Adds a protocol event listener registering it for events that would be
     * accepted by a_mask.
     *
     * @param a_listener the protocol listener to register.
     * @param a_mask used to determine which events the listener will recieve.
     */
    public void addListener(ProtocolListener a_listener, int a_mask)
    {
		if(!ProtocolEvent.isValid(a_mask)) {
			throw new IllegalArgumentException("Invalid mask parameter.") ;
        }

		ListenerSpec l_spec = new ListenerSpec() ;
		l_spec.mask = a_mask ;

        synchronized(m_listeners) {
            m_listeners.put(a_listener, l_spec) ;
            m_mask |= a_mask ; 
        }
    }


    public boolean hasListener(int a_mask)
    {
        return (m_mask & a_mask) == a_mask ;
    }


	/**
     * Synchronously fires a protocol event calling the event handler of all
     * registered listeners for the event type of a_event after the operatation
     * successfully takes place.
     *
     * @param a_event the event to fire after the operation
     */
    public void fireAfter(ProtocolEvent a_event)
    {
		Iterator l_list = m_listeners.keySet().iterator() ;
		while(l_list.hasNext()) {
			ProtocolListener l_listener = (ProtocolListener) l_list.next() ;
			ListenerSpec l_spec = (ListenerSpec)
				m_listeners.get(l_listener) ;

			// If the listener is registered for the a_event type
			if(a_event.accepts(l_spec.mask)) {
				if(l_spec.trapErrors) {
					try {
						afterOn(l_listener, a_event) ;
					} catch(Throwable t) {
						super.getLogger().error("Listener " + l_listener
							+ " failed to successfully handle event "
							+ a_event) ;
					}
				} else {
					try {
						afterOn(l_listener, a_event) ;
					} catch(RuntimeException re) {
						throw re ;
					}
				}
			}
        }
    }


	/**
     * Synchronously fires a protocol event calling the event handler of all
     * registered listeners for the event type of a_event before the operatation
     * takes place.
     *
     * @param a_event the event to fire before the operation
     */
    public void fireBefore(ProtocolEvent a_event)
    {
		Iterator l_list = m_listeners.keySet().iterator() ;
		while(l_list.hasNext()) {
			ProtocolListener l_listener = (ProtocolListener) l_list.next() ;
			ListenerSpec l_spec = (ListenerSpec)
				m_listeners.get(l_listener) ;

			// If the listener is registered for the a_event type
			if(a_event.accepts(l_spec.mask)) {
				if(l_spec.trapErrors) {
					try {
						beforeOn(l_listener, a_event) ;
					} catch(Throwable t) {
						super.getLogger().error("Listener " + l_listener
							+ " failed to successfully handle event "
							+ a_event) ;
					}
				} else {
					try {
						beforeOn(l_listener, a_event) ;
					} catch(RuntimeException re) {
						throw re ;
					}
				}
			}
		}
    }


    /**
     * Calls the after() method on a_listener using a_event as the argument
     * using a switch on the event mask to cast the event to the appropriate
     * event class type.
     * 
     * @param a_listener to call the after method on
     * @param a_event the protocol event to use when calling after()
     */
    private void afterOn(ProtocolListener a_listener, ProtocolEvent a_event)
    {
        switch(a_event.mask) {
        case(ProtocolEvent.ABANDONREQUEST_MASK):
            a_listener.after((AbandonEvent) a_event) ;
            break ;
        case(ProtocolEvent.ADDREQUEST_MASK):
            a_listener.after((AddEvent) a_event) ;
            break ;
        case(ProtocolEvent.BINDREQUEST_MASK):
            a_listener.after((BindEvent) a_event) ;
            break ;
        case(ProtocolEvent.COMPAREREQUEST_MASK):
            a_listener.after((CompareEvent) a_event) ;
            break ;
        case(ProtocolEvent.DELREQUEST_MASK):
            a_listener.after((DelEvent) a_event) ;
            break ;
        case(ProtocolEvent.EXTENDEDREQ_MASK):
            a_listener.after((ExtendedEvent) a_event) ;
            break ;
        case(ProtocolEvent.MODDNREQUEST_MASK):
            a_listener.after((ModifyDnEvent) a_event) ;
            break ;
        case(ProtocolEvent.MODIFYREQUEST_MASK):
            a_listener.after((ModifyEvent) a_event) ;
            break ;
        case(ProtocolEvent.SEARCHREQUEST_MASK):
            a_listener.after((SearchEvent) a_event) ;
            break ;
        case(ProtocolEvent.UNBINDREQUEST_MASK):
            a_listener.after((UnbindEvent) a_event) ;
            break ;
		default:
            throw new IllegalArgumentException("Unidentified event mask type "
            + a_event.mask) ;
        }
    }


    /**
     * Calls the before() method on a_listener using a_event as the argument
     * using a switch on the event mask to cast the event to the appropriate
     * event class type.
     *
     * @param a_listener to call the before method on
     * @param a_event the protocol event to use when calling before()
     */
    private void beforeOn(ProtocolListener a_listener, ProtocolEvent a_event)
    {
        switch(a_event.mask) {
        case(ProtocolEvent.ABANDONREQUEST_MASK):
            a_listener.before((AbandonEvent) a_event) ;
            break ;
        case(ProtocolEvent.ADDREQUEST_MASK):
            a_listener.before((AddEvent) a_event) ;
            break ;
        case(ProtocolEvent.BINDREQUEST_MASK):
            a_listener.before((BindEvent) a_event) ;
            break ;
        case(ProtocolEvent.COMPAREREQUEST_MASK):
            a_listener.before((CompareEvent) a_event) ;
            break ;
        case(ProtocolEvent.DELREQUEST_MASK):
            a_listener.before((DelEvent) a_event) ;
            break ;
        case(ProtocolEvent.EXTENDEDREQ_MASK):
            a_listener.before((ExtendedEvent) a_event) ;
            break ;
        case(ProtocolEvent.MODDNREQUEST_MASK):
            a_listener.before((ModifyDnEvent) a_event) ;
            break ;
        case(ProtocolEvent.MODIFYREQUEST_MASK):
            a_listener.before((ModifyEvent) a_event) ;
            break ;
        case(ProtocolEvent.SEARCHREQUEST_MASK):
            a_listener.before((SearchEvent) a_event) ;
            break ;
        case(ProtocolEvent.UNBINDREQUEST_MASK):
            a_listener.before((UnbindEvent) a_event) ;
            break ;
		default:
            throw new IllegalArgumentException("Unidentified event mask type "
            + a_event.mask) ;
        }
    }


    /** Crap */
    class ListenerSpec {
        int mask = 0 ;
        boolean trapErrors = false ;
    }


    ////////////////////
    // Module Methods //
    ////////////////////


    /**
     * Gets the service interface (a.k.a ROLE) used by this module.
     *
     * @return the ROLE of this module.
     */
	public String getImplementationRole()
    {
        return ROLE ;
    }


    /**
     * Gets a descriptive name for this module.
     *
     * @return the descriptive name for this module.
     */
    public String getImplementationName()
    {
        return "Protocol Event Module" ;
    }


    /**
     * Gets the implementing class name of this module.
     *
     * @return the name of this module's class.
     */
    public String getImplementationClassName()
    {
        return this.getClass().getName() ;
    }


    //////////////////////////////
    // Avalon Lifecycle Methods //
    //////////////////////////////


    /**
     * Module configuration life-cycle method.
     *
     * @param a_config the block level configuration for this module
     */
    public void configure(Configuration a_config)
    {
        // Does nothing ;
    }
}
