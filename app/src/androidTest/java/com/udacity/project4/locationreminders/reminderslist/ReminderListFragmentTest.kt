package com.udacity.project4.locationreminders.reminderslist

import android.app.Application
import android.os.Bundle
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.navigation.NavController
import androidx.navigation.Navigation
import androidx.recyclerview.widget.RecyclerView
import androidx.test.core.app.ApplicationProvider.getApplicationContext
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.RecyclerViewActions
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.udacity.project4.R
import com.udacity.project4.locationreminders.data.ReminderDataSource
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.data.local.LocalDB
import com.udacity.project4.locationreminders.data.local.RemindersLocalRepository
import com.udacity.project4.locationreminders.savereminder.SaveReminderViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.koin.test.KoinTest
import org.koin.test.get
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify

@RunWith(AndroidJUnit4::class)
@ExperimentalCoroutinesApi
//UI Testing
@MediumTest
class ReminderListFragmentTest : KoinTest {

    @get:Rule
    val instantExecutor = InstantTaskExecutorRule()

    private lateinit var repository: ReminderDataSource
    private lateinit var appContext: Application

    @Before
    fun init() {
        stopKoin()//stop the original app koin
        appContext = getApplicationContext()
        val myModule = module {
            viewModel {
                RemindersListViewModel(
                    appContext, get() as ReminderDataSource
                )
            }
            single {
                SaveReminderViewModel(
                    appContext, get() as ReminderDataSource
                )
            }
            single { RemindersLocalRepository(get()) as ReminderDataSource }
            single { LocalDB.createRemindersDao(appContext) }
        }
        //declare a new koin module
        startKoin {
            modules(listOf(myModule))
        }
        //Get our real repository
        repository = get()

        //clear the data to start fresh
        runBlocking {
            repository.deleteAllReminders()
        }
    }


    // Test the displayed data on the UI.
    @Test
    fun reminderData_DisplayedInUi() {
        // Given - Add a reminder to the database
        val reminderDTO = ReminderDTO(
            "Title1", "desc1", "location1", 0.0, 0.0
        )

        runBlocking {
            repository.saveReminder(reminderDTO)
        }

        // When - ReminderListFragment launched
        launchFragmentInContainer<ReminderListFragment>(Bundle(), themeResId = R.style.AppTheme)

        // Then - Reminder data is displayed
        onView(withId(R.id.reminderssRecyclerView)).perform(
                RecyclerViewActions.scrollTo<RecyclerView.ViewHolder>(
                    hasDescendant(withText(reminderDTO.title))
                )
            )
        onView(withText(reminderDTO.description)).check(matches(isDisplayed()))
    }

    // Test the navigation of the fragments.
    @Test
    fun clickAddReminderButton_navigateToSaveReminderFragment() {
        // Given - on the home screen
        val scenario =
            launchFragmentInContainer<ReminderListFragment>(themeResId = R.style.AppTheme)
        val navController = mock(NavController::class.java)
        scenario.onFragment {
            Navigation.setViewNavController(it.view!!, navController)
        }

        // When - Add reminder button clicked
        onView(withId(R.id.addReminderFAB)).perform(click())

        // Then - Navigate to SaveReminderFragment
        verify(navController).navigate(
            ReminderListFragmentDirections.toSaveReminder()
        )
    }

    @Test
    fun noData_displayEmptyText() {
        // When - Launch the fragment
        launchFragmentInContainer<ReminderListFragment>(themeResId = R.style.AppTheme)

        // Then - Empty message textView is displayed
        onView(withId(R.id.noDataTextView)).check(matches(isDisplayed()))
    }

}