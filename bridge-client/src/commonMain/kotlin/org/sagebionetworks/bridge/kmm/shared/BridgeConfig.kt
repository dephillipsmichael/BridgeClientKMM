package org.sagebionetworks.bridge.kmm.shared

import org.sagebionetworks.bridge.kmm.shared.models.ClientInfo

interface PlatformConfig {
    enum class BridgeEnvironment {
        PRODUCTION {
            override fun domainSuffix() = ""
        },
        STAGING {
            override fun domainSuffix() = "-staging"
        },
        DEVELOPMENT {
            override fun domainSuffix() = "-develop"
        },
        CUSTOM {
            override fun domainSuffix() = "-custom"
        };

        abstract fun domainSuffix(): String

        fun basePath(): String {
            return "https://webservices${domainSuffix()}.sagebridge.org"
        }
    }

    val appId: String

    val appName: String

    val appVersion: Int

    val appVersionName: String

    val bridgeEnvironment: BridgeEnvironment

    val osVersion: String

    val deviceName: String

    val osName: String
}

interface BridgeConfig : PlatformConfig {

    val sdkVersion: Int

    /**
     * Uses [.getStudyName], [.getAppVersion], [.getDeviceName], [ ][VERSION.RELEASE], and [.getSdkVersion]
     *
     * @return user agent in HTTP header format expected by server
     */
    val userAgent: String
        get() {
            return "$appId/$appVersion ($deviceName; $osName/$osVersion)"
        }

    /**
     *
     * Convert ClientInfo object into a User-Agent header value that can be used by the Bridge
     * server to filter content appropriately for your app.
     *
     *
     * There are three main stanzas in the Bridge User-Agent header, and all parts of a given
     * stanza must be provided or that stanza will be dropped from the final header:
     *
     *
     *  * appName and appVersion;
     *  * deviceName, osName and osVersion;
     *  * sdkName and sdkVersion
     *
     *
     *
     * The ClientManager will provide values for the final two groupings (and enforces
     * settings for the SDK information).
     *
     * @param info
     * a ClientInfo object
     * @return
     * string User-Agent header value
     */
    fun getUserAgent(info: ClientInfo): String? {
        // Send what is available. The server can handle all meaningful combinations.
        val sb = StringBuilder()
        if (isNotBlank(info.appName) || info.appName != null) {
            if (isNotBlank(info.appName)) {
                sb.append(info.appName)
            }
            if (info.appVersion != null) {
                sb.append("/")
                sb.append(info.appVersion)
            }
        }
        if (isNotBlank(info.deviceName) || isNotBlank(info.osName) || isNotBlank(info.osVersion)) {
            sb.append(" (")
            // Only include device name if there's also os, this is one ambiguity we cannot parse.
            if (isNotBlank(info.deviceName)) {
                sb.append(info.deviceName)
                sb.append("; ")
            }
            if (isNotBlank(info.osName)) {
                sb.append(info.osName)
            }
            if (isNotBlank(info.osVersion)) {
                sb.append("/")
                sb.append(info.osVersion)
            }
            sb.append(")")
        }
        if (isNotBlank(info.sdkName) || info.sdkVersion != null) {
            sb.append(" ")
            if (isNotBlank(info.sdkName)) {
                sb.append(info.sdkName)
            }
            if (info.sdkVersion != null) {
                sb.append("/")
                sb.append(info.sdkVersion)
            }
        }
        return if ("" == sb.toString()) {
            null
        } else sb.toString()
    }

    private fun isNotBlank(string: String?): Boolean {
        return string != null && string.isNotEmpty()
    }
}