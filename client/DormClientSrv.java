package vCampus.client;

import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import vCampus.common.vo.AccommodationImportRow;
import vCampus.common.Message;
import vCampus.common.consts.MsgConst;
import vCampus.common.enums.ApplicationType;
import vCampus.common.enums.PaymentMethod;
import vCampus.common.enums.RepairStatus;
import vCampus.common.vo.Accommodation;
import vCampus.common.vo.DormApplication;
import vCampus.common.vo.DormFeeBill;
import vCampus.common.vo.DormPaymentRecord;
import vCampus.common.vo.DormRepair;
import vCampus.common.vo.Dormitory;

import java.util.List;
/**
 * 宿舍管理模块客户端网络代理。
 *
 * UI -> DormClientSrv -> SocketClient -> Server -> DormHandler
 */
public final class DormClientSrv {

    private DormClientSrv() {
    }


    // =========================================================
    // 通用请求
    // =========================================================

    public static Message request(
            String type,
            Object data) {

        return SocketClient.send(
                new Message(type, data)
        );
    }


    // =========================================================
    // 一、学生端：宿舍查询
    // =========================================================

    public static List<Dormitory> queryAllDorm() {

        return getList(
                MsgConst.DORM_QUERY_ALL,
                null
        );
    }


    public static List<Dormitory> queryAvailableDorm() {

        return getList(
                MsgConst.DORM_QUERY_AVAILABLE,
                null
        );
    }


    public static Accommodation queryMyDorm(
            String sId) {

        return getObject(
                MsgConst.DORM_QUERY_MY,
                sId,
                Accommodation.class
        );
    }


    public static int getRemainingBeds(
            String dormId) {

        Message response =
                request(
                        MsgConst.DORM_QUERY_REMAINING_BEDS,
                        dormId
                );

        if (response.isSuccess()
                && response.getData()
                        instanceof Number) {

            return ((Number)
                    response.getData())
                    .intValue();
        }

        return -1;
    }


    /**
     * 查询指定宿舍可用床位
     */
    public static List<String> queryAvailableBeds(
            String dormId) {

        return getList(
                MsgConst.DORM_QUERY_AVAILABLE_BEDS,
                dormId
        );
    }
    
    // =========================================================
    // 二、学生端：调宿 / 退宿申请
    // =========================================================

    public static Message applyDormChange(
            String sId,
            String targetDormId,
            String targetBedNumber,
            String reason) {

        return request(
                MsgConst.DORM_APPLY_CHANGE,
                params(
                        "sId", sId,
                        "targetDormId", targetDormId,
                        "targetBedNumber", targetBedNumber,
                        "reason", reason
                )
        );
    }


    public static Message applyCheckout(
            String sId,
            ApplicationType applicationType,
            String reason) {

        return request(
                MsgConst.DORM_APPLY_CHECKOUT,
                params(
                        "sId", sId,
                        "applicationType",
                        applicationType,
                        "reason", reason
                )
        );
    }


    public static List<DormApplication>
            queryMyApplications(
                    String sId) {

        return getList(
                MsgConst.DORM_QUERY_MY_APPLICATIONS,
                sId
        );
    }


    public static Message cancelApplication(
            String sId,
            int applicationId) {

        return request(
                MsgConst.DORM_CANCEL_APPLICATION,
                params(
                        "sId", sId,
                        "applicationId",
                        applicationId
                )
        );
    }


    // =========================================================
    // 三、学生端：报修
    // =========================================================

    public static Message submitRepair(
            String sId,
            String dormId,
            String description) {

        return request(
                MsgConst.DORM_SUBMIT_REPAIR,
                params(
                        "sId", sId,
                        "dormId", dormId,
                        "description", description
                )
        );
    }


    public static List<DormRepair>
            queryMyRepairs(
                    String sId) {

        return getList(
                MsgConst.DORM_QUERY_MY_REPAIRS,
                sId
        );
    }


    public static Message cancelRepair(
            String sId,
            int repairId) {

        return request(
                MsgConst.DORM_CANCEL_REPAIR,
                params(
                        "sId", sId,
                        "repairId", repairId
                )
        );
    }


    // =========================================================
    // 四、学生端：住宿费
    // =========================================================

    public static List<DormFeeBill>
            queryMyBills(
                    String sId) {

        return getList(
                MsgConst.DORM_QUERY_MY_BILLS,
                sId
        );
    }


    public static List<DormFeeBill>
            queryMyUnpaidBills(
                    String sId) {

        return getList(
                MsgConst.DORM_QUERY_UNPAID_BILLS,
                sId
        );
    }


    public static Message payBill(
            String sId,
            int billId,
            double amount,
            PaymentMethod paymentMethod,
            String bankPIN) {

        return request(
                MsgConst.DORM_PAY_BILL,
                params(
                        "sId", sId,
                        "billId", billId,
                        "amount", amount,
                        "paymentMethod",
                        paymentMethod,
                        "bankPIN",
                        bankPIN
                )
        );
    }


    public static List<DormPaymentRecord>
            queryPaymentHistory(
                    String sId) {

        return getList(
                MsgConst.DORM_QUERY_PAYMENT_HISTORY,
                sId
        );
    }


    // =========================================================
    // 五、管理员端：宿舍信息管理
    // =========================================================

    public static List<Dormitory>
            adminQueryAllDorm() {

        return getList(
                MsgConst.DORM_ADMIN_QUERY_ALL,
                null
        );
    }


    public static List<Dormitory>
            adminQueryAvailableDorm() {

        return getList(
                MsgConst.DORM_ADMIN_QUERY_AVAILABLE,
                null
        );
    }


    public static Message addDorm(
            Dormitory dorm) {

        return request(
                MsgConst.DORM_ADMIN_ADD,
                dorm
        );
    }


    public static Message updateDorm(
            Dormitory dorm) {

        return request(
                MsgConst.DORM_ADMIN_UPDATE,
                dorm
        );
    }


    public static Message deleteDorm(
            String dormId) {

        return request(
                MsgConst.DORM_ADMIN_DELETE,
                dormId
        );
    }


    // =========================================================
    // 六、管理员端：住宿管理
    // =========================================================

    public static Message checkIn(
            String sId,
            String dormId,
            String bedNumber) {

        return request(
                MsgConst.DORM_ADMIN_CHECK_IN,
                params(
                        "sId", sId,
                        "dormId", dormId,
                        "bedNumber", bedNumber
                )
        );
    }

    public static Message batchCheckIn(
            List<AccommodationImportRow> rows,
            String academicYear,
            String semester,
            Date dueDate) {

        return request(
                MsgConst.DORM_ADMIN_BATCH_CHECK_IN,
                params(
                        "rows", rows,
                        "academicYear", academicYear,
                        "semester", semester,
                        "dueDate", dueDate
                )
        );
    }

    public static Message checkOut(
            String sId) {

        return request(
                MsgConst.DORM_ADMIN_CHECK_OUT,
                sId
        );
    }


    public static Message changeDorm(
            String sId,
            String newDormId,
            String newBedNumber) {

        return request(
                MsgConst.DORM_ADMIN_CHANGE,
                params(
                        "sId", sId,
                        "newDormId", newDormId,
                        "newBedNumber", newBedNumber
                )
        );
    }


    public static Accommodation queryStudentDorm(
            String sId) {

        return getObject(
                MsgConst.DORM_ADMIN_QUERY_STUDENT,
                sId,
                Accommodation.class
        );
    }
    
    public static List<Accommodation> queryStudentDormHistory(
            String sId) {

        return getList(
                MsgConst.DORM_QUERY_HISTORY,
                sId
        );
    }
    


    // =========================================================
    // 七、管理员端：申请审核
    // =========================================================

    public static List<DormApplication>
            queryPendingApplications() {

        return getList(
                MsgConst.DORM_ADMIN_QUERY_PENDING_APPLICATIONS,
                null
        );
    }


    public static List<DormApplication>
            queryApplicationsByType(
                    ApplicationType type) {

        return getList(
                MsgConst.DORM_ADMIN_QUERY_APPLICATIONS_BY_TYPE,
                type
        );
    }


    public static Message approveApplication(
            int applicationId,
            String adminId,
            String remark) {

        return request(
                MsgConst.DORM_ADMIN_APPROVE_APPLICATION,
                params(
                        "applicationId",
                        applicationId,
                        "adminId", adminId,
                        "remark", remark
                )
        );
    }


    public static Message rejectApplication(
            int applicationId,
            String adminId,
            String remark) {

        return request(
                MsgConst.DORM_ADMIN_REJECT_APPLICATION,
                params(
                        "applicationId",
                        applicationId,
                        "adminId", adminId,
                        "remark", remark
                )
        );
    }


    // =========================================================
    // 八、管理员端：报修管理
    // =========================================================

    public static List<DormRepair>
            queryAllRepairs() {

        return getList(
                MsgConst.DORM_ADMIN_QUERY_ALL_REPAIRS,
                null
        );
    }


    public static DormRepair queryRepairById(
            int repairId) {

        return getObject(
                MsgConst.DORM_ADMIN_QUERY_REPAIR_BY_ID,
                repairId,
                DormRepair.class
        );
    }


    public static Message updateRepairStatus(
            int repairId,
            RepairStatus status) {

        return request(
                MsgConst.DORM_ADMIN_UPDATE_REPAIR_STATUS,
                params(
                        "repairId", repairId,
                        "status", status
                )
        );
    }


    // =========================================================
    // 九、管理员端：住宿费管理
    // =========================================================

    public static Message createBill(
            String sId,
            int accommodationId,
            String academicYear,
            String semester,
            double amount,
            Date dueDate) {

        return request(
                MsgConst.DORM_ADMIN_CREATE_BILL,
                params(
                        "sId", sId,
                        "accommodationId",
                        accommodationId,
                        "academicYear",
                        academicYear,
                        "semester", semester,
                        "amount", amount,
                        "dueDate", dueDate
                )
        );
    }


    public static Message batchCreateBills(
            List<DormFeeBill> bills) {

        return request(
                MsgConst.DORM_ADMIN_BATCH_CREATE_BILLS,
                bills
        );
    }


    public static List<DormFeeBill>
            queryAllBills() {

        return getList(
                MsgConst.DORM_ADMIN_QUERY_ALL_BILLS,
                null
        );
    }


    public static List<DormFeeBill>
            queryStudentBills(
                    String sId) {

        return getList(
                MsgConst.DORM_ADMIN_QUERY_STUDENT_BILLS,
                sId
        );
    }


    public static List<DormFeeBill>
            adminQueryUnpaidBills() {

        return getList(
                MsgConst.DORM_ADMIN_QUERY_UNPAID_BILLS,
                null
        );
    }


    public static DormFeeBill queryBillById(
            int billId) {

        return getObject(
                MsgConst.DORM_ADMIN_QUERY_BILL_BY_ID,
                billId,
                DormFeeBill.class
        );
    }


    public static Message adjustBill(
            int billId,
            double amount) {

        return request(
                MsgConst.DORM_ADMIN_ADJUST_BILL,
                params(
                        "billId", billId,
                        "amount", amount
                )
        );
    }


    public static Message cancelBill(
            int billId) {

        return request(
                MsgConst.DORM_ADMIN_CANCEL_BILL,
                billId
        );
    }


    // =========================================================
    // 通用辅助方法
    // =========================================================

    private static Map<String, Object> params(
            Object... keyValues) {

        if (keyValues.length % 2 != 0) {
            throw new IllegalArgumentException(
                    "参数必须成对出现"
            );
        }

        Map<String, Object> map =
                new HashMap<>();

        for (int i = 0;
                i < keyValues.length;
                i += 2) {

            map.put(
                    (String) keyValues[i],
                    keyValues[i + 1]
            );
        }

        return map;
    }


    @SuppressWarnings("unchecked")
    private static <T> List<T> getList(
            String type,
            Object data) {

        Message response =
                request(type, data);

        if (response.isSuccess()
                && response.getData()
                        instanceof List<?>) {

            return (List<T>)
                    response.getData();
        }

        return Collections.emptyList();
    }


    private static <T> T getObject(
            String type,
            Object data,
            Class<T> clazz) {

        Message response =
                request(type, data);

        if (response.isSuccess()
                && clazz.isInstance(
                        response.getData())) {

            return clazz.cast(
                    response.getData()
            );
        }

        return null;
    }
}