package com.example

import com.example.data.repository.EduRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class ExampleUnitTest {
    private lateinit var repository: EduRepository

    @Before
    fun setup() {
        repository = EduRepository()
    }

    @Test
    fun unauthenticatedStudentCannotStartExam() {
        val result = repository.startExam("exam-math-1")
        assertTrue(result.isFailure)
        assertEquals("سجّل الدخول أولًا", result.exceptionOrNull()?.message)
    }

    @Test
    fun demoLessonsAndExamsAreNotInjectedIntoNewInstall() {
        assertTrue(repository.courseModules.value.isEmpty())
        assertTrue(repository.exams.value.isEmpty())
        assertTrue(repository.examAttempts.value.isEmpty())
    }

    @Test
    fun newWalletStartsAtZeroAndHasNoFakeTransactions() {
        assertEquals(0.0, repository.walletAccount.value.balance, 0.001)
        assertTrue(repository.transactions.value.isEmpty())
    }

    @Test
    fun coursePurchaseCannotUseDemoCourseOrLocalBalance() = runBlocking {
        val result = repository.purchaseCourse("course-physics")
        assertTrue(result.isFailure)
        assertEquals(0.0, repository.walletAccount.value.balance, 0.001)
    }
}
