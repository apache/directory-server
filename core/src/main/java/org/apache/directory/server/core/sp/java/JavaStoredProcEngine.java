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


package org.apache.directory.server.core.sp.java;


import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.ldap.LdapContext;

import org.apache.directory.server.core.sp.StoredProcEngine;
import org.apache.directory.server.core.sp.StoredProcUtils;
import org.apache.directory.shared.ldap.util.DirectoryClassUtils;


/**
 * A {@link StoredProcEngine} implementation specific to Java stored procedures.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$ $Date$
 */
public class JavaStoredProcEngine implements StoredProcEngine
{

    public static final String STORED_PROC_LANG_ID = "Java";

    private Attributes spUnit;


    /* (non-Javadoc)
     * @see org.apache.directory.server.core.sp.StoredProcEngine#invokeProcedure(javax.naming.ldap.LdapContext, java.lang.String, java.lang.Object[])
     */
    public Object invokeProcedure( LdapContext rootCtx, String fullSPName, Object[] spArgs ) throws NamingException
    {
        Attribute javaByteCode = spUnit.get( "javaByteCode" );
        String spName = StoredProcUtils.extractStoredProcName( fullSPName );
        String className = StoredProcUtils.extractStoredProcUnitName( fullSPName );

        ClassLoader loader = new LdapJavaStoredProcClassLoader( javaByteCode );
        Class clazz;
        try
        {
            clazz = loader.loadClass( className );
        }
        catch ( ClassNotFoundException e )
        {
            NamingException ne = new NamingException();
            ne.setRootCause( e );
            throw ne;
        }

        Class[] types = getTypesFromValues( spArgs );

        Method proc;
        try
        {
            proc = DirectoryClassUtils.getAssignmentCompatibleMethod( clazz, spName, types );
        }
        catch ( NoSuchMethodException e )
        {
            NamingException ne = new NamingException();
            ne.setRootCause( e );
            throw ne;
        }
        try
        {
            return proc.invoke( null, spArgs );
        }
        catch ( IllegalArgumentException e )
        {
            NamingException ne = new NamingException();
            ne.setRootCause( e );
            throw ne;
        }
        catch ( IllegalAccessException e )
        {
            NamingException ne = new NamingException();
            ne.setRootCause( e );
            throw ne;
        }
        catch ( InvocationTargetException e )
        {
            NamingException ne = new NamingException();
            ne.setRootCause( e );
            throw ne;
        }
    }


    /* (non-Javadoc)
     * @see org.apache.directory.server.core.sp.StoredProcEngine#getSPLangId()
     */
    public String getSPLangId()
    {
        return STORED_PROC_LANG_ID;
    }


    /* (non-Javadoc)
     * @see org.apache.directory.server.core.sp.StoredProcEngine#setSPUnitEntry(javax.naming.directory.Attributes)
     */
    public void setSPUnitEntry( Attributes spUnit )
    {
        this.spUnit = spUnit;
    }


    private Class[] getTypesFromValues( Object[] values )
    {
        List<Class> types = new ArrayList<Class>();

        for ( Object obj : values )
        {
            types.add( obj.getClass() );
        }

        return types.toArray( EMPTY_CLASS_ARRAY );
    }

    private static Class[] EMPTY_CLASS_ARRAY = new Class[ 0 ];

}
