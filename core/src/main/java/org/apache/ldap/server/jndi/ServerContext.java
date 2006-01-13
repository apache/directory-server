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
package org.apache.ldap.server.jndi;


import java.io.Serializable;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Set;

import javax.naming.ConfigurationException;
import javax.naming.Context;
import javax.naming.InvalidNameException;
import javax.naming.Name;
import javax.naming.NameNotFoundException;
import javax.naming.NameParser;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.Reference;
import javax.naming.Referenceable;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.SearchControls;
import javax.naming.event.EventContext;
import javax.naming.event.NamingListener;
import javax.naming.ldap.Control;
import javax.naming.spi.DirStateFactory;
import javax.naming.spi.DirectoryManager;

import org.apache.ldap.common.exception.LdapNoPermissionException;
import org.apache.ldap.common.filter.ExprNode;
import org.apache.ldap.common.filter.PresenceNode;
import org.apache.ldap.common.message.LockableAttributesImpl;
import org.apache.ldap.common.name.LdapName;
import org.apache.ldap.common.util.NamespaceTools;
import org.apache.ldap.common.util.StringTools;
import org.apache.ldap.server.DirectoryServiceConfiguration;
import org.apache.ldap.server.DirectoryService;
import org.apache.ldap.server.authn.AuthenticationService;
import org.apache.ldap.server.authn.LdapPrincipal;
import org.apache.ldap.server.partition.DirectoryPartitionNexus;
import org.apache.ldap.server.partition.DirectoryPartitionNexusProxy;


/**
 * A non-federated abstract Context implementation.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public abstract class ServerContext implements EventContext
{
    /** property key used for deleting the old RDN on a rename */
    public static final String DELETE_OLD_RDN_PROP = "java.naming.ldap.deleteRDN";

    /** The directory service which owns this context **/
    private final DirectoryService service;
    
    /** The interceptor proxy to the backend nexus */
    private final DirectoryPartitionNexus nexusProxy;

    /** The cloned environment used by this Context */
    private final Hashtable env;

    /** The distinguished name of this Context */
    private final LdapName dn;

    /** The set of registered NamingListeners */
    private final Set listeners = new HashSet();

    /** The Principal associated with this context */
    private LdapPrincipal principal;

    // ------------------------------------------------------------------------
    // Constructors
    // ------------------------------------------------------------------------


    /**
     * Must be called by all subclasses to initialize the nexus proxy and the
     * environment settings to be used by this Context implementation.  This
     * specific contstructor relies on the presence of the {@link
     * Context#PROVIDER_URL} key and value to determine the distinguished name
     * of the newly created context.  It also checks to make sure the
     * referenced name actually exists within the system.  This constructor
     * is used for all InitialContext requests.
     * 
     * @param service the parent service that manages this context
     * @param env the environment properties used by this context.
     * @throws NamingException if the environment parameters are not set 
     * correctly.
     */
    protected ServerContext( DirectoryService service, Hashtable env ) throws NamingException
    {
        this.service = service;
        
        // set references to cloned env and the proxy
        this.nexusProxy = new DirectoryPartitionNexusProxy( this, service );
        
        DirectoryServiceConfiguration cfg = service.getConfiguration();
        
        this.env = ( Hashtable ) cfg.getEnvironment().clone();
        this.env.putAll( env );

        /* --------------------------------------------------------------------
         * check for the provider URL property and make sure it exists
         * as a valid value or else we need to throw a configuration error
         * ------------------------------------------------------------------ */
        if ( ! env.containsKey( Context.PROVIDER_URL ) )
        {
            String msg = "Expected property " + Context.PROVIDER_URL;
            msg += " but could not find it in env!";

            throw new ConfigurationException( msg );
        }

        String url = ( String ) env.get( Context.PROVIDER_URL );

        if ( url == null )
        {
            String msg = "Expected value for property " + Context.PROVIDER_URL;
            msg += " but it was set to null in env!";

            throw new ConfigurationException( msg );
        }

        dn = new LdapName( url );

        if ( ! nexusProxy.hasEntry( dn ) )
        {
            throw new NameNotFoundException( dn + " does not exist" );
        }
    }


    /**
     * Must be called by all subclasses to initialize the nexus proxy and the
     * environment settings to be used by this Context implementation.  This
     * constructor is used to propagate new contexts from existing contexts.
     *
     * @param principal the directory user principal that is propagated
     * @param nexusProxy the intercepting proxy to the nexus
     * @param env the environment properties used by this context
     * @param dn the distinguished name of this context
     */
    protected ServerContext( DirectoryService service, LdapPrincipal principal, Name dn )
    {
        this.service = service;
        this.dn = ( LdapName ) dn.clone();
        this.env = ( Hashtable ) service.getConfiguration().getEnvironment();
        this.env.put( PROVIDER_URL, dn.toString() );
        this.nexusProxy = new DirectoryPartitionNexusProxy( this, service );;
        this.principal = principal;
    }


    // ------------------------------------------------------------------------
    // New Impl Specific Public Methods
    // ------------------------------------------------------------------------

    /**
     * Returns the {@link DirectoryService} which manages this context.
     */
    public DirectoryService getService()
    {
        return service;
    }

    /**
     * Gets the principal of the authenticated user which also happens to own
     */
    public LdapPrincipal getPrincipal()
    {
        return principal;
    }


    /**
     * Sets the principal of the authenticated user which also happens to own.
     * This method can be invoked only once to keep this property safe.  This
     * method has been changed to be public but it can only be set by the
     * AuthenticationService to prevent malicious code from changing the
     * effective principal.
     */
    public void setPrincipal( AuthenticationService.TrustedPrincipalWrapper wrapper )
    {
        this.principal = wrapper.getPrincipal();
    }


    // ------------------------------------------------------------------------
    // Protected Accessor Methods
    // ------------------------------------------------------------------------


    /**
     * Gets the RootNexus proxy.
     *
     * @return the proxy to the backend nexus.
     */
    protected DirectoryPartitionNexus getNexusProxy()
    {
       return nexusProxy ;
    }


    /**
     * Gets the distinguished name of the entry associated with this Context.
     * 
     * @return the distinguished name of this Context's entry.
     */
    protected Name getDn()
    {
        return dn;
    }


    // ------------------------------------------------------------------------
    // JNDI Context Interface Methods
    // ------------------------------------------------------------------------


    /**
     * @see javax.naming.Context#close()
     */
    public void close() throws NamingException
    {
        Iterator list = listeners.iterator();
        while ( list.hasNext() )
        {
            ( ( DirectoryPartitionNexusProxy ) this.nexusProxy )
                    .removeNamingListener( this, ( NamingListener ) list.next() );
        }
    }


    /**
     * @see javax.naming.Context#getNameInNamespace()
     */
    public String getNameInNamespace() throws NamingException
    {
        return dn.toString();
    }


    /**
     * @see javax.naming.Context#getEnvironment()
     */
    public Hashtable getEnvironment()
    {
        return env;
    }


    /**
     * @see javax.naming.Context#addToEnvironment(java.lang.String, 
     * java.lang.Object)
     */
    public Object addToEnvironment( String propName, Object propVal ) throws NamingException
    {
        return env.put( propName, propVal );
    }


    /**
     * @see javax.naming.Context#removeFromEnvironment(java.lang.String)
     */
    public Object removeFromEnvironment( String propName ) throws NamingException
    {
        return env.remove( propName );
    }


    /**
     * @see javax.naming.Context#createSubcontext(java.lang.String)
     */
    public Context createSubcontext( String name ) throws NamingException
    {
        return createSubcontext( new LdapName( name ) );
    }


    /**
     * @see javax.naming.Context#createSubcontext(javax.naming.Name)
     */
    public Context createSubcontext( Name name ) throws NamingException
    {
        Attributes attributes = new LockableAttributesImpl();
        LdapName target = buildTarget( name );

        String rdn = name.get( name.size() - 1 );
        String rdnAttribute = NamespaceTools.getRdnAttribute( rdn );
        String rdnValue = NamespaceTools.getRdnValue( rdn );

        attributes.put( rdnAttribute, rdnValue );
        attributes.put( JavaLdapSupport.OBJECTCLASS_ATTR, JavaLdapSupport.JCONTAINER_ATTR );
        attributes.put( JavaLdapSupport.OBJECTCLASS_ATTR, JavaLdapSupport.TOP_ATTR );
        
        /*
         * Add the new context to the server which as a side effect adds 
         * operational attributes to the attributes refering instance which
         * can them be used to initialize a new ServerLdapContext.  Remember
         * we need to copy over the controls as well to propagate the complete 
         * environment besides whats in the hashtable for env.
         */
        nexusProxy.add( target.toString(), target, attributes );
        return new ServerLdapContext( service, principal, target );
    }


    /**
     * @see javax.naming.Context#destroySubcontext(java.lang.String)
     */
    public void destroySubcontext( String name ) throws NamingException
    {
        destroySubcontext( new LdapName( name ) );
    }


    /**
     * @see javax.naming.Context#destroySubcontext(javax.naming.Name)
     */
    public void destroySubcontext( Name name ) throws NamingException
    {
        Name target = buildTarget( name );
        if ( target.size() == 0 )
        {
            throw new LdapNoPermissionException( "can't delete the rootDSE" );
        }

        nexusProxy.delete( target );
    }


    /**
     * @see javax.naming.Context#bind(java.lang.String, java.lang.Object)
     */
    public void bind( String name, Object obj ) throws NamingException
    {
        bind( new LdapName( name ), obj );
    }
    

    /**
     * @see javax.naming.Context#bind(javax.naming.Name, java.lang.Object)
     */
    public void bind( Name name, Object obj ) throws NamingException
    {
        // First, use state factories to do a transformation
        DirStateFactory.Result res = DirectoryManager.getStateToBind( obj, name, this, env, null );
        Attributes outAttrs = res.getAttributes();

        if ( outAttrs != null )
        {
            Name target = buildTarget( name );
            nexusProxy.add( target.toString(), target, outAttrs );
            return;
        }

        // Check for Referenceable
        if ( obj instanceof Referenceable )
        {
            throw new NamingException( "Do not know how to store Referenceables yet!" );
        }

        // Store different formats
        if ( obj instanceof Reference )
        {
            // Store as ref and add outAttrs
            throw new NamingException( "Do not know how to store References yet!" );
        }
        else if ( obj instanceof Serializable )
        {
            // Serialize and add outAttrs
            Attributes attributes = new LockableAttributesImpl();
            if ( outAttrs != null && outAttrs.size() > 0 )
            {
                NamingEnumeration list = outAttrs.getAll();
                while ( list.hasMore() )
                {
                    attributes.put( ( Attribute ) list.next() );
                }
            }

            Name target = buildTarget( name );

            // Serialize object into entry attributes and add it.
            JavaLdapSupport.serialize( attributes, obj );
            nexusProxy.add( target.toString(), target, attributes );
        }
        else if ( obj instanceof DirContext )
        {
            // Grab attributes and merge with outAttrs
            Attributes attributes = ( ( DirContext ) obj ).getAttributes( "" );
            if ( outAttrs != null && outAttrs.size() > 0 )
            {
                NamingEnumeration list = outAttrs.getAll();
                while ( list.hasMore() )
                {
                    attributes.put( ( Attribute ) list.next() );
                }
            }

            Name target = buildTarget( name );
            nexusProxy.add( target.toString(), target, attributes );
        }
        else
        {
            throw new NamingException( "Can't find a way to bind: " + obj );
        }
    }


    /**
     * @see javax.naming.Context#rename(java.lang.String, java.lang.String)
     */
    public void rename( String oldName, String newName ) throws NamingException
    {
        rename( new LdapName( oldName ), new LdapName( newName ) );
    }


    /**
     * @see javax.naming.Context#rename(javax.naming.Name, javax.naming.Name)
     */
    public void rename( Name oldName, Name newName ) throws NamingException
    {
        Name oldDn = buildTarget( oldName );
        Name newDn = buildTarget( newName );
        if ( oldDn.size() == 0 )
        {
            throw new LdapNoPermissionException( "can't rename the rootDSE" );
        }

        Name oldBase = oldName.getPrefix( 1 );
        Name newBase = newName.getPrefix( 1 );
        String newRdn = newName.get( newName.size() - 1 );
        String oldRdn = oldName.get( oldName.size() - 1 );
        boolean delOldRdn = true;
            
        /*
         * Attempt to use the java.naming.ldap.deleteRDN environment property
         * to get an override for the deleteOldRdn option to modifyRdn.  
         */
        if ( null != env.get( DELETE_OLD_RDN_PROP ) )
        {
            String delOldRdnStr = ( String ) env.get( DELETE_OLD_RDN_PROP );
            delOldRdn = ! delOldRdnStr.equals( "false" );
            delOldRdn = delOldRdn || delOldRdnStr.equals( "no" );
            delOldRdn = delOldRdn || delOldRdnStr.equals( "0" );
        }

        /*
         * We need to determine if this rename operation corresponds to a simple
         * RDN name change or a move operation.  If the two names are the same
         * except for the RDN then it is a simple modifyRdn operation.  If the
         * names differ in size or have a different baseDN then the operation is
         * a move operation.  Furthermore if the RDN in the move operation 
         * changes it is both an RDN change and a move operation.
         */
        if ( oldName.size() == newName.size() && oldBase.equals( newBase ) )
        {
            nexusProxy.modifyRn( oldDn, newRdn, delOldRdn );
        }
        else
        {
            Name parent = newDn.getPrefix( 1 );
            if ( newRdn.equalsIgnoreCase( oldRdn ) )
            {
                nexusProxy.move( oldDn, parent );
            }
            else
            {
                nexusProxy.move( oldDn, parent, newRdn, delOldRdn );
            }
        }
    }


    /**
     * @see javax.naming.Context#rebind(java.lang.String, java.lang.Object)
     */
    public void rebind( String name, Object obj ) throws NamingException
    {
        rebind( new LdapName( name ), obj );
    }


    /**
     * @see javax.naming.Context#rebind(javax.naming.Name, java.lang.Object)
     */
    public void rebind( Name name, Object obj ) throws NamingException
    {
        Name target = buildTarget( name );
        if ( nexusProxy.hasEntry( target ) )
        {
            nexusProxy.delete( target );
        }
        bind( name, obj );
    }


    /**
     * @see javax.naming.Context#unbind(java.lang.String)
     */
    public void unbind( String name ) throws NamingException
    {
        unbind( new LdapName( name ) );
    }


    /**
     * @see javax.naming.Context#unbind(javax.naming.Name)
     */
    public void unbind( Name name ) throws NamingException
    {
        nexusProxy.delete( buildTarget( name ) );
    }


    /**
     * @see javax.naming.Context#lookup(java.lang.String)
     */
    public Object lookup( String name ) throws NamingException
    {
        if ( StringTools.isEmpty( name ) )
        {
            return lookup( LdapName.EMPTY_LDAP_NAME );
        }
        else
        {
            return lookup( new LdapName( name ) );
        }
    }


    /**
     * @see javax.naming.Context#lookup(javax.naming.Name)
     */
    public Object lookup( Name name ) throws NamingException
    {
        Object obj;
        LdapName target = buildTarget( name );
        Attributes attributes = nexusProxy.lookup( target );

        try
        {
            obj = DirectoryManager.getObjectInstance( null, name, this, env, attributes );
        }
        catch ( Exception e )
        {
            String msg = "Failed to create an object for " + target;
            msg += " using object factories within the context's environment.";
            NamingException ne = new NamingException( msg );
            ne.setRootCause( e );
            throw ne;
        }

        if ( obj != null )
        {
            return obj;
        }

        // First lets test and see if the entry is a serialized java object
        if ( attributes.get( JavaLdapSupport.JCLASSNAME_ATTR ) != null )
        {
            // Give back serialized object and not a context
            return JavaLdapSupport.deserialize( attributes );
        }
        
        // Initialize and return a context since the entry is not a java object
        ServerLdapContext ctx = new ServerLdapContext( service, principal, target );
            
        // Need to add controls to propagate extended ldap operational env
        Control [] controls = ( ( ServerLdapContext ) this ).getRequestControls();

        if ( null != controls )
        {    
            ctx.setRequestControls( ( Control [] ) controls.clone() );
        }
        
        return ctx;
    }


    /**
     * @see javax.naming.Context#lookupLink(java.lang.String)
     */
    public Object lookupLink( String name ) throws NamingException
    {
        throw new UnsupportedOperationException();
    }


    /**
     * @see javax.naming.Context#lookupLink(javax.naming.Name)
     */
    public Object lookupLink( Name name ) throws NamingException
    {
        throw new UnsupportedOperationException();
    }


    /**
     * Non-federated implementation presuming the name argument is not a 
     * composite name spanning multiple namespaces but a compound name in 
     * the same LDAP namespace.  Hence the parser returned is always the
     * same as calling this method with the empty String.
     * 
     * @see javax.naming.Context#getNameParser(java.lang.String)
     */
    public NameParser getNameParser( String name ) throws NamingException
    {
        return LdapName.getNameParser();
    }


    /**
     * Non-federated implementation presuming the name argument is not a 
     * composite name spanning multiple namespaces but a compound name in 
     * the same LDAP namespace.  Hence the parser returned is always the
     * same as calling this method with the empty String Name.
     * 
     * @see javax.naming.Context#getNameParser(javax.naming.Name)
     */
    public NameParser getNameParser( Name name ) throws NamingException
    {
        return LdapName.getNameParser();
    }


    /**
     * @see javax.naming.Context#list(java.lang.String)
     */
    public NamingEnumeration list( String name ) throws NamingException
    {
        return list( new LdapName( name ) );
    }


    /**
     * @see javax.naming.Context#list(javax.naming.Name)
     */
    public NamingEnumeration list( Name name ) throws NamingException
    {
        return nexusProxy.list( buildTarget( name ) );
    }


    /**
     * @see javax.naming.Context#listBindings(java.lang.String)
     */
    public NamingEnumeration listBindings( String name ) throws NamingException
    {
        return listBindings( new LdapName( name ) );
    }


    /**
     * @see javax.naming.Context#listBindings(javax.naming.Name)
     */
    public NamingEnumeration listBindings( Name name ) throws NamingException
    {
        // Conduct a special one level search at base for all objects
        Name base = buildTarget( name );
        PresenceNode filter = new PresenceNode( "objectClass" );
        SearchControls ctls = new SearchControls();
        ctls.setSearchScope( SearchControls.ONELEVEL_SCOPE );
        return nexusProxy.search( base , getEnvironment(), filter, ctls );
    }


    /**
     * @see javax.naming.Context#composeName(java.lang.String, java.lang.String)
     */
    public String composeName( String name, String prefix ) throws NamingException
    {
        return composeName( new LdapName( name ), new LdapName( prefix ) ).toString();
    }


    /**
     * @see javax.naming.Context#composeName(javax.naming.Name,
     * javax.naming.Name)
     */
    public Name composeName( Name name, Name prefix ) throws NamingException
    {
        // No prefix reduces to name, or the name relative to this context
        if ( prefix == null || prefix.size() == 0 )
        {
            return name;
        }

        /*
         * Example: This context is ou=people and say name is the relative
         * name of uid=jwalker and the prefix is dc=domain.  Then we must
         * compose the name relative to prefix which would be:
         * 
         * uid=jwalker,ou=people,dc=domain.
         * 
         * The following general algorithm generates the right name:
         *      1). Find the Dn for name and walk it from the head to tail
         *          trying to match for the head of prefix.
         *      2). Remove name components from the Dn until a match for the 
         *          head of the prefix is found.
         *      3). Return the remainder of the fqn or Dn after chewing off some
         */
         
        // 1). Find the Dn for name and walk it from the head to tail
        Name fqn = buildTarget( name );
        String head = prefix.get( 0 );
        
        // 2). Walk the fqn trying to match for the head of the prefix
        while ( fqn.size() > 0 )
        {
            // match found end loop
            if ( fqn.get( 0 ).equalsIgnoreCase( head ) )
            {
                return fqn;
            }
            else // 2). Remove name components from the Dn until a match 
            {
                fqn.remove( 0 );
            }
        }

        String msg = "The prefix '" + prefix + "' is not an ancestor of this ";
        msg += "entry '" + dn + "'";
        throw new NamingException( msg );
    }


    // ------------------------------------------------------------------------
    // EventContext implementations
    // ------------------------------------------------------------------------


    public void addNamingListener( Name name, int scope, NamingListener namingListener ) throws NamingException
    {
        ExprNode filter = new PresenceNode( "objectClass" );
        SearchControls controls = new SearchControls();
        controls.setSearchScope( scope );
        ( ( DirectoryPartitionNexusProxy ) this.nexusProxy )
                .addNamingListener( this, buildTarget( name ), filter, controls, namingListener );
        listeners.add( namingListener );
    }


    public void addNamingListener( String name, int scope, NamingListener namingListener ) throws NamingException
    {
        addNamingListener( new LdapName( name ), scope, namingListener );
    }


    public void removeNamingListener( NamingListener namingListener ) throws NamingException
    {
        ( ( DirectoryPartitionNexusProxy ) this.nexusProxy ).removeNamingListener( this, namingListener );
        listeners.remove( namingListener );
    }


    public boolean targetMustExist() throws NamingException
    {
        return false;
    }


    /**
     * Allows subclasses to register and unregister listeners.
     *
     * @return the set of listeners used for tracking registered name listeners.
     */
    protected Set getListeners()
    {
        return listeners;
    }


    // ------------------------------------------------------------------------
    // Utility Methods to Reduce Code
    // ------------------------------------------------------------------------
    
    
    /**
     * Clones this context's DN and adds the components of the name relative to 
     * this context to the left hand side of this context's cloned DN. 
     * 
     * @param relativeName a name relative to this context.
     * @return the name of the target
     * @throws InvalidNameException if relativeName is not a valid name in
     *      the LDAP namespace.
     */
    LdapName buildTarget( Name relativeName ) throws InvalidNameException
    {
        // Clone our DN or absolute path
        LdapName target = ( LdapName ) dn.clone();
        
        // Add to left hand side of cloned DN the relative name arg
        target.addAll( target.size(), relativeName );
        return target;
    }
}
