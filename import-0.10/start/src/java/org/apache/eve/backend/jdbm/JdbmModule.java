/*
 * $Id: JdbmModule.java,v 1.11 2003/08/06 03:01:25 akarasulu Exp $
 *
 * -- (c) LDAPd Group                                                    --
 * -- Please refer to the LICENSE.txt file in the root directory of      --
 * -- any LDAPd project for copyright and distribution information.      --
 *
 */

package org.apache.eve.backend.jdbm ;


import java.math.BigInteger ;

import javax.naming.Name ;
import javax.naming.NamingException ;
import javax.naming.NameNotFoundException ;
import javax.naming.ContextNotEmptyException ;
import javax.naming.NameAlreadyBoundException ;

import org.apache.eve.backend.Cursor ;
import org.apache.ldap.common.filter.ExprNode ;
import org.apache.eve.backend.LdapEntry ;
import org.apache.eve.schema.SchemaManager ;
import org.apache.eve.backend.BackendModule ;
import org.apache.eve.backend.BackendException ;
import org.apache.eve.backend.jdbm.search.SearchEngine ;

import org.apache.avalon.framework.ExceptionUtil ;
import org.apache.avalon.framework.service.ServiceManager ;
import org.apache.avalon.framework.service.ServiceException ;
import org.apache.avalon.framework.configuration.Configuration ;
import org.apache.avalon.framework.configuration.ConfigurationException ;

import org.apache.commons.collections.LRUMap ;
import org.apache.eve.backend.jdbm.gui.BackendFrame ;


/**
 * Jdbm backend module.
 * 
 * @phoenix:block
 * @phoenix:service name="org.apache.eve.backend.AtomicBackend"
 * @phoenix:mx-topic name="jdbm-backend"
 */
public class JdbmModule
    extends BackendModule
{
    private JdbmDatabase m_db = null ;
    private SearchEngine m_searchEngine = null ;

    private Object m_cacheLock = new Object() ;
    private LRUMap m_byIdCache = new LRUMap() ;


    public JdbmDatabase getDatabase()
    {
        return m_db ;
    }


    private void addToCache(org.apache.eve.backend.jdbm.LdapEntryImpl an_entry)
        throws NamingException
    {
        synchronized(m_cacheLock) {
            super.m_cache.put(an_entry.getNormalizedDN().toString(), an_entry) ;
            this.m_byIdCache.put(an_entry.getEntryID(), an_entry) ;
        }
    }


    private void removeFromCache(org.apache.eve.backend.jdbm.LdapEntryImpl an_entry)
        throws NamingException
    {
        synchronized(m_cacheLock) {
            super.m_cache.remove(an_entry.getNormalizedDN().toString()) ;
            this.m_byIdCache.remove(an_entry.getEntryID()) ;
        }
    }


    //////////////////////////////////
    // Backend Crud Implementations //
    //////////////////////////////////


    public void delete( LdapEntry an_entry )
        throws BackendException, NamingException
    {
        LdapEntryImpl l_entry = ( LdapEntryImpl ) an_entry ;

        if( ! l_entry.isValid() )
        {
            throw new BackendException( "Cannot perform delete operation on "
                + "invalid entry: " + l_entry ) ;
        }

        if( m_db.getChildCount( l_entry.getEntryID() ) > 0 )
        {
            // Exception translates to NOTALLOWEDONLEAF LDAPv3 result code (66)
            ContextNotEmptyException e = new ContextNotEmptyException(
                "[66] Cannot delete entry " + l_entry.getEntryDN()
                + " it has children!" ) ;
            e.setRemainingName( l_entry.getNormalizedDN() ) ;
            throw e ;
        }

        BigInteger l_id = l_entry.getEntryID() ;
        m_db.delete( l_id ) ;
        removeFromCache( ( LdapEntryImpl ) an_entry ) ;
    }


    /**
     * Creates a new invalid entry ready to be populated and created within this
     * backend module.
     *
     * @param a_dn the non-normalized user provided distinguished name of the
     * new entry to create.
     * @throws NameAlreadyBoundException if an entry with a_dn already exists
     * within this backend.
     * @throws javax.naming.InvalidNameException if a_dn does not conform to the dn syntax
     * @throws NameNotFoundException if a_dn is not suffix and does not have a
     * parent to be attached to.
     */
    public LdapEntry newEntry( String a_dn )
        throws BackendException, NamingException
    {
        // Will throw InvalidNameException if a_dn is syntactically incorrect.
        Name l_dn = m_schema.getNormalizingParser().parse( a_dn ) ;
        if( m_db.getEntryId( l_dn.toString() ) != null )
        {
            // NameAlreadyBoundExceptions correspond to a NAMEALREADYEXISTS
            // result code with a value of [68] within the LDAPv3 protocol.
	        NameAlreadyBoundException e = new NameAlreadyBoundException(
                "[68] '" + a_dn + "' is already bound." ) ;
            e.setResolvedName( l_dn ) ;
            throw e ;
        }

        //
        // We only check for the existance of a parent entry if the new entry
        // to add is not a suffix.  This is because a suffix entry will not
        // have a parent node.
        //

        if( ! l_dn.equals( getSuffix() ) )
        {
            Name l_parent = l_dn.getSuffix( 1 ) ;
            if( m_db.getEntryId( l_parent.toString() ) == null )
            {
                // NameNotFoundExceptions in JNDI correspond to the NOSUCHOBJECT
                // result code of the LdapResult of the response in LDAPv3
                NameNotFoundException e = new NameNotFoundException(
                    "[32] Parent entry '" + l_parent
					+ "' does not exist.  Cannot create " + l_dn.get(0)
                    + " without parent." ) ;
                throw e ;
            }
        }

        LdapEntryImpl l_entry = new LdapEntryImpl( m_schema, l_dn, a_dn ) ;
        l_entry.enableLogging( getLogger() ) ;
        return l_entry ;
    }


    public LdapEntry read( Name a_dn )
        throws BackendException, NamingException
    {
        if( m_cache.containsKey( a_dn.toString() ) )
        {
            return ( LdapEntry ) m_cache.get( a_dn.toString() ) ;
        }

        BigInteger l_id = m_db.getEntryId( a_dn.toString() ) ;

        if( l_id == null )
        {
	        throw new NameNotFoundException( "[32] '" + a_dn
                + "' does not exist!" ) ;
        }

        LdapEntryImpl l_entry = m_db.read( l_id ) ;
        l_entry.enableLogging( getLogger() ) ;
        addToCache( l_entry ) ;
        return l_entry ;
    }


    LdapEntry read(BigInteger l_id)
        throws BackendException, NamingException
    {
        if(this.m_byIdCache.containsKey(l_id)) {
            return (LdapEntry) m_byIdCache.get(l_id) ;
        }

        LdapEntryImpl l_entry = m_db.read(l_id) ;
        l_entry.enableLogging(getLogger()) ;
        addToCache(l_entry) ;
        return l_entry ;
    }


    /**
     * Updates an entry in the backing store.
     *
     * @param an_entry the modified entry to update.
     */
    public void update(LdapEntry an_entry)
        throws BackendException, NamingException
    {
        removeFromCache((LdapEntryImpl) an_entry) ;
	    m_db.update((LdapEntryImpl) an_entry) ;
        addToCache((LdapEntryImpl) an_entry) ;
    }


    public void create(LdapEntry an_entry)
        throws BackendException, NamingException
    {
        LdapEntryImpl l_entry = (LdapEntryImpl) an_entry ;

        if(l_entry.isValid()) {
            throw new BackendException("Cannot create entries that have "
                + "already been created!") ;
        }

        // Add operational attributes!
        BigInteger l_id = m_db.getNextId() ;
        LdapEntryImpl l_parent = null ;
        BigInteger l_parentId = null ;
        l_entry.put(LdapEntry.ID_ATTR, l_id.toString()) ;

        if(l_entry.getNormalizedDN().equals(m_suffix)) {
            l_entry.put(LdapEntry.PARENTID_ATTR, l_id.toString()) ;
            l_entry.put(LdapEntry.PARENTDN_ATTR, l_entry.getEntryDN()) ;
            l_parent = l_entry ;
            l_parentId = l_id ;
        } else {
            l_parent = (LdapEntryImpl) getParent(l_entry.getNormalizedDN()) ;
            l_parentId = l_parent.getEntryID() ;
            l_entry.put(LdapEntry.PARENTID_ATTR, l_parentId.toString()) ;
            l_entry.put(LdapEntry.PARENTDN_ATTR, l_parent.getEntryDN()) ;
        }

        m_db.create(l_entry, l_id) ;
        addToCache(l_entry) ;
    }


    ///////////////////////////////////////
    // Search and Modify Implementations //
    ///////////////////////////////////////


    /**
     * Searches for candidate entries on the backend starting on a base DN 
     * using a search filter with search controls.
     * 
     * The distinguished name argument is NOT presumed to be normalized in
     * accordance with schema attribute syntax and attribute matching rules.
     * The DN is also NOT presumed to be syntacticly correct or within the
     * namespace of this directory information base.  Unaware of normalization
     * this method will attempt to normalize any DN arguements.  Apriori
     * normalization would be redundant.
     *
     * W O R K   I N   P R O G R E S S
     *
     * @param a_filter String representation of an LDAP search filter.
     * @param a_baseDn String representing the base of the search.
     * @param a_scope SearchControls governing how this search is to be
     * conducted.
     * @throws BackendException on Backend errors or when the operation cannot
     * proceed due to a malformed search filter, a non-existant search base, or
     * inconsistant search controls.
     * @throws javax.naming.InvalidNameException if a_baseDN is not syntactically correct.
     * @throws NameNotFoundException when a component of a_baseDN cannot be
     * resolved because it is not bound.
     * @throws javax.naming.directory.InvalidSearchFilterException when the specification of a search
     * filter is invalid. The expression of the filter may be invalid, or there
     * may be a problem with one of the parameters passed to the filter.
     * @throws javax.naming.directory.InvalidSearchControlsException when the specification of the
     * SearchControls for a search operation is invalid. For example, if the
     * scope is set to a value other than OBJECT_SCOPE, ONELEVEL_SCOPE,
     * SUBTREE_SCOPE, this exception is thrown.
     */
    public Cursor search(ExprNode a_filter, Name a_baseDn, int a_scope)
        throws BackendException, NamingException
    {
        Cursor l_cursor = new EntryCursor(this,
            m_searchEngine.search(m_db, a_filter,
            a_baseDn.toString(), a_scope)) ;
        l_cursor.enableLogging(getLogger()) ;
        return l_cursor ;
    }


    /**
     * Modifies the relative distinguished name (RDN) of an entry
	 * without changing any parent child relationships.  This call
	 * has the side effect of altering the distinguished name of
	 * descendent entries if they exist.  The boolean argument will
	 * optionally remove the existing RDN attribute value pair
	 * replacing it with the new RDN attribute value pair.  If other
	 * RDN attribute value pairs exist besides the current RDN they
	 * will be spared.
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
        m_db.modifyRdn( ( LdapEntryImpl ) an_entry,
            a_newRdn.toString(), a_deleteOldRdn ) ;
    }


    /**
     * Moves a child entry without changing the RDN under a new parent
     * entry.  This effects the parent child relationship between the
     * parent entry and the child entry.  The index for the child
     * mapping it to the current parent is destroyed and a new index
     * mapping it to the new parent is created.  As a side effect the
     * name of the child entry and all its descendants will reflect the
     * move within the DIT to a new parent.  The old parent prefix to
     * the distinguished names of the child and its descendents will be
     * replaced by the new parent DN prefix.
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
        m_db.move( ( LdapEntryImpl ) a_parentEntry, 
            ( LdapEntryImpl ) a_childEntry) ;
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
        m_db.move( ( LdapEntryImpl ) a_parentEntry, 
            ( LdapEntryImpl ) a_childEntry,
            a_newRdn.toString(), a_deleteOldRdn ) ;
    }


    ///////////////////////////////////////
    // Remaining Backend Implementations //
	///////////////////////////////////////


    public LdapEntry getParent(Name a_childDN)
        throws BackendException, NamingException
    {
	    return read(a_childDN.getSuffix(1)) ;
    }


    public boolean hasEntry(Name a_dn)
        throws BackendException, NamingException
    {
        return m_db.getEntryId(a_dn.toString())
            != null ;
    }


    public boolean isSuffix(LdapEntry an_entry)
        throws NamingException 
    {
        return an_entry.getNormalizedDN().equals(m_suffix) ;
    }


    public boolean isAdminUser(Name a_userDn)
        throws NamingException
    {
	    if(a_userDn.equals(m_adminUser)) {
            return true ;
        }

        return false ;
    }


    public Cursor listChildren(Name a_parentDN)
        throws BackendException, NamingException
    {
        BigInteger l_id =
            m_db.getEntryId(a_parentDN.toString()) ;

        if(l_id == null) {
            throw new NameNotFoundException("Cannot list the children of "
                + a_parentDN + ".  It does not exist!") ;
        }

        Cursor l_cursor = new EntryCursor(this, m_db.getChildren(l_id)) ;
        l_cursor.enableLogging(getLogger()) ;
        return l_cursor ;
    }


    public String getProperty(String a_propertyName)
        throws BackendException
    {
	    return m_db.getProperty(a_propertyName) ;
    }


    public void setProperty(String a_propertyName, String a_propertyValue)
        throws BackendException
    {
        m_db.setProperty(a_propertyName, a_propertyValue) ;
    }


    public String getImplementationRole()
    {
        return ROLE ;
    }


    /**
     * @phoenix:mx-attribute
     * @phoenix:mx-description Returns the implementation name.
     * @phoenix:mx-isWriteable no
     */
    public String getImplementationName()
    {
        return("Jdbm DB Implementation") ;
    }


    /**
     * @phoenix:mx-attribute
     * @phoenix:mx-description Returns the implementation class name.
     * @phoenix:mx-isWriteable no
     */
    public String getImplementationClassName()
    {
        return this.getClass().getName() ;
    }


    /**
     * @phoenix:mx-attribute
     * @phoenix:mx-description Returns the suffix of this backend.
     * @phoenix:mx-isWriteable no
     */
    public Name getSuffix()
    {
        return m_db.getSuffix() ;
    }


    /**
     * @phoenix:mx-operation
     * @phoenix:mx-description Synchronizes cached berkeley data with disk.
     */
    public void sync()
        throws BackendException
    {
        m_db.sync() ;
    }


    /**
     * @phoenix:mx-operation
     * @phoenix:mx-description Invokes the admin tool gui.
     */
    public void invokeAdminTool()
        throws Exception
    {
        try {
			BackendFrame l_frame = new BackendFrame() ;
            l_frame.enableLogging(getLogger()) ;
			l_frame.loadDatabase(m_db) ;
            l_frame.launch() ;
        } catch(Exception e) {
            getLogger().error("Got the following exception while trying to "
                + "invoke the admin tool: " + e.getMessage() + "\n\nTrace:\n"
                + ExceptionUtil.printStackTrace(e)) ;
        }
    }


    /////////////////////////////////////////////
    // Configuration Interface Implementations //
    /////////////////////////////////////////////

    public static final String INDICES_TAG = "indices" ;
    public static final String INDEX_TAG = "index" ;
	public static final String INAME_ATTR = "name" ;

    /**
     * Avalon Configurable interface implementation specific to a Berkeley Db.
     * Here's an example configuration with a "cn" and ""dc" index. Notice
     * that the standard backend configuration attributes are not included:<br>
     * 
     *   <config>
     *		<backend>
     * 			<indices>
     * 			<index name="cn"/>
     * 			<index name="dc"/>
     * 			</indices>
     *		</backend>
     *   </config>
     *
     * @param a_config an avalon configuration object for this backend block.
     * @throws ConfigurationException if the configuration is not correct.
     */
    public void configure(Configuration a_config)
        throws ConfigurationException
    {
        super.configure(a_config) ;

        ///////////////////////////////////////////////
        // Configure Backend Database and Components //
        ///////////////////////////////////////////////

        try {
            m_byIdCache.setMaximumSize(m_cache.getMaximumSize()) ;
            m_searchEngine = new SearchEngine() ;
            m_searchEngine.enableLogging(getLogger()) ;
            m_db = new JdbmDatabase (m_schema, m_suffix, m_wkdirPath) ;
	        m_db.enableLogging(getLogger()) ;

            Configuration [] l_indices =
	            a_config.getChild(INDICES_TAG).getChildren(INDEX_TAG) ;
	        String l_attribName = null ;
	        for(int ii = 0; ii < l_indices.length; ii++) {
	            l_attribName = l_indices[ii].getAttribute(INAME_ATTR) ;
                m_db.addIndexOn(l_attribName) ;
	        }
        } catch(NamingException e) {
            throw new ConfigurationException("Could not configure "
                + getImplementationName() + " with suffix " + m_suffix
                + " due to NamingException:\n" + e.getMessage(), e) ;
        } catch(BackendException e) {
            throw new ConfigurationException("Could not configure "
                + getImplementationName() + " with suffix " + m_suffix
                + " due to NamingException:\n" + e.getMessage(), e) ;
        }
    }


    public void stop()
        throws Exception
    {
        super.stop() ;
        m_db.close() ;
    }


    /**
     * @phoenix:dependency name="org.apache.eve.schema.SchemaManager"
     * @phoenix:dependency name="org.apache.eve.backend.UnifiedBackend"
     */
    public void service(ServiceManager a_manager)
        throws ServiceException
    {
        super.service(a_manager) ;
        m_schemaManager =
            (SchemaManager) a_manager.lookup(SchemaManager.ROLE) ;
    }
}
