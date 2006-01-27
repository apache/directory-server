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
package org.apache.directory.server.standalone.daemon;


/**
 * Exit codes for the bootstrappers.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public interface ExitCodes
{
    int CLASS_LOOKUP = 1;
    int INSTANTIATION = 2;
    int METHOD_LOOKUP = 3;
    int INITIALIZATION = 4;
    int START = 5;
    int STOP = 6;
    int PROPLOAD = 7;
    int VERIFICATION = 8;
    int DESTROY = 9;
    int BAD_ARGUMENTS = 10;
    int BAD_COMMAND = 11;
    int UNKNOWN = 12;
}
