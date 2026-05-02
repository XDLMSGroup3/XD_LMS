package com.group3.xd_lms.web;

import com.group3.xd_lms.entity.BookItem;
import com.group3.xd_lms.entity.BorrowRecord;
import com.group3.xd_lms.entity.User;
import com.group3.xd_lms.mapper.BookItemMapper;
import com.group3.xd_lms.mapper.BorrowRecordMapper;
import com.group3.xd_lms.mapper.SystemSettingsMapper;
import com.group3.xd_lms.mapper.UserMapper;
import com.group3.xd_lms.utils.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping(value = "/borrow")
public class BorrowController {

    private static final DateTimeFormatter DB_DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final BorrowRecordMapper borrowRecordMapper;
    private final BookItemMapper bookItemMapper;
    private final UserMapper userMapper;
    private final SystemSettingsMapper systemSettingsMapper;
    public BorrowController(BorrowRecordMapper borrowRecordMapper, BookItemMapper bookItemMapper, UserMapper userMapper, SystemSettingsMapper systemSettingsMapper) {
        this.borrowRecordMapper = borrowRecordMapper;
        this.bookItemMapper = bookItemMapper;
        this.userMapper = userMapper;
        this.systemSettingsMapper = systemSettingsMapper;
    }

    /**
     * 借书接口
     * URL: POST /borrow/borrowBook 或 /borrow/reader/borrowBook
     * 功能：根据RFID和用户ID创建借阅记录，更新图书状态为已借出
     *
     * @param rfidTag 图书RFID标签
     * @param userId  借阅用户ID
     * @return 借阅结果（成功/失败原因）
     */
    @RequestMapping(value = {"/borrowBook"}, method = RequestMethod.POST)
    @Transactional
    public Map<String, Object> borrowBook(@RequestParam String rfidTag, @RequestParam Long userId) {
        // 扫描RFID码进行借书
        System.out.println(rfidTag);
        BookItem bookItem = bookItemMapper.selectByRfidTag(rfidTag);
        User user = userMapper.selectById(userId);
        BigDecimal max_loan_days = systemSettingsMapper.selectValueByKey("max_loan_days");
        if (bookItem == null) { // 未找到书目
            return Result.getResultMap(500, "Search Book Failed");
        }
        if (user == null) {
            return Result.getResultMap(500, "Search User Failed");
        }
        if (bookItem.isAvailable()) {
            BorrowRecord borrowRecord = new BorrowRecord();
            borrowRecord.setUserId(userId);
            borrowRecord.setRfidTag(rfidTag);
            borrowRecord.setBorrowDate(LocalDateTime.now());
            borrowRecord.setDueDate(LocalDateTime.now().plusDays(max_loan_days.longValue()));
            borrowRecord.setUser(user);
            bookItem.setStatus(BookItem.BookStatus.Loaned);
            bookItemMapper.updateStatus(rfidTag,BookItem.BookStatus.Loaned.name());
            borrowRecordMapper.insert(borrowRecord);
            return Result.getResultMap(200, "Borrow Book Success");
        } else {
            return Result.getResultMap(500, "the Item Can't  Be Borrowed");
        }
    }

    /**
     * 还书接口
     * URL: POST /borrow/returnBook 或 /borrow/reader/returnBook
     * 功能：根据RFID更新借阅记录为已归还，更新图书状态为在馆
     *
     * @param rfidTag 图书RFID标签
     * @param userId  可选，当前操作用户ID（用于权限校验）
     * @return 还书结果（成功/失败原因）
     */
    @RequestMapping(value = {"/returnBook", "/reader/returnBook"}, method = RequestMethod.POST)
    @Transactional(rollbackFor = Exception.class)
    public HashMap<String, Object> returnBook(
            @RequestParam String rfidTag,
            @RequestParam(required = false) Long userId) {

        // 1. 参数校验 (rfidTag 必填，userId 选填)
        if (rfidTag == null || rfidTag.trim().isEmpty()) {
            return Result.getResultMap(400, "RFID Cant be empty");
        }
        rfidTag = rfidTag.trim();

        // 2. 查询未归还的记录
        BorrowRecord record = borrowRecordMapper.selectUnreturnedByRfid(rfidTag);
        if (record == null) {
            return Result.getResultMap(404, "Dont Find the Unreturned Record");
        }

        // 3. 权限校验 (如果传入了 userId，则校验是否匹配)
        if (userId != null) {
            if (!userId.equals(record.getUserId())) {
                return Result.getResultMap(500, "当前用户无权归还该图书");
            }
        }

        // 4. 执行还书逻辑
        LocalDateTime now = LocalDateTime.now();

        // 4.1 更新借阅记录状态
        // 注意：这里假设你的 Mapper 接受 String 类型的日期，如果接受 LocalDateTime 可直接传 now
        String dateStr = now.format(DB_DATE_TIME_FORMATTER);
        int updatedRecord = borrowRecordMapper.updateReturnDate(record.getId(), dateStr);
        if (updatedRecord <= 0) {
            // 抛出异常触发事务回滚
            throw new RuntimeException("Return Book Failed");
        }

        // 4.2 更新图书状态为“在馆”
        int updatedBook = bookItemMapper.updateStatus(rfidTag, BookItem.BookStatus.Available.name());
        if (updatedBook <= 0) {
            // 抛出异常触发事务回滚
            throw new RuntimeException("Return Book Failed");
        }

        // 5. 组装返回数据
        BookItem bookItem = bookItemMapper.selectByRfidTag(rfidTag);
        Map<String, Object> data = new HashMap<>();
        data.put("recordId", record.getId());
        data.put("userId", record.getUserId());
        data.put("rfidTag", rfidTag);
        data.put("returnTime", now);
        data.put("book", bookItem);

        return Result.getResultMap(200, "Return Success", data);
    }

    /**
     * 查询所有借阅记录
     * URL: GET /borrow/getAllRecords
     * 功能：获取系统中所有借阅记录的列表
     *
     * @return 借阅记录列表
     */
    @GetMapping("/getAllRecords")
    public Map<String, Object> getAllRecords() {
        List<BorrowRecord> list = borrowRecordMapper.selectAllRecords();
        if (list == null) {
            return Result.getResultMap(500, "Search Borrow Records Failed");
        }
        return Result.getListResultMap(200, "Search Success", list.size(), list);
    }

    /**
     * 根据用户ID查询借阅记录
     * URL: GET /borrow/getRecordsByUserId
     * 功能：获取指定用户的所有借阅记录
     *
     * @param userId 用户ID
     * @return 该用户的借阅记录列表
     */
    @GetMapping("/getRecordsByUserId")
    public Map<String, Object> getRecordsByUserId(@RequestParam Long userId) {
        // 参数校验
        if (userId == null || userId <= 0) {
            return Result.getResultMap(400, "User Id Cant be empty");
        }

        List<BorrowRecord> list = borrowRecordMapper.selectByUserId(userId);
        if (list == null) {
            return Result.getResultMap(500, "Search Borrow Records Failed");
        }

        return Result.getListResultMap(200, "Search Success", list.size(), list);
    }

    /**
     * 根据RFID查询借阅记录
     * URL: GET /borrow/getRecordByRfid
     * 功能：获取指定RFID图书的当前未归还借阅记录
     *
     * @param rfidTag 图书RFID标签
     * @return 该图书的借阅记录
     */
    @GetMapping("/getRecordByRfid")
    public Map<String, Object> getRecordByRfid(@RequestParam String rfidTag) {
        // 参数校验
        if (rfidTag == null || rfidTag.trim().isEmpty()) {
            return Result.getResultMap(400, "RFID Cant be empty");
        }

        BorrowRecord record = borrowRecordMapper.selectUnreturnedByRfid(rfidTag);
        if (record == null) {
            return Result.getResultMap(404, "Can not find the Unreturned Record");
        }

        return Result.getResultMap(200, "Search Success", record);
    }


    /**
     * 获取借阅总数量
     * URL: GET /borrow/getTotalCount
     * 功能：统计系统中所有借阅记录的总数
     *
     * @return 借阅记录总数
     */
    @GetMapping("/getTotalCount")
    public Map<String, Object> getTotalCount() {
        List<BorrowRecord> all = borrowRecordMapper.selectAllRecords();
        int count = all == null ? 0 : all.size();
        return Result.getListResultMap(200, "Search Success", count, null);
    }

    /**
     * 获取逾期记录总数
     * URL: GET /borrow/getOverdueCount
     * 功能：统计当前所有逾期未归还的借阅记录数量
     *
     * @return 逾期记录总数
     */
    @GetMapping("/getOverdueCount")
    public Map<String, Object> getOverdueCount() {
        List<BorrowRecord> overdue = borrowRecordMapper.selectOverdueRecords();
        int count = overdue == null ? 0 : overdue.size();
        return Result.getListResultMap(200, "Search Success", count, null);
    }

}
