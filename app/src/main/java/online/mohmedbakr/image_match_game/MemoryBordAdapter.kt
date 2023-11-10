package online.mohmedbakr.image_match_game

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import androidx.core.view.setMargins
import androidx.recyclerview.widget.RecyclerView
import com.squareup.picasso.Picasso
import online.mohmedbakr.image_match_game.databinding.MemoryCardBinding
import online.mohmedbakr.image_match_game.models.BoardSize
import online.mohmedbakr.image_match_game.models.MemoryCard

class MemoryBordAdapter(
    private val cardList: List<MemoryCard>,
    private val bordSize: BoardSize,
    private val clickListener: CardClickListener
) : RecyclerView.Adapter<MemoryBordAdapter.ViewHolder>() {
    private lateinit var binding: MemoryCardBinding
    companion object {
        private const val MARGIN_SIZE = 10
    }
    interface CardClickListener {
        fun onClickListener(position: Int)
    }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) : ViewHolder{
        val cardWidth = parent.width / bordSize.getWidth()
        val cardHeight = parent.height / bordSize.getHeight()
        val cardsSideLength = minOf(cardHeight,cardWidth) - (2 * MARGIN_SIZE)

        binding = MemoryCardBinding.inflate(LayoutInflater.from(parent.context))
        val layoutParams = binding.cardView.layoutParams as ViewGroup.MarginLayoutParams

        layoutParams.height = cardsSideLength
        layoutParams.width = cardsSideLength
        layoutParams.setMargins(MARGIN_SIZE)
        return ViewHolder()
    }

    override fun getItemCount() = cardList.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(position)
    }

    inner class ViewHolder : RecyclerView.ViewHolder(binding.root) {
        private val imageButton = binding.imageButton

        fun bind(position: Int) {

            val memoryCard = cardList[position]
            if (memoryCard.isFaceUp)
            {
                imageButton.scaleType = ImageView.ScaleType.CENTER_CROP
                if(memoryCard.imageUrl == null)
                    imageButton.setImageResource(memoryCard.identifier)
                else
                    Picasso.get().load(memoryCard.imageUrl).placeholder(R.drawable.ic_refresh).into(imageButton)
            } else
            {
                imageButton.setImageResource(R.drawable.back)
                imageButton.scaleType = ImageView.ScaleType.CENTER
            }
            imageButton.alpha = if (memoryCard.isMatch) 0.4f else 1.0f
            imageButton.setOnClickListener {
                clickListener.onClickListener(position)
            }
        }

    }

}