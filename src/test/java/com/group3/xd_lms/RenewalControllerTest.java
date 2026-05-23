package com.group3.xd_lms;

import com.group3.xd_lms.entity.BookItem;
import com.group3.xd_lms.entity.BookMetaData;
import com.group3.xd_lms.entity.BorrowRecord;
import com.group3.xd_lms.entity.User;
import com.group3.xd_lms.mapper.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;

import java.time.LocalDateTime;
import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class RenewalControllerTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private BookMetaDataMapper bookMetadataMapper;

    @Autowired
    private BookItemMapper bookItemMapper;

    @Autowired
    private BorrowRecordMapper borrowRecordMapper;

    @Autowired
    private RenewalRequestMapper renewalRequestMapper;

    private Long testUserId;
    private String testIsbn;
    private String testRfid;
    private Long testBorrowRecordId;

    @BeforeEach
    void setUp() {
        // 使用唯一标识，确保 isbn 长度不超过 20
        String timestamp = String.valueOf(System.currentTimeMillis());
        String shortTimestamp = timestamp.substring(timestamp.length() - 10);
        testIsbn = "ISBN" + shortTimestamp;
        testRfid = "RFID" + shortTimestamp;

        // 1. 创建测试用户
        String userAccount = "test_" + shortTimestamp;
        User user = User.builder()
                .user_account(userAccount)
                .password("123456")
                .fullName("续借测试用户")
                .roleId(3)
                .status(User.UserStatus.Active)
                .build();
        userMapper.insert(user);
        User insertedUser = userMapper.selectByUserAccount(userAccount);
        testUserId = insertedUser.getId();

        // 2. 创建测试图书元数据
        BookMetaData metadata = BookMetaData.builder()
                .isbn(testIsbn)
                .title("续借测试图书")
                .build();
        bookMetadataMapper.insert(metadata);

        // 3. 创建测试图书实物
        BookItem bookItem = BookItem.builder()
                .rfidTag(testRfid)
                .isbn(testIsbn)
                .status(BookItem.BookStatus.Loaned)
                .build();
        bookItemMapper.insert(bookItem);

        // 4. 创建借阅记录
        BorrowRecord record = BorrowRecord.builder()
                .userId(testUserId)
                .rfidTag(testRfid)
                .borrowDate(LocalDateTime.now())
                .dueDate(LocalDateTime.now().plusDays(30))
                .renewCount(0)
                .build();
        borrowRecordMapper.insert(record);
        BorrowRecord insertedRecord = borrowRecordMapper.selectUnreturnedByRfid(testRfid);
        testBorrowRecordId = insertedRecord.getId();
    }

    @AfterEach
    void tearDown() {
        // 先删除续借申请（如果有）
        try {
            // 由于没有直接的删除方法，通过删除 borrow_record 会级联删除
        } catch (Exception e) {
            // 忽略
        }
        // 删除图书实物
        if (testRfid != null) {
            try {
                bookItemMapper.deleteByRfidTag(testRfid);
            } catch (Exception e) {
                // 忽略
            }
        }
        // 删除图书元数据
        if (testIsbn != null) {
            try {
                bookMetadataMapper.deleteByIsbn(testIsbn);
            } catch (Exception e) {
                // 忽略
            }
        }
        // 删除用户（会级联删除 borrow_records）
        if (testUserId != null) {
            try {
                userMapper.deleteById(testUserId);
            } catch (Exception e) {
                // 忽略
            }
        }
    }

    @Test
    @Order(1)
    void testAutoRenew_Success() {
        String url = "/renewal/apply?borrowRecordId=" + testBorrowRecordId + "&userId=" + testUserId;
        ResponseEntity<HashMap> response = restTemplate.postForEntity(url, null, HashMap.class);
        assertEquals(200, response.getBody().get("status"));
        String message = (String) response.getBody().get("message");
        assertTrue(message.contains("自动续借成功"));
        BorrowRecord updated = borrowRecordMapper.selectById(testBorrowRecordId);
        assertEquals(1, updated.getRenewCount());
    }

    @Test
    @Order(2)
    void testSecondAutoRenew_Success() {
        String url = "/renewal/apply?borrowRecordId=" + testBorrowRecordId + "&userId=" + testUserId;
        restTemplate.postForEntity(url, null, HashMap.class);
        ResponseEntity<HashMap> response = restTemplate.postForEntity(url, null, HashMap.class);
        assertEquals(200, response.getBody().get("status"));
        BorrowRecord updated = borrowRecordMapper.selectById(testBorrowRecordId);
        assertEquals(2, updated.getRenewCount());
    }

    @Test
    @Order(3)
    void testManualApproval_NeedReason() {
        String url = "/renewal/apply?borrowRecordId=" + testBorrowRecordId + "&userId=" + testUserId;
        restTemplate.postForEntity(url, null, HashMap.class);
        restTemplate.postForEntity(url, null, HashMap.class);
        ResponseEntity<HashMap> response = restTemplate.postForEntity(url, null, HashMap.class);
        assertEquals(400, response.getBody().get("status"));
        String message = (String) response.getBody().get("message");
        assertTrue(message.contains("必须填写续借理由"));
    }

    @Test
    @Order(4)
    void testManualApproval_SubmitSuccess() {
        String url = "/renewal/apply?borrowRecordId=" + testBorrowRecordId + "&userId=" + testUserId;
        restTemplate.postForEntity(url, null, HashMap.class);
        restTemplate.postForEntity(url, null, HashMap.class);
        String urlWithReason = "/renewal/apply?borrowRecordId=" + testBorrowRecordId
                + "&userId=" + testUserId + "&reason=需要继续研究课题";
        ResponseEntity<HashMap> response = restTemplate.postForEntity(urlWithReason, null, HashMap.class);
        assertEquals(200, response.getBody().get("status"));
        String message = (String) response.getBody().get("message");
        assertTrue(message.contains("已提交，等待馆员审批"));
    }

    @Test
    @Order(5)
    void testDuplicateSubmission_ShouldFail() {
        String url = "/renewal/apply?borrowRecordId=" + testBorrowRecordId + "&userId=" + testUserId;
        restTemplate.postForEntity(url, null, HashMap.class);
        restTemplate.postForEntity(url, null, HashMap.class);
        String urlWithReason = "/renewal/apply?borrowRecordId=" + testBorrowRecordId
                + "&userId=" + testUserId + "&reason=第一次申请";
        restTemplate.postForEntity(urlWithReason, null, HashMap.class);
        ResponseEntity<HashMap> response = restTemplate.postForEntity(urlWithReason, null, HashMap.class);
        assertEquals(409, response.getBody().get("status"));
        String message = (String) response.getBody().get("message");
        assertTrue(message.contains("已有待审批的续借申请"));
    }

    @Test
    @Order(6)
    void testPermissionDenied() {
        String url = "/renewal/apply?borrowRecordId=" + testBorrowRecordId + "&userId=99999";
        ResponseEntity<HashMap> response = restTemplate.postForEntity(url, null, HashMap.class);
        assertEquals(403, response.getBody().get("status"));
    }

    @Test
    @Order(7)
    void testRecordNotFound() {
        String url = "/renewal/apply?borrowRecordId=99999&userId=" + testUserId;
        ResponseEntity<HashMap> response = restTemplate.postForEntity(url, null, HashMap.class);
        assertEquals(404, response.getBody().get("status"));
    }
}