package com.udacity.project4.locationreminders.data.local

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.data.dto.Result
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import org.hamcrest.CoreMatchers.`is`
import org.junit.*
import org.junit.runner.RunWith

@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
//Medium Test to test the repository
@MediumTest
class RemindersLocalRepositoryTest {

    private lateinit var reminderLocalRepo: RemindersLocalRepository
    private lateinit var database: RemindersDatabase

    // Executes each task synchronously using Architecture Components.
    @get:Rule
    var instantExecutorRule = InstantTaskExecutorRule()

    @Before
    fun setup() {
        // using an in-memory database for testing, since it doesn't survive killing the process
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(), RemindersDatabase::class.java
        ).allowMainThreadQueries().build()

        reminderLocalRepo = RemindersLocalRepository(
            database.reminderDao(), Dispatchers.Main
        )
    }

    @After
    fun cleanUp() {
        database.close()
    }

    @Test
    fun saveReminder_retrievesReminder() = runBlocking {
        // GIVEN - a new reminder saved in the database
        val reminderDTO = ReminderDTO(
            "Title1", "desc1", "location1", 0.0, 0.0
        )
        reminderLocalRepo.saveReminder(reminderDTO)

        // WHEN  - Reminder retrieved by ID
        val result = reminderLocalRepo.getReminder(reminderDTO.id)

        // THEN - Same reminder is returned
        result as Result.Success
        Assert.assertThat(result.data.title, `is`(reminderDTO.title))
        Assert.assertThat(result.data.description, `is`(reminderDTO.description))
        Assert.assertThat(result.data.location, `is`(reminderDTO.location))
    }

    @Test
    fun deleteReminder_returnEmptyList() = runBlocking {
        // GIVEN - a new reminder saved in the database
        val reminderDTO = ReminderDTO(
            "Title1", "desc1", "location1", 0.0, 0.0
        )
        reminderLocalRepo.saveReminder(reminderDTO)

        // WHEN  - Delete the reminder
        reminderLocalRepo.deleteAllReminders()

        // THEN - EmptyList is returned
        val reminder = reminderLocalRepo.getReminders()
        Assert.assertThat(reminder is Result.Success, `is`(true))
        reminder as Result.Success
        Assert.assertThat(reminder.data, `is`(emptyList()))
    }

    @Test
    fun deleteAllReminder_returnDataNotFound() = runBlocking {
        // GIVEN - a new reminder saved in the database
        val reminderDTO = ReminderDTO(
            "Title1", "desc1", "location1", 0.0, 0.0
        )
        reminderLocalRepo.saveReminder(reminderDTO)

        // WHEN  - Delete the reminder
        reminderLocalRepo.deleteAllReminders()

        // THEN - Result.Error is returned
        val reminderDTOResult = reminderLocalRepo.getReminder(reminderDTO.id)
        Assert.assertThat(reminderDTOResult is Result.Error, `is`(true))
        reminderDTOResult as Result.Error
        Assert.assertThat(reminderDTOResult.message, `is`("Reminder not found!"))
    }

}