package com.mediapicker.gallery.domain.entity

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.mediapicker.gallery.R

data class CarousalConfig(
    val showCarousal: Boolean = false,
    @DrawableRes val imageId: Int = R.drawable.ic_no_images_black_48dp,
    val addImage: Boolean = false,
    @StringRes val previewText: Int = R.string.preview
)