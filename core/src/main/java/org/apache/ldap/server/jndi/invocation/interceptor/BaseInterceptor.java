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
package org.apache.ldap.server.jndi.call.interceptor;


import javax.naming.NamingException;

import org.apache.ldap.server.auth.LdapPrincipal;
import org.apache.ldap.server.jndi.ServerContext;
import org.apache.ldap.server.jndi.call.Add;
import org.apache.ldap.server.jndi.call.Call;
import org.apache.ldap.server.jndi.call.Delete;
import org.apache.ldap.server.jndi.call.GetMatchedDN;
import org.apache.ldap.server.jndi.call.GetSuffix;
import org.apache.ldap.server.jndi.call.HasEntry;
import org.apache.ldap.server.jndi.call.IsSuffix;
import org.apache.ldap.server.jndi.call.List;
import org.apache.ldap.server.jndi.call.ListSuffixes;
import org.apache.ldap.server.jndi.call.Lookup;
import org.apache.ldap.server.jndi.call.LookupWithAttrIds;
import org.apache.ldap.server.jndi.call.Modify;
import org.apache.ldap.server.jndi.call.ModifyMany;
import org.apache.ldap.server.jndi.call.ModifyRN;
import org.apache.ldap.server.jndi.call.Move;
import org.apache.ldap.server.jndi.call.MoveAndModifyRN;
import org.apache.ldap.server.jndi.call.Search;


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
     * Gets the call's current context's Principal.
     * 
     * @return the principal making the call
     */
    public static LdapPrincipal getPrincipal( Call call )
    {
        ServerContext ctx = ( ServerContext ) call.getContextStack().peek();
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
    public void process( NextInterceptor nextInterceptor, Call call )
            throws NamingException
    {
        if( call instanceof Add )
        {
            process( nextInterceptor, ( Add ) call );
        }
        else if( call instanceof Delete )
        {
            process( nextInterceptor, ( Delete ) call );
        }
        else if( call instanceof GetMatchedDN )
        {
            process( nextInterceptor, ( GetMatchedDN ) call );
        }
        else if( call instanceof GetSuffix )
        {
            process( nextInterceptor, ( GetSuffix ) call );
        }
        else if( call instanceof HasEntry )
        {
            process( nextInterceptor, ( HasEntry ) call );
        }
        else if( call instanceof IsSuffix )
        {
            process( nextInterceptor, ( IsSuffix ) call );
        }
        else if( call instanceof List )
        {
            process( nextInterceptor, ( List ) call );
        }
        else if( call instanceof ListSuffixes )
        {
            process( nextInterceptor, ( ListSuffixes ) call );
        }
        else if( call instanceof Lookup )
        {
            process( nextInterceptor, ( Lookup ) call );
        }
        else if( call instanceof LookupWithAttrIds )
        {
            process( nextInterceptor, ( LookupWithAttrIds ) call );
        }
        else if( call instanceof Modify )
        {
            process( nextInterceptor, ( Modify ) call );
        }
        else if( call instanceof ModifyMany )
        {
            process( nextInterceptor, ( ModifyMany ) call );
        }
        else if( call instanceof ModifyRN )
        {
            process( nextInterceptor, ( ModifyRN ) call );
        }
        else if( call instanceof Move )
        {
            process( nextInterceptor, ( Move ) call );
        }
        else if( call instanceof MoveAndModifyRN )
        {
            process( nextInterceptor, ( MoveAndModifyRN ) call );
        }
        else if( call instanceof Search )
        {
            process( nextInterceptor, ( Search ) call );
        }
        else {
            throw new IllegalArgumentException(
                    "Unknown call type: " + call.getClass() );
        }
    }

    // ------------------------------------------------------------------------
    // Invocation Analogs
    // ------------------------------------------------------------------------

    protected void process( NextInterceptor nextInterceptor, Add call )
            throws NamingException
    {
        nextInterceptor.process( call );
    }

    protected void process( NextInterceptor nextInterceptor, Delete call ) 
            throws NamingException
    {
        nextInterceptor.process( call );
    }

    protected void process( NextInterceptor nextInterceptor, GetMatchedDN call ) 
            throws NamingException
    {
        nextInterceptor.process( call );
    }

    protected void process( NextInterceptor nextInterceptor, GetSuffix call ) 
            throws NamingException
    {
        nextInterceptor.process( call );
    }

    protected void process( NextInterceptor nextInterceptor, HasEntry call ) 
            throws NamingException
    {
        nextInterceptor.process( call );
    }

    protected void process( NextInterceptor nextInterceptor, IsSuffix call ) 
            throws NamingException
    {
        nextInterceptor.process( call );
    }

    protected void process( NextInterceptor nextInterceptor, List call ) 
            throws NamingException
    {
        nextInterceptor.process( call );
    }

    protected void process( NextInterceptor nextInterceptor, ListSuffixes call ) 
            throws NamingException
    {
        nextInterceptor.process( call );
    }

    protected void process( NextInterceptor nextInterceptor, Lookup call ) 
            throws NamingException
    {
        nextInterceptor.process( call );
    }

    protected void process( NextInterceptor nextInterceptor, LookupWithAttrIds call ) 
            throws NamingException
    {
        nextInterceptor.process( call );
    }

    protected void process( NextInterceptor nextInterceptor, Modify call ) 
            throws NamingException
    {
        nextInterceptor.process( call );
    }

    protected void process( NextInterceptor nextInterceptor, ModifyMany call ) 
            throws NamingException
    {
        nextInterceptor.process( call );
    }

    protected void process( NextInterceptor nextInterceptor, ModifyRN call ) 
            throws NamingException
    {
        nextInterceptor.process( call );
    }

    protected void process( NextInterceptor nextInterceptor, Move call ) 
            throws NamingException
    {
        nextInterceptor.process( call );
    }

    protected void process( NextInterceptor nextInterceptor, MoveAndModifyRN call ) 
            throws NamingException
    {
        nextInterceptor.process( call );
    }

    protected void process( NextInterceptor nextInterceptor, Search call ) 
            throws NamingException
    {
        nextInterceptor.process( call );
    }
}
