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
package org.apache.directory.server.core.configuration;

import java.beans.PropertyEditor;
import java.beans.PropertyEditorSupport;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.BasicAttributes;

import org.apache.commons.collections.MultiHashMap;
import org.apache.directory.server.core.DirectoryService;
import org.apache.directory.shared.ldap.ldif.LdifComposer;
import org.apache.directory.shared.ldap.ldif.LdifComposerImpl;
import org.apache.directory.shared.ldap.ldif.LdifReader;
import org.apache.directory.shared.ldap.util.MultiMap;
import org.apache.directory.shared.ldap.util.StringTools;

/**
 * A JavaBeans {@link PropertyEditor} that can convert {@link Attributes} to
 * LDIF string and vice versa. This class is useful when you're going to
 * configure a {@link DirectoryService} with 3rd party containers such as <a
 * href="http://www.springframework.org/">Spring Framework</a>.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
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

        Attributes attrs = (Attributes) getValue();
        try
        {
            NamingEnumeration e = attrs.getAll();
            while ( e.hasMore() )
            {
                Attribute attr = (Attribute) e.next();
                NamingEnumeration e2 = attr.getAll();
                while ( e2.hasMoreElements() )
                {
                    Object value = e2.next();
                    map.put( attr.getID(), value );
                }
            }

            return composer.compose( map );
        }
        catch (Exception e)
        {
            throw new ConfigurationException( e );
        }
    }

    /**
     * Read an entry (without DN)
     * 
     * @param text
     *            The ldif format file
     * @return An Attributes.
     */
    private Attributes readEntry( String text )
    {
        StringReader strIn = new StringReader( text );
        BufferedReader in = new BufferedReader( strIn );

        String line = null;
        Attributes attributes = new BasicAttributes( true );

        try
        {
            while ( ( line = ( (BufferedReader) in ).readLine() ) != null )
            {
                if ( line.length() == 0 )
                {
                    continue;
                }

                String addedLine = line.trim();

                if ( StringTools.isEmpty( addedLine ) )
                {
                    continue;
                }

                Attribute attribute = LdifReader.parseAttributeValue( addedLine );
                Attribute oldAttribute = attributes.get( attribute.getID() );

                if ( oldAttribute != null )
                {
                    try
                    {
                        oldAttribute.add( attribute.get() );
                        attributes.put( oldAttribute );
                    }
                    catch (NamingException ne)
                    {
                        // Do nothing
                    }
                }
                else
                {
                    attributes.put( attribute );
                }
            }
        }
        catch (IOException ioe)
        {
            // Do nothing : we can't reach this point !
        }

        return attributes;
    }

    /**
     * Converts the specified LDIF string into {@link Attributes}.
     */
    public void setAsText( String text ) throws IllegalArgumentException
    {
        if ( text == null )
        {
            text = "";
        }

        setValue( readEntry( text ) );
    }
}
