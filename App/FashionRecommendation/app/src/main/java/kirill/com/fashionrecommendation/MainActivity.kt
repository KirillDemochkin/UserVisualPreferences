package kirill.com.fashionrecommendation

import android.Manifest
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import kotlinx.android.synthetic.main.activity_main.*
import org.tensorflow.lite.Interpreter
import java.nio.MappedByteBuffer
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.io.FileInputStream
import java.nio.channels.FileChannel
import android.graphics.drawable.Drawable
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.util.Log
import android.view.View
import android.widget.Toast
import java.nio.ByteBuffer
import java.io.BufferedReader
import java.io.InputStreamReader
import java.nio.ByteOrder
import java.util.*
import kotlin.system.measureTimeMillis


class MainActivity : AppCompatActivity() {
    private lateinit var labels: List<String>
    private val REQUEST_LOCATION_PERMISSION = 0
    private val clothes: MutableList<ClothingItem> = mutableListOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        checkPermissions()

        val mobileNet = Detector(
            "mobilenet.tflite",
            this,
            1*224*224*3*4,
            1024)

        val aggregator = Detector(
            "sass.tflite",
            this,
            8 * 1024 * 4,
            75)

        labels = loadLabelList(this)
        loadItems()
        val adapter = CustomAdapter(this, clothes.map { it.picture }.toTypedArray())
        imageListView.adapter = adapter

        classifyButton.setOnClickListener {
            val featureVectorTensor = arrayOf(
                FloatArray(1024),
                FloatArray(1024),
                FloatArray(1024),
                FloatArray(1024),
                FloatArray(1024),
                FloatArray(1024),
                FloatArray(1024),
                FloatArray(1024)
                )

            for (i in 0 until clothes.size) {
                val resized = Bitmap.createScaledBitmap(clothes[i].picture, 224, 224, true)
                mobileNet.convertBitmapToByteBuffer(resized)
                val featureVector = mobileNet.doInference()
                featureVectorTensor[i] = featureVector.clone()
            }

            val classificationResults = doInferenceAggregation(aggregator, featureVectorTensor)
            val str = StringBuilder()
            for (result in classificationResults) {
                str.append("${result.category}: (${result.confidence})\n")
            }
            resultsTextView.text = str.toString()
            showMapButton.visibility = View.VISIBLE
            showAmazonButton.visibility = View.VISIBLE
        }

        showMapButton.setOnClickListener {
            val intent = Intent(this, MapsActivity::class.java)
            intent.putExtra("query", resultsTextView.text)
            startActivity(intent)
        }

        showAmazonButton.setOnClickListener{
            val intent = Intent(this, AmazonActivity::class.java)
            intent.putExtra("query", resultsTextView.text)
            startActivity(intent)
        }
    }

    private fun doInferenceAggregation(model: Detector, features: Array<FloatArray>): List<ClassificationResult> {
        model.convertTensorToByteBuffer(features)
        model.doInference()
        return model.getClassificationResults(labels)
    }

    private fun checkPermissions() {
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_COARSE_LOCATION)
            != PackageManager.PERMISSION_GRANTED) {

            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.ACCESS_COARSE_LOCATION)) {

            } else {

                ActivityCompat.requestPermissions(this,
                    arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION),
                    REQUEST_LOCATION_PERMISSION)
            }
        } else {
        }
    }

    private fun loadItems(){
        val imageNames = listOf<String>(
            "shoes.jpg",
            "converse.jpg",
            "jogging.jpg",
            "sandals.jpg",
            "jacket.jpg",
            "pantsnike.jpg",
            "runningshoes.jpg",
            "pants.jpg")

        for (i in imageNames){
            clothes.add(ClothingItem(
                Bitmap.createBitmap(BitmapFactory.decodeStream(this.assets.open(i))),
                i,
                ClassificationResult()
            ))
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int,
                                            permissions: Array<String>, grantResults: IntArray) {
        when (requestCode) {
            REQUEST_LOCATION_PERMISSION -> {

                if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {

                } else {
                    Toast.makeText(this, "The app will not work!", Toast.LENGTH_SHORT).show()
                }
                return
            }
            else -> {
            }
        }
    }

    private fun loadLabelList(activity: Activity): List<String> {
        val labels = ArrayList<String>()
        val reader = BufferedReader(InputStreamReader(activity.assets.open("labels.txt")))
        var line = reader.readLine()
        while (line != null) {
            labels.add(line)
            line = reader.readLine()
        }
        reader.close()
        return labels
    }
}
