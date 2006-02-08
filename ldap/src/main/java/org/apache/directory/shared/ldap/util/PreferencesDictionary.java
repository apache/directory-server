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
package org.apache.directory.shared.ldap.util;


import java.util.Dictionary;
import java.util.Enumeration;
import java.util.prefs.Preferences;
import java.util.prefs.BackingStoreException;


/**
 * A wrapper around Preferences to access it as a Dictionary.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class PreferencesDictionary extends Dictionary
{
    /** the underlying wrapped preferences object */
    private final Preferences prefs;


    // ------------------------------------------------------------------------
    // C O N S T R U C T O R
    // ------------------------------------------------------------------------


    public PreferencesDictionary( Preferences prefs )
    {
        this.prefs = prefs;
    }


    // ------------------------------------------------------------------------
    // E X T R A   M E T H O D S
    // ------------------------------------------------------------------------


    /**
     * Gets the Preferences used as the backing store for this Dictionary.
     *
     * @return the underlying Preferences object
     */
    public Preferences getPreferences()
    {
        return prefs;
    }


    // ------------------------------------------------------------------------
    // D I C T I O N A R Y   M E T H O D S
    // ------------------------------------------------------------------------


    public int size()
    {
        try
        {
            return prefs.keys().length;
        }
        catch ( BackingStoreException e )
        {
            throw new NestableRuntimeException( "can't get keys from prefs", e );
        }
    }


    public boolean isEmpty()
    {
        try
        {
            return prefs.keys().length == 0;
        }
        catch ( BackingStoreException e )
        {
            throw new NestableRuntimeException( "can't get keys from prefs", e );
        }
    }


    public Enumeration elements()
    {
        try
        {
            return new ArrayEnumeration( prefs.keys() )
            {
                public Object nextElement()
                {
                    String key = ( String ) super.nextElement();

                    return prefs.get( key, null );
                }
            };
        }
        catch ( BackingStoreException e )
        {
            throw new NestableRuntimeException( "can't get keys from prefs", e );
        }
    }


    public Enumeration keys()
    {
        try
        {
            return new ArrayEnumeration( prefs.keys() );
        }
        catch ( BackingStoreException e )
        {
            throw new NestableRuntimeException( "can't get keys from prefs", e );
        }
    }


    public Object get( Object key )
    {
        if ( key instanceof String )
        {
            return prefs.get( ( String ) key, null );
        }

        return prefs.get( key.toString(), null );
    }


    public Object remove( Object key )
    {
        Object retval = get( key );

        if ( key instanceof String )
        {
            prefs.remove( ( String ) key );
        }
        else
        {
            prefs.remove( key.toString() );
        }

        return retval;
    }


    public Object put( Object key, Object value )
    {
        Object retval = get( key );

        String skey = null;

        String svalue = null;

        if ( key instanceof String )
        {
            skey = ( String ) key;
        }
        else
        {
            skey = key.toString();
        }

        if ( value instanceof String )
        {
            svalue = ( String ) value;
        }
        else
        {
            svalue = value.toString();
        }

        prefs.put( skey, svalue );

        return retval;
    }
}
