package com.example

import com.example.data.repository.EduRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class ExampleRobolectricTest {

    @Test
    fun testAppBrandingAndRepositoryInitialization() {
        val repository = EduRepository()
        assertNotNull(repository)
        assertEquals("أبو قاعود", repository.appSettings.value.appName)
        assertEquals(3, repository.courses.value.size)
    }
}
