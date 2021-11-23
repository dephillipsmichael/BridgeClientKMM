/**
* Bridge Server API
* No description provided (generated by Openapi Generator https://github.com/openapitools/openapi-generator)
*
* OpenAPI spec version: 0.16.25
* 
*
* NOTE: This class is auto generated by the swagger code generator program.
* https://github.com/swagger-api/swagger-codegen.git
* Do not edit the class manually.
*/
package org.sagebionetworks.bridge.kmm.shared.models

import kotlinx.serialization.Serializable

import kotlinx.serialization.SerialName

/**
 * A pointer to a survey defined for this app, and associated with a survey to be taken as part of the mobile application health study. If the `createdOn` value is null, then the reference is to the most recently published version of the survey (the specific version that should be used will be resolved when a [ScheduledActivity](#ScheduledActivity) is created for the participant). 
 * @param identifier The survey identifier.
 * @param guid The survey guid
 * @param createdOn The createdOn timestamp of the version of the survey. This can be null, which indicates the most recently published version of the study should be used. 
 * @param href An URL to retrieve the survey.
 * @param type SurveyReference
 */
@Serializable
data class SurveyReference (
    /* The survey guid */
    @SerialName("guid")
    val guid: String,
    /* The survey identifier. */
    @SerialName("identifier")
    val identifier: String? = null,
    /* The createdOn timestamp of the version of the survey. This can be null, which indicates the most recently published version of the study should be used.  */
    @SerialName("createdOn")
    val createdOn: String? = null,
    /* An URL to retrieve the survey. */
    @SerialName("href")
    val href: String? = null,
    /* SurveyReference */
    @SerialName("type")
    val type: String? = null
)

