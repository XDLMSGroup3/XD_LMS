package com.group3.xd_lms.utils; // 建议放在 task 包下

import com.group3.xd_lms.entity.BorrowRecord;
import com.group3.xd_lms.entity.Fine;
import com.group3.xd_lms.mapper.BorrowRecordMapper;
import com.group3.xd_lms.mapper.FineMapper;
import com.group3.xd_lms.mapper.SystemSettingsMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Component
public class OverdueTask {

    @Autowired
    private FineMapper fineMapper;

    @Autowired
    private SystemSettingsMapper systemSettingsMapper;

    @Autowired
    private BorrowRecordMapper borrowRecordMapper;

    /**
     * 每日凌晨 00:01 分自动执行
     * 逻辑：
     * 1. 扫描所有逾期未归还的借阅记录
     * 2. 对于已有罚款记录：重新计算逾期天数 × 每日罚金，直接更新金额（不累加）
     * 3. 对于新逾期的记录：创建罚款记录，金额 = 逾期天数 × 每日罚金
     */
    @Transactional
//    @Scheduled(cron = "0 1 0 * * ?") // 每天凌晨 00:01
    @Scheduled(cron = "0/1 * * * * ?") // 测试用：每 1 秒执行一次
    public void autoUpdateOverdueFines() {

        // 1. 从系统设置中获取每日罚金单价
        BigDecimal dailyFineAmount = systemSettingsMapper.selectValueByKey("fine_per_day");
        if (dailyFineAmount == null) {
            dailyFineAmount = new BigDecimal("0.50"); // 兜底默认值：每天 0.5 元
            System.out.println("【系统定时任务】警告：未找到 fine_per_day 配置，使用默认值 0.50 元/天");
        }

        // 2. 获取当前日期（只取日期部分，忽略时分秒）
        LocalDate today = LocalDate.now();

        // 3. 找到所有逾期未归还的记录
        List<BorrowRecord> overdueRecords = borrowRecordMapper.selectOverdueRecords();

        int newInsertCount = 0;
        int updateCount = 0;

        if (overdueRecords != null && !overdueRecords.isEmpty()) {
            for (BorrowRecord record : overdueRecords) {
                // 4. 计算逾期天数：当前日期 - 应还日期
                LocalDate dueDate = record.getDueDate().toLocalDate();
                long overdueDays = ChronoUnit.DAYS.between(dueDate, today);

                // 如果逾期天数 <= 0，跳过
                if (overdueDays <= 0) {
                    continue;
                }

                // 5. 计算罚款金额：逾期天数 × 每日罚金
                BigDecimal amount = dailyFineAmount
                        .multiply(BigDecimal.valueOf(overdueDays))
                        .setScale(2, RoundingMode.HALF_UP);

                // 6. 检查是否已存在罚款记录
                Fine existingFine = fineMapper.selectByBorrowRecordId(record.getId());

                if (existingFine == null) {
                    // 7. 新逾期的记录：创建罚款记录
                    Fine newFine = Fine.builder()
                            .userId(record.getUserId())
                            .borrowRecordId(record.getId())
                            .amount(amount)           // 直接计算完整金额
                            .status(Fine.FineStatus.Unpaid)
                            .createdAt(LocalDateTime.now())
                            .build();
                    fineMapper.insertFine(newFine);
                    newInsertCount++;
                } else {
                    // 8. 已有罚款记录：直接更新金额（覆盖旧值，不累加）
                    if (existingFine.isUnpaid()) {
                        fineMapper.updateFineAmount(existingFine.getId(), amount);
                        updateCount++;
                    }
                }
            }
        }

        System.out.println("【系统定时任务】执行完成：" +
                "新增罚单 " + newInsertCount + " 条，" +
                "更新罚金 " + updateCount + " 条，" +
                "每日罚金单价：" + dailyFineAmount + " 元/天");
    }
}
