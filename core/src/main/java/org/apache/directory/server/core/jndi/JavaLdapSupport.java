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


import org.apache.directory.shared.ldap.message.LockableAttributeImpl;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import javax.naming.NamingException;
import javax.naming.directory.Attributes;
import javax.naming.directory.Attribute;


/**
 * Contains constants and serialization methods used to implement functionality
 * associated with RFC 2713 which enables the storage and representation of Java
 * objects within an LDAP directory.
 *
 * @see <a href="http://www.faqs.org/rfcs/rfc2713.html">RFC 2713</a>
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
class JavaLdapSupport
{
    // ------------------------------------------------------------------------
    // Attribute Id Constants Used By The Java LDAP BootstrapSchema
    // ------------------------------------------------------------------------

    /** objectClass attribute for top */
    public static final String TOP_ATTR = "top";
    /** the javaObject attribute */
    public static final String JOBJECT_ATTR = "javaObject";
    /** the objectClass attribute */
    public static final String OBJECTCLASS_ATTR = "objectClass";
    /** the javaContainer attribute */
    public static final String JCONTAINER_ATTR = "javaContainer";
    /** the javaSerializedObject attribute */
    public static final String JSERIALIZEDOBJ_ATTR = "javaSerializedObject";

    /** the javaClassName attribute */
    public static final String JCLASSNAME_ATTR = "javaClassName";
    /** the javaClassNames attribute */
    public static final String JCLASSNAMES_ATTR = "javaClassNames";
    /** the javaSerializedData attribute */
    public static final String JSERIALDATA_ATTR = "javaSerializedData";


    // ------------------------------------------------------------------------
    // Package Friendly & Private Utility Methods 
    // ------------------------------------------------------------------------

    /**
     * Resusitates an object from a serialized attribute in an entry that 
     * conforms to the specifications for representing Java Objects in an LDAP 
     * Directory (RFC 2713).
     *
     * @param attributes the entry representing a serialized object
     * @return the deserialized object
     * @throws NamingException if the object cannot be serialized
     */
    static Object deserialize( Attributes attributes ) throws NamingException
    {
        ObjectInputStream in = null;
        String className = ( String ) attributes.get( JCLASSNAME_ATTR ).get();

        try
        {
            byte[] data = ( byte[] ) attributes.get( JSERIALDATA_ATTR ).get();
            in = new ObjectInputStream( new ByteArrayInputStream( data ) );
            return in.readObject();
        }
        catch ( Exception e )
        {
            NamingException ne = new NamingException( "De-serialization of '" + className + "' instance failed:\n"
                + e.getMessage() );
            ne.setRootCause( e );
            throw ne;
        }
        finally
        {
            try
            {
                if ( in != null )
                {
                    in.close();
                }
            }
            catch ( IOException e )
            {
                throw new NamingException( "object deserialization stream close() failure" );
            }
        }
    }


    /**
     * Serializes an object into a byte array.
     *
     * @param obj the object to serialize
     * @return the object's serialized byte array form
     * @throws NamingException of the object cannot be serialized
     */
    static byte[] serialize( Object obj ) throws NamingException
    {
        ByteArrayOutputStream bytesOut = null;
        ObjectOutputStream out = null;

        try
        {
            bytesOut = new ByteArrayOutputStream();
            out = new ObjectOutputStream( bytesOut );
            out.writeObject( obj );
            return bytesOut.toByteArray();
        }
        catch ( Exception e )
        {
            NamingException ne = new NamingException( "Serialization of '" + obj + "' failed:\n" + e.getMessage() );
            ne.setRootCause( e );
            throw ne;
        }
        finally
        {
            try
            {
                if ( out != null )
                {
                    out.close();
                }
            }
            catch ( IOException e )
            {
                throw new NamingException( "object serialization stream close() failure" );
            }
        }
    }


    /**
     * Serializes an object into an entry using the attributes specified in
     * RFC 2713 to represent the serialized object.
     *
     * @param entry the set of attributes representing entry
     * @param obj the object to serialize
     * @throws NamingException if the object cannot be serialized
     */
    static void serialize( Attributes entry, Object obj ) throws NamingException
    {
        /* Let's add the object classes first:
         * objectClass: top
         * objectClass: javaObject
         * objectClass: javaContainer
         * objectClass: javaSerializedObject
         */
        Attribute objectClass = new LockableAttributeImpl( "objectClass" );
        objectClass.add( TOP_ATTR );
        objectClass.add( JOBJECT_ATTR );
        objectClass.add( JCONTAINER_ATTR );
        objectClass.add( JSERIALIZEDOBJ_ATTR );
        entry.put( objectClass );

        // Add the javaClassName and javaSerializedData attributes
        entry.put( JCLASSNAME_ATTR, obj.getClass().getName() );
        entry.put( JSERIALDATA_ATTR, serialize( obj ) );

        // Add all the class names this object can be cast to:
        Class[] classes = obj.getClass().getClasses();
        Attribute javaClassNames = new LockableAttributeImpl( JCLASSNAMES_ATTR );

        for ( int ii = 0; ii < classes.length; ii++ )
        {
            javaClassNames.add( classes[ii].getName() );
        }

        entry.put( javaClassNames );
    }
}
