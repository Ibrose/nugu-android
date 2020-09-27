/**
 * Copyright (c) 2020 SK Telecom Co., Ltd. All rights reserved.
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

package com.skt.nugu.sdk.agent.ext.mediaplayer

import com.google.gson.Gson
import com.google.gson.JsonElement

/**
 * Playlist information.
 * If not supported, should be null.
 * When create event, exclude field not supported.
 */
data class Playlist(
    val type : Type?,
    val name: String,
    val number: String,
    val length: String,
    val currentSongOrder: String
) {
    enum class Type {
        CHART,
        PLAYLIST,
        THEMELIST,
        DEFAULT
    }
    fun toJson(): JsonElement = Gson().toJsonTree(this)
}