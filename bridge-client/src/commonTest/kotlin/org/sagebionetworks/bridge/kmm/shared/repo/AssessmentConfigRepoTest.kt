package org.sagebionetworks.bridge.kmm.shared.repo

import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.filterNot
import kotlinx.coroutines.flow.first
import org.sagebionetworks.bridge.kmm.shared.BaseTest
import org.sagebionetworks.bridge.kmm.shared.cache.ResourceDatabaseHelper
import org.sagebionetworks.bridge.kmm.shared.cache.ResourceResult
import org.sagebionetworks.bridge.kmm.shared.cache.ResourceType
import org.sagebionetworks.bridge.kmm.shared.getTestClient
import org.sagebionetworks.bridge.kmm.shared.models.AssessmentInfo
import org.sagebionetworks.bridge.kmm.shared.testDatabaseDriver
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class AssessmentConfigRepoTest: BaseTest() {

    val assessmentJson = "{\"config\":{\"type\":\"assessment\",\"identifier\":\"foo\",\"versionString\":\"1.2.3\",\"resultIdentifier\":\"bar\",\"title\":\"Hello World!\",\"subtitle\":\"Subtitle\",\"detail\":\"Some text. This is a test.\",\"estimatedMinutes\":4,\"icon\":\"fooIcon\",\"footnote\":\"This is a footnote.\",\"shouldHideActions\":[\"goBackward\"],\"progressMarkers\":[\"step1\",\"step2\"],\"steps\":[{\"identifier\":\"step1\",\"type\":\"instruction\",\"title\":\"Instruction Step 1\",\"detail\":\"Here are the details for this instruction.\",\"image\":{\"type\":\"fetchable\",\"identifier\":\"instructionImage\",\"imageName\":\"crf_seated\"}}]},\"createdOn\":\"2020-10-01T17:06:18.434Z\",\"modifiedOn\":\"2020-10-01T17:14:11.262Z\",\"version\":2,\"type\":\"AssessmentConfig\"}"

    @Test
    fun testAssessmentConfig() {
        runTest {
            val assessmentConfigId = AssessmentInfo(
                key = "testGuid",
                appId = "testApp",
                guid = "testGuid",
                identifier = "testIdentifier",
                label = "foo",
                configUrl = "http://www.google.com/testpath"
            )
            val repo = AssessmentConfigRepo(
                getTestClient(assessmentJson),
                ResourceDatabaseHelper(testDatabaseDriver()),
                MainScope()
            )


            val resultJson = repo.getAssessmentConfig(assessmentConfigId).filterNot { ResourceResult.InProgress == it }.first()
            assertNotNull(resultJson)

            val db = repo.database
            val r1 = db.getResource(assessmentConfigId.guid, ResourceType.ASSESSMENT_CONFIG,
                ResourceDatabaseHelper.APP_WIDE_STUDY_ID
            )
            assertNotNull(r1)
            assertEquals(ResourceType.ASSESSMENT_CONFIG, r1.type)
            assertTrue(r1.json?.contains("identifier")?: false)
            db.clearDatabase()
        }
    }

}

