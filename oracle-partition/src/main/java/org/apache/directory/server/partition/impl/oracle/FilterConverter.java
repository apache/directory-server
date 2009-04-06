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
package org.apache.directory.server.partition.impl.oracle;

import java.util.List;

import org.apache.directory.server.core.DirectoryService;
import org.apache.directory.shared.ldap.entry.client.ClientStringValue;
import org.apache.directory.shared.ldap.filter.ExprNode;
import org.apache.directory.shared.ldap.filter.LeafNode;
import org.apache.directory.shared.ldap.filter.SimpleNode;
import org.apache.directory.shared.ldap.filter.SubstringNode;

import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;

/**
 * This <a href="http://xstream.codehaus.org/">XStream</a> converter
 * is used to lookup attribute oids on Filter ({@link ExprNode}}) marshalling and to transform
 * the a Filter instance in a usefull form for the pl/sql code
 */
public final class FilterConverter implements Converter {
	
	private DirectoryService directoryService;

	/**
	 * Create a FilterConverter instance with a
	 * reference to the directoryService containing the registries
	 * used to lookup attributes OIDs
	 * @param directoryService containing registries
	 */
	public FilterConverter(DirectoryService directoryService)
	{
	  this.directoryService= directoryService;	
	}

	/**
	 * Called form XStream to convert handled instances
	 */
	public void marshal(Object obj, 
			            HierarchicalStreamWriter writer,
			            MarshallingContext ctx)
	{
		if (obj instanceof ClientStringValue)
		  writer.setValue(obj.toString());
		else
		{
		  writer.startNode("attribute");
		  
		  try
		  {
			 writer.setValue(OraclePartition.normAtt(directoryService, ((LeafNode)obj).getAttribute()));
		  }
		  catch (Exception e)
		  {
			e.printStackTrace();
			throw new RuntimeException(e);
	      }
		  
		  writer.endNode();
		  
		  if (obj instanceof SimpleNode)
		  {
			  writer.startNode("value");
			  writer.setValue(((SimpleNode)obj).getValue().toString());
			  writer.endNode();
		  }
		  else
          if (obj instanceof SubstringNode)
          {
              SubstringNode sn= ((SubstringNode)obj);
              List<String> any= sn.getAny();
              writer.startNode("value");
              String value= "";
              
              if (sn.getInitial()!=null)
                value+= sn.getInitial()+"%";
              
              if (any!=null)
              {
                 for (String s: any)
                   value+= "%"+s+"%";
              }
              
              if (sn.getFinal()!=null)
                  value+= "%"+sn.getFinal();
              
              writer.setValue(value);
              writer.endNode();
          }
		}
	}

	/**
	 * Unused: the pl/sql will unmarshal the xml
	 */
	public Object unmarshal(HierarchicalStreamReader reader,
	                 		UnmarshallingContext     ctx)
	{
		return null;
	}

	/**
	 * Tells xstream which kind of classes this converter
	 * can transform
	 */
	public boolean canConvert(Class clazz) {
		return clazz.equals(ClientStringValue.class)
		       ||LeafNode.class.equals(clazz.getSuperclass())
		       ||SimpleNode.class.equals(clazz.getSuperclass());
		
	}
}
