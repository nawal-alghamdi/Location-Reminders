package com.udacity.project4.locationreminders.savereminder

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingClient
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationServices
import com.udacity.project4.R
import com.udacity.project4.base.BaseFragment
import com.udacity.project4.base.NavigationCommand
import com.udacity.project4.databinding.FragmentSaveReminderBinding
import com.udacity.project4.locationreminders.geofence.GeofenceBroadcastReceiver
import com.udacity.project4.locationreminders.geofence.GeofencingConstants
import com.udacity.project4.locationreminders.reminderslist.ReminderDataItem
import com.udacity.project4.utils.setDisplayHomeAsUpEnabled
import org.koin.android.ext.android.inject

class SaveReminderFragment : BaseFragment() {
    //Get the view model this time as a single to be shared with the another fragment
    override val _viewModel: SaveReminderViewModel by inject()
    private lateinit var binding: FragmentSaveReminderBinding
    private lateinit var reminderDataItem: ReminderDataItem
    private lateinit var geofencingClient: GeofencingClient

    // A PendingIntent for the Broadcast Receiver that handles geofence transitions.
    // TODO: Step 8 add in a pending intent
    private val geofencePendingIntent: PendingIntent by lazy {
        val intent = Intent(requireContext(), GeofenceBroadcastReceiver::class.java)
        intent.action = ACTION_GEOFENCE_EVENT
        PendingIntent.getBroadcast(
            this.requireContext(), 0, intent, PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding =
            DataBindingUtil.inflate(inflater, R.layout.fragment_save_reminder, container, false)
        setDisplayHomeAsUpEnabled(true)
        binding.viewModel = _viewModel
        geofencingClient = LocationServices.getGeofencingClient(requireActivity())
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.lifecycleOwner = this
        binding.selectLocation.setOnClickListener {
            // Navigate to another fragment to get the user location
            _viewModel.navigationCommand.value =
                NavigationCommand.To(SaveReminderFragmentDirections.actionSaveReminderFragmentToSelectLocationFragment())
        }

        binding.saveReminder.setOnClickListener {
            val title = _viewModel.reminderTitle.value
            val description = _viewModel.reminderDescription.value
            val location = _viewModel.reminderSelectedLocationStr.value
            val latitude = _viewModel.latitude.value
            val longitude = _viewModel.longitude.value

            reminderDataItem = ReminderDataItem(
                title, description, location, latitude, longitude
            )
            _viewModel.validateEnteredData(reminderDataItem)
            //TODO: use the user entered reminder details to:
            // 1) add a geofencing request
            // 2) save the reminder to the local db
            addGeofenceForTheSelectedLocation()
        }
    }

    @SuppressLint("MissingPermission")
    private fun addGeofenceForTheSelectedLocation() {
        // TODO: Step 10 add in code to add the geofence
        val geofence = reminderDataItem.longitude?.let { longitude ->
            reminderDataItem.latitude?.let { latitude ->
                Geofence.Builder().setRequestId(reminderDataItem.id).setCircularRegion(
                        latitude, longitude, GeofencingConstants.GEOFENCE_RADIUS_IN_METERS
                    ).setExpirationDuration(Geofence.NEVER_EXPIRE)
                    .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER).build()
            }
        }
        val geofencingRequest = geofence?.let {
            GeofencingRequest.Builder().setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
                .addGeofence(it).build()
        }
        if (geofencingRequest != null) {
            geofencingClient.addGeofences(geofencingRequest, geofencePendingIntent).run {
                addOnSuccessListener {
                    // What to do if the geofence is added
                    _viewModel.validateAndSaveReminder(reminderDataItem)
                    Log.i(TAG, reminderDataItem.title + " Geofences Added")
                }
                addOnFailureListener {
                    Toast.makeText(
                        requireContext(),
                        activity?.getString(R.string.geofences_not_added),
                        Toast.LENGTH_SHORT
                    ).show()
                    val a = it.message
                    if ((a != null)) {
                        Log.w(TAG, a + "on fail adding geofence")
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        //make sure to clear the view model after destroy, as it's a single view model.
        _viewModel.onClear()
    }

    companion object {
        private const val TAG = "SaveReminderFragment"
        internal const val ACTION_GEOFENCE_EVENT =
            "SaveReminderFragment.GeofenceReminder.action.ACTION_GEOFENCE_EVENT"
    }
}
