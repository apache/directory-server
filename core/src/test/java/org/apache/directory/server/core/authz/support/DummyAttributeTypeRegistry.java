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
package org.apache.directory.server.core.authz.support;

import java.util.ArrayList;
import java.util.Iterator;

import javax.naming.NamingException;

import org.apache.directory.server.core.authz.support.ACITupleFilter;
import org.apache.directory.server.core.schema.AttributeTypeRegistry;
import org.apache.directory.shared.ldap.schema.AttributeType;
import org.apache.directory.shared.ldap.schema.MatchingRule;
import org.apache.directory.shared.ldap.schema.Syntax;
import org.apache.directory.shared.ldap.schema.UsageEnum;

/**
 * A mock {@link AttributeTypeRegistry} to test {@link ACITupleFilter} implementations.
 *
 * @author The Apache Directory Project
 * @version $Rev$, $Date$
 *
 */
public class DummyAttributeTypeRegistry implements AttributeTypeRegistry
{
    private final boolean returnOperational;

    public DummyAttributeTypeRegistry( boolean returnOperational )
    {
        this.returnOperational = returnOperational;
    }

    public void register( String schema, AttributeType attributeType ) throws NamingException
    {
    }

    public AttributeType lookup( final String id ) throws NamingException
    {
        if( returnOperational )
        {
            return new AttributeType()
            {
                public boolean isSingleValue()
                {
                    return false;
                }

                public boolean isCanUserModify()
                {
                    return false;
                }

                public boolean isCollective()
                {
                    return false;
                }

                public UsageEnum getUsage()
                {
                    return null;
                }

                public AttributeType getSuperior() throws NamingException
                {
                    return null;
                }

                public Syntax getSyntax() throws NamingException
                {
                    return null;
                }

                public int getLength()
                {
                    return 0;
                }

                public MatchingRule getEquality() throws NamingException
                {
                    return null;
                }

                public MatchingRule getOrdering() throws NamingException
                {
                    return null;
                }

                public MatchingRule getSubstr() throws NamingException
                {
                    return null;
                }

                public boolean isObsolete()
                {
                    return false;
                }

                public String getOid()
                {
                    return String.valueOf( id.hashCode() );
                }

                public String[] getNames()
                {
                    return new String[] { id };
                }

                public String getName()
                {
                    return id;
                }

                public String getDescription()
                {
                    return id;
                }
            };
        }
        else
        {
            return new AttributeType()
            {
                public boolean isSingleValue()
                {
                    return false;
                }

                public boolean isCanUserModify()
                {
                    return true;
                }

                public boolean isCollective()
                {
                    return false;
                }

                public UsageEnum getUsage()
                {
                    return null;
                }

                public AttributeType getSuperior() throws NamingException
                {
                    return null;
                }

                public Syntax getSyntax() throws NamingException
                {
                    return null;
                }

                public int getLength()
                {
                    return 0;
                }

                public MatchingRule getEquality() throws NamingException
                {
                    return null;
                }

                public MatchingRule getOrdering() throws NamingException
                {
                    return null;
                }

                public MatchingRule getSubstr() throws NamingException
                {
                    return null;
                }

                public boolean isObsolete()
                {
                    return false;
                }

                public String getOid()
                {
                    return String.valueOf( id.hashCode() );
                }

                public String[] getNames()
                {
                    return new String[] { id };
                }

                public String getName()
                {
                    return id;
                }

                public String getDescription()
                {
                    return id;
                }
            };
        }
    }

    public String getSchemaName( String id ) throws NamingException
    {
        return "dummy";
    }

    public boolean hasAttributeType( String id )
    {
        return true;
    }

    public Iterator list()
    {
        return new ArrayList().iterator();
    }

}
