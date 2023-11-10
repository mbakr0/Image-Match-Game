package online.mohmedbakr.image_match_game

import android.graphics.Color
import android.net.Uri
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import androidx.core.view.setMargins
import androidx.recyclerview.widget.RecyclerView
import online.mohmedbakr.image_match_game.databinding.CardImageBinding
import online.mohmedbakr.image_match_game.models.BoardSize

class ImagePickerAdapter
    (
    private val imagesUri: List<Uri>,
    private val boardSize: BoardSize,
    private val onImageClickListener: ImageClickListener,
) : RecyclerView.Adapter<ImagePickerAdapter.ViewHolder>()
{
    lateinit var binding: CardImageBinding
    interface ImageClickListener{
        fun onPlaceholderCLick()
    }
    companion object {
        private const val MARGIN_SIZE = 10
    }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val cardHeight = parent.height / boardSize.getHeight()
        val cardWidth = parent.width / boardSize.getWidth()
        val cardSideLength = minOf(cardHeight,cardWidth) - (2 * MARGIN_SIZE)

        binding = CardImageBinding.inflate(LayoutInflater.from(parent.context))
        val layoutParams = binding.cusomImage.layoutParams as  ViewGroup.MarginLayoutParams
        layoutParams.height = cardSideLength
        layoutParams.width = cardSideLength
        layoutParams.setMargins(MARGIN_SIZE)
        return ViewHolder()
    }

    override fun getItemCount() = boardSize.getNumPairs()

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        if (position < imagesUri.size)
            holder.bind(imagesUri[position])
        else
            holder.bind()

    }

    inner class ViewHolder : RecyclerView.ViewHolder(binding.root){
        private val customImage: ImageView = binding.cusomImage
        fun bind(imagesUri: Uri) {
            customImage.setImageURI(imagesUri)
            customImage.setOnClickListener(null)
            customImage.setBackgroundColor(Color.TRANSPARENT)
        }

        fun bind() {
            customImage.setOnClickListener{onImageClickListener.onPlaceholderCLick()}
        }

    }

}