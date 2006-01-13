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
package org.apache.ldap.common.codec.search;

import java.nio.ByteBuffer;
import java.util.HashSet;
import java.util.Iterator;

import javax.naming.NamingException;

import org.apache.asn1.codec.DecoderException;
import org.apache.asn1.codec.EncoderException;
import org.apache.asn1.ber.Asn1Decoder;
import org.apache.asn1.ber.IAsn1Container;
import org.apache.ldap.common.codec.LdapDecoder;
import org.apache.ldap.common.codec.LdapMessage;
import org.apache.ldap.common.codec.LdapMessageContainer;
import org.apache.ldap.common.codec.search.SearchResultReference;
import org.apache.ldap.common.codec.util.LdapURL;
import org.apache.ldap.common.codec.util.LdapURLEncodingException;
import org.apache.ldap.common.util.StringTools;

import junit.framework.Assert;
import junit.framework.TestCase;

/**
 * Test the SearchResultReference codec
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class SearchResultReferenceTest extends TestCase {
    /**
     * Test the decoding of a SearchResultReference
     */
    public void testDecodeSearchResultReferenceSuccess() throws NamingException
    {
        Asn1Decoder ldapDecoder = new LdapDecoder();

        ByteBuffer  stream      = ByteBuffer.allocate( 0x3d8 );
        
        String[] ldapUrls = new String[] 
                                       {
                                       	"ldap:///",
                       			        "ldap://directory.apache.org:80/",
                       			        "ldap://d-a.org:80/",
                       			        "ldap://1.2.3.4/",
                       			        "ldap://1.2.3.4:80/",
                       			        "ldap://1.1.1.100000.a/",
                       			        "ldap://directory.apache.org:389/dc=example,dc=org/",
                       			        "ldap://directory.apache.org:389/dc=example",
                       			        "ldap://directory.apache.org:389/dc=example%202,dc=org",
                       			        "ldap://directory.apache.org:389/dc=example,dc=org?ou",
                       			        "ldap://directory.apache.org:389/dc=example,dc=org?ou,objectclass,dc",
                       			        "ldap://directory.apache.org:389/dc=example,dc=org?ou,dc,ou",
                       			        "ldap:///o=University%20of%20Michigan,c=US",
                       			        "ldap://ldap.itd.umich.edu/o=University%20of%20Michigan,c=US",
                       			        "ldap://ldap.itd.umich.edu/o=University%20of%20Michigan,c=US?postalAddress",
                       			        "ldap://host.com:6666/o=University%20of%20Michigan,c=US??sub?(cn=Babs%20Jensen)",
                       			        "ldap://ldap.itd.umich.edu/c=GB?objectClass?one",
                       			        "ldap://ldap.question.com/o=Question%3f,c=US?mail",
                       			        "ldap://ldap.netscape.com/o=Babsco,c=US???(int=%5c00%5c00%5c00%5c04)",
                       			        "ldap:///??sub??bindname=cn=Manager%2co=Foo",
                       			        "ldap:///??sub??!bindname=cn=Manager%2co=Foo"
                       			    };

        stream.put(
            new byte[]
            {
                 
                
                0x30, (byte)0x82, 0x03, (byte)0xd4,	// LDAPMessage ::=SEQUENCE {
				0x02, 0x01, 0x01, 					//     messageID MessageID
				0x73, (byte)0x82, 0x03, (byte)0xcd, //     CHOICE { ..., searchResEntry  SearchResultEntry, ...
                        							// SearchResultReference ::= [APPLICATION 19] SEQUENCE OF LDAPURL
            } );

        
        for (int i = 0; i < ldapUrls.length; i++)
        {
            stream.put((byte)0x04);
            stream.put((byte)ldapUrls[i].getBytes().length);
            
            byte[] bytes = ldapUrls[i].getBytes();
            
            for (int j=0; j < bytes.length; j++)
            {
                stream.put(bytes[j]);
            }
        }
        
        String decodedPdu = StringTools.dumpBytes( stream.array() );
        stream.flip();

        // Allocate a BindRequest Container
        IAsn1Container ldapMessageContainer = new LdapMessageContainer();

        try
        {
            ldapDecoder.decode( stream, ldapMessageContainer );
        }
        catch ( DecoderException de )
        {
            de.printStackTrace();
            Assert.fail( de.getMessage() );
        }
    	
        LdapMessage message = ( ( LdapMessageContainer ) ldapMessageContainer ).getLdapMessage();
        SearchResultReference searchResultReference      = message.getSearchResultReference();

        Assert.assertEquals( 1, message.getMessageId() );
        
        HashSet ldapUrlsSet = new HashSet();
        
        try {
	        for (int i = 0; i < ldapUrls.length; i++)
	        {
	            ldapUrlsSet.add( new LdapURL( ldapUrls[i].getBytes() ).toString() );
	        }
        } catch ( LdapURLEncodingException luee)
        {
            Assert.fail();
        }
        
        Iterator iter = searchResultReference.getSearchResultReferences().iterator();
        
        while (iter.hasNext())
        {
            LdapURL ldapUrl = (LdapURL)iter.next();
            
            if (ldapUrlsSet.contains( ldapUrl.toString()) )
            {
                ldapUrlsSet.remove( ldapUrl.toString() );
            }
            else
            {
                Assert.fail(ldapUrl.toString() + " is not present");
            }
        }
        
        Assert.assertTrue( ldapUrlsSet.size() == 0 );
        
        // Check the length
        Assert.assertEquals(0x3D8, message.computeLength());
        
        // Check the encoding
        try
        {
            ByteBuffer bb = message.encode( null );
            
            String encodedPdu = StringTools.dumpBytes( bb.array() ); 
            
            Assert.assertEquals(encodedPdu, decodedPdu );
        }
        catch ( EncoderException ee )
        {
            ee.printStackTrace();
            Assert.fail( ee.getMessage() );
        }
    }
}
