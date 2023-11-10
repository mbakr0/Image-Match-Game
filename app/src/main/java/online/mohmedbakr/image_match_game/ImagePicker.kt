package online.mohmedbakr.image_match_game

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.text.InputFilter
import android.view.MenuItem
import android.view.View
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.widget.addTextChangedListener
import androidx.recyclerview.widget.GridLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import online.mohmedbakr.image_match_game.databinding.ActivityImagePickerBinding
import online.mohmedbakr.image_match_game.models.BoardSize
import online.mohmedbakr.image_match_game.util.BOARD_SIZE
import online.mohmedbakr.image_match_game.util.BitmapScale
import online.mohmedbakr.image_match_game.util.EXTRA_GAME_NAME
import java.io.ByteArrayOutputStream

class ImagePicker : AppCompatActivity() {
    private lateinit var boardSize: BoardSize
    private var numImageRequired = 0

    private val storage = Firebase.storage
    private val database = Firebase.firestore
    private var chosenImages = mutableListOf<Uri>()

    private lateinit var binding :ActivityImagePickerBinding
    private lateinit var launcher: ActivityResultLauncher<PickVisualMediaRequest>
    private lateinit var requestPermissionLauncher: ActivityResultLauncher<String>
    private lateinit var adapter: ImagePickerAdapter

    companion object {
        private const val READ_EXTERNAL_PHOTO_CODE = 4000
        private const val READ_EXTERNAL_STORAGE = android.Manifest.permission.READ_EXTERNAL_STORAGE
        private const val MIN_GAME_NAME_LENGTH = 3
        private const val MAX_GAME_NAME_LENGTH = 14
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityImagePickerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        createLauncher()

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        boardSize = intent.getSerializableExtra(BOARD_SIZE) as BoardSize
        numImageRequired = boardSize.getNumPairs()
        title = "Choose pics (${chosenImages.size / numImageRequired} / $numImageRequired)"

        adapter = ImagePickerAdapter(chosenImages,boardSize,
            object : ImagePickerAdapter.ImageClickListener
            {
                override fun onPlaceholderCLick() {
                    when {
                        isPermissionGranted() -> {
                            launchIntentForPhoto()
                        }
                        else -> requestPermissionLauncher.launch(READ_EXTERNAL_STORAGE)
                    }
                }
            })
        binding.gameName.filters = arrayOf(InputFilter.LengthFilter(MAX_GAME_NAME_LENGTH))
        binding.gameName.addTextChangedListener { binding.btnSave.isEnabled = shouldEnabledSaveButton() }

        binding.btnSave.setOnClickListener {
            saveDataToFirebase()
        }

        binding.rvImagePicker.setHasFixedSize(true)
        binding.rvImagePicker.layoutManager = GridLayoutManager(this,boardSize.getWidth())
        binding.rvImagePicker.adapter = adapter
    }
    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId){
        android.R.id.home -> {finish();true}
        else -> super.onOptionsItemSelected(item)
    }

    private fun createLauncher() {
        launcher = registerForActivityResult(ActivityResultContracts.PickMultipleVisualMedia()){
            it?.let {
                for (i in it.indices)
                    if (chosenImages.size < numImageRequired)
                        chosenImages.add(it[i])
                adapter.notifyDataSetChanged()
                title = "Choose pics (${chosenImages.size} / $numImageRequired)"
                binding.btnSave.isEnabled = shouldEnabledSaveButton()
            }
        }

        requestPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()){isGranted->
            if (isGranted)
                launchIntentForPhoto()
        }
    }

    fun isPermissionGranted() =
        ContextCompat.checkSelfPermission(baseContext, READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED

    private fun shouldEnabledSaveButton():Boolean{
        if (numImageRequired != chosenImages.size)
            return false
        if (binding.gameName.text.isBlank() ||binding.gameName.text.length < MIN_GAME_NAME_LENGTH)
            return false
        return true
    }


    private fun launchIntentForPhoto() {
        launcher.launch(PickVisualMediaRequest(
            mediaType = ActivityResultContracts.PickVisualMedia.ImageOnly
        ))
    }

    private fun saveDataToFirebase() {
        binding.btnSave.isEnabled = false
        val customGameName = binding.gameName.text.toString()
        database.collection("game").document(customGameName).get().addOnSuccessListener { document->
            if (document != null && document.data != null)
            {
                AlertDialog.Builder(this)
                    .setTitle("Name Taken")
                    .setMessage("A game with that name ``$customGameName`` already exist")
                    .setPositiveButton("OK",null)
                binding.btnSave.isEnabled = true
            }
            else
                handleImageUploading(customGameName)

        }.addOnFailureListener {
            binding.btnSave.isEnabled = true
        }
    }

    private fun handleImageUploading(customGameName: String) {
        binding.pbUploading.visibility = View.VISIBLE
        val uploadedImagesUrls = mutableListOf<String>()
        var error = false

        for ((i,uri) in chosenImages.withIndex())
        {
            val imageByteArray = getImageByteArray(uri)
            val filePath = "images/" + customGameName + System.currentTimeMillis()+"_" + i + ".jpg"
            val photoReference = storage.reference.child(filePath)

            photoReference.putBytes(imageByteArray).addOnCompleteListener {
                it.result.toString()
            }
            photoReference.putBytes(imageByteArray)
                .continueWithTask { _ ->
                    photoReference.downloadUrl
                }.addOnCompleteListener {downUrlTask->
                    if (!downUrlTask.isSuccessful || error)
                    {
                        error = true
                        binding.pbUploading.visibility = View.GONE
                        return@addOnCompleteListener
                    }
                    binding.pbUploading.progress = uploadedImagesUrls.size * 100/ chosenImages.size
                    val imageUri = downUrlTask.result.toString()
                    uploadedImagesUrls.add(imageUri)
                    if (uploadedImagesUrls.size == chosenImages.size)
                        handleAllImageUploaded(customGameName,uploadedImagesUrls)
                }
        }

    }

    private fun handleAllImageUploaded(
        gameName: String,
        imagesUrls: MutableList<String>,
    ) {
        database.collection("games").document(gameName)
            .set(mapOf("images" to imagesUrls))
            .addOnCompleteListener {gameCreationTask->
                if (!gameCreationTask.isSuccessful )
                    return@addOnCompleteListener
                MaterialAlertDialogBuilder(this)
                    .setTitle("Uploaded Complete")
                    .setPositiveButton("Ok"){_,_->
                        val data = Intent()
                        data.putExtra(EXTRA_GAME_NAME,gameName)
                        setResult(Activity.RESULT_OK,data)
                        finish()
                    }.show()
            }
    }

    private fun getImageByteArray(photoUri: Uri) : ByteArray{
        val stream = contentResolver.openInputStream(photoUri)
        val originalBitmap = BitmapFactory.decodeStream(stream)
        val scaledBitmap = BitmapScale.scaleToFitWidth(originalBitmap,250)
        val imageByteArray = ByteArrayOutputStream()
        scaledBitmap.compress(Bitmap.CompressFormat.JPEG,60,imageByteArray)
        stream?.close()
        return  imageByteArray.toByteArray()
    }
}