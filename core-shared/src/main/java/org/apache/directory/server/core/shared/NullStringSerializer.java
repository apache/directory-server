/*
 *   Licensed to the Apache Software Foundation (ASF) under one
 *   or more contributor license agreements.  See the NOTICE file
 *   distributed with this work for additional information
 *   regarding copyright ownership.  The ASF licenses this file
 *   to you under the Apache License, Version 2.0 (the
 *   "License"); you may not use this file except in compliance
 *   with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing,
 *   software distributed under the License is distributed on an
 *   "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *   KIND, either express or implied.  See the License for the
 *   specific language governing permissions and limitations
 *   under the License.
 *
 */

package org.apache.directory.server.core.shared;


import java.io.IOException;

import org.apache.directory.api.util.Strings;

import jdbm.helper.Serializer;


/**
 * A JDBM Serializer that serializes null or empty strings and always deserializes them as empty string values.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class NullStringSerializer implements Serializer
{

    public static final NullStringSerializer INSTANCE = new NullStringSerializer();


    @Override
    public Object deserialize( byte[] data ) throws IOException
    {
        return "";
    }


    @Override
    public byte[] serialize( Object nullStr ) throws IOException
    {
        return Strings.EMPTY_BYTES;
    }

}
