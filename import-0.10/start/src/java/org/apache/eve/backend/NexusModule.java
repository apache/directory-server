/*
 * $Id: NexusModule.java,v 1.26 2003/08/22 21:15:54 akarasulu Exp $
 *
 * -- (c) LDAPd Group                                                    --
 * -- Please refer to the LICENSE.txt file in the root directory of      --
 * -- any LDAPd project for copyright and distribution information.      --
 *
 */

package org.apache.eve.backend ;


import org.apache.eve.AbstractModule ;

import java.util.Map ;
import java.util.Date ;
import java.util.HashMap ;
import java.util.Iterator ;

import javax.naming.Name ;
import javax.naming.NameParser ;
import javax.naming.NamingException ;
import javax.naming.InvalidNameException ;
import javax.naming.NameNotFoundException ;

import org.apache.ldap.common.name.LdapName ;
import org.apache.ldap.common.filter.ExprNode ;

import org.apache.eve.schema.Schema ;
import org.apache.eve.client.ClientManager ;
import org.apache.eve.schema.SchemaManager ;
import org.apache.eve.client.ClientSession ;
import org.apache.eve.client.ClientManagerSlave ;

import org.apache.avalon.framework.ExceptionUtil ;
import org.apache.avalon.framework.service.ServiceManager ;
import org.apache.avalon.framework.service.ServiceException ;
import org.apache.avalon.framework.configuration.Configuration ;
import org.apache.avalon.framework.configuration.ConfigurationException ;


/**
 * Default backend nexus or unified backend implementation for the server.
 * 
 * @phoenix:block
 * @phoenix:service name="org.apache.eve.backend.UnifiedBackend"
 * @phoenix:mx-topic name="backend-nexus"
 *
 * @author <a href="mailto:aok123@bellsouth.net">Alex Karasulu</a>
 * @author $Author: akarasulu $
 * @version $Revision: 1.26 $
 */
public class NexusModule
    extends AbstractModule
    implements UnifiedBackend, ClientManagerSlave
{
    /** Config tag for RootDSE definition */
    public static final String ROOTDSE_TAG = "RootDSE" ;
    /** Config tag for RootDSE attributes */
	public static final String ATTRIBUTE_TAG = "attribute" ;

    ClientManager m_clientMan = null ;
    SchemaManager m_schemaMan = null ;
    Schema m_schema = null ;
    RootDSE m_rootDSE = null ;

    /** Map of backend normalized suffix strings to backends */
    Map m_backends = new HashMap() ;
    Map m_repListeners = new HashMap() ;
    Map m_adminUsers = new HashMap() ;


    // -------------------------------------------------------
    // Operational Attribute Management Methods
    // -------------------------------------------------------


    /**
     * Sets an entries operational attributes based on client session parameters
     * and whether or not the operation is a create or an update.
     *
     * @param an_entry the entry to add or alter the operational attributes of.
     * @param isCreate true if op attrs are added for create or false for update
     */
    private void setOperationalAttributes( LdapEntry an_entry,
        boolean isCreate ) throws BackendException, NamingException
    {
        String l_now = new Date().toGMTString() ;
        String l_userDn = null ;
        ClientSession l_session = m_clientMan.getClientSession() ;

        if( null == l_session )
        {
            throw new BackendException(
                "Cannot obtain client session object!" ) ;
        }

        // Get authenticated principal's Dn
        l_userDn = l_session.getPrincipal().getDn().toString() ;

        if( isCreate )
        {
            if( an_entry.hasAttribute( LdapEntry.CREATETIMESTAMP_ATTR ) )
            {
                an_entry.removeValues( LdapEntry.CREATETIMESTAMP_ATTR ) ;
            }

            if( an_entry.hasAttribute( LdapEntry.CREATORSNAME_ATTR ) )
            {
                an_entry.removeValues( LdapEntry.CREATORSNAME_ATTR ) ;
            }

            an_entry.addValue( LdapEntry.CREATETIMESTAMP_ATTR, l_now ) ;
            an_entry.addValue( LdapEntry.CREATORSNAME_ATTR, l_userDn ) ;
        }

        if( an_entry.hasAttribute( LdapEntry.MODIFYTIMESTAMP_ATTR ) )
        {
            an_entry.removeValues( LdapEntry.MODIFYTIMESTAMP_ATTR ) ;
        }

        if( an_entry.hasAttribute( LdapEntry.MODIFIERSNAME_ATTR ) )
        {
            an_entry.removeValues( LdapEntry.MODIFIERSNAME_ATTR ) ;
        }

        an_entry.addValue( LdapEntry.MODIFYTIMESTAMP_ATTR, l_now ) ;
        an_entry.addValue( LdapEntry.MODIFIERSNAME_ATTR, l_userDn ) ;
    }


    // -------------------------------------------------------
    // CRUD Method Implementations
    // -------------------------------------------------------


    public void create( LdapEntry an_entry )
        throws BackendException, NamingException
    {
        setOperationalAttributes ( an_entry, true ) ;
        getBackend( an_entry.getNormalizedDN() ).create( an_entry ) ;
    }


    public LdapEntry newEntry( String a_dn )
        throws BackendException, NamingException
    {
        return getBackend( getNormalizedName( a_dn ) ).newEntry( a_dn ) ;
    }


    public LdapEntry read( Name a_dn )
        throws BackendException, NamingException
    {
        return getBackend(
            getNormalizedName( a_dn.toString() ) ).read( a_dn ) ;
    }


    public void update(LdapEntry an_entry)
        throws BackendException, NamingException
    {
        setOperationalAttributes (an_entry, false) ;
        getBackend( an_entry.getNormalizedDN() ).update(an_entry) ;
    }


    public void delete( LdapEntry an_entry )
        throws BackendException, NamingException
    {
        getBackend( an_entry.getNormalizedDN() ).delete( an_entry ) ;
    }


    public LdapEntry getParent(Name a_childDn)
        throws BackendException, NamingException
    {
        return getBackend(a_childDn).getParent(a_childDn) ;
    }


    public boolean hasEntry(Name a_dn)
        throws BackendException, NamingException
    {
        return getBackend(a_dn).hasEntry(a_dn) ;
    }


    public boolean isSuffix(LdapEntry an_entry)
    {
        try {
        	return getBackend(an_entry.getNormalizedDN()).isSuffix(an_entry) ;
        } catch(NamingException e) {
            // Should never really be thrown.
            return false ;
        }
    }


    public Name getNormalizedName(String a_name)
        throws NamingException
    {
        return m_schema.getNormalizingParser().parse(a_name) ;
    }


    public Name getName(String a_name)
        throws NamingException
    {
        return m_schema.getNameParser().parse(a_name) ;
    }


    /**
     * V E R Y   I N E F F I C I E N T
     * 
     * This could be made much more efficient by hashing all the normalized
     * admin user names against their backends in a hashmap and doing the
     * lookup after converting the a_userDn arguement into its canonical form.
     */
    public boolean isAdminUser(Name a_userDn)
        throws NamingException
    {
        if(m_adminUsers.containsKey(a_userDn.toString())) {
            return true ;
        }

        return false ;
    }


    public RootDSE getRootDSE()
    {
        return m_rootDSE ;
    }


    public Cursor listChildren(Name a_parentDn)
        throws BackendException, NamingException
    {
        return getBackend(a_parentDn).listChildren(a_parentDn) ;
    }


    public Cursor search(ExprNode a_searchFilter, Name a_baseDn,
		  int a_scope)
        throws BackendException, NamingException
    {
        // Base search without a namespace returns the RootDSE within a
        // SingletonCursor that returns only one entry: the RootDSE.
        if(a_baseDn.toString().trim().equals("") && a_scope == BASE_SCOPE) {
            return new SingletonCursor(m_rootDSE) ;
        }

        return getBackend(a_baseDn).search(a_searchFilter,
            a_baseDn, a_scope) ;
    }


    /**
     * Modifies the relative distinguished name (RDN) of an entry without 
     * changing any parent child relationships.  This call has the side effect 
     * of altering the distinguished name of descendent entries if they exist.
     * The boolean argument will optionally remove the existing RDN attribute 
     * value pair replacing it with the new RDN attribute value pair.  If other
	 * RDN attribute value pairs exist besides the current RDN they will be 
     * spared.
     * 
     * If the new Rdn and the old Rdn are equal this method does nothing.
     *
     * @param an_entry the entry whose RDN is to be modified.
     * @param a_newRdn the new Rdn that is to replace the current Rdn.
     * @param a_deleteOldRdn deletes the old Rdn attribute value pair if true.
     * @throws BackendException when the operation cannot be performed due to a
     * backing store error.
     * @throws NamingException when naming violations and or schema violations
     * occur due to attempting this operation.
     */
    public void modifyRdn( LdapEntry an_entry, Name a_newRdn,
        boolean a_deleteOldRdn ) 
	    throws BackendException, NamingException
    {
        AtomicBackend l_be = getBackend( an_entry.getNormalizedDN() ) ;
        Name l_dn = new LdapName( an_entry.getEntryDN() ) ;
        String l_oldRdnStr = l_dn.get( l_dn.size() - 1 ) ;
        String l_newRdnStr = a_newRdn.get( a_newRdn.size() - 1 ) ;
        
        // Set common operational attributes on the entry first
        setOperationalAttributes( an_entry, false ) ;
        
        // Only perform modifyRdn operation if newRdn is not the same as old
        if ( ! l_oldRdnStr.equals( l_newRdnStr ) ) 
        {
            l_be.modifyRdn( an_entry, a_newRdn, a_deleteOldRdn ) ;
        }

        getBackend(an_entry.getNormalizedDN()).
            modifyRdn(an_entry, a_newRdn, a_deleteOldRdn) ;
    }


    /**
     * This overload combines the first two method operations into one.
     * It changes the Rdn and the parent prefix at the same time while
	 * recursing name changes to all descendants of the child entry. It
	 * is obviously more complex than the other two operations alone and
	 * involves changes to both parent child indices and DN indices.
     *
     * @param a_parentEntry the parent the child is to subordinate to.
     * @param a_childEntry the child to be moved under the parent.
     * @param a_newRdn the new Rdn that is to replace the current Rdn.
     * @param a_deleteOldRdn deletes the old Rdn attribute value pair if true.
     * @throws BackendException when the operation cannot be performed due to a
     * backing store error.
     * @throws NamingException when naming violations and or schema violations
     * occur due to attempting this operation.
     */
    public void move( LdapEntry a_parentEntry, LdapEntry a_childEntry,
	    Name a_newRdn, boolean a_deleteOldRdn )
	    throws BackendException, NamingException
    {
        AtomicBackend l_be = getBackend( a_parentEntry.getNormalizedDN() ) ;
        Name l_childDn = new LdapName( a_childEntry.getEntryDN() ) ;
        String l_oldRdnStr = l_childDn.get( l_childDn.size() - 1 ) ;
        String l_newRdnStr = a_newRdn.get( a_newRdn.size() - 1 ) ;
        
        Name l_normalizedChildDn = a_childEntry.getNormalizedDN() ;
        Name l_normalizedOldParentDn = 
            l_normalizedChildDn.getPrefix( l_normalizedChildDn.size() - 1 ) ;

        // Set common operational attributes on the entry first
        setOperationalAttributes( a_childEntry, false ) ;


        if ( l_normalizedOldParentDn.equals( a_parentEntry.getNormalizedDN() ) ) 
        {
            if ( l_oldRdnStr.equals( l_newRdnStr ) )
            {
                /*
                 * The new and old rdn are the same and the new and old parents
                 * are the same as well so we do nothing.  Both rdn renames and
                 * move operations are pointless so we return.
                 */
                return ;
            }
            
            /*
             * The move operation is unnecessary but the rdn change is valid
             * so we transduce this call to a modifyRdn call on the backend
             */
            l_be.modifyRdn( a_childEntry, a_newRdn, a_deleteOldRdn ) ;
        }
        else 
        {
            if ( l_oldRdnStr.equals( l_newRdnStr ) ) 
            {
                /*
                 * The new and old rdn are the same so we convert this to a 
                 * simple move operation.
                 */
                l_be.move( a_parentEntry, a_childEntry ) ;
            } 
            else 
            {
                // Both the move and the rdn change are necessary here!
                l_be.move( a_parentEntry, a_childEntry, a_newRdn, 
                    a_deleteOldRdn ) ;
            }
        }
    }


    /**
     * Moves a child entry without changing the RDN under a new parent entry.  
     * This effects the parent child relationship between the parent entry and 
     * the child entry.  The index for the child mapping it to the current 
     * parent is destroyed and a new index mapping it to the new parent is 
     * created.  As a side effect the name of the child entry and all its 
     * descendants will reflect the move within the DIT to a new parent.  The 
     * old parent prefix to the distinguished names of the child and its 
     * descendents will be replaced by the new parent DN prefix.
     * 
     * If the new parent is the same as the old parent this call does nothing
     * since the operation is pointless.
     *
     * @param a_parentEntry the parent the child is to subordinate to.
     * @param a_childEntry the child to be moved under the parent.
     * @throws BackendException when the operation cannot be performed due to a
     * backing store error.
     * @throws NamingException when naming violations and or schema violations
     * occur due to attempting this operation.
     */
    public void move( LdapEntry a_parentEntry, LdapEntry a_childEntry )
	    throws BackendException, NamingException
    {
        AtomicBackend l_be = getBackend( a_parentEntry.getNormalizedDN() ) ;
        Name l_dn = a_childEntry.getNormalizedDN() ;
        Name l_oldParentDn = l_dn.getPrefix( l_dn.size() - 1 ) ;
        
        // Fires ModifyProtocolEvent
        setOperationalAttributes( a_childEntry, false ) ;
        
        // Only perform operation if new parent dn is different from the old 
        if ( ! a_parentEntry.getNormalizedDN().equals( l_oldParentDn ) )
        {
            l_be.move( a_parentEntry, a_childEntry ) ;
        }
    }


    //////////////////////////////////////////////
    // UnifiedBackend Interface Implementations //
    //////////////////////////////////////////////


    /**
     * Gets the suffix of the backend responsible for an entry using the entry's
     * distinguished name.
     *
     * @phoenix:mx-operation
     * @phoenix:mx-description Gets the suffix of the backend storing an entry
     * by its DN.
     */
    public String getBackendSuffixForEntry(String a_dn)
        throws InvalidNameException, NameNotFoundException
    {
        StringBuffer l_buf = new StringBuffer() ;

        l_buf.append("Repsonsible backend module suffix for entry ") ;
        l_buf.append(a_dn).append(':').append("\n") ;
	    l_buf.append("=================================") ;
	    l_buf.append("=================================\n") ;
	    l_buf.append("\n\n") ;

        try {
            Name l_dn = getNormalizedName (a_dn) ;
            BackendModule l_be = (BackendModule) getBackend(l_dn) ;
            l_buf.append(l_be.getImplementationName()).append("\n") ;
            l_buf.append(l_be.getSuffix()).append("\n") ;
        } catch(Throwable t) {
            l_buf.append(ExceptionUtil.printStackTrace(t)) ;
        }

        return l_buf.toString() ;
    }


    /**
     * Gets the most significant Dn that exists within the server and hence can
     * be matched to an actual entry.
     *
     * @param a_dn to use for the matching test.
     * @return the matching portion of a_dn, or the valid empty string dn if no
     * match was found.
     */
    public Name getMatchedDn( Name a_dn )
        throws NamingException, BackendException
    {
		LdapName l_suffix ;

        // Don't try to match for the empty dn which matches by default if we
        // try to match it in hasEntry then we will get a failure to find an
        // atomic backend for it.
        if( a_dn.size() == 0 )
        {
            return a_dn ;
        }
        // Return if a_dn itself is matched/exists.
        else if( hasEntry( a_dn ) )
        {
            return a_dn ;
        }

        // Otherwise we start removing most significant components from the head
        // of it looking for a suffix dn that matches until the "" dn results
        l_suffix = ( LdapName ) a_dn.clone() ;
		while( l_suffix.size() > 0 )
        {
			l_suffix.remove( l_suffix.size() - 1 ) ;

            // Short loop on a match
            if( hasEntry( l_suffix ) )
            {
                return l_suffix ;
            }
		}

        // Should be the empty string distinguished name
        return l_suffix ;
    }


    /**
     * Gets the backend associated with a distinguished name if the name
     * resolves to a suffix.
     *
     * @param a_dn the distinguished name to resolve to a backend
     */
    public AtomicBackend getBackend( Name a_dn )
        throws NamingException
    {
        Name l_suffix = null ;
        Iterator l_list = null ;
        AtomicBackend l_backend = null ;

        if( ! hasStarted() )
        {
            throw new IllegalStateException( "Module has not started!" ) ;
        }

        if( getLogger().isDebugEnabled() )
        {
            getLogger().debug("" //ProtocolModule.getMessageKey()
                + " - NexusModule.getBackend(): Request for DN " + a_dn) ;
        }

		// First lets just see if l_dn is a suffix to begin with using a cheap
        // lookup in the normalized suffix String to backend Hash map.
	    if( m_backends.containsKey( a_dn.toString() ) )
        {
            BackendModule l_be =
                ( BackendModule ) m_backends.get( a_dn.toString() ) ;

            if( getLogger().isDebugEnabled() )
            {
                getLogger().debug("" //ProtocolModule.getMessageKey()
                    + " - NexusModule.getBackend(): Request for DN " + a_dn
                    + " returning " + l_be.getImplementationName()
                    + " with suffix " + l_be.getSuffix() ) ;
            }

            return l_be ;
        }

        // So l_dn
		l_list = listBackends() ;
        while( l_list.hasNext() )
        {
	        l_backend = ( AtomicBackend ) l_list.next() ;
            l_suffix = l_backend.getSuffix() ;
            if( a_dn.startsWith( l_suffix ) )
            {
                if( getLogger().isDebugEnabled() )
                {
                    getLogger().debug( "" //ProtocolModule.getMessageKey()
                        + " - NexusModule.getBackend(): returning backend "
                        + l_suffix ) ;
                }
                return l_backend ;
            }
        }

        // After this point we are screwed!
        if( getLogger().isDebugEnabled() )
        {
            getLogger().debug( "Valid name " + a_dn + " not found under nexus "
                + "backends.  Throwing IllegalArgumentException!" ) ;
        }

        throw new IllegalArgumentException( a_dn + " not a valid namespace - "
            + "could not find matching backend!" ) ;
    }


    public NameParser getNameParser()
    {
        return m_schema.getNameParser() ;
    }


    public NameParser getNormalizingParser()
    {
        return m_schema.getNormalizingParser() ;
    }


    public NameParser getNormalizingParser(Name a_name)
        throws NamingException
    {
        Schema l_schema = this.getSchema(a_name) ;
        if(l_schema != null) {
            return l_schema.getNormalizingParser() ;
        }

        return null ;
    }


    /**
     * JMX intended operation to list registered running backends this nexus
     * currently has attacked to it.
     * 
     * @phoenix:mx-operation
     * @phoenix:mx-description Lists all atomic backends registered with the
     * nexus (or system unified backend).
     */
    public String getRegisteredBackends()
    {
		StringBuffer l_buf = new StringBuffer() ;

		l_buf.append("Nexus attached backend modules:").append("\n") ;
		l_buf.append("=================================") ;
		l_buf.append("=================================\n") ;
		l_buf.append("\n\n") ;

		Iterator l_list = listBackends() ;
       	while(l_list.hasNext()) {
        	BackendModule l_backend = (BackendModule) l_list.next() ;

            l_buf.append("Implementation Name:\t") ;
            l_buf.append(l_backend.getImplementationName()).append("\n") ;

            l_buf.append("Implementation Class Name:\t") ;
            l_buf.append(l_backend.getImplementationClassName()).append("\n") ;

            l_buf.append("Suffix Serviced:\t") ;
            l_buf.append(l_backend.getSuffix()).append("\n\n\n") ;
       }

       return l_buf.toString() ;
    }


    public Iterator listBackends()
    {
        return m_backends.values().iterator() ;
    }


    public Iterator listSuffixes()
    {
        return m_backends.keySet().iterator() ;
    }


    public void register( AtomicBackend a_backend )
    {
        // Dynamically register client manager with backends added on fly.
        a_backend.registerClientManager( m_clientMan ) ;
        m_backends.put( a_backend.getSuffix().toString(), a_backend ) ;
        m_adminUsers.put( a_backend.getAdminUserDN().toString(), a_backend ) ;
    }


    public void unregister(AtomicBackend a_backend)
    {
        m_backends.remove(a_backend.getSuffix().toString()) ;
        m_adminUsers.remove(a_backend.getAdminUserDN().toString()) ;
    }


    ///////////////////////////////////////
    // Life-cycle Method Implementations //
    ///////////////////////////////////////


    /**
     * We use this to get a handle on the schema manager.
     * 
     * @phoenix:dependency name="org.apache.eve.schema.SchemaManager"
     * @param a_manager the service manager that we get a BackendManager from
     */
    public void service(ServiceManager a_manager)
        throws ServiceException
    {
        m_schemaMan =
            (SchemaManager) a_manager.lookup(SchemaManager.ROLE) ;
        m_schema = m_schemaMan.getCompleteSchema() ;
    }


    public void configure(Configuration a_config)
        throws ConfigurationException
    {
        m_rootDSE = new RootDSE(m_schema) ;

        try {
			Configuration l_dseConfig = a_config.getChild(ROOTDSE_TAG, false) ;
			if(l_dseConfig == null) {
				String l_errmsg = "The " + ROOTDSE_TAG + " does not exist in "
					+ "the configuration section for the NexusModule. Cannot "
					+ "continue configuration without it!" ;
				throw new ConfigurationException(l_errmsg) ;
			}
	
			Configuration [] l_attributes = l_dseConfig.getChildren(ATTRIBUTE_TAG) ;
			for(int ii = 0 ; ii < l_attributes.length ; ii++ ) {
				String l_name = l_attributes[ii].getAttribute("name", null) ;
				String l_value = l_attributes[ii].getAttribute("value", null) ;
				m_rootDSE.addValue(l_name, l_value) ;
			}
        } catch(Throwable e) {
            String l_errmsg = "Encountered error while trying to construct the"
                + " RootDSE during the NexusModule's configuration stage." ;
            throw new ConfigurationException(l_errmsg, e) ;
        }
    }


    public String getImplementationRole()
    {
        return ROLE ;
    }


    public String getImplementationName()
    {
        return "Unified Backend Nexus" ;
    }


    public String getImplementationClassName()
    {
        return this.getClass().getName() ;
    }


    public void registerClientManager(ClientManager a_manager)
    {
        m_clientMan = a_manager ;
    }


    public Schema getSchema(Name a_dn)
        throws NamingException
    {
        BackendModule l_be = (BackendModule) getBackend(a_dn) ;
		return l_be.getSchema() ;
    }
}
