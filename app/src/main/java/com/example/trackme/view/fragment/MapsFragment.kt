package com.example.trackme.view.fragment

import android.content.res.Resources
import android.graphics.Point
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentContainerView
import com.example.trackme.R
import com.example.trackme.TrackMeApplication
import com.example.trackme.repo.entity.Position
import com.example.trackme.view.activity.RecordingActivity
import com.example.trackme.viewmodel.RecordingViewModel
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.PolylineOptions
import javax.inject.Inject


class MapsFragment : Fragment() {

    var map: GoogleMap? = null
    private var isStart = false
    private var lastPosition: Position? = null

    @Inject
    lateinit var recordViewmodel: RecordingViewModel

    private val callback = OnMapReadyCallback { googleMap ->
        map = googleMap

        setStyleMap(map!!)
    }


    private fun drawCurrentLine(newPosition: Position) {
        if (lastPosition == null) return

        if (lastPosition!!.segment == newPosition.segment) {
            val prevPos = LatLng(lastPosition!!.lat, lastPosition!!.lon)
            val lastPos = LatLng(newPosition.lat, newPosition.lon)

                val polylineOptions = PolylineOptions().color(R.color.purple_200)
                        .add(prevPos, lastPos)
                map?.addPolyline(polylineOptions)
            }
        }


    private fun drawMissingPath() {
        recordViewmodel.missingSegment.forEach {
            map?.addPolyline(recordViewmodel.getPolyValue(it))
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_maps, container, false)
    }

    override fun onStart() {
        super.onStart()

        if (recordViewmodel.missingSegment.isNotEmpty()) {
            drawMissingPath()

            val lastLatLng = recordViewmodel.missingRoute.values.last().points.last()
            val lastSegment = recordViewmodel.missingSegment.last()
            lastPosition = Position(
                0, lastLatLng.latitude, lastLatLng.longitude, lastSegment, 0
            )

            recordViewmodel.missingSegment.clear()
            recordViewmodel.missingRoute.clear()
        }
        recordViewmodel.isInBackground = false
    }


    override fun onStop() {
        super.onStop()
        recordViewmodel.isInBackground = true
        recordViewmodel.requestINcreaseSegment()

        val fPos = lastPosition
        if (fPos != null) {
            fPos.segment++
            recordViewmodel.getPolyValue(fPos.segment).add(
                LatLng(fPos.lat, fPos.lon)
            )
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val mapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment?
        inject()

        mapFragment?.getMapAsync(callback)

        recordViewmodel.lastPosition.observe(viewLifecycleOwner) {
            drawCurrentLine(it)
            if (it != null) {
                val latLng = LatLng(it.lat, it.lon)
                map?.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15f))

                if (!isStart) {
                    map?.addMarker(MarkerOptions().position(latLng))
                    isStart = true
                }

            }
            lastPosition = it
        }

        (view.parent as FragmentContainerView).tag = this
    }


    fun inject() {
        val appComponent = TrackMeApplication.instance.appComponent
        appComponent.mapComponent()
            .create(requireActivity() as RecordingActivity)
            .inject(this)
    }

    private fun setStyleMap(map: GoogleMap) {
        try {
            val success = map.setMapStyle(
                MapStyleOptions.loadRawResourceStyle(
                    requireContext(),
                    R.raw.map
                )
            )
            if (!success) {
                Log.d(TAG, "Parse Failed.")

            }
        } catch (e: Resources.NotFoundException) {
            Log.d(TAG, "Cannot find Resources")
        }
    }


    companion object {
        private val TAG = "DEBUG_LOG"
    }
}