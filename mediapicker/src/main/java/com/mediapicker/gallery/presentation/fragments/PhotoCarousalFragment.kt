package com.mediapicker.gallery.presentation.fragments

import android.Manifest
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.text.TextUtils
import android.view.View
import android.widget.LinearLayout
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import com.google.android.material.snackbar.Snackbar
import com.mediapicker.gallery.Gallery
import com.mediapicker.gallery.GalleryConfig
import com.mediapicker.gallery.R
import com.mediapicker.gallery.databinding.OssFragmentCarousalBinding
import com.mediapicker.gallery.domain.contract.GalleryPagerCommunicator
import com.mediapicker.gallery.domain.entity.GalleryViewMediaType
import com.mediapicker.gallery.domain.entity.MediaGalleryEntity
import com.mediapicker.gallery.domain.entity.PhotoFile
import com.mediapicker.gallery.presentation.activity.GalleryActivity
import com.mediapicker.gallery.presentation.activity.MediaGalleryActivity
import com.mediapicker.gallery.presentation.adapters.PagerAdapter
import com.mediapicker.gallery.presentation.carousalview.CarousalActionListener
import com.mediapicker.gallery.presentation.carousalview.MediaGalleryView
import com.mediapicker.gallery.presentation.utils.DefaultPage
import com.mediapicker.gallery.presentation.utils.PermissionsUtil
import com.mediapicker.gallery.presentation.utils.getActivityScopedViewModel
import com.mediapicker.gallery.presentation.utils.getFragmentScopedViewModel
import com.mediapicker.gallery.presentation.viewmodels.BridgeViewModel
import com.mediapicker.gallery.presentation.viewmodels.HomeViewModel
import com.mediapicker.gallery.presentation.viewmodels.VideoFile
import com.mediapicker.gallery.utils.SnackbarUtils
import java.io.Serializable
import java.util.*

open class PhotoCarousalFragment : BaseFragment(), GalleryPagerCommunicator,
    MediaGalleryView.OnGalleryItemClickListener {

    private val PHOTO_PREVIEW = 43475

    private  var permissionLauncher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { granted ->
        PermissionsUtil.handlePermissionsResult(
            requireActivity(),
            granted,
            onAllPermissionsGranted = { checkPermissions() },
            onPermissionDenied = { onPermissionDenied() }
        )
    }

    private val homeViewModel: HomeViewModel by lazy {
        getFragmentScopedViewModel { HomeViewModel(Gallery.galleryConfig) }
    }

    private val bridgeViewModel: BridgeViewModel by lazy {
        getActivityScopedViewModel {
            BridgeViewModel(
                getPhotosFromArguments(),
                getVideosFromArguments(),
                Gallery.galleryConfig
            )
        }
    }

    private val defaultPageToOpen: DefaultPage by lazy {
        getPageFromArguments()
    }

    private val ossFragmentCarousalBinding: OssFragmentCarousalBinding? by lazy {
        ossFragmentBaseBinding?.baseContainer?.findViewById<LinearLayout>(R.id.linear_layout_parent)?.let { OssFragmentCarousalBinding.bind(it) }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
    }


    override fun getLayoutId() = R.layout.oss_fragment_carousal

    override fun getScreenTitle() = Gallery.galleryConfig.galleryLabels.homeTitle.ifBlank { getString(R.string.oss_title_home_screen) }

    override fun setUpViews() {
        Gallery.pagerCommunicator = this

        if (Gallery.galleryConfig.showPreviewCarousal.showCarousal) {
            ossFragmentCarousalBinding?.mediaGalleryViewContainer?.visibility = View.VISIBLE
            ossFragmentCarousalBinding?.mediaGalleryView?.setOnGalleryClickListener(this)
            if (Gallery.galleryConfig.showPreviewCarousal.imageId != 0) {
                ossFragmentCarousalBinding?.mediaGalleryView?.updateDefaultPhoto(Gallery.galleryConfig.showPreviewCarousal.imageId)
            }
            if (Gallery.galleryConfig.showPreviewCarousal.previewText != 0) {
                ossFragmentCarousalBinding?.mediaGalleryView?.updateDefaultText(Gallery.galleryConfig.showPreviewCarousal.previewText)
            }
        }

        ossFragmentBaseBinding?.ossCustomTool?.toolbarTitle?.isAllCaps = Gallery.galleryConfig.textAllCaps
        ossFragmentCarousalBinding?.actionButton?.isAllCaps = Gallery.galleryConfig.textAllCaps
        ossFragmentCarousalBinding?.actionButton?.text = Gallery.galleryConfig.galleryLabels.homeAction.ifBlank { getString(R.string.oss_posting_next) }
        requestPermissions()
    }

    private fun checkPermissions() {
        when (homeViewModel.getMediaType()) {
            GalleryConfig.MediaType.PhotoOnly -> {
                setUpWithOutTabLayout()
            }

            GalleryConfig.MediaType.PhotoWithFolderOnly -> {
                setUpWithOutTabLayout()
            }

            GalleryConfig.MediaType.PhotoWithoutCameraFolderOnly -> {
                setUpWithOutTabLayout()
            }

            else -> {
                setUpWithOutTabLayout()
            }
        }
        openPage()
        ossFragmentCarousalBinding?.actionButton?.isSelected = false
        ossFragmentCarousalBinding?.actionButton?.setOnClickListener { onActionButtonClicked() }
    }

    private fun onPermissionDenied() {
        Gallery.galleryConfig.galleryCommunicator?.onPermissionDenied()
    }

    private fun addMediaForPager(mediaGalleryEntity: MediaGalleryEntity) {
        ossFragmentCarousalBinding?.mediaGalleryView?.addMediaForPager(mediaGalleryEntity)
    }

    private fun removeMediaFromPager(mediaGalleryEntity: MediaGalleryEntity) {
        ossFragmentCarousalBinding?.mediaGalleryView?.removeMediaFromPager(mediaGalleryEntity)
    }

    private fun showNeverAskAgainPermission() {
        Gallery.galleryConfig.galleryCommunicator?.onNeverAskPermissionAgain()
    }

    override fun initViewModels() {
        super.initViewModels()
        bridgeViewModel.getActionState().observe(this) { changeActionButtonState(it) }
        bridgeViewModel.getError().observe(this) { showError(it) }
        bridgeViewModel.getClosingSignal().observe(this) { closeIfHostingOnActivity() }
    }

    private fun closeIfHostingOnActivity() {
        if (requireActivity() is GalleryActivity) {
            requireActivity().finish()
        }
    }

    override fun setHomeAsUp() = true

    fun setActionButtonLabel(label: String) {
        ossFragmentCarousalBinding?.actionButton?.text = label
    }

    fun setCarousalActionListener(carousalActionListener: CarousalActionListener?) {
        Gallery.carousalActionListener = carousalActionListener
    }

    override fun onBackPressed() {
        closeIfHostingOnActivity()
        bridgeViewModel.onBackPressed()
    }

    private fun changeActionButtonState(state: Boolean) {
        ossFragmentCarousalBinding?.actionButton?.isSelected = state
    }

    private fun showError(error: String) {
        view?.let { SnackbarUtils.show(it, error, Snackbar.LENGTH_LONG) }
    }

    private fun setUpWithOutTabLayout() {
        ossFragmentCarousalBinding?.tabLayout?.visibility = View.GONE
        PagerAdapter(
            childFragmentManager,
            listOf(
                PhotoGridFragment.getInstance(
                    getString(R.string.oss_title_tab_photo),
                    getPhotosFromArguments()
                )
            )
        ).apply {
            ossFragmentCarousalBinding?.viewPager?.adapter = this
        }
    }

    private fun openPage() {
        if (defaultPageToOpen is DefaultPage.PhotoPage) {
            ossFragmentCarousalBinding?.viewPager?.currentItem = 0
        } else {
            ossFragmentCarousalBinding?.viewPager?.currentItem = 1
        }
    }

    private fun onActionButtonClicked() {
        bridgeViewModel.complyRules()
    }

    private fun setUpWithTabLayout() {
        PagerAdapter(
            childFragmentManager, listOf(
                PhotoGridFragment.getInstance(
                    getString(R.string.oss_title_tab_photo),
                    getPhotosFromArguments()
                ),
                VideoGridFragment.getInstance(
                    getString(R.string.oss_title_tab_video),
                    getVideosFromArguments()
                )
            )
        ).apply { ossFragmentCarousalBinding?.viewPager?.adapter = this }
        ossFragmentCarousalBinding?.tabLayout?.setupWithViewPager(ossFragmentCarousalBinding?.viewPager)
    }


    private fun getPageFromArguments(): DefaultPage {
        this.arguments?.let {
            if (it.containsKey(EXTRA_DEFAULT_PAGE)) {
                return it.getSerializable(EXTRA_DEFAULT_PAGE) as DefaultPage
            }
        }
        return DefaultPage.PhotoPage
    }

    fun reloadMedia() {
        bridgeViewModel.reloadMedia()
    }

    companion object {
        fun getInstance(
            listOfSelectedPhotos: List<PhotoFile> = emptyList(),
            listOfSelectedVideos: List<VideoFile> = emptyList(),
            defaultPageType: DefaultPage = DefaultPage.PhotoPage
        ): PhotoCarousalFragment {
            return PhotoCarousalFragment().apply {
                this.arguments = Bundle().apply {
                    putSerializable(EXTRA_SELECTED_PHOTOS, listOfSelectedPhotos as Serializable)
                    putSerializable(EXTRA_SELECTED_VIDEOS, listOfSelectedVideos as Serializable)
                    putSerializable(EXTRA_DEFAULT_PAGE, defaultPageType)
                }
            }
        }
    }

    override fun onItemClicked(photoFile: PhotoFile, isSelected: Boolean) {
        if (isSelected) {
            if (Gallery.galleryConfig.showPreviewCarousal.addImage) {
                addMediaForPager(getMediaEntity(photoFile))
            }
        } else {
            if (Gallery.galleryConfig.showPreviewCarousal.addImage) {
                removeMediaFromPager(getMediaEntity(photoFile))
            }
        }
    }

    private fun getMediaEntity(photo: PhotoFile): MediaGalleryEntity {
        var path: String? = photo.fullPhotoUrl
        var isLocalImage = false
        if (!TextUtils.isEmpty(photo.path) && photo.path?.contains("/")!!) {
            path = photo.path
            isLocalImage = true
        }
        return MediaGalleryEntity(
            photo.path,
            photo.imageId,
            path,
            isLocalImage,
            GalleryViewMediaType.IMAGE
        )
    }

    private fun convertPhotoFileToMediaGallery(photoList: List<PhotoFile>): ArrayList<MediaGalleryEntity> {
        val mediaList = ArrayList<MediaGalleryEntity>()
        for (photo in photoList) {
            mediaList.add(getMediaEntity(photo))
        }
        return mediaList
    }

    override fun onPreviewItemsUpdated(listOfSelectedPhotos: List<PhotoFile>) {
        if (Gallery.galleryConfig.showPreviewCarousal.addImage) {
            ossFragmentCarousalBinding?.mediaGalleryView?.setImagesForPager(convertPhotoFileToMediaGallery(listOfSelectedPhotos))
        }
    }

    override fun onGalleryItemClick(mediaIndex: Int) {
        Gallery.carousalActionListener?.onGalleryImagePreview()
        MediaGalleryActivity.startActivityForResult(
            this, convertPhotoFileToMediaGallery(
                bridgeViewModel.getSelectedPhotos()
            ), mediaIndex, "", PHOTO_PREVIEW
        )
    }

    private fun requestPermissions() {
        PermissionsUtil.requestPermissions(requireActivity(), permissionLauncher)
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PHOTO_PREVIEW && view != null) {
            var index = 0
            if (data != null) {
                val bundle = data.extras
                index = bundle!!.getInt("gallery_media_index", 0)
            }
            ossFragmentCarousalBinding?.mediaGalleryView?.setSelectedPhoto(index)
        }
    }
}
