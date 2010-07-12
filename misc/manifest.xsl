<?xml version="1.0" encoding="utf-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:android="http://schemas.android.com/apk/res/android"
                version="1.0">

  <xsl:output method="xml" indent="yes"/>
  <xsl:param name="git-id"/>

  <xsl:template match="//activity">
    <xsl:copy>
      <xsl:apply-templates select="@*|node()"/>
      <meta-data android:name="git-id">
        <xsl:attribute name="android:value">
          <xsl:value-of select="$git-id"/>
        </xsl:attribute>
      </meta-data>
    </xsl:copy>
  </xsl:template>

  <xsl:template match="@*|node()">
    <xsl:copy>
      <xsl:apply-templates select="@*|node()"/>
    </xsl:copy>
  </xsl:template>
  
</xsl:stylesheet>
