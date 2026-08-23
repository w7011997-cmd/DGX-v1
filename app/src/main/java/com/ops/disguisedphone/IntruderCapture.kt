package com.ops.disguisedphone

import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.provider.MediaStore
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Silently captures a front-camera photo (no preview shown) and saves it to
 * Pictures/Intruders. Requires CAMERA permission to already be granted --
 * this never prompts, since prompting mid-attempt would reveal the app to
 * whoever is holding the phone.
 */
object IntruderCapture {

    fun capture(context: Context, lifecycleOwner: LifecycleOwner) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        try {
            val providerFuture = ProcessCameraProvider.getInstance(context)
            providerFuture.addListener({
                try {
                    val provider = providerFuture.get()
                    val imageCapture = ImageCapture.Builder().build()
                    provider.unbindAll()
                    provider.bindToLifecycle(
                        lifecycleOwner,
                        CameraSelector.DEFAULT_FRONT_CAMERA,
                        imageCapture
                    )

                    val name = "intruder_" +
                        SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date()) + ".jpg"
                    val values = ContentValues().apply {
                        put(MediaStore.Images.Media.DISPLAY_NAME, name)
                        put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/Intruders")
                        }
                    }
                    val outputOptions = ImageCapture.OutputFileOptions.Builder(
                        context.contentResolver,
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                        values
                    ).build()

                    imageCapture.takePicture(
                        outputOptions,
                        ContextCompat.getMainExecutor(context),
                        object : ImageCapture.OnImageSavedCallback {
                            override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                                provider.unbindAll()
                            }
                            override fun onError(exc: ImageCaptureException) {
                                provider.unbindAll()
                            }
                        }
                    )
                } catch (e: Exception) {
                    // Must never crash or surface an error to whoever is holding the phone.
                }
            }, ContextCompat.getMainExecutor(context))
        } catch (e: Exception) {
            // Same: fail silently.
        }
    }
}
