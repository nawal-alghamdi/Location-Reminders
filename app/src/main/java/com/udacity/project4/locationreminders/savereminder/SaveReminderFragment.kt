package com.udacity.project4.locationreminders.savereminder

import android.Manifest
import android.annotation.SuppressLint
import android.annotation.TargetApi
import android.app.PendingIntent
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingClient
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationSettingsRequest
import com.udacity.project4.BuildConfig
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

    private val runningQOrLater = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q

    // A PendingIntent for the Broadcast Receiver that handles geofence transitions.
    private val geofencePendingIntent: PendingIntent by lazy {
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        val intent = Intent(requireContext(), GeofenceBroadcastReceiver::class.java)
        intent.action = ACTION_GEOFENCE_EVENT
        PendingIntent.getBroadcast(this.requireContext(), 0, intent, flags)
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
            // use the user entered reminder details to:
            // 1) add a geofencing request
            // 2) save the reminder to the local db
            // location is enabled
            if (!title.isNullOrEmpty() && !location.isNullOrEmpty()) {
                if (foregroundAndBackgroundLocationPermissionApproved()) {
                    checkDeviceLocationSettingsAndStartGeofence()
                } else {
                    requestForegroundAndBackgroundLocationPermissions()
                }
            }
        }
    }

    /*
 *  Uses the Location Client to check the current state of location settings, and gives the user
 *  the opportunity to turn on location services within our app.
 */
    private fun checkDeviceLocationSettingsAndStartGeofence(resolve: Boolean = true) {
        val locationRequest = LocationRequest.create().apply {
            priority = LocationRequest.PRIORITY_LOW_POWER
        }
        val builder = LocationSettingsRequest.Builder().addLocationRequest(locationRequest)
        val settingsClient = LocationServices.getSettingsClient(requireActivity())
        val locationSettingsResponseTask = settingsClient.checkLocationSettings(builder.build())
        locationSettingsResponseTask.addOnFailureListener { exception ->
            if (exception is ResolvableApiException && resolve) {
                try {
                    startIntentSenderForResult(
                        exception.resolution.intentSender,
                        REQUEST_TURN_DEVICE_LOCATION_ON,
                        null,
                        0,
                        0,
                        0,
                        null
                    )
                } catch (sendEx: IntentSender.SendIntentException) {
                    Log.d(TAG, "Error getting location settings resolution: " + sendEx.message)
                }
            } else {
                showDialog(
                    message = R.string.for_a_better_experience_turn_on_device_location,
                    positiveButtonLabel = android.R.string.ok,
                    positiveButtonAction = { checkDeviceLocationSettingsAndStartGeofence() },
                    negativeButtonToastMessage = R.string.device_location_must_be_enabled_to_add_a_geofence
                )
            }
        }
        locationSettingsResponseTask.addOnCompleteListener {
            if (it.isSuccessful) {
                addGeofenceForTheSelectedLocation()
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun addGeofenceForTheSelectedLocation() {
        // add in code to add the geofence
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

    @TargetApi(29)
    private fun foregroundAndBackgroundLocationPermissionApproved(): Boolean {

        val foregroundLocationApproved =
            (PackageManager.PERMISSION_GRANTED == ContextCompat.checkSelfPermission(
                requireActivity(), Manifest.permission.ACCESS_FINE_LOCATION
            ))

        val backgroundPermissionApproved = if (runningQOrLater) {
            PackageManager.PERMISSION_GRANTED == ContextCompat.checkSelfPermission(
                requireActivity(), Manifest.permission.ACCESS_BACKGROUND_LOCATION
            )
        } else {
            true
        }
        return foregroundLocationApproved && backgroundPermissionApproved
    }

    @TargetApi(29)
    private fun requestForegroundAndBackgroundLocationPermissions() {
        if (foregroundAndBackgroundLocationPermissionApproved()) return
        var permissionsArray = arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
        val resultCode = when {
            runningQOrLater -> {
                permissionsArray += Manifest.permission.ACCESS_BACKGROUND_LOCATION
                REQUEST_FOREGROUND_AND_BACKGROUND_PERMISSION_RESULT_CODE
            }

            else -> {
                Log.d(TAG, "Request foreground only location permission")
                REQUEST_FOREGROUND_ONLY_PERMISSIONS_REQUEST_CODE
            }
        }
        requestPermissions(
            permissionsArray, resultCode
        )
    }

    /*
 * In all cases, we need to have the location permission.  On Android 10+ (Q) we need to have
 * the background permission as well.
 */
    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>, grantResults: IntArray
    ) {
        Log.d(TAG, "onRequestPermissionResult")

        if (grantResults.isEmpty() || grantResults[LOCATION_PERMISSION_INDEX] == PackageManager.PERMISSION_DENIED ||
            //If the request code equals REQUEST_FOREGROUND_AND_BACKGROUND_PERMISSION_RESULT_CODE and
            // the BACKGROUND_LOCATION_PERMISSION_INDEX is denied it means that the device is running
            // API 29 or above and that background permissions were denied.
            (requestCode == REQUEST_FOREGROUND_AND_BACKGROUND_PERMISSION_RESULT_CODE && grantResults[BACKGROUND_LOCATION_PERMISSION_INDEX] == PackageManager.PERMISSION_DENIED)
        ) {
            //This app has very little use when permissions are not granted so present a snackbar
            // explaining that the user needs location permissions in order to play.
            showDialog(
                title = R.string.location_permission,
                message = R.string.allow_location_permission_from_app_settings,
                positiveButtonLabel = R.string.redirect_to_app_settings_label,
                positiveButtonAction = {
                    startActivity(Intent().apply {
                        action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                        data = Uri.fromParts("package", BuildConfig.APPLICATION_ID, null)
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    })
                },
                negativeButtonToastMessage = R.string.permission_denied_explanation
            )
        } else {
            // If not, permissions have been granted!
            checkDeviceLocationSettingsAndStartGeofence()
        }
    }

    /*
*  When we get the result from asking the user to turn on device location, we call
*  checkDeviceLocationSettingsAndStartGeofence again to make sure it's actually on, but
*  we don't resolve the check to keep the user from seeing an endless loop.
*/
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_TURN_DEVICE_LOCATION_ON) {
            checkDeviceLocationSettingsAndStartGeofence(false)
        }
    }

    private fun showDialog(
        title: Int? = null,
        message: Int,
        positiveButtonLabel: Int,
        positiveButtonAction: () -> Unit,
        negativeButtonToastMessage: Int
    ) {
        val builder = AlertDialog.Builder(requireActivity())
        with(builder) {
            if (title != null) setTitle(title)
            setMessage(message)
            setPositiveButton(positiveButtonLabel) { _dialog, _which ->
                positiveButtonAction()
            }
            setNegativeButton(R.string.cancel_label) { _, _ ->
                Toast.makeText(
                    activity, getString(negativeButtonToastMessage), Toast.LENGTH_LONG
                ).show()
            }
            show()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        //make sure to clear the view model after destroy, as it's a single view model.
        _viewModel.onClear()
    }

    companion object {
        private const val REQUEST_FOREGROUND_AND_BACKGROUND_PERMISSION_RESULT_CODE = 33
        private const val REQUEST_FOREGROUND_ONLY_PERMISSIONS_REQUEST_CODE = 34
        private const val REQUEST_TURN_DEVICE_LOCATION_ON = 29
        private const val LOCATION_PERMISSION_INDEX = 0
        private const val BACKGROUND_LOCATION_PERMISSION_INDEX = 1
        private const val TAG = "SaveReminderFragment"
        internal const val ACTION_GEOFENCE_EVENT =
            "SaveReminderFragment.GeofenceReminder.action.ACTION_GEOFENCE_EVENT"
    }
}
