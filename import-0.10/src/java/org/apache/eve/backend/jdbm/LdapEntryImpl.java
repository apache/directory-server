/*
 * $Id: LdapEntryImpl.java,v 1.9 2003/08/06 03:01:25 akarasulu Exp $
 * $Prologue$
 *
 * -- (c) LDAPd Group                                                    --
 * -- Please refer to the LICENSE.txt file in the root directory of      --
 * -- any LDAPd project for copyright and distribution information.      --
 *
 */

package org.apache.eve.backend.jdbm ;


import java.util.Date ;
import java.util.HashMap ;
import java.util.Iterator ;
import java.util.Collection ;
import java.math.BigInteger ;

import javax.naming.Name ;
import javax.naming.NamingException ;
import javax.naming.directory.AttributeInUseException ;
import javax.naming.directory.InvalidAttributeValueException ;
import javax.naming.directory.AttributeModificationException ;
import javax.naming.directory.InvalidAttributeIdentifierException ;

import org.apache.ldap.common.util.NamespaceTools ;
import org.apache.eve.backend.LdapEntry ;
import org.apache.eve.schema.Schema ;
import org.apache.eve.protocol.ProtocolModule;

import org.apache.avalon.framework.logger.Logger ;
import org.apache.avalon.framework.logger.LogEnabled ;

import org.apache.commons.collections.LRUMap ;
import org.apache.commons.collections.MultiHashMap ;
import org.apache.ldap.common.name.DnParser;


/**
 * An entry implementation for the Berkeley backend.
 * 
 * @task No schema checking going on right now - but this needs to change!
 * @task Also we need to make sure that we are storing dates in the apropriate
 * fashion
 */
public class LdapEntryImpl
    extends MultiHashMap
	implements LdapEntry, LogEnabled
{
    private final Schema m_schema ;
    private Logger m_log ;
    private Name m_normalizedDn ;
    private Name m_dn ;
    private boolean m_isValid = false ;
    private BigInteger m_id ;
    private HashMap m_attrIdMap = new HashMap() ;
    private LRUMap m_attrIdCache = new LRUMap(25) ;

    // Only used on validated entries - validation initializes them.
    private MultiHashMap m_added = null ;
    private MultiHashMap m_removed = null ;


	LdapEntryImpl( Schema a_schema, Name a_dName, String a_dnStr )
    {
        m_schema = a_schema ;	       				// schema this entry uses
        m_normalizedDn = a_dName ;	    			// normalized parsed name
        put( DN_ATTR, a_dnStr ) ;	    			// user provided name
    }


    LdapEntryImpl(Schema a_schema)
    {
        m_schema = a_schema ;
    }


    /**
     * Note that according to the Backend-Server contract this method must
     * return a Collection of attribute identifier names as they were
     * specified by the user at creation time.  These are not normalized
     * attribute identifiers.
     */
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
        String l_normalized = m_schema.normalize(Schema.DN_ATTR, a_dn) ;
        Name l_name = this.getNormalizedDN() ;
        return l_name.toString().equals(l_normalized) ;
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
     * Gets the unique identifier of this entry as a BigInteger.
     *
     * @return BigInteger unique identifier of this entry.
     */
    public BigInteger getEntryID()
    {
        if(m_id == null) {
            m_id = new BigInteger((String) getSingleValue(ID_ATTR)) ;
        }

        return m_id ;
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
        if(m_normalizedDn == null) {
            m_normalizedDn =
                m_schema.getNormalizingParser().parse(getEntryDN()) ;
        }

        return m_normalizedDn;
    }


    /**
     * Gets a Name representation of the originally user provided name without
     * any case conversions.  I added this method so we can cache this value
     * and avoid the reparse via the use of the nexus getName method.
     */
    public Name getUnNormalizedDN()
        throws NamingException
    {
        if(m_dn == null) {
            m_dn = m_schema.getNameParser().parse(getEntryDN()) ;
        }

        return m_dn ;
    }


    /**
     * Gets the number of subordinate child entries that exist for this Entry.
     *
     * @return the number of subordinates (children).
     */
    public BigInteger getNumSubordinates()
    {
        return new BigInteger((String)
            getSingleValue(NUMSUBORDINATES_ATTR)) ;
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
     * Gets the unique identifier of this entry's parent entry as a BigInteger.
     * The parent ID is equal to the entry id <code>
     * (getEntryID().equals(parentID()) == true) </code> if this entry is a 
     * root (a.k.a. suffix) entry.
     *
     * @return the uid of this entry's parent.
     */
    public BigInteger getParentID()
    {
        return new BigInteger((String) getSingleValue(PARENTID_ATTR)) ;
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
     * @param a_attribName the name of the attribute to lookup.
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
     * @param a_attribName the name of the attribute to lookup.
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


    private void checkIdentifier( String an_attribName )
        throws InvalidAttributeIdentifierException
    {
		if( ! m_schema.hasAttribute( an_attribName ) )
        {
            // InvalidAttributeIdentifierException JNDI exceptions corresponds
            // to an UNDEFINEDATTRIBUTETYPE result code within LDAPv2 with a
            // value of [17]
            throw new InvalidAttributeIdentifierException(
                "[17] " + an_attribName +
                " is not a valid schema recognized attribute name." ) ;
        }
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
        // Null args have no affect
	    if( null == a_value || null == an_attribName )
        {
            return ;
        }

		checkIdentifier( an_attribName ) ;

		if( containsKey( an_attribName ) &&
            m_schema.isSingleValue( an_attribName ) )
        {
            // AttributeInUseException JNDI exceptions correspond to
            // ATTRIBUTEORVALUEEXISTS result code in LDAPv3 with a value of [20]
            throw new AttributeInUseException( "[20] A key for attribute "
                + an_attribName + " already exists!" ) ;
        }


        // Indices do not exist on binary attributes so there is no need to
        // update them or for that matter track changes to them
        if( m_schema.isBinary( an_attribName ) )
        {
            put( an_attribName, a_value ) ;
        }

        // @todo Answer why we are not asking the schema here if the value is
        // valid when we are dealing with numbers? Shouldn't we be performing
        // some form of syntax check?
        else if( m_schema.isNumeric( an_attribName ) ||
            m_schema.isDecimal( an_attribName ) )
        {
            put( an_attribName, a_value ) ;

            if( m_isValid )
            {
                m_added.put( an_attribName, a_value ) ;
            }
        }
        else
        {
            String l_value = ( String ) a_value ;

    	    if( l_value.trim().equals( "" ) )
    		{
                return ;
            }

            if( ! m_schema.isValidSyntax( an_attribName, l_value ) )
            {
                // InvalidAttributeValueException JNDI exceptions correspond to
                // the INVALIDATTRIBUTESYNTAX result code with a value of [21]
                throw new InvalidAttributeValueException( "[21] '" + l_value
                    + "' does not comply with the syntax for attribute "
                    + an_attribName ) ;
            }

            put( an_attribName, l_value ) ;

            if( m_isValid )
            {
                m_added.put( an_attribName, l_value ) ;
            }
        }
    }


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
    public void removeValue( String an_attribName, Object a_value )
        throws InvalidAttributeIdentifierException
    {
		checkIdentifier( an_attribName ) ;

        if( m_schema.isBinary( an_attribName ) )
        {
            remove( an_attribName, a_value ) ;
        }
        else
        {
            remove( an_attribName, a_value ) ;

        	// For valid entries within the backend we track what we added and
            // removed.  Need to remove values added to shadow copy that now get
            // removed.
            if( m_isValid )
            {
                m_removed.put( an_attribName, a_value ) ;
                m_added.remove( an_attribName, a_value ) ;
            }
        }
    }


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
    public void removeValues( String an_attribName )
        throws InvalidAttributeIdentifierException
    {
		checkIdentifier( an_attribName ) ;

        // Don't care if the value does not exist
        if( ! containsKey( an_attribName ) )
        {
            return ;
        }

        Iterator l_removed = getMultiValue( an_attribName ).iterator() ;
        if( m_isValid )
        {
			while( l_removed.hasNext() )
            {
					m_removed.put( an_attribName, l_removed.next() ) ;
			}
	
			m_added.remove( an_attribName ) ;
		}

		remove( an_attribName ) ;
    }


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
    public void removeValues(String an_attribName, Object [] a_valueArray)
        throws InvalidAttributeIdentifierException
    {
		checkIdentifier( an_attribName ) ;

        for( int ii = 0; ii < a_valueArray.length; ii++ )
        {
            remove( an_attribName, a_valueArray[ii] ) ;
        }
    }


    //////////////////////////////
    // Package Friendly Methods //
    //////////////////////////////


    /**
     * This method is called to indicate that this entry has now been created
     * within a backend.  Any alterations to be made to is are going to be
     * tracked using the three multimaps.  The add, remove and change methods
     * will on use now begin to add attribute into the appropriate multimap
     * resouvoirs to enable store optimization on updates.
     */
    void validate()
    {
        m_isValid = true ;

		if(null == m_added) {
	    	m_added = new MultiHashMap() ;
        } else {
            m_added.clear() ;
        }

        if(null == m_removed) {
	    	m_removed = new MultiHashMap() ;
        } else {
            m_removed.clear() ;
        }
    }


    MultiHashMap getRemoved()
    {
        return m_removed ;
    }

    
    MultiHashMap getAdded()
    {
        return m_added ;
    }


    /**
     * Sets the parent id and parent dn operational attributes of this 
     * entry to correspond to a new parent entry.  The appropriate changes
     * are made to the added and removed multimaps to correctly update
     * this entry with the new parent child relationship.  Effects are not
     * manifested in the backend util an update is performed on this Entry.
     *
     * @param a_parentEntry the parent to make this Entry a child of.
     */
    void setParent( LdapEntry a_parentEntry )
        throws NamingException
    {
		removeValues( PARENTDN_ATTR ) ;
		addValue(PARENTDN_ATTR, a_parentEntry.getEntryDN() ) ;
	
		removeValues( PARENTID_ATTR ) ;
		addValue( PARENTID_ATTR, ( ( LdapEntryImpl ) a_parentEntry )
            .getSingleValue( ID_ATTR ) ) ;
    }


    /**
     * Sets this Entry's DN modifying both the operational attribute for 
     * tracking the user provided DN as well as the normalized DN.
     *
     * @param a_dn the new user provided dn to change this Entry's DN to.
     */
    void setEntryDN(Name a_dn)
        throws NamingException
    {
        if(getLogger().isDebugEnabled()) {
            getLogger().debug("" //ProtocolModule.getMessageKey()
                + " - LdapEntryImpl.setEntryDN() - altering entry dn from "
                + m_normalizedDn+ " to " + a_dn) ;
        }

        // Get the old user provided Dn and remove it by removing it from
        // this MultiMap and adding it to the m_removed MultiMap.
        String l_oldDn = getEntryDN() ;
        remove(DN_ATTR, l_oldDn) ;
        if(m_isValid) {
            m_removed.put(DN_ATTR, l_oldDn) ;
        }

        // Reparse as normalized name using schema's normalizing parser
		m_normalizedDn =
            m_schema.getNormalizingParser().parse(a_dn.toString()) ;

        // Add unnormalized user provided Dn argument to this MultiMap and
        // the m_added MultiMap
        put(DN_ATTR, a_dn.toString()) ;
        if(this.m_isValid) {
            m_added.put(DN_ATTR, a_dn.toString()) ;
        }

        if(getLogger().isDebugEnabled()) {
            getLogger().debug("" //ProtocolModule.getMessageKey()
                + " - LdapEntryImpl.setEntryDN() - dumping delta hashes "
                + "after removing the old dn and adding the new Dn") ;
            getLogger().debug("Added hash:\n" + this.m_added.toString()) ;
            getLogger().debug("Removed hash:\n" + this.m_removed.toString()) ;
        }
    }


    ////////////////////////////////////
    // Overridden MultiMap Interfaces //
    ////////////////////////////////////


    /**
     * Gets the user provided key form of the attribute argument first then
     * calls the super class' containsKey with the calculated UPK.  If no
     * UPK is found then false is returned.
     */
    public boolean containsKey(Object a_attrId)
    {
        String l_upk = getUserProvidedKey((String) a_attrId) ;

        if(l_upk == null) {
            return false ;
        }

        return super.containsKey(l_upk) ;
    }


    /**
     * Gets the user provided key form of the attribute argument first then
     * calls the super class' get with the calculated UPK.  If no UPK is found
     * then null is returned.
     */
    public Object get(Object a_attrId)
    {
        String l_upk = getUserProvidedKey((String) a_attrId) ;

        if(l_upk == null) {
            return null ;
        }

        return super.get(l_upk) ;
    }


    /**
     * Gets the user provided key form of the attribute argument first then
     * calls the super class' put with the calculated UPK.  If there is no
     * UPK defined it persumes that the argument is the UPK adding it to the
     * map of canonicial forms to UPKs.  So first come first serve - meaning
     * the first time an attribute is set to a value the text form of the
     * attribute identifier is set as the UPK.
     */
    public Object put(Object a_attrId, Object a_value)
    {
        String l_upk = getUserProvidedKey((String) a_attrId) ;

        if(l_upk == null) {
            setUserProvidedKey((String) a_attrId) ;
            l_upk = (String) a_attrId ;
        }

        return super.put(l_upk, a_value) ;
    }


    /**
     * Gets the user provided key form of the attribute argument first then
     * calls the super class' remove with the calculated UPK.  If no UPK is
     * found then nothing is done.
     */
    public Object remove(Object a_attrId)
    {
        String l_upk = getUserProvidedKey((String) a_attrId) ;

        if(l_upk == null) {
            return null ;
        }

        return super.remove(l_upk) ;
    }


    /**
     * Gets the user provided key form of the attribute argument first then
     * calls the super class' remove with the calculated UPK.  If no UPK is
     * found then nothing is done.
     */
    public Object remove(Object a_attrId, Object a_value)
    {
        String l_upk = getUserProvidedKey((String) a_attrId) ;

        if(l_upk == null) {
            return null ;
        }

        return super.remove(l_upk, a_value) ;
    }


    /////////////////////
    // Utility Methods //
    /////////////////////


    /**
     * Gets the canonical text normalized form of the attribute identifier. It
     * first tests to see if the canonical for exists in the normalized
     * attribute id cache.  If the cannonical form is there it is returned
     * other wise it generates the normalized form and adds it to the cache.
     * It then returns this newly generated normalized form.
     */
    private String getCanonicalKey(String a_attrId)
    {
        String l_canonical = null ;

        // Now if the argument is not the key get the canonical key either
        // from the attribute id cache or by creating it from scratch.
        if(m_attrIdCache.containsKey(a_attrId)) {
            l_canonical = (String) m_attrIdCache.get(a_attrId) ;
        } else {
            l_canonical = a_attrId.toLowerCase() ;
            m_attrIdCache.put(a_attrId, l_canonical) ;
            m_attrIdCache.put(l_canonical, l_canonical) ;
        }

        return l_canonical ;
    }


    /**
     * Gets the original user provided key as was entered into the backend the
     * first time.  It first presumes that the argument a_attrId is already \
     * normalized by attempting a lookup into the normalized attribute id to
     * user defined attribute id map.  If it finds it there the value is
     * returned.  Otherwise it calls getCanonicalKey on the argument to get
     * the canonical form and used the cannonical form for the same lookup. If
     * nothing is found in the id map then null is returned.
     */
    private String getUserProvidedKey(String a_attrId)
    {
        if(m_attrIdMap.containsKey(a_attrId)) {
            return (String) m_attrIdMap.get(a_attrId) ;
        } else {
            String l_canonical = getCanonicalKey(a_attrId) ;
            if(m_attrIdMap.containsKey(l_canonical)) {
                return (String) m_attrIdMap.get(l_canonical) ;
            }
        }

        return null ;
    }


    /**
     * Sets the user defined key in the user defined attribute Id map by making
     * a call to getCanonicalKey to get the text normalized version of the
     * argument String a_upk.  It uses this normalized form as the key and the
     * argument String a_upk to create a new entry in the attribute id map.
     */
    private void setUserProvidedKey(String a_upk)
    {
        String l_canonical = getCanonicalKey(a_upk) ;
        m_attrIdMap.put(l_canonical, a_upk) ;
    }


    public void enableLogging(Logger a_logger)
    {
        m_log = a_logger ;
    }


    public boolean isDebugEnabled()
    {
        return m_log.isDebugEnabled() ;
    }


    public Logger getLogger()
    {
        return m_log ;
    }
}
