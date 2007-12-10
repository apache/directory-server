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

package org.apache.directory.server.dns.util;


/**
 * An interface that allows an Enum to be converted to another type, such as an
 * integer or long.  Useful in cases where the Java assigned ordinal just isn't
 * reliable enough or is unable to represent the values we need.<p>
 * 
 * Implementers should also implement (though there is no way of requiring it)
 * a static method for taking the conversion the other way:
 * 
 * <code>
 *   public static Enum convert (K value);
 * </code>
 * 
 * @param <K> 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public interface EnumConverter<K>
{
    /**
     * Convert the enum to another type.
     *
     * @return The other type.
     */
    K convert();
}
