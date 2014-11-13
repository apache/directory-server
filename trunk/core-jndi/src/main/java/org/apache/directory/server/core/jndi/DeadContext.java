/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *  
 *    http://www.apache.org/licenses/LICENSE-2.0
 *  
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License. 
 *  
 */
package org.apache.directory.server.core.jndi;


import java.util.Hashtable;

import javax.naming.Context;
import javax.naming.Name;
import javax.naming.NameClassPair;
import javax.naming.NameParser;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.ServiceUnavailableException;
import javax.naming.event.EventDirContext;
import javax.naming.event.NamingListener;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.ModificationItem;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;
import javax.naming.ldap.Control;
import javax.naming.ldap.ExtendedRequest;
import javax.naming.ldap.ExtendedResponse;
import javax.naming.ldap.LdapContext;

import javax.naming.Binding;


/**
 * A do nothing placeholder context whose methods throw ServiceUnavailableExceptions.
 * JNDI provider returns this context when you perform JNDI operations against the
 * core directory service that has been shutdown or not started.  By returning a
 * non-null Context we prevent an unnecessary exception being thrown by
 * {@link javax.naming.InitialContext} and any one of its subclasses.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class DeadContext implements LdapContext, EventDirContext
{
    private static final String EXCEPTION_MSG = "Context operation unavailable when "
        + "invoked after directory service core provider has been shutdown";


    public Control[] getConnectControls() throws NamingException
    {
        throw new ServiceUnavailableException( EXCEPTION_MSG );
    }


    public Control[] getRequestControls() throws NamingException
    {
        throw new ServiceUnavailableException( EXCEPTION_MSG );
    }


    public Control[] getResponseControls() throws NamingException
    {
        throw new ServiceUnavailableException( EXCEPTION_MSG );
    }


    public void reconnect( Control[] connCtls ) throws NamingException
    {
        throw new ServiceUnavailableException( EXCEPTION_MSG );
    }


    public void setRequestControls( Control[] requestControls ) throws NamingException
    {
        throw new ServiceUnavailableException( EXCEPTION_MSG );
    }


    public ExtendedResponse extendedOperation( ExtendedRequest request ) throws NamingException
    {
        throw new ServiceUnavailableException( EXCEPTION_MSG );
    }


    public LdapContext newInstance( Control[] requestControls ) throws NamingException
    {
        throw new ServiceUnavailableException( EXCEPTION_MSG );
    }


    public Attributes getAttributes( String name ) throws NamingException
    {
        throw new ServiceUnavailableException( EXCEPTION_MSG );
    }


    public void modifyAttributes( String name, int mod_op, Attributes attrs ) throws NamingException
    {
        throw new ServiceUnavailableException( EXCEPTION_MSG );
    }


    public Attributes getAttributes( Name name ) throws NamingException
    {
        throw new ServiceUnavailableException( EXCEPTION_MSG );
    }


    public void modifyAttributes( Name name, int mod_op, Attributes attrs ) throws NamingException
    {
        throw new ServiceUnavailableException( EXCEPTION_MSG );
    }


    public void modifyAttributes( Name name, ModificationItem[] mods ) throws NamingException
    {
        throw new ServiceUnavailableException( EXCEPTION_MSG );
    }


    public DirContext getSchema( String name ) throws NamingException
    {
        throw new ServiceUnavailableException( EXCEPTION_MSG );
    }


    public DirContext getSchemaClassDefinition( String name ) throws NamingException
    {
        throw new ServiceUnavailableException( EXCEPTION_MSG );
    }


    public DirContext getSchema( Name name ) throws NamingException
    {
        throw new ServiceUnavailableException( EXCEPTION_MSG );
    }


    public DirContext getSchemaClassDefinition( Name name ) throws NamingException
    {
        throw new ServiceUnavailableException( EXCEPTION_MSG );
    }


    public void modifyAttributes( String name, ModificationItem[] mods ) throws NamingException
    {
        throw new ServiceUnavailableException( EXCEPTION_MSG );
    }


    public NamingEnumeration<SearchResult> search( String name, Attributes matchingAttributes ) throws NamingException
    {
        throw new ServiceUnavailableException( EXCEPTION_MSG );
    }


    public NamingEnumeration<SearchResult> search( Name name, Attributes matchingAttributes ) throws NamingException
    {
        throw new ServiceUnavailableException( EXCEPTION_MSG );
    }


    public void bind( String name, Object obj, Attributes attrs ) throws NamingException
    {
        throw new ServiceUnavailableException( EXCEPTION_MSG );
    }


    public void rebind( String name, Object obj, Attributes attrs ) throws NamingException
    {
        throw new ServiceUnavailableException( EXCEPTION_MSG );
    }


    public void bind( Name name, Object obj, Attributes attrs ) throws NamingException
    {
        throw new ServiceUnavailableException( EXCEPTION_MSG );
    }


    public void rebind( Name name, Object obj, Attributes attrs ) throws NamingException
    {
        throw new ServiceUnavailableException( EXCEPTION_MSG );
    }


    public Attributes getAttributes( String name, String[] attrIds ) throws NamingException
    {
        throw new ServiceUnavailableException( EXCEPTION_MSG );
    }


    public Attributes getAttributes( Name name, String[] attrIds ) throws NamingException
    {
        throw new ServiceUnavailableException( EXCEPTION_MSG );
    }


    public DirContext createSubcontext( String name, Attributes attrs ) throws NamingException
    {
        throw new ServiceUnavailableException( EXCEPTION_MSG );
    }


    public DirContext createSubcontext( Name name, Attributes attrs ) throws NamingException
    {
        throw new ServiceUnavailableException( EXCEPTION_MSG );
    }


    public NamingEnumeration<SearchResult> search( String name, Attributes matchingAttributes,
        String[] attributesToReturn )
        throws NamingException
    {
        throw new ServiceUnavailableException( EXCEPTION_MSG );
    }


    public NamingEnumeration<SearchResult> search( Name name, Attributes matchingAttributes, String[] attributesToReturn )
        throws NamingException
    {
        throw new ServiceUnavailableException( EXCEPTION_MSG );
    }


    public NamingEnumeration<SearchResult> search( String name, String filter, SearchControls cons )
        throws NamingException
    {
        throw new ServiceUnavailableException( EXCEPTION_MSG );
    }


    public NamingEnumeration<SearchResult> search( Name name, String filter, SearchControls cons )
        throws NamingException
    {
        throw new ServiceUnavailableException( EXCEPTION_MSG );
    }


    public NamingEnumeration<SearchResult> search( String name, String filterExpr, Object[] filterArgs,
        SearchControls cons )
        throws NamingException
    {
        throw new ServiceUnavailableException( EXCEPTION_MSG );
    }


    public NamingEnumeration<SearchResult> search( Name name, String filterExpr, Object[] filterArgs,
        SearchControls cons )
        throws NamingException
    {
        throw new ServiceUnavailableException( EXCEPTION_MSG );
    }


    public void close() throws NamingException
    {
        throw new ServiceUnavailableException( EXCEPTION_MSG );
    }


    public String getNameInNamespace() throws NamingException
    {
        throw new ServiceUnavailableException( EXCEPTION_MSG );
    }


    public void destroySubcontext( String name ) throws NamingException
    {
        throw new ServiceUnavailableException( EXCEPTION_MSG );
    }


    public void unbind( String name ) throws NamingException
    {
        throw new ServiceUnavailableException( EXCEPTION_MSG );
    }


    public Hashtable<String, Object> getEnvironment() throws NamingException
    {
        throw new ServiceUnavailableException( EXCEPTION_MSG );
    }


    public void destroySubcontext( Name name ) throws NamingException
    {
        throw new ServiceUnavailableException( EXCEPTION_MSG );
    }


    public void unbind( Name name ) throws NamingException
    {
        throw new ServiceUnavailableException( EXCEPTION_MSG );
    }


    public Object lookup( String name ) throws NamingException
    {
        throw new ServiceUnavailableException( EXCEPTION_MSG );
    }


    public Object lookupLink( String name ) throws NamingException
    {
        throw new ServiceUnavailableException( EXCEPTION_MSG );
    }


    public Object removeFromEnvironment( String propName ) throws NamingException
    {
        throw new ServiceUnavailableException( EXCEPTION_MSG );
    }


    public void bind( String name, Object obj ) throws NamingException
    {
        throw new ServiceUnavailableException( EXCEPTION_MSG );
    }


    public void rebind( String name, Object obj ) throws NamingException
    {
        throw new ServiceUnavailableException( EXCEPTION_MSG );
    }


    public Object lookup( Name name ) throws NamingException
    {
        throw new ServiceUnavailableException( EXCEPTION_MSG );
    }


    public Object lookupLink( Name name ) throws NamingException
    {
        throw new ServiceUnavailableException( EXCEPTION_MSG );
    }


    public void bind( Name name, Object obj ) throws NamingException
    {
        throw new ServiceUnavailableException( EXCEPTION_MSG );
    }


    public void rebind( Name name, Object obj ) throws NamingException
    {
        throw new ServiceUnavailableException( EXCEPTION_MSG );
    }


    public void rename( String oldName, String newName ) throws NamingException
    {
        throw new ServiceUnavailableException( EXCEPTION_MSG );
    }


    public Context createSubcontext( String name ) throws NamingException
    {
        throw new ServiceUnavailableException( EXCEPTION_MSG );
    }


    public Context createSubcontext( Name name ) throws NamingException
    {
        throw new ServiceUnavailableException( EXCEPTION_MSG );
    }


    public void rename( Name oldName, Name newName ) throws NamingException
    {
        throw new ServiceUnavailableException( EXCEPTION_MSG );
    }


    public NameParser getNameParser( String name ) throws NamingException
    {
        throw new ServiceUnavailableException( EXCEPTION_MSG );
    }


    public NameParser getNameParser( Name name ) throws NamingException
    {
        throw new ServiceUnavailableException( EXCEPTION_MSG );
    }


    public NamingEnumeration<NameClassPair> list( String name ) throws NamingException
    {
        throw new ServiceUnavailableException( EXCEPTION_MSG );
    }


    public NamingEnumeration<Binding> listBindings( String name ) throws NamingException
    {
        throw new ServiceUnavailableException( EXCEPTION_MSG );
    }


    public NamingEnumeration<NameClassPair> list( Name name ) throws NamingException
    {
        throw new ServiceUnavailableException( EXCEPTION_MSG );
    }


    public NamingEnumeration<Binding> listBindings( Name name ) throws NamingException
    {
        throw new ServiceUnavailableException( EXCEPTION_MSG );
    }


    public Object addToEnvironment( String propName, Object propVal ) throws NamingException
    {
        throw new ServiceUnavailableException( EXCEPTION_MSG );
    }


    public String composeName( String name, String prefix ) throws NamingException
    {
        throw new ServiceUnavailableException( EXCEPTION_MSG );
    }


    public Name composeName( Name name, Name prefix ) throws NamingException
    {
        throw new ServiceUnavailableException( EXCEPTION_MSG );
    }


    public void addNamingListener( Name name, String s, SearchControls searchControls, NamingListener namingListener )
        throws NamingException
    {
        throw new ServiceUnavailableException( EXCEPTION_MSG );
    }


    public void addNamingListener( String s, String s1, SearchControls searchControls, NamingListener namingListener )
        throws NamingException
    {
        throw new ServiceUnavailableException( EXCEPTION_MSG );
    }


    public void addNamingListener( Name name, String s, Object[] objects, SearchControls searchControls,
        NamingListener namingListener ) throws NamingException
    {
        throw new ServiceUnavailableException( EXCEPTION_MSG );
    }


    public void addNamingListener( String s, String s1, Object[] objects, SearchControls searchControls,
        NamingListener namingListener ) throws NamingException
    {
        throw new ServiceUnavailableException( EXCEPTION_MSG );
    }


    public void addNamingListener( Name name, int i, NamingListener namingListener ) throws NamingException
    {
        throw new ServiceUnavailableException( EXCEPTION_MSG );
    }


    public void addNamingListener( String s, int i, NamingListener namingListener ) throws NamingException
    {
        throw new ServiceUnavailableException( EXCEPTION_MSG );
    }


    public void removeNamingListener( NamingListener namingListener ) throws NamingException
    {
        throw new ServiceUnavailableException( EXCEPTION_MSG );
    }


    public boolean targetMustExist() throws NamingException
    {
        throw new ServiceUnavailableException( EXCEPTION_MSG );
    }
}
