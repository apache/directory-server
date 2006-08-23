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
package org.apache.directory.server.core.partition;


/**
 * Provides constants of private OIDs.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class Oid
{
    /** Private OID (1.2.6.1.4.1.18060.1.1.1.3.1) for _ndn op attrib */
    public static final String NDN = "1.2.6.1.4.1.18060.1.1.1.3.1";
    /** Private OID (1.2.6.1.4.1.18060.1.1.1.3.2) for _updn op attrib */
    public static final String UPDN = "1.2.6.1.4.1.18060.1.1.1.3.2";
    /** Private OID (1.2.6.1.4.1.18060.1.1.1.3.3) for _existance op attrib */
    public static final String EXISTANCE = "1.2.6.1.4.1.18060.1.1.1.3.3";
    /** Private OID (1.2.6.1.4.1.18060.1.1.1.3.4) for _hierarchy op attrib */
    public static final String HIERARCHY = "1.2.6.1.4.1.18060.1.1.1.3.4";
    /** Private OID (1.2.6.1.4.1.18060.1.1.1.3.5) for _oneAlias index */
    public static final String ONEALIAS = "1.2.6.1.4.1.18060.1.1.1.3.5";
    /** Private OID (1.2.6.1.4.1.18060.1.1.1.3.6) for _subAlias index */
    public static final String SUBALIAS = "1.2.6.1.4.1.18060.1.1.1.3.6";
    /** Private OID (1.2.6.1.4.1.18060.1.1.1.3.7) for _alias index */
    public static final String ALIAS = "1.2.6.1.4.1.18060.1.1.1.3.7";


    private Oid()
    {
    }
}
