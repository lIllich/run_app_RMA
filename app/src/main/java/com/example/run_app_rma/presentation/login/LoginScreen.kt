package com.example.run_app_rma.presentation.login

import androidx.compose.runtime.Composable
import com.example.run_app_rma.data.remote.AuthRepository
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(
    authRepository: AuthRepository,
    onLoginSuccess: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var message by remember { mutableStateOf<String?>(null) }
    var isLoginMode by remember { mutableStateOf(true) }
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = if (isLoginMode) "Login" else "Create Account",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") },
            singleLine = true
        )

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            singleLine = true
        )

        Button(
            onClick = {
                scope.launch {
                    val result = if (isLoginMode) {
                        authRepository.loginUser(email, password)
                    } else {
                        authRepository.registerUser(email, password)
                    }
                    if (result.isSuccess) {
                        onLoginSuccess()
                    } else {
                        message = result.exceptionOrNull()?.message ?: "Something went wrong!"
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        ) {
            Text(if (isLoginMode) "Login" else "Register")
        }

        TextButton(
            onClick = { isLoginMode = !isLoginMode },
            modifier = Modifier.align(Alignment.CenterHorizontally)
        ) {
            Text(
                if (isLoginMode) "Don't have an account? Register"
                else "Already have an account? Login"
            )
        }

        message?.let {
            Text(
                text = it,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}


/*
@Composable
fun LoginScreen(
    authRepository: AuthRepository,
    onLoginSuccess: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var message by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    Column(modifier = Modifier
        .fillMaxSize()
        .padding(16.dp),
        verticalArrangement = Arrangement.Center
    ) {
        OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("Email") })
        OutlinedTextField(value = password, onValueChange = { password = it }, label = { Text("Password") })

        Button(
            onClick = {
                scope.launch {
                    val result = authRepository.loginUser(email, password)
                    if (result.isSuccess) {
                        onLoginSuccess()  // Invoke the callback
                    } else {
                        message = result.exceptionOrNull()?.message ?: "Login failed"
                    }
                }
            },
            modifier = Modifier.padding(top = 8.dp)
        ) {
            Text("Login")
        }

        message?.let { Text(it, color = MaterialTheme.colorScheme.error) }
    }
}
*/