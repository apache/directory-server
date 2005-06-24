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
package org.apache.ldap.server.prefs;


import java.util.ArrayList;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.prefs.AbstractPreferences;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import javax.naming.Context;
import javax.naming.NameClassPair;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.BasicAttribute;
import javax.naming.directory.DirContext;
import javax.naming.directory.ModificationItem;
import javax.naming.ldap.InitialLdapContext;
import javax.naming.ldap.LdapContext;

import org.apache.ldap.common.Lockable;
import org.apache.ldap.common.message.LockableAttributeImpl;
import org.apache.ldap.common.message.LockableAttributesImpl;
import org.apache.ldap.common.util.PreferencesDictionary;
import org.apache.ldap.server.configuration.MutableStartupConfiguration;
import org.apache.ldap.server.configuration.ShutdownConfiguration;
import org.apache.ldap.server.jndi.CoreContextFactory;


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
    private static final ModificationItem[] EMPTY_MODS = new ModificationItem[0];

    /** an empty array of Strings used to get array from list */
    private static final String[] EMPTY_STRINGS = new String[0];

    /** the LDAP context representing this preferences object */
    private LdapContext ctx;

    /** the changes (ModificationItems) representing cached alterations to preferences */
    private ArrayList changes = new ArrayList(3);

    /** maps changes based on key: key->list of mods (on same key) */
    private HashMap keyToChange = new HashMap(3);


    /**
     * Creates a preferences object for the system preferences root.
     */
    public ServerSystemPreferences()
    {
        super( null, "" );

        super.newNode = false;

        MutableStartupConfiguration cfg = new MutableStartupConfiguration();
        cfg.setAllowAnonymousAccess( true );
        
        Hashtable env = new Hashtable( cfg.toJndiEnvironment() );
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
    
    public synchronized void close()
    {
        if( this.parent() != null )
        {
            throw new ServerSystemPreferenceException( "Cannot close child preferences." );
        }

        Hashtable env = new Hashtable( new ShutdownConfiguration().toJndiEnvironment() );
        env.put( Context.INITIAL_CONTEXT_FACTORY, CoreContextFactory.class.getName() );
        env.put( Context.PROVIDER_URL, PreferencesUtils.SYSPREF_BASE );

        try
        {
            ctx = new InitialLdapContext( env, null );
        }
        catch ( Exception e )
        {
            throw new ServerSystemPreferenceException( "Failed to close.", e );
        }
    }


    /**
     * Creates a preferences object using a relative name.
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
     * @param name the name of the new ServerPreferences node.
     */
    private void setUpNode( String name ) throws NamingException
    {
        Attributes attrs = new LockableAttributesImpl();

        Attribute attr = new LockableAttributeImpl( ( Lockable ) attrs, "objectClass" );

        attr.add( "top" );

        attr.add( "prefNode" );

        attr.add( "extensibleObject" );

        attrs.put( attr );

        attr = new LockableAttributeImpl( ( Lockable ) attrs, "prefNodeName" );

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
            ctx.modifyAttributes( "", ( ModificationItem[] ) changes.toArray( EMPTY_MODS ) );
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
            ctx.modifyAttributes( "", ( ModificationItem[] ) changes.toArray( EMPTY_MODS ) );
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
        ArrayList children = new ArrayList();

        NamingEnumeration list = null;

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

        return ( String[] ) children.toArray( EMPTY_STRINGS );
    }


    protected String[] keysSpi() throws BackingStoreException
    {
        Attributes attrs = null;

        ArrayList keys = new ArrayList();

        try
        {
            attrs = ctx.getAttributes( "" );

            NamingEnumeration ids = attrs.getIDs();

            while ( ids.hasMore() )
            {
                String id = ( String ) ids.next();

                if ( id.equals( "objectClass" ) || id.equals( "prefNodeName" ) )
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

        return ( String[] ) keys.toArray( EMPTY_STRINGS );
    }


    protected void removeSpi( String key )
    {
        Attribute attr = new BasicAttribute( key );

        ModificationItem mi = new ModificationItem( DirContext.REMOVE_ATTRIBUTE, attr );

        addDelta( mi );
    }


    private void addDelta( ModificationItem mi )
    {
        String key = mi.getAttribute().getID();

        List deltas = null;

        changes.add( mi );

        if ( keyToChange.containsKey( key ) )
        {
            deltas = ( List ) keyToChange.get( key );
        }
        else
        {
            deltas = new ArrayList();
        }

        deltas.add( mi );

        keyToChange.put( key, deltas );
    }


    protected String getSpi( String key )
    {
        String value = null;

        try
        {
            Attribute attr = ctx.getAttributes( "" ).get( key );

            if ( keyToChange.containsKey( key ) )
            {
                List mods = ( List ) keyToChange.get( key );

                for ( int ii = 0; ii < mods.size(); ii++ )
                {
                    ModificationItem mi = ( ModificationItem ) mods.get( ii );

                    if ( mi.getModificationOp() == DirContext.REMOVE_ATTRIBUTE )
                    {
                        attr = null;
                    }
                    else
                    {
                        attr = mi.getAttribute();
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
        Attribute attr = new BasicAttribute( key );

        attr.add( value );

        ModificationItem mi = new ModificationItem( DirContext.REPLACE_ATTRIBUTE, attr );

        addDelta( mi );
    }


    protected AbstractPreferences childSpi( String name )
    {
        return new ServerSystemPreferences( this, name );
    }
}
