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
package org.apache.directory.shared.ldap.schema;


import java.io.Serializable;


/**
 * No op (pass through or do nothing) normalizer returning what its given.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class NoOpNormalizer implements Normalizer, Serializable
{
    static final long serialVersionUID = -7817763636668562489L;


    /**
     * Creates a do nothing normalizer.
     */
    public NoOpNormalizer()
    {
    }


    /**
     * Returns the value argument as-is without alterations all the time.
     * 
     * @param value
     *            any value
     * @return the value argument returned as-is
     * @see org.apache.directory.shared.ldap.schema.Normalizer#normalize(java.lang.Object)
     */
    public Object normalize( Object value )
    {
        return value;
    }
}
