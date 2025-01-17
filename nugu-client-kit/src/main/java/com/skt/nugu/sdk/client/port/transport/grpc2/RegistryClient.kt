/**
 * Copyright (c) 2019 SK Telecom Co., Ltd. All rights reserved.
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

import com.google.common.annotations.VisibleForTesting
import com.google.gson.*
import com.skt.nugu.sdk.core.interfaces.auth.AuthDelegate
import com.skt.nugu.sdk.core.interfaces.connection.ConnectionStatusListener.ChangedReason
import com.skt.nugu.sdk.core.interfaces.transport.DnsLookup
import com.skt.nugu.sdk.core.utils.Logger
import com.skt.nugu.sdk.core.utils.UserAgent
import com.squareup.okhttp.*
import java.util.concurrent.atomic.AtomicBoolean
import java.io.IOException
import java.net.*
import javax.net.ssl.SSLHandshakeException

/**
 *  Implementation of registry
 **/
internal class RegistryClient(
    private val dnsLookup: DnsLookup?
)  {
    companion object {
        private const val TAG = "RegistryClient"
        var cachedPolicy: Policy? = null
        const val GRPC_PROTOCOL = "H2_GRPC"
        const val HTTPS_SCHEME = "https"
        const val APPLICATION_JSON = "application/json"

        fun DefaultPolicy(serverInfo: NuguServerInfo) : Policy {
            return Policy(
                healthCheckPolicy = HealthCheckPolicy(
                    ttl = 0,
                    ttlMax = 0,
                    beta = 0F,
                    retryCountLimit = 0,
                    retryDelay = 0,
                    healthCheckTimeout = 0,
                    accumulationTime = 0
                ),
                serverPolicy = listOf(
                    ServerPolicy(
                        protocol = GRPC_PROTOCOL,
                        hostname = serverInfo.deviceGW.host,
                        port = serverInfo.deviceGW.port,
                        retryCountLimit = 2,
                        connectionTimeout = 10,
                        charge = ""
                    )
                )
            )
        }

    }

    @VisibleForTesting
    internal val isShutdown = AtomicBoolean(false)

    interface Observer {
        fun onCompleted(policy: Policy?)
        fun onError(reason: ChangedReason)
    }

    fun getPolicy(serverInfo: NuguServerInfo, authDelegate: AuthDelegate, observer: Observer, isStartReceiveServerInitiatedDirective: () -> Boolean) {
        if (isShutdown.get()) {
            Logger.w(TAG, "[getPolicy] already shutdown")
            return
        }
        if(!isStartReceiveServerInitiatedDirective()) {
            Logger.d(TAG,"[getPolicy] Skip getPolicy call because keepConnection is false.")
            notifyPolicy(DefaultPolicy(serverInfo), observer)
            return
        }

        val client = OkHttpClient().apply {
            protocols = listOf(com.squareup.okhttp.Protocol.HTTP_1_1)
            connectionPool = ConnectionPool(0, 1)
        }
        dnsLookup?.let { customDns ->
            client.setDns { hostname -> customDns.lookup(hostname.toString()).toMutableList() }
        }
        val httpUrl = try {
            HttpUrl.Builder()
                .scheme(HTTPS_SCHEME)
                .host(serverInfo.registry.host)
                .port(serverInfo.registry.port)
                .addPathSegment("v1")
                .addPathSegment("policies")
                .addQueryParameter("protocol", GRPC_PROTOCOL)
                .build()
        } catch (th : Throwable) {
            val reason = ChangedReason.UNRECOVERABLE_ERROR
            reason.cause = th
            observer.onError(reason)
            return
        }

        val request = Request.Builder().url(httpUrl)
            .header("Accept", APPLICATION_JSON)
            .header("Authorization", authDelegate.getAuthorization().toString())
            .header("User-Agent", UserAgent.toString())
            .build()
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(request: Request?, e: IOException?) {
                Logger.e(TAG, "A failure occurred during getPolicy", e)
                val reason = if(e is UnknownHostException) {
                    ChangedReason.DNS_TIMEDOUT
                } else if(e is SocketTimeoutException) {
                    ChangedReason.CONNECTION_TIMEDOUT
                } else if(e is ConnectException || e is SSLHandshakeException) {
                    ChangedReason.CONNECTION_ERROR
                } else {
                    ChangedReason.UNRECOVERABLE_ERROR
                }
                reason.cause = e
                observer.onError(reason)
            }

            override fun onResponse(response: Response?) {
                val code = response?.code()
                when (code) {
                    HttpURLConnection.HTTP_OK -> {
                        val jsonObject = JsonParser.parseString(response.body().string()).asJsonObject
                        if (!(jsonObject.has("healthCheckPolicy") && jsonObject.has("serverPolicies"))) {
                            observer.onError(ChangedReason.FAILURE_PROTOCOL_ERROR)
                            return
                        }
                        val policy = Gson().fromJson(jsonObject, Policy::class.java)
                        notifyPolicy(policy, observer)
                    }
                    HttpURLConnection.HTTP_UNAUTHORIZED,
                    HttpURLConnection.HTTP_FORBIDDEN -> {
                        observer.onError(ChangedReason.INVALID_AUTH)
                    }
                    else -> {
                        cachedPolicy?.let {
                            notifyPolicy(it, observer)
                        } ?: run {
                            when (code) {
                                in 400..499 -> observer.onError(ChangedReason.INTERNAL_ERROR)
                                in 500..599 -> observer.onError(ChangedReason.SERVER_INTERNAL_ERROR)
                                else -> observer.onError(ChangedReason.UNRECOVERABLE_ERROR)
                            }
                        }
                    }
                }
            }
        })
    }

    @VisibleForTesting
    internal fun notifyPolicy(policy: Policy?, observer: Observer) {
        if (!isShutdown.get()) {
            observer.onCompleted(policy)
            // cache setting
            cachedPolicy = policy
        }
    }

    fun disconnect() {
        // nothing to do
    }

    fun shutdown() {
        if (!isShutdown.compareAndSet(false, true)) {
            Logger.w(TAG, "[shutdown] already shutdown")
        }
    }
}