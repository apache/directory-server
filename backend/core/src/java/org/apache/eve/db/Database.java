package org.apache.eve.db;


import java.util.Iterator;

import java.math.BigInteger;

import javax.naming.Name;
import javax.naming.NamingException;
import javax.naming.NamingEnumeration;
import javax.naming.directory.Attributes;
import javax.naming.directory.ModificationItem;

import org.apache.ldap.common.schema.AttributeType;


/**
 *
 */
public interface Database
{
    // @todo do these alias constants need to go elsewhere?
    /** The objectClass name for aliases: 'alias' */
    String ALIAS_OBJECT = "alias";
    /** 
     * The aliased Dn attribute name: aliasedObjectName for LDAP and
     * aliasedEntryName or X.500.
     */ 
    String ALIAS_ATTRIBUTE = "aliasedObjectName";


    // ------------------------------------------------------------------------
    // Index Operations 
    // ------------------------------------------------------------------------


    /**
     * TODO Document me!
     *
     * @param attribute TODO
     * @throws NamingException TODO
     */    
    void addIndexOn( AttributeType attribute ) throws NamingException;

    /**
     * TODO Document me!
     *
     * @param attribute TODO
     * @return TODO
     */
    boolean hasUserIndexOn( String attribute );

    /**
     * TODO Document me!
     *
     * @param attribute TODO
     * @return TODO
     */
    boolean hasSystemIndexOn( String attribute );

    /**
     * Gets the Index mapping the names of attributes as Strings to the 
     * BigInteger primary keys of entries containing one or more values of those
     * attributes.
     *
     * @return the existance Index
     */
    Index getExistanceIndex();

    /**
     * Gets the Index mapping the BigInteger primary keys of parents to the 
     * BigInteger primary keys of their children.
     *
     * @return the heirarchy Index
     */
    Index getHeirarchyIndex();
    
    /**
     * Gets the Index mapping user provided distinguished names of entries as 
     * Strings to the BigInteger primary keys of entries.
     *
     * @return the user provided distinguished name Index
     */
    Index getUpdnIndex();

    /**
     * Gets the Index mapping the normalized distinguished names of entries as
     * Strings to the BigInteger primary keys of entries.  
     *
     * @return the normalized distinguished name Index
     */
    Index getNdnIndex();

    /**
     * Gets the alias index mapping parent entries with scope expanding aliases 
     * children one level below them; this system index is used to dereference
     * aliases on one/single level scoped searches.
     * 
     * @return the one alias index
     */
    Index getOneAliasIndex();

    /**
     * Gets the alias index mapping relative entries with scope expanding 
     * alias descendents; this system index is used to dereference aliases on 
     * subtree scoped searches.
     * 
     * @return the sub alias index
     */
    Index getSubAliasIndex();

    /**
     * Gets the system index defined on the ALIAS_ATTRIBUTE which for LDAP would
     * be the aliasedObjectName and for X.500 would be aliasedEntryName.
     * 
     * @return the index on the ALIAS_ATTRIBUTE
     */
    Index getAliasIndex();

    /**
     * Sets the system index defined on the ALIAS_ATTRIBUTE which for LDAP would
     * be the aliasedObjectName and for X.500 would be aliasedEntryName.
     * 
     * @param idx the index on the ALIAS_ATTRIBUTE
     */
    void setAliasIndex( Index idx );

    /**
     * Sets the attribute existance Index.
     *
     * @param idx the attribute existance Index
     */    
    void setExistanceIndex( Index idx );

    /**
     * Sets the heirarchy Index.
     *
     * @param idx the heirarchy Index
     */    
    void setHeirarchyIndex( Index idx );

    /**
     * Sets the user provided distinguished name Index.
     *
     * @param idx the updn Index
     */    
    void setUpdnIndex( Index idx );

    /**
     * Sets the normalized distinguished name Index.
     *
     * @param idx the ndn Index
     */    
    void setNdnIndex( Index idx );
    
    /**
     * Sets the alias index mapping parent entries with scope expanding aliases 
     * children one level below them; this system index is used to dereference
     * aliases on one/single level scoped searches.
     * 
     * @param idx a one level alias index
     */
    void setOneAliasIndex( Index idx );
    
    /**
     * Sets the alias index mapping relative entries with scope expanding 
     * alias descendents; this system index is used to dereference aliases on 
     * subtree scoped searches.
     * 
     * @param idx a subtree alias index
     */
    void setSubAliasIndex( Index idx );
    
    /**
     * TODO Document me!
     *
     * @param attribute TODO
     * @return TODO
     * @throws IndexNotFoundException TODO
     */
    Index getUserIndex( String attribute ) throws IndexNotFoundException;

    /**
     * TODO Document me!
     *
     * @param attribute TODO
     * @return TODO
     * @throws IndexNotFoundException TODO
     */
    Index getSystemIndex( String attribute ) throws IndexNotFoundException;

    /**
     * TODO Document me!
     *
     * @param dn TODO
     * @return TODO
     * @throws NamingException TODO
     */
    BigInteger getEntryId( String dn ) throws NamingException;

    /**
     * TODO Document me!
     *
     * @param id TODO
     * @return TODO
     * @throws NamingException TODO
     */
    String getEntryDn( BigInteger id ) throws NamingException;

    /**
     * TODO Document me!
     * 
     * @param dn TODO
     * @return TODO
     * @throws NamingException TODO
     */
    BigInteger getParentId( String dn ) throws NamingException;

    /**
     * TODO Document me!
     *
     * @param childId TODO
     * @return TODO
     * @throws NamingException TODO
     */
    BigInteger getParentId( BigInteger childId ) throws NamingException;

    /**
     * Gets the user provided distinguished name.
     *
     * @param id the entry id
     * @return the user provided distinguished name
     * @throws NamingException if the updn index cannot be accessed
     */
    String getEntryUpdn( BigInteger id ) throws NamingException;

    /**
     * Gets the user provided distinguished name.
     *
     * @param dn the normalized distinguished name
     * @return the user provided distinguished name
     * @throws NamingException if the updn and ndn indices cannot be accessed
     */
    String getEntryUpdn( String dn ) throws NamingException;

    /**
     * TODO Document me!
     *
     * @param updn the user provided distinguished name of the entry
     * @param dn TODO
     * @param entry TODO
     * @throws NamingException TODO
     */
    void add( String updn, Name dn, Attributes entry )
        throws NamingException;

    /**
     * TODO Document me!
     *
     * @param id TODO
     * @return TODO
     * @throws NamingException TODO
     */
    Attributes lookup( BigInteger id ) throws NamingException;

    /**
     * TODO Document me!
     *
     * @param id TODO
     * @throws NamingException TODO
     */
    void delete( BigInteger id ) throws NamingException;

    /**
     * TODO Document me!
     *
     * @param id TODO
     * @return TODO
     * @throws NamingException TODO
     */
    NamingEnumeration list( BigInteger id ) throws NamingException;

    /**
     * TODO Document me!
     *
     * @param id TODO
     * @return TODO
     * @throws NamingException TODO
     */
    int getChildCount( BigInteger id ) throws NamingException;

    /**
     * @return TODO
     */ 
    Name getSuffix();

    /**
     * TODO Document me!
     *
     * @return TODO
     * @throws NamingException TODO
     */
    Attributes getSuffixEntry() throws NamingException;

    /**
     * TODO Document me!
     *
     * @throws NamingException TODO
     */
    void sync() throws NamingException;

    /**
     * TODO Document me!
     *
     * @throws NamingException TODO
     */
    void close() throws NamingException;

    /**
     * TODO Document me!
     *
     * @param key TODO
     * @param value TODO
     * @throws NamingException TODO
     */
    void setProperty( String key, String value ) throws NamingException;

    /**
     * TODO Document me!
     *
     * @param key TODO
     * @return TODO
     * @throws NamingException TODO
     */
    String getProperty( String key ) throws NamingException;

    /**
     * TODO Document me!
     *
     * @return TODO
     */
    Iterator getUserIndices();
    
    /**
     * TODO Document me!
     *
     * @return TODO
     */
    Iterator getSystemIndices();

    /**
     * TODO Document me!
     *
     * @param id TODO
     * @return TODO
     * @throws NamingException TODO
     */
    Attributes getIndices( BigInteger id ) throws NamingException;

    /**
     * TODO Document me!
     *
     * @param dn TODO
     * @param modOp TODO
     * @param mods TODO
     * @throws NamingException TODO
     */
    void modify( Name dn, int modOp, Attributes mods )
        throws NamingException;
    
    /**
     * TODO Document me!
     *
     * @param dn TODO
     * @param mods TODO
     * @throws NamingException TODO
     */
    void modify( Name dn, ModificationItem [] mods )
        throws NamingException;

    /**
     * TODO Document me!
     *
     * @param dn TODO
     * @param newRdn TODO
     * @param deleteOldRdn TODO
     * @throws NamingException TODO
     */
    void modifyRdn( Name dn, String newRdn, boolean deleteOldRdn )
        throws NamingException;

    /**
     * TODO Document me!
     *
     * @param oldChildDn TODO
     * @param newParentDn TODO
     * @throws NamingException TODO
     */
    void move( Name oldChildDn, Name newParentDn ) throws NamingException;

    /**
     * Moves a child from one location to another while changing the Rdn 
     * attribute used in the new location and optionally deleting the old 
     * Rdn attribute value pair.
     *
     * @param oldChildDn the normalized child dn to move
     * @param newParentDn the normalized new parent dn to move the child to
     * @param newRdn the new rdn of the child at its new location
     * @param deleteOldRdn switch to remove the old rdn attribute/value pair
     * @throws NamingException if a database failure results
     */
    void move( Name oldChildDn, Name newParentDn, String newRdn,
        boolean deleteOldRdn ) throws NamingException;

    /**
     * Gets the count of the total number of entries in the database.
     *
     * TODO shouldn't this be a BigInteger instead of an int? 
     * 
     * @return the number of entries in the database 
     * @throws NamingException if there is a failure to read the count
     */
    int count() throws NamingException;
}
