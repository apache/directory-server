/*
 * $Id: DirContextHelper.java,v 1.5 2003/08/22 21:15:55 akarasulu Exp $
 *
 * -- (c) LDAPd Group                                                    --
 * -- Please refer to the LICENSE.txt file in the root directory of      --
 * -- any LDAPd project for copyright and distribution information.      --
 *
 */

package org.apache.eve.jndi ;


import javax.naming.directory.DirContext;
import org.apache.eve.backend.UnifiedBackend;
import org.apache.eve.backend.LdapEntry;
import javax.naming.NamingException;
import javax.naming.directory.Attributes;
import javax.naming.Name;
import org.apache.ldap.common.name.LdapName;
import org.apache.eve.backend.BackendException;
import javax.naming.directory.BasicAttributes;
import javax.naming.directory.Attribute;
import javax.naming.directory.BasicAttribute;
import java.util.Collection;
import java.util.Iterator;
import javax.naming.directory.NoSuchAttributeException;
import javax.naming.directory.ModificationItem;
import javax.naming.NamingEnumeration;
import java.util.HashSet;
import javax.naming.directory.SearchControls;
import org.apache.ldap.common.filter.ExprNode;
import org.apache.ldap.common.filter.BranchNode;
import org.apache.ldap.common.filter.PresenceNode;
import java.util.ArrayList;
import org.apache.ldap.common.filter.SimpleNode;
import org.apache.eve.backend.Cursor;
import org.apache.ldap.common.filter.FilterParser;
import org.apache.ldap.common.filter.FilterParserImpl;
import java.io.IOException;
import java.text.ParseException;


public class DirContextHelper
{
    private final UnifiedDirContext m_ctx ;


    DirContextHelper( UnifiedDirContext a_dirCtx )
    {
        m_ctx = a_dirCtx ;
    }


    public Attributes getAttributes( Name a_rdn )
        throws NamingException
    {
        return getAttributes( getEntry( a_rdn ), null ) ;
    }


    public Attributes getAttributes( String a_rdn )
        throws NamingException
    {
        UnifiedBackend l_nexus = JndiProviderModule.getInstance().getNexus() ;
        return getAttributes( getEntry( l_nexus.getNormalizedName( a_rdn ) ), null ) ;
    }


    public Attributes getAttributes(Name name, String[] attrIds)
	    throws NamingException
    {
        return getAttributes(getEntry(name), attrIds) ;
    }


    public Attributes getAttributes(String name, String[] attrIds)
	    throws NamingException
    {
        UnifiedBackend l_nexus = JndiProviderModule.getInstance().getNexus() ;
        return getAttributes(l_nexus.getNormalizedName(name), attrIds) ;
    }


    public void modifyAttributes(Name name, int mod_op, Attributes attrs)
	    throws NamingException
    {
        LdapEntry l_entry = getEntry(name) ;
        modify(l_entry, attrs, mod_op) ;
        update(l_entry) ;
    }


    public void modifyAttributes(String name, int mod_op, Attributes attrs)
	    throws NamingException
    {
        UnifiedBackend l_nexus = JndiProviderModule.getInstance().getNexus() ;
        modifyAttributes(l_nexus.getNormalizedName(name), mod_op, attrs) ;
    }


    public void modifyAttributes( Name name, ModificationItem[] mods )
	    throws NamingException
    {
        LdapEntry l_entry = getEntry(name) ;

        for(int ii = 0; ii < mods.length; ii++) {
            ModificationItem l_mod = mods[ii] ;
            switch(l_mod.getModificationOp()) {
            case(DirContext.ADD_ATTRIBUTE):
                add(l_entry, l_mod.getAttribute()) ;
                break ;
            case(DirContext.REMOVE_ATTRIBUTE):
                remove(l_entry, l_mod.getAttribute()) ;
                break ;
            case(DirContext.REPLACE_ATTRIBUTE):
                replace(l_entry, l_mod.getAttribute()) ;
                break ;
            default:
                throw new NamingException("Unidentified modification "
                + "operation: " + l_mod.getModificationOp()) ;
            }
        }

        update(l_entry) ;
    }


    public void modifyAttributes(String name, ModificationItem[] mods)
	    throws NamingException
    {
        UnifiedBackend l_nexus = JndiProviderModule.getInstance().getNexus() ;
        modifyAttributes(l_nexus.getNormalizedName(name), mods) ;
    }


    public void bind(Name name, Object obj, Attributes attrs)
	    throws NamingException
    {
        LdapEntry l_entry = null ;

        if(null == obj && null == attrs) {
            throw new NamingException("Both the obj and attrs args were null. "
                + "At least one of these parameters must not be null.") ;
        }

        l_entry = newEntry(name) ;
        if(obj != null) {
            l_entry = getEntry(name) ;
            ContextHelper.populate(l_entry, name, obj) ;
        }

        if(attrs != null) {
            modify(l_entry, attrs, DirContext.ADD_ATTRIBUTE) ;
        }

        create(l_entry) ;
    }


    public void bind(String name, Object obj, Attributes attrs)
	    throws NamingException
    {
        UnifiedBackend l_nexus = JndiProviderModule.getInstance().getNexus() ;
        bind(l_nexus.getNormalizedName(name), obj, attrs) ;
    }


    public void rebind(Name name, Object obj, Attributes attrs)
	    throws NamingException
    {
        if(hasEntry(name)) {
            m_ctx.unbind(name) ;
        }

        bind(name, obj, attrs) ;
    }


    public void rebind(String name, Object obj, Attributes attrs)
	    throws NamingException
    {
        UnifiedBackend l_nexus = JndiProviderModule.getInstance().getNexus() ;
        rebind(l_nexus.getNormalizedName(name), obj, attrs) ;
    }


    public DirContext createSubcontext( Name a_name, Attributes a_attrs )
	    throws NamingException
    {
        LdapEntry l_entry = null ;

        if( null == a_attrs )
        {
            m_ctx.createSubcontext( a_name ) ;
        }

        l_entry = newEntry( a_name ) ;
        modify( l_entry, a_attrs, DirContext.ADD_ATTRIBUTE ) ;
        create( l_entry ) ;
        return new
            UnifiedLdapContext( m_ctx.getEnvironment(), l_entry ) ;
    }


    public DirContext createSubcontext( String a_name, Attributes a_attrs )
	    throws NamingException
    {
        UnifiedBackend l_nexus = JndiProviderModule.getInstance().getNexus() ;
        return createSubcontext( l_nexus.getName( a_name ), a_attrs) ;
    }


    ///////////////////////////////////
    // Search Method Implementations //
    ///////////////////////////////////


    /**
     * Single level scope search using this helper's context as the base DN.
     * It uses the default search controls specified by the default
     * SearchControls constructor.
     */
    public NamingEnumeration search(Name name,
	                Attributes matchingAttributes,
	                String[] attributesToReturn)
	    throws NamingException
    {
        Cursor l_cursor = null ;
        UnifiedBackend l_nexus = JndiProviderModule.getInstance().getNexus() ;

        // If matchingAttributes is null or empty then all children under this
        // context are returned.
        if(null == matchingAttributes || matchingAttributes.size() == 0) {
            l_cursor = listChildren(name) ;
        } else {
            l_cursor = search(getExprNode(matchingAttributes), name,
                SearchControls.ONELEVEL_SCOPE) ;
        }

        return new SearchResultEnumeration(m_ctx, l_nexus, l_cursor,
            attributesToReturn, new SearchControls()) ;
    }


    public NamingEnumeration search(String name,
	                Attributes matchingAttributes,
	                String[] attributesToReturn)
	    throws NamingException
    {
        UnifiedBackend l_nexus = JndiProviderModule.getInstance().getNexus() ;
        return search(l_nexus.getName(name), matchingAttributes,
            attributesToReturn) ;
    }


    public NamingEnumeration search(Name name,
	                Attributes matchingAttributes)
	    throws NamingException
    {
        return search(name, matchingAttributes, null) ;
    }


    public NamingEnumeration search(String name,
	                Attributes matchingAttributes)
	    throws NamingException
    {
        UnifiedBackend l_nexus = JndiProviderModule.getInstance().getNexus() ;
        return search(l_nexus.getName(name), matchingAttributes, null) ;
    }


    public NamingEnumeration search(Name name,
	                String filter,
	                SearchControls cons)
	    throws NamingException
    {
        Cursor l_cursor = search(getExprNode(filter), name,
            cons.getSearchScope()) ;
        UnifiedBackend l_nexus = JndiProviderModule.getInstance().getNexus() ;
        return new SearchResultEnumeration(m_ctx, l_nexus, l_cursor,
            null, cons) ;
    }


    public NamingEnumeration search(String name,
	                String filter,
	                SearchControls cons)
	    throws NamingException
    {
        UnifiedBackend l_nexus = JndiProviderModule.getInstance().getNexus() ;
        return search(l_nexus.getName(name), filter, cons) ;
    }


    public NamingEnumeration search(Name name,
	                String filterExpr,
	                Object[] filterArgs,
	                SearchControls cons)
	    throws NamingException
    {
        Cursor l_cursor = search(getExprNode(filterExpr, filterArgs), name,
            cons.getSearchScope()) ;
        UnifiedBackend l_nexus = JndiProviderModule.getInstance().getNexus() ;
        return new SearchResultEnumeration(m_ctx, l_nexus, l_cursor,
            null, cons) ;
    }


    public NamingEnumeration search(String name,
	                String filterExpr,
	                Object[] filterArgs,
	                SearchControls cons)
	    throws NamingException
    {
        UnifiedBackend l_nexus = JndiProviderModule.getInstance().getNexus() ;
        return search(l_nexus.getName(name), filterExpr, filterArgs, cons) ;
    }


    ////////////////////////////////////////////
    // Private & Pkg Friendly Utility Methods //
    ////////////////////////////////////////////


    /**
     * Adds attributes to an LdapEntry.
     *
     * @param a_entry the entry to add the attributes to
     * @param a_attribute the set of attributes and their values to add.
     */
    static void add( LdapEntry a_entry, Attribute a_attribute )
        throws NamingException
    {
        NamingEnumeration l_list = a_attribute.getAll() ;

        while( l_list.hasMore() )
        {
            a_entry.addValue( a_attribute.getID(), l_list.next() ) ;
        }
    }


    /**
     * Removes values from the entry which are specified in the Attribute.  When
     * the Attribute argument has no entries this method interprets the call to
     * mean remove all values associated with the id of a_attribute for complete
     * remove.  If there are some values associated with a_attribute then only
     * those values are removed in a partial manner.
     *
     * @param a_entry the LdapEntry to remove the values or attribute from
     * @param a_attribute the attribute and/or its values to remove.
     */
    static void remove( LdapEntry a_entry, Attribute a_attribute )
        throws NamingException
    {
        NamingEnumeration l_list = a_attribute.getAll() ;

        // If a_attributes does have value then we only remove those values and
        // not all of the values associated with the id of the attribute so this
        // is a partial remove scenario
        if( l_list.hasMore() )
        {
            while( l_list.hasMore() )
            {
                a_entry.removeValue( a_attribute.getID(), l_list.next() ) ;
            }
        }
        // Here all values associated with the attribute are removed.
        else
        {
            a_entry.removeValues( a_attribute.getID() ) ;
        }
    }


    static void replace( LdapEntry a_entry, Attribute a_attribute )
        throws NamingException
    {
        // Clear all values here for the attribute to replace.
        a_entry.removeValues( a_attribute.getID() ) ;

        // Now add all the new values for this attribute.
        NamingEnumeration l_list = a_attribute.getAll() ;
        while( l_list.hasMore() )
        {
            a_entry.addValue( a_attribute.getID(), l_list.next() ) ;
        }
    }


    /**
     * Modifies an entry according to a modification type using the attributes
     * specified within an Attributes parameter.
     *
     * @param a_entry the LdapEntry to modify
     * @param a_attributes the set of attributes and values to use in the
     * modification
     * @param a_modOp the modification type/operation to perform
     */
    static void modify( LdapEntry a_entry, Attributes a_attributes,
        int a_modOp )
        throws NamingException
    {
        NamingEnumeration l_list = a_attributes.getAll() ;

        while( l_list.hasMore() )
        {
            Attribute l_attr = ( Attribute ) l_list.next() ;

            switch( a_modOp )
            {
            case( DirContext.ADD_ATTRIBUTE ):
                add( a_entry, l_attr ) ;
                break ;
            case( DirContext.REMOVE_ATTRIBUTE ):
                remove( a_entry, l_attr ) ;
                break ;
            case( DirContext.REPLACE_ATTRIBUTE ):
                replace( a_entry, l_attr ) ;
                break ;
            default:
                // We put this exception into the OTHER category since the code
                // catches the problem and is not an OPERATIONSERROR.
                throw new NamingException(
                    "[80] Unidentified modification operation: " + a_modOp ) ;
            }
        }
    }


    void update( LdapEntry a_entry )
        throws NamingException
    {
        UnifiedBackend l_nexus = JndiProviderModule.getInstance().getNexus() ;

        try
        {
            l_nexus.update( a_entry ) ;
        }
        catch( BackendException be )
        {
            NamingException l_ne = new NamingException(
                "Failed on entry update for '" + a_entry.getEntryDN() + "'." ) ;
            l_ne.setRootCause( be ) ;
            throw l_ne ;
        }
    }


    void create( LdapEntry a_entry )
        throws NamingException
    {
        UnifiedBackend l_nexus = JndiProviderModule.getInstance().getNexus() ;

        try
        {
            l_nexus.create( a_entry ) ;
        }
        catch( BackendException be )
        {
            NamingException l_ne = new NamingException(
                "Failed on entry create for '" + a_entry.getEntryDN() + "'." ) ;
            l_ne.setRootCause( be ) ;
            throw l_ne ;
        }
    }


    void delete( LdapEntry a_entry )
        throws NamingException
    {
        UnifiedBackend l_nexus = JndiProviderModule.getInstance().getNexus() ;

        try
        {
            l_nexus.delete( a_entry ) ;
        }
        catch( BackendException be )
        {
            NamingException l_ne = new NamingException(
                "Failed on entry delete for '" + a_entry.getEntryDN() + "'." ) ;
            l_ne.setRootCause( be ) ;
            throw l_ne ;
        }
    }


    /**
     * Utility method to create a new invalid entry that is not yet created in
     * the backend.
     *
     * @param a_name the non-normalized relative dn of the new entry to
     */
    LdapEntry newEntry( Name a_name )
        throws NamingException
    {
        UnifiedBackend l_nexus = JndiProviderModule.getInstance().getNexus() ;
        LdapEntry l_entry = m_ctx.getEntry() ;
        Name l_dn = new LdapName() ;
        l_dn.addAll( l_entry.getUnNormalizedDN() ) ;
        l_dn.addAll( a_name ) ;

        try
        {
            return l_nexus.newEntry( l_dn.toString() ) ;
        }
        catch( BackendException be )
        {
            NamingException l_ne = new NamingException( "Failed on newEntry( "
                + l_dn + " ) due to backend failure." ) ;
            l_ne.setRootCause( be ) ;
            throw l_ne ;
        }
    }


    /**
     * Gets an LdapEntry from the appropriate DIB off the nexus using a name
     * relative to the entry associated with the DirContext of this helper.
     */
    LdapEntry getEntry( Name a_rdn )
        throws NamingException
    {
        UnifiedBackend l_nexus = JndiProviderModule.getInstance().getNexus() ;
        LdapEntry l_entry = m_ctx.getEntry() ;
        Name l_dn = new LdapName() ;
        l_dn.addAll( l_nexus.getName( l_entry.getEntryDN() ) ) ;
        l_dn.addAll( a_rdn ) ;

        try
        {
            return l_nexus.read( l_dn ) ;
        }
        catch( BackendException be )
        {
            NamingException l_ne = new NamingException( "Failed on entry read "
                + "for '" + l_dn + "'." ) ;
            l_ne.setRootCause( be ) ;
            throw l_ne ;
        }
    }


    /**
     * Checks for a entry from the appropriate DIB off the nexus using a name
     * relative to the entry associated with the DirContext of this helper.
     */
    boolean hasEntry( Name a_rdn )
        throws NamingException
    {
        UnifiedBackend l_nexus = JndiProviderModule.getInstance().getNexus() ;
        LdapEntry l_entry = m_ctx.getEntry() ;
        Name l_dn = new LdapName() ;
        l_dn.addAll( l_entry.getNormalizedDN() ) ;
        l_dn.addAll( a_rdn ) ;

        try
        {
            return l_nexus.hasEntry( l_dn ) ;
        }
        catch( BackendException be )
        {
            NamingException l_ne = new NamingException( "Failed on hasEntry "
                + "for '" + l_dn + "'." ) ;
            l_ne.setRootCause( be ) ;
            throw l_ne ;
        }
    }


    Cursor listChildren( Name a_rdn )
        throws NamingException
    {
        UnifiedBackend l_nexus = JndiProviderModule.getInstance().getNexus() ;
        LdapEntry l_entry = m_ctx.getEntry() ;
        Name l_dn = new LdapName() ;
        l_dn.addAll( l_entry.getNormalizedDN() ) ;
        l_dn.addAll( a_rdn ) ;

        try
        {
            return l_nexus.listChildren( l_dn ) ;
        }
        catch( BackendException be )
        {
            NamingException l_ne = new NamingException(
                "Failed on listChildren for '" + l_dn + "'." ) ;
            l_ne.setRootCause( be ) ;
            throw l_ne ;
        }
    }


    Cursor search(ExprNode a_filter, Name a_rn, int a_scope)
        throws NamingException
    {
        UnifiedBackend l_nexus = JndiProviderModule.getInstance().getNexus() ;
        LdapEntry l_entry = m_ctx.getEntry() ;
        Name l_dn = new LdapName() ;
        l_dn.addAll( l_entry.getNormalizedDN() ) ;
        l_dn.addAll( a_rn ) ;

        try {
            return l_nexus.search(a_filter, l_dn, a_scope) ;
        } catch(BackendException e) {
            NamingException l_ne = new NamingException("Failed on search "
                + "for '" + l_dn + "'.") ;
            l_ne.setRootCause(e) ;
            throw l_ne ;
        }
    }


    /**
     * @return attribute in a_entry for an attribute identitifier if the
     * attribute exists.
     * @throws NoSuchAttributeException if a_entry does not contain the
     * requested attribute.
     */
    static Attribute getAttribute(LdapEntry a_entry, String a_attribName)
        throws NoSuchAttributeException
    {
        BasicAttribute l_attrib = new BasicAttribute(a_attribName) ;
        Collection l_values = (Collection) a_entry.getMultiValue(a_attribName) ;

        // SCREAM if attribute does not exist.
        if(null == l_values) {
            throw new NoSuchAttributeException("Attribute " + a_attribName
                + " does not exist within entry '"
                + a_entry.getEntryDN() + "'") ;
        }

        Iterator l_list = l_values.iterator() ;
        while(l_list.hasNext()) {
            l_attrib.add(l_list.next()) ;
        }

        return l_attrib ;
    }


    /**
     * Generates the set of attributes for an entry using a specified attribute
     * list to select for specific attributes.  If the attribute list array is
     * null all attributes are packaged and returned.
     */
    static Attributes getAttributes(LdapEntry a_entry, String [] attribList)
        throws NamingException
    {
        // Don't need this bullshit of the entry does it for us already.
        HashSet l_lut = null ;

        // Optionally create fast normalized lookup table for attribute ids
        if(attribList != null) {
            // Hash lowercased versions of attributes so we do not generate strs
            // over and over again while searching through attribute list.
            l_lut = new HashSet(attribList.length) ;
            for(int ii = 0; ii < attribList.length; ii++) {
                String l_lower = attribList[ii].toLowerCase() ;
                l_lut.add(l_lower) ;
            }
        }

        // Iterate through list of attributes and add them to the new Attributes
        // instance based on the attribute list array if it was not null
        // otherwise we return all attributes.
        BasicAttributes l_attributes = new BasicAttributes(true) ;
        Iterator l_list = a_entry.attributes().iterator() ;
        while(l_list.hasNext()) {
            String l_attribName = (String) l_list.next() ;

            // If l_lut exists we add the attribute only if it is in the lut
            if(l_lut != null && l_lut.contains(l_attribName.toLowerCase())) {
                l_attributes.put(getAttribute(a_entry, l_attribName)) ;
            // Lut is null so we add all attributes.
            } else if(null == l_lut) {
                l_attributes.put(getAttribute(a_entry, l_attribName)) ;
            }

            // If lut is not null but the attribute name is not present in
            // it as a key then we do not include the attribute in the response.
        }

        return l_attributes ;
    }


    /**
     * Builds an expression tree that matches for a set of attribute value
     * pairs under one AND expression.  If an attribute does not have any
     * values then it is a presence assertion node.  Otherwise the attributes
     * values are used in as many equality expressions as there are values using
     * that attributes identifier.
     */
    ExprNode getExprNode(Attributes l_attributes)
        throws NamingException
    {
        ArrayList l_nodes = new ArrayList() ;
        NamingEnumeration l_attrList = l_attributes.getAll() ;
        while(l_attrList.hasMore()) {
            Attribute l_attr = (Attribute) l_attrList.next() ;

            if(l_attr.size() == 0) {
                l_nodes.add(new PresenceNode(l_attr.getID())) ;
            } else {
                NamingEnumeration l_values = l_attr.getAll() ;
                while(l_values.hasMore()) {
                    String l_val = (String) l_values.next() ;
                    l_nodes.add(new SimpleNode(l_attr.getID(), l_val,
                        SimpleNode.EQUALITY)) ;
                }
            }
        }

        return new BranchNode(BranchNode.AND, l_nodes) ;
    }


    static FilterParser s_parser = null ;
    ExprNode getExprNode(String a_filter)
        throws NamingException
    {
        if(null == s_parser) {
            try {
                s_parser = new FilterParserImpl() ;
            } catch(IOException e) {
                NamingException l_ne = new NamingException("Failed on "
                    + "FilterParser init.") ;
                l_ne.setRootCause(e) ;
                throw l_ne ;
            }
        }

        try {
            return s_parser.parse(a_filter) ;
        } catch(ParseException e) {
            NamingException l_ne = new NamingException("Failed on "
                + "FilterParser parse of filter:\n'" + a_filter + "'") ;
            l_ne.setRootCause(e) ;
            throw l_ne ;
        } catch(IOException e) {
            NamingException l_ne = new NamingException("Failed on "
                + "FilterParser parse of filter:\n'" + a_filter + "'") ;
            l_ne.setRootCause(e) ;
            throw l_ne ;
        }
    }


    ExprNode getExprNode(String a_filter, Object [] a_filterArgs)
        throws NamingException
    {
        StringBuffer l_buf = new StringBuffer() ;
        StringBuffer l_tmp = new StringBuffer() ;
        for(int ii = 0 ; ii < a_filter.length() ; ii++) {
            if(a_filter.charAt(ii) == '{') {
                ii++ ;
                while(a_filter.charAt(ii) != '}') {
                    l_tmp.append(a_filter.charAt(ii)) ;
                    ii++ ;
                }

                int l_arg = Integer.parseInt(l_tmp.toString()) ;
                l_buf.append(a_filterArgs[l_arg].toString()) ;
            } else {
                l_buf.append(a_filter.charAt(ii)) ;
            }
        }

        return getExprNode(l_buf.toString()) ;
    }
}
