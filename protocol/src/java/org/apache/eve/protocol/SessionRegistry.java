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
package org.apache.eve.protocol;


import java.util.*;
import javax.naming.InitialContext;
import javax.naming.Context;

import org.apache.seda.listener.ClientKey;
import org.apache.seda.event.EventRouter;
import org.apache.seda.event.DisconnectEvent;


/**
 * A client session state based on JNDI contexts.
 *
 * @author <a href="mailto:directory-dev@incubator.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class SessionRegistry
{
    private static SessionRegistry s_singleton;
    /** a handle on the event router */
    private final EventRouter router;
    /** the set of client contexts */
    private final Map contexts = new HashMap();
    /** the observer to listen for key expiration */
    private final Observer keyObserver = new KeyExpirationObserver();
    /** the properties associated with this SessionRegistry */
    private Hashtable env;


    /**
     * Gets the singleton instance for this SessionRegistry.  If the singleton
     * does not exist one is created.
     *
     * @return the singleton SessionRegistry instance
     */
    public static SessionRegistry getSingleton()
    {
        return s_singleton;
    }


    static void releaseSingleton()
    {
        s_singleton = null;
    }


    /**
     * Creates a singleton session state object for the system.
     *
     * @param env the properties associated with this SessionRegistry
     */
    SessionRegistry( Hashtable env, EventRouter router )
    {
        this.router = router;

        if ( s_singleton == null )
        {
            s_singleton = this;
        }
        else
        {
            throw new IllegalStateException( "there can only be one singlton" );
        }


        if ( env == null )
        {
            this.env = new Hashtable();
            this.env.put( Context.PROVIDER_URL, "" );
            this.env.put( Context.INITIAL_CONTEXT_FACTORY, "org.apache.eve.jndi.EveContextFactory" );
        }
        else
        {
            this.env = env;
            this.env.put( Context.PROVIDER_URL, "" );
        }
    }


    /**
     * Gets a cloned copy of the environment associated with this registry.
     *
     * @return the registry environment
     */
    public Hashtable getEnvironment()
    {
        return ( Hashtable ) env.clone();
    }


    /**
     * Gets the InitialContext to the root of the system that was gotten for
     * client.
     *
     * @param key the client's key
     * @return the InitialContext or null
     */
    public InitialContext get( ClientKey key )
    {
        key.addObserver( keyObserver );
        synchronized( contexts )
        {
            return ( InitialContext ) contexts.get( key );
        }
    }


    /**
     * Sets the initial context associated with a newly authenticated client.
     *
     * @param key the client's key
     * @param ictx the initial context gotten
     */
    public void put( ClientKey key, InitialContext ictx )
    {
        key.deleteObserver( keyObserver );  // remove first just in case
        key.addObserver( keyObserver );
        synchronized( contexts )
        {
            contexts.put( key, ictx );
        }
    }


    /**
     * Removes the state mapping a JNDI initial context for the client's key.
     *
     * @param key the client's key
     */
    public void remove( ClientKey key )
    {
        key.deleteObserver( keyObserver );
        synchronized( contexts )
        {
            contexts.remove( key );
        }
    }


    /**
     * A key expiration observer.
     */
    class KeyExpirationObserver implements Observer
    {
        /**
         * This is called whenever the client's key expires. We react by removing
         * the entry if any of the client's in our InitialContext registry.
         *
         * @param o the ClientKey that has expired
         * @param arg will be null
         */
        public void update( Observable o, Object arg )
        {
            // cast just to make sure
            ClientKey key = ( ClientKey ) o;
            key.deleteObserver( keyObserver );
            synchronized( contexts )
            {
                contexts.remove( key );
            }
        }
    }


    /**
     * Terminates the session by publishing a disconnect event.
     *
     * @param key the client key of the client to disconnect
     */
    public void terminateSession( ClientKey key )
    {
        DisconnectEvent event = new DisconnectEvent( this, key );
        router.publish( event );
    }
}
