package com.example.trackme.view.fragment

import android.content.res.Resources
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
import com.example.trackme.repo.entity.SubPosition
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

    var lines = listOf<SubPosition>()
    var map: GoogleMap? = null
    var isStart = false
    var lastPosition: Position? = null

    @Inject
    lateinit var recordViewmodel: RecordingViewModel

    private val callback = OnMapReadyCallback { googleMap ->
        map = googleMap

        setStyleMap(map!!)
        //drawAll()
    }

    private fun drawAll() {
        if (lines.isEmpty())
            return
        map?.addMarker(
            MarkerOptions().position(
                LatLng(lines.first().lat.toDouble(), lines.first().lng.toDouble())
            )
        )
        for (i in (0..lines.size - 2)) {
            if (lines[i].segmentId != lines[i + 1].segmentId)
                continue
            val lastLocation = lines[i + 1]
            val prevLocation = lines[i]

            val lastPos = LatLng(lastLocation.lat.toDouble(), lastLocation.lng.toDouble())
            val prevPos = LatLng(prevLocation.lat.toDouble(), prevLocation.lng.toDouble())

            val polylineOptions = PolylineOptions().color(R.color.purple_200)
                .add(prevPos, lastPos)
            map?.addPolyline(polylineOptions)
        }
        if (lines.last().segmentId != lines[lines.size - 2].segmentId) {
            val lastPos = LatLng(lines.last().lat.toDouble(), lines.last().lng.toDouble())
            val polylineOptions = PolylineOptions().color(R.color.purple_200)
                .add(lastPos, lastPos)
            map?.addPolyline(polylineOptions)

        }
        Log.d("MapsFragment", "DrawAll size: ${lines.size}")
    }

    private fun drawCurrentLine(newPosition: Position) {
        if (lastPosition == null) return

        if (lastPosition!!.segment == newPosition.segment) {
            val lastLatLng = LatLng(lastPosition!!.lat, lastPosition!!.lon)
            val newLatLng = LatLng(newPosition.lat, newPosition.lon)

            val polylineOptions = PolylineOptions().color(R.color.purple_200)
                .add(lastLatLng, newLatLng)
            map?.addPolyline(polylineOptions)
        }

        Log.d("MAPSFRAGMENT", "${lastPosition!!.segment} && ${newPosition.segment}")
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

        if(recordViewmodel.missingSegment.isNotEmpty()){
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
        //drawAll()
    }


    override fun onStop() {
        super.onStop()
        recordViewmodel.isInBackground = true
        val fPos = lastPosition
        if(fPos != null){
            fPos.segment++
            recordViewmodel.getPolyValue(fPos.segment).add(
                LatLng(fPos.lat, fPos.lon)
            )
        }

        //map?.clear()
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
        private val REQUEST_CODE = 1
    }
}