package com.udacity.project4.locationreminders.data.local

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.CoreMatchers.notNullValue
import org.hamcrest.MatcherAssert.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
//Unit test the DAO
@SmallTest
class RemindersDaoTest {

    private lateinit var database: RemindersDatabase

    // Executes each task synchronously using Architecture Components.
    @get:Rule
    var instantExecutorRule = InstantTaskExecutorRule()

    @Before
    fun initDb() {
        // using an in-memory database because the information stored here disappears when the
        // process is killed
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(), RemindersDatabase::class.java
        ).build()
    }

    @After
    fun closeDb() = database.close()

    @Test
    fun insertReminderAndGetById() = runBlockingTest {
        // GIVEN - insert a Reminder
        val reminderDTO = ReminderDTO(
            "Title1", "desc1", "location1", 0.0, 0.0
        )
        database.reminderDao().saveReminder(reminderDTO)

        // WHEN - Get the reminder by id from the database
        val loaded = database.reminderDao().getReminderById(reminderDTO.id)

        // THEN - The loaded data contains the expected values
        assertThat<ReminderDTO>(loaded as ReminderDTO, notNullValue())
        assertThat(loaded.id, `is`(reminderDTO.id))
        assertThat(loaded.title, `is`(reminderDTO.title))
        assertThat(loaded.description, `is`(reminderDTO.description))
        assertThat(loaded.location, `is`(reminderDTO.location))
    }

    @Test
    fun deleteReminderReturnEmptyList() = runBlockingTest {
        // GIVEN - insert a Reminder
        val reminderDTO = ReminderDTO(
            "Title1", "desc1", "location1", 0.0, 0.0
        )
        database.reminderDao().saveReminder(reminderDTO)

        // WHEN - Deleting the reminders from the database
        database.reminderDao().deleteAllReminders()

        // THEN - The loaded data is empty list
        val result = database.reminderDao().getReminders()
        assertThat(result, `is`(emptyList()))
    }

}