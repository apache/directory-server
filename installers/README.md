> Licensed to the Apache Software Foundation (ASF) under one
> or more contributor license agreements.  See the NOTICE file
> distributed with this work for additional information
> regarding copyright ownership.  The ASF licenses this file
> to you under the Apache License, Version 2.0 (the
> "License"); you may not use this file except in compliance
> with the License.  You may obtain a copy of the License at
>
>    http://www.apache.org/licenses/LICENSE-2.0
>
> Unless required by applicable law or agreed to in writing,
> software distributed under the License is distributed on an
> "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
> KIND, either express or implied.  See the License for the
> specific language governing permissions and limitations
> under the License.

# Debugging a Maven plugin under Eclipse

Debugging one of the installers maven plugins in Eclipse is quite easy:

* First set a breakpoint in the _execute()_ function of the plugin you are interested in debugging
* Create a new *Maven Build* debug congiguration
* Set the base Directory to _${workspace_loc:/apacheds-installers-2.0.0.AM26-SNAPSHOT}_ (or whatever version you are debugging). This can be done using the _workspace_ button below the input box. 
* Set the goals to _clean install_
* Set the _profile_ to match the plugin you want to debug (*mac*, *bin*, *debian*, *rpm*, *windows*, *archive*, *docker*, or *installers*)
* You are good to go !

