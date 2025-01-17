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
package com.skt.nugu.sdk.client.port.transport.grpc2

import org.junit.Assert.*

import junit.framework.TestCase
import org.junit.Test

class NuguServerInfoTest {
    @Test
    fun testNuguServerInfo() {
        val info = NuguServerInfo.Builder().deviceGW(host = "deviceGW.sk.com", 44)
            .registry(host = "registry.sk.com", 443)
            .build()
        assertNotNull(info.toString())
    }

    @Test
    fun testDelegate() {
        val info = NuguServerInfo.Builder().deviceGW(url = "deviceGW.sk.com")
            .registry(url = "registry.sk.com")
            .build()
        val delegate = object : NuguServerInfo.Delegate {
            override val serverInfo: NuguServerInfo
                get() = info

        }
        assertNotNull(delegate.serverInfo)
        assertEquals(info, delegate.serverInfo)
    }
}