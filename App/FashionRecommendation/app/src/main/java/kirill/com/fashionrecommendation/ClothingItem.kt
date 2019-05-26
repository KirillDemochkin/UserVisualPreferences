package kirill.com.fashionrecommendation

import android.graphics.Bitmap

data class ClothingItem(val picture: Bitmap, val name: String, var result: ClassificationResult)