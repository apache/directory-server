/*
 * $Id: Database.java,v 1.4 2003/03/13 18:27:14 akarasulu Exp $
 *
 * -- (c) LDAPd Group                                                    --
 * -- Please refer to the LICENSE.txt file in the root directory of      --
 * -- any LDAPd project for copyright and distribution information.      --
 *
 */

package org.apache.eve.backend.jdbm ;


import javax.naming.Name ;
import java.util.Iterator ;
import java.math.BigInteger ;
import javax.naming.NamingException ;

import org.apache.eve.schema.Schema ;
import org.apache.eve.backend.BackendException ;

import org.apache.eve.backend.jdbm.index.Index ;
import org.apache.eve.backend.jdbm.index.IndexCursor;
import org.apache.eve.backend.Cursor ;

import org.apache.regexp.RE ;
import org.apache.commons.collections.MultiMap ;
import org.apache.avalon.framework.logger.Logger;


/**
 *
 */
public interface Database
{
    public Schema getSchema() ;

    //////////////////////
    // Index Operations //
    //////////////////////

    public void addIndexOn(String an_attribute)
        throws BackendException, NamingException ;

    public boolean hasIndexOn(String an_attribute) ;

    public Index getIndex(String an_attribute)
        throws IndexNotFoundException ;

    public BigInteger getEntryId(String a_dn)
        throws BackendException, NamingException ;

    public String getEntryDn(BigInteger a_id)
        throws BackendException ;

    public BigInteger getParentId(String a_dn)
        throws BackendException, NamingException ;

    public BigInteger getParentId(BigInteger a_childId)
        throws BackendException ;

    public int count()
        throws BackendException ;

    public int getIndexScanCount(String an_attribute)
        throws BackendException, NamingException ;

    public int getIndexScanCount(String an_attribute, String a_value)
        throws BackendException, NamingException ;

    public int getIndexScanCount(String an_attribute, String a_value,
        boolean isGreaterThan)
        throws BackendException, NamingException ;

    public boolean assertIndexValue(String an_attribute, Object a_value,
	    BigInteger a_id)
        throws BackendException, NamingException ;

    public boolean assertIndexValue(String an_attribute, Object a_value,
	    BigInteger a_id, boolean isGreaterThan)
        throws BackendException, NamingException ;

    public IndexCursor getIndexCursor(String an_attribute)
        throws BackendException, NamingException ;

    public IndexCursor getIndexCursor(String an_attribute, String a_value)
        throws BackendException, NamingException ;

    public IndexCursor getIndexCursor(String an_attribute, String a_value,
        boolean isGreaterThan)
        throws BackendException, NamingException ;

    public IndexCursor getIndexCursor(String an_attribute, RE a_regex)
        throws BackendException, NamingException ;

    public IndexCursor getIndexCursor(String an_attribute, RE a_regex,
        String a_prefix)
        throws BackendException, NamingException ;

    //////////////////////////////////
    // Master Table CRUD Operations //
    //////////////////////////////////

    public void create(LdapEntryImpl an_entry, BigInteger a_id)
        throws BackendException, NamingException ;

    public LdapEntryImpl read(BigInteger a_id)
        throws BackendException, NamingException ;

    public void update(LdapEntryImpl an_entry)
        throws BackendException, NamingException ;

    public void delete(BigInteger a_id)
        throws BackendException, NamingException ;

    /////////////////////////////
    // Parent/Child Operations //
    /////////////////////////////

    public Cursor getChildren(BigInteger a_id)
        throws BackendException, NamingException ;

    public int getChildCount(BigInteger a_id)
        throws BackendException, NamingException ;

    public Name getSuffix() ;

    public LdapEntryImpl getSuffixEntry()
        throws BackendException, NamingException ;

    public BigInteger getNextId()
        throws BackendException ;

    public BigInteger getCurrentId()
        throws BackendException ;

    public void sync() ;

    public void close() ;

    /////////////////////
    // Utility Methods //
    /////////////////////


    public void setProperty(String a_propertyName, String a_propertyValue)
        throws BackendException ;

    public String getProperty(String a_propertyName)
        throws BackendException ;

    /**
     * Lists only the User Defined Index (UDI) Attributes.
     */
    public Iterator getUDIAttributes() ;

    public final static String [] SYS_INDICES =
    { Schema.DN_ATTR, Schema.EXISTANCE_ATTR, Schema.HIERARCHY_ATTR} ;

    /**
     * Gets the names of the maditory system indices used by the database.
     * These names are used as the [operational] 'attribute' or key names
     * of the indices.  All cursor operations and assertion operations use
     * these names to select the appropriate UDI (User Defined Index) or SDI
     * (System Defined Index) to operate upon.
     */
	public String [] getSystemIndices() ;

    public MultiMap getIndices(BigInteger a_id)
        throws BackendException, NamingException ;

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
    public void modifyRdn(org.apache.eve.backend.jdbm.LdapEntryImpl an_entry,
        String a_newRdn, boolean a_deleteOldRdn)
	    throws BackendException, NamingException ;

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
	    throws BackendException, NamingException ;

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
    public void move(LdapEntryImpl a_parentEntry, LdapEntryImpl a_childEntry)
	    throws BackendException, NamingException ;
}
