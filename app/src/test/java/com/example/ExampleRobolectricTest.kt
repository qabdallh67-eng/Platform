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
        assertEquals(1, repository.courses.value.size)
        assertEquals("عبد الله", repository.courses.value.single().title)
        assertEquals(0.0, repository.walletAccount.value.balance, 0.001)
    }
}
