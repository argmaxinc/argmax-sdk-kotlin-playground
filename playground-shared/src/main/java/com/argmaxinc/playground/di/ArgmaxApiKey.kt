//  For licensing see accompanying LICENSE.md file.
//  Copyright © 2025 Argmax, Inc. All rights reserved.

package com.argmaxinc.playground.di

import javax.inject.Qualifier

/**
 * Qualifier for the resolved Argmax runtime API key value.
 *
 * This key is produced by dependency injection from app-provided key material and should be
 * consumed only where SDK licensing initialization is performed.
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class ArgmaxApiKey
