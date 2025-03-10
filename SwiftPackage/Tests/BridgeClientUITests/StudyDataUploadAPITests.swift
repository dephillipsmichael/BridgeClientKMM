//
//  StudyDataUploadAPITests.swift
//  BridgeFileUploadsTests
//
//  Copyright © 2021 Sage Bionetworks. All rights reserved.
//
// Redistribution and use in source and binary forms, with or without modification,
// are permitted provided that the following conditions are met:
//
// 1.  Redistributions of source code must retain the above copyright notice, this
// list of conditions and the following disclaimer.
//
// 2.  Redistributions in binary form must reproduce the above copyright notice,
// this list of conditions and the following disclaimer in the documentation and/or
// other materials provided with the distribution.
//
// 3.  Neither the name of the copyright holder(s) nor the names of any contributors
// may be used to endorse or promote products derived from this software without
// specific prior written permission. No license is granted to the trademarks of
// the copyright holders even if such marks are included in this software.
//
// THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
// AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
// IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
// ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE
// FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
// DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
// SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
// CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
// OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
// OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
//

import XCTest
@testable import BridgeClient
@testable import BridgeClientUI

class StudyDataUploadAPITests : XCTestCase, BridgeFileUploadManagerTestCaseTyped {
    typealias T = StudyDataUploadObject
    
    var uploadRequestSuccessResponseFile: String = "sd-upload-request-success"
    var uploadRequestExpiredResponseFile: String = "sd-upload-request-expired"
    var mockURLSession: MockURLSession = MockURLSession()
    var mockAppManager: MockBridgeClientAppManager = MockBridgeClientAppManager(appId: "not-a-real-appid")
    var testFileId: String = "TestFileId"
    var savedSession: URLSession?
    var savedDelay: TimeInterval?
    var savedAppManager: BridgeClientAppManager?
    var uploadApi: BridgeFileUploadAPI = StudyDataUploadAPI.shared
    var uploadExtras: Codable?
    
    lazy var requestEndpoint: String = {
        "/v3/uploads"
    }()
    
    var uploadSucceededNotification: Notification.Name = .SBBStudyFileUploaded
    var uploadRequestFailedNotification: Notification.Name = .SBBStudyFileUploadRequestFailed
    var uploadToS3FailedNotification: Notification.Name  = .SBBStudyFileUploadToS3Failed
    var bridgeNotificationFailedNotification: Notification.Name = .SBBStudyFileUploadBridgeNotificationFailed

    override func setUp() {
        // Put setup code here. This method is called before the invocation of each test method in the class.
        super.setUp()
        self.genericSetUp()
    }

    override func tearDown() {
        // Put teardown code here. This method is called after the invocation of each test method in the class.
        self.genericTearDown()
        super.tearDown()
    }
    
    func uploadRequestFailed412Tests(userInfo: [AnyHashable : Any]) {
        let sdua = StudyDataUploadAPI.shared
        let fileName = userInfo[sdua.fileNameKey] as? String
        XCTAssertNotNil(fileName, "SBBStudyFileUploadRequestFailed notification userInfo has no file name string at '\(sdua.fileNameKey)'")
    }
    
    func uploadSucceeded503RetriedTests(userInfo: [AnyHashable : Any]) {
        let sdua = StudyDataUploadAPI.shared
        let fileName = userInfo[sdua.fileNameKey] as? String
        XCTAssertNotNil(fileName, "SBBStudyFileUploaded notification userInfo has no file name string at '\(sdua.fileNameKey)'")
    }
    
    func testUploadRequestFails() {
        self.tryUploadRequestFails()
    }
    
    func testUploadFileToBridgeWhenS3RespondsWithVariousFailuresThatShouldRetryLater() {
        self.tryUploadFileToBridgeWhenS3RespondsWithVariousFailuresThatShouldRetryLater()
    }
    
    func testUploadFileToBridgeHappyPath() {
        self.tryUploadFileToBridgeHappyPath()
    }
    
}
