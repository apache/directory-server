/*
 * $Id: JdbmDatabase.java,v 1.12 2003/08/06 03:01:25 akarasulu Exp $
 *
 * -- (c) LDAPd Group                                                    --
 * -- Please refer to the LICENSE.txt file in the root directory of      --
 * -- any LDAPd project for copyright and distribution information.      --
 *
 */

package org.apache.eve.backend.jdbm ;


import java.util.Map ;
import java.util.HashMap ;
import java.util.Iterator ;
import java.util.Collection ;

import javax.naming.Name ;
import javax.naming.NameParser ;
import javax.naming.NamingException ;

import java.io.File ;
import java.io.IOException ;

import java.math.BigInteger ;
import java.text.ParseException ;

import org.apache.eve.backend.Cursor ;
import org.apache.eve.schema.Schema ;
import org.apache.eve.schema.Normalizer ;
import org.apache.eve.backend.LdapEntry ;
import org.apache.eve.backend.BackendException ;
import org.apache.eve.backend.jdbm.index.Index ;
import org.apache.eve.backend.jdbm.index.JdbmIndex ;
import org.apache.eve.backend.jdbm.index.IndexRecord ;
import org.apache.eve.backend.jdbm.table.MasterTable ;
import org.apache.eve.backend.jdbm.index.IndexCursor ;

import org.apache.regexp.RE ;
import org.apache.avalon.framework.ExceptionUtil ;
import org.apache.avalon.framework.logger.Logger ;
import org.apache.avalon.framework.logger.AbstractLogEnabled ;

import org.apache.commons.collections.MultiMap ;
import org.apache.commons.collections.MultiHashMap ;

import org.apache.ldap.common.name.DnParser ;
import org.apache.ldap.common.util.NamespaceTools ;

import jdbm.recman.RecordManager ;


/**
 * No caching just coordinated maintance of indices on CRUD operations for
 * LdapEntrys in a BerkeleyDatabase.  This is not a module but a component
 * used by the Berkeley Db Backend Module.
 * @testcase <{TestDatabase}>
 */
public class JdbmDatabase
    extends AbstractLogEnabled
    implements Database
{
	public static boolean debug = true ;

    public Logger log ;
    private final RecordManager m_recMan ;
    private final Name m_suffix ;
    private final String m_wkdir ;
    private final Schema m_schema ;
    private final MasterTable m_master ;
    private final JdbmIndex m_nameIdx ;
    private final JdbmIndex m_existanceIdx ;
    private final JdbmIndex m_hierarchyIdx ;
    private final Map m_indices ;

    private NameParser m_nonNormalizingParser = null ;
    private final NameParser m_parser ;


    public JdbmDatabase(final Schema a_schema,
        final Name a_suffix,
        final String a_wkdirPath)
        throws BackendException, NamingException
    {
        m_schema = a_schema ;
        m_suffix = a_suffix ;
        m_wkdir = a_wkdirPath ;

        m_parser = m_schema.getNormalizingParser() ;
        try {
            m_nonNormalizingParser = new DnParser() ;
        } catch(IOException e) {
            throw new BackendException("Could not initialize the non-" +
                "normalizing DnParser", e) ;
        }

        try {
            m_recMan = new RecordManager(a_wkdirPath
                + File.separator + "master") ;
            m_recMan.disableTransactions() ;
        } catch(IOException e) {
            String l_msg = "Could not initialize RecordManager:\n"
                + ExceptionUtil.printStackTrace(e) ;
            throw new BackendException(l_msg, e) ;
        }

        m_master = new MasterTable(m_recMan) ;
        m_nameIdx = new JdbmIndex(Schema.DN_ATTR, m_schema, m_wkdir) ;
        m_existanceIdx =
            new JdbmIndex(Schema.EXISTANCE_ATTR, m_schema, m_wkdir) ;
        m_hierarchyIdx =
            new JdbmIndex(Schema.HIERARCHY_ATTR, m_schema, m_wkdir) ;
        m_indices = new HashMap() ;
    }


    public Schema getSchema()
    {
        return m_schema ;
    }


    //////////////////////
    // Index Operations //
    //////////////////////


    public void addIndexOn(String an_attribute)
        throws BackendException, NamingException
    {
        an_attribute = an_attribute.toLowerCase() ;
        if(m_schema.isBinary(an_attribute)) {
            throw new BackendException(
                "Indices not allowed on binary attributes!") ;
        }

        Normalizer l_normalizer = m_schema.getNormalizer(an_attribute, true) ;

        if(null == l_normalizer) {
            throw new BackendException("Cannot create index on attribute "
                + an_attribute + " when schema does not have a normalizer for "
                + "attribute " + an_attribute) ;
            
        }

        an_attribute = an_attribute.toLowerCase() ;
        JdbmIndex l_index = new JdbmIndex(an_attribute, m_schema, m_wkdir) ;
        l_index.enableLogging(super.getLogger()) ;
        m_indices.put(an_attribute, l_index) ;
    }


    public boolean hasIndexOn(String an_attribute)
    {
        try {
            Index l_index = getIndex(an_attribute) ;
            return l_index != null ;
        } catch(IndexNotFoundException e) {
            return false ;
        }
    }


    public Index getIndex(String an_attribute)
        throws IndexNotFoundException
    {
        String l_lowerCased = an_attribute.toLowerCase() ;

        if(m_indices.containsKey(an_attribute)) {
            return (Index) m_indices.get(an_attribute) ;
        } else if(m_indices.containsKey(l_lowerCased)) {
            return (Index) m_indices.get(l_lowerCased) ;
        } else if(an_attribute.equals(Schema.DN_ATTR)) {
            return m_nameIdx ;
        } else if(an_attribute.equals(Schema.EXISTANCE_ATTR)) {
            return m_existanceIdx ;
        } else if(an_attribute.equals(Schema.HIERARCHY_ATTR)) {
            return m_hierarchyIdx ;
        } else {
            throw new IndexNotFoundException("An index on attribute " +
                an_attribute + " does not exist!") ;
        }
    }


    public BigInteger getEntryId(String a_dn)
        throws BackendException, NamingException
    {
        return m_nameIdx.getForward(a_dn) ;
    }


    public String getEntryDn(BigInteger a_id)
        throws BackendException
    {
        return (String) m_nameIdx.getReverse(a_id) ;
    }


    public BigInteger getParentId(String a_dn)
        throws BackendException, NamingException
    {
        BigInteger l_childId = m_nameIdx.getForward(a_dn) ;
        return (BigInteger) m_hierarchyIdx.getReverse(l_childId) ;
    }


    public BigInteger getParentId(BigInteger a_childId)
        throws BackendException
    {
        return (BigInteger) m_hierarchyIdx.getReverse(a_childId) ;
    }


    public int count()
        throws BackendException
    {
        return m_master.count() ;
    }


    public int getIndexScanCount(String an_attribute)
        throws BackendException, NamingException
    {
        return getIndex(an_attribute).count() ;
    }


    public int getIndexScanCount(String an_attribute, String a_value)
        throws BackendException, NamingException
    {
        return getIndex(an_attribute).count(a_value) ;
    }


    public int getIndexScanCount(String an_attribute, String a_value,
        boolean isGreaterThan)
        throws BackendException, NamingException
    {
        return getIndex(an_attribute).count(a_value, isGreaterThan) ;
    }


    public boolean assertIndexValue(String an_attribute, Object a_value,
	    BigInteger a_id)
        throws BackendException, NamingException
    {
        return getIndex(an_attribute).hasValue(a_value, a_id) ;
    }


    public boolean assertIndexValue(String an_attribute, Object a_value,
	    BigInteger a_id, boolean isGreaterThan)
        throws BackendException, NamingException
    {
        return getIndex(an_attribute).hasValue(a_value, a_id, isGreaterThan) ;
    }


    public IndexCursor getIndexCursor(String an_attribute)
        throws BackendException, NamingException
    {
	    return getIndex(an_attribute).getCursor() ;
    }


    public IndexCursor getIndexCursor(String an_attribute, String a_value)
        throws BackendException, NamingException
    {
	    return getIndex(an_attribute).getCursor(a_value) ;
    }


    public IndexCursor getIndexCursor(String an_attribute, String a_value,
        boolean isGreaterThan)
        throws BackendException, NamingException
    {
	    return getIndex(an_attribute).getCursor(a_value, isGreaterThan) ;
    }


    public IndexCursor getIndexCursor(String an_attribute, RE a_regex)
        throws BackendException, NamingException
    {
	    return getIndex(an_attribute).getCursor(a_regex) ;
    }


    public IndexCursor getIndexCursor(String an_attribute, RE a_regex,
        String a_prefix)
        throws BackendException, NamingException
    {
	    return getIndex(an_attribute).getCursor(a_regex, a_prefix) ;
    }


    //////////////////////////////////
    // Master Table CRUD Operations //
    //////////////////////////////////


    public void create(LdapEntryImpl an_entry, BigInteger a_id)
        throws BackendException, NamingException
    {
        String l_ldif = m_schema.getLdifComposer().compose(an_entry) ;
        m_master.put(l_ldif, a_id) ;
        addIndices(an_entry) ;
        an_entry.validate() ;
    }


    public LdapEntryImpl read(BigInteger a_id)
        throws BackendException, NamingException
    {
        LdapEntryImpl l_entry = new LdapEntryImpl(this.m_schema) ;
        l_entry.enableLogging(getLogger()) ;

        String l_ldif = m_master.get(a_id) ;

        if(getLogger().isDebugEnabled()) {
            getLogger().debug("JdbmDatabase.read(BigInteger): Extracted the "
                + "following LDIF from the master table:\n" + l_ldif) ;
        }

        try {
            m_schema.getLdifParser().parse(l_entry, l_ldif) ;
        } catch(ParseException e) {
            throw new BackendException("Database may be corrupt.  Cannot "
                + "parse ldif entries in backing store.", e) ;
        }

        if(getLogger().isDebugEnabled()) {
            getLogger().debug("JdbmDatabase.read(BigInteger): Resusitated the "
                + "following entry using the ldif parser:\n" + l_entry) ;
        }

        l_entry.validate() ;
        return l_entry ;
    }


    public void update(LdapEntryImpl an_entry)
        throws BackendException, NamingException
    {
        String l_ldif = m_schema.getLdifComposer().compose(an_entry) ;
        m_master.put(l_ldif, an_entry.getEntryID()) ;
        updateIndices(an_entry) ;
        an_entry.validate() ;
    }


    public void delete(BigInteger a_id)
        throws BackendException, NamingException
    {
        if(getLogger().isDebugEnabled()) {
            getLogger().debug("JdbmDatabase.delete(BigInteger): "
                + "Deleting entry with id " + a_id) ;
        }

        LdapEntryImpl l_entry = read(a_id) ;

        if(getLogger().isDebugEnabled()) {
            getLogger().debug("JdbmDatabase.delete(BigInteger): "
                + "Resusitated entry for deletion " + l_entry) ;
        }

        dropIndices(l_entry) ;
        m_master.del(a_id) ;
    }


    /////////////////////////////
    // Parent/Child Operations //
    /////////////////////////////


	public Cursor getChildren(BigInteger a_id)
        throws BackendException, NamingException
    {
        return m_hierarchyIdx.getCursor(a_id) ;
    }


	public int getChildCount(BigInteger a_id)
        throws BackendException, NamingException
    {
        return this.m_hierarchyIdx.count(a_id) ;
    }


	public Name getSuffix()
    {
        return m_suffix ;
    }


    public LdapEntryImpl getSuffixEntry()
        throws BackendException, NamingException
    {
        return read(getEntryId(m_suffix.toString())) ;
    }


    public BigInteger getNextId()
        throws BackendException
    {
        return m_master.getNextId() ;
    }


    public BigInteger getCurrentId()
        throws BackendException
    {
        return m_master.getCurrentId() ;
    }


    public void sync()
    {
        Iterator l_list = m_indices.values().iterator() ;
        while(l_list.hasNext()) {
            Index l_index = (Index) l_list.next() ;

            try {
                l_index.sync() ;
            } catch(Throwable t) {
                super.getLogger().error("" //ProtocolModule.getMessageKey()
                    + " - Database.sync(): Failed to sync index on attribute "
                    + l_index.getAttribute()
                    + " - index data may be lost.", t) ;
            }
        }

        try {
            this.m_existanceIdx.sync() ;
        } catch(Throwable t) {
	        getLogger().error("Failed to sync the existance table data may be "
                + "lost.", t) ;
        }

        try {
            this.m_hierarchyIdx.sync() ;
        } catch(Throwable t) {
	        getLogger().error("Failed to sync the hierarchy table data may be "
                + "lost.", t) ;
        }

        try {
            this.m_nameIdx.sync() ;
        } catch(Throwable t) {
	        getLogger().error("Failed to sync the distinguished name table "
                + "data may be lost.", t) ;
        }

        try {
            m_master.sync() ;
        } catch(Throwable t) {
	        getLogger().error("Failed to sync the master table data may be "
                + "lost.", t) ;
        }
    }


    public void close()
    {
        Iterator l_list = m_indices.values().iterator() ;
        while(l_list.hasNext()) {
            Index l_index = (Index) l_list.next() ;

            try {
               l_index.close() ;
            } catch(Throwable t) {
                getLogger().error("Failed to close index on attribute "
                    + l_index.getAttribute()
                    + " - index data may be lost.", t) ;
            }
        }

        try {
            this.m_existanceIdx.close() ;
        } catch(Throwable t) {
	        getLogger().error("Failed to close the existance table data may be "
                + "lost.", t) ;
        }

        try {
            this.m_hierarchyIdx.close() ;
        } catch(Throwable t) {
	        getLogger().error("Failed to close the hierarchy table data may be "
                + "lost.", t) ;
        }

        try {
            this.m_nameIdx.close() ;
        } catch(Throwable t) {
	        getLogger().error("Failed to close the distinguished name table "
                + "data may be lost.", t) ;
        }

        try {
            m_master.close() ;
        } catch(Throwable t) {
	        getLogger().error("Failed to close the master table data may be "
                + "lost.", t) ;
        }

        try {
            m_recMan.close() ;
        } catch(Throwable t) {
	        getLogger().error("Failed to close the database environment.", t) ;
        }
    }


    public void enableLogging(Logger a_logger)
    {
        super.enableLogging(a_logger) ;
        log = a_logger ;
	    m_nameIdx.enableLogging(a_logger) ;
        m_existanceIdx.enableLogging(a_logger) ;
        m_hierarchyIdx.enableLogging(a_logger) ;
        m_master.enableLogging(a_logger) ;
    }


    /////////////////////
    // Utility Methods //
    /////////////////////


    public void setProperty(String a_propertyName, String a_propertyValue)
        throws BackendException
    {
        m_master.setProperty(a_propertyName, a_propertyValue) ;
    }


    public String getProperty(String a_propertyName)
        throws BackendException
    {
        return this.m_master.getProperty(a_propertyName) ;
    }


    /**
     * Lists only the User Defined Index (UDI) Attributes.
     */
    public Iterator getUDIAttributes()
    {
        return m_indices.keySet().iterator() ;
    }


    public final static String [] SYS_INDICES =
    { Schema.DN_ATTR, Schema.EXISTANCE_ATTR, Schema.HIERARCHY_ATTR} ;

    /**
     * Gets the names of the maditory system indices used by the database.
     * These names are used as the [operational] 'attribute' or key names
     * of the indices.  All cursor operations and assertion operations use
     * these names to select the appropriate UDI (User Defined Index) or SDI
     * (System Defined Index) to operate upon.
     */
	public String [] getSystemIndices()
    {
		return SYS_INDICES ;
    }


    public MultiMap getIndices(BigInteger a_id)
        throws BackendException, NamingException
    {
        MultiHashMap l_map = new MultiHashMap() ;

        // Get the distinguishedName to id mapping
        l_map.put("distinguishedName", m_nameIdx.getReverse(a_id)) ;

        // Get all standard index attribute to value mappings
        Iterator l_indices = m_indices.values().iterator() ;
        while(l_indices.hasNext()) {
            Index l_index = (Index) l_indices.next() ;
            Cursor l_cursor = l_index.getReverseCursor(a_id) ;
            while(l_cursor.hasMore()) {
                IndexRecord l_rec = (IndexRecord) l_cursor.next() ;
                Object l_val = l_rec.getIndexKey() ;
                l_map.put(l_index.getAttribute(), l_val) ;
            }
        }

        // Get all existance mappings for this id creating a special key
        // that looks like so 'existance[attribute]' and the value is set to id
        Cursor l_cursor = m_existanceIdx.getReverseCursor(a_id) ;
        StringBuffer l_val = new StringBuffer() ;
        while(l_cursor.hasMore()) {
            IndexRecord l_rec = (IndexRecord) l_cursor.next() ;
            l_val.append("existance[").append(l_rec.getIndexKey()).append(']') ;
            l_map.put(l_val.toString(), l_rec.getEntryId()) ;
            l_val.setLength(0) ;
        }

        // Get all parent child mappings for this entry as the parent using the
        // key 'child' with many entries following it.
        l_cursor = this.m_hierarchyIdx.getCursor(a_id) ;
        while(l_cursor.hasMore()) {
            IndexRecord l_rec = (IndexRecord) l_cursor.next() ;
            l_map.put("child", l_rec.getEntryId()) ;
        }

        return l_map ;
    }


    private void addIndices(LdapEntry an_entry)
        throws BackendException, NamingException
    {
        BigInteger l_id = ((LdapEntryImpl) an_entry).getEntryID() ;
        Iterator l_list = an_entry.attributes().iterator() ;

        m_nameIdx.add(an_entry.getNormalizedDN().toString(), l_id) ;

        if(getLogger().isDebugEnabled() && debug) {
			getLogger().debug("Adding name index ("
                + an_entry.getNormalizedDN().toString()
                + ", " + l_id.toString() + ")") ;
        }

        m_hierarchyIdx.add(((LdapEntryImpl) an_entry).getParentID(), l_id) ;

        while(l_list.hasNext()) {
            String l_attribute = (String) l_list.next() ;
            String l_tolower = l_attribute.toLowerCase() ;

            if(m_indices.containsKey(l_tolower)) {
	            Index l_index = (Index) m_indices.get(l_tolower) ;
	            Iterator l_values = // Get values using correct case.
                    an_entry.getMultiValue(l_attribute).iterator() ;
                while(l_values.hasNext()) {
                    l_index.add((String) l_values.next(), l_id) ;
                }

                // Existance index will never be hit since 'existance' is not an
	            // attribute based index in the formal sense. Nor are the name
                // and hierarchy indices.
	            m_existanceIdx.add(l_tolower, l_id) ;
            }
        }
    }


    private void dropIndices(LdapEntry an_entry)
        throws BackendException, NamingException
    {
        if(getLogger().isDebugEnabled()) {
            getLogger().debug("JdbmModule.dropIndices(): deleting "
                + "indices on entry\n" + an_entry) ;
        }

        BigInteger l_id = ((LdapEntryImpl) an_entry).getEntryID() ;
        Iterator l_list = an_entry.attributes().iterator() ;

        m_nameIdx.drop(an_entry.getNormalizedDN().toString(), l_id) ;
        m_hierarchyIdx.drop(
            ((LdapEntryImpl) an_entry).getParentID(), l_id) ;

        while(l_list.hasNext()) {
            String l_attribute = ((String) l_list.next()) ;
            String l_tolower = l_attribute.toLowerCase() ;

            if(m_indices.containsKey(l_tolower)) {
	            Index l_index = (Index) m_indices.get(l_tolower) ;
	            Iterator l_values = // Get values using correct case.
                    an_entry.getMultiValue(l_attribute).iterator() ;
                while(l_values.hasNext()) {
                    l_index.drop((String) l_values.next(), l_id) ;
                }

                // Existance index will never be hit since 'existance' is not an
	            // attribute based index in the formal sense. Nor are the name
                // and hierarchy indices.
	            m_existanceIdx.drop(l_tolower, l_id) ;
            }
        }
    }


    private void updateIndices( LdapEntryImpl an_entry )
        throws BackendException, NamingException
    {
        if ( getLogger().isDebugEnabled() ) 
        {
            getLogger().debug( "" //ProtocolModule.getMessageKey()
                + " - Database.updateIndices() - dumping delta hashes "
                + "after removing the old dn and adding the new Dn" ) ;
            getLogger().debug( "Added hash:\n" + an_entry.getAdded() ) ;
            getLogger().debug( "Removed hash:\n" + an_entry.getRemoved() ) ;
        }

	    BigInteger l_id = an_entry.getEntryID() ;

        //
        // Remove indices for all attributes removed.
        //

        Iterator l_removedAttribs = an_entry.getRemoved().keySet().iterator() ;
	    
        while ( l_removedAttribs.hasNext() ) 
        {
	        String l_removedAttrib = ( String ) l_removedAttribs.next() ;
	        String l_tolower = l_removedAttrib.toLowerCase() ;

            if ( m_indices.containsKey( l_tolower ) ) 
            {
                Index l_index = ( Index ) m_indices.get( l_tolower ) ;

                // Remove indices for all the removed values.
	            Iterator l_values = ( ( Collection )
	                an_entry.getRemoved().get( l_removedAttrib ) ).iterator() ;
	            while ( l_values.hasNext() ) 
                {
                    Object l_val = l_values.next() ;
                    if ( getLogger().isDebugEnabled() ) 
                    {
                        getLogger().debug( "" //ProtocolModule.getMessageKey()
                            + " - Database.updateIndices() - removing index "
                            + l_tolower + " with value " + l_val ) ;
                    }
	                l_index.drop( l_val, l_id ) ;
	            }

                // Existance table must be handled specifically since it is not
	            // an [operational] attribute and is not caught by the loop
                // above. We need to make sure that we only remove an existance
	            // index if all of the values for that attribute have been
                // dropped.  We also need to watch out for stale keys to empty
	            // collections in the multimap.
	            if ( ! an_entry.containsKey( l_removedAttrib ) ) 
                {
	                m_existanceIdx.drop( l_tolower, l_id ) ;
	            }
	            else if ( an_entry.containsKey( l_removedAttrib ) &&
	               an_entry.getMultiValue( l_removedAttrib ).size() == 0 )
	            {
	                m_existanceIdx.drop( l_tolower, l_id ) ;
	            }
            }
            else if ( l_tolower.equals( "parentid" ) )
            {
                Iterator l_list = ( ( Collection ) 
                    an_entry.getRemoved().get( l_removedAttrib) ).iterator() ;
                    
                while( l_list.hasNext() )
                {
                    BigInteger l_oldParentId = null ;
                    Object l_obj = l_list.next() ;
                    
                    if ( l_obj instanceof String )
                    {
                        l_oldParentId = new BigInteger( ( String ) l_obj ) ; 
                    }
                    else if ( l_obj instanceof BigInteger )
                    {
                        l_oldParentId = ( BigInteger ) l_obj ;
                    }
                    
                        
                    m_hierarchyIdx.drop( l_oldParentId, l_id ) ;
                    
                    if( getLogger().isDebugEnabled() )
                    {
                        getLogger().debug( "removed old parentid '" 
                            + l_oldParentId + "' from hierarchy index" ) ;
                    }
                }
            } // Now we need to check for DN attribute changes due to modify ops
            else if( l_tolower.equals( Schema.DN_ATTR ) ) 
            {
                Iterator l_list = ( ( Collection )
                    an_entry.getRemoved().get( l_removedAttrib ) ).iterator() ;

                while( l_list.hasNext() ) 
                {
                    Object l_oldDn = l_list.next() ;
                    m_nameIdx.drop( l_oldDn, l_id ) ;

                    if( getLogger().isDebugEnabled() ) 
                    {
                        getLogger().debug( "removed old Dn '" + l_oldDn 
                            + "' from name index" ) ;
                    }
                }
            } // Close else if
        } // Close while


        //
        // Create indices for all attribute value pairs added.
        //

        Iterator l_addedAttribs = an_entry.getAdded().keySet().iterator() ;
	    while ( l_addedAttribs.hasNext() ) 
        {
	        String l_addedAttrib = ( String ) l_addedAttribs.next() ;
            String l_tolower = l_addedAttrib.toLowerCase() ;

            if ( m_indices.containsKey( l_tolower ) ) 
            {
                Index l_index = ( Index ) m_indices.get( l_tolower ) ;

                // Create indices for all the added values.
	            Iterator l_values = ( ( Collection )
	                an_entry.getAdded().get( l_addedAttrib ) ).iterator() ;
	            while ( l_values.hasNext() )  
                {
                    Object l_val = l_values.next() ;
                    l_index.add( l_val, l_id ) ;
                    
                    if( getLogger().isDebugEnabled() ) 
                    {
                        getLogger().debug( "" //ProtocolModule.getMessageKey()
                            + " - Database.updateIndices() - added index "
                            + l_tolower + " with value " + l_val ) ;
                    }
	            }

                // Add an index into the existance table
	            m_existanceIdx.add( l_tolower, l_id ) ;
            } // Now we need to check for DN attribute changes due to modify ops
            else if ( l_tolower.equals( "parentid" ) )
            {
                Iterator l_list = ( ( Collection )
                    an_entry.getAdded().get( l_addedAttrib ) ).iterator() ;

                while ( l_list.hasNext() ) 
                {
                    BigInteger l_newParentId = null ;
                    Object l_obj = l_list.next() ;
                    
                    if ( l_obj instanceof String )
                    {
                        l_newParentId = new BigInteger( ( String ) l_obj ) ; 
                    }
                    else if ( l_obj instanceof BigInteger )
                    {
                        l_newParentId = ( BigInteger ) l_obj ;
                    }
                    
                    m_hierarchyIdx.add( l_newParentId, l_id ) ;

                    if ( getLogger().isDebugEnabled() ) 
                    {
                        getLogger().debug( "added new Dn '" + l_newParentId 
                            + "' to hierarchy index" ) ;
                    }
                }
            }
            else if ( l_tolower.equals( Schema.DN_ATTR ) ) 
            {
                Iterator l_list = ( ( Collection )
                    an_entry.getAdded().get( l_addedAttrib ) ).iterator() ;

                while ( l_list.hasNext() ) 
                {
                    String l_newDn = ( String ) l_list.next() ;
                    m_nameIdx.add( l_newDn, l_id ) ;

                    if ( getLogger().isDebugEnabled() ) 
                    {
                        getLogger().debug( "added new Dn '"
                            + l_newDn + "' to name index" ) ;
                    }
                }
            } // Close else if
        } // Close while
    } // Close updateIndices()


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
    public void modifyRdn( LdapEntryImpl an_entry,
        String a_newRdn, boolean a_deleteOldRdn )
	    throws BackendException, NamingException
    {
        String l_attribute = NamespaceTools.getRdnAttribute( a_newRdn ) ;
	    String l_value = NamespaceTools.getRdnValue( a_newRdn ) ;

        // This name is not Normalized since we want to preserve the user
        // provided version of the Dn.
	    Name l_name = m_nonNormalizingParser.parse( an_entry.getEntryDN() ) ;

        if ( a_deleteOldRdn ||
            m_schema.isSingleValue( l_attribute.toLowerCase() ) )
        {
	        String l_oldRdn = l_name.get( l_name.size() - 1 ) ;
            String l_oldAttribute = NamespaceTools.getRdnAttribute( l_oldRdn ) ;
            String l_oldValue = NamespaceTools.getRdnValue( l_oldRdn ) ;
            
	        an_entry.removeValue( l_oldAttribute, l_oldValue ) ;
	    }

        l_name.remove( l_name.size() - 1 ) ;
	    l_name.add( l_name.size(), a_newRdn ) ;
	    an_entry.addValue( l_attribute, l_value ) ;

        if( getLogger().isDebugEnabled() ) 
        {
            getLogger().debug( "" //ProtocolModule.getMessageKey()
                + " - Database.modifyRdn() - modifyDn() on entry '" 
                + an_entry.getEntryDN() + "' using new Dn of '" 
                + l_name + "'" ) ;
        }

	    modifyDn( ( LdapEntryImpl ) an_entry, l_name) ;
    }


    /**
     * Recursively modifies the distinguished name of an entry and the names of
     * its descendants calling itself in the recursion.
     *
     * @param an_entry Entry being altered to have a new DN.
     * @param a_dn Distinguished name to set as the new DN
     */
    void modifyDn(LdapEntryImpl an_entry, Name a_dn)
	    throws BackendException, NamingException
    {
        LdapEntryImpl l_child ;
	    Cursor l_children ;
	    Name l_childDN ;

        if ( getLogger().isDebugEnabled() ) 
        {
            getLogger().debug( "" //ProtocolModule.getMessageKey()
                + " - Database.modifyDn() - called on entry '"
                + an_entry.getNormalizedDN() + "' to change it's DN to '"
                + a_dn ) ;
        }
	
        // Package friendly method that sets the user provided dn using
	    // a name.  It automatically updates the normalized dn so it is
	    // synchronized with the change.  setEntryDN must add the 
	    // appropriate hints in the entry so the update can properly modify
	    // indices.  If it seems like too much of a hassle then we can
	    // handle index maintenance here with replication of update function
	    // -ality.
	    an_entry.setEntryDN( a_dn ) ;
	    update( an_entry ) ;
	    l_children = getChildren( an_entry.getEntryID() ) ;
	
        // List children using the DN since the updated parent has had
	    // it's indices updated listChildren will lookup the id by dn
	    // then use the parent child index to lookup all the children.
	    while( l_children.hasMore() ) 
        {
            IndexRecord l_rec = ( IndexRecord ) l_children.next() ;
	        l_child = read( l_rec.getEntryId() ) ;
	        l_childDN = ( Name ) a_dn.clone() ;

            // Add to the copy of the parent the unnormalized rdn of the child
            // Dn to get the new Dn of the child entry - call recursively.
	        l_childDN.add( 0,
                NamespaceTools.getRdn( l_child.getEntryDN() ) ) ;
            modifyDn( l_child, l_childDN ) ;
	    }
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
    public void move(LdapEntryImpl a_parentEntry, LdapEntryImpl a_childEntry,
	    String a_newRdn, boolean a_deleteOldRdn)
	    throws BackendException, NamingException
    {
        // @TODO NOT A VERY EFFICENT OPERATION AT ALL ! ! !  FIX IT ! ! !
	    ( ( LdapEntryImpl ) a_childEntry ).setParent( ( LdapEntryImpl ) 
            a_parentEntry ) ;
        modifyRdn( a_childEntry, a_newRdn, a_deleteOldRdn ) ;
        move( a_parentEntry, a_childEntry ) ;
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
    public void move( LdapEntryImpl a_parentEntry, LdapEntryImpl a_childEntry )
	    throws BackendException, NamingException
    {
        Name l_parentDn =
            m_nonNormalizingParser.parse( a_parentEntry.getEntryDN() ) ;
	    Name l_childDn =
            m_nonNormalizingParser.parse( a_childEntry.getEntryDN() ) ;
	    String l_rdn = l_childDn.get( l_childDn.size() - 1 ) ;
	
        // This call should be package friendly and should automatically
	    // replace the current parent id and dn operational attributes 
	    // with the new parent's respective operational attribute values.
	    ( ( LdapEntryImpl ) a_childEntry ).setParent( 
            ( LdapEntryImpl ) a_parentEntry ) ;
            
	    Name l_newDn = l_parentDn ;
	    l_newDn.add( l_newDn.size(), l_rdn ) ;
	    modifyDn( ( LdapEntryImpl ) a_childEntry, l_newDn ) ;
    }
}

