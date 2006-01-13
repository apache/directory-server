/*
 *   @(#) $Id$
 *
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
package org.apache.ldap.server.configuration;

import java.beans.PropertyEditor;
import java.beans.PropertyEditorSupport;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.BasicAttributes;

import org.apache.commons.collections.MultiHashMap;
import org.apache.ldap.common.ldif.LdifComposer;
import org.apache.ldap.common.ldif.LdifComposerImpl;
import org.apache.ldap.common.ldif.LdifParser;
import org.apache.ldap.common.ldif.LdifParserImpl;
import org.apache.ldap.common.util.MultiMap;
import org.apache.ldap.server.DirectoryService;

/**
 * A JavaBeans {@link PropertyEditor} that can convert {@link Attributes}
 * to LDIF string and vice versa.  This class is useful when you're going
 * to configure a {@link DirectoryService} with 3rd party containers
 * such as <a href="http://www.springframework.org/">Spring Framework</a>.
 *
 * @author The Apache Directory Project
 * @version $Rev$, $Date$
 */
public class AttributesPropertyEditor extends PropertyEditorSupport
{

    /**
     * Creates a new instance.
     */
    public AttributesPropertyEditor()
    {
        super();
    }

    /**
     * Creates a new instance with source object.
     */
    public AttributesPropertyEditor( Object source )
    {
        super( source );
    }

    /**
     * Returns LDIF string of {@link Attributes} object.
     */
    public String getAsText()
    {
        LdifComposer composer = new LdifComposerImpl();
        MultiMap map = new MultiMap()
        {
            // FIXME Stop forking commons-collections.
            private final MultiHashMap map = new MultiHashMap();

            public Object remove( Object arg0, Object arg1 )
            {
                return map.remove( arg0, arg1 );
            }

            public int size()
            {
                return map.size();
            }

            public Object get( Object arg0 )
            {
                return map.get( arg0 );
            }

            public boolean containsValue( Object arg0 )
            {
                return map.containsValue( arg0 );
            }

            public Object put( Object arg0, Object arg1 )
            {
                return map.put( arg0, arg1 );
            }

            public Object remove( Object arg0 )
            {
                return map.remove( arg0 );
            }

            public Collection values()
            {
                return map.values();
            }

            public boolean isEmpty()
            {
                return map.isEmpty();
            }

            public boolean containsKey( Object key )
            {
                return map.containsKey( key );
            }

            public void putAll( Map arg0 )
            {
                map.putAll( arg0 );
            }

            public void clear()
            {
                map.clear();
            }

            public Set keySet()
            {
                return map.keySet();
            }

            public Set entrySet()
            {
                return map.entrySet();
            }
        };
        
        Attributes attrs = ( Attributes ) getValue();
        try
        {
            NamingEnumeration e = attrs.getAll();
            while( e.hasMore() )
            {
                Attribute attr = ( Attribute ) e.next();
                NamingEnumeration e2 = attr.getAll();
                while( e2.hasMoreElements() )
                {
                    Object value = e2.next();
                    map.put( attr.getID(), value );
                }
            }

            return composer.compose( map );
        }
        catch( Exception e )
        {
            throw new ConfigurationException( e );
        }
    }

    /**
     * Converts the specified LDIF string into {@link Attributes}.
     */
    public void setAsText( String text ) throws IllegalArgumentException
    {
        if( text == null )
        {
            text = "";
        }

        Attributes attrs = new BasicAttributes( true );
        LdifParser parser = new LdifParserImpl();
        try
        {
            parser.parse( attrs, text.trim() );
            setValue( attrs );
        }
        catch( NamingException e )
        {
            throw ( IllegalArgumentException ) new IllegalArgumentException().initCause( e );
        }
    }
}
