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
package org.apache.ldap.server.jndi.request.interceptor;


import javax.naming.NamingException;

import org.apache.ldap.server.auth.LdapPrincipal;
import org.apache.ldap.server.jndi.ServerContext;
import org.apache.ldap.server.jndi.request.Add;
import org.apache.ldap.server.jndi.request.Delete;
import org.apache.ldap.server.jndi.request.GetMatchedDN;
import org.apache.ldap.server.jndi.request.GetSuffix;
import org.apache.ldap.server.jndi.request.HasEntry;
import org.apache.ldap.server.jndi.request.IsSuffix;
import org.apache.ldap.server.jndi.request.List;
import org.apache.ldap.server.jndi.request.ListSuffixes;
import org.apache.ldap.server.jndi.request.Lookup;
import org.apache.ldap.server.jndi.request.LookupWithAttrIds;
import org.apache.ldap.server.jndi.request.ModifyMany;
import org.apache.ldap.server.jndi.request.ModifyRN;
import org.apache.ldap.server.jndi.request.Modify;
import org.apache.ldap.server.jndi.request.Move;
import org.apache.ldap.server.jndi.request.MoveAndModifyRN;
import org.apache.ldap.server.jndi.request.Call;
import org.apache.ldap.server.jndi.request.Search;


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
public abstract class BaseInterceptor implements Interceptor
{
    /**
     * Gets the request's current context's Principal.
     * 
     * @return the principal making the call
     */
    public static LdapPrincipal getPrincipal( Call request )
    {
        ServerContext ctx = ( ServerContext ) request.getContextStack().peek();
        return ctx.getPrincipal();
    }

    protected BaseInterceptor()
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
    public void process( NextInterceptor nextProcessor, Call request )
            throws NamingException
    {
        if( request instanceof Add )
        {
            process( nextProcessor, ( Add ) request );
        }
        else if( request instanceof Delete )
        {
            process( nextProcessor, ( Delete ) request );
        }
        else if( request instanceof GetMatchedDN )
        {
            process( nextProcessor, ( GetMatchedDN ) request );
        }
        else if( request instanceof GetSuffix )
        {
            process( nextProcessor, ( GetSuffix ) request );
        }
        else if( request instanceof HasEntry )
        {
            process( nextProcessor, ( HasEntry ) request );
        }
        else if( request instanceof IsSuffix )
        {
            process( nextProcessor, ( IsSuffix ) request );
        }
        else if( request instanceof List )
        {
            process( nextProcessor, ( List ) request );
        }
        else if( request instanceof ListSuffixes )
        {
            process( nextProcessor, ( ListSuffixes ) request );
        }
        else if( request instanceof Lookup )
        {
            process( nextProcessor, ( Lookup ) request );
        }
        else if( request instanceof LookupWithAttrIds )
        {
            process( nextProcessor, ( LookupWithAttrIds ) request );
        }
        else if( request instanceof Modify )
        {
            process( nextProcessor, ( Modify ) request );
        }
        else if( request instanceof ModifyMany )
        {
            process( nextProcessor, ( ModifyMany ) request );
        }
        else if( request instanceof ModifyRN )
        {
            process( nextProcessor, ( ModifyRN ) request );
        }
        else if( request instanceof Move )
        {
            process( nextProcessor, ( Move ) request );
        }
        else if( request instanceof MoveAndModifyRN )
        {
            process( nextProcessor, ( MoveAndModifyRN ) request );
        }
        else if( request instanceof Search )
        {
            process( nextProcessor, ( Search ) request );
        }
        else {
            throw new IllegalArgumentException(
                    "Unknown request type: " + request.getClass() );
        }
    }

    // ------------------------------------------------------------------------
    // Invocation Analogs
    // ------------------------------------------------------------------------

    protected void process( NextInterceptor nextProcessor, Add request )
            throws NamingException
    {
        nextProcessor.process( request );
    }

    protected void process( NextInterceptor nextProcessor, Delete request ) 
            throws NamingException
    {
        nextProcessor.process( request );
    }

    protected void process( NextInterceptor nextProcessor, GetMatchedDN request ) 
            throws NamingException
    {
        nextProcessor.process( request );
    }

    protected void process( NextInterceptor nextProcessor, GetSuffix request ) 
            throws NamingException
    {
        nextProcessor.process( request );
    }

    protected void process( NextInterceptor nextProcessor, HasEntry request ) 
            throws NamingException
    {
        nextProcessor.process( request );
    }

    protected void process( NextInterceptor nextProcessor, IsSuffix request ) 
            throws NamingException
    {
        nextProcessor.process( request );
    }

    protected void process( NextInterceptor nextProcessor, List request ) 
            throws NamingException
    {
        nextProcessor.process( request );
    }

    protected void process( NextInterceptor nextProcessor, ListSuffixes request ) 
            throws NamingException
    {
        nextProcessor.process( request );
    }

    protected void process( NextInterceptor nextProcessor, Lookup request ) 
            throws NamingException
    {
        nextProcessor.process( request );
    }

    protected void process( NextInterceptor nextProcessor, LookupWithAttrIds request ) 
            throws NamingException
    {
        nextProcessor.process( request );
    }

    protected void process( NextInterceptor nextProcessor, Modify request ) 
            throws NamingException
    {
        nextProcessor.process( request );
    }

    protected void process( NextInterceptor nextProcessor, ModifyMany request ) 
            throws NamingException
    {
        nextProcessor.process( request );
    }

    protected void process( NextInterceptor nextProcessor, ModifyRN request ) 
            throws NamingException
    {
        nextProcessor.process( request );
    }

    protected void process( NextInterceptor nextProcessor, Move request ) 
            throws NamingException
    {
        nextProcessor.process( request );
    }

    protected void process( NextInterceptor nextProcessor, MoveAndModifyRN request ) 
            throws NamingException
    {
        nextProcessor.process( request );
    }

    protected void process( NextInterceptor nextProcessor, Search request ) 
            throws NamingException
    {
        nextProcessor.process( request );
    }
}
