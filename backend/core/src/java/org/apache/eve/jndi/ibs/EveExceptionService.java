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
package org.apache.eve.jndi.ibs;


import javax.naming.Name;
import javax.naming.NamingException;
import javax.naming.directory.Attributes;

import org.apache.eve.jndi.*;
import org.apache.eve.jndi.exception.EveNameNotFoundException;
import org.apache.eve.jndi.exception.EveNameAlreadyBoundException;

import org.apache.eve.RootNexus;


/**
 * An interceptor based service used to detect, raise and handle eve exceptions
 * in one place.  This interceptor has two modes of operation.  The first mode
 * is as a before chain interceptor where it raises exceptions.  An example
 * where this interceptor raises an exception is when an entry already exists
 * at a DN and is added once again to the same DN.  The other mode is as an on
 * error chain interceptor.  In this mode the service may wrap exceptions and
 * add extra information to an exception which is to be thrown back at the
 * caller.
 *
 * @author <a href="mailto:directory-dev@incubator.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class EveExceptionService extends BaseInterceptor
{
    /** the root nexus of the system */
    private RootNexus nexus = null;


    /**
     * Creates an interceptor that is also the exception handling service.
     *
     * @param nexus the root partition nexus
     */
    public EveExceptionService( RootNexus nexus )
    {
        this.nexus = nexus;
    }


    /**
     * In the pre-invocation state this interceptor method checks to see if
     * the entry to be added already exists.  If it does an exception is
     * raised.
     *
     * @see BaseInterceptor#add(String, Name, Attributes)
     */
    protected void add( String upName, Name normName, Attributes entry ) throws NamingException
    {
        Invocation invocation = getInvocation();

        if ( invocation.getState() == InvocationStateEnum.PREINVOCATION )
        {
            if ( nexus.hasEntry( normName ) )
            {
                NamingException ne = new EveNameAlreadyBoundException();
                invocation.setBeforeFailure( ne );
                throw ne;
            }
        }
    }


    /**
     * @see BaseInterceptor#lookup(javax.naming.Name)
     */
    protected void lookup( Name dn ) throws NamingException
    {
        Invocation invocation = getInvocation();

        if ( invocation.getState() == InvocationStateEnum.PREINVOCATION )
        {
            if ( ! nexus.hasEntry( dn ) )
            {
                NamingException ne = new EveNameNotFoundException();
                invocation.setBeforeFailure( ne );
                throw ne;
            }
        }
    }
}
