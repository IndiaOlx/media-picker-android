package com.mediapicker.gallery.presentation.adapters

import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import com.mediapicker.gallery.Gallery
import com.mediapicker.gallery.R
import com.mediapicker.gallery.databinding.OssItemCameraSelectionBinding
import com.mediapicker.gallery.databinding.OssItemPhotoSelectionBinding
import com.mediapicker.gallery.databinding.OssItemVideoSelectionBinding
import com.mediapicker.gallery.domain.entity.*
import com.mediapicker.gallery.util.AnimationHelper
import java.io.File


class SelectPhotoImageAdapter(
    private var listOfGalleryItems: List<IGalleryItem>,
    var listCurrentPhotos: List<PhotoFile>,
    private val onGalleryItemClickListener: IGalleryItemClickListener,
    private val fromGallery: Boolean = true
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        const val ITEM_TYPE_PHOTO = 0
        const val ITEM_TYPE_CAMERA = 1
        const val ITEM_TYPE_ALBUM = 2
    }

    fun updateGalleryItems(itemList: List<IGalleryItem>) {
        this.listOfGalleryItems = itemList
        notifyDataSetChanged()
    }

    override fun getItemViewType(position: Int): Int {
        val item = listOfGalleryItems[position]
        return if (item is PhotoFile) ITEM_TYPE_PHOTO else if (item is PhotoAlbum) ITEM_TYPE_ALBUM else ITEM_TYPE_CAMERA
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == ITEM_TYPE_CAMERA || viewType == ITEM_TYPE_ALBUM) {
            val binding: OssItemCameraSelectionBinding = OssItemCameraSelectionBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            CameraViewHolder(binding)
        } else {
            val binding: OssItemPhotoSelectionBinding = OssItemPhotoSelectionBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            PhotoViewHolder(binding)
        }
    }

    override fun getItemCount() = listOfGalleryItems.size

    private fun getPosition(photo: PhotoFile): Int {
        var i = 0
        while (i < listCurrentPhotos.size) {
            if (listCurrentPhotos[i] == photo) {
                return ++i
            }
            i++
        }
        return 0
    }

    override fun onBindViewHolder(viewHolder: RecyclerView.ViewHolder, position: Int) {
        when (viewHolder.itemViewType) {
            ITEM_TYPE_CAMERA -> {
                val cameraViewHolder = viewHolder as CameraViewHolder
                cameraViewHolder.itemView.setOnClickListener { v -> onClickCamera() }
                cameraViewHolder.binding.folderName.isAllCaps = Gallery.galleryConfig.textAllCaps
                cameraViewHolder.binding.folderName.text =
                    viewHolder.itemView.context.getString(R.string.oss_label_camera)
            }
            ITEM_TYPE_ALBUM -> {
                val cameraViewHolder = viewHolder as CameraViewHolder

                cameraViewHolder.itemView.setOnClickListener { v -> onGalleryItemClickListener.onFolderItemClick() }
                cameraViewHolder.binding.folderName.isAllCaps = Gallery.galleryConfig.textAllCaps
                cameraViewHolder.binding.folderName.text =
                    viewHolder.itemView.context.getString(R.string.oss_label_folder)
                cameraViewHolder.binding.img.setImageResource(R.drawable.oss_media_ic_folder_icon)
            }
            else -> {
                val photoViewHolder = viewHolder as PhotoViewHolder
                photoViewHolder.photoFile = listOfGalleryItems[position] as PhotoFile
                photoViewHolder.binding.imgCoverText.visibility = View.GONE
                if (listCurrentPhotos.contains(photoViewHolder.photoFile)) {
                    photoViewHolder.binding.whiteOverlay.visibility = View.VISIBLE
                    photoViewHolder.binding.imgSelectedText.text =
                        getPosition(photoViewHolder.photoFile).toString()
                    photoViewHolder.binding.imgSelectedText.background =
                        photoViewHolder.itemView.context
                            .resources.getDrawable(R.drawable.oss_circle_photo_indicator_selected)
                    photoViewHolder.itemView.scaleX = AnimationHelper.SELECTED_SCALE
                    photoViewHolder.itemView.scaleY = AnimationHelper.SELECTED_SCALE
                    setSelectedPhoto(photoViewHolder)
                } else {
                    photoViewHolder.binding.imgSelectedText.text = ""
                    photoViewHolder.binding.imgSelectedText.background =
                        photoViewHolder.itemView.context
                            .resources.getDrawable(R.drawable.oss_circle_photo_indicator)
                    photoViewHolder.binding.whiteOverlay.visibility = View.GONE
                    photoViewHolder.itemView.scaleX = AnimationHelper.UNSELECTED_SCALE
                    photoViewHolder.itemView.scaleY = AnimationHelper.UNSELECTED_SCALE

                }
                if (photoViewHolder.photoFile.imageId != photoViewHolder.itemView.tag) {
                    loadImageIntoView(
                        photoViewHolder.photoFile,
                        photoViewHolder.binding.cropedImage
                    )
                    photoViewHolder.itemView.tag = photoViewHolder.photoFile.imageId
                }
                photoViewHolder.itemView.setOnClickListener { v ->
                    val path = photoViewHolder.photoFile.path
                    val imageId = photoViewHolder.photoFile.imageId
                    val fullPhotoUrl = photoViewHolder.photoFile.fullPhotoUrl
                    val photo = PhotoFile.Builder()
                        .imageId(imageId)
                        .path(path!!)
                        .smallPhotoUrl("")
                        .fullPhotoUrl(fullPhotoUrl!!)
                        .photoBackendId(0L)
                        .action(Action.ADD)
                        .status(Status.PENDING)
                        .build()
                    trackSelectPhotos()
                    handleItemClick(photo, position)
                }
            }
        }
    }

    private fun setSelectedPhoto(photoViewHolder: PhotoViewHolder) {
        if (Gallery.galleryConfig.photoTag.shouldShowPhotoTag) {
            photoViewHolder.binding.imgCoverText.visibility = View.VISIBLE
            photoViewHolder.binding.imgCoverText.text = Gallery.galleryConfig.photoTag.photoTagText
        } else if (listCurrentPhotos.indexOf(photoViewHolder.photoFile) == 0 && Gallery.galleryConfig.needToShowCover.shouldShowPhotoTag) {
            photoViewHolder.binding.imgCoverText.visibility = View.VISIBLE
            photoViewHolder.binding.imgCoverText.text = Gallery.galleryConfig.needToShowCover.photoTagText
        } else {
            photoViewHolder.binding.imgCoverText.visibility = View.GONE
        }
    }

    private fun trackSelectPhotos() {
    }

    private fun handleItemClick(photoFile: PhotoFile, position: Int) {
        onGalleryItemClickListener.onPhotoItemClick(photoFile, position)
    }

    private fun onClickCamera() {
        onGalleryItemClickListener.onCameraIconClick()
    }


    private fun loadImageIntoView(photoFile: PhotoFile, imageView: ImageView) {
        val options = RequestOptions()
            .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
            .fitCenter()
        if (photoFile.isAlreadyUploaded) {
            Glide.with(imageView.context)
                .load(photoFile.fullPhotoUrl)
                .apply(options)
                .into(imageView)
        } else if (!photoFile.path.isNullOrEmpty()) {
            Glide.with(imageView.context)
                .load(Uri.fromFile(File(photoFile.path!!)))
                .thumbnail(0.1f)
                .apply(options)
                .into(imageView)
        }
    }
}


internal class CameraViewHolder(var binding: OssItemCameraSelectionBinding) : RecyclerView.ViewHolder(binding.root) {
}

internal class PhotoViewHolder(var binding: OssItemPhotoSelectionBinding) : RecyclerView.ViewHolder(binding.root) {
    lateinit var photoFile: PhotoFile

}

interface IGalleryItemClickListener {
    fun onPhotoItemClick(photoFile: PhotoFile, position: Int)
    fun onFolderItemClick()
    fun onCameraIconClick()
}

