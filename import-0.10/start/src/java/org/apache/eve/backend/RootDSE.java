/*
 * $Id: RootDSE.java,v 1.3 2003/03/13 18:27:00 akarasulu Exp $
 * $Prologue$
 *
 * -- (c) LDAPd Group                                                    --
 * -- Please refer to the LICENSE.txt file in the root directory of      --
 * -- any LDAPd project for copyright and distribution information.      --
 *
 */

package org.apache.eve.backend ;


import java.util.Date ;
import java.util.Collection ;

import javax.naming.Name ;
import javax.naming.NamingException ;
import javax.naming.directory.AttributeInUseException ;
import javax.naming.directory.InvalidAttributeValueException ;
import javax.naming.directory.InvalidAttributeIdentifierException ;

import org.apache.commons.collections.MultiHashMap ;

import org.apache.ldap.common.name.LdapName ;
import org.apache.eve.schema.Schema ;


/**
 * An entry implementation for the Berkeley backend.
 * 
 * @task No schema checking going on right now - but this needs to change!
 * @task Also we need to make sure that we are storing dates in the apropriate
 * fashion
 */
public class RootDSE
    extends MultiHashMap
	implements LdapEntry
{
    private final Schema m_schema ;
    private final Name m_dn = new LdapName() ;
    private boolean m_isValid = false ;


    RootDSE(Schema a_schema)
    {
        m_schema = a_schema ;
    }


    public Collection attributes()
    {
        return this.keySet() ;
    }


    public Schema getSchema()
    {
        return m_schema ;
    }


	public boolean equals(String a_dn)
        throws NamingException
    {
        if(a_dn == null) {
            return false ;
        }

        return a_dn.trim().equals("") ;
    }


    public boolean hasAttribute(String an_attributeName)
    {
        return containsKey(an_attributeName) ;
    }


    public boolean hasAttributeValuePair(String an_attribute, Object a_value)
    {
        if(containsKey(an_attribute)) {
            Collection l_collection = getMultiValue(an_attribute) ;

            if(null == l_collection && null == a_value) {
                return true ;
            } else if(null == l_collection || null == a_value) {
                return false ;
            } else if(l_collection.contains(a_value)) {
                return true ;
            } else {
                return false ;
            }
        } else {
	    return false ;
        }
    }


    /**
     * Gets this entry's creation timestamp as a Date.
     *
     * @return Date representing the creation timestamp of this entry.
     */
    public Date getCreateTimestamp()
    {
	return new Date((String) getSingleValue(CREATETIMESTAMP_ATTR)) ;
    }


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
    public String getCreatorsName()
    {
        return (String) getSingleValue(CREATORSNAME_ATTR) ;
    }


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
    public String getEntryDN()
    {
        return (String) getSingleValue(DN_ATTR) ;
    }


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
    public String getModifiersName()
    {
        return (String) getSingleValue(MODIFIERSNAME_ATTR) ;
    }


    /**
     * Gets this entry's modification timestamp as a Date.
     *
     * @return Date representing the timestamp this entry was last modified.
     */
    public Date getModifyTimestamp()
    {
        return new Date((String)
            getSingleValue(MODIFYTIMESTAMP_ATTR)) ;
    }


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
    public Name getNormalizedDN()
        throws NamingException
    {
        return m_dn ;
    }


    public Name getUnNormalizedDN()
        throws NamingException
    {
        return m_dn ;
    }


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
    public String getParentDN()
    {
        return (String) getSingleValue(PARENTDN_ATTR) ;
    }


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
    public String getSubschemaSubentryDN()
    {
        return (String) getSingleValue(SUBSCHEMASUBENTRY_ATTR) ;
    }


    /**
     * Checks whether or not this Entry is a valid entry residing within a
     * backend.  Entries are validated on successful create() calls.  When
     * Entry instances are initialized in the java sense via the newEntry()
     * call on backends, they are in the invalid state.
     *
     * @return true if this entry has been persisted to a backend,
     * false otherwise.
     */
    public boolean isValid()
    {
        return m_isValid ;
    }


    /**
     * Gets a multivalued attribute by name.
     *
     * @param an_attribName the name of the attribute to lookup.
     * @return a Collection or null if no attribute value exists.
     */
    public Collection getMultiValue(String an_attribName)
    {
        return (Collection) get(an_attribName) ;
    }


    /**
     * Gets a single valued attribute by name or returns the first value of a
     * multivalued attribute.
     *
     * @param an_attribName the name of the attribute to lookup.
     * @return an Object value which is either a String or byte [] or null if
     * the attribute does not exist.
     */
    public Object getSingleValue(String an_attribName)
    {
        Collection l_col = (Collection) get(an_attribName) ;

		if(null == l_col || l_col.isEmpty()) {
            return null ;
        }

        return l_col.iterator().next() ;
    }


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
    public void addValue(String an_attribName, Object a_value)
        throws
        AttributeInUseException,
        InvalidAttributeValueException,
        InvalidAttributeIdentifierException
    {
	    if(null == a_value || null == an_attribName) {
            return ;
        }

        if(!m_schema.hasAttribute(an_attribName)) {
            throw new InvalidAttributeIdentifierException(an_attribName +
                " is not a valid schema recognized attribute name.") ;
        }

		if(containsKey(an_attribName) &&
            m_schema.isSingleValue(an_attribName))
        {
            throw new AttributeInUseException("A key for attribute "
                + an_attribName + " already exists!") ;
        }


		String l_value = null ;

		if(a_value.getClass().isArray()) {
			l_value = new String((byte []) a_value) ;
		} else {
			l_value = (String) a_value ;
		}

		if(l_value.trim().equals("")) {
			return ;
		}

		if(!m_schema.isValidSyntax(an_attribName, l_value)) {
			throw new InvalidAttributeValueException("'" + l_value
				+ "' does not comply with the syntax for attribute "
				+ an_attribName) ;
		}

		put(an_attribName, l_value) ;
    }


    /**
     * Removes the attribute/value pair in this Entry only without affecting
     * other values that the attribute may have.
     *
     * @param an_attribName attribute name/key
     * @param a_value the value to remove
     * @throws InvalidAttributeIdentifierException when an attempt is made to modify
     * an attribute, its identifier, or its values that conflicts with the
     * attribute's (schema) definition or the attribute's state.  Also thrown
     * if the specified attribute name does not exist as a key in this Entry.
     */
    public void removeValue(String an_attribName, Object a_value)
        throws InvalidAttributeIdentifierException
    {
        if(!m_schema.hasAttribute(an_attribName)) {
            throw new InvalidAttributeIdentifierException(an_attribName +
                " is not a valid schema recognized attribute name.") ;
        }

		remove(an_attribName, a_value) ;
    }


    /**
     * Removes all the attribute/value pairs in this Entry associated with the
     * attribute.
     *
     * @param an_attribName attribute name/key
     * @param an_attribName the value to remove
     * @throws InvalidAttributeIdentifierException when an attempt is made to modify
     * an attribute, its identifier, or its values that conflicts with the
     * attribute's (schema) definition or the attribute's state.  Also thrown
     * if the specified attribute name does not exist as a key in this Entry.
     */
    public void removeValues(String an_attribName)
        throws InvalidAttributeIdentifierException
    {
        if(!m_schema.hasAttribute(an_attribName)) {
            throw new InvalidAttributeIdentifierException(an_attribName +
                " is not a valid schema recognized attribute name.") ;
        }

        if(!containsKey(an_attribName)) {
            return ;
        }

		remove(an_attribName) ;
    }


    /**
     * Removes the specified set of attribute/value pairs in this Entry.
     *
     * @param an_attribName attribute name/key
     * @param a_valueArray the set of values to remove
     * @throws InvalidAttributeIdentifierException when an attempt is made to modify
     * an attribute, its identifier, or its values that conflicts with the
     * attribute's (schema) definition or the attribute's state.  Also thrown
     * if the specified attribute name does not exist as a key in this Entry.
     */
    public void removeValues(String an_attribName, Object [] a_valueArray)
        throws InvalidAttributeIdentifierException
    {
        if(!m_schema.hasAttribute(an_attribName)) {
            throw new InvalidAttributeIdentifierException(an_attribName +
                " is not a valid schema recognized attribute name.") ;
        }

		for(int ii = 0; ii < a_valueArray.length; ii++) {
			remove(an_attribName, a_valueArray[ii]) ;
		}
    }
}
