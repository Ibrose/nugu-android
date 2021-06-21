/**
 * Copyright (c) 2021 SK Telecom Co., Ltd. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http:www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.skt.nugu.sdk.core.network

import com.skt.nugu.sdk.core.interfaces.transport.Transport
import com.skt.nugu.sdk.core.utils.Logger

class ServerInitiatedDirectiveController(val TAG: String) {
    private var isStart: Boolean = false
    private var completionListenerCalled = false
    private var initialized = false
    private var listener: (() -> Unit?)? = null

    fun notifyOnCompletionListener() {
        if (!completionListenerCalled && isStart) {
            listener?.invoke()
        }
        completionListenerCalled = true
    }

    fun setOnCompletionListener(onCompletionListener: () -> Unit) {
        listener = onCompletionListener
    }

    /**
     * Start the DirectivesService
     * @return true is success, otherwise false
     */
    fun start(transport: Transport?): Boolean {
        if (isStart) {
            Logger.w(TAG, "[start] ServerInitiatedDirective is already started.")
        }
        isStart = true
        completionListenerCalled = false
        return initialized.also { initialized ->
            if (initialized) {
                transport?.startDirectivesService() ?: Logger.w(
                    TAG,
                    "[start] activeTransport is not possible."
                )
            }
            this.initialized = true
        }
    }

    fun stop(transport: Transport?) {
        listener = null

        if (!isStart) {
            Logger.w(TAG, "[stop] ServerInitiatedDirective is already stopped. (isStart=$isStart)")
            return
        }
        isStart = false

        transport?.stopDirectivesService() ?: Logger.w(
            TAG,
            "[stop] activeTransport is not possible."
        )
    }

    fun release() {
        listener = null
        isStart = false
        completionListenerCalled = false
    }

    fun isStarted() = isStart
}