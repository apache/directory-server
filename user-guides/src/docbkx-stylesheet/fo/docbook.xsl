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

<xsl:stylesheet
  xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
  xmlns:fo="http://www.w3.org/1999/XSL/Format"
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

  <xsl:template name="footer.content">
    <xsl:param name="pageclass" select="''"/>
    <xsl:param name="sequence" select="''"/>
    <xsl:param name="position" select="''"/>
    <xsl:param name="gentext-key" select="''"/>
    <xsl:choose>
      <xsl:when test="$position = 'left'">
        <fo:block />
        <fo:block>© 2003-2010</fo:block>
      </xsl:when>
      <xsl:when test="$position = 'center'">
        <fo:block>
          <fo:page-number />
        </fo:block>
        <fo:block>
          <fo:basic-link external-destination="url('http://www.apache.org/')"
               text-decoration="underline"
               color="blue">The Apache Software Foundation</fo:basic-link>
        </fo:block>
      </xsl:when>
      <xsl:when test="$position = 'right'">
        <fo:block />
        <fo:block>
          <fo:basic-link external-destination="url('http://directory.apache.org/privacy-policy.html')"
               text-decoration="underline"
               color="blue">Privacy Policy</fo:basic-link>
        </fo:block>
      </xsl:when>
    </xsl:choose>
  </xsl:template>  

  <xsl:template match="processing-instruction('linebreak')">
    <fo:block/>
  </xsl:template>
  
</xsl:stylesheet>