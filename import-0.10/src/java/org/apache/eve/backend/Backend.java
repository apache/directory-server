/*
 * $Id: Backend.java,v 1.9 2003/03/27 16:30:37 jmachols Exp $
 *
 * -- (c) LDAPd Group                                                    --
 * -- Please refer to the LICENSE.txt file in the root directory of      --
 * -- any LDAPd project for copyright and distribution information.      --
 *
 */

package org.apache.eve.backend ;


import javax.naming.Name ;
import javax.naming.NamingException ;
import javax.naming.directory.DirContext ;
import javax.naming.directory.SearchControls ;

import org.apache.ldap.common.filter.ExprNode ;
import org.apache.eve.client.ClientManagerSlave ;


/**
 * Backend modules store entries in a manner specific to a backing store.
 * 
 * Backends may be memory resident non persistant stores, use flat files, xml,
 * relational databases, non relational databases, or even other surrogate LDAP
 * directories to store entries in whatever scheme they see fit. No limitations
 * are placed on how Backends store or manage entries.
 *
 * These interfaces simply define a means to get to Entry attributes in a
 * standard way.  Hence this interface establishes one of the primary contracts
 * that the ldap server holds with a pluggable backend module.  Other
 * complementary interfaces in this package are artifacts that are required to
 * interface with a Backend or the attribute contents of entries.  Backend
 * implementations must uphold these server-backend contracts.
 *
 * Related Documentation:
 * <ul>
 * <li><a href=backendhowto.html>HowTo Build A Backend</a></li>
 * <li><a href=dnnormalization.html>Distinguished Name Normalization</a></li>
 * <li><a href=contract.html>Server-Backend Contract.</a></li>
 * <li><a href=http://afs.wu-wien.ac.at/manuals/rfc-ldap.html>
 * Relevant LDAP RFCs</a></li></ul>
 *
 * @author <a href="mailto:aok123@bellsouth.net">Alex Karasulu</a>
 * @author $Author: jmachols $
 * @version $Revision: 1.9 $
 */
public interface Backend extends ClientManagerSlave
{
    /**
     * Constant used to represent object level search scope which equals
     * SearchControls.OBJECT_SCOPE.
     */
    int BASE_SCOPE = SearchControls.OBJECT_SCOPE ;
    /**
     * Constant used to represent single level search scope which equals
     * SearchControls.ONELEVEL_SCOPE.
     */
    int SINGLE_SCOPE = SearchControls.ONELEVEL_SCOPE ;
    /**
     * Constant used to represent subtree level search scope which equals
     * SearchControls.SUBTREE_SCOPE.
     */
    int SUBTREE_SCOPE = SearchControls.SUBTREE_SCOPE ;

    /** Constant used to represent attribute/value addition. */
    int ADD_ATTRIBUTE = DirContext.ADD_ATTRIBUTE ;
    /** Constant used to represent attribute/value removal. */
    int REMOVE_ATTRIBUTE = DirContext.REMOVE_ATTRIBUTE ;
    /** Constant used to represent attribute/value replacement. */
    int REPLACE_ATTRIBUTE = DirContext.REPLACE_ATTRIBUTE ;


    ///////////////////////////
    // Entry CRUD Operations //
    ///////////////////////////


    /**
     * Creates a new Entry without adding it to the database.  The entry is
     * invalid until it is added to the instantiating backend via a create()
     * call. The new Entry contains a single operational attribute: the DN of
     * the Entry in the case and format specified by the user.  Hence calls to
     * the <code>getEntryDN()</code> method of the Entry shall return the DN
     * argument to this method.  Calls to the <code>getNormalizedDN()</code>
     * method will return the normalized DN.
     * 
     * To comply with the server backend contract the DN argument must not be
     * tampered with by a normalizer.  This method expects a_dn to be the
     * original user provided unformatted DN.
     *
     * @param a_dn the user provided distinguished name of the new Entry.
     * @return the newly instantiated Entry to be added to this Backend via the
     * create() method.
     * @throws javax.naming.NameNotFoundException when a component of the parent cannot be
     * resolved because it is not bound - the parent entry of the new Entry
     * must be bound for this factory method.
     * @throws javax.naming.InvalidNameException when a_dn is not syntactically correct.
     * @throws javax.naming.NameAlreadyBoundException when an Entry with a_dn already
     * exists.
     * @throws BackendException when a backing store error occurs.
     */
    LdapEntry newEntry(String a_dn)
        throws BackendException, NamingException ;

    /**
     * Deletes (cru<b>D</b>) an entry from the Backend and all cached
     * referrences to it.  The Entry cache if any is purged of referrences to
     * the Entry.
     *
     * @param an_entry Entry to be deleted
     * @throws BackendException when removal from the backend fails due to a
     * backing store error, or an_entry is <b>NOT</b> valid, or is not managed
     * by this Backend.
     * @throws javax.naming.ContextNotEmptyException when non leaf entries are attempted to
     * be deleted.
     */
    void delete(LdapEntry an_entry)
        throws BackendException, NamingException ;

    /**
     * Creates (<b>C</b>rud) an Entry in this Backend.
     *
     * @param an_entry to put into this Backend.
     * @throws BackendException if anything goes drastically wrong with this
     * Backend while attempting to perform the operation, or if an Entry with
     * the same DN already exists within this Backend, or if the Entry is not
     * managed by this Backend.
     * @throws javax.naming.directory.SchemaViolationException if the attributes supplied with an
     * entry do not satisfy objectclass schema constraints.
     * @throws javax.naming.NameAlreadyBoundException if an Entry with a_dn already exists,
     * this would occur if another thread creates an Entry with its DN between
     * the call to newEntry and this method.
     * @throws javax.naming.directory.InvalidAttributesException when all the mandatory attributes
     * required by the objectclasses of the object are not present.
     */
    void create(LdapEntry an_entry)
        throws BackendException, NamingException ;


    /**
     * Read (c<b>R</b>ud) an Entry from this Backend based on it's unique DN.
     *
     * The distinguished name argument is NOT presumed to be normalized in
     * accordance with schema attribute syntax and attribute matching rules.
     * The DN is also NOT presumed to be syntacticly correct or within the
     * namespace of this directory information base.  Unaware of normalization
     * this method will attempt to normalize any DN arguements.  Apriori
     * normalization would be redundant.
     *
     * @param a_dn the unique distinguished name of the entry to get.
     * @return the entry identified by a_dn or null if one by that name does 
     * not exist.
     * @throws BackendException if anything goes drastically wrong with this
     * Backend while attempting to perform the operation.
     * @throws javax.naming.NameNotFoundException when a component of the name cannot be
     * resolved because it is not bound.
     * @throws javax.naming.InvalidNameException if a_dn is not syntactically correct.
     */
    LdapEntry read(Name a_dn)
        throws BackendException, NamingException ;

    /**
     * Updates (cr<b>U</b>d) the valid Entry by presuming that it has been
     * altered.  The alterations may effect multiple attributes by adding
     * values, removing values or changing values.  The Entry must be valid
     * for this operation to take place.
     *
     * @param an_entry Entry to modify
     * @throws BackendException when the modification fails, or an_entry is not
     * valid.
     * @throws javax.naming.directory.SchemaViolationException if the attributes supplied with an
     * entry do not satisfy objectclass schema constraints.
     * @throws javax.naming.directory.InvalidAttributesException when all the mandatory attributes
     * required by the objectclasses of the object are not present.
     */
    void update(LdapEntry an_entry)
        throws BackendException, NamingException ;


    //////////////////////////////////////
    // Parent Child Relation Operations //
    //////////////////////////////////////


    /**
     * Lists the children of an entry identified by a_dn.  The returned Cursor
     * enumerates through the children.<br>
     * <br>
     * The distinguished name argument is NOT presumed to be normalized in
     * accordance with schema attribute syntax and attribute matching rules.
     * The DN is also NOT presumed to be syntacticly correct or within the
     * namespace of this directory information base.  Unaware of normalization
     * this method will attempt to normalize any DN arguements.  Apriori
     * normalization would be redundant.
     *
     * @param a_parentDN the parent entry's distinguished name.
     * @return a cursor enumerating over the child entries of the parent.
     * @throws BackendException if anything goes drastically wrong with this
     * Backend while attempting to perform the operation.
     * @throws javax.naming.NameNotFoundException when a component of the name cannot be
     * resolved because it is not bound.
     * @throws javax.naming.InvalidNameException if a_parentDN is not syntactically correct.
     */
    Cursor listChildren(Name a_parentDN)
        throws BackendException, NamingException ;

    /**
     * Gets the parent of a child entry.
     * 
     * The distinguished name argument is NOT presumed to be normalized in
     * accordance with schema attribute syntax and attribute matching rules.
     * The DN is also NOT presumed to be syntacticly correct or within the
     * namespace of this directory information base.  Unaware of normalization
     * this method will attempt to normalize any DN arguements.  Apriori
     * normalization would be redundant.
     *
     * @param a_childDN the distinguished name of the child entry.
     * @return the parent entry of an entry identified by a child DN or the
     * child entry if the child is the suffix.
     * @throws BackendException if anything goes drastically wrong with this
     * Backend while attempting to perform the operation.
     * @throws javax.naming.NameNotFoundException when a component of the name cannot be
     * resolved because it is not bound.
     * @throws javax.naming.InvalidNameException if a_childDN is not syntactically correct.
     */
    LdapEntry getParent(Name a_childDN)
        throws BackendException, NamingException ;

    /**
     * Checks to see if an entry with a distinguished name exists within this
     * Backend.
     * 
     * The distinguished name argument is NOT presumed to be normalized in
     * accordance with schema attribute syntax and attribute matching rules.
     * The DN is also NOT presumed to be syntacticly correct or within the
     * namespace of this directory information base.  Unaware of normalization
     * this method will attempt to normalize any DN arguements.  Apriori
     * normalization would be redundant.
     *
     * @param a_dn the distinguished name of the entry to check for.
     * @return true if the entry exists within this Backend instance.
     * @throws BackendException if anything goes drastically wrong with this
     * Backend while attempting to perform the operation.
     * @throws javax.naming.InvalidNameException if a_dn is not syntactically correct.
     */
    boolean hasEntry(Name a_dn)
        throws BackendException, NamingException ;


    /**
     * Tests to see if this entry is a/the suffix Entry of this Backend.
     * Unified composite backends will return true if the Entry is a valid
     * suffix within the set of Backends composing it.
     *
     * @param an_entry the Entry to test if it is the suffix of this Backend or one of
     * its composing Backends if this Backend is a UnifiedBackend.
     * @return true if the Entry's DN is equivalent to the DN of this Backend,
     * or one contained by a UnifiedBackend, otherwise false.
     */
    boolean isSuffix(LdapEntry an_entry)
        throws NamingException ;

    /**
     * Checks to see if a user dn is the dn of this backend or one of its
     * AtomicBackends if it is a UnifiedBackend.
     *
     * @param a_userDn the user dn to check for.
     * @return true if a_userDn represents an admin dn, false otherwise.
     * @throws NamingException if their is a problem with the specified user dn.
     */
    boolean isAdminUser(Name a_userDn)
        throws NamingException ;

    ////////////////////////
    // Special Operations //
    ////////////////////////

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
     * @param a_baseDN String representing the base of the search.
     * @param a_scope SearchControls governing how this search is to be
     * conducted.
     * @throws BackendException on Backend errors or when the operation cannot
     * proceed due to a malformed search filter, a non-existant search base, or
     * inconsistant search controls.
     * @throws javax.naming.InvalidNameException if a_baseDN is not syntactically correct.
     * @throws javax.naming.NameNotFoundException when a component of a_baseDN cannot be
     * resolved because it is not bound.
     * @throws javax.naming.directory.InvalidSearchFilterException when the specification of a search
     * filter is invalid. The expression of the filter may be invalid, or there
     * may be a problem with one of the parameters passed to the filter.
     * @throws javax.naming.directory.InvalidSearchControlsException when the specification of the
     * SearchControls for a search operation is invalid. For example, if the
     * scope is set to a value other than OBJECT_SCOPE, ONELEVEL_SCOPE,
     * SUBTREE_SCOPE, this exception is thrown.
     */
    Cursor search(ExprNode a_filter, Name a_baseDN, int a_scope)
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
    void modifyRdn(LdapEntry an_entry, Name a_newRdn, boolean a_deleteOldRdn)
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
    void move(LdapEntry a_parentEntry, LdapEntry a_childEntry)
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
    void move(LdapEntry a_parentEntry, LdapEntry a_childEntry,
	    Name a_newRdn, boolean a_deleteOldRdn)
	    throws BackendException, NamingException ;
}
