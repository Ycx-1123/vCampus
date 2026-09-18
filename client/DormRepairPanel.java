package vCampus.client;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

import vCampus.common.Message;
import vCampus.common.enums.RepairStatus;
import vCampus.common.vo.DormRepair;


/**
 * 管理员宿舍报修管理Panel
 *
 * 功能：
 * 1. 查询全部报修
 * 2. 修改维修状态
 *
 */
public class DormRepairPanel extends JPanel {


    private JTable table;

    private DefaultTableModel tableModel;


    private JButton refreshButton;

    private JButton processButton;

    private JButton finishButton;


    private Object currentUser;



    public DormRepairPanel(Object user){

        this.currentUser = user;

        initUI();

        loadRepairs();

    }





    /**
     * 初始化界面
     */
    private void initUI(){


        setLayout(
                new BorderLayout()
        );



        // ============================
        // 表格
        // ============================

        String[] columns = {

                "报修编号",
                "学生一卡通",
                "宿舍编号",
                "问题描述",
                "报修状态"

        };


        tableModel =
                new DefaultTableModel(
                        columns,
                        0
                ){

                    @Override
                    public boolean isCellEditable(
                            int row,
                            int column){

                        return false;

                    }

                };



        table =
                new JTable(tableModel);



        table.setRowHeight(28);



        add(
                new JScrollPane(table),
                BorderLayout.CENTER
        );






        // ============================
        // 底部按钮
        // ============================


        JPanel buttonPanel =
                new JPanel();


        processButton =
                new JButton("处理中");


        finishButton =
                new JButton("维修完成");





        buttonPanel.add(
                processButton
        );


        buttonPanel.add(
                finishButton
        );



        add(
                buttonPanel,
                BorderLayout.SOUTH
        );






        // ============================
        // 事件监听
        // ============================


        processButton.addActionListener(e -> {

            updateRepairStatus(
                    RepairStatus.PROCESSING
            );

        });



        finishButton.addActionListener(e -> {

            updateRepairStatus(
                    RepairStatus.FINISHED
            );

        });

    }







    /**
     * 加载全部报修
     */
    public void refreshData() {
        loadRepairs();
    }


    private void loadRepairs(){


        try {


            tableModel.setRowCount(0);



            List<DormRepair> repairs =
                    DormClientSrv.queryAllRepairs();



            if(repairs == null){

                return;

            }




            for(DormRepair repair : repairs){


                tableModel.addRow(
                        new Object[]{


                                repair.getRepairId(),


                                repair.getsId(),


                                repair.getDormId(),


                                repair.getDescription(),


                                repair.getStatus()


                        }
                );

            }



        }catch(Exception e){


            e.printStackTrace();



            JOptionPane.showMessageDialog(
                    this,
                    "加载报修数据失败："
                            + e.getMessage()
            );

        }


    }








    /**
     * 修改维修状态
     */
    private void updateRepairStatus(
            RepairStatus status){



        int row =
                table.getSelectedRow();



        if(row < 0){


            JOptionPane.showMessageDialog(
                    this,
                    "请选择一条报修记录"
            );


            return;

        }






        try {


            int repairId =
                    Integer.parseInt(
                            tableModel.getValueAt(
                                    row,
                                    0
                            ).toString()
                    );





            Message message =
                    DormClientSrv.updateRepairStatus(
                            repairId,
                            status
                    );





            if(message != null){

                JOptionPane.showMessageDialog(
                        this,
                        message.getResponseMsg()
                );

            }



            loadRepairs();





        }catch(Exception e){


            e.printStackTrace();



            JOptionPane.showMessageDialog(
                    this,
                    "修改状态失败："
                            + e.getMessage()
            );

        }

    }

}