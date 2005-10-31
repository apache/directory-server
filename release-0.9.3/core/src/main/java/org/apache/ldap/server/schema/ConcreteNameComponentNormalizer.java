/*
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
package org.apache.ldap.server.schema;


import javax.naming.NamingException;

import org.apache.ldap.common.name.NameComponentNormalizer;
import org.apache.ldap.common.schema.AttributeType;
import org.apache.ldap.common.schema.Normalizer;


/**
 * A DN Name component Normalizer which uses the bootstrap registries to find
 * the appropriate normalizer for the attribute of the name component with which
 * to normalize the name component value.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class ConcreteNameComponentNormalizer implements NameComponentNormalizer
{
    /** the at registry used to dynamically resolve Normalizers */
    private final AttributeTypeRegistry registry;


    /**
     * Creates a DN Name component Normalizer which uses the bootstrap
     * registries to find the appropriate normalizer for the attribute of the
     * name component with which to normalize the name component value.
     *
     * @param registry the at registry used to dynamically resolve Normalizers
     */
    public ConcreteNameComponentNormalizer( AttributeTypeRegistry registry )
    {
        this.registry = registry;
    }


    /**
     * @see NameComponentNormalizer#normalizeByName(String, String)
     */
    public String normalizeByName( String name, String value ) throws NamingException
    {
        return lookup( name ).normalize( value ).toString();
    }


    /**
     * @see NameComponentNormalizer#normalizeByOid(String, String)
     */
    public String normalizeByOid( String oid, String value ) throws NamingException
    {
        return lookup( oid ).normalize( value ).toString();
    }


    /**
     * Looks up the Normalizer to use for a name component using the attributeId
     * for the name component.  First the attribute is resolved, then its
     * equality matching rule is looked up.  The normalizer of that matching
     * rule is returned.
     *
     * @param id the name or oid of the attribute in the name component to
     * normalize the value of
     * @return the Normalizer to use for normalizing the value of the attribute
     * @throws NamingException if there are failures resolving the Normalizer
     */
    private Normalizer lookup( String id ) throws NamingException
    {
        AttributeType type = registry.lookup( id );
        return type.getEquality().getNormalizer();
    }


    /**
     * @see NameComponentNormalizer#isDefined(String)
     */
    public boolean isDefined( String id )
    {
        return registry.hasAttributeType( id );
    }
}
