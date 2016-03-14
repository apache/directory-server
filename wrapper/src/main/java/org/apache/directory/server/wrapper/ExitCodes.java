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
package org.apache.directory.server.wrapper;


/**
 * Exit codes for the bootstrappers.
 * Final reference -> class shouldn't be extended
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public final class ExitCodes
{
    /**
     *  Ensures no construction of this class, also ensures there is no need for final keyword above
     *  (Implicit super constructor is not visible for default constructor),
     *  but is still self documenting.
     */
    private ExitCodes()
    {
    }

    public static final int CLASS_LOOKUP = 1;
    public static final int INSTANTIATION = 2;
    public static final int METHOD_LOOKUP = 3;
    public static final int INITIALIZATION = 4;
    public static final int START = 5;
    public static final int STOP = 6;
    public static final int PROPLOAD = 7;
    public static final int VERIFICATION = 8;
    public static final int DESTROY = 9;
    public static final int BAD_ARGUMENTS = 10;
    public static final int BAD_COMMAND = 11;
    public static final int UNKNOWN = 12;
    public static final int INVOCATION = 13;
}
