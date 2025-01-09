package com.example.testapplication

import com.example.testapplication.Model.AuthUser
import com.example.testapplication.Model.Conversations
import com.example.testapplication.Model.UserAuthToken
import com.example.testapplication.Model.UserDataAuth
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.QueryMap

interface ApiInterface {
    @POST("v3/users/{id}/auth_tokens")
    fun createAuthToken(@Path("id") id: String): Call<UserAuthToken>

    @POST("v3/users")
    fun createUserWithAuth(@Body user: UserDataAuth) : Call<AuthUser>

    @GET("v3/conversations")
    fun getConversations() : Call<List<Conversations>>

    @GET("v3/users")
    fun getUsers() : Call<List<Conversations>>

    @GET("v3/groups")
    fun getGroups() : Call<List<Conversations>>

    @GET("v3.0/calls")
    fun getCalls() : Call<List<Conversations>>

    @GET("v3/users/{uid}")
    fun getUserDetails(@Path("uid") uid : String) : Call<List<Conversations>>

    @DELETE("v3/conversations/{conversationId}")
    fun deleteConvo(@Path("conversationId") conversationId : String) : Call<List<Conversations>>

    @GET("v3/messages")
    fun getConversationMessages(@QueryMap params: Map<String, String>) : Call<List<Conversations>>

    @POST("v3/messages")
    fun sendMessage(@Body message: RequestBody): Call<List<Conversations>>

    @POST("v3/groups")
    fun createGroup(@Body message: RequestBody) : Call<List<Conversations>>

    @POST("v3/users/{uid}/blockedusers")
    fun blockUsers(@Path("uid") uid : String, @Body message: RequestBody) : Call<List<Conversations>>

    @POST("v3/groups/{guid}/members")
    fun createAuthToken(@Path("guid") guid: String, @Body message: RequestBody): Call<List<Conversations>>


}