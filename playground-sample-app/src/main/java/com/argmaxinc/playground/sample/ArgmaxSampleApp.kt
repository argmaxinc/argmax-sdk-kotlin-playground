//  For licensing see accompanying LICENSE.md file.
//  Copyright © 2025 Argmax, Inc. All rights reserved.

package com.argmaxinc.playground.sample

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * Sample-shell [Application] used to bootstrap Hilt for the public-safe playground app.
 *
 * The shared playground logic lives in `:playground-shared`, while this shell owns only the
 * app-specific Android entrypoint and dependency bindings that should not be shared across apps.
 */
@HiltAndroidApp
class ArgmaxSampleApp : Application()
