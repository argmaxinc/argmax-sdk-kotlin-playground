//  For licensing see accompanying LICENSE.md file.
//  Copyright © 2025 Argmax, Inc. All rights reserved.

package com.argmaxinc.playground.viewmodel

import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModel
import com.argmaxinc.playground.view.Screen
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

/**
 * Owns the navigation back stack for the Navigation3-based UI.
 */
@HiltViewModel
class NavigationViewModel
    @Inject
    constructor() : ViewModel() {
        /**
         * Mutable back stack of [Screen] destinations, starting at [Screen.Home].
         */
        val backStack = mutableStateListOf<Screen>(Screen.Home)

        /**
         * Push a new [screen] destination onto the back stack.
         */
        fun navigateTo(screen: Screen) {
            backStack.add(screen)
        }

        /**
         * Pop the current destination if possible.
         */
        fun pop() {
            if (backStack.size > 1) {
                backStack.removeAt(backStack.lastIndex)
            }
        }
    }
