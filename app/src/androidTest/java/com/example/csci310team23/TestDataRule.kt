package com.example.csci310team23

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.csci310team23.data.local.AppDatabase
import com.example.csci310team23.data.local.PreferencesManager
import com.example.csci310team23.data.repository.AppRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement
import java.time.LocalDate

class TestDataRule(
    private val email: String,
    private val password: String
) : TestRule {

    lateinit var repository: AppRepository
        private set

    lateinit var context: Context
        private set

    lateinit var preferences: PreferencesManager
        private set

    override fun apply(base: Statement, description: Description?): Statement {
        return object : Statement() {
            override fun evaluate() {
                context = ApplicationProvider.getApplicationContext()
                AppGraph.provide(context)
                preferences = PreferencesManager(context)
                repository = AppGraph.repository

                runBlocking {
                    withContext(Dispatchers.IO) {
                        AppDatabase.get(context).clearAllTables()
                    }
                    preferences.hasSeenLanding = true

                    val user = repository.registerUser(
                        name = "Test Author",
                        email = email,
                        studentId = "1234567890",
                        password = password
                    )
                    repository.completeProfile(
                        userId = user.id,
                        department = "CS",
                        school = "Viterbi",
                        birthDate = LocalDate.now().minusYears(21),
                        bio = "Instrumentation tester"
                    )
                }

                try {
                    base.evaluate()
                } finally {
                    runBlocking {
                        withContext(Dispatchers.IO) {
                            AppDatabase.get(context).clearAllTables()
                        }
                    }
                }
            }
        }
    }
}
