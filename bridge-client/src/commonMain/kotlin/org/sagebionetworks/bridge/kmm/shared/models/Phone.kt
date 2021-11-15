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
 * The user's phone number.
 * @param number The phone number (can be formatted in any way that's useful for end users).
 * @param regionCode CLDR two-letter region code describing the region in which the phone number was issued.
 * @param nationalFormat The phone number formatted in the commonly accepted national format of the region code. This value is read-only. 
 * @param type Phone
 */
@Serializable
data class Phone (
    /* The phone number (can be formatted in any way that's useful for end users). */
    @SerialName("number")
    val number: String,
    /* CLDR two-letter region code describing the region in which the phone number was issued. */
    @SerialName("regionCode")
    val regionCode: String,
    /* The phone number formatted in the commonly accepted national format of the region code. This value is read-only.  */
    @SerialName("nationalFormat")
    val nationalFormat: String? = null,
    /* Phone */
    @SerialName("type")
    val type: String? = null
)

