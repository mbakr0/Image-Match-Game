package online.mohmedbakr.image_match_game.util
import android.graphics.Bitmap

object BitmapScale {
    fun scaleToFitWidth(bitmap:Bitmap,width:Int) : Bitmap {
        val factor = width / bitmap.width.toFloat()
        return Bitmap.createScaledBitmap(bitmap,width,(bitmap.height * factor).toInt(),true)
    }

    fun scaleToFitHeight(bitmap:Bitmap,height:Int): Bitmap {
        val factor = height / bitmap.height.toFloat()
        return Bitmap.createScaledBitmap(bitmap,(bitmap.width * factor).toInt(),height,true)
    }
}
