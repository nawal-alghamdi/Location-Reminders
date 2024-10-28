package com.udacity.project4.locationreminders.savereminder.selectreminderlocation


import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Resources
import android.location.Location
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.*
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.navigation.fragment.findNavController
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.PointOfInterest
import com.udacity.project4.R
import com.udacity.project4.base.BaseFragment
import com.udacity.project4.databinding.FragmentSelectLocationBinding
import com.udacity.project4.locationreminders.savereminder.SaveReminderViewModel
import com.udacity.project4.utils.setDisplayHomeAsUpEnabled
import org.koin.android.ext.android.inject

class SelectLocationFragment : BaseFragment(), OnMapReadyCallback {

    //Use Koin to get the view model of the SaveReminder
    override val _viewModel: SaveReminderViewModel by inject()
    private lateinit var binding: FragmentSelectLocationBinding
    private lateinit var map: GoogleMap
    private var poi: PointOfInterest? = null
    private val fusedLocationClient by lazy {
        LocationServices.getFusedLocationProviderClient(requireActivity())
    }
    private val requestPermissionMessage = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        R.string.allow_background_permission_from_app_settings
    } else R.string.allow_location_permission_from_app_settings

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding =
            DataBindingUtil.inflate(inflater, R.layout.fragment_select_location, container, false)
        binding.viewModel = _viewModel
        binding.lifecycleOwner = this
        setHasOptionsMenu(true)
        setDisplayHomeAsUpEnabled(true)

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        // call this function after the user confirms on the selected location
        binding.saveButton.setOnClickListener {
            onLocationSelected()
        }

        return binding.root
    }

    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap
        // zoom to the user location after taking his permission
        checkLocationPermissions()

        // add style to the map
        try {
            val success = googleMap.setMapStyle(
                MapStyleOptions.loadRawResourceStyle(
                    requireActivity(), R.raw.style_json
                )
            )
            if (!success) {
                Log.e(TAG, "Style parsing failed.")
            }
        } catch (e: Resources.NotFoundException) {
            Log.e(TAG, "Can't find style. Error: ", e)
        }

        // put a marker to location that the user selected
        setPoiClick(map)
    }

    private fun onLocationSelected() {
        //         When the user confirms on the selected location,
        //         send back the selected location details to the view model
        //         and navigate back to the previous fragment to save the reminder and add the geofence
        if (isBackgroundPermissionGranted()) {
            if (poi != null) {
                _viewModel.selectedPOI.value = poi
                _viewModel.latitude.value = poi?.latLng?.latitude
                _viewModel.longitude.value = poi?.latLng?.longitude
                _viewModel.reminderSelectedLocationStr.value = poi?.name
                Log.d(TAG, "POI latLng: ${poi!!.latLng}, POI name: ${poi!!.name}")
                findNavController().popBackStack()
            } else {
                Toast.makeText(activity, getString(R.string.err_select_poi), Toast.LENGTH_SHORT)
                    .show()
            }
        } else {
            requestBackgroundLocationPermission()
        }
    }

    private fun setPoiClick(map: GoogleMap) {
        map.setOnPoiClickListener { poi ->
            map.clear()
            this.poi = poi
            val poiMarker = map.addMarker(
                MarkerOptions().position(poi.latLng).title(poi.name).snippet(
                    getString(
                        R.string.lat_long_snippet, poi.latLng.latitude, poi.latLng.longitude
                    )
                )
            )
            poiMarker?.showInfoWindow()
        }
    }

    @SuppressLint("MissingPermission")
    private fun enableMyLocation() {
        val zoomLevel = 15f
        map.isMyLocationEnabled = true
        fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
            val userLocation = location?.let { LatLng(it.latitude, location.longitude) }
            userLocation?.let { CameraUpdateFactory.newLatLngZoom(it, zoomLevel) }
                ?.let { map.moveCamera(it) }
        }
    }

    private fun checkLocationPermissions() {
        if (ContextCompat.checkSelfPermission(
                requireActivity(), Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            enableMyLocation()
        } else {
            showLocationPermissionDialog(title = R.string.grant_location_permission_title,
                message = R.string.please_grant_location_permission,
                positiveButtonLabel = R.string.grant_permission_label,
                positiveButtonAction = { requestLocationPermission() })
        }
    }

    private fun requestLocationPermission() {
        requestPermissions(
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), REQUEST_LOCATION_PERMISSION
        )
    }

    private fun isBackgroundPermissionGranted(): Boolean {
        // On Android API level less than 29, when your app receives foreground location access,
        // it automatically receives background location access as well.
        return when {
            Build.VERSION.SDK_INT < Build.VERSION_CODES.Q && ContextCompat.checkSelfPermission(
                requireActivity(), Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED -> true

            Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && ContextCompat.checkSelfPermission(
                requireActivity(), Manifest.permission.ACCESS_BACKGROUND_LOCATION
            ) == PackageManager.PERMISSION_GRANTED -> true

            else -> false
        }
    }

    @SuppressLint("InlinedApi")
    private fun requestBackgroundLocationPermission() {
        showLocationPermissionDialog(
            title = R.string.location_permission,
            message = requestPermissionMessage,
            positiveButtonLabel = R.string.redirect_to_app_settings_label
        ) {
            startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", activity?.packageName, null)
            })
        }
    }

    private fun showLocationPermissionDialog(
        title: Int? = null, message: Int, positiveButtonLabel: Int, positiveButtonAction: () -> Unit
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
                    activity, getString(R.string.permission_denied_explanation), Toast.LENGTH_LONG
                ).show()
            }
            show()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        when (requestCode) {
            REQUEST_LOCATION_PERMISSION -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    enableMyLocation()
                } else {
                    handlePermissionDenied()
                }
            }

            REQUEST_BACKGROUND_LOCATION_PERMISSION -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    enableMyLocation()
                } else {
                    handlePermissionDenied()
                }
            }

            else -> super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        }
    }

    private fun handlePermissionDenied() {
        Toast.makeText(
            context, "${getString(requestPermissionMessage)} to save location", Toast.LENGTH_LONG
        ).show()
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.map_options, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
        // Change the map type based on the user's selection.
        R.id.normal_map -> {
            map.mapType = GoogleMap.MAP_TYPE_NORMAL
            true
        }

        R.id.hybrid_map -> {
            map.mapType = GoogleMap.MAP_TYPE_HYBRID
            true
        }

        R.id.satellite_map -> {
            map.mapType = GoogleMap.MAP_TYPE_SATELLITE
            true
        }

        R.id.terrain_map -> {
            map.mapType = GoogleMap.MAP_TYPE_TERRAIN
            true
        }

        else -> super.onOptionsItemSelected(item)
    }

    companion object {
        private const val REQUEST_LOCATION_PERMISSION = 12
        private const val REQUEST_BACKGROUND_LOCATION_PERMISSION = 13
        private const val TAG = "SelectLocationFragment"
    }


}
