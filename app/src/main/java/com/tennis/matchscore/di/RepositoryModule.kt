package com.tennis.matchscore.di

import com.tennis.matchscore.data.repository.MatchRepositoryImpl
import com.tennis.matchscore.data.repository.PlayerRepositoryImpl
import com.tennis.matchscore.data.repository.MatchFormatRepositoryImpl
import com.tennis.matchscore.domain.repository.MatchRepository
import com.tennis.matchscore.domain.repository.PlayerRepository
import com.tennis.matchscore.domain.repository.MatchFormatRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindMatchRepository(
        matchRepositoryImpl: MatchRepositoryImpl
    ): MatchRepository

    @Binds
    @Singleton
    abstract fun bindPlayerRepository(
        playerRepositoryImpl: PlayerRepositoryImpl
    ): PlayerRepository

    @Binds
    @Singleton
    abstract fun bindMatchFormatRepository(
        matchFormatRepositoryImpl: MatchFormatRepositoryImpl
    ): MatchFormatRepository
}