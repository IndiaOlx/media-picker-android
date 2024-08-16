package com.mediapicker.sample

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.mediapicker.gallery.Gallery
import com.mediapicker.gallery.GalleryConfig
import com.mediapicker.gallery.domain.contract.IGalleryCommunicator
import com.mediapicker.gallery.domain.entity.*
import com.mediapicker.gallery.presentation.fragments.HomeFragment
import com.mediapicker.gallery.presentation.utils.DefaultPage
import com.mediapicker.gallery.presentation.viewmodels.VideoFile
import com.mediapicker.sample.databinding.ActivityMainBinding
import java.io.File


class MainActivity : AppCompatActivity() {

    private val REQUEST_VIDEO_CAPTURE: Int = 1000
    private var fragment: HomeFragment? = null

    private var videoCaptureLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val videoUri: Uri? = result.data?.data
            Toast.makeText(baseContext, "Recorded ", Toast.LENGTH_LONG).show()
            fragment?.reloadMedia()
        }
    }

    private val activityMainBinding: ActivityMainBinding by lazy {
        ActivityMainBinding.inflate(layoutInflater)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(activityMainBinding.root)
        setUpGallery()
        showStepFragment()
    }

    private fun getValidation(): Validation {
        return Validation.ValidationBuilder()
            .setMinPhotoSelection(Rule.MinPhotoSelection(1, "Minimum 1 photos can be selected "))
            .setMaxPhotoSelection(Rule.MaxPhotoSelection(5, "Maximum 5 photos can be selected "))
            .build()
    }

    private fun setUpGallery() {
        val galleryConfig = GalleryConfig.GalleryConfigBuilder(
            applicationContext,
            BuildConfig.APPLICATION_ID + ".provider",
            MyClientGalleryCommunicator()
        )
            .useMyPhotoCamera(true)
            .useMyVideoCamera(false)
            .needToShowPreviewCarousal(CarousalConfig(true, 0, true, 0))
            .mediaScanningCriteria(GalleryConfig.MediaScanningCriteria("", ""))
            .typeOfMediaSupported(GalleryConfig.MediaType.PhotoWithFolderAndVideo)
            .validation(getValidation())
            .photoTag(PhotoTag(true, "RC photo"))
            .build()
        Gallery.init(galleryConfig)
    }

    private fun attachGalleryFragment() {
        try {
            val transaction = supportFragmentManager.beginTransaction()
            val photos = SelectedItemHolder.listOfSelectedPhotos
            fragment = DemoHomeFragment.getInstance(
                photos,
                SelectedItemHolder.listOfSelectedVideos,
                defaultPageType = DefaultPage.PhotoPage
            )
            transaction.replace(
                activityMainBinding.container.id,
                fragment!!,
                fragment!!::class.java.simpleName
            )
            transaction.addToBackStack(fragment!!.javaClass.name)
            transaction.commitAllowingStateLoss()
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
    }

    fun jumpToGallery() {
        /* startActivity(GalleryActivity.getGalleryActivityIntent(SelectedItemHolder.listOfSelectedPhotos,
             SelectedItemHolder.listOfSelectedVideos,
             defaultPageType = DefaultPage.PhotoPage,context = baseContext))*/
        attachGalleryFragment()
    }

    private fun showStepFragment() {
        try {
            val transaction = supportFragmentManager.beginTransaction()
            val fragment = StepFragment()
            transaction.replace(
                activityMainBinding.container.id,
                fragment,
                fragment::class.java.simpleName
            )
            transaction.commitAllowingStateLoss()
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
    }

    inner class MyClientGalleryCommunicator : IGalleryCommunicator {
        override fun onCloseMainScreen() {
            Toast.makeText(baseContext, "Close on main screen", Toast.LENGTH_LONG).show()

        }

        override fun actionButtonClick(
            listOfSelectedPhotos: List<PhotoFile>,
            listofSelectedVideos: List<VideoFile>
        ) {
            SelectedItemHolder.listOfSelectedPhotos = listOfSelectedPhotos.toMutableList()
            SelectedItemHolder.listOfSelectedVideos = listofSelectedVideos
            showStepFragment()
        }

        override fun onFolderSelect() {
            showMessage("folderClicked")
        }

        override fun captureImage() {
            showMessage("captureImage")
        }

        override fun onImageCaptured(capturedImage: File) {
            showMessage("onImageCaptured")
        }

        override fun recordVideo() {
            dispatchTakeVideoIntent()
        }

        override fun onVideoRecorded(file: File) {
            showMessage("onVideoRecorded")
        }

        private fun showMessage(msg: String) {
            Toast.makeText(applicationContext, msg, Toast.LENGTH_LONG).show()

        }

        override fun onPermissionDenied() {
            Toast.makeText(applicationContext, "Permission denied :(", Toast.LENGTH_LONG).show()
        }

        override fun onNeverAskPermissionAgain() {
            Toast.makeText(applicationContext, "Permission denied :(", Toast.LENGTH_LONG).show()
        }
    }


    private fun dispatchTakeVideoIntent() {
        val videoIntent = Intent(MediaStore.ACTION_VIDEO_CAPTURE)
        videoCaptureLauncher.launch(videoIntent)
    }

}


object SelectedItemHolder {
    var listOfSelectedPhotos = emptyList<PhotoFile>()

//    var listOfSelectedPhotos = mutableListOf<PhotoFile>().apply {
//        val builder= PhotoFile.Builder()
//        builder.apolloKey = "11111"
//        builder.imageId = 25
//        builder.fullPhotoUrl("https://www.hackingwithswift.com/uploads/matrix.jpg")
//        this.add(builder.build())

    /*   val builder1 = PhotoFile.Builder()
       builder1.apolloKey = "11112"
       builder1.imageId = 20
       builder1.fullPhotoUrl("https://www.hackingwithswift.com/uploads/matrix.jpg")
       this.add(builder1.build())*/
//    }
    var listOfSelectedVideos = emptyList<VideoFile>()
}