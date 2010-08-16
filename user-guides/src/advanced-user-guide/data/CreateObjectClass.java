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
package examples.schema;

import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.BasicAttribute;
import javax.naming.directory.BasicAttributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;

public class CreateObjectClass {

    public static void main(String[] args) throws NamingException {

        DirContext ctx = new InitialDirContext();
        DirContext schema = ctx.getSchema("");

        Attributes attrs = new BasicAttributes(true);
        attrs.put("NUMERICOID", "1.3.6.1.4.1.18060.0.4.3.3.1");
        attrs.put("NAME", "ship");
        attrs.put("DESC", "An entry which represents a ship");
        attrs.put("SUP", "top");
        attrs.put("STRUCTURAL", "true");

        Attribute must = new BasicAttribute("MUST");
        must.add("cn");
        attrs.put(must);

        Attribute may = new BasicAttribute("MAY");
        may.add("numberOfGuns");
        may.add("description");
        attrs.put(may);

        // attrs.put("X-SCHEMA", "sevenSeas");

        schema.createSubcontext("ClassDefinition/ship", attrs);
    }

}
