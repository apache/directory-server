/*
 * $Id: ContextHelper.java,v 1.7 2003/08/22 21:15:55 akarasulu Exp $
 *
 * -- (c) LDAPd Group                                                    --
 * -- Please refer to the LICENSE.txt file in the root directory of      --
 * -- any LDAPd project for copyright and distribution information.      --
 *
 */

package org.apache.eve.jndi ;


import org.apache.ldap.common.name.LdapName ;
import org.apache.eve.backend.LdapEntry ;
import org.apache.eve.backend.UnifiedBackend ;
import org.apache.eve.backend.BackendException ;

import javax.naming.Name ;
import javax.naming.NamingException ;
import javax.naming.NameNotFoundException ;

import java.io.IOException ;
import java.io.ObjectInputStream ;
import java.io.ObjectOutputStream ;
import java.io.ByteArrayInputStream ;
import java.io.ByteArrayOutputStream ;
import javax.naming.Context;
import org.apache.ldap.common.util.NamespaceTools;
import javax.naming.NamingEnumeration;
import org.apache.eve.backend.Cursor;
import javax.naming.OperationNotSupportedException;


/**
 * Contains implementation content for the UnifiedContext due to its massive
 * size.
 * 
 * @author <a href="mailto:aok123@bellsouth.net">Alex Karasulu</a>
 * @author $Author: akarasulu $
 * @version $Revision: 1.7 $
 */
public class ContextHelper
{
    public static final String TOP_ATTR = "top" ;
    public static final String JOBJECT_ATTR = "javaObject" ;
    public static final String OBJECTCLASS_ATTR = "objectClass" ;
    public static final String JCONTAINER_ATTR = "javaContainer" ;
    public static final String JSERIALIZEDOBJ_ATTR = "javaSerializedObject" ;

    public static final String JCLASSNAME_ATTR = "javaClassName" ;
    public static final String JCLASSNAMES_ATTR = "javaClassNames" ;
    public static final String JSERIALDATA_ATTR = "javaSerializedData" ;

    private final UnifiedContext m_ctx ;



	/**
     * Creates a context helper for a UnifiedContext.
     */
    ContextHelper( UnifiedContext a_ctx )
    {
        m_ctx = a_ctx ;
    }


    public Name composeName( Name a_rdn, Name a_prefix )
        throws NamingException
    {
        LdapEntry l_entry = m_ctx.getEntry() ;

        // Example: This context is ou=people and say name is the relative
        // name of uid=jwalker and the prefix is dc=domain.  Then we must
        // compose the name relative to prefix which would be:
        // uid=jwalker,ou=people,dc=domain.

        // To do so we apply the following general algorithm.  Find this
        // context's name relative to the prefix argument: let's call this
        // the prefix relative name or pfn.  Construct the Name of the name
        // argument and add to this the pfn as the suffix via a Name.addAll().

        // Preliminary Step I: Return the context relative name arg if prefix
        // is null or is an empty name.
        if( a_prefix == null || a_prefix.size() == 0 )
        {
            return a_rdn ;
        }

        // Should we check to see if name arg context exists? Or do we presume
        // that it does and handle failures on context lookups?

        // Step I: Find the prn
        // Grab last component of the prefix.  Find its position in the l_fqn.
        // Take the prefix of l_fqn upto and including this last component.

        String l_lastComp = a_prefix.get( a_prefix.size() - 1 ) ;
        Name l_fqn = l_entry.getNormalizedDN() ;
        Name l_prn = null ;
        for( int ii = 0 ; ii < l_fqn.size(); ii++ )
        {
            if( l_fqn.get( ii ).equals( l_lastComp ) )
            {
                l_prn = l_fqn.getPrefix( ii + 1 ) ;
                break ;
            }
        }

        // Step II: Throw exception if l_prn not found which indicates that the
        // prefix is not an ancestor of this context.
        if( null == l_prn )
        {
            throw new NamingException( "The prefix '" + a_prefix+ "' is not an"
                + " ancestor of this context '" + l_fqn + "'" ) ;
        }

        // Step III: Compose the name
        Name l_retval = new LdapName() ;
        l_retval.addAll( a_rdn ) ;
        l_retval.addAll( l_prn ) ;
        return l_retval ;
    }


    public String composeName( String a_rdn, String a_prefix )
	    throws NamingException
    {
        if( a_prefix == null || a_prefix.trim().equals( "" ) )
        {
            return a_rdn ;
        }

        UnifiedBackend l_nexus = JndiProviderModule.getInstance().getNexus() ;
        return composeName( l_nexus.getNormalizedName( a_rdn ),
            l_nexus.getNormalizedName( a_prefix ) ).toString() ;
    }


    public Object lookup( Name a_rdn ) throws NamingException
    {
        if( a_rdn.size() == 0 )
        {
            return m_ctx.clone() ;
        }

        Name l_dn = ( Name ) a_rdn.clone() ;
        LdapEntry l_entry = m_ctx.getEntry() ;
		UnifiedBackend l_nexus = JndiProviderModule.getInstance().getNexus() ;
        l_dn.addAll( l_entry.getNormalizedDN() ) ;

        try
        {
            if( l_nexus.hasEntry( l_dn ) )
            {
                LdapEntry l_target = l_nexus.read( l_dn ) ;

                if( l_target.hasAttribute( JCLASSNAME_ATTR ) )
                {
                    return deserialize( l_target ) ;
                }

                return new UnifiedLdapContext(
                    m_ctx.getEnvironment(), l_target ) ;
            }
        }
        catch( BackendException e )
        {
            NamingException l_ne = new NamingException( "Nexus read('" + l_dn
                + "') failure:\n" + e.getMessage() ) ;
            l_ne.setRootCause( e ) ;
        }

        // JNDI NameNotFoundExceptions corresponds to LDAPv3 NOSUCHOBJECT result
        // code with an enumeration value of [32]
        throw new NameNotFoundException(
            "[32] Fully qualified name '" + l_dn
            + "' for name relative to this context of '" + a_rdn
            + "' was not found" ) ;
    }


    public Object lookup( String a_rdn ) throws NamingException
    {
        if( null == a_rdn || a_rdn.trim().equals( "" ) )
        {
            return m_ctx.clone() ;
        }

        UnifiedBackend l_nexus = JndiProviderModule.getInstance().getNexus() ;
        return lookup( l_nexus.getNormalizedName( a_rdn ) ) ;
    }


    public void bind( Name a_rdn, Object an_obj ) throws NamingException
    {
        if( an_obj instanceof UnifiedLdapContext )
        {
            throw new IllegalArgumentException(
                "Cannot bind a directory context object!" ) ;
        }

        Name l_dn = ( Name ) a_rdn.clone() ;
        LdapEntry l_entry = m_ctx.getEntry() ;
        LdapEntry l_newEntry = null ;
        UnifiedBackend l_nexus = JndiProviderModule.getInstance().getNexus() ;
        l_dn.addAll( l_entry.getNormalizedDN() ) ;

        try
        {
            l_newEntry = l_nexus.newEntry( l_dn.toString() ) ;
        }
        catch( BackendException e )
        {
            NamingException l_ne = new NamingException( "Failed to get handle "
                + "on an entry implementation for name '" + a_rdn+ "' due to "
                + "nexus exception: " ) ;
            l_ne.setRootCause( e ) ;
            throw l_ne ;
        }

        populate( l_newEntry, a_rdn, an_obj ) ;

        try
        {
            l_nexus.create( l_newEntry ) ;
        }
        catch( BackendException e )
        {
            NamingException l_ne = new NamingException("Failed create entry "
                + "for name '" + a_rdn + "' due to "
                + "nexus exception.  Entry contains:\n" + l_newEntry ) ;
            l_ne.setRootCause( e ) ;
            throw l_ne ;
        }
    }


    public void bind( String a_rdn, Object an_obj) throws NamingException
    {
        UnifiedBackend l_nexus = JndiProviderModule.getInstance().getNexus() ;
        bind( l_nexus.getNormalizedName( a_rdn ), an_obj ) ;
    }


    public void rebind( Name a_rdn, Object an_obj ) throws NamingException
    {
        Name l_dn = new LdapName() ;
        LdapEntry l_entry = m_ctx.getEntry() ;
        UnifiedBackend l_nexus = JndiProviderModule.getInstance().getNexus() ;
        l_dn.addAll( l_entry.getNormalizedDN() ) ;
        l_dn.addAll( a_rdn ) ;

        try
        {
            if( l_nexus.hasEntry( l_dn ) )
            {
                unbind( a_rdn ) ;
            }

            bind( a_rdn, an_obj ) ;
        }
        catch( BackendException e )
        {
            NamingException l_ne = new NamingException( "Failed entry lookup "
                + "for name '" + l_dn + "' due to nexus exception.") ;
            l_ne.setRootCause(e) ;
            throw l_ne ;
        }
    }


    public void rebind( String a_rdn, Object an_obj ) throws NamingException
    {
        UnifiedBackend l_nexus = JndiProviderModule.getInstance().getNexus() ;
        rebind( l_nexus.getNormalizedName( a_rdn ), an_obj ) ;
    }


    public void unbind( Name a_rdn ) throws NamingException
    {
        LdapEntry l_entry = m_ctx.getEntry() ;
        UnifiedBackend l_nexus = JndiProviderModule.getInstance().getNexus() ;
        Name l_dn = new LdapName() ;
        l_dn.addAll( l_entry.getNormalizedDN() ) ;
        l_dn.addAll( a_rdn ) ;

        try
        {
            if( l_nexus.hasEntry( l_dn ) )
            {
                l_nexus.delete( l_entry ) ;
            }
        }
        catch( BackendException e )
        {
            NamingException l_ne = new NamingException( "Failed entry deletion "
                + "for name '" + l_dn + "' due to nexus exception.") ;
            l_ne.setRootCause( e ) ;
            throw l_ne ;
        }
    }


    public void unbind( String a_rdn ) throws NamingException
    {
        UnifiedBackend l_nexus = JndiProviderModule.getInstance().getNexus() ;
        unbind( l_nexus.getNormalizedName( a_rdn ) ) ;
    }


    public void rename( Name a_oldRdn, Name a_newRdn ) throws NamingException
    {
        UnifiedBackend l_nexus = JndiProviderModule.getInstance().getNexus() ;
        LdapEntry l_entry = m_ctx.getEntry() ;
        LdapEntry l_oldEntry = null ;
        Name l_oldDn = new LdapName() ;
        Name l_newDn = new LdapName() ;
        l_oldDn.addAll( l_entry.getNormalizedDN() ) ;
        l_oldDn.addAll( a_oldRdn ) ;
        l_newDn.addAll( l_entry.getNormalizedDN() ) ;
        l_newDn.addAll( a_newRdn ) ;

        try
        {
            l_oldEntry = l_nexus.read( l_oldDn ) ;
        }
        catch( BackendException e )
        {
            NamingException l_ne = new NamingException( "Failed entry rename "
                + "on old name '" + l_oldDn + "' to new name of '"
                + l_newDn + "' due to a nexus read failure." ) ;
            l_ne.setRootCause( e ) ;
            throw l_ne ;
        }

        // If both DNs are same size and their suffixes equal one another
        // then this defaults to a simple modifyRdn operation.
        if( l_oldDn.size() == l_newDn.size() )
        {
            try
            {
                l_nexus.modifyRdn( l_oldEntry, a_newRdn, true ) ;
            }
            catch( BackendException e )
            {
                NamingException l_ne = new NamingException( "Failed on nexus "
                    + "modifyRdn on old name '" + l_oldDn + "' to new name '"
                    + l_newDn + "' due to a backend failure." ) ;
                l_ne.setRootCause( e ) ;
                throw l_ne ;
            }
        }
        // Here we are moving the entry down into a deeper level and need to
        // determine if we are to change the rdn or not.  If the first
        // componenet of the oldName equals the first component of the newName
        // then this is a simple move without an Rdn change.
        else if( a_oldRdn.get( 0 ).equals( a_newRdn.get( 0 ) ) )
        {
            Name l_parentDn = l_newDn.getSuffix( 1 ) ;
            LdapEntry l_parent = null ;

            try
            {
                l_parent = l_nexus.read( l_parentDn ) ;
            }
            catch( BackendException e )
            {
                NamingException l_ne = new NamingException( "Failed rename "
                    + "on old name '" + l_oldDn + "' to new name of '"
                    + l_newDn + "' due to a nexus parent read error for a "
                    + "parent with name '" + l_parentDn + "'." ) ;
                l_ne.setRootCause(e) ;
                throw l_ne ;
            }

            try
            {
                l_nexus.move( l_parent, l_oldEntry ) ;
            }
            catch( BackendException e )
            {
                NamingException l_ne = new NamingException( "Failed on nexus "
                    + "move(" + l_parentDn + ", " + l_oldDn + ") due to a "
                    + "backend failure." ) ;
                l_ne.setRootCause( e ) ;
                throw l_ne ;
            }

        // Here we are doing a move and a Rdn change at the same time.
        }
        else
        {
            Name l_parentDn = l_newDn.getSuffix( 1 ) ;
            LdapEntry l_parent = null ;

            try
            {
                l_parent = l_nexus.read( l_parentDn ) ;
            }
            catch( BackendException e )
            {
                NamingException l_ne = new NamingException( "Failed rename "
                    + "on old name '" + l_oldDn + "' to new name of '"
                    + l_newDn + "' due to a nexus parent read error for a "
                    + "parent with name '" + l_parentDn + "'." ) ;
                l_ne.setRootCause( e ) ;
                throw l_ne ;
            }

            Name l_rdn = a_newRdn.getPrefix( 1 ) ;
            try
            {
                l_nexus.move( l_parent, l_oldEntry, l_rdn, true ) ;
            }
            catch( BackendException e )
            {
                NamingException l_ne = new NamingException( "Failed on nexus "
                    + "move(" + l_parentDn + ", " + l_oldDn + ", " + l_rdn
                    + ", true) " + "due to a backend failure." ) ;
                l_ne.setRootCause( e ) ;
                throw l_ne ;
            }
        }
    }


    public void rename( String a_oldRdn, String a_newRdn )
        throws NamingException
    {
        UnifiedBackend l_nexus = JndiProviderModule.getInstance().getNexus() ;
        rename( l_nexus.getNormalizedName( a_oldRdn ),
            l_nexus.getNormalizedName( a_newRdn ) ) ;
    }


    /**
     * Destroys subcontexts by first building the distinguished name of the
     * entry to destroy using the supplied relative distinguished name and
     * the name of this node.
     *
     * @param a_rdn relative distinguished name describing a subordinate context
     * to this one.
     */
    public void destroySubcontext( Name a_rdn )
        throws NamingException
    {
        UnifiedBackend l_nexus = JndiProviderModule.getInstance().getNexus() ;
		LdapEntry l_entry = m_ctx.getEntry() ;
        LdapEntry l_oldEntry = null ;
        Name l_dn = new LdapName() ;
        l_dn.addAll( l_entry.getNormalizedDN() ) ;
        l_dn.addAll( a_rdn ) ;

        try
        {
            l_oldEntry = l_nexus.read( l_dn ) ;

            // The exception here is equivalent to an
            if( l_nexus.isSuffix( l_oldEntry ) )
            {
				StringBuffer l_buf = new StringBuffer() ;
				l_buf.append( "[53] Entry with Dn '" ) ;
				l_buf.append( l_dn.toString() ) ;
				l_buf.append( "' is a suffix.  Will not allow the deletion" ) ;
				l_buf.append( " of a suffix entry!  To remove suffix" ) ;
				l_buf.append( " detach the backend from the server by" ) ;
				l_buf.append( " removing the backend from the server's" ) ;
				l_buf.append( " config.xml file." ) ;
				String l_msg = l_buf.toString() ;
 	   			throw new OperationNotSupportedException( l_msg ) ;
            }
        }
        catch( BackendException e )
        {
            NamingException l_ne = new NamingException( "Failed on nexus "
                + "read(" + l_dn + ") due to a backend failure." ) ;
            l_ne.setRootCause( e ) ;
            throw l_ne ;
        }

        try
        {
            l_nexus.delete( l_oldEntry ) ;
        }
        catch( BackendException e )
        {
            NamingException l_ne = new NamingException( "Failed on nexus "
                + "delete(" + l_dn + ") due to a backend failure." ) ;
            l_ne.setRootCause( e ) ;
            throw l_ne ;
        }
    }


    public void destroySubcontext( String a_rdn ) throws NamingException
    {
        UnifiedBackend l_nexus = JndiProviderModule.getInstance().getNexus() ;
        destroySubcontext( l_nexus.getNormalizedName( a_rdn ) ) ;
    }


    public Context createSubcontext( Name a_rdn )
        throws NamingException
    {
        UnifiedBackend l_nexus = JndiProviderModule.getInstance().getNexus() ;
        LdapEntry l_entry = m_ctx.getEntry() ;
        LdapEntry l_oldEntry = null ;

        // The argument name is really the name relative to this context so we
        // need to build out the fully qualified name a.k.a. distinguished name
        Name l_dn = new LdapName() ;
        l_dn.addAll( l_entry.getUnNormalizedDN() ) ;
        l_dn.addAll( a_rdn ) ;

        try
        {
            l_oldEntry = l_nexus.newEntry( l_dn.toString() ) ;
        }
        catch( BackendException be )
        {
            NamingException l_ne = new NamingException( "Failed on nexus "
                + "newEntry(" + l_dn + ") due to a backend failure." ) ;
            l_ne.setRootCause( be ) ;
            throw l_ne ;
        }

        String l_rdn = a_rdn.get( 0 ) ;
        String l_rdnAttribute = NamespaceTools.getRdnAttribute( l_rdn ) ;
        String l_rdnValue = NamespaceTools.getRdnValue( l_rdn ) ;
        l_oldEntry.addValue( l_rdnAttribute, l_rdnValue ) ;
        l_oldEntry.addValue( OBJECTCLASS_ATTR, JCONTAINER_ATTR ) ;
        l_oldEntry.addValue( OBJECTCLASS_ATTR, TOP_ATTR ) ;

        try
        {
            l_nexus.create( l_oldEntry ) ;
        }
        catch( BackendException be )
        {
            NamingException l_ne = new NamingException( "Failed on nexus "
                + "create(" + l_dn + ") due to a backend failure on entry:\n"
                + l_oldEntry ) ;
            l_ne.setRootCause( be ) ;
            throw l_ne ;
        }

        return new UnifiedLdapContext( m_ctx.getEnvironment(),
            l_oldEntry ) ;
    }


    public Context createSubcontext( String a_rdn )
        throws NamingException
    {
        UnifiedBackend l_nexus = JndiProviderModule.getInstance().getNexus() ;
        return createSubcontext( l_nexus.getName( a_rdn ) ) ;
    }


    public NamingEnumeration list( Name a_rdn ) throws NamingException
    {
        UnifiedBackend l_nexus = JndiProviderModule.getInstance().getNexus() ;
        LdapEntry l_entry = m_ctx.getEntry() ;
        Cursor l_children = null ;
        Name l_dn = new LdapName() ;
        l_dn.addAll( l_entry.getNormalizedDN() ) ;
        l_dn.addAll( a_rdn ) ;

        try
        {
            l_children = l_nexus.listChildren( l_dn ) ;
        }
        catch( BackendException e )
        {
            NamingException l_ne = new NamingException( "Failed on nexus "
                + "listChildren(" + l_dn + ") due to a backend failure." ) ;
            l_ne.setRootCause( e ) ;
            throw l_ne ;
        }

        return new NameClassPairEnumeration( l_nexus, l_children ) ;
    }


    public NamingEnumeration list(String a_rdn ) throws NamingException
    {
        UnifiedBackend l_nexus = JndiProviderModule.getInstance().getNexus() ;
        return list( l_nexus.getNormalizedName( a_rdn ) ) ;
    }


    public NamingEnumeration listBindings( Name a_rdn ) throws NamingException
    {
        UnifiedBackend l_nexus = JndiProviderModule.getInstance().getNexus() ;
        LdapEntry l_entry = m_ctx.getEntry() ;
        Cursor l_children = null ;
        Name l_dn = new LdapName() ;
        l_dn.addAll( l_entry.getNormalizedDN() ) ;
        l_dn.addAll( a_rdn ) ;

        try
        {
            l_children = l_nexus.listChildren( l_dn ) ;
        }
        catch( BackendException e )
        {
            NamingException l_ne = new NamingException( "Failed on nexus "
                + "listChildren("+l_dn+") due to a backend failure." ) ;
            l_ne.setRootCause( e ) ;
            throw l_ne ;
        }

        return new BindingEnumeration( m_ctx.getEnvironment(), l_children ) ;
    }


    public NamingEnumeration listBindings( String a_rdn ) throws NamingException
    {
        UnifiedBackend l_nexus = JndiProviderModule.getInstance().getNexus() ;
        return listBindings( l_nexus.getNormalizedName( a_rdn ) ) ;
    }


    ////////////////////////////////////////////////
    // Package Friendly & Private Utility Methods //
    ////////////////////////////////////////////////


    static Object deserialize( LdapEntry a_entry )
        throws NamingException
    {
        ObjectInputStream l_in = null ;
        String l_className = ( String )
            a_entry.getSingleValue( JCLASSNAME_ATTR ) ;

        try
        {
            byte [] l_data = ( byte [] )
                a_entry.getSingleValue( JSERIALDATA_ATTR ) ;
            l_in = new ObjectInputStream( new ByteArrayInputStream( l_data ) ) ;
            Object l_obj = l_in.readObject() ;
            return l_obj ;
        }
        catch( Exception e )
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
            catch( IOException e )
            {
                throw new NamingException(
                    "object deserialization stream close() failure" ) ;
            }
        }
    }


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
        catch( Exception e )
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
            catch( IOException e )
            {
                throw new NamingException(
                    "object serialization stream close() failure" ) ;
            }
        }
    }


    static void populate( LdapEntry a_entry, Name a_rdn, Object a_obj )
        throws NamingException
    {
        // Add the rdn attribute
        a_entry.addValue(NamespaceTools.getRdnAttribute( a_rdn.get( 0 ) ),
            NamespaceTools.getRdnValue( a_rdn.get( 0 ) ) ) ;

        // Let's add the object classes first:
        //  objectClass: top
        //  objectClass: javaObject
        //  objectClass: javaContainer
        //  objectClass: javaSerializedObject
        a_entry.addValue( OBJECTCLASS_ATTR, TOP_ATTR ) ;
        a_entry.addValue( OBJECTCLASS_ATTR, JOBJECT_ATTR ) ;
        a_entry.addValue( OBJECTCLASS_ATTR, JCONTAINER_ATTR ) ;
        a_entry.addValue( OBJECTCLASS_ATTR, JSERIALIZEDOBJ_ATTR ) ;

        // Add the javaClassName and javaSerializedData attributes
        a_entry.addValue( JCLASSNAME_ATTR, a_obj.getClass().getName() ) ;
        a_entry.addValue( JSERIALDATA_ATTR, serialize( a_obj ) ) ;

        // Add all the class names this object can be cast to:
        Class [] l_classes = a_obj.getClass().getClasses() ;
        for( int ii = 0; ii < l_classes.length; ii++ )
        {
            a_entry.addValue( JCLASSNAMES_ATTR, l_classes[ii].getName() ) ;
        }
    }
}
