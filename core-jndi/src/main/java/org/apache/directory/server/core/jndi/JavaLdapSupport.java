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


import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import javax.naming.NamingException;

import org.apache.directory.api.ldap.model.constants.SchemaConstants;
import org.apache.directory.api.ldap.model.entry.Attribute;
import org.apache.directory.api.ldap.model.entry.DefaultAttribute;
import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.directory.api.ldap.model.exception.LdapInvalidAttributeValueException;
import org.apache.directory.api.ldap.model.schema.AttributeType;
import org.apache.directory.api.ldap.model.schema.SchemaManager;
import org.apache.directory.server.i18n.I18n;


/**
 * Contains constants and serialization methods used to implement functionality
 * associated with RFC 2713 which enables the storage and representation of Java
 * objects within an LDAP directory.
 *
 * @see <a href="http://www.faqs.org/rfcs/rfc2713.html">RFC 2713</a>
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
final class JavaLdapSupport
{
    private JavaLdapSupport()
    {
    }

    // ------------------------------------------------------------------------
    // Attribute Id Constants Used By The Java LDAP BootstrapSchema
    // ------------------------------------------------------------------------

    /** the javaObject attribute */
    public static final String JOBJECT_ATTR = "javaObject";
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
    static Object deserialize( Entry serverEntry ) throws NamingException
    {
        String className = null;

        try
        {
            className = serverEntry.get( JCLASSNAME_ATTR ).getString();
        }
        catch ( LdapInvalidAttributeValueException liave )
        {
            NamingException ne = new NamingException( I18n.err( I18n.ERR_06017_INSTANCE_DESERIALIZATION_FAILED, className, liave.getLocalizedMessage() ) );
            ne.setRootCause( liave );
            throw ne;
        }

        try ( ObjectInputStream in = new ObjectInputStream( 
                new ByteArrayInputStream( ( byte[] ) serverEntry.get( JSERIALDATA_ATTR ).getBytes() ) ) )
        {
            return in.readObject();
        }
        catch ( Exception e )
        {
            NamingException ne = new NamingException( I18n.err( I18n.ERR_06017_INSTANCE_DESERIALIZATION_FAILED, className, e.getLocalizedMessage() ) );
            ne.setRootCause( e );
            throw ne;
        }
    }


    /**
     * Serializes an object into a byte array.
     *
     * @param obj the object to serialize
     * @return the object's serialized byte array form
     * @throws NamingException of the object cannot be serialized
     */
    static byte[] serialize( Object obj ) throws LdapException
    {
        ByteArrayOutputStream bytesOut = new ByteArrayOutputStream();

        try ( ObjectOutputStream out = new ObjectOutputStream( bytesOut ) )
        {
            out.writeObject( obj );
            
            return bytesOut.toByteArray();
        }
        catch ( Exception e )
        {
            throw new LdapException( I18n.err( I18n.ERR_06018_SERIALIZATION_FAILED, obj, e.getLocalizedMessage() ) );
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
    static void serialize( Entry entry, Object obj, SchemaManager schemaManager ) throws LdapException
    {
        /* Let's add the object classes first:
         * objectClass: top
         * objectClass: javaObject
         * objectClass: javaContainer
         * objectClass: javaSerializedObject
         */
        entry.put( SchemaConstants.OBJECT_CLASS_AT,
            SchemaConstants.TOP_OC,
            JOBJECT_ATTR,
            JCONTAINER_ATTR,
            JSERIALIZEDOBJ_ATTR );

        // Add the javaClassName and javaSerializedData attributes
        entry.put( JCLASSNAME_ATTR, obj.getClass().getName() );
        entry.put( JSERIALDATA_ATTR, serialize( obj ) );

        // Add all the class names this object can be cast to:
        Class<?>[] classes = obj.getClass().getClasses();
        AttributeType attributeType = schemaManager.lookupAttributeTypeRegistry( JCLASSNAMES_ATTR );
        Attribute javaClassNames = new DefaultAttribute( attributeType, JCLASSNAMES_ATTR );

        for ( int ii = 0; ii < classes.length; ii++ )
        {
            javaClassNames.add( classes[ii].getName() );
        }

        entry.put( javaClassNames );
    }
}
