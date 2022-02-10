package com.michael.camerax

import android.annotation.SuppressLint
import android.util.Log
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.protobuf.ByteString
import io.grpc.ConnectivityState
import io.grpc.ManagedChannel
import io.grpc.ManagedChannelBuilder
import kotlinx.coroutines.*
import java.net.Inet4Address
import java.net.InetAddress
import java.net.NetworkInterface
import java.net.SocketException
import java.util.*
import java.util.concurrent.TimeUnit

fun getLocalIpAddress(): String? {
    try {
        val en: Enumeration<NetworkInterface> = NetworkInterface.getNetworkInterfaces()
        while (en.hasMoreElements()) {
            val intf: NetworkInterface = en.nextElement()
            val enumIpAddr: Enumeration<InetAddress> = intf.inetAddresses
            while (enumIpAddr.hasMoreElements()) {
                val inetAddress: InetAddress = enumIpAddr.nextElement()
                if (!inetAddress.isLoopbackAddress && inetAddress is Inet4Address) {
                    return inetAddress.hostAddress
                }
            }
        }
    } catch (ex: SocketException) {
        ex.printStackTrace()
    }
    return null
}

class ImageAnalyzer : ImageAnalysis.Analyzer {
    private var serverIp: String? = "192.168.200.55"
    private val port = 50051
    private var channel: ManagedChannel? = null
    private var client: ImageClient? = null

    init {
        initClient()
    }

    private fun initClient() {
        CoroutineScope(Dispatchers.IO).launch {
            runCatching {
                Log.d("serverIp", serverIp.toString())
                serverIp?.let {
                    channel = ManagedChannelBuilder.forAddress(it, port).usePlaintext().build()
                    channel?.let { ch ->
                        client = ImageClient(ch)
                    }
                }
            }
        }
    }

    fun destroyClient() {
        client = null
    }

    @SuppressLint("UnsafeOptInUsageError")
    override fun analyze(image: ImageProxy) {
        val imageBytes = ImageUtil.imageToByteArray(image.image)

        startUploadImageTask(imageBytes)

        image.close()
    }

    /**
    override fun analyze(image: ImageProxy) {
    val yBuffer = image.planes[0].buffer // Y
    val vuBuffer = image.planes[2].buffer // VU

    val ySize = yBuffer.remaining()
    val vuSize = vuBuffer.remaining()

    val nv21 = ByteArray(ySize + vuSize)

    yBuffer.get(nv21, 0, ySize)
    vuBuffer.get(nv21, ySize, vuSize)

    val yuvImage = YuvImage(nv21, ImageFormat.NV21, image.width, image.height, null)
    val out = ByteArrayOutputStream()
    yuvImage.compressToJpeg(Rect(0, 0, yuvImage.width, yuvImage.height), 50, out)
    val imageBytes = out.toByteArray()

    startExportMapTask(imageBytes)

    image.close()
    } */

    private fun startUploadImageTask(byteArray: ByteArray) {
        CoroutineScope(Dispatchers.Default).launch {
            try {
                doUploadImageTask(byteArray)
            } catch (e: Exception) {
                Log.e("doUploadImageTask", "$e")
            }
        }
    }

    private suspend fun doUploadImageTask(byteArray: ByteArray) {
        withContext(Dispatchers.Default) {
            Log.d(
                "doUploadImageTask",
                "${coroutineContext[CoroutineName]} is executing on thread : ${Thread.currentThread().name}"
            )

            client?.let {
            } ?: initClient()

            client?.let {
                if (channel?.getState(true) == ConnectivityState.READY) {
                    it.uploadImage(it.generateImageStream(ByteString.copyFrom(byteArray)))
                }
            }
        }
    }
}
