package kirill.com.fashionrecommendation

import android.app.Activity
import android.graphics.Bitmap
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel

class Detector(modelPath: String, parentActivity: Activity, inputBufferSize: Int, resultArraySize: Int){
    private val tflite: Interpreter
    private lateinit var result:  Array<FloatArray>
    private lateinit var data: ByteBuffer
    private val intValues = IntArray(224 * 224)

    init {
        val tfliteOptions = Interpreter.Options()
        tfliteOptions.setUseNNAPI(true)
        tflite = Interpreter(loadModelFile(parentActivity, modelPath), tfliteOptions)
        data = ByteBuffer.allocateDirect(inputBufferSize)
        data.order(ByteOrder.nativeOrder())
        result = arrayOf(FloatArray(resultArraySize))
    }

    fun doInference() : FloatArray {
        tflite.run(data, result)
        return result[0].clone()
    }
    fun convertTensorToByteBuffer(tensor: Array<FloatArray>) {
        data.rewind()
        for (i in 0 until 8) {
            for (j in 0 until 1024) {
                data.putFloat(tensor[i][j])
            }
        }
    }

    fun convertBitmapToByteBuffer(bitmap: Bitmap) {
        data.rewind()
        bitmap.getPixels(intValues, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        var pixel = 0
        for (i in 0 until 224) {
            for (j in 0 until 224) {
                val value = intValues[pixel++]
                addPixelValue(value)
            }
        }
    }

    private fun getProbability(labelIndex: Int): Float {
        return result[0][labelIndex]
    }

    private fun setProbability(labelIndex: Int, value: Number) {
        result[0][labelIndex] = value.toFloat()
    }

    private fun getNormalizedProbability(labelIndex: Int): Float {
        return result[0][labelIndex]
    }

    fun getClassificationResults(labels: List<String>) : List<ClassificationResult>{
        val confidences = mutableListOf<ClassificationResult>()

        for (i in 0 until labels.size){
            confidences.add(ClassificationResult(labels[i], getNormalizedProbability(i)))
        }
        return confidences.sortedBy { it.confidence}.takeLast(5)
    }

    private fun addPixelValue(pixelValue: Int) {
        data.putFloat(((pixelValue shr 16 and 0xFF) - 127.5f) / 127.5f)
        data.putFloat(((pixelValue shr 8 and 0xFF) - 127.5f) / 127.5f)
        data.putFloat(((pixelValue and 0xFF) - 127.5f) / 127.5f)
    }

    private fun loadModelFile(activity: Activity, path: String): MappedByteBuffer {
        val fileDescriptor = activity.assets.openFd(path)
        val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
        val fileChannel = inputStream.channel
        val startOffset = fileDescriptor.startOffset
        val declaredLength = fileDescriptor.declaredLength
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
    }
}