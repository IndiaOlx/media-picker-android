package com.mediapicker.gallery.presentation.viewmodels

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.mediapicker.gallery.GalleryConfig
import com.mediapicker.gallery.domain.action.RuleAction
import com.mediapicker.gallery.domain.entity.PhotoFile
import com.mediapicker.gallery.domain.entity.Rule
import com.mediapicker.gallery.domain.entity.Validation

class BridgeViewModel(
    private var listOfSelectedPhotos: List<PhotoFile>,
    private var listOfSelectedVideos: List<VideoFile>, private val galleryConfig: GalleryConfig?
) : ViewModel() {

    private fun getValidation(): Validation {
        return Validation.ValidationBuilder()
            .setMinPhotoSelection(Rule.MinPhotoSelection(1, "Minimum 1 photos can be selected "))
            .setMaxPhotoSelection(Rule.MaxPhotoSelection(5, "Maximum 5 photos can be selected "))
            .build()
    }


    private val ruleAction: RuleAction =
        RuleAction(mediaValidation = galleryConfig?.validation ?: getValidation())

    private val reloadMediaLiveData = MutableLiveData<Unit>()

    private val recordVideoLiveData = MutableLiveData<Unit>()

    private val actionButtonStateLiveData = MutableLiveData<Boolean>()

    private val errorStateLiveData = MutableLiveData<String>()

    private val closeHostingViewLiveData = MutableLiveData<Boolean>()

    fun recordVideoWithNativeCamera() = recordVideoLiveData

    fun getActionState() = actionButtonStateLiveData

    fun getMediaStateLiveData() = reloadMediaLiveData

    fun getError() = errorStateLiveData

    fun getClosingSignal() = closeHostingViewLiveData

    fun setCurrentSelectedPhotos(listOfSelectedPhotos: List<PhotoFile>) {
        this.listOfSelectedPhotos = listOfSelectedPhotos
        shouldEnableActionButton()
    }

    fun setCurrentSelectedVideos(listOfSelectedVideos: List<VideoFile>) {
        this.listOfSelectedVideos = listOfSelectedVideos
        shouldEnableActionButton()
    }

    fun getSelectedPhotos(): List<PhotoFile> = listOfSelectedPhotos

    private fun shouldEnableActionButton() {
        galleryConfig?.let {
            if (it.shouldOnlyValidatePhoto()) {
                val status = ruleAction.shouldEnableActionButton(listOfSelectedPhotos.size)
                actionButtonStateLiveData.postValue(status)
            } else {
                val status = ruleAction.shouldEnableActionButton(
                    Pair(
                        listOfSelectedPhotos.size, listOfSelectedVideos.size
                    )
                )
                actionButtonStateLiveData.postValue(status)
            }
        }

    }

    private fun onActionButtonClick() {
        galleryConfig?.galleryCommunicator?.actionButtonClick(
            listOfSelectedPhotos, listOfSelectedVideos
        )
    }


    fun shouldRecordVideo() {
        galleryConfig?.let {
            if (it.shouldUseVideoCamera) {
                recordVideoLiveData.postValue(Unit)
            } else {
                it.galleryCommunicator?.recordVideo()
            }
        }
    }

    fun onBackPressed() {
        galleryConfig?.galleryCommunicator?.onCloseMainScreen()
    }

    fun getMaxSelectionLimit(): Int =
        galleryConfig?.validation?.getMaxPhotoSelectionRule()?.maxSelectionLimit ?: 5

    fun getMaxVideoSelectionLimit(): Int =
        galleryConfig?.validation?.getMaxVideoSelectionRule()?.maxSelectionLimit ?: 5

    fun getMaxLimitErrorResponse(): String =
        galleryConfig?.validation?.getMaxPhotoSelectionRule()?.message ?: ""

    fun reloadMedia() {
        reloadMediaLiveData.postValue(Unit)
    }

    fun shouldUseMyCamera(): Boolean {
        galleryConfig?.galleryCommunicator?.captureImage()
        return galleryConfig?.shouldUsePhotoCamera == true
    }

    fun onFolderSelect() {
        galleryConfig?.galleryCommunicator?.onFolderSelect()
    }

    fun getMaxVideoLimitErrorResponse(): String =
        galleryConfig?.validation?.getMaxVideoSelectionRule()?.message ?: ""

    fun complyRules() {
        val error = ruleAction.getFirstFailingMessage(
            Pair(
                listOfSelectedPhotos.size,
                listOfSelectedVideos.size
            )
        )
        if (error.isEmpty()) {
            onActionButtonClick()
            closeHostingViewLiveData.postValue(true)
        } else {
            errorStateLiveData.postValue(error)
        }
    }

}
