/*
 * $Id: UnifiedBackend.java,v 1.13 2003/08/22 21:15:54 akarasulu Exp $
 *
 * -- (c) LDAPd Group                                                    --
 * -- Please refer to the LICENSE.txt file in the root directory of      --
 * -- any LDAPd project for copyright and distribution information.      --
 *
 */

package org.apache.eve.backend ;


import java.util.Iterator ;

import javax.naming.Name ;
import javax.naming.NameParser ;
import javax.naming.NamingException ;
import javax.naming.InvalidNameException ;
import javax.naming.NameNotFoundException ;
import javax.naming.ldap.LdapContext ;

import org.apache.eve.schema.Schema ;


/**
 * Represents a composite backend nexus that forwards backend requests to the
 * apropriate Backends registered with it.  UnifiedBackends are used to
 * implement a name switch to get to the respective backend if one exists.
 *
 * Effectively, the Unified backend is a specific router interface in a system
 * implementing the Router, (a.k.a. Request Router or Multiplexer) design
 * pattern.  AtomicBackends are the channels which are added and removed using
 * the register and unregister methods of the UnifiedBackend.  The routed
 * messages are AtomicBackend interface calls.
 */
public interface UnifiedBackend
	extends Backend
{
    /**
     * Role of this service interface as mandated by the avalon framework.
     */
    public static final String ROLE = UnifiedBackend.class.getName() ;


    /**
     * Get's a Backend module by checking to see if a distinguished name
     * contains the suffix of a backend as a prefix.  If no backend can be
     * found for a valid DN an IllegalArgumentException is thrown or an
     * InvalidNameException is thrown if the DN is not syntactically correct.
     *
     * @param a_dn the distinguished name potentially contained by the backend.
     * @return the Backend that would contain the entry if it were to exist.
     * @throws InvalidNameException if a_dn is syntactically incorrect.
     * @throws NameNotFoundException if no backend can house the
     * hypothetical entry - it is not within the namespace of the federated
     * backends.
     * @deprecated we would like to deprecate this method because we would no
     * longer like to expose it to the outside world which could misuse it
     * without providing the required dn in normalized form.
     */
    AtomicBackend getBackend(Name a_dn)
        throws NamingException ;


    /**
     * Gets the most significant Dn that exists within the server and hence can
     * be matched to an actual entry.
     *
     * @param a_dn to use for the matching test.
     * @return the matching portion of a_dn, or the valid empty string dn if no
     * match was found.
     */
    Name getMatchedDn( Name a_dn )
        throws NamingException, BackendException ;

    /**
     * Gets an iteration over the Backends managed by this BackendManager.
     *
     * @return the iteration over Backend instances.
     */
    Iterator listBackends() ;

    /**
     * Gets an iteration over the suffixes of the Backends managed by this
     * UnifiedBackend.
     *
     * @return the iteration over Backend suffix names as Strings.
     */
    Iterator listSuffixes() ;

    Schema getSchema(Name a_dn)
        throws NamingException ;

    NameParser getNormalizingParser() ;

    NameParser getNameParser() ;

    NameParser getNormalizingParser(Name a_dn)
        throws NamingException ;

    Name getNormalizedName(String a_name)
        throws NamingException ;

    Name getName(String a_name)
        throws NamingException ;

	RootDSE getRootDSE() ;

	/**
     * Registers a Backend with this BackendManager.  Called for each Backend
     * implementation after it is started.  This is the only way it will receive
     * requests from the protocol server.
     *
     * @param a_backend Backend component to register with this manager.
     */
    void register(AtomicBackend a_backend) ;

    /**
     * Unregisters a Backend with this BackendManager.  Called for each
     * registered Backend right befor it is to be stopped.  This prevents
     * protocol server requests from reaching the Backend.
     *
     * @param a_backend Backend component to unregister with this manager.
     */
    void unregister(AtomicBackend a_backend) ;
}
