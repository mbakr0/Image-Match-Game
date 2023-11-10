package online.mohmedbakr.image_match_game.util

import android.content.Context
import android.content.Intent
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.ActivityResultLauncher
import androidx.appcompat.app.AlertDialog
import androidx.swiperefreshlayout.widget.CircularProgressDrawable
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import online.mohmedbakr.image_match_game.ImagePicker
import online.mohmedbakr.image_match_game.R
import online.mohmedbakr.image_match_game.databinding.DialogBoardSizeBinding
import online.mohmedbakr.image_match_game.databinding.DialogDownloadBoardBinding
import online.mohmedbakr.image_match_game.models.BoardSize

object Dialog {

    fun showCreationDialog(
        launcher: ActivityResultLauncher<Intent>,
        context: Context,
        dialogBoard: DialogBoardSizeBinding
    ):Boolean {
        val radioGroup = dialogBoard.radioGroup

        showAlertDialog(context,"Create your own memory game",dialogBoard.root){
            val desiredBoardSize = when(radioGroup.checkedRadioButtonId){
                R.id.rbEasy -> BoardSize.EASY
                R.id.rbMedium -> BoardSize.MEDIUM
                else -> BoardSize.HARD
            }
            launcher.launch(Intent(context, ImagePicker::class.java).putExtra(BOARD_SIZE,desiredBoardSize))
        }
        return true

    }

    fun loadingDialog(context: Context,dialogView:DialogDownloadBoardBinding): AlertDialog {
        (dialogView.root.parent as ViewGroup?)?.removeView(dialogView.root)
        val circularProgress= CircularProgressDrawable(context)
        circularProgress.strokeWidth = 5f
        circularProgress.centerRadius = 30f
        circularProgress.start()
        dialogView.progress.setImageDrawable(circularProgress)

        return MaterialAlertDialogBuilder(context)
            .setTitle("Loading name game")
            .setView(dialogView.root)
            .create()
    }

    fun showAlertDialog(context: Context, title:String, view: View?, positiveClickListener: View.OnClickListener): Boolean {
        (view?.parent as ViewGroup?)?.removeView(view)
        MaterialAlertDialogBuilder(context)
            .setTitle(title)
            .setView(view)
            .setNegativeButton("Cancel",null)
            .setPositiveButton("Ok") { _, _ ->
                positiveClickListener.onClick(null)
            }
            .show()
        return true
    }
}