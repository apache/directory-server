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
package org.apache.ldap.server.jndi.request.processor;


import javax.naming.NamingException;

import org.apache.ldap.server.auth.LdapPrincipal;
import org.apache.ldap.server.jndi.ServerContext;
import org.apache.ldap.server.jndi.request.AddRequest;
import org.apache.ldap.server.jndi.request.DeleteRequest;
import org.apache.ldap.server.jndi.request.GetMatchedNameRequest;
import org.apache.ldap.server.jndi.request.GetSuffixRequest;
import org.apache.ldap.server.jndi.request.HasEntryRequest;
import org.apache.ldap.server.jndi.request.IsSuffixRequest;
import org.apache.ldap.server.jndi.request.ListRequest;
import org.apache.ldap.server.jndi.request.ListSuffixesRequest;
import org.apache.ldap.server.jndi.request.LookUpRequest;
import org.apache.ldap.server.jndi.request.LookUpWithAttributeIdsRequest;
import org.apache.ldap.server.jndi.request.ModifyManyRequest;
import org.apache.ldap.server.jndi.request.ModifyRelativeNameRequest;
import org.apache.ldap.server.jndi.request.ModifyRequest;
import org.apache.ldap.server.jndi.request.MoveRequest;
import org.apache.ldap.server.jndi.request.MoveWithNewRelativeNameRequest;
import org.apache.ldap.server.jndi.request.Request;
import org.apache.ldap.server.jndi.request.SearchRequest;


/**
 * An interceptor base class which delegates handling of specific Invocations
 * to member methods within this Interceptor.  These handler methods are
 * analogous to the methods assocated with the Invocation.  They have the same
 * name and arguments as do the method associated with the Invocation.  The
 * analog member methods simply serve as a clean way to handle interception
 * without having to cast parameter Objects or recode this huge switch statement
 * for each concrete Interceptor implementation.
 *
 * A ThreadLocal is used by all BaseInterceptors to associate the current
 * Thread of execution with an Invocation object.  This is done to optimize
 * the use of a single thread local for all instances of the BaseInterceptor
 * class.  It also removes the need for the invoke() method implementation to
 * have to set and [un]set the thread local Invocation on each invoke call of
 * every BaseInterceptor instance.
 *
 * The question then arrises, "Why do we need the ThreadLocal?"  Well why pass
 * around the Invocation object to all analog methods.  Plus we use member
 * methods rather than static methods to access thread locals and make the
 * analogs appear cleaner matching their respective invocation methods.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public abstract class BaseRequestProcessor implements RequestProcessor
{
    /**
     * Gets the request's current context's Principal.
     * 
     * @return the principal making the call
     */
    public static LdapPrincipal getPrincipal( Request request )
    {
        ServerContext ctx = ( ServerContext ) request.getContextStack().peek();
        return ctx.getPrincipal();
    }

    protected BaseRequestProcessor()
    {
    }

    // ------------------------------------------------------------------------
    // Interceptor's Invoke Method
    // ------------------------------------------------------------------------

    /**
     * Uses a switch on the invocation method type to call the respective member
     * analog method that does the work of the Interceptor for that Invocation
     * method.
     */
    public void process( NextRequestProcessor nextProcessor, Request request )
            throws NamingException
    {
        if( request instanceof AddRequest )
        {
            process( nextProcessor, ( AddRequest ) request );
        }
        else if( request instanceof DeleteRequest )
        {
            process( nextProcessor, ( DeleteRequest ) request );
        }
        else if( request instanceof GetMatchedNameRequest )
        {
            process( nextProcessor, ( GetMatchedNameRequest ) request );
        }
        else if( request instanceof GetSuffixRequest )
        {
            process( nextProcessor, ( GetSuffixRequest ) request );
        }
        else if( request instanceof HasEntryRequest )
        {
            process( nextProcessor, ( HasEntryRequest ) request );
        }
        else if( request instanceof IsSuffixRequest )
        {
            process( nextProcessor, ( IsSuffixRequest ) request );
        }
        else if( request instanceof ListRequest )
        {
            process( nextProcessor, ( ListRequest ) request );
        }
        else if( request instanceof ListSuffixesRequest )
        {
            process( nextProcessor, ( ListSuffixesRequest ) request );
        }
        else if( request instanceof LookUpRequest )
        {
            process( nextProcessor, ( LookUpRequest ) request );
        }
        else if( request instanceof LookUpWithAttributeIdsRequest )
        {
            process( nextProcessor, ( LookUpWithAttributeIdsRequest ) request );
        }
        else if( request instanceof ModifyRequest )
        {
            process( nextProcessor, ( ModifyRequest ) request );
        }
        else if( request instanceof ModifyManyRequest )
        {
            process( nextProcessor, ( ModifyManyRequest ) request );
        }
        else if( request instanceof ModifyRelativeNameRequest )
        {
            process( nextProcessor, ( ModifyRelativeNameRequest ) request );
        }
        else if( request instanceof MoveRequest )
        {
            process( nextProcessor, ( MoveRequest ) request );
        }
        else if( request instanceof MoveWithNewRelativeNameRequest )
        {
            process( nextProcessor, ( MoveWithNewRelativeNameRequest ) request );
        }
        else if( request instanceof SearchRequest )
        {
            process( nextProcessor, ( SearchRequest ) request );
        }
        else {
            throw new IllegalArgumentException(
                    "Unknown request type: " + request.getClass() );
        }
    }

    // ------------------------------------------------------------------------
    // Invocation Analogs
    // ------------------------------------------------------------------------

    protected void process( NextRequestProcessor nextProcessor, AddRequest request )
            throws NamingException
    {
        nextProcessor.process( request );
    }

    protected void process( NextRequestProcessor nextProcessor, DeleteRequest request ) 
            throws NamingException
    {
        nextProcessor.process( request );
    }

    protected void process( NextRequestProcessor nextProcessor, GetMatchedNameRequest request ) 
            throws NamingException
    {
        nextProcessor.process( request );
    }

    protected void process( NextRequestProcessor nextProcessor, GetSuffixRequest request ) 
            throws NamingException
    {
        nextProcessor.process( request );
    }

    protected void process( NextRequestProcessor nextProcessor, HasEntryRequest request ) 
            throws NamingException
    {
        nextProcessor.process( request );
    }

    protected void process( NextRequestProcessor nextProcessor, IsSuffixRequest request ) 
            throws NamingException
    {
        nextProcessor.process( request );
    }

    protected void process( NextRequestProcessor nextProcessor, ListRequest request ) 
            throws NamingException
    {
        nextProcessor.process( request );
    }

    protected void process( NextRequestProcessor nextProcessor, ListSuffixesRequest request ) 
            throws NamingException
    {
        nextProcessor.process( request );
    }

    protected void process( NextRequestProcessor nextProcessor, LookUpRequest request ) 
            throws NamingException
    {
        nextProcessor.process( request );
    }

    protected void process( NextRequestProcessor nextProcessor, LookUpWithAttributeIdsRequest request ) 
            throws NamingException
    {
        nextProcessor.process( request );
    }

    protected void process( NextRequestProcessor nextProcessor, ModifyRequest request ) 
            throws NamingException
    {
        nextProcessor.process( request );
    }

    protected void process( NextRequestProcessor nextProcessor, ModifyManyRequest request ) 
            throws NamingException
    {
        nextProcessor.process( request );
    }

    protected void process( NextRequestProcessor nextProcessor, ModifyRelativeNameRequest request ) 
            throws NamingException
    {
        nextProcessor.process( request );
    }

    protected void process( NextRequestProcessor nextProcessor, MoveRequest request ) 
            throws NamingException
    {
        nextProcessor.process( request );
    }

    protected void process( NextRequestProcessor nextProcessor, MoveWithNewRelativeNameRequest request ) 
            throws NamingException
    {
        nextProcessor.process( request );
    }

    protected void process( NextRequestProcessor nextProcessor, SearchRequest request ) 
            throws NamingException
    {
        nextProcessor.process( request );
    }
}
