/*
 * $Id: ProtocolModule.java,v 1.11 2003/08/22 21:15:56 akarasulu Exp $
 *
 * -- (c) LDAPd Group                                                    --
 * -- Please refer to the LICENSE.txt file in the root directory of      --
 * -- any LDAPd project for copyright and distribution information.      --
 *
 */

package org.apache.eve.protocol ;


import java.util.Map ;
import java.util.HashMap ;
import java.util.EventObject ;

import javax.naming.Name ;
import javax.naming.NamingException ;

import org.apache.avalon.framework.logger.Logger ;
import org.apache.avalon.framework.ExceptionUtil ;
import org.apache.avalon.framework.service.ServiceManager ;
import org.apache.avalon.framework.service.ServiceException ;
import org.apache.avalon.framework.configuration.Configuration ;
import org.apache.avalon.framework.configuration.ConfigurationException ;

import org.apache.ldap.common.name.LdapName ;
import org.apache.ldap.common.message.Request ;
import org.apache.ldap.common.message.LdapResult ;
import org.apache.ldap.common.message.SearchRequest ;
import org.apache.ldap.common.message.ResultResponse ;
import org.apache.ldap.common.message.LdapResultImpl ;
import org.apache.ldap.common.message.ResultCodeEnum ;
import org.apache.ldap.common.message.AddResponseImpl ;
import org.apache.ldap.common.message.BindResponseImpl ;
import org.apache.ldap.common.message.MessageTypeEnum ;
import org.apache.ldap.common.message.ModifyResponseImpl ;
import org.apache.ldap.common.message.SingleReplyRequest ;
import org.apache.ldap.common.message.DeleteResponseImpl ;
import org.apache.ldap.common.message.CompareResponseImpl ;
import org.apache.ldap.common.message.ExtendedResponseImpl ;
import org.apache.ldap.common.message.ModifyDnResponseImpl ;

import org.apache.eve.encoder.Encoder ;
import org.apache.eve.client.ClientKey ;
import org.apache.eve.seda.AbstractStage ;
import org.apache.eve.event.RequestEvent ;
import org.apache.eve.event.ResponseEvent ;
import org.apache.eve.client.ClientManager ;
import org.apache.eve.output.OutputManager ;
import org.apache.eve.backend.UnifiedBackend ;
import org.apache.eve.event.AbstractEventHandler ;
import org.apache.eve.event.protocol.EventManager ;
import org.apache.eve.protocol.extended.PayloadHandler ;
import org.apache.eve.security.auth.AuthenticationManager ;

import javax.naming.ldap.LdapContext ;
import java.util.Hashtable ;
import javax.naming.Context ;
import javax.naming.InitialContext ;


/**
 * Protocol engine stage: the request processing stage of the pipeline.
 * 
 * @phoenix:block
 * @phoenix:service name="org.apache.eve.protocol.ProtocolEngine"
 * @phoenix:mx-topic name="ProtocolModule"
 */
public class ProtocolModule
    extends AbstractStage
    implements ProtocolEngine
{
	/** The version support by this ProtocolEngine */
    public static final int PROTOCOL_VERSION = 3 ;
    /** Extended request handler configuration tag name */
    public static final String HANDLER_TAG = "handler" ;
    /** Extended request handler configuration 'oid' attribute name */
    public static final String OID_ATTR = "oid" ;
    /** Extended request handler configuration handler 'class' attribute name */
    public static final String CLASS_ATTR = "class" ;

    /** Lookup Table of MessageTypeEnums to the Request's respective handler */
    private Map m_handlers = new HashMap( 10 ) ;
    /** Map of extended request OIDs to the respective request handlers */
    private Map m_extendedHandlers = new HashMap() ;


    // ------------------------------------------------------------------------
    // Blocks the ProtocolModule depends on
    // ------------------------------------------------------------------------

    /** Reference to the event manager to dispatch protocol events */
    private EventManager m_eventManager = null ;
    /** Reference to the client manager to manage client sessions */
    private ClientManager m_clientManager = null ;
	/** Reference to the nexus which several request handlers depend on */
	private UnifiedBackend m_nexus = null ;
    /** Reference to the authentication manager for bind operations */
	private AuthenticationManager m_authManager = null ;
    /** Reference to the encoder to hande off response events to */
	private Encoder m_encoder = null ;
    /** Reference to the output manager synchronously search responses */
    private OutputManager m_outputManager = null ;

    // ------------------------------------------------------------------------
    // Explicit Default Constructor
    // ------------------------------------------------------------------------


    /**
     * Instantiates this module and creates the stage event handler so it can
     * be log enabled in the first lifecycle method.
     *
     * @todo look into the correct way to enable logging in these handlers while
     * making their instantiation reside within the initialize life-cycle
     * method.
     */
    public ProtocolModule()
    {
        m_handler = new RequestEventHandler() ;
    }


    // ------------------------------------------------------------------------
    // RequestListener Interface Method Implementations
    // ------------------------------------------------------------------------


    /**
     * Recieves a SEDA RequstEvent by simply enqueing it onto the stage queue.
     *
     * @param a_event the enqueued RequestEvent
     */
    public void requestReceived( RequestEvent a_event )
    {
        enqueue(a_event) ;
    }


    // ------------------------------------------------------------------------
    // ProtocolEngine Interface Method Implementations
    // ------------------------------------------------------------------------


    /**
     * Gets the protocol version of this ProtocolEngine implementation.
     *
     * @return 2 if the protocol implementation is LDAPv2, otherwise 3
     * @phoenix:mx-attribute
     * @phoenix:mx-description the LDAP protocol version of this module.
     * @phoenix:mx-isWriteable no
     */
    public int getProtocolVersion()
    {
        return PROTOCOL_VERSION ;
    }


    // ------------------------------------------------------------------------
    // ClientManagerSlave Interface Method Implementation
    // ------------------------------------------------------------------------


    /**
     * Registers the client manager this module is to use.
     *
     * @param a_manager the ClientManager this module is to become the slave of.
     */
    public void registerClientManager(ClientManager a_manager)
    {
        m_clientManager = a_manager ;
    }


    // ------------------------------------------------------------------------
    // Module Interface Method Implementations.
    // ------------------------------------------------------------------------


    /**
     * Gets the service interface name of this module.
     *
     * @return the role of this module's implemented service.
     * @phoenix:mx-attribute
     * @phoenix:mx-description Returns the service role name.
     * @phoenix:mx-isWriteable no
     */
    public String getImplementationRole()
    {
        return ROLE ;
    }


    /**
     * Gets the name of the implementation.  For example the name of the
     * Berkeley DB Backend module is "Berkeley DB Backend".
     *
     * @return String representing the module implementation type name.
     * @phoenix:mx-attribute
     * @phoenix:mx-description Returns the implementation name.
     * @phoenix:mx-isWriteable no
     */
    public String getImplementationName()
    {
        return "LDAP V3 Protocol Module" ;
    }


    /**
     * Gets the name of the implementation class.  For example the name of the
     * Berkeley DB Backend implementation class is <code>
     * "ldapdd.backend.berkeley.BackendBDb" </code>.
     *
     * @return String representing the module implementation's class name.
     * @phoenix:mx-attribute
     * @phoenix:mx-description Returns the implementation class name.
     * @phoenix:mx-isWriteable no
     */
    public String getImplementationClassName()
    {
        return this.getClass().getName() ;
    }


    // ------------------------------------------------------------------------
    // Avalon Life-Cycle Interface Method Implementations
    // ------------------------------------------------------------------------


    /**
     * Enables logging for this module first through super method call then
     * log enables this stage's event handler.
     *
     * @param a_logger the logger used to log enable this stage.
     */
    public void enableLogging( Logger a_logger )
    {
        super.enableLogging( a_logger ) ;
        m_handler.enableLogging( a_logger ) ;
        Utils.enableLogging( a_logger ) ;
    }


    /**
     * Need lot of services to get the job done.
     *
     * @phoenix:dependency name="org.apache.eve.event.protocol.EventManager"
     * @phoenix:dependency name="org.apache.eve.backend.UnifiedBackend"
     * @phoenix:dependency name="org.apache.eve.security.auth.AuthenticationManager"
     * @phoenix:dependency name="org.apache.eve.output.OutputManager"
     * @phoenix:dependency name="org.apache.eve.encoder.Encoder"
     * @phoenix:dependency name="org.apache.avalon.cornerstone.services.threads.ThreadManager"
     */
    public void service( ServiceManager a_manager )
        throws ServiceException
    {
        super.service( a_manager ) ;
		m_nexus = ( UnifiedBackend ) a_manager.lookup( UnifiedBackend.ROLE ) ;
        m_authManager = ( AuthenticationManager )
            a_manager.lookup( AuthenticationManager.ROLE ) ;
        m_encoder = ( Encoder ) a_manager.lookup( Encoder.ROLE ) ;
        m_outputManager = ( OutputManager )
            a_manager.lookup( OutputManager.ROLE ) ;
        m_eventManager = ( EventManager )
            a_manager.lookup( EventManager.ROLE ) ;
    }


    /**
     * Configures the module by loading extended request processor information.
     *
     * @param a_config the module's configuration read from config.xml
     */
    public void configure( Configuration a_config )
        throws ConfigurationException
    {
        super.configure( a_config ) ;

        PayloadHandler l_handler = null ;
        Configuration [] l_handlers = a_config.getChildren( HANDLER_TAG ) ;
        for( int ii = 0 ; ii < l_handlers.length ; ii++ )
        {
            String l_oid = l_handlers[ii].getAttribute( OID_ATTR, null ) ;
            String l_clazz = l_handlers[ii].getAttribute( CLASS_ATTR, null ) ;

            if( null == l_oid )
            {
                throw new ConfigurationException(
                    "Handler must have an 'oid' attribute" ) ;
            }

            if( null == l_clazz )
            {
                throw new ConfigurationException(
                    "Handler must have a 'class' attribute" ) ;
            }

            try
            {
                l_handler = ( PayloadHandler )
                    Class.forName( l_clazz ).newInstance() ;
            }
            catch( Exception e )
            {
                throw new ConfigurationException(
                    "Failed to instantiate handler class " + l_clazz
                    + " for extended request OID " + l_oid ) ;
            }

            m_extendedHandlers.put( l_oid, l_handler ) ;
        }
    }


    public void initialize()
        throws Exception
    {
		RequestHandler l_handler = new AbandonHandler( this ) ;
		m_handlers.put( l_handler.getRequestType(), l_handler ) ;

		l_handler = new AddHandler( this ) ;
		m_handlers.put( l_handler.getRequestType(), l_handler ) ;

		l_handler = new BindHandler( this ) ;
		m_handlers.put( l_handler.getRequestType(), l_handler ) ;

		l_handler = new CompareHandler( this ) ;
		m_handlers.put( l_handler.getRequestType(), l_handler ) ;

		l_handler = new DeleteHandler( this ) ;
		m_handlers.put( l_handler.getRequestType(), l_handler ) ;

		l_handler = new ModifyHandler( this ) ;
		m_handlers.put( l_handler.getRequestType(), l_handler ) ;

		l_handler = new ExtendedHandler( this ) ;
		m_handlers.put( l_handler.getRequestType(), l_handler ) ;

		l_handler = new ModifyDnHandler( this ) ;
		m_handlers.put( l_handler.getRequestType(), l_handler ) ;

		l_handler = new SearchHandler( this ) ;
        m_handlers.put( l_handler.getRequestType(), l_handler ) ;

		l_handler = new UnbindHandler( this ) ;
        m_handlers.put( l_handler.getRequestType(), l_handler ) ;
    }


    // ------------------------------------------------------------------------
    // Stage Event Handler Implementation.
    // ------------------------------------------------------------------------


    /**
     * Stage Event Handler Implementation for RequestEvents.
     */
    class RequestEventHandler extends AbstractEventHandler
    {
        /**
         * Event handler method for processing RequestEvents.
         *
         * @param a_event the RequestEvent to process.
         */
        public void handleEvent( EventObject a_event )
        {
			Request l_request = null ;
            ClientKey l_clientKey = null ;

            // Throw protocol exception if the event is not a request event.
            if( ! ( a_event instanceof RequestEvent ) )
            {
                throw new ProtocolException(
                    "Unrecognized event: " + a_event ) ;
            }

            // Extract the ClientKey and Request parameters from the event
			l_request = ( ( RequestEvent ) a_event ).getRequest() ;
            l_clientKey = ( ClientKey )
                ( ( RequestEvent ) a_event ).getSource() ;
            m_clientManager.threadAssociate( l_clientKey ) ;
            m_clientManager.threadAssociate( l_clientKey ) ;

			// Get the handler if we have one defined.
			RequestHandler l_handler = ( RequestHandler )
                m_handlers.get( l_request.getType() ) ;
			if( l_handler == null )
            {
                throw new ProtocolException( "Unknown request message type: "
                    + l_request.getType() ) ;
            }

            // Based on the handler type start request handling.
            switch( l_handler.getHandlerType().getValue() )
            {
            case( HandlerTypeEnum.NOREPLY_VAL ):
                NoReplyHandler l_noreply = ( NoReplyHandler ) l_handler ;
            	l_noreply.handle( l_request ) ;
                break ;
            case( HandlerTypeEnum.SINGLEREPLY_VAL ):
                SingleReplyHandler l_single = ( SingleReplyHandler ) l_handler ;
            	doSingleReply( l_single, ( SingleReplyRequest ) l_request ) ;
                break ;
            case( HandlerTypeEnum.SEARCH_VAL ):
                SearchHandler l_search = ( SearchHandler ) l_handler ;
				l_search.handle( ( SearchRequest ) l_request ) ;
                break ;
            default:
                throw new ProtocolException( "Unrecognized handler type: "
                    + l_handler.getRequestType() ) ;
            }

            m_clientManager.threadDisassociate() ;
        }
    }


    private void doSingleReply( SingleReplyHandler a_handler,
        SingleReplyRequest a_request )
    {
        int l_id = a_request.getMessageId() ;
        LdapResult l_result = null ;
        ResultResponse l_response = null ;

        try
        {
			l_response = a_handler.handle( a_request ) ;
        }

        // If the individual handlers do not do a global catch and report this
        // will sheild the server from complete failure on a request reporting
        // at a minimum the stack trace that cause the request to fail.
        catch( Throwable t )
        {
            switch( a_request.getResponseType().getValue() )
            {
            case( MessageTypeEnum.ADDRESPONSE_VAL ):
				l_response = new AddResponseImpl( l_id ) ;
                break ;
            case( MessageTypeEnum.BINDRESPONSE_VAL ):
                l_response = new BindResponseImpl( l_id ) ;
                break ;
            case( MessageTypeEnum.COMPARERESPONSE_VAL ):
				l_response = new CompareResponseImpl( l_id ) ;
                break ;
            case( MessageTypeEnum.DELRESPONSE_VAL ):
                l_response = new DeleteResponseImpl( l_id ) ;
                break ;
            case( MessageTypeEnum.EXTENDEDRESP_VAL ):
               	l_response = new ExtendedResponseImpl( l_id ) ;
                break ;
            case( MessageTypeEnum.MODDNRESPONSE_VAL ):
                l_response = new ModifyDnResponseImpl( l_id ) ;
                break ;
            case( MessageTypeEnum.MODIFYRESPONSE_VAL ):
                l_response = new ModifyResponseImpl( l_id ) ;
                break ;
            }

            // @todo We should be able to email this to LDAPd auto-bug list.
            // if some of error reporting configuration parameters are set. Or
            // perhaps this is something best left to a logger customization.
            String l_msg = "Encountered an operational error while processing "
                + a_request.getType() + " request. Please report the"
                + " the following server stack trace to the LDAPd Group:\n"
                + ExceptionUtil.printStackTrace( t ) ;
            getLogger().error( l_msg ) ;
            l_result = new LdapResultImpl( l_response ) ;
            l_result.setMatchedDn( "" ) ;
            l_result.setErrorMessage( l_msg ) ;
            l_result.setResultCode( ResultCodeEnum.OPERATIONSERROR ) ;
            l_response.setLdapResult( l_result ) ;
        }

        ClientKey l_clientKey = m_clientManager.getClientKey() ;
		ResponseEvent l_event = new ResponseEvent( l_clientKey, l_response ) ;
        m_encoder.responseComposed( l_event ) ;
    }


    // ------------------------------------------------------------------------
    // Package Friendly Accessors For Blocks the RequestHandlers depends on
    // ------------------------------------------------------------------------


    /**
     * Package friendly accessor to the EventManager for access by
     * RequestHandlers within this package.
     *
     * @return the ClientManager this module depends on
     */
    EventManager getEventManager()
    {
        return m_eventManager ;
    }


    /**
     * Package friendly accessor to the ClientManager for access by
     * RequestHandlers within this package.
     *
     * @return the ClientManager this module depends on
     */
    ClientManager getClientManager()
    {
        return m_clientManager ;
    }


    /**
     * Package friendly accessor to the UnifiedBackend for access by
     * RequestHandlers within this package.
     *
     * @return the UnifiedBackend this module depends on
     */
    UnifiedBackend getNexus()
    {
        return m_nexus ;
    }


    /**
     * Package friendly accessor to the AuthenticationManager for access by
     * RequestHandlers within this package.
     *
     * @return the AuthenticationManager this module depends on
     */
    AuthenticationManager getAuthenticationManager()
    {
        return m_authManager ;
    }


    /**
     * Package friendly accessor to the Encoder for access by
     * RequestHandlers within this package.
     *
     * @return the Encoder this module depends on
     */
    Encoder getEncoder()
    {
        return m_encoder ;
    }


    /**
     * Package friendly accessor to the OutputManager for access by
     * RequestHandlers within this package.
     *
     * @return the OutputManager this module depends on
     */
    OutputManager getOutputManager()
    {
        return m_outputManager ;
    }


    // ------------------------------------------------------------------------
    // Package Friendly Utility Methods Used By RequestHandlers
    // ------------------------------------------------------------------------


    /**
     * Gets the javax.naming.Name respresent a distinguished name provided as
     * a String.
     *
     * @param a_dn the distinguished name String.
     * @return the Name representing the String argument.
     */
    Name getName( String a_dn )
        throws NamingException
    {
        if( a_dn == null || a_dn.trim().equals( "" ) )
        {
			return new LdapName() ;
        }

		return m_nexus.getName( a_dn ) ;
    }


    /**
     * Specifically used for operational errors (a.k.a. unexplained exceptions).
     *
     * @param a_response the ResultResponse object to build the result for
     * @param a_dn the dn associated with the request used to find the
     * matchingDn for the response.
     * @param t the throwable if any associated with the result.
     */
    void setResult( ResultResponse a_response, Name a_dn, Throwable t )
    {
        setResult( a_response, ResultCodeEnum.OPERATIONSERROR, a_dn,
            "Operational error encountered please contact report trace!", t ) ;
    }


    /**
     * Overload used for successful results.
     *
     * @param a_response the ResultResponse object to build the result for
     * @param a_dn the dn associated with the request used to find the
     * matchingDn for the response.
     */
    void setResult( ResultResponse a_response, Name a_dn )
    {
		setResult( a_response, ResultCodeEnum.SUCCESS, a_dn, null, null ) ;
    }


    /**
     * Utility function that creates and populates an LdapResult into a
     * ResultResponse. So many places used this same code we had to put
     * the it into one place as a utility function.
     *
     * @param a_response the ResultResponse object to build the result for
     * @param a_resultCode the returned error code.
     * @param a_dn the dn associated with the request used to find the
     * matchingDn for the response.
     * @param a_msg the error message to use.
     */
    void setResult( ResultResponse a_response, ResultCodeEnum a_resultCode,
        Name a_dn, String a_msg )
    {
		setResult( a_response, a_resultCode, a_dn, a_msg, null ) ;
    }


    /**
     * Utility function that creates and populates an LdapResult into a
     * ResultResponse when the matched Dn is unknown or irrelavent.
     *
     * @param a_response the ResultResponse object to build the result for
     * @param a_resultCode the returned error code.
     * @param a_msg the error message to use.
     * @param t the throwable if any associated with the result.
     */
    void setResult( ResultResponse a_response, ResultCodeEnum a_resultCode,
        String a_msg, Throwable t )
    {
        setResult( a_response, a_resultCode, null, a_msg, t ) ;
    }


    /**
     * Utility function that creates and populates an LdapResult into a
     * ResultResponse. So many places used this same code we had to put
     * the it into one place as a utility function.
     *
     * @param a_response the ResultResponse object to build the result for
     * @param a_resultCode the returned error code.
     * @param a_dn the dn associated with the request used to find the
     * matchingDn for the response.
     * @param a_msg the error message to use.
     * @param t the throwable if any associated with the result.
     */
    void setResult( ResultResponse a_response, ResultCodeEnum a_resultCode,
        Name a_dn, String a_msg, Throwable t )
    {
        // Initialize the result object and set it in the response
		LdapResult l_result = new LdapResultImpl( a_response ) ;
        a_response.setLdapResult( l_result ) ;

        // Log the error if this result is associated with a non null Throwable,
        // error message and is not a part of a successful response.
        if( t != null && a_msg != null &&
            a_resultCode != ResultCodeEnum.SUCCESS )
        {
            getLogger().error( a_msg, t ) ;
        }

        // Do no error message if all is null
        if( a_msg == null && t == null )
        {
            l_result.setErrorMessage( "" ) ;
        }
        // Use a_msg in error message if throwable is null only
        else if( t == null && a_msg != null )
        {
            l_result.setErrorMessage( a_msg ) ;
        }
        // Use the stack trace as error message if a_msg is null and debug is on
        else if( a_msg == null && t != null )
        {
            if( getLogger().isDebugEnabled() )
            {
            	l_result.setErrorMessage( ExceptionUtil.printStackTrace( t ) ) ;
            }
        }
        // When a_msg and t are not null
		else
        {
            // Only append stack trace if debugging is enabled
            if( getLogger().isDebugEnabled() )
            {
				l_result.setErrorMessage( a_msg
					+ ExceptionUtil.printStackTrace( t ) ) ;
            }
            // Do not append stack trace if debugging is off
            else
            {
				l_result.setErrorMessage( a_msg ) ;
            }
        }


        l_result.setResultCode( a_resultCode ) ;

        if( a_dn == null )
        {
            a_dn = new LdapName() ;
        }

		try
        {
            Name l_matchedDn = m_nexus.getMatchedDn( a_dn ) ;
            l_result.setMatchedDn( l_matchedDn.toString() ) ;
        }
        // Regardless of exception this is an operational error
        catch( Exception e )
        {
            StringBuffer l_buf = new StringBuffer() ;
            l_buf.append( "Could not find matching Dn for '" ) ;
            l_buf.append( a_dn ) ;
            l_buf.append( "' due to operational error: " ) ;
            l_buf.append( e.getMessage() ) ;
			String l_msg = l_buf.toString() ;

			l_result.setMatchedDn( "" ) ;
			l_result.setErrorMessage( l_msg ) ;
			l_result.setResultCode( ResultCodeEnum.OPERATIONSERROR ) ;
			getLogger().error( l_msg, e ) ;
        }
    }


    /**
     * Utility method to get ahold of an ldap context based on a DN.
     *
     * @param a_dn the distinguished name of the ldap entry to get a JNDI context to.
     * @return the context associated with an entry specified by dn.
     */
    public LdapContext getContext( String a_dn )
        throws NamingException
    {
        return getContext( getName( a_dn ) ) ;
    }


    /**
     * Utility method to get ahold of an ldap context based on a DN.
     *
     * @param a_dn the distinguished name of the ldap entry to get a JNDI context to.
     * @return the context associated with an entry specified by dn.
     */
    public LdapContext getContext( Name a_dn )
        throws NamingException
    {
        Hashtable l_env = new Hashtable() ;
        InitialContext l_initialContext = null ;

        l_env.put( Context.INITIAL_CONTEXT_FACTORY,
            "org.apache.eve.jndi.ServerContextFactory" ) ;
	    l_initialContext = new InitialContext( l_env ) ;
	    return ( LdapContext ) l_initialContext.lookup( a_dn ) ;
    }


    /**
     * Utility method to get then initial context.
     *
     * @return the initial context for the directory.
     */
    public InitialContext getInitialContext()
        throws NamingException
    {
        Hashtable l_env = new Hashtable() ;
        l_env.put( Context.INITIAL_CONTEXT_FACTORY,
            "org.apache.eve.jndi.ServerContextFactory" ) ;
	    return new InitialContext( l_env ) ;
    }
}




