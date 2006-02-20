/*
 *   Copyright 2005 The Apache Software Foundation
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

package org.apache.directory.server.dns.store.operations;


import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import javax.naming.CompoundName;
import javax.naming.Name;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;

import org.apache.directory.server.dns.messages.QuestionRecord;
import org.apache.directory.server.dns.messages.RecordClass;
import org.apache.directory.server.dns.messages.RecordType;
import org.apache.directory.server.dns.messages.ResourceRecord;
import org.apache.directory.server.dns.messages.ResourceRecordModifier;
import org.apache.directory.server.dns.store.DnsAttribute;
import org.apache.directory.server.protocol.shared.store.ContextOperation;


/**
 * A JNDI context operation for looking up Resource Records from an embedded JNDI provider.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class GetRecords implements ContextOperation
{
    private static final long serialVersionUID = 1077580995617778894L;

    /** The name of the question to get. */
    private final QuestionRecord question;


    /**
     * Creates the action to be used against the embedded JNDI provider.
     */
    public GetRecords(QuestionRecord question)
    {
        this.question = question;
    }

    /**
     * Mappings of type to objectClass.
     */
    private static final Map TYPE_TO_OBJECTCLASS;

    static
    {
        Map typeToObjectClass = new HashMap();
        typeToObjectClass.put( RecordType.SOA, "apacheDnsStartOfAuthorityRecord" );
        typeToObjectClass.put( RecordType.A, "apacheDnsAddressRecord" );
        typeToObjectClass.put( RecordType.NS, "apacheDnsNameServerRecord" );
        typeToObjectClass.put( RecordType.CNAME, "apacheDnsCanonicalNameRecord" );
        typeToObjectClass.put( RecordType.PTR, "apacheDnsPointerRecord" );
        typeToObjectClass.put( RecordType.MX, "apacheDnsMailExchangeRecord" );
        typeToObjectClass.put( RecordType.SRV, "apacheDnsServiceRecord" );
        typeToObjectClass.put( RecordType.TXT, "apacheDnsTextRecord" );

        TYPE_TO_OBJECTCLASS = Collections.unmodifiableMap( typeToObjectClass );
    }

    /**
     * Mappings of type to objectClass.
     */
    private static final Map OBJECTCLASS_TO_TYPE;

    static
    {
        Map objectClassToType = new HashMap();
        objectClassToType.put( "apacheDnsStartOfAuthorityRecord", RecordType.SOA );
        objectClassToType.put( "apacheDnsAddressRecord", RecordType.A );
        objectClassToType.put( "apacheDnsNameServerRecord", RecordType.NS );
        objectClassToType.put( "apacheDnsCanonicalNameRecord", RecordType.CNAME );
        objectClassToType.put( "apacheDnsPointerRecord", RecordType.PTR );
        objectClassToType.put( "apacheDnsMailExchangeRecord", RecordType.MX );
        objectClassToType.put( "apacheDnsServiceRecord", RecordType.SRV );
        objectClassToType.put( "apacheDnsTextRecord", RecordType.TXT );
        objectClassToType.put( "apacheDnsReferralNameServer", RecordType.NS );
        objectClassToType.put( "apacheDnsReferralAddress", RecordType.A );

        OBJECTCLASS_TO_TYPE = Collections.unmodifiableMap( objectClassToType );
    }


    /**
     * Note that the base is a relative path from the exiting context.
     * It is not a DN.
     */
    public Object execute( DirContext ctx, Name base ) throws Exception
    {
        if ( question == null )
        {
            return null;
        }

        String name = question.getDomainName();
        RecordType type = question.getRecordType();

        SearchControls controls = new SearchControls();
        controls.setSearchScope( SearchControls.SUBTREE_SCOPE );

        String filter = "(objectClass=" + ( String ) TYPE_TO_OBJECTCLASS.get( type ) + ")";

        NamingEnumeration list = ctx.search( transformDomainName( name ), filter, controls );

        Set set = new HashSet();

        while ( list.hasMore() )
        {
            SearchResult result = ( SearchResult ) list.next();
            Name relative = getRelativeName( ctx.getNameInNamespace(), result.getName() );

            set.add( getRecord( result.getAttributes(), relative ) );
        }

        return set;
    }


    /**
     * Marshals a RecordStoreEntry from an Attributes object.
     *
     * @param attrs the attributes of the DNS question
     * @return the entry for the question
     * @throws NamingException if there are any access problems
     */
    private ResourceRecord getRecord( Attributes attrs, Name relative ) throws NamingException
    {
        String SOA_MINIMUM = "86400";
        String SOA_CLASS = "IN";

        ResourceRecordModifier modifier = new ResourceRecordModifier();

        Attribute attr;

        // if no name, transform rdn
        attr = attrs.get( DnsAttribute.NAME );

        if ( attr != null )
        {
            modifier.setDnsName( ( String ) attr.get() );
        }
        else
        {
            relative = getDomainComponents( relative );

            String dnsName;
            dnsName = transformDistinguishedName( relative.toString() );
            modifier.setDnsName( dnsName );
        }

        // type is implicit in objectclass
        attr = attrs.get( DnsAttribute.TYPE );

        if ( attr != null )
        {
            modifier.setDnsType( RecordType.getTypeByName( ( String ) attr.get() ) );
        }
        else
        {
            modifier.setDnsType( getType( attrs.get( "objectclass" ) ) );
        }

        // class defaults to SOA CLASS
        String dnsClass = ( attr = attrs.get( DnsAttribute.CLASS ) ) != null ? ( String ) attr.get() : SOA_CLASS;
        modifier.setDnsClass( RecordClass.getTypeByName( dnsClass ) );

        // ttl defaults to SOA MINIMUM
        String dnsTtl = ( attr = attrs.get( DnsAttribute.TTL ) ) != null ? ( String ) attr.get() : SOA_MINIMUM;
        modifier.setDnsTtl( Integer.parseInt( dnsTtl ) );

        NamingEnumeration ids = attrs.getIDs();

        while ( ids.hasMore() )
        {
            String id = ( String ) ids.next();
            modifier.put( id, ( String ) attrs.get( id ).get() );
        }

        return modifier.getEntry();
    }


    /**
     * Uses the algorithm in <a href="http://www.faqs.org/rfcs/rfc2247.html">RFC 2247</a>
     * to transform any Internet domain name into a distinguished name.
     *
     * @param domainName the domain name
     * @return the distinguished name
     */
    String transformDomainName( String domainName )
    {
        if ( domainName == null || domainName.length() == 0 )
        {
            return "";
        }

        StringBuffer buf = new StringBuffer( domainName.length() + 16 );

        buf.append( "dc=" );
        buf.append( domainName.replaceAll( "\\.", ",dc=" ) );

        return buf.toString();
    }


    /**
     * Uses the algorithm in <a href="http://www.faqs.org/rfcs/rfc2247.html">RFC 2247</a>
     * to transform a distinguished name into an Internet domain name.
     *
     * @param distinguishedName the distinguished name
     * @return the domain name
     */
    String transformDistinguishedName( String distinguishedName )
    {
        if ( distinguishedName == null || distinguishedName.length() == 0 )
        {
            return "";
        }

        String domainName = distinguishedName.replaceFirst( "dc=", "" );
        domainName = domainName.replaceAll( ",dc=", "." );

        return domainName;
    }


    private RecordType getType( Attribute objectClass ) throws NamingException
    {
        NamingEnumeration list = objectClass.getAll();

        while ( list.hasMore() )
        {
            String value = ( String ) list.next();

            if ( !value.equals( "apacheDnsAbstractRecord" ) )
            {
                RecordType type = ( RecordType ) OBJECTCLASS_TO_TYPE.get( value );

                if ( type == null )
                {
                    throw new RuntimeException( "Record type to objectClass mapping has not been set." );
                }

                return type;
            }
        }

        throw new NamingException( "ResourceRecord requires STRUCTURAL objectClass" );
    }


    private Name getRelativeName( String nameInNamespace, String baseDn ) throws NamingException
    {
        Properties props = new Properties();
        props.setProperty( "jndi.syntax.direction", "right_to_left" );
        props.setProperty( "jndi.syntax.separator", "," );
        props.setProperty( "jndi.syntax.ignorecase", "true" );
        props.setProperty( "jndi.syntax.trimblanks", "true" );

        Name searchBaseDn = null;

        Name ctxRoot = new CompoundName( nameInNamespace, props );
        searchBaseDn = new CompoundName( baseDn, props );

        if ( !searchBaseDn.startsWith( ctxRoot ) )
        {
            throw new NamingException( "Invalid search base " + baseDn );
        }

        for ( int ii = 0; ii < ctxRoot.size(); ii++ )
        {
            searchBaseDn.remove( 0 );
        }

        return searchBaseDn;
    }


    private Name getDomainComponents( Name name ) throws NamingException
    {
        for ( int ii = 0; ii < name.size(); ii++ )
        {
            if ( !name.get( ii ).startsWith( "dc=" ) )
            {
                name.remove( ii );
            }
        }

        return name;
    }
}
