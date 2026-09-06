package com.streamvault.app.di

import javax.inject.Qualifier

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class MainPlayerEngine

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class PreviewPlayerEngine

@javax.inject.Qualifier
@Retention(AnnotationRetention.RUNTIME)
annotation class AuxiliaryPlayerEngine
