package com.udacity.project4.authentication


import androidx.lifecycle.ViewModel
import androidx.lifecycle.map

class AuthenticationViewModel : ViewModel() {



    val authenticationState = FirebaseUserLiveData().map { user ->
        if (user != null) {
            FirebaseUserLiveData.AuthenticationState.AUTHENTICATED
        }
        else {
            FirebaseUserLiveData.AuthenticationState.UNAUTHENTICATED
        }
    }

}