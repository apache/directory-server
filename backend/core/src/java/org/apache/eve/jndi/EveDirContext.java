package org.apache.eve.jndi;


import java.io.IOException ;
import java.util.Hashtable ;
import java.text.ParseException ;

import javax.naming.Name ;
import javax.naming.ldap.Control ;
import javax.naming.NamingException ;
import javax.naming.NamingEnumeration ;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes ;
import javax.naming.directory.DirContext ;
import javax.naming.directory.SearchControls ;
import javax.naming.directory.ModificationItem ;
import javax.naming.directory.InvalidSearchFilterException ;

import org.apache.ldap.common.name.LdapName ;
import org.apache.ldap.common.filter.ExprNode ;
import org.apache.ldap.common.filter.BranchNode ;
import org.apache.ldap.common.filter.SimpleNode ;
import org.apache.ldap.common.filter.PresenceNode ;
import org.apache.ldap.common.filter.FilterParser ;
import org.apache.ldap.common.util.NamespaceTools ;
import org.apache.ldap.common.filter.FilterParserImpl ;

import org.apache.eve.PartitionNexus;


/**
 * The DirContext implementation for the Server Side JNDI LDAP provider.
 *
 */
public class EveDirContext extends EveContext implements DirContext
{
    
    
    // ------------------------------------------------------------------------
    // Constructors
    // ------------------------------------------------------------------------
    
    /**
     * Creates a new EveDirContext by reading the PROVIDER_URL to resolve the
     * distinguished name for this context.
     *
     * @param a_nexusProxy the proxy to the backend nexus
     * @param a_env the environment used for this context
     * @throws NamingException if something goes wrong
     */
    public EveDirContext( PartitionNexus a_nexusProxy, Hashtable a_env ) throws NamingException
    {
        super( a_nexusProxy, a_env ) ;
    }


    /**
     * Creates a new EveDirContext with a distinguished name which is used to
     * set the PROVIDER_URL to the distinguished name for this context.
     * 
     * @param a_nexusProxy the intercepting proxy to the nexus
     * @param a_env the environment properties used by this context
     * @param a_dn the distinguished name of this context
     */
    protected EveDirContext( PartitionNexus a_nexusProxy, Hashtable a_env, LdapName a_dn )
    {
        super( a_nexusProxy, a_env, a_dn ) ;
    }


    // ------------------------------------------------------------------------
    // DirContext Implementations
    // ------------------------------------------------------------------------


    /**
     * @see javax.naming.directory.DirContext#getAttributes(java.lang.String)
     */
    public Attributes getAttributes( String a_name ) throws NamingException
    {
        return getAttributes( new LdapName( a_name ) ) ;
    }
    

    /**
     * @see javax.naming.directory.DirContext#getAttributes(javax.naming.Name)
     */
    public Attributes getAttributes( Name a_name ) throws NamingException
    {
        return getNexusProxy().lookup( buildTarget( a_name ) ) ;
    }


    /**
     * @see javax.naming.directory.DirContext#getAttributes(java.lang.String,
     *      java.lang.String[])
     */
    public Attributes getAttributes( String a_name, String[] a_attrIds )
        throws NamingException
    {
        return getAttributes( new LdapName( a_name ), a_attrIds ) ;
    }


    /**
     * @see javax.naming.directory.DirContext#getAttributes(javax.naming.Name,
     *      java.lang.String[])
     */
    public Attributes getAttributes( Name a_name, String[] a_attrIds )
        throws NamingException
    {
        return getNexusProxy().lookup( buildTarget( a_name ), a_attrIds ) ;
    }
    

    /**
     * @see javax.naming.directory.DirContext#modifyAttributes(java.lang.String,
     *      int, javax.naming.directory.Attributes)
     */
    public void modifyAttributes( String a_name, int a_modOp, 
        Attributes a_attrs ) throws NamingException
    {
        modifyAttributes( new LdapName( a_name ), a_modOp, a_attrs ) ;
    }


    /**
     * @see javax.naming.directory.DirContext#modifyAttributes(
     * javax.naming.Name,int, javax.naming.directory.Attributes)
     */
    public void modifyAttributes( Name a_name, int a_modOp, Attributes a_attrs )
        throws NamingException
    {
        getNexusProxy().modify( buildTarget( a_name ), a_modOp, a_attrs ) ;
    }


    /**
     * @see javax.naming.directory.DirContext#modifyAttributes(java.lang.String,
     *      javax.naming.directory.ModificationItem[])
     */
    public void modifyAttributes( String a_name, ModificationItem[] a_mods )
        throws NamingException
    {
        modifyAttributes( new LdapName( a_name ), a_mods ) ;
    }


    /**
     * @see javax.naming.directory.DirContext#modifyAttributes(
     * javax.naming.Name, javax.naming.directory.ModificationItem[])
     */
    public void modifyAttributes( Name a_name, ModificationItem[] a_mods )
        throws NamingException
    {
        getNexusProxy().modify( buildTarget( a_name ), a_mods ) ;
    }
    

    /**
     * @see javax.naming.directory.DirContext#bind(java.lang.String,
     *      java.lang.Object, javax.naming.directory.Attributes)
     */
    public void bind( String a_name, Object a_obj, Attributes a_attrs )
        throws NamingException
    {
        bind( new LdapName( a_name ), a_obj, a_attrs ) ;
    }


    /**
     * @see javax.naming.directory.DirContext#bind(javax.naming.Name,
     *      java.lang.Object, javax.naming.directory.Attributes)
     */
    public void bind( Name a_name, Object a_obj, Attributes a_attrs )
        throws NamingException
    {
        if ( null == a_obj && null == a_attrs )  
        {
            throw new NamingException( "Both a_obj and a_attrs args are null. "
                + "At least one of these parameters must not be null." ) ;
        }

        // A null a_attrs defaults this to the Context.bind() operation        
        if ( null == a_attrs )
        {
            super.bind( a_name, a_obj ) ;
        }
        // No object binding so we just add the attributes
        else if ( null == a_obj )
        {
            Attributes l_clone = ( Attributes ) a_attrs.clone() ;
            Name l_target = buildTarget( a_name ) ;
            getNexusProxy().add( l_target.toString(), l_target, l_clone ) ;
        }
        // Need to perform serialization of object into a copy of a_attrs
        else 
        {
            if ( a_obj instanceof EveLdapContext )
            {
                throw new IllegalArgumentException(
                    "Cannot bind a directory context object!" ) ;
            }

            Attributes l_clone = ( Attributes ) a_attrs.clone() ;
            JavaLdap.serialize( l_clone, a_obj ) ;
            Name l_target = buildTarget( a_name ) ;
            getNexusProxy().add( l_target.toString(), l_target, l_clone ) ;
        }
    }


    /**
     * @see javax.naming.directory.DirContext#rebind(java.lang.String,
     *      java.lang.Object, javax.naming.directory.Attributes)
     */
    public void rebind( String a_name, Object a_obj, Attributes a_attrs )
        throws NamingException
    {
        rebind( new LdapName( a_name ), a_obj, a_attrs ) ;
    }


    /**
     * @see javax.naming.directory.DirContext#rebind(javax.naming.Name,
     *      java.lang.Object, javax.naming.directory.Attributes)
     */
    public void rebind( Name a_name, Object a_obj, Attributes a_attrs )
        throws NamingException
    {
        Name l_target = buildTarget( a_name ) ;

        if ( getNexusProxy().hasEntry( l_target ) ) 
        {
            getNexusProxy().delete( l_target ) ;
        }

        bind( a_name, a_obj, a_attrs ) ;
    }


    /**
     * @see javax.naming.directory.DirContext#createSubcontext(java.lang.String,
     *      javax.naming.directory.Attributes)
     */
    public DirContext createSubcontext( String a_name, Attributes a_attrs )
        throws NamingException
    {
        return createSubcontext( new LdapName( a_name ), a_attrs ) ;        
    }


    /**
     * @see javax.naming.directory.DirContext#createSubcontext(
     * javax.naming.Name, javax.naming.directory.Attributes)
     */
    public DirContext createSubcontext( Name a_name, Attributes a_attrs )
        throws NamingException
    {
        if ( null == a_attrs )
        {
            return ( DirContext ) super.createSubcontext( a_name ) ;
        }
        
        // @todo again note that we presume single attribute name components
        LdapName l_target = buildTarget( a_name ) ;
        String l_rdn = a_name.get( a_name.size() - 1 ) ;
        String l_rdnAttribute = NamespaceTools.getRdnAttribute( l_rdn ) ;
        String l_rdnValue = NamespaceTools.getRdnValue( l_rdn ) ;

        // Clone the attributes and add the Rdn attributes
        Attributes l_attributes = ( Attributes ) a_attrs.clone() ; 
        l_attributes.put( l_rdnAttribute, l_rdnValue ) ;
        
        // Add the new context to the server which as a side effect adds 
        getNexusProxy().add( l_target.toString(), l_target, l_attributes ) ;

        // Initialize the new context
        EveLdapContext l_ctx = new EveLdapContext( getNexusProxy(),
            getEnvironment(), l_target ) ;
        Control [] l_controls = ( Control [] )
            ( ( EveLdapContext ) this ).getRequestControls().clone() ;
        l_ctx.setRequestControls( l_controls ) ;
        return l_ctx ;
    }


    /**
     * Presently unsupported operation!
     *
     * @param a_name TODO
     * @return TODO
     * @throws NamingException all the time.
     * @see javax.naming.directory.DirContext#getSchema(javax.naming.Name)
     */
    public DirContext getSchema( Name a_name ) throws NamingException
    {
        throw new UnsupportedOperationException() ;
    }
    

    /**
     * Presently unsupported operation!
     * 
     * @param a_name TODO
     * @return TODO
     * @throws NamingException all the time.
     * @see javax.naming.directory.DirContext#getSchema(java.lang.String)
     */
    public DirContext getSchema( String a_name ) throws NamingException
    {
        throw new UnsupportedOperationException() ;
    }


    /**
     * Presently unsupported operation!
     * 
     * @param a_name TODO
     * @return TODO
     * @throws NamingException all the time.
     * @see javax.naming.directory.DirContext#getSchemaClassDefinition(
     * javax.naming.Name)
     */
    public DirContext getSchemaClassDefinition( Name a_name )
        throws NamingException
    {
        throw new UnsupportedOperationException() ;
    }


    /**
     * Presently unsupported operation!
     * 
     * @param a_name TODO
     * @return TODO
     * @throws NamingException all the time.
     * @see javax.naming.directory.DirContext#getSchemaClassDefinition(
     * java.lang.String)
     */
    public DirContext getSchemaClassDefinition( String a_name )
        throws NamingException
    {
        throw new UnsupportedOperationException() ;
    }


    // ------------------------------------------------------------------------
    // Search Operation Implementations
    // ------------------------------------------------------------------------


    /**
     * @see javax.naming.directory.DirContext#search(java.lang.String,
     *      javax.naming.directory.Attributes)
     */
    public NamingEnumeration search( String a_name, 
        Attributes a_matchingAttributes ) throws NamingException
    {
        return search( new LdapName( a_name ), a_matchingAttributes, null ) ; 
    }


    /**
     * @see javax.naming.directory.DirContext#search(javax.naming.Name,
     *      javax.naming.directory.Attributes)
     */
    public NamingEnumeration search( Name a_name, 
        Attributes a_matchingAttributes ) throws NamingException
    {
        return search( a_name, a_matchingAttributes, null ) ; 
    }


    /**
     * @see javax.naming.directory.DirContext#search(java.lang.String,
     *      javax.naming.directory.Attributes, java.lang.String[])
     */
    public NamingEnumeration search( String a_name, 
        Attributes a_matchingAttributes, String[] a_attributesToReturn ) 
        throws NamingException
    {
        return search( new LdapName( a_name ), a_matchingAttributes, 
            a_attributesToReturn ) ;
    }


    /**
     * TODO may need to refactor some of this functionality into a filter 
     * utility class in commons.
     * 
     * @see javax.naming.directory.DirContext#search(javax.naming.Name,
     *      javax.naming.directory.Attributes, java.lang.String[])
     */
    public NamingEnumeration search( Name a_name, 
        Attributes a_matchingAttributes, String[] a_attributesToReturn ) 
        throws NamingException
    {
        SearchControls l_ctls = new SearchControls() ;
        LdapName l_target = buildTarget( a_name ) ;

        // If we need to return specific attributes add em to the SearchControls
        if ( null != a_attributesToReturn )
        {
            l_ctls.setReturningAttributes( a_attributesToReturn ) ;
        } 

        // If matchingAttributes is null/empty use a match for everything filter
        if ( null == a_matchingAttributes || a_matchingAttributes.size() <= 0 )
        {
            PresenceNode l_filter = new PresenceNode( "objectClass" ) ;
            return getNexusProxy().search( l_target , getEnvironment(), 
                l_filter, l_ctls ) ;
        }

        /*
         * Go through the set of attributes using each attribute value pair as 
         * an attribute value assertion within one big AND filter expression.
         */
        Attribute l_attr = null ;
        SimpleNode l_node = null ;
        BranchNode l_filter = new BranchNode( BranchNode.AND ) ;
        NamingEnumeration l_list = a_matchingAttributes.getAll() ;
        
        // Loop through each attribute value pair
        while ( l_list.hasMore() )
        {
            l_attr = ( Attribute ) l_list.next() ;
            
            /*
             * According to JNDI if an attribute in the a_matchingAttributes
             * list does not have any values then we match for just the presence
             * of the attribute in the entry
             */
            if ( l_attr.size() == 0 )
            {
                l_filter.addNode( new PresenceNode( l_attr.getID() ) ) ;
                continue ;
            }
            
            /*
             * With 1 or more value we build a set of simple nodes and add them
             * to the AND node - each attribute value pair is a simple AVA node.
             */
            for ( int ii = 0; ii < l_attr.size(); ii++ )
            {
                Object l_val = l_attr.get( ii ) ;
                
                // Add simpel AVA node if its value is a String 
                if ( l_val instanceof String )
                {
                    l_node = new SimpleNode( l_attr.getID(), 
                        ( String ) l_val, SimpleNode.EQUALITY ) ;
                    l_filter.addNode( l_node ) ;
                }
            }
        }

        return getNexusProxy().search( l_target , getEnvironment(), 
            l_filter, l_ctls ) ;
    }


    /**
     * @see javax.naming.directory.DirContext#search(java.lang.String,
     *      java.lang.String, javax.naming.directory.SearchControls)
     */
    public NamingEnumeration search( String a_name, String a_filter,
        SearchControls a_cons ) throws NamingException
    {
        return search( new LdapName( a_name ), a_filter, a_cons ) ;
    }


    /**
     * @see javax.naming.directory.DirContext#search(javax.naming.Name,
     *      java.lang.String, javax.naming.directory.SearchControls)
     */
    public NamingEnumeration search( Name a_name, String a_filter,
        SearchControls a_cons ) throws NamingException
    {
        ExprNode l_filter = null ;
        LdapName l_target = buildTarget( a_name ) ;

        try 
        {
            /*
             * TODO Added this parser initialization code to the FilterImpl
             * and have a static class parser that can be globally accessed. 
             */
            FilterParser l_parser = new FilterParserImpl() ;
            l_filter = l_parser.parse( a_filter ) ;
        }
        catch ( ParseException pe )
        {
            InvalidSearchFilterException l_isfe = 
                new InvalidSearchFilterException (
                "Encountered parse exception while parsing the filter: '" 
                + a_filter + "'" ) ;
            l_isfe.setRootCause( pe ) ;
            throw l_isfe ;
        }
        catch ( IOException ioe )
        {
            NamingException l_ne = new NamingException(
                "Parser failed with IO exception on filter: '" 
                + a_filter + "'" ) ;
            l_ne.setRootCause( ioe ) ;
            throw l_ne ;
        }
        
        return getNexusProxy().search( l_target , getEnvironment(), 
            l_filter, new SearchControls() ) ;
    }


    /**
     * @see javax.naming.directory.DirContext#search(java.lang.String,
     *      java.lang.String, java.lang.Object[],
     *      javax.naming.directory.SearchControls)
     */
    public NamingEnumeration search( String a_name, String a_filterExpr,
        Object[] a_filterArgs, SearchControls a_cons ) throws NamingException
    {
        return search( new LdapName( a_name ), a_filterExpr, a_filterArgs,
            a_cons ) ;
    }


    /**
     * TODO Factor out the filter variable code into the commons filter pkg &
     * test it there.
     * 
     * @see javax.naming.directory.DirContext#search(javax.naming.Name,
     *      java.lang.String, java.lang.Object[],
     *      javax.naming.directory.SearchControls)
     */
    public NamingEnumeration search( Name a_name, String a_filterExpr,
        Object[] a_filterArgs, SearchControls a_cons ) throws NamingException
    {
        int l_start ;
        StringBuffer l_buf = new StringBuffer( a_filterExpr ) ;
        
        // Scan until we hit the end of the string buffer 
        for ( int ii = 0; ii < l_buf.length(); ii++ )
        {
            // Advance until we hit the start of a variable
            while ( '{' != l_buf.charAt( ii ) )
            {
                ii++ ;
            }
            
            // Record start of variable at '{'
            l_start = ii ;
            
            // Advance to the end of a variable at '}'
            while ( '}' != l_buf.charAt( ii ) ) 
            {
                ii++ ;
            }
            
            /*
             * Replace the '{ i }' with the string representation of the value
             * held in the a_filterArgs array at index l_index.
             */           
            l_buf.replace( l_start, ii + 1, a_filterArgs[ii].toString() ) ;
        }
        
        return search( a_name, l_buf.toString(), a_cons ) ;
    }
}
