package vCampus.client;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

import vCampus.common.Message;
import vCampus.common.vo.DormFeeBill;


/**
 * 管理员住宿费管理Panel
 *
 * 功能：
 * 1. 查询全部住宿费账单
 * 2. 调整账单金额
 * 3. 作废账单
 */
public class DormFeePanel extends JPanel {


    private JTable table;

    private DefaultTableModel tableModel;


    private JButton adjustButton;

    private JButton cancelButton;


    private Object currentUser;



    public DormFeePanel(Object user){

        this.currentUser = user;

        initUI();

        loadBills();

    }




    private void initUI(){


        setLayout(
                new BorderLayout()
        );



        String[] columns = {

                "账单编号",
                "学生一卡通",
                "住宿记录ID",
                "学年",
                "学期",
                "应缴金额",
                "已缴金额",
                "截止日期",
                "状态"

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



        JPanel buttonPanel =
                new JPanel();


        adjustButton =
                new JButton("调整金额");


        cancelButton =
                new JButton("作废账单");

        buttonPanel.add(adjustButton);

        buttonPanel.add(cancelButton);



        add(
                buttonPanel,
                BorderLayout.SOUTH
        );






        adjustButton.addActionListener(e -> {

            adjustBill();

        });



        cancelButton.addActionListener(e -> {

            cancelBill();

        });

    }







    /**
     * 对外提供刷新账单数据的方法
     *
     * 用于管理员从其他页面完成操作后，
     * 重新查询数据库中的最新账单。
     */
    public void refreshData(){

        loadBills();

    }







    /**
     * 查询全部账单
     */
    private void loadBills(){


        try{


            tableModel.setRowCount(0);



            List<DormFeeBill> bills =
                    DormClientSrv.queryAllBills();



            if(bills == null){

                return;

            }



            for(DormFeeBill bill : bills){


                tableModel.addRow(
                        new Object[]{

                                bill.getBillId(),

                                bill.getsId(),

                                bill.getAccommodationId(),

                                bill.getAcademicYear(),

                                bill.getSemester(),

                                bill.getAmount(),

                                bill.getPaidAmount(),

                                bill.getDueDate(),

                                bill.getStatus()


                        }
                );


            }



        }catch(Exception e){


            e.printStackTrace();


            JOptionPane.showMessageDialog(
                    this,
                    "加载账单失败："
                    + e.getMessage()
            );

        }

    }







    /**
     * 调整账单金额
     */
    private void adjustBill(){


        int row =
                table.getSelectedRow();



        if(row < 0){


            JOptionPane.showMessageDialog(
                    this,
                    "请选择账单"
            );


            return;

        }



        try{


            int billId =
                    Integer.parseInt(
                            tableModel.getValueAt(
                                    row,
                                    0
                            ).toString()
                    );



            String amountText =
                    JOptionPane.showInputDialog(
                            this,
                            "请输入新的金额"
                    );



            if(amountText == null
                    || amountText.trim().isEmpty()){

                return;

            }



            double amount =
                    Double.parseDouble(
                            amountText
                    );



            Message message =
                    DormClientSrv.adjustBill(
                            billId,
                            amount
                    );



            showMessage(message);



            loadBills();



        }catch(Exception e){


            e.printStackTrace();


            JOptionPane.showMessageDialog(
                    this,
                    "调整失败："
                    + e.getMessage()
            );

        }

    }







    /**
     * 作废账单
     */
    private void cancelBill(){


        int row =
                table.getSelectedRow();



        if(row < 0){


            JOptionPane.showMessageDialog(
                    this,
                    "请选择账单"
            );


            return;

        }



        int result =
                JOptionPane.showConfirmDialog(
                        this,
                        "确认作废该账单？",
                        "提示",
                        JOptionPane.YES_NO_OPTION
                );



        if(result != JOptionPane.YES_OPTION){

            return;

        }



        try{


            int billId =
                    Integer.parseInt(
                            tableModel.getValueAt(
                                    row,
                                    0
                            ).toString()
                    );



            Message message =
                    DormClientSrv.cancelBill(
                            billId
                    );



            showMessage(message);



            loadBills();



        }catch(Exception e){


            e.printStackTrace();


            JOptionPane.showMessageDialog(
                    this,
                    "作废失败："
                    + e.getMessage()
            );

        }

    }







    /**
     * 统一消息提示
     */
    private void showMessage(
            Message message){


        if(message == null){

            JOptionPane.showMessageDialog(
                    this,
                    "服务器无响应"
            );


            return;

        }



        JOptionPane.showMessageDialog(
                this,
                message.getResponseMsg()
        );

    }

}