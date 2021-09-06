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
import androidx.fragment.app.FragmentContainer
import androidx.fragment.app.FragmentContainerView
import com.example.trackme.R
import com.example.trackme.TrackMeApplication
import com.example.trackme.repo.entity.SubPosition
import com.example.trackme.view.activity.RecordingActivity
import com.example.trackme.viewmodel.RecordingViewModel
import com.example.trackme.viewmodel.segment
import com.google.android.gms.maps.*
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.PolylineOptions
import pub.devrel.easypermissions.EasyPermissions
import javax.inject.Inject


class MapsFragment : Fragment() {

    var lines = listOf<SubPosition>()
    var map: GoogleMap? = null
    @Inject
    lateinit var recordViewmodel: RecordingViewModel
    private var isStart = false

    private val callback = OnMapReadyCallback { googleMap ->
        map = googleMap

        setStyleMap(map!!)
        drawAll()
    }

    private fun drawAll() {
        if(lines.isEmpty())
            return
        map?.addMarker(MarkerOptions().position(
                    LatLng(lines.first().lat.toDouble(),lines.first().lng.toDouble())))
        for(i in (0..lines.size-2)){
            if(lines[i].segmentId != lines[i+1].segmentId)
                continue
            val lastLocation = lines[i+1]
            val prevLocation = lines[i]

            val lastPos = LatLng(lastLocation.lat.toDouble(),lastLocation.lng.toDouble())
            val prevPos = LatLng(prevLocation.lat.toDouble(),prevLocation.lng.toDouble())

            val polylineOptions = PolylineOptions().color(R.color.purple_200)
                        .add(prevPos, lastPos)
            map?.addPolyline(polylineOptions)
        }
        if(lines.last().segmentId != lines[lines.size-2].segmentId) {
            val lastPos = LatLng(lines.last().lat.toDouble(), lines.last().lng.toDouble())
            val polylineOptions = PolylineOptions().color(R.color.purple_200)
                    .add(lastPos, lastPos)
            map?.addPolyline(polylineOptions)

        }
        Log.d("MapsFragment", "DrawAll size: ${lines.size}")
    }

    private fun drawCurrentLine() {
        if(lines.size > 1) {

            val lastLocation = lines.last()
            val prevLocation = lines[lines.size-2]
            Log.d("MAPSFRAGMENT", "${lastLocation.segmentId} && ${prevLocation.segmentId}")
            if(lastLocation.segmentId == prevLocation.segmentId) {
                val lastPos = LatLng(lastLocation.lat.toDouble(),lastLocation.lng.toDouble())
                val prevPos = LatLng(prevLocation.lat.toDouble(),prevLocation.lng.toDouble())

                val polylineOptions = PolylineOptions().color(R.color.purple_200)
                        .add(prevPos, lastPos)
                map?.addPolyline(polylineOptions)
            }
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
        drawAll()
    }

    override fun onStop() {
        super.onStop()
        map?.clear()
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val mapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment?
        inject()

        mapFragment?.getMapAsync(callback)

        recordViewmodel.route.observe(viewLifecycleOwner,{
            lines = it
            drawCurrentLine()
            if(lines.isNotEmpty()){
                val lastPos = LatLng(lines.last().lat.toDouble(),lines.last().lng.toDouble())
                map?.animateCamera(CameraUpdateFactory
                        .newLatLngZoom(lastPos,15f))

                if(!isStart && lines.isNotEmpty()){
                    val firstPos = LatLng(lines[0].lat.toDouble(),lines[0].lng.toDouble())
                    map?.addMarker(MarkerOptions().position(firstPos))
                    isStart = true
                }
            }
        })

        (view.parent as FragmentContainerView).tag = this
    }


    fun checkPermission(): Boolean{
        var res = false
        if(Build.VERSION.SDK_INT < Build.VERSION_CODES.Q){
            res = EasyPermissions.hasPermissions(
                requireContext(),
                android.Manifest.permission.ACCESS_FINE_LOCATION,
                android.Manifest.permission.ACCESS_COARSE_LOCATION
            )
//            Log.d("TAG", "result SDK < Q and $res")
        }
        else{
            res = EasyPermissions.hasPermissions(
                requireContext(),
                android.Manifest.permission.ACCESS_FINE_LOCATION,
                android.Manifest.permission.ACCESS_COARSE_LOCATION,
                android.Manifest.permission.ACCESS_BACKGROUND_LOCATION
            )
//            Log.d("TAG", "result SDK >= Q and $res")

        }
        return res
    }


    fun inject(){
        val appComponent = TrackMeApplication.instance.appComponent
        appComponent.mapComponent()
            .create(requireActivity() as RecordingActivity)
            .inject(this)
    }
    private fun setStyleMap(map: GoogleMap){
        try{
            val success = map.setMapStyle(
                MapStyleOptions.loadRawResourceStyle(
                    requireContext(),
                    R.raw.map
                )
            )
            if(!success){
                Log.d(TAG, "Parse Failed.")

            }
        }catch (e: Resources.NotFoundException){
            Log.d(TAG, "Cannot find Resources")
        }
    }




    companion object{
        private val TAG = "DEBUG_LOG"
        private val REQUEST_CODE = 1
    }
}