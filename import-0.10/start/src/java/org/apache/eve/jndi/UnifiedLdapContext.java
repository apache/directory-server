/*
 * $Id: UnifiedLdapContext.java,v 1.4 2003/08/22 21:15:55 akarasulu Exp $
 *
 * -- (c) LDAPd Group                                                    --
 * -- Please refer to the LICENSE.txt file in the root directory of      --
 * -- any LDAPd project for copyright and distribution information.      --
 *
 */

package org.apache.eve.jndi ;


import java.util.Hashtable ;

import javax.naming.NamingException ;
import javax.naming.ldap.Control ;
import javax.naming.ldap.LdapContext ;
import javax.naming.ldap.ExtendedRequest ;
import javax.naming.ldap.ExtendedResponse ;

import org.apache.eve.backend.LdapEntry ;


/**
 * The internal server side LdapContext implementation used to access and
 * modify server entries.  Most bind operations will be unsupported since
 * instances of this context are already bound.  This context is the main
 * class of the internal server side provider.  It is used whenever an
 * initial context is requested within stored procedures or within a
 * server trigger.  It is also the basis for server side communication between
 * a server host with an embedded ldapd server component.  The UnifiedBackend
 * as well as a public Kernel interface will expose a handle to the RootDSE
 * using this LdapContext implementation.
 *
 * Note that the documentation used by the Java APIs for Context, DirContext
 * and LdapContext are copied here for convenience.   Eventually we can make
 * these chunks of javadocs @see references to the respective interfaces.
 * 
 * @author <a href="mailto:aok123@bellsouth.net">Alex Karasulu</a>
 * @author $Author: akarasulu $
 * @version $Revision: 1.4 $
 */
public class UnifiedLdapContext
    extends UnifiedDirContext
    implements LdapContext
{
    UnifiedLdapContext( Hashtable an_environment )
    {
        super( an_environment ) ;
    }


    UnifiedLdapContext( Hashtable an_environment, LdapEntry a_entry )
        throws NamingException
    {
        super( an_environment, a_entry ) ;
    }


    /////////////////////////////////
    // LdapContext Implementations //
    /////////////////////////////////


   /**
    * Performs an extended operation.
    *
    * This method is used to support LDAPv3 extended operations.
    * @param request The non-null request to be performed.
    * @return The possibly null response of the operation. null means
    * the operation did not generate any response.
    * @throws NamingException If an error occurred while performing the
    * extended operation.
    */
    public ExtendedResponse extendedOperation(ExtendedRequest request)
	    throws NamingException
    {
        throw new UnsupportedOperationException() ;
    }


    /**
     * Creates a new instance of this context initialized using request controls.
     *
     * This method is a convenience method for creating a new instance
     * of this context for the purposes of multithreaded access.
     * For example, if multiple threads want to use different context
     * request controls,
     * each thread may use this method to get its own copy of this context
     * and set/get context request controls without having to synchronize with other 
     * threads.
     *<p>
     * The new context has the same environment properties and connection 
     * request controls as this context. See the class description for details.
     * Implementations might also allow this context and the new context
     * to share the same network connection or other resources if doing 
     * so does not impede the independence of either context.
     *
     * @param requestControls The possibly null request controls 
     * to use for the new context.
     * If null, the context is initialized with no request controls.
     *
     * @return A non-null <tt>LdapContext</tt> instance.
     * @exception NamingException If an error occurred while creating
     * the new instance.
     * @see javax.naming.ldap.InitialLdapContext
     */
    public LdapContext newInstance(Control[] requestControls)
	    throws NamingException
    {
        throw new UnsupportedOperationException() ;
    }


    /**
     * Reconnects to the LDAP server using the supplied controls and 
     * this context's environment.
     *<p>
     * This method is a way to explicitly initiate an LDAP "bind" operation.
     * For example, you can use this method to set request controls for
     * the LDAP "bind" operation, or to explicitly connect to the server 
     * to get response controls returned by the LDAP "bind" operation.
     *<p>
     * This method sets this context's <tt>connCtls</tt>
     * to be its new connection request controls. This context's
     * context request controls are not affected.
     * After this method has been invoked, any subsequent 
     * implicit reconnections will be done using <tt>connCtls</tt>.
     * <tt>connCtls</tt> are also used as
     * connection request controls for new context instances derived from this
     * context.
     * These connection request controls are not
     * affected by <tt>setRequestControls()</tt>.
     *<p>
     * Service provider implementors should read the "Service Provider" section
     * in the class description for implementation details.
     * @param connCtls The possibly null controls to use. If null, no
     * controls are used.
     * @exception NamingException If an error occurred while reconnecting.
     * @see #getConnectControls
     * @see #newInstance
     */
    public void reconnect(Control[] connCtls) throws NamingException
    {
        throw new UnsupportedOperationException() ;
    }


    /**
     * Retrieves the connection request controls in effect for this context.
     * The controls are owned by the JNDI implementation and are
     * immutable. Neither the array nor the controls may be modified by the
     * caller.
     *
     * @return A possibly-null array of controls. null means no connect controls
     * have been set for this context.
     * @exception NamingException If an error occurred while getting the request
     * controls.
     */
    public Control[] getConnectControls() throws NamingException
    {
        throw new UnsupportedOperationException() ;
    }


    /**
     * Sets the request controls for methods subsequently 
     * invoked on this context.
     * The request controls are owned by the JNDI implementation and are
     * immutable. Neither the array nor the controls may be modified by the
     * caller.
     * <p>
     * This removes any previous request controls and adds
     * <tt>requestControls</tt> 
     * for use by subsequent methods invoked on this context.
     * This method does not affect this context's connection request controls.
     *<p>
     * Note that <tt>requestControls</tt> will be in effect until the next
     * invocation of <tt>setRequestControls()</tt>. You need to explicitly
     * invoke <tt>setRequestControls()</tt> with <tt>null</tt> or an empty
     * array to clear the controls if you don't want them to affect the
     * context methods any more.
     * To check what request controls are in effect for this context, use
     * <tt>getRequestControls()</tt>.
     * @param requestControls The possibly null controls to use. If null, no
     * controls are used.
     * @exception NamingException If an error occurred while setting the
     * request controls.
     * @see #getRequestControls
     */
    public void setRequestControls(Control[] requestControls)
    	throws NamingException
    {
        throw new UnsupportedOperationException() ;
    }


    /**
     * Retrieves the request controls in effect for this context.
     * The request controls are owned by the JNDI implementation and are
     * immutable. Neither the array nor the controls may be modified by the
     * caller.
     *
     * @return A possibly-null array of controls. null means no request controls
     * have been set for this context.
     * @exception NamingException If an error occurred while getting the request
     * controls.
     * @see #setRequestControls
     */
    public Control[] getRequestControls() throws NamingException
    {
        throw new UnsupportedOperationException() ;
    }


    /**
     * Retrieves the response controls produced as a result of the last
     * method invoked on this context.
     * The response controls are owned by the JNDI implementation and are
     * immutable. Neither the array nor the controls may be modified by the
     * caller.
     *<p>
     * These response controls might have been generated by a successful or
     * failed operation.
     *<p>
     * When a context method that may return response controls is invoked,
     * response controls from the previous method invocation are cleared.
     * <tt>getResponseControls()</tt> returns all of the response controls
     * generated by LDAP operations used by the context method in the order
     * received from the LDAP server.
     * Invoking <tt>getResponseControls()</tt> does not
     * clear the response controls. You can call it many times (and get
     * back the same controls) until the next context method that may return
     * controls is invoked.
     *<p>
     * @return A possibly null array of controls. If null, the previous
     * method invoked on this context did not produce any controls.
     * @exception NamingException If an error occurred while getting the response
     * controls.
     */
    public Control[] getResponseControls() throws NamingException
    {
        throw new UnsupportedOperationException() ;
    }
}
