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

import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.Serializable

import kotlinx.serialization.SerialName

/**
 * 
 */
@Serializable
data class UserSessionInfo (
    /* First name (given name) of the user. */
    @SerialName("firstName")
    val firstName: String? = null,
    /* Last name (family name) of the user. */
    @SerialName("lastName")
    val lastName: String? = null,
    /* An externally-assignable identifier a research partner can use to re-identify a user's data in the exported data set (this must be provided by the application, it is not created by Bridge). It is a string that can be set or updated to any value without constraints, unless Bridge is configured to manage the study's external IDs. Then the ID must be submitted on sign up, and cannot be modified afterward.  */
    @SerialName("externalId")
    val externalId: String? = null,
    /* An ID assigned to this account by Bridge system. This ID is exposed in the API and is different from the health code assigned to the user's anonymized data. Bridge never exports this ID along with the health code from Bridge.    */
    @SerialName("id")
    val id: String,
    /* True if the user has consented to be contacted via email outside the application, false otherwise.  */
    @SerialName("notifyByEmail")
    val notifyByEmail: Boolean? = null,
    /* A map of user profile attributes that have been set for this user (the attributes themselves must be specified in the app's configuration, and the values are stored encrypted in case they capture personally-identifying information).  */
    @SerialName("attributes")
    val attributes: Map<String, String>? = null,
    @SerialName("sharingScope")
    val sharingScope: SharingScope? = null,
    /* The date and time the account was created. */
    @SerialName("createdOn")
    val createdOn: String? = null,
    /* Has the user verified that they have control of this email address? */
    @SerialName("emailVerified")
    val emailVerified: Boolean? = null,
    /* Has the user verified that they have control of this phone number? */
    @SerialName("phoneVerified")
    val phoneVerified: Boolean? = null,
    @SerialName("status")
    val status: AccountStatus? = null,
    @SerialName("roles")
    val roles: List<Role>? = null,
    /* The data groups set for this user. Data groups must be strings that are defined in the list of all valid data groups for the app, as part of the app object.   */
    @SerialName("dataGroups")
    val dataGroups: List<String>? = null,
    /* Client data for a user should be in a syntactically valid JSON format. It will be returned as is to the client (as JSON).  */
    @SerialName("clientData")
    val clientData: JsonElement? = null,
    /* Two letter language codes to assign to this user (these are initially retrieved from the user's `Accept-Language` header and then persisted as part of account configuration).   */
    @SerialName("languages")
    val languages: List<String>? = null,
    /* The studies this participant is associated to. */
    @SerialName("studyIds")
    val studyIds: List<String>,
    /* The exernal IDs this participant is associated to, mapped to the study that issued the external ID. Typically a user signs up with the external ID, and is assigned to that study as a result. */
    @SerialName("externalIds")
    val externalIds: Map<String, String>? = null,
    /* Is the user currently authenticated? */
    @SerialName("authenticated")
    val authenticated: Boolean,
    /* The session token that must be returned to the server to access services requiring authentication. */
    @SerialName("sessionToken")
    val sessionToken: String,
    /* A token, supplied when a new session is returned, that can be used to refresh the session at a later time. The API to reauthenticate with the token will refresh your session token and reset the session's timeout value. The reauthentication token can only be used one time.   */
    @SerialName("reauthToken")
    val reauthToken: String? = null,
    @SerialName("environment")
    val environment: Environment? = null,
    /* The user's email. */
    @SerialName("email")
    val email: String? = null,
    @SerialName("phone")
    val phone: Phone? = null,
    /* True if the sharing scope is anything other than \"no_sharing\". */
    @SerialName("dataSharing")
    val dataSharing: Boolean? = null,
    /* True if all *required* consents have been signed and the versions signed are the most up-to-date versions of those consents. */
    @SerialName("signedMostRecentConsent")
    val signedMostRecentConsent: Boolean? = null,
    /* The numerical identifier for a Synapse account. For administrative accounts, this will allow the user to sign in using Synapse accounts, no further credentials are needed to access the system. This value can only be set when creating an account; otherwise the identifiers update API must be used to add the ID to an existing account.  */
    @SerialName("synapseUserId")
    val synapseUserId: String? = null,
    /* True if all required consents have been signed. */
    @SerialName("consented")
    val consented: Boolean = false,
    /* A mapping from a subpopulation GUID to information about the participant's consent status in that subpopulation (whether consented or not). Only the subpopulations that currently apply to this user will have a ConsentStatus object in the map.  */
    @SerialName("consentStatuses")
    val consentStatuses: Map<String, ConsentStatus>? = null,
    /* Records of every enrollment (whether withdrawn or not) in every study for this participant. The key of this mapping is the studyId, and the value is the record of the enrollment. */
    @SerialName("enrollments")
    val enrollments: kotlin.collections.Map<kotlin.String, EnrollmentInfo>? = null,
    /* The identifier of the organization this account is a member of (this will be an administrative account and not a study participant). */
    @SerialName("orgMembership")
    val orgMembership: String? = null,
    /* UserSessionInfo */
    @SerialName("type")
    val type: String? = null
)

