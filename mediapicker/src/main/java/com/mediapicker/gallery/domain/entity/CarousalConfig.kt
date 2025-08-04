package com.mediapicker.gallery.domain.entity

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.mediapicker.gallery.R

data class CarousalConfig(
    val showCarousal: Boolean = false,
    @DrawableRes val imageId: Int = R.drawable.pic_default_photo,
    val addImage: Boolean = false,
    @StringRes val previewText: Int = R.string.gallery_view_default_photo_banner_text
)