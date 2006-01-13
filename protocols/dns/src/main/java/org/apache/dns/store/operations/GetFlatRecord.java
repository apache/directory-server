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
package org.apache.dns.store.operations;

import javax.naming.Name;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.BasicAttribute;
import javax.naming.directory.BasicAttributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.SearchResult;

import org.apache.dns.messages.QuestionRecord;
import org.apache.dns.messages.RecordClass;
import org.apache.dns.messages.RecordType;
import org.apache.dns.messages.ResourceRecord;
import org.apache.dns.messages.ResourceRecordModifier;
import org.apache.dns.store.DnsAttribute;
import org.apache.protocol.common.store.ContextOperation;

/**
 * A JNDI context operation for looking up a Resource Record with flat attributes.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class GetFlatRecord implements ContextOperation
{
    private static final long serialVersionUID = 4931303293468915435L;

    /** The name of the question to get. */
    private final QuestionRecord question;

    /**
     * Creates the action to be used against the embedded JNDI provider.
     */
    public GetFlatRecord( QuestionRecord question )
    {
        this.question = question;
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

        Attributes matchAttrs = new BasicAttributes( true );

        matchAttrs.put( new BasicAttribute( DnsAttribute.NAME, question.getDomainName() ) );
        matchAttrs.put( new BasicAttribute( DnsAttribute.TYPE, question.getRecordType().getCode() ) );
        matchAttrs.put( new BasicAttribute( DnsAttribute.CLASS, question.getRecordClass().getCode() ) );

        ResourceRecord record = null;

        NamingEnumeration answer = ctx.search( base, matchAttrs );

        if ( answer.hasMore() )
        {
            SearchResult result = (SearchResult) answer.next();

            Attributes attrs = result.getAttributes();

            if ( attrs == null )
            {
                return null;
            }

            record = getRecord( attrs );
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

        String dnsName = ( attr = attrs.get( DnsAttribute.NAME ) ) != null ? (String) attr.get() : null;
        String dnsType = ( attr = attrs.get( DnsAttribute.TYPE ) ) != null ? (String) attr.get() : null;
        String dnsClass = ( attr = attrs.get( DnsAttribute.CLASS ) ) != null ? (String) attr.get() : null;
        String dnsTtl = ( attr = attrs.get( DnsAttribute.TTL ) ) != null ? (String) attr.get() : null;

        modifier.setDnsName( dnsName );
        modifier.setDnsType( RecordType.getTypeByName( dnsType ) );
        modifier.setDnsClass( RecordClass.getTypeByName( dnsClass ) );
        modifier.setDnsTtl( Integer.parseInt( dnsTtl ) );

        NamingEnumeration ids = attrs.getIDs();

        while ( ids.hasMore() )
        {
            String id = (String) ids.next();
            modifier.put( id, (String) attrs.get( id ).get() );
        }

        return modifier.getEntry();
    }
}
