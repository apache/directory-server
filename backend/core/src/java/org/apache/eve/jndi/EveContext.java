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
package org.apache.eve.jndi;


import java.util.Hashtable ;

import javax.naming.Name ;
import javax.naming.Context ;
import javax.naming.NameParser ;
import javax.naming.ldap.Control ;
import javax.naming.NamingException ;
import javax.naming.NamingEnumeration ;
import javax.naming.directory.Attributes ;
import javax.naming.InvalidNameException ;
import javax.naming.directory.SearchControls ;

import org.apache.ldap.common.name.LdapName ;
import org.apache.ldap.common.filter.PresenceNode ;
import org.apache.ldap.common.util.NamespaceTools ;
import org.apache.ldap.common.message.LockableAttributesImpl ;

import org.apache.eve.PartitionNexus;


/**
 * A non-federated abstract Context implementation.
 *
 * @author <a href="mailto:directory-dev@incubator.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public abstract class EveContext implements Context
{
    /** */
    public static final String DELETE_OLD_RDN_PROP = "java.naming.ldap.deleteRDN" ;

    /** The interceptor proxy to the backend nexus */
    private final PartitionNexus m_nexusProxy ;
    /** The cloned environment used by this Context */
    private final Hashtable m_env ;
    /** The distinguished name of this Context */
    private final LdapName m_dn ;    
    

    // ------------------------------------------------------------------------
    // Constructors
    // ------------------------------------------------------------------------


    /**
     * Must be called by all subclasses to initialize the nexus proxy and the
     * environment settings to be used by this Context implementation.
     * 
     * @param a_nexusProxy the intercepting proxy to the nexus.
     * @param a_env the environment properties used by this context.
     * @throws NamingException if the environment parameters are not set 
     * correctly.
     */
    protected EveContext( PartitionNexus a_nexusProxy, Hashtable a_env ) throws NamingException
    {
        m_nexusProxy = a_nexusProxy ;
        m_env = ( Hashtable ) a_env.clone() ;
        
        if ( null == m_env.get( PROVIDER_URL ) )
        {
            throw new NamingException( PROVIDER_URL 
                + " property not found in environment." ) ;
        }
        

        /*
         * TODO Make sure we can handle URLs here as well as simple DNs
         * The PROVIDER_URL is interpreted as just a entry Dn since we are 
         * within the server.  However this may change in the future if we 
         * want to convey the listener from which the protocol originating
         * requests are comming from.
         */
        m_dn = new LdapName( ( String ) m_env.get( PROVIDER_URL ) ) ;
    }


    /**
     * Must be called by all subclasses to initialize the nexus proxy and the
     * environment settings to be used by this Context implementation.
     * 
     * @param a_nexusProxy the intercepting proxy to the nexus
     * @param a_env the environment properties used by this context
     * @param a_dn the distinguished name of this context
     */
    protected EveContext( PartitionNexus a_nexusProxy, Hashtable a_env, LdapName a_dn )
    {
        m_dn = ( LdapName ) a_dn.clone() ; 
        m_env = ( Hashtable ) a_env.clone() ;
        m_env.put( PROVIDER_URL, m_dn.toString() ) ; 
        m_nexusProxy = a_nexusProxy ;
    }


    // ------------------------------------------------------------------------
    // Protected Accessor Methods
    // ------------------------------------------------------------------------


    /**
     * Gets the RootNexus proxy.
     * 
     * @return the proxy to the backend nexus.
     */
    protected PartitionNexus getNexusProxy()
    {
       return m_nexusProxy  ;
    }
    
    
    /**
     * Gets the distinguished name of the entry associated with this Context.
     * 
     * @return the distinguished name of this Context's entry.
     */
    protected Name getDn()
    {
        return m_dn ;
    }


    // ------------------------------------------------------------------------
    // JNDI Context Interface Methods
    // ------------------------------------------------------------------------


    /**
     * @see javax.naming.Context#close()
     */
    public void close() throws NamingException
    {
        // Does nothing yet?
    }


    /**
     * @see javax.naming.Context#getNameInNamespace()
     */
    public String getNameInNamespace() throws NamingException
    {
        return m_dn.toString() ;
    }


    /**
     * @see javax.naming.Context#getEnvironment()
     */
    public Hashtable getEnvironment() throws NamingException
    {
        return m_env ;
    }


    /**
     * @see javax.naming.Context#addToEnvironment(java.lang.String, 
     * java.lang.Object)
     */
    public Object addToEnvironment( String a_propName, Object a_propVal )
        throws NamingException
    {
        return m_env.put( a_propName, a_propVal ) ;
    }


    /**
     * @see javax.naming.Context#removeFromEnvironment(java.lang.String)
     */
    public Object removeFromEnvironment( String a_propName ) 
        throws NamingException
    {
        return m_env.remove( a_propName ) ;
    }


    /**
     * @see javax.naming.Context#createSubcontext(java.lang.String)
     */
    public Context createSubcontext( String a_name ) throws NamingException
    {
        return createSubcontext( new LdapName( a_name ) ) ;
    }


    /**
     * @see javax.naming.Context#createSubcontext(javax.naming.Name)
     */
    public Context createSubcontext( Name a_name ) throws NamingException
    {
        /* 
         * Start building the server side attributes to be added directly to
         * the backend.
         * 
         * The RDN from a_name can be a multivalued RDN based on more than one
         * attribute using the '+' AVA concatenator in a name component.  Right
         * now this code will bomb out because we presume single valued RDNs.
         * 
         * TODO Add multivalued RDN handling code 
         */
        Attributes l_attributes = new LockableAttributesImpl() ;
        LdapName l_target = buildTarget( a_name ) ;
        String l_rdn = a_name.get( a_name.size() - 1 ) ;
        String l_rdnAttribute = NamespaceTools.getRdnAttribute( l_rdn ) ;
        String l_rdnValue = NamespaceTools.getRdnValue( l_rdn ) ;

        /* 
         * TODO Add code within the interceptor service managing operational
         * attributes the ability to add the target user provided DN to the 
         * attributes before normalization.  The result should have ths same
         * affect as the following line within the interceptor.
         * 
         * l_attributes.put( BootstrapSchema.DN_ATTR, l_target.toString() ) ;
         */
        l_attributes.put( l_rdnAttribute, l_rdnValue ) ;
        l_attributes.put( JavaLdapSupport.OBJECTCLASS_ATTR,
            JavaLdapSupport.JCONTAINER_ATTR ) ;
        l_attributes.put( JavaLdapSupport.OBJECTCLASS_ATTR,
            JavaLdapSupport.TOP_ATTR ) ;
        
        /*
         * Add the new context to the server which as a side effect adds 
         * operational attributes to the l_attributes refering instance which 
         * can them be used to initialize a new EveLdapContext.  Remember
         * we need to copy over the controls as well to propagate the complete 
         * environment besides whats in the hashtable for m_env.
         */
        m_nexusProxy.add( l_target.toString(), l_target, l_attributes ) ;
        
        EveLdapContext l_ctx =
            new EveLdapContext( m_nexusProxy, m_env, l_target ) ;
        Control [] l_controls = ( Control [] )
            ( ( EveLdapContext ) this ).getRequestControls().clone() ;
        l_ctx.setRequestControls( l_controls ) ;
        return l_ctx ;
    }


    /**
     * @see javax.naming.Context#destroySubcontext(java.lang.String)
     */
    public void destroySubcontext( String a_name ) throws NamingException
    {
        destroySubcontext( new LdapName( a_name ) ) ;
    }


    /**
     * @see javax.naming.Context#destroySubcontext(javax.naming.Name)
     */
    public void destroySubcontext( Name a_name ) throws NamingException
    {
        Name l_target = buildTarget( a_name ) ;
        m_nexusProxy.delete( l_target ) ;
    }


    /**
     * @see javax.naming.Context#bind(java.lang.String, java.lang.Object)
     */
    public void bind( String a_name, Object a_obj ) throws NamingException
    {
        bind( new LdapName( a_name ), a_obj ) ;
    }
    

    /**
     * @see javax.naming.Context#bind(javax.naming.Name, java.lang.Object)
     */
    public void bind( Name a_name, Object a_obj ) throws NamingException
    {
        if ( a_obj instanceof EveLdapContext )
        {
            throw new IllegalArgumentException(
                "Cannot bind a directory context object!" ) ;
        }

        /* 
         * Start building the server side attributes to be added directly to
         * the backend.
         * 
         * The RDN from a_name can be a multivalued RDN based on more than one
         * attribute using the '+' AVA concatenator in a name component.  Right
         * now this code will bomb out because we presume single valued RDNs.
         * 
         * TODO Add multivalued RDN handling code 
         */
        Attributes l_attributes = new LockableAttributesImpl() ;
        Name l_target = buildTarget( a_name ) ;

        // Serialize object into entry attributes and add it.
        JavaLdapSupport.serialize( l_attributes, a_obj ) ;
        m_nexusProxy.add( l_target.toString(), l_target, l_attributes ) ;
    }


    /**
     * @see javax.naming.Context#rename(java.lang.String, java.lang.String)
     */
    public void rename( String a_oldName, String a_newName ) 
        throws NamingException
    {
        rename( new LdapName( a_oldName ), new LdapName( a_newName ) ) ;
    }


    /**
     * @see javax.naming.Context#rename(javax.naming.Name, javax.naming.Name)
     */
    public void rename( Name a_oldName, Name a_newName ) throws NamingException
    {
        Name l_oldDn = buildTarget( a_oldName ) ;
        Name l_newDn = buildTarget( a_newName ) ;
        Name l_oldBase = a_oldName.getSuffix( 1 ) ;
        Name l_newBase = a_newName.getSuffix( 1 ) ;

        String l_newRdn = a_newName.get( a_newName.size() - 1 ) ;
        String l_oldRdn = a_oldName.get( a_oldName.size() - 1 ) ;
                
        boolean l_delOldRdn = true ;
            
        /*
         * Attempt to use the java.naming.ldap.deleteRDN environment property
         * to get an override for the deleteOldRdn option to modifyRdn.  
         */
        if ( null != m_env.get( DELETE_OLD_RDN_PROP ) )
        {
            String l_delOldRdnStr = 
                ( String ) m_env.get( DELETE_OLD_RDN_PROP ) ;
            l_delOldRdn = ! ( l_delOldRdnStr.equals( "false" ) ||
                l_delOldRdnStr.equals( "no" ) || 
                l_delOldRdnStr.equals( "0" ) ) ; 
        }

        /*
         * We need to determine if this rename operation corresponds to a simple
         * RDN name change or a move operation.  If the two names are the same
         * except for the RDN then it is a simple modifyRdn operation.  If the
         * names differ in size or have a different baseDN then the operation is
         * a move operation.  Furthermore if the RDN in the move operation 
         * changes it is both an RDN change and a move operation.
         */
        if ( a_oldName.size() == a_newName.size() && 
            l_oldBase.equals( l_newBase ) )
        {
            m_nexusProxy.modifyRn( l_oldDn, l_newRdn, l_delOldRdn ) ;
        }
        else
        {
            Name l_parent = l_newDn.getSuffix( 1 ) ;
            
            if ( l_newRdn.equalsIgnoreCase( l_oldRdn ) )
            {
                m_nexusProxy.move( l_oldDn, l_parent ) ;
            }
            else
            {
                m_nexusProxy.move( l_oldDn, l_parent, l_newRdn, l_delOldRdn ) ;
            }
        }
    }


    /**
     * @see javax.naming.Context#rebind(java.lang.String, java.lang.Object)
     */
    public void rebind( String a_name, Object a_obj ) throws NamingException
    {
        rebind( new LdapName( a_name ), a_obj ) ;
    }


    /**
     * @see javax.naming.Context#rebind(javax.naming.Name, java.lang.Object)
     */
    public void rebind( Name a_name, Object a_obj ) throws NamingException
    {
        Name l_target = buildTarget( a_name ) ;

        if ( m_nexusProxy.hasEntry( l_target ) ) 
        {
            m_nexusProxy.delete( l_target ) ;
        }

        bind( a_name, a_obj ) ;
    }


    /**
     * @see javax.naming.Context#unbind(java.lang.String)
     */
    public void unbind( String a_name ) throws NamingException
    {
        unbind( new LdapName( a_name ) ) ;
    }


    /**
     * @see javax.naming.Context#unbind(javax.naming.Name)
     */
    public void unbind( Name a_name ) throws NamingException
    {
        m_nexusProxy.delete( buildTarget( a_name ) ) ;
    }


    /**
     * @see javax.naming.Context#lookup(java.lang.String)
     */
    public Object lookup( String a_name ) throws NamingException
    {
        return lookup( new LdapName( a_name ) ) ;
    }


    /**
     * @see javax.naming.Context#lookup(javax.naming.Name)
     */
    public Object lookup( Name a_name ) throws NamingException
    {
        LdapName l_target = buildTarget( a_name ) ;
        Attributes l_attributes = m_nexusProxy.lookup( l_target ) ;
        
        // First lets test and see if the entry is a serialized java object
        if ( l_attributes.get( JavaLdapSupport.JCLASSNAME_ATTR ) != null )
        {
            // Give back serialized object and not a context
            return JavaLdapSupport.deserialize( l_attributes ) ;
        }
        
        // Initialize and return a context since the entry is not a java object
        EveLdapContext l_ctx = new EveLdapContext( m_nexusProxy,
            m_env, l_target ) ; 
            
        // Need to add controls to propagate extended ldap operational env
        Control [] l_controls = ( ( EveLdapContext ) this )
            .getRequestControls() ; 
        if ( null != l_controls )
        {    
            l_ctx.setRequestControls( ( Control [] ) l_controls.clone() ) ;
        }
        
        return l_ctx ;
    }


    /**
     * @see javax.naming.Context#lookupLink(java.lang.String)
     */
    public Object lookupLink( String a_name ) throws NamingException
    {
        throw new UnsupportedOperationException() ;
    }


    /**
     * @see javax.naming.Context#lookupLink(javax.naming.Name)
     */
    public Object lookupLink( Name a_name ) throws NamingException
    {
        throw new UnsupportedOperationException() ;
    }


    /**
     * Non-federated implementation presuming the name argument is not a 
     * composite name spanning multiple namespaces but a compound name in 
     * the same LDAP namespace.  Hence the parser returned is always the
     * same as calling this method with the empty String.
     * 
     * @see javax.naming.Context#getNameParser(java.lang.String)
     */
    public NameParser getNameParser( String a_name ) throws NamingException
    {
        return LdapName.getNameParser() ;
    }


    /**
     * Non-federated implementation presuming the name argument is not a 
     * composite name spanning multiple namespaces but a compound name in 
     * the same LDAP namespace.  Hence the parser returned is always the
     * same as calling this method with the empty String Name.
     * 
     * @see javax.naming.Context#getNameParser(javax.naming.Name)
     */
    public NameParser getNameParser( Name a_name ) throws NamingException
    {
        return LdapName.getNameParser() ;
    }


    /**
     * @see javax.naming.Context#list(java.lang.String)
     */
    public NamingEnumeration list( String a_name ) throws NamingException
    {
        return list( new LdapName( a_name ) ) ;
    }


    /**
     * @see javax.naming.Context#list(javax.naming.Name)
     */
    public NamingEnumeration list( Name a_name ) throws NamingException
    {
        return m_nexusProxy.list( buildTarget( a_name ) ) ;
    }


    /**
     * @see javax.naming.Context#listBindings(java.lang.String)
     */
    public NamingEnumeration listBindings( String a_name ) 
        throws NamingException
    {
        return listBindings( new LdapName( a_name ) ) ;
    }


    /**
     * @see javax.naming.Context#listBindings(javax.naming.Name)
     */
    public NamingEnumeration listBindings( Name a_name ) throws NamingException
    {
        // Conduct a special one level search at base for all objects
        Name l_base = buildTarget( a_name ) ;
        PresenceNode l_filter = new PresenceNode( "objectClass" ) ;
        SearchControls l_ctls = new SearchControls() ;
        l_ctls.setSearchScope( SearchControls.ONELEVEL_SCOPE ) ;

        return m_nexusProxy.search( l_base , getEnvironment(), l_filter, 
            l_ctls ) ;
    }


    /**
     * @see javax.naming.Context#composeName(java.lang.String, java.lang.String)
     */
    public String composeName( String a_name, String a_prefix )
        throws NamingException
    {
        return composeName( new LdapName( a_name ), 
            new LdapName( a_prefix ) ).toString() ;
    }


    /**
     * TODO Needs some serious testing here!
     * @see javax.naming.Context#composeName(javax.naming.Name, 
     * javax.naming.Name)
     */
    public Name composeName( Name a_name, Name a_prefix ) throws NamingException
    {
        // No prefix reduces to a_name, or the name relative to this context
        if ( a_prefix == null || a_prefix.size() == 0 )
        {
            return a_name ;
        }

        /*
         * Example: This context is ou=people and say name is the relative
         * name of uid=jwalker and the prefix is dc=domain.  Then we must
         * compose the name relative to a_prefix which would be:
         * 
         * uid=jwalker,ou=people,dc=domain.
         * 
         * The following general algorithm generates the right name:
         *      1). Find the Dn for a_name and walk it from the head to tail
         *          trying to match for the head of a_prefix.
         *      2). Remove name components from the Dn until a match for the 
         *          head of the prefix is found.
         *      3). Return the remainder of the fqn or Dn after chewing off some
         */
         
        // 1). Find the Dn for a_name and walk it from the head to tail
        Name l_fqn = buildTarget( a_name ) ;
        String l_head = a_prefix.get( 0 ) ;
        
        // 2). Walk the fqn trying to match for the head of the prefix
        while ( l_fqn.size() > 0 )
        {
            // match found end loop
            if ( l_fqn.get( 0 ).equalsIgnoreCase( l_head ) ) 
            {
                return l_fqn ;
            }
            else // 2). Remove name components from the Dn until a match 
            {
                l_fqn.remove( 0 ) ;
            }
        }
        
        throw new NamingException( "The prefix '" + a_prefix 
            + "' is not an ancestor of this "  + "entry '" + m_dn + "'" ) ;
    }
    
    
    // ------------------------------------------------------------------------
    // Utility Methods to Reduce Code
    // ------------------------------------------------------------------------
    
    
    /**
     * Clones this context's DN and adds the components of the name relative to 
     * this context to the left hand side of this context's cloned DN. 
     * 
     * @param a_relativeName a name relative to this context.
     * @return the name of the target
     * @throws InvalidNameException if a_relativeName is not a valid name in 
     *      the LDAP namespace.
     */
    LdapName buildTarget( Name a_relativeName )
        throws InvalidNameException
    {
        // Clone our DN or absolute path
        LdapName l_target = ( LdapName ) m_dn.clone() ;
        
        // Add to left hand side of cloned DN the relative name arg
        l_target.addAll( l_target.size(), a_relativeName ) ;
        return l_target ;
    }
}
