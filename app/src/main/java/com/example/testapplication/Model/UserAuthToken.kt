package com.example.testapplication.Model

class UserAuthToken {
    var data : UserData? = null
}

class UserData{
    var uid = ""
    var authToken = ""
    var createdAt: Long? = null
}