package online.mohmedbakr.image_match_game.models
enum class BoardSize (val numCards:Int,name:String){
    EASY(8,"Easy"),
    MEDIUM(18,"Medium"),
    HARD(24,"Hard");

    companion object {
        fun getByValue(value:Int) = values().first { it.numCards == value }
        fun getStringBoardSize(value: Int) = getByValue(value).let {
           "${it.name} (${it.getHeight()} x ${it.getWidth()})"
        }
    }

    fun getWidth() = when(this){
        EASY -> 2
        MEDIUM -> 3
        HARD -> 4
    }

    fun getHeight() = numCards / getWidth()
    fun getNumPairs() = numCards / 2
}
