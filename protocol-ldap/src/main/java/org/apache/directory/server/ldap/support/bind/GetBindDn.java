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
package org.apache.directory.server.ldap.support.bind;


import javax.naming.Name;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.SearchResult;

import org.apache.directory.server.protocol.shared.store.ContextOperation;
import org.apache.directory.shared.ldap.message.AttributeImpl;
import org.apache.directory.shared.ldap.message.AttributesImpl;
import org.apache.directory.shared.ldap.util.StringTools;


/**
 * Encapsulates the action of looking up a user in an embedded ApacheDS DIT.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev: 494161 $, $Date: 2007-01-08 11:39:36 -0800 (Mon, 08 Jan 2007) $
 */
public class GetBindDn implements ContextOperation
{
    private static final long serialVersionUID = 4598007518413451945L;

    /** The name of the principal to get. */
    private String username;

    private String bindDn;
    private String userPassword;


    /**
     * Creates the action to be used against the embedded ApacheDS DIT.
     * 
     * @param username The username to search for in the directory.
     */
    public GetBindDn( String username )
    {
        this.username = username;
    }


    /**
     * Accessor method for retrieving the DN for the username.
     *
     * @return The DN to bind the user as.
     */
    public String getBindDn()
    {
        return bindDn;
    }


    /**
     * Accessor method for retrieving the user's password.
     *
     * @return The user's password.
     */
    public String getUserPassword()
    {
        return userPassword;
    }


    /**
     * Note that the base is a relative path from the existing context.
     * It is not a DN.
     */
    public Object execute( DirContext ctx, Name base )
    {
        if ( username == null )
        {
            return null;
        }

        String[] attrIDs =
            { "userPassword" };

        Attributes matchAttrs = new AttributesImpl( true );
        matchAttrs.put( new AttributeImpl( "uid", username ) );

        try
        {
            NamingEnumeration<SearchResult> answer = ctx.search( "", matchAttrs, attrIDs );

            if ( answer.hasMore() )
            {
                SearchResult result = answer.next();

                // Changed from original GetPrincipal, along with accessor and member variable.
                bindDn = result.getName();

                Attributes attrs = result.getAttributes();

                if ( attrs == null )
                {
                    return null;
                }

                Object userPassword;
                Attribute userPasswordAttr = attrs.get( "userPassword" );

                if ( userPasswordAttr == null )
                {
                    userPassword = "";
                }
                else
                {
                    userPassword = userPasswordAttr.get();

                    if ( userPassword instanceof byte[] )
                    {
                        userPassword = StringTools.asciiBytesToString( ( byte[] ) userPassword );
                    }
                }

                this.userPassword = ( String ) userPassword;
            }
        }
        catch ( NamingException e )
        {
            return null;
        }

        return null;
    }
}
