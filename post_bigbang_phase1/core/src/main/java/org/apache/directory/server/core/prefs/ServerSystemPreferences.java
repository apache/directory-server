/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *  
 *    http://www.apache.org/licenses/LICENSE-2.0
 *  
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License. 
 *  
 */
package org.apache.directory.server.core.prefs;


import org.apache.directory.server.constants.ApacheSchemaConstants;
import org.apache.directory.server.core.DirectoryService;
import org.apache.directory.server.core.jndi.CoreContextFactory;
import org.apache.directory.shared.ldap.constants.SchemaConstants;
import org.apache.directory.shared.ldap.message.AttributeImpl;
import org.apache.directory.shared.ldap.message.AttributesImpl;
import org.apache.directory.shared.ldap.message.ModificationItemImpl;
import org.apache.directory.shared.ldap.util.PreferencesDictionary;

import javax.naming.Context;
import javax.naming.NameClassPair;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.ModificationItem;
import javax.naming.ldap.InitialLdapContext;
import javax.naming.ldap.LdapContext;
import java.util.*;
import java.util.prefs.AbstractPreferences;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;


/**
 * A server side system {@link Preferences} implementation.  This implementation
 * presumes the creation of a root system preferences node in advance.  This
 * should be included with the system.ldif that is packaged with the server.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class ServerSystemPreferences extends AbstractPreferences
{
    /** an empty array of ModificationItems used to get array from list */
    private static final ModificationItemImpl[] EMPTY_MODS = new ModificationItemImpl[0];

    /** an empty array of Strings used to get array from list */
    private static final String[] EMPTY_STRINGS = new String[0];

    /** the LDAP context representing this preferences object */
    private LdapContext ctx;

    /** the changes (ModificationItems) representing cached alterations to preferences */
    private ArrayList<ModificationItem> changes = new ArrayList<ModificationItem>( 3 );

    /** maps changes based on key: key->list of mods (on same key) */
    private HashMap<String, List<ModificationItem>> keyToChange = new HashMap<String, List<ModificationItem>>( 3 );


    /**
     * Creates a preferences object for the system preferences root.
     * @param service the directory service core
     */
    public ServerSystemPreferences( DirectoryService service )
    {
        super( null, "" );
        super.newNode = false;

        Hashtable<String, Object> env = new Hashtable<String, Object>();
        env.put( DirectoryService.JNDI_KEY, service );
        env.put( Context.INITIAL_CONTEXT_FACTORY, CoreContextFactory.class.getName() );
        env.put( Context.PROVIDER_URL, PreferencesUtils.SYSPREF_BASE );

        try
        {
            ctx = new InitialLdapContext( env, null );
        }
        catch ( Exception e )
        {
            throw new ServerSystemPreferenceException( "Failed to open.", e );
        }
    }


    public synchronized void close() throws NamingException
    {
        if ( this.parent() != null )
        {
            throw new ServerSystemPreferenceException( "Cannot close child preferences." );
        }

        this.ctx.close();
    }


    /**
     * Creates a preferences object using a relative name.
     * 
     * @param name the name of the preference node to create
     * @param parent the parent of the preferences node to create
     */
    public ServerSystemPreferences( ServerSystemPreferences parent, String name )
    {
        super( parent, name );
        LdapContext parentCtx = parent.getLdapContext();

        try
        {
            ctx = ( LdapContext ) parentCtx.lookup( "prefNodeName=" + name );
            super.newNode = false;
        }
        catch ( NamingException e )
        {
            super.newNode = true;
        }

        if ( super.newNode )
        {
            try
            {
                setUpNode( name );
            }
            catch ( Exception e )
            {
                throw new ServerSystemPreferenceException( "Failed to set up node.", e );
            }
        }
    }


    // ------------------------------------------------------------------------
    // Utility Methods
    // ------------------------------------------------------------------------

    /**
     * Wrapps this ServerPreferences object as a Dictionary.
     *
     * @return a Dictionary that uses this ServerPreferences object as the underlying backing store
     */
    public Dictionary wrapAsDictionary()
    {
        return new PreferencesDictionary( this );
    }


    /**
     * Gets access to the LDAP context associated with this ServerPreferences node.
     *
     * @return the LDAP context associate with this ServerPreferences node
     */
    LdapContext getLdapContext()
    {
        return ctx;
    }


    /**
     * Sets up a new ServerPreferences node by injecting the required information
     * such as the node name attribute and the objectClass attribute.
     *
     * @param name the name of the new ServerPreferences node
     * @throws NamingException if we fail to created the new node
     */
    private void setUpNode( String name ) throws NamingException
    {
        Attributes attrs = new AttributesImpl();
        Attribute attr = new AttributeImpl( SchemaConstants.OBJECT_CLASS_AT );
        attr.add( SchemaConstants.TOP_OC );
        attr.add( ApacheSchemaConstants.PREF_NODE_OC );
        attr.add( SchemaConstants.EXTENSIBLE_OBJECT_OC );
        attrs.put( attr );
        attr = new AttributeImpl( "prefNodeName" );
        attr.add( name );
        attrs.put( attr );

        LdapContext parent = ( ( ServerSystemPreferences ) parent() ).getLdapContext();
        parent.bind( "prefNodeName=" + name, null, attrs );
        ctx = ( LdapContext ) parent.lookup( "prefNodeName=" + name );
        super.newNode = false;
    }


    // ------------------------------------------------------------------------
    // Protected SPI Methods
    // ------------------------------------------------------------------------

    protected void flushSpi() throws BackingStoreException
    {
        if ( ctx == null )
        {
            throw new BackingStoreException( "Ldap context not available for " + super.absolutePath() );
        }

        if ( changes.isEmpty() )
        {
            return;
        }

        try
        {
            //noinspection SuspiciousToArrayCall
            ctx.modifyAttributes( "", changes.toArray( EMPTY_MODS ) );
        }
        catch ( NamingException e )
        {
            throw new BackingStoreException( e );
        }

        changes.clear();
        keyToChange.clear();
    }


    protected void removeNodeSpi() throws BackingStoreException
    {
        try
        {
            ctx.destroySubcontext( "" );
        }
        catch ( NamingException e )
        {
            throw new BackingStoreException( e );
        }

        ctx = null;
        changes.clear();
        keyToChange.clear();
    }


    protected void syncSpi() throws BackingStoreException
    {
        if ( ctx == null )
        {
            throw new BackingStoreException( "Ldap context not available for " + super.absolutePath() );
        }

        if ( changes.isEmpty() )
        {
            return;
        }

        try
        {
            //noinspection SuspiciousToArrayCall
            ctx.modifyAttributes( "", changes.toArray( EMPTY_MODS ) );
        }
        catch ( NamingException e )
        {
            throw new BackingStoreException( e );
        }

        changes.clear();
        keyToChange.clear();
    }


    protected String[] childrenNamesSpi() throws BackingStoreException
    {
        List<String> children = new ArrayList<String>();
        NamingEnumeration list;

        try
        {
            list = ctx.list( "" );
            
            while ( list.hasMore() )
            {
                NameClassPair ncp = ( NameClassPair ) list.next();

                children.add( ncp.getName() );
            }
        }
        catch ( NamingException e )
        {
            throw new BackingStoreException( e );
        }

        return children.toArray( EMPTY_STRINGS );
    }


    protected String[] keysSpi() throws BackingStoreException
    {
        Attributes attrs;
        List<String> keys = new ArrayList<String>();

        try
        {
            attrs = ctx.getAttributes( "" );
            NamingEnumeration ids = attrs.getIDs();
            
            while ( ids.hasMore() )
            {
                String id = ( String ) ids.next();
                
                if ( id.equals( SchemaConstants.OBJECT_CLASS_AT ) || id.equals( "prefNodeName" ) )
                {
                    continue;
                }
                
                keys.add( id );
            }
        }
        catch ( NamingException e )
        {
            throw new BackingStoreException( e );
        }

        return keys.toArray( EMPTY_STRINGS );
    }


    protected void removeSpi( String key )
    {
        Attribute attr = new AttributeImpl( key );
        ModificationItemImpl mi = new ModificationItemImpl( DirContext.REMOVE_ATTRIBUTE, attr );
        addDelta( mi );
    }


    private void addDelta( ModificationItemImpl mi )
    {
        String key = mi.getAttribute().getID();
        List<ModificationItem> deltas;
        changes.add( mi );
        
        if ( keyToChange.containsKey( key ) )
        {
            deltas = keyToChange.get( key );
        }
        else
        {
            deltas = new ArrayList<ModificationItem>();
        }

        deltas.add( mi );
        keyToChange.put( key, deltas );
    }


    protected String getSpi( String key )
    {
        String value;

        try
        {
            Attribute attr = ctx.getAttributes( "" ).get( key );
            if ( keyToChange.containsKey( key ) )
            {
                List<ModificationItem> mods = keyToChange.get( key );
                for ( ModificationItem mod : mods )
                {
                    if ( mod.getModificationOp() == DirContext.REMOVE_ATTRIBUTE )
                    {
                        attr = null;
                    }
                    else
                    {
                        attr = mod.getAttribute();
                    }
                }
            }

            if ( attr == null )
            {
                return null;
            }

            value = ( String ) attr.get();
        }
        catch ( Exception e )
        {
            throw new ServerSystemPreferenceException( "Failed to get SPI.", e );
        }

        return value;
    }


    protected void putSpi( String key, String value )
    {
        Attribute attr = new AttributeImpl( key );
        attr.add( value );
        ModificationItemImpl mi = new ModificationItemImpl( DirContext.REPLACE_ATTRIBUTE, attr );
        addDelta( mi );
    }


    protected AbstractPreferences childSpi( String name )
    {
        return new ServerSystemPreferences( this, name );
    }
}
