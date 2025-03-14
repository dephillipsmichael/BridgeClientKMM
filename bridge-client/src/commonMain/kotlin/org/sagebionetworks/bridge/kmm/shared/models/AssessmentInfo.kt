/**
* Bridge Server API
* No description provided (generated by Openapi Generator https://github.com/openapitools/openapi-generator)
*
* OpenAPI spec version: 0.21.18
* 
*
* NOTE: This class is auto generated by the swagger code generator program.
* https://github.com/swagger-api/swagger-codegen.git
* Do not edit the class manually.
*/
package org.sagebionetworks.bridge.kmm.shared.models

import io.ktor.http.*
import kotlinx.serialization.Serializable

import kotlinx.serialization.SerialName
import kotlinx.serialization.Transient

/**
 * Information about an assessment in order to render it in a UI prior to execution.
 * @param key Because assessments can be configured with different display information, they are not referenced in the schedule portion of a timeline by their GUIDs. Instead they are given a unique string key that is used to look up the assessment.
 * @param guid Each assessment revision is assigned a unique GUID which can be used to retrieve it through the API. Assessments will also have a unique combination of an identifier and a revision, and can be retrieved as a set of revisions under a given identifier.
 * @param appId
 * @param identifier A human-readable identifier for an assessment, which can have one or more revisions.
 * @param revision The revision of the assessment under the given identifier (the identifier and the revision together are mapped to a single GUID which can be used to reference the assessment; only the GUID is required in this object).
 * @param label
 * @param minutesToComplete The number of minutes it takes for a participant to complete this assessment.
 * @param colorScheme
 * @param configUrl The URL to download the configuration associated with this assessment.
 * @param type AssessmentInfo
 */
@Serializable
data class AssessmentInfo (
    /* Because assessments can be configured with different display information, they are not referenced in the schedule portion of a timeline by their GUIDs. Instead they are given a unique string key that is used to look up the assessment. */
    @SerialName("key")
    val key: String,
    /* Each assessment revision is assigned a unique GUID which can be used to retrieve it through the API. Assessments will also have a unique combination of an identifier and a revision, and can be retrieved as a set of revisions under a given identifier. */
    @SerialName("guid")
    val guid: String,
    @SerialName("appId")
    val appId: String,
    /* A human-readable identifier for an assessment, which can have one or more revisions. */
    @SerialName("identifier")
    val identifier: String,
    /* The revision of the assessment under the given identifier (the identifier and the revision together are mapped to a single GUID which can be used to reference the assessment; only the GUID is required in this object). */
    @SerialName("revision")
    val revision: Int? = null,
    @SerialName("label")
    val label: String,
    /* The number of minutes it takes for a participant to complete this assessment. */
    @SerialName("minutesToComplete")
    val minutesToComplete: Int? = null,
    @SerialName("colorScheme")
    val colorScheme: ColorScheme? = null,
    /* The URL to download the configuration associated with this assessment. */
    @SerialName("configUrl")
    val configUrl: String,
    /* AssessmentInfo */
    @SerialName("type")
    val type: String? = null
) {

    /** Path portion of config url */
    @Transient
    val configPath: String
        get() = Url(configUrl).encodedPath

}

