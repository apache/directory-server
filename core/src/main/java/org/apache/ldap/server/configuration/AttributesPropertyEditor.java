package org.apache.ldap.server.configuration;

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

public class AttributesPropertyEditor extends PropertyEditorSupport
{

    public AttributesPropertyEditor()
    {
        super();
    }

    public AttributesPropertyEditor( Object source )
    {
        super( source );
    }

    public String getAsText()
    {
        LdifComposer composer = new LdifComposerImpl();
        MultiMap map = new MultiMap()
        {
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
