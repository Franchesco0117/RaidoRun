package com.francisco.raidorun

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import com.francisco.raidorun.databinding.ActivityCameraBinding
import java.io.File
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Color
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.util.DisplayMetrics
import android.util.Log
import android.webkit.MimeTypeMap
import android.widget.Toast
import androidx.camera.core.AspectRatio
import androidx.camera.core.CameraInfoUnavailableException
import androidx.camera.core.ImageCaptureException
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.net.toFile
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.francisco.raidorun.LoginActivity.Companion.userEmail
import com.google.android.material.snackbar.Snackbar
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * CameraActivity handles capturing photos using the device's camera. It provides functionality to switch
 * between front and back cameras, capture images, and save them with a timestamp-based filename.
 */
class CameraActivity : AppCompatActivity() {

    companion object {
        private val REQUIRED_PERMISSION = arrayOf(Manifest.permission.CAMERA)
        private val REQUEST_CODE_PERMISSION = 10

        private const val RATIO_4_3_VALUE = 4.0 / 3.0
        private const val RATIO_16_9_VALUE = 16.0 / 9.0
    }

    lateinit var binding : ActivityCameraBinding

    private var preview: Preview? = null

    private var lensFacing: Int = CameraSelector.LENS_FACING_BACK
    private var cameraProvider: ProcessCameraProvider? = null

    private var imageCapture: ImageCapture? = null

    private lateinit var outPutDirectory: File
    private lateinit var cameraExecutor: ExecutorService

    private lateinit var dateRun: String
    private lateinit var startTimeRun: String

    private var FILE_NAME: String = ""

    /**
     * CameraActivity
     *
     * This activity manages the process of taking a photo using the device's camera.
     * It is responsible for:
     * - Initializing and opening the camera.
     * - Displaying a camera preview on screen.
     * - Capturing the image and saving it temporarily.
     * - Returning the captured image back to the calling activity.
     *
     * Common use cases include profile picture updates or exercise evidence uploads.
     *
     * Permissions required:
     * - CAMERA
     * - WRITE_EXTERNAL_STORAGE (for older devices or if saving externally)
     *
     * Make sure to:
     * - Declare this activity in AndroidManifest.xml.
     * - Handle runtime permissions for Android 6.0 (API 23) and above.
     *
     * Author: [Francisco Castro]
     * Created: [21/MAY/2025]
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCameraBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val bundle = intent.extras
        dateRun = bundle?.getString("dateRun").toString()
        startTimeRun = bundle?.getString("startTimeRun").toString()

        outPutDirectory = getOutPutDirectory()

        cameraExecutor = Executors.newSingleThreadExecutor()

        binding.cameraCaptureButton.setOnClickListener {
            takePhoto()
        }

        binding.cameraSwitchButton.setOnClickListener {
            lensFacing = if (CameraSelector.LENS_FACING_FRONT == lensFacing) {
                CameraSelector.LENS_FACING_BACK
            } else {
                CameraSelector.LENS_FACING_FRONT
            }

            bindCamera()
        }

        if (allPermissionsGranted()) {
            startCamera()
        } else {
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSION, REQUEST_CODE_PERMISSION)
        }

    }

    /**
     * Handles the result of runtime permission requests.
     * If all required permissions are granted, it proceeds to start the camera.
     * Otherwise, it shows a message and closes the activity.
     *
     * @param requestCode The request code passed in requestPermissions().
     * @param permissions The requested permissions.
     * @param grantResults The grant results for the corresponding permissions.
     */
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == REQUEST_CODE_PERMISSION) {
            if (allPermissionsGranted()) {
                startCamera()
            } else {
                Toast.makeText(this, R.string.permissions_required, Toast.LENGTH_LONG).show()
                finish()
            }
        }
    }

    /**
     * Checks if all required permissions have been granted.
     */
    private fun allPermissionsGranted() = REQUIRED_PERMISSION.all {
        ContextCompat.checkSelfPermission(baseContext, it) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Starts the camera by binding it to the lifecycle and selecting the proper lens.
     */
    private fun startCamera() {
        val cameraProviderFinally = ProcessCameraProvider.getInstance(this)
        cameraProviderFinally.addListener(Runnable {

            cameraProvider = cameraProviderFinally.get()
            lensFacing = when {
                hasBackCamera() -> CameraSelector.LENS_FACING_BACK
                hasFrontCamera() -> CameraSelector.LENS_FACING_FRONT
                else -> throw IllegalStateException("You have no camera")
            }

            manageSwitchButton()

            bindCamera()

        }, ContextCompat.getMainExecutor(this))
    }

    /**
     * Enables or disables the camera switch button based on available cameras.
     */
    private fun manageSwitchButton() {
        val switchButton = binding.cameraSwitchButton
        try {
            switchButton.isEnabled = hasBackCamera() && hasFrontCamera()
        } catch (e: CameraInfoUnavailableException) {
            Log.e("CameraConnection", "Camera info is unavailable", e)
            switchButton.isEnabled = false
        }
    }

    /**
     * Checks if the device has a back camera.
     */
    private fun hasBackCamera(): Boolean {
        return cameraProvider?.hasCamera(CameraSelector.DEFAULT_BACK_CAMERA) ?: false
    }

    /**
     * Checks if the device has a front camera.
     */
    private fun hasFrontCamera(): Boolean {
        return cameraProvider?.hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA) ?: false
    }

    /**
     * Binds the selected camera to the lifecycle and prepares the preview and image capture use cases.
     */
    private fun bindCamera() {
        val metrics = DisplayMetrics().also {
            binding.viewFinder.display.getRealMetrics(it)
        }

        val screenAspectRatio = aspectRadio(metrics.widthPixels, metrics.heightPixels)
        val rotation = binding.viewFinder.display.rotation

        val cameraProvider = cameraProvider ?: throw IllegalStateException("Failed to bind camera")

        val cameraSelector = CameraSelector.Builder().requireLensFacing(lensFacing).build()

        preview = Preview.Builder()
            .setTargetAspectRatio(screenAspectRatio)
            .setTargetRotation(rotation)
            .build()

        imageCapture = ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
            .setTargetAspectRatio(screenAspectRatio)
            .setTargetRotation(rotation)
            .build()

        cameraProvider.unbindAll()

        try {
            cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture)
            preview?.setSurfaceProvider(binding.viewFinder.surfaceProvider)
        } catch (exc: Exception) {
            Log.e("CameraRaidoRun", "Failed to bind camera", exc)
        }
    }

    /**
     * Calculates and returns the closest standard camera aspect ratio (either 4:3 or 16:9)
     * based on the given width and height of the screen.
     *
     * This method helps in selecting the best-suited aspect ratio for the camera preview
     * by comparing the actual preview ratio to standard values.
     *
     * @param width The width of the screen or view.
     * @param height The height of the screen or view.
     * @return [AspectRatio.RATIO_4_3] if the preview ratio is closer to 4:3,
     *         otherwise [AspectRatio.RATIO_16_9].
     */
    private fun aspectRadio(width: Int, height: Int): Int {
        val previewRatio = max(width, height).toDouble() / min(width, height)

        if (abs(previewRatio - RATIO_4_3_VALUE) <= abs(previewRatio - RATIO_16_9_VALUE)) {
            return AspectRatio.RATIO_4_3
        }

        return AspectRatio.RATIO_16_9
    }

    /**
     * Returns the output directory for saved images.
     * Falls back to internal storage if external directory is unavailable.
     */
    private fun getOutPutDirectory(): File {
        val mediaDirectory = externalMediaDirs.firstOrNull()?.let {
            File(it, "RaidoRun").apply {
                mkdirs()
            }
        }

        return if (mediaDirectory != null && mediaDirectory.exists()) mediaDirectory else filesDir
    }

    /**
     * Captures a photo, saves it to a file, updates the media scanner, and shows a confirmation Snackbar.
     */
    private fun takePhoto() {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss_SSS", Locale.getDefault()).format(Date())
        var fileName = getString(R.string.app_name) + "_" + timeStamp

        // Limpiar caracteres especiales
        fileName = fileName.replace(":", "")
        fileName = fileName.replace("/", "")
        fileName = fileName.replace(" ", "_")

        val photoFile = File(outPutDirectory, fileName + ".jpg")
        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

        imageCapture?.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(this),
            object: ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {

                    val saveUri = Uri.fromFile(photoFile)

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        setGalleryThumbnail(saveUri)
                    }

                    val mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(saveUri.toFile().extension)
                    MediaScannerConnection.scanFile(
                        baseContext,
                        arrayOf(saveUri.toFile().absolutePath),
                        arrayOf(mimeType)
                    ) { _, uri ->

                    }

                    val clMain = findViewById<ConstraintLayout>(R.id.clMain)
                    Snackbar.make(clMain, "${R.string.image} $fileName ${R.string.saved}", Snackbar.LENGTH_LONG).setAction(R.string.ok) {
                        clMain.setBackgroundColor(Color.CYAN)
                    }.show()
                }

                override fun onError(exception: ImageCaptureException) {
                    val clMain = findViewById<ConstraintLayout>(R.id.clMain)
                    Snackbar.make(clMain, "${R.string.error_saving_image} ${exception.message}", Snackbar.LENGTH_LONG).setAction(R.string.ok) {
                        clMain.setBackgroundColor(Color.CYAN)
                    }.show()
                    Log.e("TakePhoto", "Error saving image", exception)
                }
            })
    }

    /**
     * Loads and displays the most recently taken photo as a circular thumbnail
     * in the photo view button using Glide.
     *
     * @param uri The [Uri] of the image to be displayed as a thumbnail.
     */
    private fun setGalleryThumbnail(uri: Uri) {
        var thumbnail = binding.photoViewButton
        thumbnail.post {
            Glide.with(thumbnail)
                .load(uri)
                .apply(RequestOptions.circleCropTransform())
                .into(thumbnail)
        }
    }
}