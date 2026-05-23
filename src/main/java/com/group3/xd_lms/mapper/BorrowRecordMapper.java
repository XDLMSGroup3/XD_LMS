package com.group3.xd_lms.mapper;

import com.group3.xd_lms.entity.BorrowRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface BorrowRecordMapper {

    // 新增借阅记录（借书）
    int insert(BorrowRecord borrowRecord);

    // 归还图书：更新归还时间
    int updateReturnDate(@Param("id") Long id,
                         @Param("returnDate") String returnDate);

    // 续借：更新应还日期 + 续借次数
    int updateRenewInfo(@Param("id") Long id,
                        @Param("dueDate") LocalDateTime dueDate,
                        @Param("renewCount") Integer renewCount);

    // 根据ID查询借阅记录
    BorrowRecord selectById(@Param("id") Long id);

    // 查询某个用户的所有借阅记录
    List<BorrowRecord> selectByUserId(@Param("userId") Long userId);

    // 查询某个用户当前未归还的记录
    List<BorrowRecord> selectUnreturnedByUserId(@Param("userId") Long userId);

    // 根据 RFID 查询当前未归还的借阅记录（用于还书）
    BorrowRecord selectUnreturnedByRfid(@Param("rfidTag") String rfidTag);

    // 查询所有逾期未还记录
    List<BorrowRecord> selectOverdueRecords();

    // 查询所有借阅记录
    List<BorrowRecord> selectAllRecords();

    //转借后修改数据库借阅记录,将书籍从 A 转移给 B，并重置借阅周期,record 包含 id(记录ID), userId(接收者ID), transferFromUserId(原持有者ID), dueDate(新应还日期)
    int updateForP2pTransfer(BorrowRecord record);

    //更新应还日期与续借次数 - 支持管理员 R2 审批通过后的数据更新
    int updateDueDateAndRenewCount(BorrowRecord record);
    /**
     * 根据用户ID删除所有已归还的借阅记录
     * @param userId 用户ID
     * @return 删除的记录条数
     */
    int deleteReturnedRecordsByUserId(Long userId);

    /**
     * 查询用户转借给其他人的记录（作为转出方，未归还）
     * @param userId 转出用户ID
     * @return P2P转借记录列表
     */
    List<BorrowRecord> selectP2pTransferOutRecordsByUserId(Long userId);
}
