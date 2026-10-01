package com.match.app.ui.common

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SecureMediaModelTest {

    @Test
    fun `canonical profile storage paths are protected media`() {
        assertTrue(isProtectedFirebaseStorageSource("photos/alice/profile.jpg"))
        assertTrue(isProtectedFirebaseStorageSource("videos/alice/intro.mp4"))
        assertTrue(isProtectedFirebaseStorageSource("voicebios/alice/intro.m4a"))
        assertTrue(isProtectedFirebaseStorageSource("gs://example.appspot.com/videos/alice/intro.mp4"))
    }

    @Test
    fun `ordinary local values are not classified as protected storage`() {
        assertFalse(isProtectedFirebaseStorageSource(""))
        assertFalse(isProtectedFirebaseStorageSource("/data/user/0/com.match.app/files/photo.jpg"))
    }
}
