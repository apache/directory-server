/*
 * $Id: Utils.java,v 1.2 2003/08/22 21:15:56 akarasulu Exp $
 *
 * -- (c) LDAPd Group                                                    --
 * -- Please refer to the LICENSE.txt file in the root directory of      --
 * -- any LDAPd project for copyright and distribution information.      --
 *
 */

package org.apache.eve.protocol;


import java.util.Hashtable ;

import javax.naming.Name ;
import javax.naming.Context ;
import javax.naming.NameParser ;
import javax.naming.InitialContext ;
import javax.naming.NamingException ;
import javax.naming.ldap.LdapContext ;

import org.apache.ldap.common.name.LdapName ;
import org.apache.ldap.common.message.LdapResult ;
import org.apache.ldap.common.message.ResultResponse ;
import org.apache.ldap.common.message.ResultCodeEnum ;
import org.apache.ldap.common.message.LdapResultImpl ;

import org.apache.avalon.framework.ExceptionUtil ;
import org.apache.avalon.framework.logger.Logger ;


/**
 * Utility functions used internally within this package by the protocol
 * engine's handlers.
 * 
 * @author <a href="mailto:aok123@bellsouth.net">Alex Karasulu</a>
 * @author $Author: akarasulu $
 * @version $Revision: 1.2 $
 */
class Utils
{
	private static Logger s_log ;



    // ------------------------------------------------------------------------
    // Package Friendly Utility Methods Used By RequestHandlers
    // ------------------------------------------------------------------------


    static void enableLogging( Logger a_logger )
    {
        s_log = a_logger ;
    }


    static Logger getLogger()
    {
        return s_log ;
    }


    /**
     * Gets the javax.naming.Name respresent a distinguished name provided as
     * a String.
     *
     * @param a_dn the distinguished name String.
     * @return the Name representing the String argument.
     */
    static Name getName( String a_dn )
        throws NamingException
    {
        if( a_dn == null || a_dn.trim().equals( "" ) )
        {
			return new LdapName() ;
        }

        InitialContext l_ctx = new InitialContext() ;
        NameParser l_parser = l_ctx.getNameParser( a_dn ) ;
		return l_parser.parse( a_dn ) ;
    }


    /**
     * Attempts to deduce the result code from a prefix within the error message
     * of the throwable argument.  Defaults to OPERATIONSERROR if no result code
     * can be resolved.
     *
     * @param a_response the ResultResponse object to build the result for
     * @param a_dn the dn associated with the request used to find the
     * matchingDn for the response.
     * @param t the throwable if any associated with the result.
     */
    static void setResult( ResultResponse a_response, String a_dn, Throwable t )
    {
        setResult( a_response, getResultCode( t ), a_dn, t ) ;
    }


    /**
     * Attempts to deduce the result code from a prefix within the error message
     * of the throwable argument.  Defaults to OPERATIONSERROR if no result code
     * can be resolved.
     *
     * @param a_response the ResultResponse object to build the result for
     * @param a_dn the dn associated with the request used to find the
     * matchingDn for the response.
     * @param t the throwable if any associated with the result.
     */
    static void setResult( ResultResponse a_response, Name a_dn, Throwable t )
    {
        setResult( a_response, getResultCode( t ), a_dn, null, t, true ) ;
    }


    /**
     * Overload used for successful results.
     *
     * @param a_response the ResultResponse object to build the result for
     * @param a_dn the dn associated with the request used to find the
     * matchingDn for the response.
     */
    static void setResult( ResultResponse a_response, Name a_dn )
    {
		setResult( a_response, ResultCodeEnum.SUCCESS, a_dn,
            null, null, true ) ;
    }


    /**
     * Overload used for successful results.
     *
     * @param a_response the ResultResponse object to build the result for
     * @param a_dn the dn associated with the request used to find the
     * matchingDn for the response.
     */
    static void setResult( ResultResponse a_response, String a_dn )
    {
		setResult( a_response, ResultCodeEnum.SUCCESS, a_dn,
            null, null, true ) ;
    }


    /**
     * Overload used for successful results.
     *
     * @param a_response the ResultResponse object to build the result for
     * @param a_dn the dn associated with the request used to find the
     * matchingDn for the response.
     * @param a_matchForDn whether or not to resolve a matching dn - if false
     * then a_dn is used as is.
     */
    static void setResult( ResultResponse a_response, Name a_dn,
        boolean a_matchForDn )
    {
		setResult( a_response, ResultCodeEnum.SUCCESS, a_dn,
            null, null, true ) ;
    }


    /**
     * Overload used for successful results.
     *
     * @param a_response the ResultResponse object to build the result for
     * @param a_dn the dn associated with the request used to find the
     * matchingDn for the response.
     * @param a_matchForDn whether or not to resolve a matching dn - if false
     * then a_dn is used as is.
     */
    static void setResult( ResultResponse a_response, String a_dn,
        boolean a_matchForDn )
    {
		setResult( a_response, ResultCodeEnum.SUCCESS, a_dn,
            null, null, true ) ;
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
    static void setResult( ResultResponse a_response,
        ResultCodeEnum a_resultCode, Name a_dn, String a_msg )
    {
		setResult( a_response, a_resultCode, a_dn, a_msg, null, true ) ;
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
    static void setResult( ResultResponse a_response,
        ResultCodeEnum a_resultCode, String a_dn, String a_msg )
    {
		setResult( a_response, a_resultCode, a_dn, a_msg, null, true ) ;
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
    static void setResult( ResultResponse a_response,
        ResultCodeEnum a_resultCode, String a_msg, Throwable t )
    {
        setResult( a_response, a_resultCode, "", a_msg, t, true ) ;
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
    static void setResult( ResultResponse a_response,
        ResultCodeEnum a_resultCode, String a_dn, String a_msg, Throwable t )
    {
        setResult( a_response, a_resultCode, a_dn, a_msg, t, true ) ;
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
     * @param a_matchForDn whether or not we attempt to find a matching dn for
     * a_dn.
     */
    static void setResult( ResultResponse a_response,
        ResultCodeEnum a_resultCode, String a_dn, String a_msg, Throwable t,
        boolean a_matchForDn )
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
            a_dn = "" ;
        }

		try
        {
            if( a_matchForDn )
            {
            	Name l_matchedDn = getMatchedDn( a_dn ) ;
                l_result.setMatchedDn( l_matchedDn.toString() ) ;
            }
            else
            {
                l_result.setMatchedDn( a_dn ) ;
            }
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
    static void setResult( ResultResponse a_response,
        ResultCodeEnum a_resultCode, Name a_dn, String a_msg,
        Throwable t, boolean a_matchForDn )
    {
        if( a_dn == null )
        {
            setResult( a_response, a_resultCode, "", a_msg, t, a_matchForDn ) ;
        }
        else
        {
        	setResult( a_response, a_resultCode, a_dn.toString(), a_msg,
                t, a_matchForDn ) ;
        }
    }


    /**
     * Utility method to get ahold of an ldap context based on a DN.
     *
     * @param a_dn the distinguished name of the ldap entry to get a JNDI context to.
     * @return the context associated with an entry specified by dn.
     */
    static LdapContext getContext( String a_dn )
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
    static LdapContext getContext( Name a_dn )
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
    static InitialContext getInitialContext()
        throws NamingException
    {
        Hashtable l_env = new Hashtable() ;
        l_env.put( Context.INITIAL_CONTEXT_FACTORY,
            "org.apache.eve.jndi.ServerContextFactory" ) ;
	    return new InitialContext( l_env ) ;
    }


    /**
     * Gets the most significant Dn that exists within the server and hence can
     * be matched to an actual entry.
     *
     * @param a_dn to use for the matching test.
     * @return the matching portion of a_dn, or the valid empty string dn if no
     * match was found.
     */
	static Name getMatchedDn( String a_dn )
        throws NamingException
    {
        Name l_dn = null ;
        Context l_ctx = null ;
        InitialContext l_initCtx = null ;

        // Don't try to match for the empty dn which matches by default
        if( a_dn == null || a_dn.trim().equals( "" ) )
        {
            return new LdapName() ;
        }

        l_initCtx = getInitialContext() ;
		l_dn = l_initCtx.getNameParser( a_dn ).parse( a_dn ) ;
        while( l_dn.size() > 0 )
        {
			try
			{
				l_ctx = ( Context ) l_initCtx.lookup( l_dn ) ;
			}
			catch( NamingException ne )
			{
                // Ignore exceptions
			}

            if( l_ctx == null )
            {
            	l_dn.remove( l_dn.size()-1 ) ;
            }
            else
            {
                return l_dn ;
            }
        }

        return l_dn ;
    }


    /**
     * Gets the most significant Dn that exists within the server and hence can
     * be matched to an actual entry.
     *
     * @param a_dn to use for the matching test.
     * @return the matching portion of a_dn, or the valid empty string dn if no
     * match was found.
     */
	static Name getMatchedDn( Name a_dn )
        throws NamingException
    {
        Name l_dn = ( Name ) a_dn.clone() ;
        Context l_ctx = null ;
        InitialContext l_initCtx = null ;

        // Don't try to match for the empty dn which matches by default
        if( l_dn.size() == 0 )
        {
            return l_dn ;
        }

        l_initCtx = getInitialContext() ;
        while( l_dn.size() > 0 )
        {
			try
			{
				l_ctx = ( Context ) l_initCtx.lookup( l_dn ) ;
			}
			catch( NamingException ne )
			{
                // Ignore exceptions
			}

            if( l_ctx == null )
            {
            	l_dn.remove( l_dn.size() - 1 ) ;
            }
            else
            {
                return l_dn ;
            }
        }

        return l_dn ;
    }


    /**
     * Gets the result code embedded within the message of a throwable.  Because
     * there is a loss of resolution in error codes due to many LDAPv3 result
     * codes mapping to the same JNDI exception, we need to embed the result
     * code associated with the error within the exception message.  We cannot
     * change the JNDI exception to add result code fields.  So if the result
     * code is embedded it is within the first four characters of the message.
     * LDAPv3 result codes only go upto 80 so 2 digits are used at the most and
     * single digit values are padded with zeros in the front.  The '[' and ']'
     * characters are used to delimit the result code value.  So for example if
     * the result code is a 51 for the BUSY result code then the message of the
     * exception is prefixed with '[51]'.  If the result code is a 3 for
     * TIMELIMITEXCEEDED then the prefix would be '[03]'.
     *
     * Note that if the result code extraction process fails due to an number
     * format exception on the failure to parse the integer value then the
     * failure is logged as a warning and OPERATIONSERROR is returned.  If the
     * prefix does not exist it is presumed that the exception was due to an
     * internal server failure that has no value other than OPERATIONSERROR
     * associated with it.  Unintentional exceptions not created and thrown by
     * the code such as NullPointerExceptions hence result in OPERATIONSERRORS.
     * And if debugging is enabled the entire stack trace eventually is
     * delivered to the client with a request to report the bug.
     *
     * This method extracts the embedded result code string and parses it into
     * an integer.  It then calls the getResultCode(int) with the integer as the
     * argument to get the ResultCodeEnum enumeration constant which it then
     * returns.
     *
     * @param t the throwable carrying the embedded result code as a prefix
     * @return the result code associated with the exception.
     */
    static ResultCodeEnum getResultCode( Throwable t )
    {
        String l_msg = t.getMessage() ;
        ResultCodeEnum l_resultCode = null ;

        // Don't try to extract the result code value if the message string is
        // null or does not have the length to contain the prefix for the code
        if( l_msg == null || l_msg.length() < 4 )
        {
            l_resultCode = ResultCodeEnum.OPERATIONSERROR ;
        }
        // Attempt to parse only if we have the starting and ending brackets
		else if( l_msg.charAt( 0 ) == '[' && l_msg.charAt( 3 ) == ']' )
        {
            String l_code = l_msg.substring( 1, 3 ) ;

            try
            {
            	int l_enumValue = Integer.parseInt( l_code ) ;
                l_resultCode = ResultCodeEnum.getResultCodeEnum( l_enumValue ) ;
            }
			catch( NumberFormatException nfe )
            {
                getLogger().warn( "Found embedded result code field of '"
                    + l_code + "' within error message however could not parse"
                    + " result code enumeration value from it." ) ;
                l_resultCode = ResultCodeEnum.OPERATIONSERROR ;
            }
        }
        // No prefix detected so we set the result code to OPERATIONSERROR
        else
        {
             l_resultCode = ResultCodeEnum.OPERATIONSERROR ;
        }

        return l_resultCode ;
    }
}
