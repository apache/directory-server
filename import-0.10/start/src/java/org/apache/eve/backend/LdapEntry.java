/*
 * $Id: LdapEntry.java,v 1.8 2003/03/13 18:26:55 akarasulu Exp $
 *
 * -- (c) LDAPd Group                                                    --
 * -- Please refer to the LICENSE.txt file in the root directory of      --
 * -- any LDAPd project for copyright and distribution information.      --
 *
 */

package org.apache.eve.backend ;

import java.math.BigInteger ;
import java.io.Serializable ;

import java.util.Date ;
import java.util.Collection ;

import javax.naming.Name ;
import javax.naming.directory.SchemaViolationException ;
import javax.naming.directory.AttributeModificationException ;
import javax.naming.directory.InvalidAttributeValueException ;
import javax.naming.directory.InvalidAttributeIdentifierException ;
import javax.naming.directory.AttributeInUseException ;
import java.util.Iterator;
import org.apache.eve.schema.Schema;
import javax.naming.NamingException;


/**
 * Internal server side representation of an LDAP entry.  All backend entry
 * implementations are expected to implement this interface.  The interface
 * defines standard house keeping attributes used by this LDAP v3
 * implementation.
 *
 * @task We need to explicitly outline some semantics for the various house
 * keeping constants - like the parent id of the suffix entry should always be
 * itself etc.
 * 
 * @author <a href="mailto:aok123@bellsouth.net">Alex Karasulu</a>
 * @author $Author: akarasulu $
 * @version $Revision: 1.8 $
 * @see Backend
 */
public interface LdapEntry
    extends Serializable
{
    /**
     * The identifier of the Distinguished name attribute for this entry.  The
     * value of this attribute is NOT a normalized DN it is the user provide 
     * DN.
     *
     * @see getEntryDN
     */
    String DN_ATTR = Schema.DN_ATTR ;
    /**
     * The identifier of the normalized Distinguished name attribute for this
     * Entry's parent.
     * 
     * @see getParentDN
     */
    String PARENTDN_ATTR = "parentdn" ;
    /**
     * The identifier of the normalized Distinguished name attribute for this
     * Entry's subschema subentry.
     * 
     * @see getSubschemaSubentry
     */
    String SUBSCHEMASUBENTRY_ATTR = "subschemasubentry" ;
    /**
     * The identifier of the normalized Distinguished name attribute for this
     * Entry's creator.
     * 
     * @see getCreatorsName
     */
    String CREATORSNAME_ATTR = "creatorsname" ;
    /**
     * The identifier of the normalized Distinguished name attribute for this
     * Entry's last modifier.
     * 
     * @see getModifiersName
     */
    String MODIFIERSNAME_ATTR = "modifiersname" ;
    /** Unique entry identifier attribute's name */
    String ID_ATTR = "entryid" ;
    /** Entry's parent id attribute's identifier */
    String PARENTID_ATTR = Schema.HIERARCHY_ATTR ;
    /** Number of subordinates attribute's identifier */
    String NUMSUBORDINATES_ATTR = "numsubordinates" ;
    /** Create timestamp attribute's identifier */
    String CREATETIMESTAMP_ATTR = "createtimestamp" ;
    /** Modifier's timestamp attribute's identifier */
    String MODIFYTIMESTAMP_ATTR = "modifytimestamp" ;

    Collection attributes() ;

    boolean hasAttribute(String an_attributeName) ;

    /**
     * Checks whether or not this Entry is a valid entry residing within a
     * backend.  Entries are validated on successful create() calls.  When
     * Entry instances are initialized in the java sense via the newEntry()
     * call on backends, they are in the invalid state.
     *
     * @return true if this entry has been persisted to a backend,
     * false otherwise.
     */
    boolean isValid() ;

    /**
     * Gets this entry's creation timestamp as a Date.
     *
     * @return Date representing the creation timestamp of this entry.
     */
    Date getCreateTimestamp() ;

    /**
     * Gets this entry's modification timestamp as a Date.
     *
     * @return Date representing the timestamp this entry was last modified.
     */
    Date getModifyTimestamp() ;

    /**
     * Gets the unique distinguished name associated with this entry as it was
     * supplied during creation without whitespace trimming or character case
     * conversions.  This version of the DN is kept within the body of this
     * Entry as an operational attribute so that it could be returned as it was
     * given to the server w/o normalization effects: case and whitespace will
     * be entact.
     *
     * @return the distinguished name of this entry as a String.
     */
    String getEntryDN() ;

    /**
     * Gets the normalized unique distinguished name associated with this 
     * entry. This DN unlike the user specified DN accessed via getDN() is not
     * an operational attribute composing the body of this Entry.
     *
     * The distinguished name is presumed to be normalized by the server naming
     * subsystem in accordance with schema attribute syntax and attribute
     * matching rules. The DN is also presumed to be syntacticly correct and
     * within the namespace of this directory information base from which this
     * Entry originated.
     *
     * @return the normalized distinguished name of this entry as a String.
     */
    Name getNormalizedDN()
        throws NamingException ;

    Name getUnNormalizedDN()
        throws NamingException ;

    /**
     * Gets the normalized unique distinguished name of this Entry's parent.
     *
     * The distinguished name is presumed to be normalized by the server naming
     * subsystem in accordance with schema attribute syntax and attribute
     * matching rules. The DN is also presumed to be syntacticly correct and
     * within the namespace of this directory information base from which this
     * Entry originated.
     *
     * @return the normalized distinguished name of this Entry's parent.
     */
    String getParentDN() ;

    /**
     * Gets the distinguished name of the creator of this entry.
     *
     * The distinguished name is presumed to be normalized by the server naming
     * subsystem in accordance with schema attribute syntax and attribute
     * matching rules. The DN is also presumed to be syntacticly correct and
     * within the namespace of this directory information base from which this
     * Entry originated.
     * 
     * @return the distinguished name of the creator.
     */
    String getCreatorsName() ;

    /**
     * Gets the distinguished name of the last modifier of this entry.
     *
     * The distinguished name is presumed to be normalized by the server naming
     * subsystem in accordance with schema attribute syntax and attribute
     * matching rules. The DN is also presumed to be syntacticly correct and
     * within the namespace of this directory information base from which this
     * Entry originated.
     * 
     * @return the DN of the user to modify this entry last.
     */
    String getModifiersName() ;

    Schema getSchema() ;

    /**
     * Gets the distinguished name of the subschema subentry for this Entry.
     *
     * The distinguished name is presumed to be normalized by the server naming
     * subsystem in accordance with schema attribute syntax and attribute
     * matching rules. The DN is also presumed to be syntacticly correct and
     * within the namespace of this directory information base from which this
     * Entry originated.
     * 
     * @return String of the subschema subentry distinguished name.
     */
    String getSubschemaSubentryDN() ;

    /**
     * Gets a single valued attribute by name or returns the first value of a
     * multivalued attribute.
     *
     * @param a_attribName the name of the attribute to lookup.
     * @return an Object value which is either a String or byte [] or null if
     * the attribute does not exist.
     */
    Object getSingleValue(String an_attribName) ;

    /**
     * Gets a multivalued attribute by name.
     *
     * @param a_attribName the name of the attribute to lookup.
     * @return a Collection or null if no attribute value exists.
     */
    Collection getMultiValue(String an_attribName) ;

    /**
     * Adds a value to this Entry potentially resulting in more than one value
     * for the attribute/key.
     * 
     * @param an_attribName attribute name/key
     * @param a_value the value to add
     * @throws InvalidAttributeIdentifierException when an attempt is made to
     * add to or create an attribute with an invalid attribute identifier.
     * @throws InvalidAttributeValueException when an attempt is made to add to
     * an attribute a value that conflicts with the attribute's schema
     * definition. This could happen, for example, if attempting to add an
     * attribute with no value when the attribute is required to have at least
     * one value, or if attempting to add more than one value to a single
     * valued-attribute, or if attempting to add a value that conflicts with 
     * the syntax of the attribute.
     */
    void addValue(String an_attribName, Object a_value)
        throws
        AttributeInUseException,
        InvalidAttributeValueException,
        InvalidAttributeIdentifierException ;

    /**
     * Removes the attribute/value pair in this Entry only without affecting
     * other values that the attribute may have.
     *
     * @param an_attribName attribute name/key
     * @param a_value the value to remove
     * @throws AttributeModificationException when an attempt is made to modify
     * an attribute, its identifier, or its values that conflicts with the
     * attribute's (schema) definition or the attribute's state.  Also thrown
     * if the specified attribute name does not exist as a key in this Entry.
     */
    void removeValue(String an_attribName, Object a_value)
        throws InvalidAttributeIdentifierException ;

    /**
     * Removes the specified set of attribute/value pairs in this Entry.
     *
     * @param an_attribName attribute name/key
     * @param a_valueArray the set of values to remove
     * @throws AttributeModificationException when an attempt is made to modify
     * an attribute, its identifier, or its values that conflicts with the
     * attribute's (schema) definition or the attribute's state.  Also thrown
     * if the specified attribute name does not exist as a key in this Entry.
     */
    void removeValues(String an_attribName, Object [] a_valueArray)
        throws InvalidAttributeIdentifierException ;

    /**
     * Removes all the attribute/value pairs in this Entry associated with the
     * attribute.
     *
     * @param an_attribName attribute name/key
     * @param a_value the value to remove
     * @throws AttributeModificationException when an attempt is made to modify
     * an attribute, its identifier, or its values that conflicts with the
     * attribute's (schema) definition or the attribute's state.  Also thrown
     * if the specified attribute name does not exist as a key in this Entry.
     */
    void removeValues(String an_attribName)
        throws InvalidAttributeIdentifierException ;

    /**
     * Checks to see if this LdapEntry is equal to an entry with a Dn.
     *
     * @param a_dn the dn to test for equality.
     * @return true if this entry has a Dn equal to a_dn, false otherwise.
     */
    boolean equals(String a_dn)
        throws NamingException ;
}
