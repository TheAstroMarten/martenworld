package com.martenworld.app.di

import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * Hilt module placeholder. Everything so far is constructor-injected, so this exists
 * to anchor future bindings (interfaces, qualifiers) in one place.
 */
@Module
@InstallIn(SingletonComponent::class)
object AppModule
