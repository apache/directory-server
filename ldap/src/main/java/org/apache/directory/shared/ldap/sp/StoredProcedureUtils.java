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


package org.apache.directory.shared.ldap.sp;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.net.URL;

import javax.naming.NamingException;
import javax.naming.directory.Attributes;
import javax.naming.directory.BasicAttributes;
import javax.naming.ldap.LdapContext;

import org.apache.commons.lang.SerializationUtils;
import org.apache.directory.shared.ldap.message.extended.StoredProcedureRequest;
import org.apache.directory.shared.ldap.message.extended.StoredProcedureResponse;

/**
 * A utility class for working with Java classes as Stored Procedures.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev:$
 */
public class StoredProcedureUtils
{
    
    /**
     * Returns the stream data of a Java class file
     * whose fully qualified name is provided.
     * 
     * @param fullClassName
     *           Fully qualified name of the class
     *           with package name included and ".class" extension excluded.
     * @param resourceLoader
     *           The resource loader for the Stored Procedure Class. 
     * @return
     *           Stream data of the class file as a byte array.
     * @throws NamingException
     *           If an IO error occurs during reading the class file.
     */
    public static byte[] getClassFileAsStream( String fullClassName, Class resourceLoader ) throws NamingException
    {
        int lastDot = fullClassName.lastIndexOf( '.' );
        String classFileName = fullClassName.substring( lastDot + 1 ) + ".class";
        
        URL url = resourceLoader.getResource( classFileName );
        InputStream in = resourceLoader.getResourceAsStream( classFileName );
        File file = new File( url.getFile() );
        int size = ( int ) file.length();
        byte[] buf = new byte[size];
        
        try
        {
            in.read( buf );
            in.close();
        }
        catch ( IOException e )
        {
            NamingException ne = new NamingException();
            ne.setRootCause( e );
            throw ne;
        }
        
        return buf;
    }
    
    /**
     * Loads a Java class's stream data as a subcontext of an LdapContext given.
     * 
     * @param ctx
     *           The parent context of the Java class entry to be loaded.
     * @param fullClassName
     *           Fully qualified name of the class
     *           with package name included and ".class" extension excluded.
     * @param resourceLoader
     *           The resource loader for the Stored Procedure Class. 
     * @throws NamingException
     *           If an error occurs during creating the subcontext.
     */
    public static void loadStoredProcedureClass( LdapContext ctx, String fullClassName, Class resourceLoader ) throws NamingException
    {
        byte[] buf = getClassFileAsStream( fullClassName, resourceLoader );
        
        Attributes attributes = new BasicAttributes( "objectClass", "top", true );
        attributes.get( "objectClass" ).add( "javaClass" );
        attributes.put( "fullyQualifiedClassName", fullClassName );
        attributes.put( "byteCode", buf );
        
        ctx.createSubcontext( "fullyQualifiedClassName=" + fullClassName , attributes );
    }
    
    /**
     * Issues a Stored Procedure Call Extended Operation Request over an LdapContext.
     * 
     * @param ctx
     * @param procedureName
     * @param arguments
     * @return
     * @throws NamingException
     */
    public static Object callStoredProcedure( LdapContext ctx, String procedureName, Object[] arguments ) throws NamingException
    {
        String language = "Java";
        
        Object responseObject;
        try
        {
            StoredProcedureRequest req = new StoredProcedureRequest( 0, procedureName, language );
            for ( int i = 0; i < arguments.length; i++ )
            {
                byte[] type = arguments[i].getClass().getName().getBytes( "UTF-8" );
                
                byte[] value = SerializationUtils.serialize( ( Serializable ) arguments[i] );
                
                req.addParameter( type, value );
            }
            
            StoredProcedureResponse resp = ( StoredProcedureResponse ) ctx.extendedOperation( req );
            
            byte[] responseStream = resp.getEncodedValue();
            responseObject = SerializationUtils.deserialize( responseStream );
        }
        catch ( Exception e )
        {
            NamingException ne = new NamingException();
            ne.setRootCause( e );
            throw ne;
        }
        
        return responseObject;
    }
}
