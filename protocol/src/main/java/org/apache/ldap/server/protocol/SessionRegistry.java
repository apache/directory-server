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
package org.apache.ldap.server.protocol;


import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

import javax.naming.Context;
import javax.naming.NamingException;
import javax.naming.directory.InitialDirContext;
import javax.naming.ldap.Control;
import javax.naming.ldap.InitialLdapContext;

import org.apache.ldap.common.exception.LdapNoPermissionException;
import org.apache.mina.protocol.ProtocolSession;


/**
 * A client session state based on JNDI contexts.
 *
 * @author <a href="mailto:directory-dev@incubator.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class SessionRegistry
{
    private static SessionRegistry s_singleton;
    /** the set of client contexts */
    private final Map contexts = new HashMap();
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
    SessionRegistry( Hashtable env )
    {
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
            this.env.put( Context.INITIAL_CONTEXT_FACTORY, "org.apache.ldap.server.jndi.ServerContextFactory" );
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
     * client.  If the context is not present then there was no bind operation
     * that set it.  Hence this operation requesting the IC is anonymous.
     *
     * @param session the client's key
     * @param connCtls connection controls if any to use if creating anon context
     * @param allowAnonymous true if anonymous requests will create anonymous
     * InitialContext if one is not present for the operation
     * @return the InitialContext or null
     */
    public InitialLdapContext getInitialLdapContext( ProtocolSession session,
                                                     Control[] connCtls,
                                                     boolean allowAnonymous )
            throws NamingException
    {
        InitialLdapContext ictx = null;

        synchronized( contexts )
        {
            ictx = ( InitialLdapContext ) contexts.get( session );
        }

        if ( ictx == null && allowAnonymous )
        {
            if ( env.containsKey( "eve.disable.anonymous" ) )
            {
                throw new LdapNoPermissionException( "Anonymous binds have been disabled!" );
            }

            Hashtable cloned = ( Hashtable ) env.clone();
            ictx = new InitialLdapContext( cloned, connCtls );
        }

        return ictx;
    }


    /**
     * Sets the initial context associated with a newly authenticated client.
     *
     * @param session the client session
     * @param ictx the initial context gotten
     */
    public void setInitialLdapContext( ProtocolSession session, InitialDirContext ictx )
    {
        synchronized( contexts )
        {
            contexts.put( session, ictx );
        }
    }


    /**
     * Removes the state mapping a JNDI initial context for the client's key.
     *
     * @param session the client's key
     */
    public void remove( ProtocolSession session )
    {
        synchronized( contexts )
        {
            contexts.remove( session );
        }
    }


    /**
     * Terminates the session by publishing a disconnect event.
     *
     * @param session the client key of the client to disconnect
     */
    public void terminateSession( ProtocolSession session )
    {
        session.close();
    }
}
