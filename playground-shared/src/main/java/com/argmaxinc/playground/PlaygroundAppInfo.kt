//  For licensing see accompanying LICENSE.md file.
//  Copyright © 2025 Argmax, Inc. All rights reserved.

package com.argmaxinc.playground

/**
 * Immutable app-owned metadata shown inside the shared playground UI.
 *
 * The shared module does not read app-module `BuildConfig` directly so multiple apps can
 * reuse the same screens while still surfacing their own display name and version information.
 *
 * @property displayName Human-readable application name rendered in the shared header.
 * @property versionName User-facing semantic application version string.
 * @property versionCode Monotonic integer application version code.
 * @property sdkVersion User-facing SDK version string displayed alongside app metadata.
 */
data class PlaygroundAppInfo(
    val displayName: String,
    val versionName: String,
    val versionCode: Int,
    val sdkVersion: String,
)
