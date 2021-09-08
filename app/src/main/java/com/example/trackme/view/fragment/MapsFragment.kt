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
import com.example.trackme.repo.entity.SubPosition
import com.example.trackme.view.activity.RecordingActivity
import com.example.trackme.viewmodel.RecordingViewModel
import com.google.android.gms.maps.*
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.PolylineOptions
import javax.inject.Inject


class MapsFragment : Fragment() {

    var lines = listOf<SubPosition>()
    var map: GoogleMap? = null
    var isStart = false
    @Inject
    lateinit var recordViewmodel: RecordingViewModel

    private val callback = OnMapReadyCallback { googleMap ->
        map = googleMap

        setStyleMap(map!!)
//        drawAll()
    }

    private fun drawAll() {

        if(recordViewmodel.livePolyline.value.isNullOrEmpty())
            return
        map?.addMarker(MarkerOptions().position(
                    LatLng(lines.first().lat.toDouble(),lines.first().lng.toDouble())))
        for(poly in recordViewmodel.livePolyline.value!!){
            map?.addPolyline(poly)
        }
        Log.d("MapsFragment", "DrawAll size: ${lines.size}")
    }

    private fun drawCurrentLine() {
        if(lines.size > 1) {

            val lastLocation = lines.last()
            val prevLocation = lines[lines.size-2]
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
//        drawAll()
    }

    override fun onStop() {
        super.onStop()
//        map?.clear()
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
//        recordViewmodel.livePolyline.observe(viewLifecycleOwner,{
//            map?.addPolyline(it.last())
//        })
        (view.parent as FragmentContainerView).tag = this
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