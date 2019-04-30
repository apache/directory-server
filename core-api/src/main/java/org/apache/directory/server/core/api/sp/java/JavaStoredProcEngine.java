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

package org.apache.directory.server.core.api.sp.java;


import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import org.apache.directory.api.ldap.model.entry.Attribute;
import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.directory.api.util.MethodUtils;
import org.apache.directory.server.core.api.CoreSession;
import org.apache.directory.server.core.api.sp.StoredProcEngine;
import org.apache.directory.server.core.api.sp.StoredProcUtils;


/**
 * A {@link StoredProcEngine} implementation specific to Java stored procedures.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class JavaStoredProcEngine implements StoredProcEngine
{

    public static final String STORED_PROC_LANG_ID = "Java";

    private Entry spUnit;

    private static final Class<?>[] EMPTY_CLASS_ARRAY = new Class[0];

    /* (non-Javadoc)
     * @see org.apache.directory.server.core.sp.StoredProcEngine#invokeProcedure(OperationContext, String, Object[])
     */
    @Override
    public Object invokeProcedure( CoreSession session, String fullSPName, Object[] spArgs ) throws LdapException
    {
        Attribute javaByteCode = spUnit.get( "javaByteCode" );
        String spName = StoredProcUtils.extractStoredProcName( fullSPName );
        String className = StoredProcUtils.extractStoredProcUnitName( fullSPName );

        ClassLoader loader = new LdapJavaStoredProcClassLoader( javaByteCode );
        Class<?> clazz;

        try
        {
            clazz = loader.loadClass( className );
        }
        catch ( ClassNotFoundException e )
        {
            throw new LdapException( e );
        }

        Class<?>[] types = getTypesFromValues( spArgs );

        Method proc;
        try
        {
            proc = MethodUtils.getAssignmentCompatibleMethod( clazz, spName, types );
        }
        catch ( NoSuchMethodException e )
        {
            throw new LdapException( e );
        }
        try
        {
            return proc.invoke( null, spArgs );
        }
        catch ( IllegalArgumentException | IllegalAccessException | InvocationTargetException e )
        {
            throw new LdapException( e );
        }
    }


    /* (non-Javadoc)
     * @see org.apache.directory.server.core.sp.StoredProcEngine#getSPLangId()
     */
    @Override
    public String getSPLangId()
    {
        return STORED_PROC_LANG_ID;
    }


    /* (non-Javadoc)
     * @see org.apache.directory.server.core.sp.StoredProcEngine#setSPUnitEntry(javax.naming.directory.Attributes)
     */
    @Override
    public void setSPUnitEntry( Entry spUnit )
    {
        this.spUnit = spUnit;
    }


    private Class<?>[] getTypesFromValues( Object[] values )
    {
        List<Class<?>> types = new ArrayList<>();

        for ( Object obj : values )
        {
            types.add( obj.getClass() );
        }

        return types.toArray( EMPTY_CLASS_ARRAY );
    }
}
