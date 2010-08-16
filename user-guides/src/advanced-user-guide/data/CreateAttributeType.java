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
import javax.naming.directory.Attributes;
import javax.naming.directory.BasicAttributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;

public class CreateAttributeType {

    public static void main(String[] args) throws NamingException {

        DirContext ctx = new InitialDirContext();
        DirContext schema = ctx.getSchema("");

        Attributes attrs = new BasicAttributes(true);
        attrs.put("NUMERICOID", "1.3.6.1.4.1.18060.0.4.3.2.1");
        attrs.put("NAME", "numberOfGuns");
        attrs.put("DESC", "Number of guns of a ship");
        attrs.put("EQUALITY", "integerOrderingMatch");
        attrs.put("SYNTAX", "1.3.6.1.4.1.1466.115.121.1.27");
        attrs.put("SINGLE-VALUE", "true");

        // attrs.put("X-SCHEMA", "sevenSeas");

        schema.createSubcontext("AttributeDefinition/numberOfGuns", attrs);
    }
}
