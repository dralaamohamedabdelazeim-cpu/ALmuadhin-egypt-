package com.example.almuadhin.di

import android.app.AlarmManager
import android.content.Context
import com.example.almuadhin.data.local.AppDatabase
import com.example.almuadhin.data.local.PrayerDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideAlarmManager(@ApplicationContext context: Context): AlarmManager {
        return context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    }
    
    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return AppDatabase.getInstance(context)
    }
    
    @Provides
    @Singleton
    fun providePrayerDao(database: AppDatabase): PrayerDao {
        return database.prayerDao()
    }

/*
    @Provides
    @Singleton
    fun provideNoorDao(database: AppDatabase): com.example.almuadhin.noor.data.local.NoorDao {
        return database.noorDao()
    }
*/
    @Provides
    @Singleton
    fun provideFirebaseAuth(): com.google.firebase.auth.FirebaseAuth {
        return com.google.firebase.auth.FirebaseAuth.getInstance()
    }

    @Provides
    @Singleton
    fun provideFirebaseDatabase(): com.google.firebase.database.FirebaseDatabase {
        val instance = com.google.firebase.database.FirebaseDatabase.getInstance()
        instance.setPersistenceEnabled(true) // Enable offline persistence
        return instance
    }
}

