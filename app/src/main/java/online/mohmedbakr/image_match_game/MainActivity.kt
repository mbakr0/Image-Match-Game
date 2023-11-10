package online.mohmedbakr.image_match_game

import android.animation.ArgbEvaluator
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.recyclerview.widget.GridLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.squareup.picasso.Picasso
import nl.dionsegijn.konfetti.core.Party
import nl.dionsegijn.konfetti.core.Position
import nl.dionsegijn.konfetti.core.emitter.Emitter
import online.mohmedbakr.image_match_game.databinding.ActivityMainBinding
import online.mohmedbakr.image_match_game.databinding.DialogBoardSizeBinding
import online.mohmedbakr.image_match_game.databinding.DialogDownloadBoardBinding
import online.mohmedbakr.image_match_game.models.BoardSize
import online.mohmedbakr.image_match_game.models.MemoryGame
import online.mohmedbakr.image_match_game.models.UserImageList
import online.mohmedbakr.image_match_game.util.Dialog.loadingDialog
import online.mohmedbakr.image_match_game.util.Dialog.showAlertDialog
import online.mohmedbakr.image_match_game.util.Dialog.showCreationDialog
import online.mohmedbakr.image_match_game.util.EXTRA_GAME_NAME
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {

    private val database = Firebase.firestore
    private var gameName:String? = null
    private var customGameImages :List<String>? = null

    private lateinit var adapter: MemoryBordAdapter
    private lateinit var memoryGame: MemoryGame
    private lateinit var launcher:ActivityResultLauncher<Intent>
    private lateinit var binding : ActivityMainBinding
    private lateinit var dialogBoardSizeBinding :DialogBoardSizeBinding
    private lateinit var dialogDownloadBoardBinding :DialogDownloadBoardBinding


    private val party = Party(
        speed = 0f,
        maxSpeed = 25f,
        damping = 0.9f,
        spread = 360,
        colors = listOf(0xfce18a, 0xff726d, 0xf4306d, 0xb48def),
        position = Position.Relative(0.5, 0.3),
        emitter = Emitter(duration = 100, TimeUnit.SECONDS).perSecond(250)
    )

    private var boardSize = BoardSize.EASY
    private var gameWon = 0
    private var bestScore = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        binding = ActivityMainBinding.inflate(layoutInflater)
        dialogBoardSizeBinding = DialogBoardSizeBinding.inflate(layoutInflater,null,false)
        dialogDownloadBoardBinding = DialogDownloadBoardBinding.inflate(layoutInflater,null,false)
        setContentView(binding.root)
        createLauncher()
        setupGame()
    }
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main_menu,menu)
        return true
    }
    override fun onOptionsItemSelected(item: MenuItem) =
        when (item.itemId) {
            R.id.refresh -> showAlertDialog(this,"Quit your current game ?", null) { setupGame() }
            R.id.new_size -> showDifficultyDialog()
            R.id.custom_game -> showCreationDialog(launcher,this,dialogBoardSizeBinding)
            R.id.mi_download -> showDownloadDialog()
            else -> super.onOptionsItemSelected(item)
        }

    private fun showDownloadDialog(): Boolean {
        val loadingDialog = loadingDialog(this,dialogDownloadBoardBinding)
        loadingDialog.show()
        database.collection("games").get().addOnSuccessListener { documents->
            loadingDialog.dismiss()
            if (documents.documents.isEmpty()) {
                MaterialAlertDialogBuilder(this)
                    .setMessage("No game exist")
                    .show()
                return@addOnSuccessListener
            }
            downloadListOfNameGame(documents)
        }
        return true
    }

    private fun downloadListOfNameGame(documents: QuerySnapshot) {

        val max = documents.documents.maxOf { it.id.length }
        val array = Array(documents.documents.size){
            val gameName = documents.documents[it].id
            val boardSize = (documents.documents[it].data?.get("images") as ArrayList<*>).let {list->
                BoardSize.getStringBoardSize(list.size * 2)
            }
            String.format("%-${max + 20}s %s",gameName,boardSize)
        }
        MaterialAlertDialogBuilder(this)
            .setTitle("Select name game")
            .setItems(array){ _, which ->
                downLoadGame(documents.documents[which].id)
            }
            .show()
    }

    private fun downLoadGame(customGameName: String) {
        database.collection("games").document(customGameName).get().addOnSuccessListener { document->
            val userImageList = document.toObject(UserImageList::class.java)
            if (userImageList?.images == null)
                return@addOnSuccessListener
            val numCard = userImageList.images.size * 2
            customGameImages = userImageList.images
            boardSize = BoardSize.getByValue(numCard)
            for(i in userImageList.images)
                Picasso.get().load(i).fetch()
            gameName = customGameName
            setupGame()

        }
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun updateGameWithFLip(position: Int) {
        if(memoryGame.cardFaceUp(position))
            return

        if (memoryGame.flipCard(position)){
            val color = ArgbEvaluator().evaluate(
                memoryGame.numPairsFound.toFloat() / boardSize.getNumPairs().toFloat(),
                resources.getColor(R.color.black,resources.newTheme()),
                resources.getColor(R.color.teal_700,resources.newTheme()),
            ) as Int
            binding.tvNumPairs.setTextColor(color)
            val text = "Pairs : ${memoryGame.numPairsFound} / ${boardSize.getNumPairs()}"
            binding.tvNumPairs.text = text
            if(memoryGame.haveWonGame())
            {
                binding.konfettiView.visibility = View.VISIBLE
                binding.konfettiView.start(party)
                showAlertDialog(this,"You won. Play again ?",null) { setupGame() }
            }
        }

        val text = "Moves : ${memoryGame.getNumMoves()}"
        binding.tvNumMoves.text = text
        adapter.notifyDataSetChanged()
    }

    private fun setupGame() {
        binding.konfettiView.visibility = View.GONE
        binding.konfettiView.stop(party)
        supportActionBar?.title = gameName ?: getString(R.string.app_name)
        if(::memoryGame.isInitialized)
        {
            gameWon += memoryGame.numGameWon()
            bestScore = memoryGame.getBestScore(bestScore)
        }
        memoryGame = MemoryGame(boardSize,customGameImages)
        adapter = MemoryBordAdapter(memoryGame.cards,boardSize,object : MemoryBordAdapter.CardClickListener{
            override fun onClickListener(position: Int) {
                updateGameWithFLip(position)
            }
        })
        binding.rvBord.adapter = adapter
        binding.rvBord.setHasFixedSize(true)
        binding.rvBord.layoutManager = GridLayoutManager(this,boardSize.getWidth())

        val pairText = "Pair ${memoryGame.numPairsFound} / ${boardSize.getNumPairs()}"
        val movesText = "Moves : ${memoryGame.getNumMoves()}"
        val gameWonText = "$gameWon Game won"
        val bestScoreText = "Best score : $bestScore"
        binding.tvNumPairs.text = pairText
        binding.tvNumMoves.text = movesText
        binding.numGameWon.text  = gameWonText
        binding.bestMoves.text = bestScoreText
    }

    private fun createLauncher() {
        launcher  = registerForActivityResult(ActivityResultContracts.StartActivityForResult()){result->
            if (result.resultCode == Activity.RESULT_OK)
            {
                result.data?.getStringExtra(EXTRA_GAME_NAME)?.let { downLoadGame(it) }
            }
        }
    }

    private fun showDifficultyDialog(): Boolean {
        val radioGroup = dialogBoardSizeBinding.radioGroup
        when(boardSize){
            BoardSize.EASY -> radioGroup.check(R.id.rbEasy)
            BoardSize.MEDIUM -> radioGroup.check(R.id.rbMedium)
            BoardSize.HARD -> radioGroup.check(R.id.rbHard)
        }
        showAlertDialog(this,getString(R.string.choose_difficulty),dialogBoardSizeBinding.root){
            boardSize = when(radioGroup.checkedRadioButtonId){
                R.id.rbEasy -> BoardSize.EASY
                R.id.rbMedium -> BoardSize.MEDIUM
                else -> BoardSize.HARD
            }
            gameName = null
            customGameImages = null
            setupGame()
        }
        return true
    }


}