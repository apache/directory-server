package org.apache.eve.jndi ;

import java.io.ByteArrayInputStream ;
import java.io.ByteArrayOutputStream ;
import java.io.IOException ;
import java.io.ObjectInputStream ;
import java.io.ObjectOutputStream ;

import javax.naming.NamingException ;
import javax.naming.directory.Attributes ;


/**
 * Contains constants and serialization methods used to implement functionality
 * associated with RFC 2713 which enables the storage and representation of Java
 * objects within an LDAP directory.
 *
 * @see <a href="http://www.faqs.org/rfcs/rfc2713.html">RFC 2713</a>
 */
public class JavaLdap
{
    // ------------------------------------------------------------------------
    // Attribute Id Constants Used By The Java LDAP SchemaGroup
    // ------------------------------------------------------------------------

    /** */
    public static final String TOP_ATTR = "top" ;
    /** */
    public static final String JOBJECT_ATTR = "javaObject" ;
    /** */
    public static final String OBJECTCLASS_ATTR = "objectClass" ;
    /** */
    public static final String JCONTAINER_ATTR = "javaContainer" ;
    /** */
    public static final String JSERIALIZEDOBJ_ATTR = "javaSerializedObject" ;

    /** */
    public static final String JCLASSNAME_ATTR = "javaClassName" ;
    /** */
    public static final String JCLASSNAMES_ATTR = "javaClassNames" ;
    /** */
    public static final String JSERIALDATA_ATTR = "javaSerializedData" ;


    // ------------------------------------------------------------------------
    // Package Friendly & Private Utility Methods 
    // ------------------------------------------------------------------------


    /**
     * Resusitates an object from a serialized attribute in an entry that 
     * conforms to the specifications for representing Java Objects in an LDAP 
     * Directory (RFC 2713).
     *
     * @param a_attributes the entry representing a serialized object
     * @return the deserialized object
     * @throws NamingException if the object cannot be serialized
     */
    static Object deserialize( Attributes a_attributes )
        throws NamingException
    {
        ObjectInputStream l_in = null ;
        String l_className = ( String )
            a_attributes.get( JCLASSNAME_ATTR ).get() ;

        try
        {
            byte [] l_data = ( byte [] )
                a_attributes.get( JSERIALDATA_ATTR ).get() ;
            l_in = new ObjectInputStream( new ByteArrayInputStream( l_data ) ) ;
            Object l_obj = l_in.readObject() ;
            return l_obj ;
        }
        catch ( Exception e )
        {
            NamingException l_ne = new NamingException( "De-serialization of '"
                + l_className + "' instance failed:\n" + e.getMessage() ) ;
            l_ne.setRootCause( e ) ;
            throw l_ne ;
        }
        finally
        {
            try
            {
                l_in.close() ;
            }
            catch ( IOException e )
            {
                throw new NamingException(
                    "object deserialization stream close() failure" ) ;
            }
        }
    }

    
    /**
     * Serializes an object into a byte array.
     *
     * @param a_obj the object to serialize 
     * @return the object's serialized byte array form
     * @throws NamingException of the object cannot be serialized
     */
    static byte [] serialize( Object a_obj )
        throws NamingException
    {
        ByteArrayOutputStream l_bytesOut = null ;
        ObjectOutputStream l_out = null ;

        try
        {
            l_bytesOut = new ByteArrayOutputStream() ;
            l_out = new ObjectOutputStream( l_bytesOut ) ;
            l_out.writeObject( a_obj ) ;
            return l_bytesOut.toByteArray() ;
        }
        catch ( Exception e )
        {
            NamingException l_ne = new NamingException( "Serialization of '"
                + a_obj + "' failed:\n" + e.getMessage() ) ;
            l_ne.setRootCause( e ) ;
            throw l_ne ;
        }
        finally
        {
            try
            {
                l_out.close() ;
            }
            catch ( IOException e )
            {
                throw new NamingException(
                    "object serialization stream close() failure" ) ;
            }
        }
    }


    /**
     * Serializes an object into an entry using the attributes specified in
     * RFC 2713 to represent the serialized object.
     *
     * @param a_entry the set of attributes representing entry
     * @param a_obj the object to serialize
     * @throws NamingException if the object cannot be serialized
     */
    static void serialize( Attributes a_entry, Object a_obj )
        throws NamingException
    {
        /* Let's add the object classes first:
         * objectClass: top
         * objectClass: javaObject
         * objectClass: javaContainer
         * objectClass: javaSerializedObject
         */
        a_entry.put( OBJECTCLASS_ATTR, TOP_ATTR ) ;
        a_entry.put( OBJECTCLASS_ATTR, JOBJECT_ATTR ) ;
        a_entry.put( OBJECTCLASS_ATTR, JCONTAINER_ATTR ) ;
        a_entry.put( OBJECTCLASS_ATTR, JSERIALIZEDOBJ_ATTR ) ;

        // Add the javaClassName and javaSerializedData attributes
        a_entry.put( JCLASSNAME_ATTR, a_obj.getClass().getName() ) ;
        a_entry.put( JSERIALDATA_ATTR, serialize( a_obj ) ) ;

        // Add all the class names this object can be cast to:
        Class [] l_classes = a_obj.getClass().getClasses() ;
        for ( int ii = 0; ii < l_classes.length; ii++ )
        {
            a_entry.put( JCLASSNAMES_ATTR, l_classes[ii].getName() ) ;
        }
    }
}
