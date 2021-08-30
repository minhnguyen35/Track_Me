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
import com.example.trackme.R
import com.example.trackme.TrackMeApplication
import com.example.trackme.view.activity.RecordingActivity
import com.example.trackme.viewmodel.MapService
import com.example.trackme.viewmodel.segment
import com.google.android.gms.maps.*
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.PolylineOptions
import pub.devrel.easypermissions.EasyPermissions


class MapsFragment : Fragment() {

    var lines = mutableListOf<segment>()
    private var map: GoogleMap? = null
    private var isDeny = true
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

        MapService.path.observe(viewLifecycleOwner,{
            lines = it
            drawCurrentLine()
            if(lines.isNotEmpty() && lines.last().isNotEmpty()){
                map?.animateCamera(CameraUpdateFactory
                        .newLatLngZoom(lines.last().last(),15f))

                if(!isStart){
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
            Log.d("TAG", "result SDK < Q and $res")
        }
        else{
            res = EasyPermissions.hasPermissions(
                requireContext(),
                android.Manifest.permission.ACCESS_FINE_LOCATION,
                android.Manifest.permission.ACCESS_COARSE_LOCATION,
                android.Manifest.permission.ACCESS_BACKGROUND_LOCATION
            )
            Log.d("TAG", "result SDK >= Q and $res")

        }
        return res
    }

    fun enableLocation(){
        if(checkPermission()){
            map?.isMyLocationEnabled = true
            isDeny = false
            return
        }
        if(Build.VERSION.SDK_INT < Build.VERSION_CODES.Q){
            EasyPermissions.requestPermissions(
                this,
                "This Feature Need To Access Location For Tracking Route",
                REQUEST_CODE,
                android.Manifest.permission.ACCESS_COARSE_LOCATION,
                android.Manifest.permission.ACCESS_FINE_LOCATION
            )
        }
        else{
            EasyPermissions.requestPermissions(
                this,
                "This Feature Need To Access Location For Tracking Route",
                REQUEST_CODE,
                android.Manifest.permission.ACCESS_COARSE_LOCATION,
                android.Manifest.permission.ACCESS_FINE_LOCATION,
                android.Manifest.permission.ACCESS_BACKGROUND_LOCATION
            )
        }
    }



//    override fun onPermissionsGranted(requestCode: Int, perms: MutableList<String>) {
//        enableLocation()
//    }
//
//    override fun onPermissionsDenied(requestCode: Int, perms: MutableList<String>) {
//        if(EasyPermissions.somePermissionPermanentlyDenied(this, perms)){
//            AppSettingsDialog.Builder(this).build().show()
//        }
//        else{
//            enableLocation()
//        }
//    }

//    override fun onRequestPermissionsResult(
//        requestCode: Int,
//        permissions: Array<out String>, grantResults: IntArray
//    ) {
//        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
//        EasyPermissions.onRequestPermissionsResult(
//            requestCode, permissions,
//            grantResults, this
//        )
//
//    }

//    override fun onResume() {
//        super.onResume()
//        enableLocation()
//    }
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
    private fun moveCamera(destination: LatLng) {
        val projection: Projection = map!!.getProjection()
        val bounds = projection.visibleRegion.latLngBounds
        val boundsTopY = projection.toScreenLocation(bounds.northeast).y
        val boundsBottomY = projection.toScreenLocation(bounds.southwest).y
        val boundsTopX = projection.toScreenLocation(bounds.northeast).x
        val boundsBottomX = projection.toScreenLocation(bounds.southwest).x
        val offsetY = (boundsBottomY - boundsTopY) / 10
        val offsetX = (boundsTopX - boundsBottomX) / 10
        val destinationPoint: Point = projection.toScreenLocation(destination)
        val destinationX: Int = destinationPoint.x
        val destinationY: Int = destinationPoint.y
        var scrollX = 0
        var scrollY = 0
        if (destinationY <= boundsTopY + offsetY) {
            scrollY = -Math.abs(boundsTopY + offsetY - destinationY)
        } else if (destinationY >= boundsBottomY - offsetY) {
            scrollY = Math.abs(destinationY - (boundsBottomY - offsetY))
        }
        if (destinationX >= boundsTopX - offsetX) {
            scrollX = Math.abs(destinationX - (boundsTopX - offsetX))
        } else if (destinationX <= boundsBottomX + offsetX) {
            scrollX = -Math.abs(boundsBottomX + offsetX - destinationX)
        }

        map!!.animateCamera(CameraUpdateFactory.scrollBy(scrollX.toFloat(), scrollY.toFloat()))
    }



    companion object{
        private val TAG = "DEBUG_LOG"
        private val REQUEST_CODE = 1
    }
}