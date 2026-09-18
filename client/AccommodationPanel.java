package vCampus.client;

import vCampus.common.Message;
import vCampus.common.vo.Accommodation;
import vCampus.common.vo.AccommodationImportRow;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableModel;

import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.awt.*;
import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.List;


public class AccommodationPanel extends JPanel {

    private JTable table;

    private DefaultTableModel tableModel;

    private JTextField sidField;


    public AccommodationPanel() {

        initUI();

        SwingUtilities.invokeLater(
                () -> queryStudent()
        );
    }


    private void initUI() {

        setLayout(
                new BorderLayout()
        );


        // =========================
        // 顶部查询区域
        // =========================

        JPanel topPanel =
                new JPanel(
                        new FlowLayout(
                                FlowLayout.LEFT
                        )
                );


        topPanel.add(
                new JLabel("学生学号：")
        );


        sidField =
                new JTextField(15);


        topPanel.add(
                sidField
        );


        JButton queryButton =
                new JButton("查询");


        topPanel.add(
                queryButton
        );


        add(
                topPanel,
                BorderLayout.NORTH
        );


        // =========================
        // 中间住宿信息表格
        // =========================

        String[] columns = {

                "记录ID",

                "学生学号",

                "宿舍编号",

                "床位",

                "入住时间",

                "退宿时间",

                "状态"

        };


        tableModel =
                new DefaultTableModel(
                        columns,
                        0
                ) {

                    private static final long
                            serialVersionUID = 1L;


                    @Override
                    public boolean isCellEditable(
                            int row,
                            int column) {

                        return false;
                    }
                };


        table =
                new JTable(
                        tableModel
                );


        add(
                new JScrollPane(table),
                BorderLayout.CENTER
        );


        // =========================
        // 底部按钮
        // =========================

        JPanel bottomPanel =
                new JPanel();


        JButton importButton =
                new JButton("批量导入入住");


        JButton checkInButton =
                new JButton("办理入住");


        JButton checkOutButton =
                new JButton("办理退宿");


        JButton changeButton =
                new JButton("调换宿舍");


        bottomPanel.add(
                importButton
        );


        bottomPanel.add(
                checkInButton
        );


        bottomPanel.add(
                checkOutButton
        );


        bottomPanel.add(
                changeButton
        );


        add(
                bottomPanel,
                BorderLayout.SOUTH
        );


        // =========================
        // 事件
        // =========================

        queryButton.addActionListener(
                e -> queryStudent()
        );


        importButton.addActionListener(
                e -> importExcel()
        );


        checkInButton.addActionListener(
                e -> checkIn()
        );


        checkOutButton.addActionListener(
                e -> checkOut()
        );


        changeButton.addActionListener(
                e -> changeDorm()
        );
    }


    /**
     * 自动刷新入住管理
     *
     * 保留当前学号筛选条件，重新从数据库查询住宿信息。
     */
    public void refreshData() {
        queryStudent(false);
    }


    /**
     * 查询住宿历史
     *
     * 学号为空：
     * 查询所有学生的住宿历史
     *
     * 学号不为空：
     * 查询指定学生的住宿历史
     */
    private void queryStudent() {
        queryStudent(true);
    }


    private void queryStudent(boolean showMessage) {

        String sId =
                sidField
                        .getText()
                        .trim();


        tableModel.setRowCount(0);


        List<Accommodation> list =
                DormClientSrv
                        .queryStudentDormHistory(
                                sId
                        );


        if (list == null
                || list.isEmpty()) {

            if(showMessage){
                JOptionPane.showMessageDialog(
                        this,
                        sId.isEmpty()
                                ? "暂无住宿历史记录"
                                : "该学生暂无住宿记录"
                );
            }

            return;
        }


        for (Accommodation accommodation
                : list) {

            tableModel.addRow(
                    new Object[]{

                            accommodation
                                    .getRecordId(),

                            accommodation
                                    .getsId(),

                            accommodation
                                    .getDormId(),

                            accommodation
                                    .getBedNumber(),

                            accommodation
                                    .getCheckInTime(),

                            accommodation
                                    .getCheckOutTime(),

                            accommodation
                                    .getStatus()
                    }
            );
        }
    }


    /**
     * 批量导入 Excel
     */
    private void importExcel() {

        JFileChooser chooser =
                new JFileChooser();


        chooser.setDialogTitle(
                "选择住宿分配 Excel 文件"
        );


        chooser.setFileFilter(
                new FileNameExtensionFilter(
                        "Excel 文件 (*.xlsx, *.xls)",
                        "xlsx",
                        "xls"
                )
        );


        int result =
                chooser.showOpenDialog(this);


        if (result != JFileChooser.APPROVE_OPTION) {

            return;
        }


        File file =
                chooser.getSelectedFile();


        List<AccommodationImportRow> rows =
                readExcel(file);


        if (rows == null
                || rows.isEmpty()) {

            JOptionPane.showMessageDialog(
                    this,
                    "Excel 中没有有效的住宿分配数据"
            );

            return;
        }


        JTextField academicYearField =
                new JTextField(
                        "2026-2027"
                );


        JComboBox<String> semesterBox =
                new JComboBox<>(
                        new String[]{
                                "第一学期",
                                "第二学期"
                        }
                );


        JTextField dueDateField =
                new JTextField(
                        "2026-09-30"
                );


        JPanel panel =
                new JPanel(
                        new GridLayout(
                                3,
                                2,
                                8,
                                8
                        )
                );


        panel.add(
                new JLabel("学年：")
        );


        panel.add(
                academicYearField
        );


        panel.add(
                new JLabel("学期：")
        );


        panel.add(
                semesterBox
        );


        panel.add(
                new JLabel("缴费截止日期：")
        );


        panel.add(
                dueDateField
        );


        int confirm =
                JOptionPane.showConfirmDialog(
                        this,
                        panel,
                        "批量导入住宿信息",
                        JOptionPane.OK_CANCEL_OPTION,
                        JOptionPane.PLAIN_MESSAGE
                );


        if (confirm != JOptionPane.OK_OPTION) {

            return;
        }


        String academicYear =
                academicYearField
                        .getText()
                        .trim();


        String semester =
                String.valueOf(
                        semesterBox
                                .getSelectedItem()
                );


        String dueDateText =
                dueDateField
                        .getText()
                        .trim();


        if (academicYear.isEmpty()
                || semester.isEmpty()
                || dueDateText.isEmpty()) {

            JOptionPane.showMessageDialog(
                    this,
                    "学年、学期和缴费截止日期不能为空"
            );

            return;
        }


        Date dueDate;


        try {

            dueDate =
                    new SimpleDateFormat(
                            "yyyy-MM-dd"
                    ).parse(
                            dueDateText
                    );

        } catch (Exception e) {

            JOptionPane.showMessageDialog(
                    this,
                    "缴费截止日期格式错误，请使用 yyyy-MM-dd"
            );

            return;
        }


        int confirmImport =
                JOptionPane.showConfirmDialog(
                        this,

                        "确认导入 "
                                + rows.size()
                                + " 条住宿记录？\n\n"
                                + "学年："
                                + academicYear
                                + "\n"
                                + "学期："
                                + semester
                                + "\n"
                                + "缴费截止日期："
                                + dueDateText,

                        "确认批量导入",

                        JOptionPane.YES_NO_OPTION
                );


        if (
                confirmImport
                != JOptionPane.YES_OPTION
        ) {

            return;
        }


        Message response =
                DormClientSrv.batchCheckIn(
                        rows,
                        academicYear,
                        semester,
                        dueDate
                );


        JOptionPane.showMessageDialog(
                this,
                response.getResponseMsg(),
                "批量导入结果",
                response.isSuccess()
                        ? JOptionPane.INFORMATION_MESSAGE
                        : JOptionPane.ERROR_MESSAGE
        );


        if (response.isSuccess()) {

            sidField.setText("");

            queryStudent();
        }
    }


    /**
     * 读取 Excel
     *
     * Excel格式：
     *
     * 第一行：
     * 学生学号 | 宿舍编号 | 床位号 | 住宿费
     *
     * 第二行开始：
     * 具体住宿数据
     */
    private List<AccommodationImportRow> readExcel(
            File file) {

        List<AccommodationImportRow> rows =
                new ArrayList<>();


        try (
                FileInputStream fis =
                        new FileInputStream(file);

                Workbook workbook =
                        WorkbookFactory.create(fis)
        ) {

            Sheet sheet =
                    workbook.getSheetAt(0);


            DataFormatter formatter =
                    new DataFormatter();


            for (
                    int i = 1;
                    i <= sheet.getLastRowNum();
                    i++
            ) {

                Row row =
                        sheet.getRow(i);


                if (row == null) {

                    continue;
                }


                String sId =
                        formatter
                                .formatCellValue(
                                        row.getCell(0)
                                )
                                .trim();


                String dormId =
                        formatter
                                .formatCellValue(
                                        row.getCell(1)
                                )
                                .trim();


                String bedNumber =
                        formatter
                                .formatCellValue(
                                        row.getCell(2)
                                )
                                .trim();


                String feeText =
                        formatter
                                .formatCellValue(
                                        row.getCell(3)
                                )
                                .trim();


                if (
                        sId.isEmpty()
                        && dormId.isEmpty()
                        && bedNumber.isEmpty()
                        && feeText.isEmpty()
                ) {

                    continue;
                }


                if (
                        sId.isEmpty()
                        || dormId.isEmpty()
                        || bedNumber.isEmpty()
                        || feeText.isEmpty()
                ) {

                    continue;
                }


                double fee =
                        Double.parseDouble(
                                feeText
                        );


                AccommodationImportRow importRow =
                        new AccommodationImportRow(
                                sId,
                                dormId,
                                bedNumber,
                                fee
                        );


                rows.add(
                        importRow
                );
            }


        } catch (Exception e) {

            JOptionPane.showMessageDialog(
                    this,
                    "Excel读取失败："
                            + e.getMessage(),
                    "错误",
                    JOptionPane.ERROR_MESSAGE
            );

            return null;
        }


        return rows;
    }


    /**
     * 办理入住
     */
    private void checkIn() {

        JTextField sid =
                new JTextField();


        JTextField dormId =
                new JTextField();


        JTextField bedNumber =
                new JTextField();


        Object[] fields = {

                "学生学号：",
                sid,

                "宿舍编号：",
                dormId,

                "床位号：",
                bedNumber
        };


        int result =
                JOptionPane.showConfirmDialog(
                        this,
                        fields,
                        "办理入住",
                        JOptionPane.OK_CANCEL_OPTION
                );


        if (
                result
                != JOptionPane.OK_OPTION
        ) {

            return;
        }


        String sId =
                sid.getText()
                        .trim();


        String dorm =
                dormId.getText()
                        .trim();


        String bed =
                bedNumber.getText()
                        .trim();


        if (
                sId.isEmpty()
                || dorm.isEmpty()
                || bed.isEmpty()
        ) {

            JOptionPane.showMessageDialog(
                    this,
                    "请完整填写入住信息"
            );

            return;
        }


        Message response =
                DormClientSrv.checkIn(
                        sId,
                        dorm,
                        bed
                );


        JOptionPane.showMessageDialog(
                this,
                response.getResponseMsg()
        );


        if (response.isSuccess()) {

            sidField.setText("");

            queryStudent();
        }
    }


    /**
     * 办理退宿
     */
    private void checkOut() {

        String sId =
                sidField
                        .getText()
                        .trim();


        if (sId.isEmpty()) {

            JOptionPane.showMessageDialog(
                    this,
                    "请先输入学生学号"
            );

            return;
        }


        int confirm =
                JOptionPane.showConfirmDialog(
                        this,
                        "确认办理该学生的退宿？",
                        "确认退宿",
                        JOptionPane.YES_NO_OPTION
                );


        if (
                confirm
                != JOptionPane.YES_OPTION
        ) {

            return;
        }


        Message response =
                DormClientSrv.checkOut(
                        sId
                );


        JOptionPane.showMessageDialog(
                this,
                response.getResponseMsg()
        );


        if (response.isSuccess()) {

            sidField.setText("");

            queryStudent();
        }
    }


    /**
     * 调换宿舍
     */
    private void changeDorm() {

        String sId =
                sidField
                        .getText()
                        .trim();


        if (sId.isEmpty()) {

            JOptionPane.showMessageDialog(
                    this,
                    "请先输入学生学号"
            );

            return;
        }


        JTextField dormId =
                new JTextField();


        JTextField bedNumber =
                new JTextField();


        Object[] fields = {

                "新宿舍编号：",
                dormId,

                "新床位号：",
                bedNumber
        };


        int result =
                JOptionPane.showConfirmDialog(
                        this,
                        fields,
                        "调换宿舍",
                        JOptionPane.OK_CANCEL_OPTION
                );


        if (
                result
                != JOptionPane.OK_OPTION
        ) {

            return;
        }


        String newDormId =
                dormId
                        .getText()
                        .trim();


        String newBedNumber =
                bedNumber
                        .getText()
                        .trim();


        if (
                newDormId.isEmpty()
                || newBedNumber.isEmpty()
        ) {

            JOptionPane.showMessageDialog(
                    this,
                    "请完整填写新宿舍信息"
            );

            return;
        }


        Message response =
                DormClientSrv.changeDorm(
                        sId,
                        newDormId,
                        newBedNumber
                );


        JOptionPane.showMessageDialog(
                this,
                response.getResponseMsg()
        );


        if (response.isSuccess()) {

            sidField.setText("");

            queryStudent();
        }
    }
}