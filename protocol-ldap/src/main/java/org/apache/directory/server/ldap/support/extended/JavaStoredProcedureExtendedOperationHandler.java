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
package org.apache.directory.server.ldap.support.extended;


import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.naming.NamingException;

import org.apache.commons.lang.SerializationUtils;
import org.apache.directory.server.core.jndi.ServerLdapContext;
import org.apache.directory.server.core.sp.LdapClassLoader;
import org.apache.directory.shared.ldap.codec.extended.operations.StoredProcedure;
import org.apache.directory.shared.ldap.codec.extended.operations.StoredProcedure.StoredProcedureParameter;
import org.apache.directory.shared.ldap.exception.LdapNamingException;
import org.apache.directory.shared.ldap.message.ResultCodeEnum;
import org.apache.directory.shared.ldap.util.DirectoryClassUtils;
import org.apache.directory.shared.ldap.util.SpringClassUtils;
import org.apache.directory.shared.ldap.util.StringTools;


/**
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$ $Date$
 */
public class JavaStoredProcedureExtendedOperationHandler implements LanguageSpecificStoredProceureExtendedOperationHandler
{
    private static final Class[] EMPTY_CLASS_ARRAY = new Class[0];
    
    public byte[] handleStoredProcedureExtendedOperation( ServerLdapContext ctx, StoredProcedure pojo ) 
        throws NamingException
    {
        List types = new ArrayList( pojo.getParameters().size() );
        List values = new ArrayList( pojo.getParameters().size() );
        
        Iterator it = pojo.getParameters().iterator();
        while ( it.hasNext() )
        {
            StoredProcedureParameter pPojo = ( StoredProcedureParameter ) it.next();
            
            Class type;
            try
            {
                type = SpringClassUtils.forName( StringTools.utf8ToString( pPojo.getType() ) );
            }
            catch ( ClassNotFoundException e )
            {
                NamingException ne = new NamingException();
                ne.setRootCause( e );
                throw ne;
            } 
            
            types.add( type );
            
            byte[] serializedValue = pPojo.getValue();
            
            try
            {
                values.add( SerializationUtils.deserialize( serializedValue ) );
            }
            catch ( Exception e )
            {
                NamingException ne = new NamingException();
                ne.setRootCause( e );
                throw ne;
            }
            
        }
        
        // TODO determine what to do with the exception
        Object response = executeProcedure( ctx, StringTools.utf8ToString( pojo.getProcedure() ), 
                ( Class[] ) types.toArray( EMPTY_CLASS_ARRAY ), values.toArray() );
        
        byte[] serializedResponse = null;
        
        try
        {
            serializedResponse = SerializationUtils.serialize( ( Serializable ) response );
        }
        catch ( Exception e )
        {
            NamingException ne = new NamingException();
            ne.setRootCause( e );
            throw ne;
        }
        
        return serializedResponse;
        
    }
    
    public Object executeProcedure( ServerLdapContext ctx, String procedure, Class[] types, Object[] values ) 
        throws NamingException
    {
        int lastDot = procedure.lastIndexOf( '.' );
        String className = procedure.substring( 0, lastDot );
        String methodName = procedure.substring( lastDot + 1 );
        LdapClassLoader loader = new LdapClassLoader( ctx );
        
        try
        {
            Class clazz = loader.loadClass( className );
            Method proc = DirectoryClassUtils.getAssignmentCompatibleMethod( clazz, methodName, types );
            return proc.invoke( null, values );
        }
        catch ( Exception e )
        {
            LdapNamingException lne = new LdapNamingException( ResultCodeEnum.OTHER );
            lne.setRootCause( e );
            throw lne;
        }
    }
    
}
