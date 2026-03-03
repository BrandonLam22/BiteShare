package org.example.biteshare.app

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import org.example.biteshare.domain.Model
import org.example.biteshare.view.WelcomeView
import org.example.biteshare.view.LoginView
import org.example.biteshare.view.SignupView
import org.example.biteshare.viewmodel.WelcomeViewModel
import org.example.biteshare.viewmodel.LoginViewModel
import org.example.biteshare.viewmodel.SignupViewModel


private sealed class AuthScreen {
    data object Welcome : AuthScreen()
    data object Login : AuthScreen()
    data object Signup : AuthScreen()
    data object Home : AuthScreen()
}

@Composable
fun AuthGate() {
    val model = remember { Model() }
    var currentScreen by remember { mutableStateOf<AuthScreen>(AuthScreen.Welcome) }

    when (val screen = currentScreen) {
        is AuthScreen.Welcome -> {
            val welcomeVm = remember { WelcomeViewModel(model) }
            WelcomeView(
                viewModel = welcomeVm,
                onNavigateToLogin = { currentScreen = AuthScreen.Login },
                onNavigateToSignup = { currentScreen = AuthScreen.Signup }
            )
        }
        is AuthScreen.Login -> {
            val loginVm = remember { LoginViewModel(model) }
            LoginView(
                viewModel = loginVm,
                onLoginSuccess = { currentScreen = AuthScreen.Home },
                onNavigateToSignup = { currentScreen = AuthScreen.Signup }
            )
        }
        is AuthScreen.Signup -> {
            val signupVm = remember { SignupViewModel(model) }
            SignupView(
                viewModel = signupVm,
                onSignupSuccess = { currentScreen = AuthScreen.Home },
                onNavigateToLogin = { currentScreen = AuthScreen.Login }
            )
        }
        is AuthScreen.Home -> {
            AppRoot()
        }
    }
}