package vCampus.client;

import vCampus.common.Message;
import vCampus.common.vo.Dormitory;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

public class DormInfoPanel extends JPanel {

    private JTable table;

    private DefaultTableModel tableModel;

    public DormInfoPanel() {

        initUI();

        loadDorm();
    }


    private void initUI() {

        setLayout(
                new BorderLayout()
        );


        String[] columns = {

                "宿舍编号",
                "楼栋",
                "房间号",
                "性别",
                "容量",
                "入住人数",
                "剩余床位",
                "住宿费"

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
                new JTable(tableModel);


        add(
                new JScrollPane(table),
                BorderLayout.CENTER
        );


        JPanel buttonPanel =
                new JPanel();


        JButton add =
                new JButton("新增");


        JButton delete =
                new JButton("删除");


        buttonPanel.add(add);

        buttonPanel.add(delete);


        add(
                buttonPanel,
                BorderLayout.SOUTH
        );


        add.addActionListener(
                e -> addDorm()
        );


        delete.addActionListener(
                e -> deleteDorm()
        );

    }


    /**
     * 查询宿舍
     */
    private void loadDorm() {

        tableModel.setRowCount(0);


        List<Dormitory> list =
                DormClientSrv.adminQueryAllDorm();


        if (list == null)
            return;


        for (Dormitory dorm : list) {

            String genderText;

            if ("MALE".equalsIgnoreCase(dorm.getGender())) {
                genderText = "男寝";
            } else if ("FEMALE".equalsIgnoreCase(dorm.getGender())) {
                genderText = "女寝";
            } else {
                genderText = "";
            }


            tableModel.addRow(

                    new Object[]{

                            dorm.getDormId(),

                            dorm.getBuilding(),

                            dorm.getRoomNumber(),

                            genderText,

                            dorm.getCapacity(),

                            dorm.getOccupied(),

                            dorm.getRemainingBeds(),

                            dorm.getFeePerSemester()

                    }

            );

        }

    }


    /**
     * 新增宿舍
     */
    private void addDorm() {

        JTextField id =
                new JTextField();


        JTextField building =
                new JTextField();


        JTextField room =
                new JTextField();


        JComboBox<String> gender =
                new JComboBox<>(
                        new String[]{
                                "男寝",
                                "女寝"
                        }
                );


        JTextField capacity =
                new JTextField();


        JTextField fee =
                new JTextField();


        Object[] fields = {

                "宿舍编号",
                id,

                "楼栋",
                building,

                "房间号",
                room,

                "性别",
                gender,

                "容量",
                capacity,

                "费用",
                fee

        };


        int result =
                JOptionPane.showConfirmDialog(
                        this,

                        fields,

                        "新增宿舍",

                        JOptionPane.OK_CANCEL_OPTION
                );


        if (result == JOptionPane.OK_OPTION) {

            Dormitory dorm =
                    new Dormitory();


            dorm.setDormId(
                    id.getText()
            );


            dorm.setBuilding(
                    building.getText()
            );


            dorm.setRoomNumber(
                    room.getText()
            );


            String genderValue;

            if ("男寝".equals(
                    gender.getSelectedItem())) {

                genderValue = "MALE";

            } else {

                genderValue = "FEMALE";

            }


            dorm.setGender(
                    genderValue
            );


            dorm.setCapacity(
                    Integer.parseInt(
                            capacity.getText()
                    )
            );


            dorm.setFeePerSemester(
                    Double.parseDouble(
                            fee.getText()
                    )
            );


            Message msg =
                    DormClientSrv.addDorm(dorm);


            JOptionPane.showMessageDialog(
                    this,

                    msg.getResponseMsg()
            );


            loadDorm();

        }

    }


    /**
     * 删除宿舍
     */
    private void deleteDorm() {

        int row =
                table.getSelectedRow();


        if (row == -1) {

            JOptionPane.showMessageDialog(
                    this,

                    "请选择宿舍"
            );

            return;

        }


        String dormId =
                tableModel.getValueAt(
                        row,
                        0
                ).toString();


        Message msg =
                DormClientSrv.deleteDorm(
                        dormId
                );


        JOptionPane.showMessageDialog(
                this,

                msg.getResponseMsg()
        );


        loadDorm();

    }

}