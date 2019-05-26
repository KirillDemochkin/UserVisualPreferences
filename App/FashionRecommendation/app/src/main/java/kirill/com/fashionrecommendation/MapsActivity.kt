package kirill.com.fashionrecommendation

import android.content.Context
import android.location.Location
import android.location.LocationManager
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Toast
import com.github.kittinunf.fuel.httpGet
import com.github.kittinunf.fuel.json.responseJson
import com.github.kittinunf.result.Result
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.GoogleMap
import kotlinx.android.synthetic.main.activity_maps.*
import org.json.JSONArray
import org.json.JSONObject
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.MapsInitializer
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.LatLng
import java.lang.Exception
import com.google.android.gms.maps.CameraUpdate
import com.google.android.gms.maps.model.CameraPosition
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock


class MapsActivity : AppCompatActivity(), OnMapReadyCallback {


    private val API_KEY = "AIzaSyBnPM17VdbFk0NqUXyiWJRa3aThUN5Oacw"
    private val RADIUS = 1000
    private val placesList = mutableListOf<Shop>()
    private lateinit var shopNames: MutableList<String>
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var adapter: ArrayAdapter<String>
    private lateinit var query: List<String>
    private val lock = ReentrantLock()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_maps)

        mapView.onCreate(savedInstanceState);

        mapView.onResume();

        try {
            MapsInitializer.initialize(this.applicationContext);
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
        shopNames = placesList.map { it.name }.toMutableList()
        adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, shopNames)
        mapResultList.adapter = adapter
        query = intent
            ?.extras
            ?.getCharSequence("query")
            ?.split('\n')
            ?.map { it.split(':')[0] }
            ?.filter { it.isNotEmpty() }
            ?: listOf("Zara")
        /*query = intent
            ?.extras
            ?.getCharSequence("query")
            ?.split('\n')
            ?.map { it.split(':')[0] }?.last { it.isNotEmpty() } ?: ""*/
        mapView.getMapAsync(this)
    }



    private fun handleMapResult(place: JSONObject, map: GoogleMap){
        val name = place.getString("name")
        val lat = place.getJSONObject("geometry").getJSONObject("location").getDouble("lat")
        val lng = place.getJSONObject("geometry").getJSONObject("location").getDouble("lng")
        val storeLocation = LatLng(lat, lng)
        map.addMarker(
            MarkerOptions().position(storeLocation)
                .title(name)
        )
        //map.moveCamera(CameraUpdateFactory.newLatLng(storeLocation))
        placesList.add(Shop(LatLng(lat, lng), name))
    }

    override fun onMapReady(googleMap: GoogleMap?) {
        shopNames.clear()
        googleMap?.let {
            fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
            try {
                fusedLocationClient.lastLocation
                    .addOnSuccessListener { location: Location? ->
                        location?.let {
                            val cameraPosition = CameraPosition.Builder()
                                .target(
                                    LatLng(
                                        location.latitude,
                                        location.longitude
                                    )
                                )      // Sets the center of the map to Mountain View
                                .zoom(10f)                   // Sets the zoom
                                .bearing(90f)                // Sets the orientation of the camera to east
                                .tilt(30f)                   // Sets the tilt of the camera to 30 degrees
                                .build()                   // Creates a CameraPosition from the builder
                            googleMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition))
                            query.forEach { q ->
                                "https://maps.googleapis.com/maps/api/place/textsearch/json?query=$q&location=${location.latitude},${location.longitude}&radius=$RADIUS&type=store&key=$API_KEY"
                                    .httpGet()
                                    .responseJson { request, response, result ->
                                        when (result) {
                                            is Result.Failure -> {
                                                val ex = result.getException()
                                            }
                                            is Result.Success -> {
                                                lock.withLock {
                                                    val data = result.get().obj().getJSONArray("results")
                                                    for (i in 0 until data.length()) {
                                                        handleMapResult(data.getJSONObject(i), googleMap)
                                                    }
                                                    shopNames.addAll(placesList.map { it.name })
                                                    adapter.notifyDataSetChanged()
                                                    //mapResultList.adapter = adapter
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    } catch (ex: SecurityException) {
                    Toast.makeText(this, "Missing Permissions", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

