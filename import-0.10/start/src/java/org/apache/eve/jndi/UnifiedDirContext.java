/*
 * $Id: UnifiedDirContext.java,v 1.10 2003/08/22 21:15:55 akarasulu Exp $
 *
 * -- (c) LDAPd Group                                                    --
 * -- Please refer to the LICENSE.txt file in the root directory of      --
 * -- any LDAPd project for copyright and distribution information.      --
 *
 */

package org.apache.eve.jndi ;


import java.util.Hashtable ;

import javax.naming.Name ;
import javax.naming.Context ;
import javax.naming.NamingException ;
import javax.naming.NamingEnumeration ;

import javax.naming.directory.Attributes ;
import javax.naming.directory.DirContext ;
import javax.naming.directory.SearchControls ;
import javax.naming.directory.ModificationItem ;

import org.apache.eve.backend.LdapEntry ;


/**
 * The internal server side LdapContext implementation used to access and
 * modify server entries.  Most bind operations will be unsupported since
 * instances of this context are already bound.  This context is the main
 * class of the internal server side provider.  It is used whenever an
 * initial context is requested within stored procedures or within a
 * server trigger.  It is also the basis for server side communication between
 * a server host with an embedded ldapd server component.  The UnifiedBackend
 * as well as a public Kernel interface will expose a handle to the RootDSE
 * using this LdapContext implementation.
 *
 * Note that the documentation used by the Java APIs for Context, DirContext
 * and LdapContext are copied here for convenience.   Eventually we can make
 * these chunks of javadocs @see references to the respective interfaces.
 * 
 * @author <a href="mailto:aok123@bellsouth.net">Alex Karasulu</a>
 * @author $Author: akarasulu $
 * @version $Revision: 1.10 $
 */
public abstract class UnifiedDirContext
    extends UnifiedContext
    implements DirContext
{
    private final DirContextHelper m_helper ;


    protected UnifiedDirContext( Hashtable an_environment )
    {
        super( an_environment ) ;
        m_helper = new DirContextHelper( this ) ;
    }


    protected UnifiedDirContext( Hashtable an_environment, LdapEntry a_entry )
        throws NamingException
    {
        super( an_environment, a_entry ) ;
        m_helper = new DirContextHelper( this ) ;
    }


    ////////////////////////////////
    // DirContext Implementations //
    ////////////////////////////////


    /**
     * Retrieves all of the attributes associated with a named object.
     * See the class description regarding attribute models, attribute
     * type names, and operational attributes.
     *
     * @param name
     *		the name of the object from which to retrieve attributes
     * @return	the set of attributes associated with <code>name</code>.
     *		Returns an empty attribute set if name has no attributes;
     *		never null.
     * @throws	NamingException if a naming exception is encountered
     *
     * @see #getAttributes(String)
     * @see #getAttributes(Name, String[])
     */
    public Attributes getAttributes(Name name) throws NamingException
    {
        return m_helper.getAttributes(name) ;
    }


    /**
     * Retrieves all of the attributes associated with a named object.
     * See {@link #getAttributes(Name)} for details.
     *
     * @param name
     *		the name of the object from which to retrieve attributes
     * @return	the set of attributes associated with <code>name</code>
     *
     * @throws	NamingException if a naming exception is encountered
     */
    public Attributes getAttributes(String name) throws NamingException
    {
        return m_helper.getAttributes(name) ;
    }


    /**
     * Retrieves selected attributes associated with a named object.
     * See the class description regarding attribute models, attribute
     * type names, and operational attributes.
     *
     * <p> If the object does not have an attribute
     * specified, the directory will ignore the nonexistent attribute
     * and return those requested attributes that the object does have.
     *
     * <p> A directory might return more attributes than was requested
     * (see <strong>Attribute Type Names</strong> in the class description),
     * but is not allowed to return arbitrary, unrelated attributes.
     *
     * <p> See also <strong>Operational Attributes</strong> in the class
     * description.
     *
     * @param name
     *		the name of the object from which to retrieve attributes
     * @param attrIds
     *		the identifiers of the attributes to retrieve.
     * 		null indicates that all attributes should be retrieved;
     *     	an empty array indicates that none should be retrieved.
     * @return	the requested attributes; never null
     *
     * @throws	NamingException if a naming exception is encountered
     */
    public Attributes getAttributes(Name name, String[] attrIds)
	    throws NamingException
    {
        return m_helper.getAttributes(name, attrIds) ;
    }


    /**
     * Retrieves selected attributes associated with a named object.
     * See {@link #getAttributes(Name, String[])} for details.
     *
     * @param name
     *		The name of the object from which to retrieve attributes
     * @param attrIds
     *		the identifiers of the attributes to retrieve.
     * 		null indicates that all attributes should be retrieved;
     *     	an empty array indicates that none should be retrieved.
     * @return	the requested attributes; never null
     *
     * @throws	NamingException if a naming exception is encountered
     */
    public Attributes getAttributes(String name, String[] attrIds)
	    throws NamingException
    {
        return m_helper.getAttributes(name, attrIds) ;
    }


    /**
     * Modifies the attributes associated with a named object.
     * The order of the modifications is not specified.  Where
     * possible, the modifications are performed atomically.
     *
     * @param name
     *		the name of the object whose attributes will be updated
     * @param mod_op
     *		the modification operation, one of:
     *			<code>ADD_ATTRIBUTE</code>,
     *			<code>REPLACE_ATTRIBUTE</code>,
     *			<code>REMOVE_ATTRIBUTE</code>.
     * @param attrs
     *		the attributes to be used for the modification; may not be null
     *
     * @throws	javax.naming.directory.AttributeModificationException if the modification cannot
     *		be completed successfully
     * @throws	NamingException if a naming exception is encountered
     *
     * @see #modifyAttributes(Name, ModificationItem[])
     */
    public void modifyAttributes(Name name, int mod_op, Attributes attrs)
	    throws NamingException
    {
        m_helper.modifyAttributes(name, mod_op, attrs) ;
    }


    /**
     * Modifies the attributes associated with a named object.
     * See {@link #modifyAttributes(Name, int, Attributes)} for details.
     *
     * @param name
     *		the name of the object whose attributes will be updated
     * @param mod_op
     *		the modification operation, one of:
     *			<code>ADD_ATTRIBUTE</code>,
     *			<code>REPLACE_ATTRIBUTE</code>,
     *			<code>REMOVE_ATTRIBUTE</code>.
     * @param attrs
     *		the attributes to be used for the modification; map not be null
     *
     * @throws	javax.naming.directory.AttributeModificationException if the modification cannot
     *		be completed successfully
     * @throws	NamingException if a naming exception is encountered
     */
    public void modifyAttributes(String name, int mod_op, Attributes attrs)
	    throws NamingException
    {
        m_helper.modifyAttributes(name, mod_op, attrs) ;
    }


    /**
     * Modifies the attributes associated with a named object using
     * an ordered list of modifications.
     * The modifications are performed
     * in the order specified.  Each modification specifies a
     * modification operation code and an attribute on which to
     * operate.  Where possible, the modifications are
     * performed atomically.
     *
     * @param name
     *		the name of the object whose attributes will be updated
     * @param mods
     *		an ordered sequence of modifications to be performed;
     *		may not be null
     *
     * @throws	javax.naming.directory.AttributeModificationException if the modifications
     *		cannot be completed successfully
     * @throws	NamingException if a naming exception is encountered
     *
     * @see #modifyAttributes(Name, int, Attributes)
     * @see <{ModificationItem}>
     */
    public void modifyAttributes(Name name, ModificationItem[] mods)
	    throws NamingException
    {
        m_helper.modifyAttributes(name, mods) ;
    }


    /**
     * Modifies the attributes associated with a named object using
     * an ordered list of modifications.
     * See {@link #modifyAttributes(Name, ModificationItem[])} for details.
     *
     * @param name
     *		the name of the object whose attributes will be updated
     * @param mods
     *		an ordered sequence of modifications to be performed;
     *		may not be null
     *
     * @throws	javax.naming.directory.AttributeModificationException if the modifications
     *		cannot be completed successfully
     * @throws	NamingException if a naming exception is encountered
     */
    public void modifyAttributes(String name, ModificationItem[] mods)
	    throws NamingException
    {
        m_helper.modifyAttributes(name, mods) ;
    }


    /**
     * Binds a name to an object, along with associated attributes.
     * If <tt>attrs</tt> is null, the resulting binding will have
     * the attributes associated with <tt>obj</tt> if <tt>obj</tt> is a
     * <tt>DirContext</tt>, and no attributes otherwise.
     * If <tt>attrs</tt> is non-null, the resulting binding will have
     * <tt>attrs</tt> as its attributes; any attributes associated with
     * <tt>obj</tt> are ignored.
     *
     * @param name
     *		the name to bind; may not be empty
     * @param obj
     *		the object to bind; possibly null
     * @param attrs
     *		the attributes to associate with the binding
     *
     * @throws	javax.naming.NameAlreadyBoundException if name is already bound
     * @throws	javax.naming.directory.InvalidAttributesException if some "mandatory" attributes
     *		of the binding are not supplied
     * @throws	NamingException if a naming exception is encountered
     *
     * @see Context#bind(Name, Object)
     * @see #rebind(Name, Object, Attributes)
     */
    public void bind(Name name, Object obj, Attributes attrs)
	    throws NamingException
    {
        m_helper.bind(name, obj, attrs) ;
    }


    /**
     * Binds a name to an object, along with associated attributes.
     * See {@link #bind(Name, Object, Attributes)} for details.
     *
     * @param name
     *		the name to bind; may not be empty
     * @param obj
     *		the object to bind; possibly null
     * @param attrs
     *		the attributes to associate with the binding
     *
     * @throws	javax.naming.NameAlreadyBoundException if name is already bound
     * @throws	javax.naming.directory.InvalidAttributesException if some "mandatory" attributes
     *		of the binding are not supplied
     * @throws	NamingException if a naming exception is encountered
     */
    public void bind(String name, Object obj, Attributes attrs)
	    throws NamingException
    {
        m_helper.bind(name, obj, attrs) ;
    }


    /**
     * Binds a name to an object, along with associated attributes,
     * overwriting any existing binding.
     * If <tt>attrs</tt> is null and <tt>obj</tt> is a <tt>DirContext</tt>,
     * the attributes from <tt>obj</tt> are used.
     * If <tt>attrs</tt> is null and <tt>obj</tt> is not a <tt>DirContext</tt>,
     * any existing attributes associated with the object already bound
     * in the directory remain unchanged.
     * If <tt>attrs</tt> is non-null, any existing attributes associated with
     * the object already bound in the directory are removed and <tt>attrs</tt>
     * is associated with the named object.  If <tt>obj</tt> is a
     * <tt>DirContext</tt> and <tt>attrs</tt> is non-null, the attributes
     * of <tt>obj</tt> are ignored.
     *
     * @param name
     *		the name to bind; may not be empty
     * @param obj
     *		the object to bind; possibly null
     * @param attrs
     *		the attributes to associate with the binding
     *
     * @throws	javax.naming.directory.InvalidAttributesException if some "mandatory" attributes
     *		of the binding are not supplied
     * @throws	NamingException if a naming exception is encountered
     *
     * @see Context#bind(Name, Object)
     * @see #bind(Name, Object, Attributes)
     */
    public void rebind(Name name, Object obj, Attributes attrs)
	    throws NamingException
    {
        m_helper.rebind(name, obj, attrs) ;
    }


    /**
     * Binds a name to an object, along with associated attributes,
     * overwriting any existing binding.
     * See {@link #rebind(Name, Object, Attributes)} for details.
     *
     * @param name
     *		the name to bind; may not be empty
     * @param obj
     *		the object to bind; possibly null
     * @param attrs
     *		the attributes to associate with the binding
     *
     * @throws	javax.naming.directory.InvalidAttributesException if some "mandatory" attributes
     *		of the binding are not supplied
     * @throws	NamingException if a naming exception is encountered
     */
    public void rebind(String name, Object obj, Attributes attrs)
	    throws NamingException
    {
        m_helper.rebind(name, obj, attrs) ;
    }


    /**
     * Creates and binds a new context, along with associated attributes.
     * This method creates a new subcontext with the given name, binds it in
     * the target context (that named by all but terminal atomic
     * component of the name), and associates the supplied attributes
     * with the newly created object.
     * All intermediate and target contexts must already exist.
     * If <tt>attrs</tt> is null, this method is equivalent to
     * <tt>Context.createSubcontext()</tt>.
     *
     * @param a_name
     *		the name of the context to create; may not be empty
     * @param a_attrs
     *		the attributes to associate with the newly created context
     * @return	the newly created context
     *
     * @throws	javax.naming.NameAlreadyBoundException if the name is already bound
     * @throws	javax.naming.directory.InvalidAttributesException if <code>attrs</code> does not
     *		contain all the mandatory attributes required for creation
     * @throws	NamingException if a naming exception is encountered
     *
     * @see Context#createSubcontext(Name)
     */
    public DirContext createSubcontext( Name a_name, Attributes a_attrs )
	    throws NamingException
    {
        return m_helper.createSubcontext( a_name, a_attrs) ;
    }


    /**
     * Creates and binds a new context, along with associated attributes.
     * See {@link #createSubcontext(Name, Attributes)} for details.
     *
     * @param a_name
     *		the name of the context to create; may not be empty
     * @param a_attrs
     *		the attributes to associate with the newly created context
     * @return	the newly created context
     *
     * @throws	javax.naming.NameAlreadyBoundException if the name is already bound
     * @throws	javax.naming.directory.InvalidAttributesException if <code>attrs</code> does not
     *		contain all the mandatory attributes required for creation
     * @throws	NamingException if a naming exception is encountered
     */
    public DirContext createSubcontext( String a_name, Attributes a_attrs )
	    throws NamingException
    {
        return m_helper.createSubcontext( a_name, a_attrs ) ;
    }


    ///////////////////////
    // Schema Operations //
    ///////////////////////


    /**
     * Retrieves the schema associated with the named object.
     * The schema describes rules regarding the structure of the namespace
     * and the attributes stored within it.  The schema
     * specifies what types of objects can be added to the directory and where
     * they can be added; what mandatory and optional attributes an object
     * can have. The range of support for schemas is directory-specific.
     *
     * <p> This method returns the root of the schema information tree
     * that is applicable to the named object. Several named objects
     * (or even an entire directory) might share the same schema.
     *
     * <p> Issues such as structure and contents of the schema tree,
     * permission to modify to the contents of the schema
     * tree, and the effect of such modifications on the directory
     * are dependent on the underlying directory.
     *
     * @param name
     *		the name of the object whose schema is to be retrieved
     * @return	the schema associated with the context; never null
     * @throws	javax.naming.OperationNotSupportedException if schema not supported
     * @throws	NamingException if a naming exception is encountered
     */
    public DirContext getSchema(Name name) throws NamingException
    {
        throw new UnsupportedOperationException() ;
    }


    /**
     * Retrieves the schema associated with the named object.
     * See {@link #getSchema(Name)} for details.
     *
     * @param name
     *		the name of the object whose schema is to be retrieved
     * @return	the schema associated with the context; never null
     * @throws	javax.naming.OperationNotSupportedException if schema not supported
     * @throws	NamingException if a naming exception is encountered
     */
    public DirContext getSchema(String name) throws NamingException
    {
        throw new UnsupportedOperationException() ;
    }


    /**
     * Retrieves a context containing the schema objects of the
     * named object's class definitions.
     *<p>
     * One category of information found in directory schemas is
     * <em>class definitions</em>.  An "object class" definition
     * specifies the object's <em>type</em> and what attributes (mandatory
     * and optional) the object must/can have. Note that the term
     * "object class" being referred to here is in the directory sense
     * rather than in the Java sense.
     * For example, if the named object is a directory object of
     * "Person" class, <tt>getSchemaClassDefinition()</tt> would return a
     * <tt>DirContext</tt> representing the (directory's) object class
     * definition of "Person".
     *<p>
     * The information that can be retrieved from an object class definition
     * is directory-dependent.
     *<p>
     * Prior to JNDI 1.2, this method
     * returned a single schema object representing the class definition of
     * the named object.
     * Since JNDI 1.2, this method returns a <tt>DirContext</tt> containing
     * all of the named object's class definitions.
     *
     * @param name
     *		the name of the object whose object class
     *		definition is to be retrieved
     * @return	the <tt>DirContext</tt> containing the named
     *		object's class definitions; never null
     *
     * @throws	javax.naming.OperationNotSupportedException if schema not supported
     * @throws	NamingException if a naming exception is encountered
     */
    public DirContext getSchemaClassDefinition(Name name)
	    throws NamingException
    {
        throw new UnsupportedOperationException() ;
    }


    /**
     * Retrieves a context containing the schema objects of the
     * named object's class definitions.
     * See {@link #getSchemaClassDefinition(Name)} for details.
     *
     * @param name
     *		the name of the object whose object class
     *		definition is to be retrieved
     * @return	the <tt>DirContext</tt> containing the named
     *		object's class definitions; never null
     *
     * @throws	javax.naming.OperationNotSupportedException if schema not supported
     * @throws	NamingException if a naming exception is encountered
     */
    public DirContext getSchemaClassDefinition(String name)
	    throws NamingException
    {
        throw new UnsupportedOperationException() ;
    }


    ///////////////////////
    // Search Operations //
    ///////////////////////


    /**
     * Searches in a single context for objects that contain a
     * specified set of attributes, and retrieves selected attributes.
     * The search is performed using the default
     * <code>SearchControls</code> settings.
     * <p>
     * For an object to be selected, each attribute in
     * <code>matchingAttributes</code> must match some attribute of the
     * object.  If <code>matchingAttributes</code> is empty or
     * null, all objects in the target context are returned.
     *<p>
     * An attribute <em>A</em><sub>1</sub> in
     * <code>matchingAttributes</code> is considered to match an
     * attribute <em>A</em><sub>2</sub> of an object if
     * <em>A</em><sub>1</sub> and <em>A</em><sub>2</sub> have the same
     * identifier, and each value of <em>A</em><sub>1</sub> is equal
     * to some value of <em>A</em><sub>2</sub>.  This implies that the
     * order of values is not significant, and that
     * <em>A</em><sub>2</sub> may contain "extra" values not found in
     * <em>A</em><sub>1</sub> without affecting the comparison.  It
     * also implies that if <em>A</em><sub>1</sub> has no values, then
     * testing for a match is equivalent to testing for the presence
     * of an attribute <em>A</em><sub>2</sub> with the same
     * identifier.
     *<p>
     * The precise definition of "equality" used in comparing attribute values
     * is defined by the underlying directory service.  It might use the
     * <code>Object.equals</code> method, for example, or might use a schema
     * to specify a different equality operation.
     * For matching based on operations other than equality (such as
     * substring comparison) use the version of the <code>search</code>
     * method that takes a filter argument.
     * <p>
     * When changes are made to this <tt>DirContext</tt>,
     * the effect on enumerations returned by prior calls to this method
     * is undefined.
     *<p>
     * If the object does not have the attribute
     * specified, the directory will ignore the nonexistent attribute
     * and return the requested attributes that the object does have.
     *<p>
     * A directory might return more attributes than was requested
     * (see <strong>Attribute Type Names</strong> in the class description),
     * but is not allowed to return arbitrary, unrelated attributes.
     *<p>
     * See also <strong>Operational Attributes</strong> in the class
     * description.
     *
     * @param name
     *		the name of the context to search
     * @param matchingAttributes
     *		the attributes to search for.  If empty or null,
     *		all objects in the target context are returned.
     * @param attributesToReturn
     *		the attributes to return.  null indicates that
     *		all attributes are to be returned;
     *		an empty array indicates that none are to be returned.
     * @return
     *		a non-null enumeration of <tt>SearchResult</tt> objects.
     *		Each <tt>SearchResult</tt> contains the attributes
     *		identified by <code>attributesToReturn</code>
     *		and the name of the corresponding object, named relative
     * 		to the context named by <code>name</code>.
     * @throws	NamingException if a naming exception is encountered
     *
     * @see <{SearchControls}>
     * @see javax.naming.directory.SearchResult
     * @see #search(Name, String, Object[], SearchControls)
     */
    public NamingEnumeration search(Name name,
	                Attributes matchingAttributes,
	                String[] attributesToReturn)
	    throws NamingException
    {
        throw new UnsupportedOperationException() ;
    }


    /**
     * Searches in a single context for objects that contain a
     * specified set of attributes, and retrieves selected attributes.
     * See {@link #search(Name, Attributes, String[])} for details.
     *
     * @param name
     *		the name of the context to search
     * @param matchingAttributes
     *		the attributes to search for
     * @param attributesToReturn
     *		the attributes to return
     * @return	a non-null enumeration of <tt>SearchResult</tt> objects
     * @throws	NamingException if a naming exception is encountered
     */
    public NamingEnumeration search(String name,
	                Attributes matchingAttributes,
	                String[] attributesToReturn)
	    throws NamingException
    {
        throw new UnsupportedOperationException() ;
    }


    /**
     * Searches in a single context for objects that contain a
     * specified set of attributes.
     * This method returns all the attributes of such objects.
     * It is equivalent to supplying null as
     * the <tt>atributesToReturn</tt> parameter to the method
     * <code>search(Name, Attributes, String[])</code>.
     * <br>
     * See {@link #search(Name, Attributes, String[])} for a full description.
     *
     * @param name
     *		the name of the context to search
     * @param matchingAttributes
     *		the attributes to search for
     * @return	an enumeration of <tt>SearchResult</tt> objects
     * @throws	NamingException if a naming exception is encountered
     *
     * @see #search(Name, Attributes, String[])
     */
    public NamingEnumeration search(Name name,
	                Attributes matchingAttributes)
	    throws NamingException
    {
        throw new UnsupportedOperationException() ;
    }


    /**
     * Searches in a single context for objects that contain a
     * specified set of attributes.
     * See {@link #search(Name, Attributes)} for details.
     *
     * @param name
     *		the name of the context to search
     * @param matchingAttributes
     *		the attributes to search for
     * @return	an enumeration of <tt>SearchResult</tt> objects
     * @throws	NamingException if a naming exception is encountered
     */
    public NamingEnumeration search(String name,
	                Attributes matchingAttributes)
	    throws NamingException
    {
        throw new UnsupportedOperationException() ;
    }


    /**
     * Searches in the named context or object for entries that satisfy the
     * given search filter.  Performs the search as specified by
     * the search controls.
     * <p>
     * The format and interpretation of <code>filter</code> follows RFC 2254
     * with the
     * following interpretations for <code>attr</code> and <code>value</code>
     * mentioned in the RFC.
     * <p>
     * <code>attr</code> is the attribute's identifier.
     * <p>
     * <code>value</code> is the string representation the attribute's value.
     * The translation of this string representation into the attribute's value
     * is directory-specific.
     * <p>
     * For the assertion "someCount=127", for example, <code>attr</code>
     * is "someCount" and <code>value</code> is "127".
     * The provider determines, based on the attribute ID ("someCount")
     * (and possibly its schema), that the attribute's value is an integer.
     * It then parses the string "127" appropriately.
     *<p>
     * Any non-ASCII characters in the filter string should be
     * represented by the appropriate Java (Unicode) characters, and
     * not encoded as UTF-8 octets.  Alternately, the
     * "backslash-hexcode" notation described in RFC 2254 may be used.
     *<p>
     * If the directory does not support a string representation of
     * some or all of its attributes, the form of <code>search</code> that
     * accepts filter arguments in the form of Objects can be used instead.
     * The service provider for such a directory would then translate
     * the filter arguments to its service-specific representation
     * for filter evaluation.
     * See <code>search(Name, String, Object[], SearchControls)</code>.
     * <p>
     * RFC 2254 defines certain operators for the filter, including substring
     * matches, equality, approximate match, greater than, less than.  These
     * operators are mapped to operators with corresponding semantics in the
     * underlying directory. For example, for the equals operator, suppose
     * the directory has a matching rule defining "equality" of the
     * attributes in the filter. This rule would be used for checking
     * equality of the attributes specified in the filter with the attributes
     * of objects in the directory. Similarly, if the directory has a
     * matching rule for ordering, this rule would be used for
     * making "greater than" and "less than" comparisons.
     *<p>
     * Not all of the operators defined in RFC 2254 are applicable to all
     * attributes.  When an operator is not applicable, the exception
     * <code>InvalidSearchFilterException</code> is thrown.
     * <p>
     * The result is returned in an enumeration of <tt>SearchResult</tt>s.
     * Each <tt>SearchResult</tt> contains the name of the object
     * and other information about the object (see SearchResult).
     * The name is either relative to the target context of the search
     * (which is named by the <code>name</code> parameter), or
     * it is a URL string. If the target context is included in
     * the enumeration (as is possible when
     * <code>cons</code> specifies a search scope of
     * <code>SearchControls.OBJECT_SCOPE</code> or
     * <code>SearchControls.SUBSTREE_SCOPE</code>), its name is the empty
     * string. The <tt>SearchResult</tt> may also contain attributes of the
     * matching object if the <tt>cons</tt> argument specified that attributes
     * be returned.
     *<p>
     * If the object does not have a requested attribute, that
     * nonexistent attribute will be ignored.  Those requested
     * attributes that the object does have will be returned.
     *<p>
     * A directory might return more attributes than were requested
     * (see <strong>Attribute Type Names</strong> in the class description)
     * but is not allowed to return arbitrary, unrelated attributes.
     *<p>
     * See also <strong>Operational Attributes</strong> in the class
     * description.
     *
     * @param name
     *		the name of the context or object to search
     * @param filter
     *		the filter expression to use for the search; may not be null
     * @param cons
     *		the search controls that control the search.  If null,
     *		the default search controls are used (equivalent
     *		to <tt>(new SearchControls())</tt>).
     * @return	an enumeration of <tt>SearchResult</tt>s of
     * 		the objects that satisfy the filter; never null
     *
     * @throws	javax.naming.directory.InvalidSearchFilterException if the search filter specified is
     *		not supported or understood by the underlying directory
     * @throws	javax.naming.directory.InvalidSearchControlsException if the search controls
     * 		contain invalid settings
     * @throws	NamingException if a naming exception is encountered
     *
     * @see #search(Name, String, Object[], SearchControls)
     * @see <{SearchControls}>
     * @see javax.naming.directory.SearchResult
     */
    public NamingEnumeration search(Name name,
	                String filter,
	                SearchControls cons)
	    throws NamingException
    {
        throw new UnsupportedOperationException() ;
    }


    /**
     * Searches in the named context or object for entries that satisfy the
     * given search filter.  Performs the search as specified by
     * the search controls.
     * See {@link #search(Name, String, SearchControls)} for details.
     *
     * @param name
     *		the name of the context or object to search
     * @param filter
     *		the filter expression to use for the search; may not be null
     * @param cons
     *		the search controls that control the search.  If null,
     *		the default search controls are used (equivalent
     *		to <tt>(new SearchControls())</tt>).
     *
     * @return	an enumeration of <tt>SearchResult</tt>s for
     * 		the objects that satisfy the filter.
     * @throws	javax.naming.directory.InvalidSearchFilterException if the search filter specified is
     *		not supported or understood by the underlying directory
     * @throws	javax.naming.directory.InvalidSearchControlsException if the search controls
     * 		contain invalid settings
     * @throws	NamingException if a naming exception is encountered
     */
    public NamingEnumeration search(String name,
	                String filter,
	                SearchControls cons)
	    throws NamingException
    {
        throw new UnsupportedOperationException() ;
    }


    /**
     * Searches in the named context or object for entries that satisfy the
     * given search filter.  Performs the search as specified by
     * the search controls.
     *<p>
     * The interpretation of <code>filterExpr</code> is based on RFC
     * 2254.  It may additionally contain variables of the form
     * <code>{i}</code> -- where <code>i</code> is an integer -- that
     * refer to objects in the <code>filterArgs</code> array.  The
     * interpretation of <code>filterExpr</code> is otherwise
     * identical to that of the <code>filter</code> parameter of the
     * method <code>search(Name, String, SearchControls)</code>.
     *<p>
     * When a variable <code>{i}</code> appears in a search filter, it
     * indicates that the filter argument <code>filterArgs[i]</code>
     * is to be used in that place.  Such variables may be used
     * wherever an <em>attr</em>, <em>value</em>, or
     * <em>matchingrule</em> production appears in the filter grammar
     * of RFC 2254, section 4.  When a string-valued filter argument
     * is substituted for a variable, the filter is interpreted as if
     * the string were given in place of the variable, with any
     * characters having special significance within filters (such as
     * <code>'*'</code>) having been escaped according to the rules of
     * RFC 2254.
     *<p>
     * For directories that do not use a string representation for
     * some or all of their attributes, the filter argument
     * corresponding to an attribute value may be of a type other than
     * String.  Directories that support unstructured binary-valued
     * attributes, for example, should accept byte arrays as filter
     * arguments.  The interpretation (if any) of filter arguments of
     * any other type is determined by the service provider for that
     * directory, which maps the filter operations onto operations with
     * corresponding semantics in the underlying directory.
     *<p>
     * This method returns an enumeration of the results.
     * Each element in the enumeration contains the name of the object
     * and other information about the object (see <code>SearchResult</code>).
     * The name is either relative to the target context of the search
     * (which is named by the <code>name</code> parameter), or
     * it is a URL string. If the target context is included in
     * the enumeration (as is possible when
     * <code>cons</code> specifies a search scope of
     * <code>SearchControls.OBJECT_SCOPE</code> or
     * <code>SearchControls.SUBSTREE_SCOPE</code>),
     * its name is the empty string.
     *<p>
     * The <tt>SearchResult</tt> may also contain attributes of the matching
     * object if the <tt>cons</tt> argument specifies that attributes be
     * returned.
     *<p>
     * If the object does not have a requested attribute, that
     * nonexistent attribute will be ignored.  Those requested
     * attributes that the object does have will be returned.
     *<p>
     * A directory might return more attributes than were requested
     * (see <strong>Attribute Type Names</strong> in the class description)
     * but is not allowed to return arbitrary, unrelated attributes.
     *<p>
     * If a search filter with invalid variable substitutions is provided
     * to this method, the result is undefined.
     * When changes are made to this DirContext,
     * the effect on enumerations returned by prior calls to this method
     * is undefined.
     *<p>
     * See also <strong>Operational Attributes</strong> in the class
     * description.
     *
     * @param name
     *		the name of the context or object to search
     * @param filterExpr
     *		the filter expression to use for the search.
     *		The expression may contain variables of the
     *		form "<code>{i}</code>" where <code>i</code>
     *		is a nonnegative integer.  May not be null.
     * @param filterArgs
     *		the array of arguments to substitute for the variables
     *		in <code>filterExpr</code>.  The value of
     *		<code>filterArgs[i]</code> will replace each
     *		occurrence of "<code>{i}</code>".
     *		If null, equivalent to an empty array.
     * @param cons
     *		the search controls that control the search.  If null,
     *		the default search controls are used (equivalent
     *		to <tt>(new SearchControls())</tt>).
     * @return	an enumeration of <tt>SearchResult</tt>s of the objects
     *		that satisfy the filter; never null
     *
     * @throws	ArrayIndexOutOfBoundsException if <tt>filterExpr</tt> contains
     *		<code>{i}</code> expressions where <code>i</code> is outside
     *		the bounds of the array <code>filterArgs</code>
     * @throws	javax.naming.directory.InvalidSearchControlsException if <tt>cons</tt> contains
     *		invalid settings
     * @throws	javax.naming.directory.InvalidSearchFilterException if <tt>filterExpr</tt> with
     *		<tt>filterArgs</tt> represents an invalid search filter
     * @throws	NamingException if a naming exception is encountered
     *
     * @see #search(Name, Attributes, String[])
     * @see <{MessageFormat}>
     */
    public NamingEnumeration search(Name name,
	                String filterExpr,
	                Object[] filterArgs,
	                SearchControls cons)
	    throws NamingException
    {
        throw new UnsupportedOperationException() ;
    }


    /**
     * Searches in the named context or object for entries that satisfy the
     * given search filter.  Performs the search as specified by
     * the search controls.
     * See {@link #search(Name, String, Object[], SearchControls)} for details.
     *
     * @param name
     *		the name of the context or object to search
     * @param filterExpr
     *		the filter expression to use for the search.
     *		The expression may contain variables of the
     *		form "<code>{i}</code>" where <code>i</code>
     *		is a nonnegative integer.  May not be null.
     * @param filterArgs
     *		the array of arguments to substitute for the variables
     *		in <code>filterExpr</code>.  The value of
     *		<code>filterArgs[i]</code> will replace each
     *		occurrence of "<code>{i}</code>".
     *		If null, equivalent to an empty array.
     * @param cons
     *		the search controls that control the search.  If null,
     *		the default search controls are used (equivalent
     *		to <tt>(new SearchControls())</tt>).
     * @return	an enumeration of <tt>SearchResult</tt>s of the objects
     *		that satisfy the filter; never null
     *
     * @throws	ArrayIndexOutOfBoundsException if <tt>filterExpr</tt> contains
     *		<code>{i}</code> expressions where <code>i</code> is outside
     *		the bounds of the array <code>filterArgs</code>
     * @throws	javax.naming.directory.InvalidSearchControlsException if <tt>cons</tt> contains
     *		invalid settings
     * @throws	javax.naming.directory.InvalidSearchFilterException if <tt>filterExpr</tt> with
     *		<tt>filterArgs</tt> represents an invalid search filter
     * @throws	NamingException if a naming exception is encountered
     */
    public NamingEnumeration search(String name,
	                String filterExpr,
	                Object[] filterArgs,
	                SearchControls cons)
	    throws NamingException
    {
        throw new UnsupportedOperationException() ;
    }
}
