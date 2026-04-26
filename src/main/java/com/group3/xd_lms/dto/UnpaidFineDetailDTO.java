package com.group3.xd_lms.dto;

import com.group3.xd_lms.entity.BorrowRecord;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;


@Data
@AllArgsConstructor
@NoArgsConstructor
public class UnpaidFineDetailDTO {
    private Long fineId;            // 罚款记录ID
    private BigDecimal fineAmount;  // 罚款金额
    private BorrowRecord borrowRecord;  // 借阅记录对象
    private String bookTitle;       // 书名
}
