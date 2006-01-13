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

package org.apache.ldap.common.name;


import javax.naming.NamingException;

import org.apache.ldap.common.schema.Normalizer;


/**
 * A simple NameComponentNormalizer which uses the same Normalizer to always
 * normalize the value the same way regardless of the attribute the value is
 * for.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class SimpleNameComponentNormalizer implements NameComponentNormalizer
{
    /** the normalizer used to normalize the value every time */
    private final Normalizer normalizer;


    /**
     * Creates a new SimpleNameComponentNormalizer with the normalizer it uses
     * ever time irrespective of the attribute name or oid.
     *
     * @param normalizer the Normalizer to use for all normalization requests
     */
    public SimpleNameComponentNormalizer( Normalizer normalizer )
    {
        this.normalizer = normalizer;
    }

    public String normalizeByName( String name, String val ) throws NamingException
    {
        return (String)normalizer.normalize( val );
    }

    public String normalizeByName( String name, byte[] val ) throws NamingException
    {
        return (String)normalizer.normalize( val );
    }

    public String normalizeByOid( String oid, String val ) throws NamingException
    {
        return (String)normalizer.normalize( val );
    }

    public String normalizeByOid( String oid, byte[] val ) throws NamingException
    {
        return (String)normalizer.normalize( val );
    }

    public boolean isDefined( String oid )
    {
        return true;
    }
}
