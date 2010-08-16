<?xml version="1.0" encoding="UTF-8"?>
<!--
Licensed to the Apache Software Foundation (ASF) under one
or more contributor license agreements.  See the NOTICE file
distributed with this work for additional information
regarding copyright ownership.  The ASF licenses this file
to you under the Apache License, Version 2.0 (the
"License"); you may not use this file except in compliance
with the License.  You may obtain a copy of the License at

  http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing,
software distributed under the License is distributed on an
"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
KIND, either express or implied.  See the License for the
specific language governing permissions and limitations
under the License.
-->
<!DOCTYPE stylesheet [
<!ENTITY green_css SYSTEM "../../main/resources/css/green_20091029.css">
<!ENTITY common_css SYSTEM "../../main/resources/css/common_20091029.css">
]>

<xsl:stylesheet
  xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
  version="1.0">

  <!-- imports the original docbook stylesheet -->
  <xsl:import
    href="urn:docbkx:stylesheet" />

  <!-- Important links: - http://www.sagehill.net/docbookxsl/ - http://docbkx-tools.sourceforge.net/ -->
  
  <!-- set bellow all your custom xsl configuration -->

  <xsl:param
    name="section.autolabel"
    select="1" />
  <xsl:param
    name="toc.section.depth"
    select="4" />
  <xsl:param
    name="section.label.includes.component.label"
    select="2" />

  <!-- Add graphic symbols to admonitions -->
  <xsl:param name="admon.graphics" select="1"/>
  <xsl:param name="admon.graphics.extension">.gif</xsl:param>

  <xsl:template
    name="user.head.content">
    <style
      type="text/css">
&green_css;
&common_css;
    </style>
    <link rel="shortcut icon" href="images/server-icon_16x16.png" />
  </xsl:template>

  <xsl:template match="processing-instruction('linebreak')">
    <br/>
  </xsl:template>

  <xsl:template name="user.footer.navigation">
    <hr/>
    <table
      class="copyrightfooter"
      width="100%">
      <tbody>
        <tr>
          <td align="center">
            © 2003-2010,
            <a
              href="http://www.apache.org/">The Apache Software Foundation</a>
            - <a href="http://directory.apache.org/privacy-policy.html">Privacy Policy</a>
          </td>
        </tr>
      </tbody>
    </table>
  </xsl:template>

</xsl:stylesheet>