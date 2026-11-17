package com.rajon.jarvis.di

import android.app.Application
import androidx.room.Room
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.rajon.jarvis.ai.provider.FailoverEngine
import com.rajon.jarvis.data.local.ChatMessageDao
import com.rajon.jarvis.data.local.JarvisDatabase
import com.rajon.jarvis.data.local.MemoryDao
import com.rajon.jarvis.data.local.ReminderDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideDatabase(app: Application): JarvisDatabase {
        return Room.databaseBuilder(
            app,
            JarvisDatabase::class.java,
            "jarvis_database"
        )
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    @Singleton
    fun provideChatMessageDao(db: JarvisDatabase): ChatMessageDao = db.chatMessageDao()

    @Provides
    @Singleton
    fun provideReminderDao(db: JarvisDatabase): ReminderDao = db.reminderDao()

    @Provides
    @Singleton
    fun provideMemoryDao(db: JarvisDatabase): MemoryDao = db.memoryDao()

    @Provides
    @Singleton
    fun provideGson(): Gson = GsonBuilder().create()

    @Provides
    @Singleton
    fun provideFailoverEngine(): FailoverEngine = FailoverEngine()
}
