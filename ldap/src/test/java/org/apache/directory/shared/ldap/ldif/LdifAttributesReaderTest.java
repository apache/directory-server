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
package org.apache.directory.shared.ldap.ldif;


import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;

import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.NamingException;

import org.apache.directory.shared.ldap.constants.SchemaConstants;
import org.apache.directory.shared.ldap.util.StringTools;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertNotNull;


/**
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev: 379008 $
 */
public class LdifAttributesReaderTest
{
    private byte[] data;
    
    private File HJENSEN_JPEG_FILE = null;
    
    private File createFile( String name, byte[] data ) throws IOException
    {
        File jpeg = File.createTempFile( name, "jpg" );
        
        jpeg.createNewFile();

        DataOutputStream os = new DataOutputStream( new FileOutputStream( jpeg ) );

        os.write( data );
        os.close();

        // This file will be deleted when the JVM
        // will exit.
        jpeg.deleteOnExit();

        return jpeg;
    }

    /**
     * Create a file to be used by ":<" values
     */
    @Before public void setUp() throws Exception
    {
        data = new byte[256];

        for ( int i = 0; i < 256; i++ )
        {
            data[i] = (byte) i;
        }

        HJENSEN_JPEG_FILE = createFile( "hjensen", data );
    }

    @Test public void testLdifNull() throws NamingException
    {
        String ldif = null;

        LdifAttributesReader reader = new LdifAttributesReader();
        Attributes attributes = reader.parseAttributes( ldif );

        assertEquals( 0, attributes.size() );
    }
    

    @Test public void testLdifEmpty() throws NamingException
    {
        String ldif = "";

        LdifAttributesReader reader = new LdifAttributesReader();
        Attributes attributes = reader.parseAttributes( ldif );

        assertEquals( 0, attributes.size() );
    }

    
    @Test public void testLdifEmptyLines() throws NamingException
    {
        String ldif = "\n\n\r\r\n";

        LdifAttributesReader reader = new LdifAttributesReader();
        Attributes attributes = reader.parseAttributes( ldif );
        assertNull( attributes );
    }

    
    @Test public void testLdifComments() throws NamingException
    {
        String ldif = 
            "#Comment 1\r" + 
            "#\r" + 
            " th\n" + 
            " is is still a comment\n" + 
            "\n";

        LdifAttributesReader reader = new LdifAttributesReader();
        Attributes attributes = reader.parseAttributes( ldif );

        assertNull( attributes );
    }

    
    @Test public void testLdifVersionStart() throws NamingException
    {
        String ldif = 
            "cn: app1\n" + 
            "objectClass: top\n" + 
            "objectClass: apApplication\n" + 
            "displayName:   app1   \n" + 
            "dependencies:\n" + 
            "envVars:";


        LdifAttributesReader reader = new LdifAttributesReader();
        Attributes attributes = reader.parseAttributes( ldif );

        assertEquals( 1, reader.getVersion() );
        assertNotNull( attributes );

        Attribute attr = attributes.get( "displayname" );
        assertTrue( attr.contains( "app1" ) );
    }


    /**
     * Spaces at the end of values should not be included into values.
     * 
     * @throws NamingException
     */
    @Test public void testLdifParserEndSpaces() throws NamingException
    {
        String ldif = 
            "cn: app1\n" + 
            "objectClass: top\n" + 
            "objectClass: apApplication\n" + 
            "displayName:   app1   \n" + 
            "dependencies:\n" + 
            "envVars:";

        LdifAttributesReader reader = new LdifAttributesReader();

        Attributes attributes = reader.parseAttributes( ldif );
        assertNotNull( attributes );

        Attribute attr = attributes.get( "displayname" );
        assertTrue( attr.contains( "app1" ) );

    }


    @Test public void testLdifParser() throws NamingException
    {
        String ldif = 
            "cn: app1\n" + 
            "objectClass: top\n" + 
            "objectClass: apApplication\n" + 
            "displayName: app1   \n" + 
            "dependencies:\n" + 
            "envVars:";

        LdifAttributesReader reader = new LdifAttributesReader();
        Attributes attributes = reader.parseAttributes( ldif );

        assertNotNull( attributes );

        Attribute attr = attributes.get( "cn" );
        assertTrue( attr.contains( "app1" ) );

        attr = attributes.get( "objectclass" );
        assertTrue( attr.contains( "top" ) );
        assertTrue( attr.contains( "apApplication" ) );

        attr = attributes.get( "displayname" );
        assertTrue( attr.contains( "app1" ) );

        attr = attributes.get( "dependencies" );
        assertNull( attr.get() );

        attr = attributes.get( "envvars" );
        assertNull( attr.get() );
    }

    
    @Test public void testLdifParserMuiltiLineComments() throws NamingException
    {
        String ldif = 
            "#comment\n" + 
            " still a comment\n" + 
            "cn: app1#another comment\n" + 
            "objectClass: top\n" + 
            "objectClass: apApplication\n" + 
            "displayName: app1\n" + 
            "serviceType: http\n" + 
            "dependencies:\n" + 
            "httpHeaders:\n" + 
            "startupOptions:\n" + 
            "envVars:";

        LdifAttributesReader reader = new LdifAttributesReader();
        Attributes attributes = reader.parseAttributes( ldif );

        assertNotNull( attributes );

        Attribute attr = attributes.get( "cn" );
        assertTrue( attr.contains( "app1#another comment" ) );

        attr = attributes.get( "objectclass" );
        assertTrue( attr.contains( "top" ) );
        assertTrue( attr.contains( "apApplication" ) );

        attr = attributes.get( "displayname" );
        assertTrue( attr.contains( "app1" ) );

        attr = attributes.get( "dependencies" );
        assertNull( attr.get() );

        attr = attributes.get( "envvars" );
        assertNull( attr.get() );
    }

    
    @Test public void testLdifParserMultiLineEntries() throws NamingException
    {
        String ldif = 
            "#comment\n" + 
            "cn: app1#another comment\n" + 
            "objectClass: top\n" + 
            "objectClass: apAppli\n" +
            " cation\n" + 
            "displayName: app1\n" + 
            "serviceType: http\n" + 
            "dependencies:\n" + 
            "httpHeaders:\n" + 
            "startupOptions:\n" + 
            "envVars:";

        LdifAttributesReader reader = new LdifAttributesReader();
        Attributes attributes = reader.parseAttributes( ldif );

        assertNotNull( attributes );

        Attribute attr = attributes.get( "cn" );
        assertTrue( attr.contains( "app1#another comment" ) );

        attr = attributes.get( "objectclass" );
        assertTrue( attr.contains( "top" ) );
        assertTrue( attr.contains( "apApplication" ) );

        attr = attributes.get( "displayname" );
        assertTrue( attr.contains( "app1" ) );

        attr = attributes.get( "dependencies" );
        assertNull( attr.get() );

        attr = attributes.get( "envvars" );
        assertNull( attr.get() );
    }

    
    @Test public void testLdifParserBase64() throws NamingException, UnsupportedEncodingException
    {
        String ldif = 
            "#comment\n" + 
            "cn:: RW1tYW51ZWwgTMOpY2hhcm55\n" + 
            "objectClass: top\n" + 
            "objectClass: apApplication\n" + 
            "displayName: app1\n" + 
            "serviceType: http\n" + 
            "dependencies:\n" + 
            "httpHeaders:\n" + 
            "startupOptions:\n" + 
            "envVars:";

        LdifAttributesReader reader = new LdifAttributesReader();
        Attributes attributes = reader.parseAttributes( ldif );

        assertNotNull( attributes );

        Attribute attr = attributes.get( "cn" );
        assertTrue( attr.contains( "Emmanuel L\u00e9charny".getBytes( "UTF-8" ) ) );

        attr = attributes.get( "objectclass" );
        assertTrue( attr.contains( "top" ) );
        assertTrue( attr.contains( "apApplication" ) );

        attr = attributes.get( "displayname" );
        assertTrue( attr.contains( "app1" ) );

        attr = attributes.get( "dependencies" );
        assertNull( attr.get() );

        attr = attributes.get( "envvars" );
        assertNull( attr.get() );
    }

    
    @Test public void testLdifParserBase64MultiLine() throws NamingException, UnsupportedEncodingException
    {
        String ldif = 
            "#comment\n" + 
            "cn:: RW1tYW51ZWwg\n" + 
            " TMOpY2hhcm55ICA=\n" + 
            "objectClass: top\n" + 
            "objectClass: apApplication\n" + 
            "displayName: app1\n" + 
            "serviceType: http\n" + 
            "dependencies:\n" + 
            "httpHeaders:\n" + 
            "startupOptions:\n" + 
            "envVars:";

        LdifAttributesReader reader = new LdifAttributesReader();
        Attributes attributes = reader.parseAttributes( ldif );

        assertNotNull( attributes );

        Attribute attr = attributes.get( "cn" );
        assertTrue( attr.contains( "Emmanuel L\u00e9charny  ".getBytes( "UTF-8" ) ) );

        attr = attributes.get( "objectclass" );
        assertTrue( attr.contains( "top" ) );
        assertTrue( attr.contains( "apApplication" ) );

        attr = attributes.get( "displayname" );
        assertTrue( attr.contains( "app1" ) );

        attr = attributes.get( "dependencies" );
        assertNull( attr.get() );

        attr = attributes.get( "envvars" );
        assertNull( attr.get() );
    }

    
    @Test public void testLdifParserRFC2849Sample1() throws NamingException
    {
        String ldif = 
            "objectclass: top\n" + 
            "objectclass: person\n" + 
            "objectclass: organizationalPerson\n" + 
            "cn: Barbara Jensen\n" + 
            "cn: Barbara J Jensen\n" + 
            "cn: Babs Jensen\n" + 
            "sn: Jensen\n" + 
            "uid: bjensen\n" + 
            "telephonenumber: +1 408 555 1212\n" + 
            "description: A big sailing fan.\n"; 

        LdifAttributesReader reader = new LdifAttributesReader();
        Attributes attributes = reader.parseAttributes( ldif );

        Attribute attr = attributes.get( "objectclass" );
        assertTrue( attr.contains( "top" ) );
        assertTrue( attr.contains( "person" ) );
        assertTrue( attr.contains( "organizationalPerson" ) );

        attr = attributes.get( "cn" );
        assertTrue( attr.contains( "Barbara Jensen" ) );
        assertTrue( attr.contains( "Barbara J Jensen" ) );
        assertTrue( attr.contains( "Babs Jensen" ) );

        attr = attributes.get( "sn" );
        assertTrue( attr.contains( "Jensen" ) );

        attr = attributes.get( "uid" );
        assertTrue( attr.contains( "bjensen" ) );

        attr = attributes.get( "telephonenumber" );
        assertTrue( attr.contains( "+1 408 555 1212" ) );

        attr = attributes.get( "description" );
        assertTrue( attr.contains( "A big sailing fan." ) );

    }

    
    @Test public void testLdifParserRFC2849Sample2() throws NamingException
    {
        String ldif = 
            "objectclass: top\n" + 
            "objectclass: person\n" + 
            "objectclass: organizationalPerson\n" + 
            "cn: Barbara Jensen\n" + 
            "cn: Barbara J Jensen\n" + 
            "cn: Babs Jensen\n" + 
            "sn: Jensen\n" + 
            "uid: bjensen\n" + 
            "telephonenumber: +1 408 555 1212\n" + 
            "description:Babs is a big sailing fan, and travels extensively in sea\n" + 
            " rch of perfect sailing conditions.\n" + 
            "title:Product Manager, Rod and Reel Division";

        LdifAttributesReader reader = new LdifAttributesReader();
        Attributes attributes = reader.parseAttributes( ldif );

        Attribute attr = attributes.get( "objectclass" );
        assertTrue( attr.contains( "top" ) );
        assertTrue( attr.contains( "person" ) );
        assertTrue( attr.contains( "organizationalPerson" ) );

        attr = attributes.get( "cn" );
        assertTrue( attr.contains( "Barbara Jensen" ) );
        assertTrue( attr.contains( "Barbara J Jensen" ) );
        assertTrue( attr.contains( "Babs Jensen" ) );

        attr = attributes.get( "sn" );
        assertTrue( attr.contains( "Jensen" ) );

        attr = attributes.get( "uid" );
        assertTrue( attr.contains( "bjensen" ) );

        attr = attributes.get( "telephonenumber" );
        assertTrue( attr.contains( "+1 408 555 1212" ) );

        attr = attributes.get( "description" );
        assertTrue( attr
                .contains( "Babs is a big sailing fan, and travels extensively in search of perfect sailing conditions." ) );

        attr = attributes.get( "title" );
        assertTrue( attr.contains( "Product Manager, Rod and Reel Division" ) );

    }

    
    @Test public void testLdifParserRFC2849Sample3() throws NamingException, Exception
    {
        String ldif = 
            "objectclass: top\n" + 
            "objectclass: person\n" + 
            "objectclass: organizationalPerson\n" + 
            "cn: Gern Jensen\n" + 
            "cn: Gern O Jensen\n" + 
            "sn: Jensen\n" + 
            "uid: gernj\n" + 
            "telephonenumber: +1 408 555 1212\n" + 
            "description:: V2hhdCBhIGNhcmVmdWwgcmVhZGVyIHlvdSBhcmUhICBUaGlzIHZhbHVl\n" + 
            " IGlzIGJhc2UtNjQtZW5jb2RlZCBiZWNhdXNlIGl0IGhhcyBhIGNvbnRyb2wgY2hhcmFjdG\n" + 
            " VyIGluIGl0IChhIENSKS4NICBCeSB0aGUgd2F5LCB5b3Ugc2hvdWxkIHJlYWxseSBnZXQg\n" + 
            " b3V0IG1vcmUu";

        LdifAttributesReader reader = new LdifAttributesReader();
        Attributes attributes = reader.parseAttributes( ldif );

        Attribute attr = attributes.get( "objectclass" );
        assertTrue( attr.contains( "top" ) );
        assertTrue( attr.contains( "person" ) );
        assertTrue( attr.contains( "organizationalPerson" ) );

        attr = attributes.get( "cn" );
        assertTrue( attr.contains( "Gern Jensen" ) );
        assertTrue( attr.contains( "Gern O Jensen" ) );

        attr = attributes.get( "sn" );
        assertTrue( attr.contains( "Jensen" ) );

        attr = attributes.get( "uid" );
        assertTrue( attr.contains( "gernj" ) );

        attr = attributes.get( "telephonenumber" );
        assertTrue( attr.contains( "+1 408 555 1212" ) );

        attr = attributes.get( "description" );
        assertTrue( attr
                .contains( "What a careful reader you are!  This value is base-64-encoded because it has a control character in it (a CR).\r  By the way, you should really get out more."
                        .getBytes( "UTF-8" ) ) );
    }

    
    @Test public void testLdifParserRFC2849Sample3VariousSpacing() throws NamingException, Exception
    {
        String ldif = 
            "objectclass:top\n" + 
            "objectclass:   person   \n" + 
            "objectclass:organizationalPerson\n" + 
            "cn:Gern Jensen\n"  + 
            "cn:Gern O Jensen\n" + 
            "sn:Jensen\n" + 
            "uid:gernj\n" + 
            "telephonenumber:+1 408 555 1212  \n" + 
            "description::  V2hhdCBhIGNhcmVmdWwgcmVhZGVyIHlvdSBhcmUhICBUaGlzIHZhbHVl\n" + 
            " IGlzIGJhc2UtNjQtZW5jb2RlZCBiZWNhdXNlIGl0IGhhcyBhIGNvbnRyb2wgY2hhcmFjdG\n" + 
            " VyIGluIGl0IChhIENSKS4NICBCeSB0aGUgd2F5LCB5b3Ugc2hvdWxkIHJlYWxseSBnZXQg\n" + 
            " b3V0IG1vcmUu  ";

        LdifAttributesReader reader = new LdifAttributesReader();
        Attributes attributes = reader.parseAttributes( ldif );

        Attribute attr = attributes.get( "objectclass" );
        assertTrue( attr.contains( "top" ) );
        assertTrue( attr.contains( "person" ) );
        assertTrue( attr.contains( "organizationalPerson" ) );

        attr = attributes.get( "cn" );
        assertTrue( attr.contains( "Gern Jensen" ) );
        assertTrue( attr.contains( "Gern O Jensen" ) );

        attr = attributes.get( "sn" );
        assertTrue( attr.contains( "Jensen" ) );

        attr = attributes.get( "uid" );
        assertTrue( attr.contains( "gernj" ) );

        attr = attributes.get( "telephonenumber" );
        assertTrue( attr.contains( "+1 408 555 1212" ) );

        attr = attributes.get( "description" );
        assertTrue( attr
                .contains( "What a careful reader you are!  This value is base-64-encoded because it has a control character in it (a CR).\r  By the way, you should really get out more."
                        .getBytes( "UTF-8" ) ) );
    }

    
    @Test public void testLdifParserRFC2849Sample4() throws NamingException, Exception
    {
        String ldif = 
            "# dn:: ou=営業部,o=Airius\n" + 
            "objectclass: top\n" + 
            "objectclass: organizationalUnit\n" + 
            "ou:: 5Za25qWt6YOo\n" + 
            "# ou:: 営業部\n" + 
            "ou;lang-ja:: 5Za25qWt6YOo\n" + 
            "# ou;lang-ja:: 営業部\n" + 
            "ou;lang-ja;phonetic:: 44GI44GE44GO44KH44GG44G2\n" + 
            "# ou;lang-ja:: えいぎょうぶ\n" + 
            "ou;lang-en: Sales\n" + 
            "description: Japanese office\n";

        LdifAttributesReader reader = new LdifAttributesReader();
        Attributes attributes = reader.parseAttributes( ldif );

        String[][] values =
            {
                { "objectclass", "top" },
                { "objectclass", "organizationalUnit" },
                { "ou", "\u55b6\u696d\u90e8" },
                { "ou;lang-ja", "\u55b6\u696d\u90e8" },
                { "ou;lang-ja;phonetic", "\u3048\u3044\u304e\u3087\u3046\u3076" }, // 3048 = え, 3044 = い, 304e = ぎ
                                                                                // 3087 = ょ, 3046 = う, 3076 = ぶ
                { "ou;lang-en", "Sales" },
                { "description", "Japanese office" }
            }; 

        for ( int j = 0; j < values.length; j++ )
        {
            Attribute attr = attributes.get( values[j][0] );

            if ( attr.contains( values[j][1] ) )
            {
                assertTrue( true );
            }
            else
            {
                assertTrue( attr.contains( values[j][1].getBytes( "UTF-8" ) ) );
            }
        }
    }

    
    @Test public void testLdifParserRFC2849Sample5() throws NamingException, Exception
    {
        String ldif = 
            "objectclass: top\n" + 
            "objectclass: person\n" + 
            "objectclass: organizationalPerson\n" + 
            "cn: Horatio Jensen\n" + 
            "cn: Horatio N Jensen\n" + 
            "sn: Jensen\n" + 
            "uid: hjensen\n" + 
            "telephonenumber: +1 408 555 1212\n" + 
            "jpegphoto:< file:" + HJENSEN_JPEG_FILE.getAbsolutePath() + "\n";

        LdifAttributesReader reader = new LdifAttributesReader();
        Attributes attributes = reader.parseAttributes( ldif );

        String[][] values =
            {
                { "objectclass", "top" },
                { "objectclass", "person" },
                { "objectclass", "organizationalPerson" },
                { "cn", "Horatio Jensen" },
                { "cn", "Horatio N Jensen" },
                { "sn", "Jensen" },
                { "uid", "hjensen" },
                { "telephonenumber", "+1 408 555 1212" },
                { "jpegphoto", null } 
            };

        for ( int i = 0; i < values.length; i++ )
        {
            if ( "jpegphoto".equalsIgnoreCase( values[i][0] ) )
            {
                Attribute attr = attributes.get( values[i][0] );
                assertEquals( StringTools.dumpBytes( data ), StringTools.dumpBytes( (byte[]) attr.get() ) );
            }
            else
            {
                Attribute attr = attributes.get( values[i][0] );

                if ( attr.contains( values[i][1] ) )
                {
                    assertTrue( true );
                }
                else
                {
                    assertTrue( attr.contains( values[i][1].getBytes( "UTF-8" ) ) );
                }
            }
        }
    }

    
    @Test public void testLdifParserRFC2849Sample5WithSizeLimit() throws Exception
    {
        String ldif = 
            "objectclass: top\n" + 
            "objectclass: person\n" + 
            "objectclass: organizationalPerson\n" + 
            "cn: Horatio Jensen\n" + 
            "cn: Horatio N Jensen\n" + 
            "sn: Jensen\n" + 
            "uid: hjensen\n" + 
            "telephonenumber: +1 408 555 1212\n" + 
            "jpegphoto:< file:" + HJENSEN_JPEG_FILE.getAbsolutePath() + "\n";

        LdifAttributesReader reader = new LdifAttributesReader();
        reader.setSizeLimit( 128 );

        try
        {
            reader.parseAttributes( ldif );
            fail();
        }
        catch (NamingException ne)
        {
            assertEquals( "Error while parsing the ldif buffer", ne.getMessage() );
        }
    }

    
    @Test public void testLdifAttributesReaderDirServer() throws NamingException, Exception
    {
        String ldif = 
            "# -------------------------------------------------------------------\n" +
            "#\n" +
            "#  Licensed to the Apache Software Foundation (ASF) under one\n" +
            "#  or more contributor license agreements.  See the NOTICE file\n" +
            "#  distributed with this work for additional information\n" +
            "#  regarding copyright ownership.  The ASF licenses this file\n" +
            "#  to you under the Apache License, Version 2.0 (the\n" +
            "#  \"License\"); you may not use this file except in compliance\n" +
            "#  with the License.  You may obtain a copy of the License at\n" +
            "#  \n" +
            "#    http://www.apache.org/licenses/LICENSE-2.0\n" +
            "#  \n" +
            "#  Unless required by applicable law or agreed to in writing,\n" +
            "#  software distributed under the License is distributed on an\n" +
            "#  \"AS IS\" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY\n" +
            "#  KIND, either express or implied.  See the License for the\n" +
            "#  specific language governing permissions and limitations\n" +
            "#  under the License. \n" +
            "#  \n" +
            "#\n" +
            "# EXAMPLE.COM is freely and reserved for testing according to this RFC:\n" +
            "#\n" +
            "# http://www.rfc-editor.org/rfc/rfc2606.txt\n" +
            "#\n" +
            "# -------------------------------------------------------------------\n" +
            "\n" +
            "objectclass: top\n" +
            "objectclass: organizationalunit\n" +
            "ou: Users";
            
        LdifAttributesReader reader = new LdifAttributesReader();

        Attributes attributes = reader.parseAttributes( ldif );

        Attribute attr = attributes.get( "objectclass" );
        assertTrue( attr.contains( "top" ) );
        assertTrue( attr.contains( "organizationalunit" ) );

        attr = attributes.get( "ou" );
        assertTrue( attr.contains( "Users" ) );
    }

    
    @Test public void testLdifParserCommentsEmptyLines() throws NamingException, Exception
    {
        String ldif = 
            "#\n" +
            "#  Licensed to the Apache Software Foundation (ASF) under one\n" +
            "#  or more contributor license agreements.  See the NOTICE file\n" +
            "#  distributed with this work for additional information\n" +
            "#  regarding copyright ownership.  The ASF licenses this file\n" +
            "#  to you under the Apache License, Version 2.0 (the\n" +
            "#  \"License\"); you may not use this file except in compliance\n" +
            "#  with the License.  You may obtain a copy of the License at\n" +
            "#  \n" +
            "#    http://www.apache.org/licenses/LICENSE-2.0\n" +
            "#  \n" +
            "#  Unless required by applicable law or agreed to in writing,\n" +
            "#  software distributed under the License is distributed on an\n" +
            "#  \"AS IS\" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY\n" +
            "#  KIND, either express or implied.  See the License for the\n" +
            "#  specific language governing permissions and limitations\n" +
            "#  under the License. \n" +
            "#  \n" +
            "#\n" +
            "#\n" +
            "#   EXAMPLE.COM is freely and reserved for testing according to this RFC:\n" +
            "#\n" +
            "#   http://www.rfc-editor.org/rfc/rfc2606.txt\n" +
            "#\n" +
            "#\n" +
            "\n" +
            "#\n" +
            "# This ACI allows brouse access to the root suffix and one level below that to anyone.\n" +
            "# At this level there is nothing critical exposed.  Everything that matters is one or\n" +
            "# more levels below this.\n" +
            "#\n" +
            "\n" +
            "objectClass: top\n" +
            "objectClass: subentry\n" +
            "objectClass: accessControlSubentry\n" +
            "subtreeSpecification: { maximum 1 }\n" +
            "prescriptiveACI: { identificationTag \"browseRoot\", precedence 100, authenticationLevel none, itemOrUserFirst userFirst: { userClasses { allUsers }, userPermissions { { protectedItems {entry}, grantsAndDenials { grantReturnDN, grantBrowse } } } } }\n";

        LdifAttributesReader reader = new LdifAttributesReader();
        Attributes attributes = reader.parseAttributes( ldif );

        Attribute attr = attributes.get( "objectClass" );
        assertTrue( attr.contains( "top" ) );
        assertTrue( attr.contains( SchemaConstants.SUBENTRY_OC ) );
        assertTrue( attr.contains( "accessControlSubentry" ) );

        attr = attributes.get( "subtreeSpecification" );
        assertTrue( attr.contains( "{ maximum 1 }" ) );

        attr = attributes.get( "prescriptiveACI" );
        assertTrue( attr.contains( "{ identificationTag \"browseRoot\", precedence 100, authenticationLevel none, itemOrUserFirst userFirst: { userClasses { allUsers }, userPermissions { { protectedItems {entry}, grantsAndDenials { grantReturnDN, grantBrowse } } } } }" ) );
    }
}
