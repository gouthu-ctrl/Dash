package com.dash.travel.di

import android.content.Context
import android.util.Log
import androidx.room.Room
import com.dash.travel.data.local.DashDatabase
import com.dash.travel.data.remote.SupabaseManager
import com.dash.travel.data.repository.*
import com.dash.travel.data.search.LocalSearchService

/**
 * Simple manual Dependency Injection container
 */
object DiContainer {
    
    // Context must be set before accessing Room-based dependencies
    private var appContext: Context? = null
    
    val isInitialized: Boolean get() = appContext != null
    
    fun initialize(context: Context) {
        if (appContext == null) {
            appContext = context.applicationContext
            Log.d("DiContainer", "Initialized with context")
        }
    }
    
    // Room Database (lazy initialization with null-safety)
    val database: DashDatabase by lazy {
        val ctx = requireNotNull(appContext) { 
            "DiContainer.initialize(context) must be called before accessing database" 
        }
        Room.databaseBuilder(
            ctx,
            DashDatabase::class.java, 
            "dash_database"
        ).fallbackToDestructiveMigration().build()
    }
    
    // Room DAOs
    val recentLocationsDao by lazy { database.recentLocationsDao() }
    val tripDao by lazy { database.tripDao() }
    val itineraryDao by lazy { database.itineraryDao() }
    
    // Local Search Service
    val localSearchService by lazy { 
        LocalSearchService(requireNotNull(appContext) { 
            "DiContainer.initialize(context) must be called before accessing localSearchService" 
        })
    }
    
    // Supabase Repositories
    val tripRepository by lazy { 
        TripRepository(SupabaseManager.client) 
    }
    
    val documentRepository by lazy { 
        DocumentRepository(SupabaseManager.client) 
    }
    
    val priceAlertRepository by lazy { 
        PriceAlertRepository(SupabaseManager.client) 
    }
    
    val aiRepository by lazy { 
        AIRepository(SupabaseManager.client) 
    }
    
    val profileRepository by lazy { 
        ProfileRepository(SupabaseManager.client) 
    }

    val settingsRepository by lazy {
        SettingsRepository(requireNotNull(appContext))
    }
}
