package vCampus.client;


import vCampus.common.Message;
import vCampus.common.vo.Admin;
import vCampus.common.vo.DormApplication;


import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;



/**
 * 宿舍申请审批管理
 *
 * 管理员功能：
 * 1. 查看待审批申请
 * 2. 同意申请
 * 3. 拒绝申请
 */
public class DormApplicationPanel extends JPanel {


    /**
     * 当前管理员
     */
    private Admin admin;



    /**
     * 表格
     */
    private JTable table;



    /**
     * 表格模型
     */
    private DefaultTableModel model;


    /**
     * 审批成功后刷新入住管理
     */
    private Runnable accommodationRefresh;


    public DormApplicationPanel(Admin admin){
        this(admin, null);
    }


    public DormApplicationPanel(
            Admin admin,
            Runnable accommodationRefresh){

        this.admin = admin;
        this.accommodationRefresh = accommodationRefresh;

        initUI();

        loadApplications();

    }




    /**
     * 初始化界面
     */
    private void initUI() {

        setLayout(new BorderLayout());

        // =========================
        // 表格
        // =========================
        String[] columns = {
                "申请ID",
                "学生ID",
                "申请类型",
                "当前宿舍",
                "当前床位",
                "目标宿舍",
                "目标床位",
                "申请原因",
                "状态",
                "提交时间"
        };

        model =
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
                        model
                );

        table.setRowHeight(30);

        add(
                new JScrollPane(table),
                BorderLayout.CENTER
        );


        // =========================
        // 底部按钮
        // =========================
        JPanel bottomPanel = new JPanel();

        JButton approveButton =
                new JButton("通过申请");

        JButton rejectButton =
                new JButton("拒绝申请");

        bottomPanel.add(approveButton);
        bottomPanel.add(rejectButton);

        add(
                bottomPanel,
                BorderLayout.SOUTH
        );


        // =========================
        // 事件
        // =========================

        // 通过申请
        approveButton.addActionListener(
                e -> approve()
        );

        // 拒绝申请
        rejectButton.addActionListener(
                e -> reject()
        );
    }




    /**
     * 加载待审批申请
     */
    public void refreshData() {
        loadApplications();
    }


    private void loadApplications(){


        model.setRowCount(0);



        List<DormApplication> list =
                DormClientSrv.queryPendingApplications();



        if(list == null){

            return;

        }




        for(DormApplication app:list){


            model.addRow(
                    new Object[]{


                            app.getApplicationId(),


                            app.getsId(),


                            app.getType(),


                            app.getCurrentDormId(),


                            app.getCurrentBedNumber(),


                            app.getTargetDormId(),


                            app.getTargetBedNumber(),


                            app.getReason(),


                            app.getStatus(),


                            app.getSubmitTime()


                    }
            );


        }



    }






    /**
     * 获取当前选择申请ID
     */
    private int getSelectedApplicationId() {

        int row =
                table.getSelectedRow();

        if(row == -1){

            JOptionPane.showMessageDialog(
                    this,
                    "请选择申请记录"
            );

            return -1;
        }

        return Integer.parseInt(
                model.getValueAt(
                        row,
                        0
                ).toString()
        );
    }




    /**
     * 审批通过
     */
    private void approve() {

        int applicationId =
                getSelectedApplicationId();

        if(applicationId == -1){
            return;
        }

        String remark =
                JOptionPane.showInputDialog(
                        this,
                        "请输入审批意见"
                );

        // 用户取消输入
        if(remark == null){
            return;
        }

        Message message =
                DormClientSrv.approveApplication(
                        applicationId,
                        admin.getACard(),
                        remark
                );

        JOptionPane.showMessageDialog(
                this,
                message.getResponseMsg()
        );

        // 审批成功后自动刷新
        if(message.isSuccess()){
            loadApplications();

            if(accommodationRefresh != null){
                accommodationRefresh.run();
            }
        }
    }






    /**
     * 审批拒绝
     */
    private void reject() {

        int applicationId =
                getSelectedApplicationId();

        if(applicationId == -1){
            return;
        }

        String remark =
                JOptionPane.showInputDialog(
                        this,
                        "请输入拒绝原因"
                );

        // 用户取消输入
        if(remark == null){
            return;
        }

        Message message =
                DormClientSrv.rejectApplication(
                        applicationId,
                        admin.getACard(),
                        remark
                );

        JOptionPane.showMessageDialog(
                this,
                message.getResponseMsg()
        );

        // 拒绝成功后自动刷新
        if(message.isSuccess()){
            loadApplications();
        }
    }
}