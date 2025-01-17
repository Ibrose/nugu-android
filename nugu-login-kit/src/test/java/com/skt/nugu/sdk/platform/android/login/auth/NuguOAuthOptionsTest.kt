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
package com.skt.nugu.sdk.platform.android.login.auth

import org.junit.Assert
import org.junit.Test
import java.lang.NullPointerException
import java.net.MalformedURLException

class NuguOAuthOptionsTest {
    @Test
    fun testNuguOAuthOptions() {
        val response1 = NuguOAuthOptions(
            grantType = NuguOAuthClient.GrantType.CLIENT_CREDENTIALS.value,
            clientId = "dummy_clientId",
            clientSecret = "dummy_clientSecret",
            redirectUri = "dummy_redirectUri",
            deviceUniqueId = "dummy_deviceUniqueId"
        )
        Assert.assertNotNull(response1)
        val response2 = NuguOAuthOptions.Builder()
            .grantType(NuguOAuthClient.GrantType.CLIENT_CREDENTIALS.value)
            .clientId("dummy_clientId")
            .clientSecret("dummy_clientSecret")
            .redirectUri("dummy_redirectUri")
            .deviceUniqueId("dummy_deviceUniqueId")
            .build()
        Assert.assertNotNull(response2)
        Assert.assertEquals(response1, response2)
    }

    @Test(expected = IllegalArgumentException::class)
    fun testRedirectUriIsNull() {
        val response2 = NuguOAuthOptions.Builder()
            .grantType(NuguOAuthOptions.AUTHORIZATION_CODE)
            .clientId("dummy_clientId")
            .clientSecret("dummy_clientSecret")
            .deviceUniqueId("dummy_deviceUniqueId")
            .build()
        Assert.assertNotNull(response2)
    }

}