/*
 *   Copyright 2004 The Apache Software Foundation
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 */
package org.apache.eve.jndi;


import java.io.IOException;
import java.util.Hashtable;
import java.text.ParseException;

import javax.naming.Name;
import javax.naming.ldap.Control;
import javax.naming.NamingException;
import javax.naming.NamingEnumeration;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.SearchControls;
import javax.naming.directory.ModificationItem;
import javax.naming.directory.InvalidSearchFilterException;

import org.apache.ldap.common.name.LdapName;
import org.apache.ldap.common.filter.ExprNode;
import org.apache.ldap.common.filter.BranchNode;
import org.apache.ldap.common.filter.SimpleNode;
import org.apache.ldap.common.filter.PresenceNode;
import org.apache.ldap.common.filter.FilterParser;
import org.apache.ldap.common.util.NamespaceTools;
import org.apache.ldap.common.filter.FilterParserImpl;

import org.apache.eve.PartitionNexus;


/**
 * The DirContext implementation for the Server Side JNDI LDAP provider.
 *
 * @author <a href="mailto:directory-dev@incubator.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public abstract class EveDirContext extends EveContext implements DirContext
{
    
    
    // ------------------------------------------------------------------------
    // Constructors
    // ------------------------------------------------------------------------
    
    /**
     * Creates a new EveDirContext by reading the PROVIDER_URL to resolve the
     * distinguished name for this context.
     *
     * @param nexusProxy the proxy to the backend nexus
     * @param env the environment used for this context
     * @throws NamingException if something goes wrong
     */
    public EveDirContext( PartitionNexus nexusProxy, Hashtable env ) throws NamingException
    {
        super( nexusProxy, env );
    }


    /**
     * Creates a new EveDirContext with a distinguished name which is used to
     * set the PROVIDER_URL to the distinguished name for this context.
     * 
     * @param nexusProxy the intercepting proxy to the nexus
     * @param env the environment properties used by this context
     * @param dn the distinguished name of this context
     */
    protected EveDirContext( PartitionNexus nexusProxy, Hashtable env, LdapName dn )
    {
        super( nexusProxy, env, dn );
    }


    // ------------------------------------------------------------------------
    // DirContext Implementations
    // ------------------------------------------------------------------------


    /**
     * @see javax.naming.directory.DirContext#getAttributes(java.lang.String)
     */
    public Attributes getAttributes( String name ) throws NamingException
    {
        return getAttributes( new LdapName( name ) );
    }
    

    /**
     * @see javax.naming.directory.DirContext#getAttributes(javax.naming.Name)
     */
    public Attributes getAttributes( Name name ) throws NamingException
    {
        return getNexusProxy().lookup( buildTarget( name ) );
    }


    /**
     * @see javax.naming.directory.DirContext#getAttributes(java.lang.String,
     *      java.lang.String[])
     */
    public Attributes getAttributes( String name, String[] attrIds )
        throws NamingException
    {
        return getAttributes( new LdapName( name ), attrIds );
    }


    /**
     * @see javax.naming.directory.DirContext#getAttributes(javax.naming.Name,
     *      java.lang.String[])
     */
    public Attributes getAttributes( Name name, String[] attrIds )
        throws NamingException
    {
        return getNexusProxy().lookup( buildTarget( name ), attrIds );
    }
    

    /**
     * @see javax.naming.directory.DirContext#modifyAttributes(java.lang.String,
     *      int, javax.naming.directory.Attributes)
     */
    public void modifyAttributes( String name, int modOp,
        Attributes attrs ) throws NamingException
    {
        modifyAttributes( new LdapName( name ), modOp, attrs );
    }


    /**
     * @see javax.naming.directory.DirContext#modifyAttributes(
     * javax.naming.Name,int, javax.naming.directory.Attributes)
     */
    public void modifyAttributes( Name name, int modOp, Attributes attrs )
        throws NamingException
    {
        getNexusProxy().modify( buildTarget( name ), modOp, attrs );
    }


    /**
     * @see javax.naming.directory.DirContext#modifyAttributes(java.lang.String,
     *      javax.naming.directory.ModificationItem[])
     */
    public void modifyAttributes( String name, ModificationItem[] mods )
        throws NamingException
    {
        modifyAttributes( new LdapName( name ), mods );
    }


    /**
     * @see javax.naming.directory.DirContext#modifyAttributes(
     * javax.naming.Name, javax.naming.directory.ModificationItem[])
     */
    public void modifyAttributes( Name name, ModificationItem[] mods )
        throws NamingException
    {
        getNexusProxy().modify( buildTarget( name ), mods );
    }
    

    /**
     * @see javax.naming.directory.DirContext#bind(java.lang.String,
     *      java.lang.Object, javax.naming.directory.Attributes)
     */
    public void bind( String name, Object obj, Attributes attrs )
        throws NamingException
    {
        bind( new LdapName( name ), obj, attrs );
    }


    /**
     * @see javax.naming.directory.DirContext#bind(javax.naming.Name,
     *      java.lang.Object, javax.naming.directory.Attributes)
     */
    public void bind( Name name, Object obj, Attributes attrs )
        throws NamingException
    {
        if ( null == obj && null == attrs )
        {
            throw new NamingException( "Both obj and attrs args are null. "
                + "At least one of these parameters must not be null." );
        }

        // A null attrs defaults this to the Context.bind() operation
        if ( null == attrs )
        {
            super.bind( name, obj );
        }
        // No object binding so we just add the attributes
        else if ( null == obj )
        {
            Attributes clone = ( Attributes ) attrs.clone();
            Name target = buildTarget( name );
            getNexusProxy().add( target.toString(), target, clone );
        }
        // Need to perform serialization of object into a copy of attrs
        else 
        {
            if ( obj instanceof EveLdapContext )
            {
                throw new IllegalArgumentException(
                    "Cannot bind a directory context object!" );
            }

            Attributes clone = ( Attributes ) attrs.clone();
            JavaLdapSupport.serialize( clone, obj );
            Name target = buildTarget( name );
            getNexusProxy().add( target.toString(), target, clone );
        }
    }


    /**
     * @see javax.naming.directory.DirContext#rebind(java.lang.String,
     *      java.lang.Object, javax.naming.directory.Attributes)
     */
    public void rebind( String name, Object obj, Attributes attrs )
        throws NamingException
    {
        rebind( new LdapName( name ), obj, attrs );
    }


    /**
     * @see javax.naming.directory.DirContext#rebind(javax.naming.Name,
     *      java.lang.Object, javax.naming.directory.Attributes)
     */
    public void rebind( Name name, Object obj, Attributes attrs )
        throws NamingException
    {
        Name target = buildTarget( name );

        if ( getNexusProxy().hasEntry( target ) )
        {
            getNexusProxy().delete( target );
        }

        bind( name, obj, attrs );
    }


    /**
     * @see javax.naming.directory.DirContext#createSubcontext(java.lang.String,
     *      javax.naming.directory.Attributes)
     */
    public DirContext createSubcontext( String name, Attributes attrs )
        throws NamingException
    {
        return createSubcontext( new LdapName( name ), attrs );
    }


    /**
     * @see javax.naming.directory.DirContext#createSubcontext(
     * javax.naming.Name, javax.naming.directory.Attributes)
     */
    public DirContext createSubcontext( Name name, Attributes attrs )
        throws NamingException
    {
        if ( null == attrs )
        {
            return ( DirContext ) super.createSubcontext( name );
        }
        
        // @todo again note that we presume single attribute name components
        LdapName target = buildTarget( name );
        String rdn = name.get( name.size() - 1 );
        String rdnAttribute = NamespaceTools.getRdnAttribute( rdn );
        String rdnValue = NamespaceTools.getRdnValue( rdn );

        // Clone the attributes and add the Rdn attributes
        Attributes attributes = ( Attributes ) attrs.clone();
        attributes.put( rdnAttribute, rdnValue );
        
        // Add the new context to the server which as a side effect adds 
        getNexusProxy().add( target.toString(), target, attributes );

        // Initialize the new context
        EveLdapContext ctx = new EveLdapContext( getNexusProxy(),
            getEnvironment(), target );
        
        Control [] controls = ( ( EveLdapContext ) this ).getRequestControls();
        if ( controls != null )
        {
        	controls = ( Control[] ) controls.clone();
        }
        else
        {
        	controls = new Control[0];
        }
        
        ctx.setRequestControls( controls );
        return ctx;
    }


    /**
     * Presently unsupported operation!
     *
     * @param name TODO
     * @return TODO
     * @throws NamingException all the time.
     * @see javax.naming.directory.DirContext#getSchema(javax.naming.Name)
     */
    public DirContext getSchema( Name name ) throws NamingException
    {
        throw new UnsupportedOperationException();
    }
    

    /**
     * Presently unsupported operation!
     * 
     * @param name TODO
     * @return TODO
     * @throws NamingException all the time.
     * @see javax.naming.directory.DirContext#getSchema(java.lang.String)
     */
    public DirContext getSchema( String name ) throws NamingException
    {
        throw new UnsupportedOperationException();
    }


    /**
     * Presently unsupported operation!
     * 
     * @param name TODO
     * @return TODO
     * @throws NamingException all the time.
     * @see javax.naming.directory.DirContext#getSchemaClassDefinition(
     * javax.naming.Name)
     */
    public DirContext getSchemaClassDefinition( Name name )
        throws NamingException
    {
        throw new UnsupportedOperationException();
    }


    /**
     * Presently unsupported operation!
     * 
     * @param name TODO
     * @return TODO
     * @throws NamingException all the time.
     * @see javax.naming.directory.DirContext#getSchemaClassDefinition(
     * java.lang.String)
     */
    public DirContext getSchemaClassDefinition( String name )
        throws NamingException
    {
        throw new UnsupportedOperationException();
    }


    // ------------------------------------------------------------------------
    // Search Operation Implementations
    // ------------------------------------------------------------------------


    /**
     * @see javax.naming.directory.DirContext#search(java.lang.String,
     *      javax.naming.directory.Attributes)
     */
    public NamingEnumeration search( String name,
        Attributes matchingAttributes ) throws NamingException
    {
        return search( new LdapName( name ), matchingAttributes, null );
    }


    /**
     * @see javax.naming.directory.DirContext#search(javax.naming.Name,
     *      javax.naming.directory.Attributes)
     */
    public NamingEnumeration search( Name name,
        Attributes matchingAttributes ) throws NamingException
    {
        return search( name, matchingAttributes, null );
    }


    /**
     * @see javax.naming.directory.DirContext#search(java.lang.String,
     *      javax.naming.directory.Attributes, java.lang.String[])
     */
    public NamingEnumeration search( String name,
        Attributes matchingAttributes, String[] attributesToReturn )
        throws NamingException
    {
        return search( new LdapName( name ), matchingAttributes,
            attributesToReturn );
    }


    /**
     * TODO may need to refactor some of this functionality into a filter 
     * utility class in commons.
     * 
     * @see javax.naming.directory.DirContext#search(javax.naming.Name,
     *      javax.naming.directory.Attributes, java.lang.String[])
     */
    public NamingEnumeration search( Name name,
        Attributes matchingAttributes, String[] attributesToReturn )
        throws NamingException
    {
        SearchControls ctls = new SearchControls();
        LdapName target = buildTarget( name );

        // If we need to return specific attributes add em to the SearchControls
        if ( null != attributesToReturn )
        {
            ctls.setReturningAttributes( attributesToReturn );
        } 

        // If matchingAttributes is null/empty use a match for everything filter
        if ( null == matchingAttributes || matchingAttributes.size() <= 0 )
        {
            PresenceNode filter = new PresenceNode( "objectClass" );
            return getNexusProxy().search( target , getEnvironment(),
                filter, ctls );
        }

        /*
         * Go through the set of attributes using each attribute value pair as 
         * an attribute value assertion within one big AND filter expression.
         */
        Attribute attr = null;
        SimpleNode node = null;
        BranchNode filter = new BranchNode( BranchNode.AND );
        NamingEnumeration list = matchingAttributes.getAll();
        
        // Loop through each attribute value pair
        while ( list.hasMore() )
        {
            attr = ( Attribute ) list.next();
            
            /*
             * According to JNDI if an attribute in the matchingAttributes
             * list does not have any values then we match for just the presence
             * of the attribute in the entry
             */
            if ( attr.size() == 0 )
            {
                filter.addNode( new PresenceNode( attr.getID() ) );
                continue;
            }
            
            /*
             * With 1 or more value we build a set of simple nodes and add them
             * to the AND node - each attribute value pair is a simple AVA node.
             */
            for ( int ii = 0; ii < attr.size(); ii++ )
            {
                Object val = attr.get( ii );
                
                // Add simpel AVA node if its value is a String 
                if ( val instanceof String )
                {
                    node = new SimpleNode( attr.getID(),
                        ( String ) val, SimpleNode.EQUALITY );
                    filter.addNode( node );
                }
            }
        }

        return getNexusProxy().search( target , getEnvironment(),
            filter, ctls );
    }


    /**
     * @see javax.naming.directory.DirContext#search(java.lang.String,
     *      java.lang.String, javax.naming.directory.SearchControls)
     */
    public NamingEnumeration search( String name, String filter,
        SearchControls cons ) throws NamingException
    {
        return search( new LdapName( name ), filter, cons );
    }


    /**
     * @see javax.naming.directory.DirContext#search(javax.naming.Name,
     *      java.lang.String, javax.naming.directory.SearchControls)
     */
    public NamingEnumeration search( Name name, String filter,
        SearchControls cons ) throws NamingException
    {
        ExprNode filterNode = null;
        LdapName target = buildTarget( name );

        try 
        {
            /*
             * TODO Added this parser initialization code to the FilterImpl
             * and have a static class parser that can be globally accessed. 
             */
            FilterParser parser = new FilterParserImpl();
            filterNode = parser.parse( filter );
        }
        catch ( ParseException pe )
        {
            InvalidSearchFilterException isfe =
                new InvalidSearchFilterException (
                "Encountered parse exception while parsing the filter: '" 
                + filter + "'" );
            isfe.setRootCause( pe );
            throw isfe;
        }
        catch ( IOException ioe )
        {
            NamingException ne = new NamingException(
                "Parser failed with IO exception on filter: '" 
                + filter + "'" );
            ne.setRootCause( ioe );
            throw ne;
        }
        
        return getNexusProxy().search( target , getEnvironment(),
            filterNode, new SearchControls() );
    }


    /**
     * @see javax.naming.directory.DirContext#search(java.lang.String,
     *      java.lang.String, java.lang.Object[],
     *      javax.naming.directory.SearchControls)
     */
    public NamingEnumeration search( String name, String filterExpr,
        Object[] filterArgs, SearchControls cons ) throws NamingException
    {
        return search( new LdapName( name ), filterExpr, filterArgs,
            cons );
    }


    /**
     * TODO Factor out the filter variable code into the commons filter pkg &
     * test it there.
     * 
     * @see javax.naming.directory.DirContext#search(javax.naming.Name,
     *      java.lang.String, java.lang.Object[],
     *      javax.naming.directory.SearchControls)
     */
    public NamingEnumeration search( Name name, String filterExpr,
        Object[] filterArgs, SearchControls cons ) throws NamingException
    {
        int start;
        StringBuffer buf = new StringBuffer( filterExpr );
        
        // Scan until we hit the end of the string buffer 
        for ( int ii = 0; ii < buf.length(); ii++ )
        {
            // Advance until we hit the start of a variable
            while ( '{' != buf.charAt( ii ) )
            {
                ii++;
            }
            
            // Record start of variable at '{'
            start = ii;
            
            // Advance to the end of a variable at '}'
            while ( '}' != buf.charAt( ii ) )
            {
                ii++;
            }
            
            /*
             * Replace the '{ i }' with the string representation of the value
             * held in the filterArgs array at index index.
             */           
            buf.replace( start, ii + 1, filterArgs[ii].toString() );
        }
        
        return search( name, buf.toString(), cons );
    }
}
