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
     * 1. 扫描新逾期并插入记录
     * 2. 对已存在的逾期记录累加金额
     */
    @Transactional // 保证批量操作的原子性
    @Scheduled(cron = "0 1 0 * * ?")
    public void autoUpdateOverdueFines() {
        // 1. 从系统设置中获取每日罚金单价
        BigDecimal dailyFineAmount = systemSettingsMapper.selectValueByKey("fine_per_day");

        // 2. 找到所有逾期未归还记录
        // 注意：selectOverdueRecords 的 SQL 应当只查 [未归还] 且 [已过期] 的记录
        List<BorrowRecord> overdueRecords = borrowRecordMapper.selectOverdueRecords();
        int newInsertCount = 0;
        if (overdueRecords != null && !overdueRecords.isEmpty()) {
            for (BorrowRecord record : overdueRecords) {
                // 3. 检查 fine 表中是否已经存在该借阅记录的罚款单 (防止重复创建)
                // 建议在 FineMapper 中实现 selectByBorrowRecordId 方法
                Fine existingFine = fineMapper.selectByBorrowRecordId(record.getId());

                if (existingFine == null) {
                    // 4. 如果是新产生的逾期，执行插入
                    Fine newFine = Fine.builder()
                            .userId(record.getUserId())
                            .borrowRecordId(record.getId())
                            .amount(BigDecimal.valueOf(0)) // 初始金额设为一天的罚金
                            .status(Fine.FineStatus.Unpaid)
                            .build();
                    fineMapper.insertFine(newFine);
                    newInsertCount++;
                }
            }
        }

        // 5. 调用 Mapper 执行对已存在的 Unpaid 罚单进行金额累加
        int updatedRows = fineMapper.incrementDailyFines(dailyFineAmount);

        System.out.println("【系统定时任务】扫描完成：新增罚单 " + newInsertCount + " 条，累加罚金 " + updatedRows + " 条。");
    }
}
