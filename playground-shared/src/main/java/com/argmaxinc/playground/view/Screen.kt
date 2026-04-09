//  For licensing see accompanying LICENSE.md file.
//  Copyright © 2025 Argmax, Inc. All rights reserved.

package com.argmaxinc.playground.view

import androidx.navigation3.runtime.NavKey

/**
 * Navigation destinations for the app.
 */
sealed interface Screen : NavKey {
    /** Home screen with model selection and navigation buttons. */
    data object Home : Screen

    /** Screen that handles recording/file transcription. */
    data object Transcribe : Screen

    /** Screen that handles live streaming transcription. */
    data object Stream : Screen
}
