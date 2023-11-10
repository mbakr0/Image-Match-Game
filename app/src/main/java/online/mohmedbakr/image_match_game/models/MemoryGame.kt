package online.mohmedbakr.image_match_game.models
import online.mohmedbakr.image_match_game.util.DEFAULT_ICONS

class MemoryGame(
    private val boardSize: BoardSize,
    customGameImages: List<String>?
) {
    private var numCardFlips = 0
    val cards : List<MemoryCard> = if (customGameImages == null) {
        val chosenImages = DEFAULT_ICONS.take(boardSize.getNumPairs())
        val randomizedImages = (chosenImages + chosenImages).shuffled()
        randomizedImages.map { MemoryCard(it) }
    } else {
        val randomizedImages = (customGameImages + customGameImages).shuffled()
        randomizedImages.map { MemoryCard(it.hashCode(),it) }
    }
    var numPairsFound = 0


    private var indexOfSingleSelectedCard:Int? = null
    fun flipCard(position: Int) :Boolean{
        numCardFlips++
        var foundMatch = false
        if (indexOfSingleSelectedCard == null)
        {
            restoreCards()
            indexOfSingleSelectedCard = position
        } else {
            foundMatch = checkForMatch(position, indexOfSingleSelectedCard!!)
            indexOfSingleSelectedCard = null
        }
        cards[position].isFaceUp = !cards[position].isFaceUp
        return foundMatch
    }

    private fun checkForMatch(position1: Int, position2: Int):Boolean {
        if (cards[position1].identifier != cards[position2].identifier)
            return false
        cards[position1].isMatch = true
        cards[position2].isMatch = true
        numPairsFound++
        return true
    }

    private fun restoreCards() {
        cards.forEach { card-> if (!card.isMatch) card.isFaceUp = false }
    }


    fun cardFaceUp(position: Int) = cards[position].isFaceUp
    fun getNumMoves() = numCardFlips / 2
    fun numGameWon() = if (haveWonGame()) 1 else 0
    fun getBestScore(bestScore: Int) =
        if(haveWonGame())
            calculateScore(bestScore)
        else bestScore

    fun haveWonGame() = numPairsFound == boardSize.getNumPairs()
    private fun calculateScore(bestScore: Int) =
        if (bestScore == 0)
            getNumMoves()
        else
            minOf(getNumMoves(),bestScore)

}
