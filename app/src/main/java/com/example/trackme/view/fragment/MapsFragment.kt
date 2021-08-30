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
import androidx.lifecycle.ViewModel
import com.example.trackme.R
import com.example.trackme.TrackMeApplication
import com.example.trackme.view.activity.RecordingActivity
import com.example.trackme.viewmodel.MapService
import com.example.trackme.viewmodel.MapViewModel
import com.example.trackme.viewmodel.line
import com.example.trackme.viewmodel.segment
import com.google.android.gms.maps.*
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.PolylineOptions
import pub.devrel.easypermissions.AppSettingsDialog
import pub.devrel.easypermissions.EasyPermissions
import javax.inject.Inject


class MapsFragment : Fragment() {

    var lines = mutableListOf<segment>()
    private var map: GoogleMap? = null
    private var isStart = false

    private val callback = OnMapReadyCallback { googleMap ->
        map = googleMap

        setStyleMap(map!!)
        drawAll()
    }

    private fun drawAll() {
        if(lines.isNotEmpty() && lines.first().isNotEmpty())
            map?.addMarker(MarkerOptions().position(lines.first().first()))
        for(line in lines){
            val polylineOptions = PolylineOptions().color(R.color.purple_200)
                    .addAll(line)
            map?.addPolyline(polylineOptions)
        }
    }

    private fun drawCurrentLine() {
        if(!lines.isEmpty() && lines.last().size > 1) {
            val lastLocation = lines.last().last()
            val prevLocation = lines.last()[lines.last().size-2]
            val polylineOptions = PolylineOptions().color(R.color.purple_200)
                    .add(prevLocation,lastLocation)
            map?.addPolyline(polylineOptions)
        }

    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_maps, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val mapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment?

        mapFragment?.getMapAsync(callback)
        Log.d("MAPSFRAGMENT", "is start $isStart")

        MapService.path.observe(viewLifecycleOwner,{
            lines = it
            drawCurrentLine()
            if(lines.isNotEmpty() && lines.last().isNotEmpty()){
                map?.animateCamera(CameraUpdateFactory
                        .newLatLngZoom(lines.last().last(),15f))

                if(!isStart && lines.first().isNotEmpty()){
                    map?.addMarker(MarkerOptions().position(lines.first().first()))
                    isStart = true
                }
            }
        })

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