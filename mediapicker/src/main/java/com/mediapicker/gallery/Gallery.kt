package com.mediapicker.gallery

import com.mediapicker.gallery.domain.contract.GalleryPagerCommunicator
import com.mediapicker.gallery.domain.contract.IGalleryCommunicator
import com.mediapicker.gallery.presentation.carousalview.CarousalActionListener

object Gallery {
    internal var galleryConfig: GalleryConfig? = null

    internal var pagerCommunicator: GalleryPagerCommunicator? = null

    internal var carousalActionListener: CarousalActionListener? = null

    fun init(galleryConfig: GalleryConfig) {
        this.galleryConfig = galleryConfig
    }

    fun updateCommunicator(galleryCommunicator: IGalleryCommunicator?) {
        galleryConfig?.galleryCommunicator = galleryCommunicator
    }

//    internal fun getApp() = galleryConfig.applicationContext

    internal fun getClientAuthority(): String = galleryConfig?.clientAuthority ?: ""

    internal fun isGalleryConfigInitialized() = this::galleryConfig != null

    fun clean() {
        pagerCommunicator = null
        carousalActionListener = null

        if (galleryConfig != null) {
            galleryConfig?.galleryCommunicator = null
        }
    }

}
