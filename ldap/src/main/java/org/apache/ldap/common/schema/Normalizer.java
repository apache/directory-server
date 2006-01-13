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
package org.apache.ldap.common.schema ;


import javax.naming.NamingException ;


/**
 * Converts attribute values to a canonical form.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public interface Normalizer
{
    /**
     * Gets the normalized value.
     *
     * @param value the value to normalize. It must *not* be null !
     * @return the normalized form for a value
     * @throws NamingException if an error results during normalization
     */
    Object normalize( Object value ) throws NamingException ;
}
