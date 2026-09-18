package vCampus.client;


import vCampus.common.vo.Admin;


import javax.swing.*;
import java.awt.*;


public class AdminDormPanel extends JPanel {


    private Admin admin;


    public AdminDormPanel(Admin admin){

        this.admin = admin;

        initUI();

    }


    private void initUI(){

        setLayout(
                new BorderLayout()
        );


        JLabel title =
                new JLabel(
                        "宿舍管理系统",
                        SwingConstants.CENTER
                );


        title.setFont(
                new Font(
                        "微软雅黑",
                        Font.BOLD,
                        22
                )
        );


        add(
                title,
                BorderLayout.NORTH
        );


        JTabbedPane tabs =
                new JTabbedPane();


        tabs.addTab(
                "宿舍信息",
                new DormInfoPanel()
        );


        AccommodationPanel accommodationPanel =
                new AccommodationPanel();

        tabs.addTab(
                "入住管理",
                accommodationPanel
        );


        DormApplicationPanel applicationPanel =
                new DormApplicationPanel(
                        admin,
                        accommodationPanel::refreshData
                );

        tabs.addTab(
                "申请审批",
                applicationPanel
        );


        DormRepairPanel repairPanel =
                new DormRepairPanel(admin);

        tabs.addTab(
                "报修管理",
                repairPanel
        );


        DormFeePanel feePanel =
                new DormFeePanel(admin);

        tabs.addTab(
                "费用管理",
                feePanel
        );


        tabs.addChangeListener(e -> {

            int index =
                    tabs.getSelectedIndex();


            if(index == 1){

                accommodationPanel.refreshData();

            }


            if(index == 2){

                applicationPanel.refreshData();

            }


            if(index == 3){

                repairPanel.refreshData();

            }


            if(index == 4){

                feePanel.refreshData();

            }

        });


        Timer refreshTimer =
                new Timer(
                        3000,
                        e -> {

                            int index =
                                    tabs.getSelectedIndex();


                            if(index == 1){

                                accommodationPanel.refreshData();

                            }


                            if(index == 2){

                                applicationPanel.refreshData();

                            }


                            if(index == 3){

                                repairPanel.refreshData();

                            }


                            if(index == 4){

                                feePanel.refreshData();

                            }

                        }
                );


        refreshTimer.start();


        add(
                tabs,
                BorderLayout.CENTER
        );

    }

}