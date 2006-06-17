/*
 *   Copyright 2006 The Apache Software Foundation
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
package org.apache.directory.server.ldap.support.extended;


import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.naming.NamingException;

import org.apache.directory.server.core.jndi.ServerLdapContext;
import org.apache.directory.server.core.sp.LdapClassLoader;
import org.apache.directory.shared.ldap.codec.extended.operations.StoredProcedure;
import org.apache.directory.shared.ldap.codec.extended.operations.StoredProcedure.StoredProcedureParameter;
import org.apache.directory.shared.ldap.exception.LdapNamingException;
import org.apache.directory.shared.ldap.message.ResultCodeEnum;
import org.apache.directory.shared.ldap.util.ClassUtils;
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
    
    public void handleStoredProcedureExtendedOperation( ServerLdapContext ctx, StoredProcedure pojo ) 
        throws ClassNotFoundException, NamingException
    {
        List types = new ArrayList( pojo.getParameters().size() );
        List values = new ArrayList( pojo.getParameters().size() );
        
        Iterator it = pojo.getParameters().iterator();
        while ( it.hasNext() )
        {
            StoredProcedureParameter pPojo = ( StoredProcedureParameter ) it.next();
            
            // Get type from String even if it holds a primitive type name
            Class type = SpringClassUtils.forName( StringTools.utf8ToString( pPojo.getType() ) ); 
            
            types.add( type );
            
            byte[] value = pPojo.getValue();
            
            /**
             * If the type name refers to a Java primitive then
             * we know that the value is encoded as its String representation and
             * we retrieve the String and initialize a wrapper of the primitive.
             * 
             * Note that this is just how we prefer Java Specific Stored Procedures
             * to handle parameters. Of course we do not have to it this way.
             */
            if ( type.isPrimitive() )
            {
                values.add( getInitializedPrimitiveWrapperInstance( type, value ) );
            }
            /**
             * If the type is a complex Java type then
             * just deserialize the object.
             */
            else
            {
                try
                {
                    // TODO Irritating syntax! Just wanted to see how it looks like..
                    values.add
                    ( 
                        (
                            new ObjectInputStream
                            ( 
                                new ByteArrayInputStream( value )
                            ) 
                        ).readObject()
                    );
                }
                catch (IOException e)
                {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
                catch (ClassNotFoundException e)
                {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
            
        }
        
        // TODO determine what to do with the exception
        executeProcedure( ctx, StringTools.utf8ToString( pojo.getProcedure() ), 
                ( Class[] ) types.toArray( EMPTY_CLASS_ARRAY ), values.toArray() );
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
    
    
    private Object getInitializedPrimitiveWrapperInstance( Class type, byte[] value )
    {
        Object instance = null;
        try
        {
            instance = ClassUtils
                    .primitiveToWrapper( type )
                    .getConstructor( new Class[] {String.class} )
                    .newInstance( new Object[] { StringTools.utf8ToString( value ) } );
        }
        catch ( Exception e )
        {
            e.printStackTrace();
        }
        return instance;
    }
}
