package com.udacity.project4.locationreminders.savereminder

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.udacity.project4.R
import com.udacity.project4.locationreminders.MainCoroutineRule
import com.udacity.project4.locationreminders.data.FakeDataSource
import com.udacity.project4.locationreminders.getOrAwaitValue
import com.udacity.project4.locationreminders.reminderslist.ReminderDataItem
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.CoreMatchers.notNullValue
import org.hamcrest.MatcherAssert.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.stopKoin

@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
class SaveReminderViewModelTest {

    // Provide testing to the SaveReminderViewModel and its live data objects
    private lateinit var repository: FakeDataSource

    //Subject under test
    private lateinit var saveReminderViewModel: SaveReminderViewModel

    @get:Rule
    var instantTaskExecutorRule = InstantTaskExecutorRule()

    @get:Rule
    var mainCoroutineRule = MainCoroutineRule()


    @Before
    fun setupSaveReminderViewModel() {
        repository = FakeDataSource()
        // Given a viewModel
        saveReminderViewModel = SaveReminderViewModel(
            ApplicationProvider.getApplicationContext(), repository
        )
    }

    @After
    fun tearDown() {
        stopKoin()
    }

    @Test
    fun saveReminder_returnSavedReminder_andShowToast() = runTest {
        // Given a reminderDTO
        val reminderDTO = ReminderDataItem(
            "Title1", "desc1", "location1", 0.0, 0.0
        )

        // When - Saving the reminder
        saveReminderViewModel.saveReminder(reminderDTO)

        // Then - Reminder is saved to the data source and a toast is shown
        assertThat(repository.getReminder(reminderDTO.id), `is`(notNullValue()))
        val toastMessage = saveReminderViewModel.showToast.getOrAwaitValue()
        assertThat(toastMessage, `is`("Reminder Saved !"))
    }

    @Test
    fun validateEnteredData_showErrorSnackBar() {
        // Given - ReminderDataItem with title empty
        val reminderDTO = ReminderDataItem(
            "", "desc1", "location1", 0.0, 0.0
        )

        // When - Validating the entered reminder
        saveReminderViewModel.validateAndSaveReminder(reminderDTO)

        // Then - SnackBar with the error message is shown
        val snackBarText = saveReminderViewModel.showSnackBarInt.getOrAwaitValue()
        assertThat(snackBarText, `is`(R.string.err_enter_title))
    }

}