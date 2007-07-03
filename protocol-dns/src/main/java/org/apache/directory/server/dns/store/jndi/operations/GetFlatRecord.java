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
package org.apache.directory.server.dns.store.jndi.operations;


import java.util.HashSet;
import java.util.Set;

import javax.naming.Name;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.SearchResult;

import org.apache.directory.server.dns.messages.QuestionRecord;
import org.apache.directory.server.dns.messages.RecordClass;
import org.apache.directory.server.dns.messages.RecordType;
import org.apache.directory.server.dns.messages.ResourceRecord;
import org.apache.directory.server.dns.messages.ResourceRecordModifier;
import org.apache.directory.server.dns.store.DnsAttribute;
import org.apache.directory.server.dns.store.jndi.DnsOperation;
import org.apache.directory.shared.ldap.message.AttributeImpl;
import org.apache.directory.shared.ldap.message.AttributesImpl;


/**
 * A JNDI context operation for looking up a Resource Record with flat attributes.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class GetFlatRecord implements DnsOperation
{
    private static final long serialVersionUID = 4931303293468915435L;

    /** The name of the question to get. */
    private final QuestionRecord question;


    /**
     * Creates the action to be used against the embedded JNDI provider.
     * 
     * @param question 
     */
    public GetFlatRecord( QuestionRecord question )
    {
        this.question = question;
    }


    /**
     * Note that the base is a relative path from the exiting context.
     * It is not a DN.
     */
    public Set<ResourceRecord> execute( DirContext ctx, Name base ) throws Exception
    {
        if ( question == null )
        {
            return null;
        }

        Attributes matchAttrs = new AttributesImpl( true );

        matchAttrs.put( new AttributeImpl( DnsAttribute.NAME, question.getDomainName() ) );
        matchAttrs.put( new AttributeImpl( DnsAttribute.TYPE, question.getRecordType().name() ) );
        matchAttrs.put( new AttributeImpl( DnsAttribute.CLASS, question.getRecordClass().name() ) );

        Set<ResourceRecord> record = new HashSet<ResourceRecord>();

        NamingEnumeration<SearchResult> answer = ctx.search( base, matchAttrs );

        if ( answer.hasMore() )
        {
            SearchResult result = answer.next();

            Attributes attrs = result.getAttributes();

            if ( attrs == null )
            {
                return null;
            }

            record.add( getRecord( attrs ) );
        }

        return record;
    }


    /**
     * Marshals a RecordStoreEntry from an Attributes object.
     *
     * @param attrs the attributes of the DNS question
     * @return the entry for the question
     * @throws NamingException if there are any access problems
     */
    private ResourceRecord getRecord( Attributes attrs ) throws NamingException
    {
        ResourceRecordModifier modifier = new ResourceRecordModifier();

        Attribute attr;

        String dnsName = ( attr = attrs.get( DnsAttribute.NAME ) ) != null ? ( String ) attr.get() : null;
        String dnsType = ( attr = attrs.get( DnsAttribute.TYPE ) ) != null ? ( String ) attr.get() : null;
        String dnsClass = ( attr = attrs.get( DnsAttribute.CLASS ) ) != null ? ( String ) attr.get() : null;
        String dnsTtl = ( attr = attrs.get( DnsAttribute.TTL ) ) != null ? ( String ) attr.get() : null;

        modifier.setDnsName( dnsName );
        modifier.setDnsType( RecordType.valueOf( dnsType ) );
        modifier.setDnsClass( RecordClass.valueOf( dnsClass ) );
        modifier.setDnsTtl( Integer.parseInt( dnsTtl ) );

        NamingEnumeration<String> ids = attrs.getIDs();

        while ( ids.hasMore() )
        {
            String id = ids.next();
            modifier.put( id, ( String ) attrs.get( id ).get() );
        }

        return modifier.getEntry();
    }
}
