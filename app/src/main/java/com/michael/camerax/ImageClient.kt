package com.michael.camerax

import android.util.Log
import com.google.protobuf.ByteString
import com.michael.grpc.image.ImageServiceGrpcKt
import com.michael.grpc.image.ImageUploadRequest
import com.michael.grpc.image.Metadata
import io.grpc.ManagedChannel
import io.grpc.StatusException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import java.util.*
import java.util.concurrent.RejectedExecutionException

class ImageClient(channel: ManagedChannel) {
    private val stub = ImageServiceGrpcKt.ImageServiceCoroutineStub(channel)

    fun generateImageStream(image: ByteString): Flow<ImageUploadRequest> = flow {
        val uniqueId: String = UUID.randomUUID().toString()
        val metadata: Metadata = Metadata.newBuilder().setId(uniqueId).setImageFormat("jpeg").build()

        var request = ImageUploadRequest.newBuilder().setMetadata(metadata).build()
        emit(request)
        request = ImageUploadRequest.newBuilder().setImage(image).build()
        emit(request)
    }
    suspend fun uploadImage(request: Flow<ImageUploadRequest>) {
        try {
            Log.d("uploadImage", request.toString())
            val response = stub.upload(request)
            println("Received response {id: ${response.first().id}, status: ${response.first().status}}")

        } catch (e: StatusException) {
            e.printStackTrace()
        } catch (e: RejectedExecutionException) {
            e.printStackTrace()
        }
    }
}