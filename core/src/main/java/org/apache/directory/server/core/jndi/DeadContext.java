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
package org.apache.directory.server.core.jndi;


import java.util.Hashtable;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.Name;
import javax.naming.NameParser;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.event.EventDirContext;
import javax.naming.event.NamingListener;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.ModificationItem;
import javax.naming.directory.SearchControls;
import javax.naming.ldap.Control;
import javax.naming.ldap.ExtendedRequest;
import javax.naming.ldap.ExtendedResponse;
import javax.naming.ldap.LdapContext;

import org.apache.directory.server.core.configuration.SyncConfiguration;
import org.apache.directory.shared.ldap.exception.LdapServiceUnavailableException;
import org.apache.directory.shared.ldap.message.ResultCodeEnum;


/**
 * A do nothing placeholder context whose methods throw ConfigurationExceptions.
 * JNDI provider returns this context when your specify {@link SyncConfiguration}
 * in JNDI environment.  By returning a non-null Context we prevent an unnecessary
 * exception being thrown by {@link InitialContext} and any one of its subclasses.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class DeadContext implements LdapContext, EventDirContext
{
    private final String EXCEPTION_MSG = "Context operation unavailable when " +
            "invoked after Eve provider has been shutdown";


    public Control[] getConnectControls() throws NamingException
    {
        throw new LdapServiceUnavailableException( EXCEPTION_MSG, ResultCodeEnum.UNAVAILABLE );
    }


    public Control[] getRequestControls() throws NamingException
    {
        throw new LdapServiceUnavailableException( EXCEPTION_MSG, ResultCodeEnum.UNAVAILABLE );
    }


    public Control[] getResponseControls() throws NamingException
    {
        throw new LdapServiceUnavailableException( EXCEPTION_MSG, ResultCodeEnum.UNAVAILABLE );
    }


    public void reconnect( Control[] connCtls ) throws NamingException
    {
        throw new LdapServiceUnavailableException( EXCEPTION_MSG, ResultCodeEnum.UNAVAILABLE );
    }


    public void setRequestControls( Control[] requestControls ) throws NamingException
    {
        throw new LdapServiceUnavailableException( EXCEPTION_MSG, ResultCodeEnum.UNAVAILABLE );
    }


    public ExtendedResponse extendedOperation( ExtendedRequest request ) throws NamingException
    {
        throw new LdapServiceUnavailableException( EXCEPTION_MSG, ResultCodeEnum.UNAVAILABLE );
    }


    public LdapContext newInstance( Control[] requestControls ) throws NamingException
    {
        throw new LdapServiceUnavailableException( EXCEPTION_MSG, ResultCodeEnum.UNAVAILABLE );
    }


    public Attributes getAttributes( String name ) throws NamingException
    {
        throw new LdapServiceUnavailableException( EXCEPTION_MSG, ResultCodeEnum.UNAVAILABLE );
    }


    public void modifyAttributes( String name, int mod_op, Attributes attrs ) throws NamingException
    {
        throw new LdapServiceUnavailableException( EXCEPTION_MSG, ResultCodeEnum.UNAVAILABLE );
    }


    public Attributes getAttributes( Name name ) throws NamingException
    {
        throw new LdapServiceUnavailableException( EXCEPTION_MSG, ResultCodeEnum.UNAVAILABLE );
    }


    public void modifyAttributes( Name name, int mod_op, Attributes attrs ) throws NamingException
    {
        throw new LdapServiceUnavailableException( EXCEPTION_MSG, ResultCodeEnum.UNAVAILABLE );
    }


    public DirContext getSchema( String name ) throws NamingException
    {
        throw new LdapServiceUnavailableException( EXCEPTION_MSG, ResultCodeEnum.UNAVAILABLE );
    }


    public DirContext getSchemaClassDefinition( String name ) throws NamingException
    {
        throw new LdapServiceUnavailableException( EXCEPTION_MSG, ResultCodeEnum.UNAVAILABLE );
    }


    public DirContext getSchema( Name name ) throws NamingException
    {
        throw new LdapServiceUnavailableException( EXCEPTION_MSG, ResultCodeEnum.UNAVAILABLE );
    }


    public DirContext getSchemaClassDefinition( Name name ) throws NamingException
    {
        throw new LdapServiceUnavailableException( EXCEPTION_MSG, ResultCodeEnum.UNAVAILABLE );
    }


    public void modifyAttributes( String name, ModificationItem[] mods ) throws NamingException
    {
        throw new LdapServiceUnavailableException( EXCEPTION_MSG, ResultCodeEnum.UNAVAILABLE );
    }


    public void modifyAttributes( Name name, ModificationItem[] mods ) throws NamingException
    {
        throw new LdapServiceUnavailableException( EXCEPTION_MSG, ResultCodeEnum.UNAVAILABLE );
    }


    public NamingEnumeration search( String name, Attributes matchingAttributes ) throws NamingException
    {
        throw new LdapServiceUnavailableException( EXCEPTION_MSG, ResultCodeEnum.UNAVAILABLE );
    }


    public NamingEnumeration search( Name name, Attributes matchingAttributes ) throws NamingException
    {
        throw new LdapServiceUnavailableException( EXCEPTION_MSG, ResultCodeEnum.UNAVAILABLE );
    }


    public void bind( String name, Object obj, Attributes attrs ) throws NamingException
    {
        throw new LdapServiceUnavailableException( EXCEPTION_MSG, ResultCodeEnum.UNAVAILABLE );
    }


    public void rebind( String name, Object obj, Attributes attrs ) throws NamingException
    {
        throw new LdapServiceUnavailableException( EXCEPTION_MSG, ResultCodeEnum.UNAVAILABLE );
    }


    public void bind( Name name, Object obj, Attributes attrs ) throws NamingException
    {
        throw new LdapServiceUnavailableException( EXCEPTION_MSG, ResultCodeEnum.UNAVAILABLE );
    }


    public void rebind( Name name, Object obj, Attributes attrs ) throws NamingException
    {
        throw new LdapServiceUnavailableException( EXCEPTION_MSG, ResultCodeEnum.UNAVAILABLE );
    }


    public Attributes getAttributes( String name, String[] attrIds ) throws NamingException
    {
        throw new LdapServiceUnavailableException( EXCEPTION_MSG, ResultCodeEnum.UNAVAILABLE );
    }


    public Attributes getAttributes( Name name, String[] attrIds ) throws NamingException
    {
        throw new LdapServiceUnavailableException( EXCEPTION_MSG, ResultCodeEnum.UNAVAILABLE );
    }


    public DirContext createSubcontext( String name, Attributes attrs ) throws NamingException
    {
        throw new LdapServiceUnavailableException( EXCEPTION_MSG, ResultCodeEnum.UNAVAILABLE );
    }


    public DirContext createSubcontext( Name name, Attributes attrs ) throws NamingException
    {
        throw new LdapServiceUnavailableException( EXCEPTION_MSG, ResultCodeEnum.UNAVAILABLE );
    }


    public NamingEnumeration search( String name, Attributes matchingAttributes, String[] attributesToReturn ) throws NamingException
    {
        throw new LdapServiceUnavailableException( EXCEPTION_MSG, ResultCodeEnum.UNAVAILABLE );
    }


    public NamingEnumeration search( Name name, Attributes matchingAttributes, String[] attributesToReturn ) throws NamingException
    {
        throw new LdapServiceUnavailableException( EXCEPTION_MSG, ResultCodeEnum.UNAVAILABLE );
    }


    public NamingEnumeration search( String name, String filter, SearchControls cons ) throws NamingException
    {
        throw new LdapServiceUnavailableException( EXCEPTION_MSG, ResultCodeEnum.UNAVAILABLE );
    }


    public NamingEnumeration search( Name name, String filter, SearchControls cons ) throws NamingException
    {
        throw new LdapServiceUnavailableException( EXCEPTION_MSG, ResultCodeEnum.UNAVAILABLE );
    }


    public NamingEnumeration search( String name, String filterExpr, Object[] filterArgs, SearchControls cons ) throws NamingException
    {
        throw new LdapServiceUnavailableException( EXCEPTION_MSG, ResultCodeEnum.UNAVAILABLE );
    }


    public NamingEnumeration search( Name name, String filterExpr, Object[] filterArgs, SearchControls cons ) throws NamingException
    {
        throw new LdapServiceUnavailableException( EXCEPTION_MSG, ResultCodeEnum.UNAVAILABLE );
    }


    public void close() throws NamingException
    {
        throw new LdapServiceUnavailableException( EXCEPTION_MSG, ResultCodeEnum.UNAVAILABLE );
    }


    public String getNameInNamespace() throws NamingException
    {
        throw new LdapServiceUnavailableException( EXCEPTION_MSG, ResultCodeEnum.UNAVAILABLE );
    }


    public void destroySubcontext( String name ) throws NamingException
    {
        throw new LdapServiceUnavailableException( EXCEPTION_MSG, ResultCodeEnum.UNAVAILABLE );
    }


    public void unbind( String name ) throws NamingException
    {
        throw new LdapServiceUnavailableException( EXCEPTION_MSG, ResultCodeEnum.UNAVAILABLE );
    }


    public Hashtable getEnvironment() throws NamingException
    {
        throw new LdapServiceUnavailableException( EXCEPTION_MSG, ResultCodeEnum.UNAVAILABLE );
    }


    public void destroySubcontext( Name name ) throws NamingException
    {
        throw new LdapServiceUnavailableException( EXCEPTION_MSG, ResultCodeEnum.UNAVAILABLE );
    }


    public void unbind( Name name ) throws NamingException
    {
        throw new LdapServiceUnavailableException( EXCEPTION_MSG, ResultCodeEnum.UNAVAILABLE );
    }


    public Object lookup( String name ) throws NamingException
    {
        throw new LdapServiceUnavailableException( EXCEPTION_MSG, ResultCodeEnum.UNAVAILABLE );
    }


    public Object lookupLink( String name ) throws NamingException
    {
        throw new LdapServiceUnavailableException( EXCEPTION_MSG, ResultCodeEnum.UNAVAILABLE );
    }


    public Object removeFromEnvironment( String propName ) throws NamingException
    {
        throw new LdapServiceUnavailableException( EXCEPTION_MSG, ResultCodeEnum.UNAVAILABLE );
    }


    public void bind( String name, Object obj ) throws NamingException
    {
        throw new LdapServiceUnavailableException( EXCEPTION_MSG, ResultCodeEnum.UNAVAILABLE );
    }


    public void rebind( String name, Object obj ) throws NamingException
    {
        throw new LdapServiceUnavailableException( EXCEPTION_MSG, ResultCodeEnum.UNAVAILABLE );
    }


    public Object lookup( Name name ) throws NamingException
    {
        throw new LdapServiceUnavailableException( EXCEPTION_MSG, ResultCodeEnum.UNAVAILABLE );
    }


    public Object lookupLink( Name name ) throws NamingException
    {
        throw new LdapServiceUnavailableException( EXCEPTION_MSG, ResultCodeEnum.UNAVAILABLE );
    }


    public void bind( Name name, Object obj ) throws NamingException
    {
        throw new LdapServiceUnavailableException( EXCEPTION_MSG, ResultCodeEnum.UNAVAILABLE );
    }


    public void rebind( Name name, Object obj ) throws NamingException
    {
        throw new LdapServiceUnavailableException( EXCEPTION_MSG, ResultCodeEnum.UNAVAILABLE );
    }


    public void rename( String oldName, String newName ) throws NamingException
    {
        throw new LdapServiceUnavailableException( EXCEPTION_MSG, ResultCodeEnum.UNAVAILABLE );
    }


    public Context createSubcontext( String name ) throws NamingException
    {
        throw new LdapServiceUnavailableException( EXCEPTION_MSG, ResultCodeEnum.UNAVAILABLE );
    }


    public Context createSubcontext( Name name ) throws NamingException
    {
        throw new LdapServiceUnavailableException( EXCEPTION_MSG, ResultCodeEnum.UNAVAILABLE );
    }


    public void rename( Name oldName, Name newName ) throws NamingException
    {
        throw new LdapServiceUnavailableException( EXCEPTION_MSG, ResultCodeEnum.UNAVAILABLE );
    }


    public NameParser getNameParser( String name ) throws NamingException
    {
        throw new LdapServiceUnavailableException( EXCEPTION_MSG, ResultCodeEnum.UNAVAILABLE );
    }


    public NameParser getNameParser( Name name ) throws NamingException
    {
        throw new LdapServiceUnavailableException( EXCEPTION_MSG, ResultCodeEnum.UNAVAILABLE );
    }


    public NamingEnumeration list( String name ) throws NamingException
    {
        throw new LdapServiceUnavailableException( EXCEPTION_MSG, ResultCodeEnum.UNAVAILABLE );
    }


    public NamingEnumeration listBindings( String name ) throws NamingException
    {
        throw new LdapServiceUnavailableException( EXCEPTION_MSG, ResultCodeEnum.UNAVAILABLE );
    }


    public NamingEnumeration list( Name name ) throws NamingException
    {
        throw new LdapServiceUnavailableException( EXCEPTION_MSG, ResultCodeEnum.UNAVAILABLE );
    }


    public NamingEnumeration listBindings( Name name ) throws NamingException
    {
        throw new LdapServiceUnavailableException( EXCEPTION_MSG, ResultCodeEnum.UNAVAILABLE );
    }


    public Object addToEnvironment( String propName, Object propVal ) throws NamingException
    {
        throw new LdapServiceUnavailableException( EXCEPTION_MSG, ResultCodeEnum.UNAVAILABLE );
    }


    public String composeName( String name, String prefix ) throws NamingException
    {
        throw new LdapServiceUnavailableException( EXCEPTION_MSG, ResultCodeEnum.UNAVAILABLE );
    }


    public Name composeName( Name name, Name prefix ) throws NamingException
    {
        throw new LdapServiceUnavailableException( EXCEPTION_MSG, ResultCodeEnum.UNAVAILABLE );
    }


    public void addNamingListener( Name name, String s, SearchControls searchControls, NamingListener namingListener )
            throws NamingException
    {
        throw new LdapServiceUnavailableException( EXCEPTION_MSG, ResultCodeEnum.UNAVAILABLE );
    }


    public void addNamingListener( String s, String s1, SearchControls searchControls, NamingListener namingListener ) throws NamingException
    {
        throw new LdapServiceUnavailableException( EXCEPTION_MSG, ResultCodeEnum.UNAVAILABLE );
    }


    public void addNamingListener( Name name, String s, Object[] objects, SearchControls searchControls, NamingListener namingListener ) throws NamingException
    {
        throw new LdapServiceUnavailableException( EXCEPTION_MSG, ResultCodeEnum.UNAVAILABLE );
    }


    public void addNamingListener( String s, String s1, Object[] objects, SearchControls searchControls, NamingListener namingListener ) throws NamingException
    {
        throw new LdapServiceUnavailableException( EXCEPTION_MSG, ResultCodeEnum.UNAVAILABLE );
    }


    public void addNamingListener( Name name, int i, NamingListener namingListener ) throws NamingException
    {
        throw new LdapServiceUnavailableException( EXCEPTION_MSG, ResultCodeEnum.UNAVAILABLE );
    }


    public void addNamingListener( String s, int i, NamingListener namingListener ) throws NamingException
    {
        throw new LdapServiceUnavailableException( EXCEPTION_MSG, ResultCodeEnum.UNAVAILABLE );
    }


    public void removeNamingListener( NamingListener namingListener ) throws NamingException
    {
        throw new LdapServiceUnavailableException( EXCEPTION_MSG, ResultCodeEnum.UNAVAILABLE );
    }


    public boolean targetMustExist() throws NamingException
    {
        throw new LdapServiceUnavailableException( EXCEPTION_MSG, ResultCodeEnum.UNAVAILABLE );
    }
}
