package com.xxcamixx.contabilidad.network

import retrofit2.http.*
import com.xxcamixx.contabilidad.model.CloudPayload
import com.xxcamixx.contabilidad.model.SyncResponse
import com.xxcamixx.contabilidad.model.UserSyncRequest
import com.xxcamixx.contabilidad.model.UserSyncResponse
import com.xxcamixx.contabilidad.model.UserTimeRequest
import com.xxcamixx.contabilidad.model.UserTimeResponse
import com.xxcamixx.contabilidad.model.UserData
import com.xxcamixx.contabilidad.model.UserManageRequest
import com.xxcamixx.contabilidad.model.ChatMessage
import com.xxcamixx.contabilidad.model.ChatSendRequest
import com.xxcamixx.contabilidad.model.BcvResponse

interface ServerApi {
    @POST("api/backup/{userId}") suspend fun uploadBackup(@Path("userId") userId: String, @Body data: CloudPayload): SyncResponse
    @GET("api/backup/{userId}") suspend fun getBackup(@Path("userId") userId: String): CloudPayload?
    @POST("api/users/sync") suspend fun syncUser(@Body request: UserSyncRequest): UserSyncResponse
    @POST("api/users/time") suspend fun addUserTime(@Body request: UserTimeRequest): UserTimeResponse
    @GET("api/users") suspend fun getAllUsers(): Map<String, UserData>
    @POST("api/users/manage") suspend fun manageUser(@Body request: UserManageRequest): SyncResponse
    @GET("api/chat/{email}") suspend fun getChat(@Path("email") email: String): List<ChatMessage>
    @GET("api/admin/chats") suspend fun getAllChats(): Map<String, List<ChatMessage>>
    @POST("api/chat") suspend fun sendMessage(@Body req: ChatSendRequest): SyncResponse
    @DELETE("api/chat/{email}") suspend fun clearChat(@Path("email") email: String): SyncResponse

    // NUEVO ENDPOINT BCV
    @GET("api/bcv") suspend fun getBcvRate(): BcvResponse
}
