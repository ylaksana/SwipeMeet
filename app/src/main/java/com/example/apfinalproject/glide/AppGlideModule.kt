package com.example.apfinalproject.glide

import android.content.Context
import android.net.Uri
import android.widget.ImageView
import com.bumptech.glide.Glide
import com.bumptech.glide.Registry
import com.bumptech.glide.annotation.GlideModule
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.module.AppGlideModule
import com.bumptech.glide.request.RequestOptions
import com.firebase.ui.storage.images.FirebaseImageLoader
import com.google.firebase.storage.StorageReference
import java.io.InputStream

@GlideModule
class AppGlideModule : AppGlideModule() {
    override fun registerComponents(
        context: Context,
        glide: Glide,
        registry: Registry,
    ) {
        // Register FirebaseImageLoader to handle StorageReference
        registry.append(
            StorageReference::class.java,
            InputStream::class.java,
            FirebaseImageLoader.Factory(),
        )
    }
}

object Glide {
    private var glideOptions =
        RequestOptions()
            // Options like CenterCrop are possible, but I like this one best
            // Evidently you need fitCenter or Transform.  If you use centerCrop, your
            // list disappears.  I think that was an old bug.
            .centerCrop()
            // Rounded corners are so lovely.
            .transform(RoundedCorners(20))

    /**
     * Fetch an image and display it in an ImageView
     * @param imageSource: Any - Accepts either Uri or StorageReference
     * @param imageView: ImageView - The ImageView to display the image in
     */
    fun fetch(
        imageSource: Any,
        imageView: ImageView,
    ) {
        // Layout engine does not know size of imageView
        // Hardcoding this here is a bad idea.  What would be better?

        val width = imageView.layoutParams.width
        val height = imageView.layoutParams.height
        GlideApp.with(imageView.context)
            .asBitmap() // Try to display animated Gifs and video still
            .load(
                when (imageSource) {
                    is StorageReference -> imageSource
                    is Uri -> imageSource
                    else -> throw IllegalArgumentException("Image source must be either StorageReference or Uri")
                },
            )
            .apply(glideOptions)
            .error(android.R.color.holo_red_dark)
            .override(width, height)
            .into(imageView)
    }
}
