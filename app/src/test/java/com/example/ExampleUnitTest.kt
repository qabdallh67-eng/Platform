package com.example

import com.example.data.model.*
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
    fun testOneAttemptPolicyEnforcement() {
        val examId = "exam-math-1"
        // First attempt succeeds and starts
        val res1 = repository.startExam(examId)
        assertTrue(res1.isSuccess)
        val attempt1 = res1.getOrThrow()
        assertEquals(1, attempt1.attemptNumber)

        // Submit attempt 1
        repository.submitExam(attempt1.id, mapOf("q-1" to "٣ جتا(٣س)", "q-2" to "صواب"), 120)

        // Second attempt on a 1-attempt exam MUST fail with policy message
        val res2 = repository.startExam(examId)
        assertFalse("Second attempt should be rejected under strict policy", res2.isSuccess)
        assertTrue(res2.exceptionOrNull()?.message?.contains("استنفدت") == true)
    }

    @Test
    fun testMandatoryExamBlocksNextLessonUntilPassed() {
        val modules = repository.courseModules.value["course-math"]!!
        val lessons = modules.first().lessons
        val lesson2 = lessons[1] // Has mandatory exam
        val lesson3 = lessons[2] // Locked until lesson2's mandatory exam is passed

        // Lesson 3 should initially be locked
        assertFalse(repository.isLessonUnlocked(lesson3, lessons))

        // Start and pass the exam
        val attempt = repository.startExam("exam-math-1").getOrThrow()
        val passedResult = repository.submitExam(
            attempt.id,
            mapOf("q-1" to "٣ جتا(٣س)", "q-2" to "صواب"),
            60
        ).getOrThrow()
        assertTrue(passedResult.passed)

        // With passing exam and completed lesson 2, lesson 3 unlocks!
        repository.markLessonComplete(lesson2.id)
        assertTrue(repository.isLessonUnlocked(lesson3, lessons))
    }

    @Test
    fun testWalletCoursePurchaseDeduction() = runBlocking {
        // Initial balance is 350 EGP
        assertEquals(350.0, repository.walletAccount.value.balance, 0.01)

        // Purchase Physics course (220 EGP)
        val purchaseRes = repository.purchaseCourse("course-physics")
        assertTrue(purchaseRes.isSuccess)

        // Balance should now be 130 EGP (350 - 220)
        assertEquals(130.0, repository.walletAccount.value.balance, 0.01)

        // Attempting to purchase Chemistry course (180 EGP) with 130 EGP must fail!
        val insufficientRes = repository.purchaseCourse("course-chem")
        assertFalse(insufficientRes.isSuccess)
        assertTrue(insufficientRes.exceptionOrNull()?.message?.contains("غير كافٍ") == true)
    }

    @Test
    fun testImmediateObjectiveGrading() {
        val attempt = repository.startExam("exam-math-1").getOrThrow()
        val answers = mapOf(
            "q-1" to "٣ جتا(٣س)", // Correct MCQ (10 marks)
            "q-2" to "صواب"        // Correct True/False (10 marks)
        )
        val result = repository.submitExam(attempt.id, answers, 60).getOrThrow()
        assertEquals(2, result.correctAnswersCount)
        assertTrue(result.score >= 20.0)
        assertTrue(result.percentage >= 60.0)
        assertTrue(result.passed)
    }
}
