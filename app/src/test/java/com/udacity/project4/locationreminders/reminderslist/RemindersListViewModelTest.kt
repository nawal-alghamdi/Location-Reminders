package com.udacity.project4.locationreminders.reminderslist

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.firebase.FirebaseApp
import com.udacity.project4.locationreminders.MainCoroutineRule
import com.udacity.project4.locationreminders.data.FakeDataSource
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.getOrAwaitValue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.hamcrest.CoreMatchers.*
import org.hamcrest.MatcherAssert
import org.junit.After
import org.junit.Assert.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.stopKoin

@RunWith(AndroidJUnit4::class)
@ExperimentalCoroutinesApi
class RemindersListViewModelTest {

    // Provide testing to the RemindersListViewModel and its live data objects
    private lateinit var reminderListViewModel: RemindersListViewModel
    private lateinit var remindersRepo: FakeDataSource

    @get:Rule
    var instantTaskExecutorRule = InstantTaskExecutorRule()

    @get:Rule
    var mainCoroutineRule = MainCoroutineRule()

    @Before
    fun setupViewModel() {
        FirebaseApp.initializeApp(InstrumentationRegistry.getInstrumentation().targetContext)
        remindersRepo = FakeDataSource()
        reminderListViewModel = RemindersListViewModel(
            ApplicationProvider.getApplicationContext(), remindersRepo
        )
    }

    @After
    fun tearDown() {
        stopKoin()
    }

    @Test
    fun loadReminders_remindersListNotNull() {
        // Given - Add reminder to the repository
        val reminderDTO = ReminderDTO(
            "Title1", "desc1", "location1", 0.0, 0.0
        )

        mainCoroutineRule.runBlockingTest {
            remindersRepo.saveReminder(reminderDTO)
        }

        // When - Loading reminders
        reminderListViewModel.loadReminders()

        // Then - reminder list is not null
        val value = reminderListViewModel.remindersList.getOrAwaitValue()
        assertThat(value, not(nullValue()))
    }


    @Test
    fun loadReminders_loading() {
        // Pause dispatcher so we can verify initial values
        mainCoroutineRule.pauseDispatcher()

        // Load the reminders in the viewModel
        reminderListViewModel.loadReminders()

        // Then progress indicator is shown
        MatcherAssert.assertThat(reminderListViewModel.showLoading.getOrAwaitValue(), `is`(true))

        // Execute pending coroutines actions
        mainCoroutineRule.resumeDispatcher()

        // Then progress indicator is hidden
        MatcherAssert.assertThat(reminderListViewModel.showLoading.getOrAwaitValue(), `is`(false))
    }

    @Test
    fun loadRemindersWhenErrorIsSet_callErrorToDisplay() {
        // Given - make the repo return error result
        remindersRepo.setReturnError(true)

        // When - Loading the reminders in the viewModel
        reminderListViewModel.loadReminders()

        // Then - The snackBar is updated with the error message.
        val snackBarText = reminderListViewModel.showSnackBar.getOrAwaitValue()
        assertThat(snackBarText, `is`("There was an error getting the reminders"))
    }

}