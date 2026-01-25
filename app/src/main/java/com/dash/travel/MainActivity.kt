package com.dash.travel

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.dash.travel.ui.screens.HomeScreen
import com.dash.travel.ui.screens.WelcomeScreen
import com.dash.travel.ui.theme.DashTheme
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.Google
import io.github.jan.supabase.auth.providers.builtin.IDToken
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.exceptions.RestException
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.plugins.ResponseException
import io.ktor.client.plugins.ServerResponseException
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Initialize Supabase Client
        val supabase = createSupabaseClient(
            supabaseUrl = BuildConfig.SUPABASE_URL,
            supabaseKey = BuildConfig.SUPABASE_KEY
        ) {
            install(Auth)
            install(Postgrest)
        }

        setContent {
            DashTheme {
                val navController = rememberNavController()
                val coroutineScope = rememberCoroutineScope()
                val context = this

                // Check if user is already logged in
                LaunchedEffect(Unit) {
                    val session = supabase.auth.currentSessionOrNull()
                    if (session != null) {
                        launch { updateProfile(supabase) }
                        navController.navigate("home") {
                            popUpTo("welcome") { inclusive = true }
                        }
                    }
                }

                NavHost(navController = navController, startDestination = "welcome") {
                    composable("welcome") {
                        WelcomeScreen(
                            onGoogleLoginClicked = {
                                val googleAvailability = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(context)
                                if (googleAvailability != ConnectionResult.SUCCESS) {
                                    Toast.makeText(context, "Google Play Services not available", Toast.LENGTH_LONG).show()
                                    return@WelcomeScreen
                                }

                                coroutineScope.launch {
                                    try {
                                        val credentialManager = CredentialManager.create(context)
                                        
                                        // Use the Web Client ID from Google Cloud Console
                                        val googleIdOption = GetGoogleIdOption.Builder()
                                            .setFilterByAuthorizedAccounts(false)
                                            .setServerClientId("750370194796-e2187fr9ivo7selsn92nrrt316mclddf.apps.googleusercontent.com")
                                            .setAutoSelectEnabled(false)
                                            .build()

                                        val request = GetCredentialRequest.Builder()
                                            .addCredentialOption(googleIdOption)
                                            .build()

                                        val result = credentialManager.getCredential(
                                            request = request,
                                            context = context
                                        )
                                        
                                        val credential = result.credential
                                        if (credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                                            val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                                            val idToken = googleIdTokenCredential.idToken

                                            // Sign in to Supabase with the ID Token
                                            // Ensure nonce handling matches your Supabase config (skip_nonce_check=true)
                                            supabase.auth.signInWith(IDToken) {
                                                this.idToken = idToken
                                                this.provider = Google
                                            }

                                            // Update profile with location info
                                            launch { updateProfile(supabase) }

                                            // Navigate to Home
                                            navController.navigate("home") {
                                                popUpTo("welcome") { inclusive = true }
                                            }
                                        } else {
                                            Log.e("Auth", "Unexpected credential type: ${credential.type}")
                                            Toast.makeText(context, "Unexpected credential type", Toast.LENGTH_SHORT).show()
                                        }
                                    } catch (e: NoCredentialException) {
                                        Log.e("Auth", "No credential available", e)
                                        Toast.makeText(context, "No Google Accounts found. Please add one in Settings.", Toast.LENGTH_LONG).show()
                                    } catch (e: GetCredentialException) {
                                        Log.e("Auth", "GetCredentialException: ${e.message}", e)
                                        Toast.makeText(context, "Auth Error: ${e.message}", Toast.LENGTH_SHORT).show()
                                    } catch (e: RestException) {
                                        Log.e("Auth", "Supabase RestException: ${e.error}", e)
                                        Toast.makeText(context, "Supabase Error: ${e.description ?: e.error}", Toast.LENGTH_LONG).show()
                                    } catch (e: ClientRequestException) {
                                        val status = e.response.status.value
                                        val body = e.response.bodyAsText()
                                        Log.e("Auth", "Client Error $status: $body", e)
                                        Toast.makeText(context, "Client Error $status: $body", Toast.LENGTH_LONG).show()
                                    } catch (e: ServerResponseException) {
                                        val status = e.response.status.value
                                        val body = e.response.bodyAsText()
                                        Log.e("Auth", "Server Error $status: $body", e)
                                        Toast.makeText(context, "Server Error $status: $body", Toast.LENGTH_LONG).show()
                                    } catch (e: ResponseException) {
                                        val errorBody = e.response.bodyAsText()
                                        Log.e("Auth", "Server Error: ${e.response.status} Body: $errorBody", e)
                                        Toast.makeText(context, "Server Error: ${e.response.status} - $errorBody", Toast.LENGTH_LONG).show()
                                    } catch (e: HttpRequestTimeoutException) {
                                        Log.e("Auth", "Timeout", e)
                                        Toast.makeText(context, "Connection Timeout. Check your Supabase URL.", Toast.LENGTH_LONG).show()
                                    } catch (e: Exception) {
                                        Log.e("Auth", "Login failed: ${e.message}", e)
                                        Toast.makeText(context, "Login failed: ${e.message}", Toast.LENGTH_LONG).show()
                                    }
                                }
                            }
                        )
                    }
                    composable("home") {
                        HomeScreen()
                    }
                }
            }
        }
    }
}
