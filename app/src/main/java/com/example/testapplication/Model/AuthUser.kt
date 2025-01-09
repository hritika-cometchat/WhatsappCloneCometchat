package com.example.testapplication.Model

class AuthUser
{
    var data : UserDataAuth? = null
}

class UserDataAuth{
    var uid : String = ""
    var name : String = ""
    var withAuthToken : Boolean = false
    var createdAt : Long = 0L
    var authToken : String = ""
}