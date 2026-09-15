package com.match.app.domain.profile

import org.junit.Assert.assertTrue
import org.junit.Test

class IndiaProfileCatalogTest {
    @Test fun `catalog covers representative regions across India`() {
        val states = IndiaProfileCatalog.statesAndUnionTerritories
        assertTrue("Telangana" in states)
        assertTrue("Karnataka" in states)
        assertTrue("Odisha" in states)
        assertTrue("Tamil Nadu" in states)
        assertTrue("Punjab" in states)
        assertTrue("West Bengal" in states)
        assertTrue("Jammu and Kashmir" in states)
        assertTrue("Delhi" in states)
    }

    @Test fun `state language suggestions do not remove member choice`() {
        assertTrue("Telugu" in IndiaProfileCatalog.languageSuggestionsForState("Telangana"))
        assertTrue("Kannada" in IndiaProfileCatalog.languageSuggestionsForState("Karnataka"))
        assertTrue("Odia" in IndiaProfileCatalog.languageSuggestionsForState("Odisha"))
        assertTrue("Punjabi" in IndiaProfileCatalog.languageSuggestionsForState("Punjab"))
        assertTrue("Other" in IndiaProfileCatalog.indianLanguages)
    }
}
