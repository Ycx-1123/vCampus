package vCampus.client;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;
import vCampus.common.Message;
import vCampus.common.consts.MsgConst;
import vCampus.common.vo.Student;
import vCampus.common.vo.Clazz;
import vCampus.common.vo.Major;
import vCampus.common.vo.Department;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.util.HashMap;
import java.util.Map;


public class AdminStudentPanel extends JPanel {
    //【升级 全套绿色同色系配色】
    private static final Color HEADER_GREEN = new Color(20, 120, 100);
    private static final Color HEADER_LIGHT_GREEN = new Color(235, 245, 240);
    private static final Color PAGE_BG = new Color(247, 249, 250);
    private static final Color CARD_BG = Color.WHITE;
    private static final Color BORDER_COLOR = new Color(210, 225, 220);
    private static final Color BTN_GREEN = new Color(70, 160, 100);
    private static final Color TABLE_HEADER_GREEN = new Color(220, 240, 230);

    public AdminStudentPanel() {
        setBackground(PAGE_BG);
        setLayout(new BorderLayout(10, 10));
        setBorder(new EmptyBorder(15,15,15,15));

        //顶部大标题
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setBackground(HEADER_GREEN);
        JLabel title = new JLabel("学籍管理中心", SwingConstants.CENTER);
        title.setFont(new Font("微软雅黑",Font.BOLD,22));
        title.setForeground(Color.WHITE);
        topPanel.add(title,BorderLayout.CENTER);
        add(topPanel,BorderLayout.NORTH);

        //四标签页容器
        JTabbedPane tabPane = new JTabbedPane();

        tabPane.addTab("学籍管理", new StudentInnerPanel());
        tabPane.addTab("班级管理", new ClazzInnerPanel());
        tabPane.addTab("专业管理", new MajorInnerPanel());
        tabPane.addTab("院系管理", new DeptInnerPanel());

        add(tabPane,BorderLayout.CENTER);
    }

  //==================== 标签页1：学籍管理（已对接Socket‑数据库） ====================
    private class StudentInnerPanel extends JPanel{
        private JTable studentTable;
        private DefaultTableModel tableModel;
        private JTextField txtSCard;
        private JTextField txtStuid;
        private JTextField txtName;
        private JTextField txtEnrollYear;
        private JTextField txtClassId;
        private JTextField txtDept;
        private JTextField txtMajor;
        private JTextField txtCreditGot;
        private JTextField txtCreditNeed;
        private JTextField txtGender;
        private JTextField txtPhone;
        private JTextField txtEmail;
        private JTextField txtAddress;
        private JCheckBox chkLocked;
        private Student selectedStudent;

        // ★ 新增：查询栏控件
        private JComboBox<String> cmbSearchType;
        private JTextField txtSearchKey;

        public StudentInnerPanel(){
            setBackground(PAGE_BG);
            setLayout(new BorderLayout(10,10));

            JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
            splitPane.setDividerLocation(400);

            //左侧学生列表
            JPanel leftPanel = new JPanel(new BorderLayout());
            leftPanel.setBackground(CARD_BG);
            leftPanel.setBorder(BorderFactory.createTitledBorder("学生列表"));

            // ★ 新增：搜索栏
            JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 6));
            searchPanel.setBackground(CARD_BG);
            cmbSearchType = new JComboBox<>(new String[]{"按一卡通号", "按班级编号"});
            txtSearchKey = new JTextField(14);
            JButton btnSearch = new JButton("查询");
            JButton btnShowAll = new JButton("显示全部");
            styleButton(btnSearch);
            styleButton(btnShowAll);
            btnSearch.setPreferredSize(new Dimension(80, 30));
            btnShowAll.setPreferredSize(new Dimension(90, 30));
            searchPanel.add(cmbSearchType);
            searchPanel.add(txtSearchKey);
            searchPanel.add(btnSearch);
            searchPanel.add(btnShowAll);
            leftPanel.add(searchPanel, BorderLayout.NORTH);

            String[] columnNames = {"一卡通号","学号","姓名","班级编号","学籍锁定"};
            tableModel = new DefaultTableModel(columnNames,0);
            studentTable = new JTable(tableModel);
            studentTable.getTableHeader().setBackground(TABLE_HEADER_GREEN);
            studentTable.getTableHeader().setForeground(HEADER_GREEN);
            JScrollPane scrollPane = new JScrollPane(studentTable);
            leftPanel.add(scrollPane,BorderLayout.CENTER);
            splitPane.setLeftComponent(leftPanel);

            //右侧详情表单
            JPanel rightPanel = new JPanel();
            rightPanel.setBackground(HEADER_LIGHT_GREEN);
            rightPanel.setBorder(BorderFactory.createCompoundBorder(
                    new LineBorder(BORDER_COLOR,1),
                    new EmptyBorder(12,12,12,12)
            ));
            rightPanel.setLayout(new GridBagLayout());
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.insets = new Insets(8,8,8,8);
            gbc.anchor = GridBagConstraints.WEST;

            txtSCard = new JTextField(18);
            txtSCard.setEditable(false);
            txtStuid = new JTextField(18);
            txtName = new JTextField(18);
            txtEnrollYear = new JTextField(18);
            txtClassId = new JTextField(18);
            txtDept = new JTextField(18);
            txtDept.setEditable(false);  // 院系只读，由班级联动带出
            txtMajor = new JTextField(18);
            txtMajor.setEditable(false); // 专业只读，由班级联动带出
            txtCreditGot = new JTextField(18);
            txtCreditGot.setEditable(false); // ★ 已修学分只读，由服务端 CreditService 统计得出
            txtCreditNeed = new JTextField(18);
            txtCreditNeed.setEditable(false); // 所需学分只读，由培养方案联动带出
            txtGender = new JTextField(18);
            txtPhone = new JTextField(18);
            txtEmail = new JTextField(18);
            txtAddress = new JTextField(18);
            chkLocked = new JCheckBox("学籍已锁定");

            int row = 0;
            addFormRow(rightPanel,gbc,"一卡通账号：",txtSCard,row++);
            addFormRow(rightPanel,gbc,"学号：",txtStuid,row++);
            addFormRow(rightPanel,gbc,"姓名：",txtName,row++);
            addFormRow(rightPanel,gbc,"入学年份：",txtEnrollYear,row++);
            addFormRow(rightPanel,gbc,"班级编号：",txtClassId,row++);
            addFormRow(rightPanel,gbc,"院系：",txtDept,row++);
            addFormRow(rightPanel,gbc,"专业：",txtMajor,row++);
            addFormRow(rightPanel,gbc,"已修学分：",txtCreditGot,row++);
            addFormRow(rightPanel,gbc,"所需学分：",txtCreditNeed,row++);
            addFormRow(rightPanel,gbc,"性别：",txtGender,row++);
            addFormRow(rightPanel,gbc,"电话：",txtPhone,row++);
            addFormRow(rightPanel,gbc,"电子邮箱：",txtEmail,row++);
            addFormRow(rightPanel,gbc,"地址：",txtAddress,row++);

            // ★ 学籍状态：只读展示（禁用，不可点击）
            gbc.gridx = 0; gbc.gridy = row;
            rightPanel.add(new JLabel("学籍状态："), gbc);
            gbc.gridx = 1;
            chkLocked.setEnabled(false);      // 不可交互
            chkLocked.setOpaque(false);       // 背景透明，视觉更像展示
            rightPanel.add(chkLocked, gbc);

            splitPane.setRightComponent(new JScrollPane(rightPanel));
            add(splitPane,BorderLayout.CENTER);
            
            
            //底部按钮栏
            JPanel btnPanel = new JPanel();
            btnPanel.setBackground(PAGE_BG);
            JButton btnRefresh = new JButton("刷新列表");
            JButton btnSave = new JButton("保存修改");
            JButton btnLock = new JButton("锁定学籍");
            JButton btnUnlock = new JButton("解锁学籍");
            JButton btnAdd = new JButton("新增学生");
            JButton btnDelete = new JButton("删除学生");
            JButton btnImportExcel = new JButton("Excel导入");
            styleButton(btnRefresh);
            styleButton(btnSave);
            styleButton(btnLock);
            styleButton(btnUnlock);
            styleButton(btnAdd);
            styleButton(btnDelete);
            styleButton(btnImportExcel);
            btnPanel.add(btnRefresh);
            btnPanel.add(btnSave);
            btnPanel.add(btnLock);
            btnPanel.add(btnUnlock);
            btnPanel.add(btnAdd);
            btnPanel.add(btnDelete);
            btnPanel.add(btnImportExcel);
            add(btnPanel,BorderLayout.SOUTH);

            //表格选中事件
            studentTable.getSelectionModel().addListSelectionListener(e->{
                if(!e.getValueIsAdjusting()){
                    int selectedRow = studentTable.getSelectedRow();
                    if(selectedRow >= 0){
                        String sCard = (String)tableModel.getValueAt(selectedRow,0);
                        loadStudentInfo(sCard);
                    }
                }
            });

            //按钮事件绑定
            btnRefresh.addActionListener(e -> refreshStudentList());
            btnSave.addActionListener(e -> saveStudentEdit());
            btnLock.addActionListener(e -> lockStudent());
            btnUnlock.addActionListener(e -> unlockStudent());
            btnAdd.addActionListener(e -> addNewStudent());
            btnDelete.addActionListener(e -> deleteStudent());
            btnImportExcel.addActionListener(e -> importStudentExcel());

            // ★ 新增：查询事件绑定
            btnSearch.addActionListener(e -> doSearchStudent());
            btnShowAll.addActionListener(e -> refreshStudentList());
            txtSearchKey.addActionListener(e -> doSearchStudent()); // 回车触发查询
            cmbSearchType.addActionListener(e -> txtSearchKey.setText("")); // 切换类型时清空输入

            SwingUtilities.invokeLater(this::refreshStudentList);
        }

        private void addFormRow(JPanel panel,GridBagConstraints gbc,String labelText,JTextField field,int row){
            gbc.gridx = 0;
            gbc.gridy = row;
            panel.add(new JLabel(labelText),gbc);
            gbc.gridx = 1;
            panel.add(field,gbc);
        }
        private void styleButton(JButton btn){
            btn.setBackground(BTN_GREEN);
            btn.setForeground(Color.WHITE);
            btn.setFocusPainted(false);
            btn.setBorder(new LineBorder(BTN_GREEN,1));
            btn.setFont(new Font("微软雅黑",Font.BOLD,14));
            btn.setPreferredSize(new Dimension(110,36));
        }

        // ★ 新增：根据下拉框选择的方式查询学生
        private void doSearchStudent(){
            String type = (String) cmbSearchType.getSelectedItem();
            String key = txtSearchKey.getText().trim();

            if("按一卡通号".equals(type)){
                // —— 按一卡通查单个学生 ——
                if(key.isEmpty()){
                    JOptionPane.showMessageDialog(this,"请输入一卡通号");
                    return;
                }
                new Thread(()->{
                    Message req = new Message();
                    req.setType(MsgConst.QUERY_STUDENT_BY_SCARD);
                    req.setData(key);
                    Message res = SocketClient.send(req);
                    SwingUtilities.invokeLater(()->{
                        if(res == null){
                            JOptionPane.showMessageDialog(this,"服务端无响应，请检查服务器是否启动");
                            return;
                        }
                        tableModel.setRowCount(0);
                        if(res.isSuccess() && res.getData() instanceof Student){
                            Student s = (Student) res.getData();
                            tableModel.addRow(new Object[]{
                                    s.getSCard(),
                                    s.getSId(),
                                    s.getSname(),
                                    s.getClazzid(),
                                    s.isLocked() ? "已锁定" : "未锁定"
                            });
                            // 自动选中该行，触发详情加载
                            if(studentTable.getRowCount() > 0){
                                studentTable.setRowSelectionInterval(0, 0);
                            }
                        }else{
                            JOptionPane.showMessageDialog(this,"未找到该学生："+res.getResponseMsg());
                        }
                    });
                }).start();
            }else{
                // —— 按班级编号查全班 ——
                if(key.isEmpty()){
                    JOptionPane.showMessageDialog(this,"请输入班级编号");
                    return;
                }
                new Thread(()->{
                    Message req = new Message();
                    req.setType(MsgConst.QUERY_STUDENT_BY_CID);
                    req.setData(key);
                    Message res = SocketClient.send(req);
                    SwingUtilities.invokeLater(()->{
                        if(res == null){
                            JOptionPane.showMessageDialog(this,"服务端无响应，请检查服务器是否启动");
                            return;
                        }
                        tableModel.setRowCount(0);
                        if(res.isSuccess() && res.getData() instanceof List<?>){
                            List<Student> stuList = (List<Student>) res.getData();
                            if(stuList.isEmpty()){
                                JOptionPane.showMessageDialog(this,"该班级暂无学生");
                                return;
                            }
                            for(Student s : stuList){
                                tableModel.addRow(new Object[]{
                                        s.getSCard(),
                                        s.getSId(),
                                        s.getSname(),
                                        s.getClazzid(),
                                        s.isLocked() ? "已锁定" : "未锁定"
                                });
                            }
                        }else{
                            JOptionPane.showMessageDialog(this,"查询失败："+res.getResponseMsg());
                        }
                    });
                }).start();
            }
        }

        private void fillFormData(Student stu){
            txtSCard.setText(stu.getSCard());
            txtStuid.setText(stu.getSId());
            txtName.setText(stu.getSname());
            txtEnrollYear.setText(stu.getEnrollYear());
            txtClassId.setText(stu.getClazzid());
            // ★ 已修学分不再从数据库取，先显示占位，等待 loadCompletedCredit 填入统计结果
            txtCreditGot.setText("统计中...");
            txtCreditNeed.setText(stu.getRequiredCredit()+""); // 先按数据库里的值显示，等待联动刷新
            txtGender.setText(stu.getSgender());
            txtPhone.setText(stu.getSphone());
            txtEmail.setText(stu.getSemail());
            txtAddress.setText(stu.getSaddress());
            chkLocked.setSelected(stu.isLocked());
        }

        /**
         * ★ 新增：调用服务端 CreditService.calcCompletedCredit 统计已修学分
         * 口径：非当前学期 + 成绩>=60 + 同课程重修取最高分
         */
        private void loadCompletedCredit(String sCard){
            if(sCard == null || sCard.trim().isEmpty()) return;
            new Thread(()->{
                Message req = new Message();
                req.setType(MsgConst.QUERY_COMPLETED_CREDIT); // 服务端 StudentHandler 处理
                Map<String,String> param = new HashMap<>();
                param.put("studentId", sCard.trim());
                param.put("semester", ""); // 空串 => 服务端自行取当前开放轮次学期
                req.setData(param);
                Message res = SocketClient.send(req);

                String creditText = "0";
                if(res != null && res.isSuccess() && res.getData() instanceof Map){
                    Map<String,Object> data = (Map<String,Object>) res.getData();
                    Object credit = data.get("completedCredit");
                    if(credit != null){
                        creditText = String.valueOf(credit);
                    }
                }
                final String finalCredit = creditText;
                SwingUtilities.invokeLater(()->{
                    txtCreditGot.setText(finalCredit);
                });
            }).start();
        }

        // 根据班级编号联动加载专业、院系信息，并进一步获取培养方案所需学分
        private void loadMajorDeptByClazz(String clazzId){
            if(clazzId == null || clazzId.trim().isEmpty()){
                txtMajor.setText("");
                txtDept.setText("");
                txtCreditNeed.setText(""); // 清空所需学分
                return;
            }
            new Thread(()->{
                String majorName = "";
                String deptName = "";
                String requiredCredits = "";

                // 1. 查询班级信息，获取专业ID
                Message reqClazz = new Message();
                reqClazz.setType(MsgConst.QUERY_CLAZZ_BY_CLAZZID);
                reqClazz.setData(clazzId.trim());
                Message resClazz = SocketClient.send(reqClazz);

                String majorId = null;
                if(resClazz != null && resClazz.isSuccess() && resClazz.getData() instanceof Clazz){
                    Clazz clazz = (Clazz) resClazz.getData();
                    majorId = clazz.getMajorid();
                }

                // 2. 查询专业信息，获取专业名+院系ID
                if(majorId != null && !majorId.trim().isEmpty()){
                    Message reqMajor = new Message();
                    reqMajor.setType(MsgConst.QUERY_MAJOR_BY_MAJORID);
                    reqMajor.setData(majorId.trim());
                    Message resMajor = SocketClient.send(reqMajor);

                    if(resMajor != null && resMajor.isSuccess() && resMajor.getData() instanceof Major){
                        Major major = (Major) resMajor.getData();
                        majorName = major.getMajorname();
                        String deptId = major.getDeptid();

                        // 3. 查询院系信息，获取院系名
                        if(deptId != null && !deptId.trim().isEmpty()){
                            Message reqDept = new Message();
                            reqDept.setType(MsgConst.QUERY_DEPT_BY_DEPTID);
                            reqDept.setData(deptId.trim());
                            Message resDept = SocketClient.send(reqDept);

                            if(resDept != null && resDept.isSuccess() && resDept.getData() instanceof Department){
                                Department dept = (Department) resDept.getData();
                                deptName = dept.getDeptname();
                            }
                        }
                    }
                }

                // 4. 查询培养方案，获取所需学分
                if (!majorName.isEmpty() && selectedStudent != null) {
                    String grade = selectedStudent.getEnrollYear(); // 获取入学年份
                    if (grade != null && !grade.trim().isEmpty()) {
                        Message reqProgram = new Message();
                        reqProgram.setType(MsgConst.QUERY_PROGRAM_BY_MAJOR_AND_GRADE);
                        Map<String, String> programParam = new HashMap<>();
                        programParam.put("major", majorName);
                        programParam.put("grade", grade);
                        reqProgram.setData(programParam);
                        Message resProgram = SocketClient.send(reqProgram);

                        // 服务端返回的直接是 Integer
                        if (resProgram != null && resProgram.isSuccess() && resProgram.getData() instanceof Integer) {
                            requiredCredits = String.valueOf(resProgram.getData());
                        } else {
                            requiredCredits = "0"; // 未找到培养方案时给默认值
                        }
                    }
                }

                final String finalMajor = majorName;
                final String finalDept = deptName;
                final String finalRequiredCredits = requiredCredits;
                SwingUtilities.invokeLater(()->{
                    txtMajor.setText(finalMajor);
                    txtDept.setText(finalDept);
                    txtCreditNeed.setText(finalRequiredCredits); // 填入所需学分
                });
            }).start();
        }

        //刷新学生列表
        private void refreshStudentList(){
            new Thread(()->{
                Message req = new Message();
                req.setType(MsgConst.QUERY_ALL_STUDENT);
                Message res = SocketClient.send(req);
                SwingUtilities.invokeLater(()->{
                    if(res == null){
                        JOptionPane.showMessageDialog(this,"服务端无响应，请检查服务器是否启动");
                        return;
                    }
                    if(res.isSuccess() && res.getData() instanceof List<?>){
                        tableModel.setRowCount(0);
                        List<Student> stuList = (List<Student>) res.getData();
                        for(Student s : stuList){
                            Object[] row = {
                                    s.getSCard(),
                                    s.getSId(),
                                    s.getSname(),
                                    s.getClazzid(),
                                    s.isLocked() ? "已锁定":"未锁定"
                            };
                            tableModel.addRow(row);
                        }
                    }else{
                        JOptionPane.showMessageDialog(this,"加载学生列表失败："+res.getResponseMsg());
                    }
                });
            }).start();
        }

        //查询单个学生详情
        private void loadStudentInfo(String sCard){
            new Thread(()->{
                Message req = new Message();
                req.setType(MsgConst.QUERY_STUDENT_BY_SCARD);
                req.setData(sCard);
                Message res = SocketClient.send(req);
                SwingUtilities.invokeLater(()->{
                    if(res == null){
                        JOptionPane.showMessageDialog(this,"服务端无响应");
                        return;
                    }
                    if(res.isSuccess() && res.getData() instanceof Student){
                        selectedStudent = (Student) res.getData();
                        fillFormData(selectedStudent);
                        // 加载完学生基本信息后，联动加载专业、院系以及培养方案所需学分
                        loadMajorDeptByClazz(selectedStudent.getClazzid());
                        // ★ 新增：向服务端请求已修学分统计并填入只读框
                        loadCompletedCredit(selectedStudent.getSCard());
                    }else{
                        JOptionPane.showMessageDialog(this,"未找到该学生："+res.getResponseMsg());
                    }
                });
            }).start();
        }

        //保存（新增/修改通用）
        private void saveStudentEdit(){
            // 基础非空校验
            if(txtSCard.getText().trim().isEmpty()){
                JOptionPane.showMessageDialog(this,"一卡通号不能为空");
                return;
            }
            if(txtName.getText().trim().isEmpty()){
                JOptionPane.showMessageDialog(this,"请填写学生姓名");
                return;
            }
            double got,need;
            try{
                // ★ txtCreditGot 只读，可能是"统计中..."占位，解析前先兜底
                String gotText = txtCreditGot.getText().trim();
                if(gotText.isEmpty() || "统计中...".equals(gotText)){
                    got = 0;
                }else{
                    got = Double.parseDouble(gotText);
                }
                need = txtCreditNeed.getText().trim().isEmpty() ? 0 : Double.parseDouble(txtCreditNeed.getText().trim());
            }catch (NumberFormatException e){
                JOptionPane.showMessageDialog(this,"学分必须输入有效数字");
                return;
            }
            // 封装学生数据
            Student s = new Student();
            s.setSCard(txtSCard.getText().trim());
            s.setSId(txtStuid.getText().trim());
            s.setSname(txtName.getText().trim());
            s.setEnrollYear(txtEnrollYear.getText().trim());
            s.setClazzid(txtClassId.getText().trim());
            s.setSgender(txtGender.getText().trim());
            s.setSphone(txtPhone.getText().trim());
            s.setSemail(txtEmail.getText().trim());
            s.setSaddress(txtAddress.getText().trim());
            s.setFinishedCredit(got); // 回写当前统计值，防止被服务端 UPDATE 覆盖
            s.setRequiredCredit(need);
            s.setLocked(chkLocked.isSelected());

            // 新增学生时赋予初始密码123456，编辑时不改动密码
            if(selectedStudent == null){
                s.setSpassword("123456");
            }

            new Thread(()->{
                Message req = new Message();
                // selectedStudent为null = 新增，否则 = 修改
                if(selectedStudent == null){
                    req.setType(MsgConst.ADD_STUDENT);
                }else{
                    req.setType(MsgConst.UPDATE_STUDENT);
                }
                req.setData(s);
                Message res = SocketClient.send(req);
                SwingUtilities.invokeLater(()->{
                    if(res == null){
                        JOptionPane.showMessageDialog(this,"服务端无响应");
                        return;
                    }
                    if(res.isSuccess()){
                        String tip = selectedStudent == null ? "新增学生成功" : "保存修改成功";
                        JOptionPane.showMessageDialog(this,tip);
                        refreshStudentList();
                        // 新增成功后自动加载新学生信息，切换为编辑状态
                        if(selectedStudent == null){
                            loadStudentInfo(s.getSCard());
                        }
                    }else{
                        JOptionPane.showMessageDialog(this,"操作失败："+res.getResponseMsg());
                    }
                });
            }).start();
        }


        //锁定学籍
        private void lockStudent(){
            if(selectedStudent == null) {
                JOptionPane.showMessageDialog(this,"请先选中学生");
                return;
            }
            new Thread(()->{
                Message req = new Message();
                req.setType(MsgConst.LOCK_STUDENT);
                req.setData(selectedStudent.getSCard());
                Message res = SocketClient.send(req);
                SwingUtilities.invokeLater(()->{
                    if(res == null){
                        JOptionPane.showMessageDialog(this,"服务端无响应");
                        return;
                    }
                    if(res.isSuccess()){
                        JOptionPane.showMessageDialog(this,"学籍锁定成功");
                        refreshStudentList();
                        loadStudentInfo(selectedStudent.getSCard());
                    }else{
                        JOptionPane.showMessageDialog(this,"锁定失败："+res.getResponseMsg());
                    }
                });
            }).start();
        }

        //解锁学籍
        private void unlockStudent(){
            if(selectedStudent == null) {
                JOptionPane.showMessageDialog(this,"请先选中学生");
                return;
            }
            new Thread(()->{
                Message req = new Message();
                req.setType(MsgConst.UNLOCK_STUDENT);
                req.setData(selectedStudent.getSCard());
                Message res = SocketClient.send(req);
                SwingUtilities.invokeLater(()->{
                    if(res == null){
                        JOptionPane.showMessageDialog(this,"服务端无响应");
                        return;
                    }
                    if(res.isSuccess()){
                        JOptionPane.showMessageDialog(this,"学籍解锁成功");
                        refreshStudentList();
                        loadStudentInfo(selectedStudent.getSCard());
                    }else{
                        JOptionPane.showMessageDialog(this,"解锁失败："+res.getResponseMsg());
                    }
                });
            }).start();
        }

        //新增学生（初始化表单，不直接提交）
        private void addNewStudent(){
            String newSCard = JOptionPane.showInputDialog("请输入新学生一卡通号");
            if(newSCard==null || newSCard.trim().isEmpty()) return;

            // 清空表单，进入新增状态
            selectedStudent = null;
            txtSCard.setText(newSCard.trim());
            txtStuid.setText("");
            txtName.setText("");
            txtEnrollYear.setText("");
            txtClassId.setText("");
            txtDept.setText("");
            txtMajor.setText("");
            txtCreditGot.setText("0");   // 新增学生尚无选课记录
            txtCreditNeed.setText("0");
            txtGender.setText("");
            txtPhone.setText("");
            txtEmail.setText("");
            txtAddress.setText("");
            chkLocked.setSelected(false);

            JOptionPane.showMessageDialog(this,"请在右侧表单填写完整信息后，点击【保存修改】完成新增");
            txtName.requestFocus();
        }

        //删除学生
        private void deleteStudent(){
            if(selectedStudent == null) {
                JOptionPane.showMessageDialog(this,"请先选中要删除的学生");
                return;
            }
            int opt = JOptionPane.showConfirmDialog(this,"确定删除该学生？删除后不可恢复","确认删除",JOptionPane.YES_NO_OPTION);
            if(opt != JOptionPane.YES_OPTION) return;

            new Thread(()->{
                Message req = new Message();
                req.setType(MsgConst.DELETE_STUDENT);
                req.setData(selectedStudent.getSCard());
                Message res = SocketClient.send(req);
                SwingUtilities.invokeLater(()->{
                    if(res == null){
                        JOptionPane.showMessageDialog(this,"服务端无响应");
                        return;
                    }
                    if(res.isSuccess()){
                        JOptionPane.showMessageDialog(this,"删除成功");
                        refreshStudentList();
                        // 清空表单
                        txtSCard.setText("");
                        txtStuid.setText("");
                        txtName.setText("");
                        txtEnrollYear.setText("");
                        txtClassId.setText("");
                        txtDept.setText("");
                        txtMajor.setText("");
                        txtCreditGot.setText("");
                        txtCreditNeed.setText("");
                        txtGender.setText("");
                        txtPhone.setText("");
                        txtEmail.setText("");
                        txtAddress.setText("");
                        chkLocked.setSelected(false);
                        selectedStudent = null;
                    }else{
                        JOptionPane.showMessageDialog(this,"删除失败："+res.getResponseMsg());
                    }
                });
            }).start();
        }

        //Excel一键导入学生
        private void importStudentExcel(){
            JFileChooser fc = new JFileChooser();
            fc.setDialogTitle("选择学生Excel文件");
            fc.setFileFilter(new FileNameExtensionFilter("Excel文件 (*.xlsx)", "xlsx"));
            if(fc.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) return;
            File file = fc.getSelectedFile();

            //读取文件为字节数组
            byte[] fileBytes;
            try(FileInputStream fis = new FileInputStream(file);
                ByteArrayOutputStream baos = new ByteArrayOutputStream()){
                byte[] buf = new byte[8192];
                int n;
                while((n = fis.read(buf)) > 0) baos.write(buf, 0, n);
                fileBytes = baos.toByteArray();
            }catch (Exception ex){
                JOptionPane.showMessageDialog(this,"读取文件失败："+ex.getMessage(),"错误",JOptionPane.ERROR_MESSAGE);
                return;
            }

            //后台线程发送给服务端
            new Thread(()->{
                Map<String,Object> param = new HashMap<>();
                param.put("fileBytes", fileBytes);
                param.put("fileName", file.getName());
                Message req = new Message();
                req.setType(MsgConst.ADMIN_IMPORT_STUDENT_EXCEL);
                req.setData(param);
                Message res = SocketClient.send(req);
                SwingUtilities.invokeLater(()->{
                    if(res == null){
                        JOptionPane.showMessageDialog(this,"服务端无响应，请检查服务器是否启动");
                        return;
                    }
                    if(res.isSuccess() && res.getData() instanceof Map){
                        Map<String,Object> result = (Map<String,Object>) res.getData();
                        StringBuilder sb = new StringBuilder();
                        sb.append("导入完成！\n");
                        sb.append("总行数：").append(result.get("totalRows")).append("\n");
                        sb.append("成功：").append(result.get("successCount")).append(" 条\n");
                        sb.append("失败：").append(result.get("failCount")).append(" 条");
                        List<String> errList = (List<String>) result.get("errors");
                        if(errList != null && !errList.isEmpty()){
                            sb.append("\n\n失败明细：\n");
                            for(String err : errList){
                                sb.append("  ").append(err).append("\n");
                            }
                        }
                        JOptionPane.showMessageDialog(this, sb.toString(),"导入结果",JOptionPane.INFORMATION_MESSAGE);
                        refreshStudentList();   //导入完刷新列表
                    }else{
                        JOptionPane.showMessageDialog(this,"导入失败："+res.getResponseMsg(),"错误",JOptionPane.ERROR_MESSAGE);
                    }
                });
            }).start();
        }
    }

  //====================标签页2：班级【完整网络连通】====================
    private class ClazzInnerPanel extends JPanel {
        private DefaultTableModel clazzModel;
        private JTextField txtClazzId, txtMajorId;
        private Clazz selectedClazz;

        // ★ 新增：查询栏控件
        private JComboBox<String> cmbSearchType;
        private JTextField txtSearchKey;

        public ClazzInnerPanel(){
            setLayout(new BorderLayout());
            setBackground(PAGE_BG);
            JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
            split.setDividerLocation(400);

            //左侧班级列表
            JPanel left = new JPanel(new BorderLayout());
            left.setBackground(CARD_BG);
            left.setBorder(BorderFactory.createTitledBorder("班级列表"));

            // ★ 新增：搜索栏
            JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 6));
            searchPanel.setBackground(CARD_BG);
            cmbSearchType = new JComboBox<>(new String[]{"按班级编号", "按专业编号"});
            txtSearchKey = new JTextField(14);
            JButton btnSearch = new JButton("查询");
            JButton btnShowAll = new JButton("显示全部");
            styleBtn(btnSearch);
            styleBtn(btnShowAll);
            btnSearch.setPreferredSize(new Dimension(80, 30));
            btnShowAll.setPreferredSize(new Dimension(90, 30));
            searchPanel.add(cmbSearchType);
            searchPanel.add(txtSearchKey);
            searchPanel.add(btnSearch);
            searchPanel.add(btnShowAll);
            left.add(searchPanel, BorderLayout.NORTH);

            String[] col = {"班级编号","所属专业编号"};
            clazzModel = new DefaultTableModel(col,0);
            JTable table = new JTable(clazzModel);
            table.getTableHeader().setBackground(TABLE_HEADER_GREEN);
            table.getTableHeader().setForeground(HEADER_GREEN);
            left.add(new JScrollPane(table),BorderLayout.CENTER);
            split.setLeftComponent(left);

            //右侧详情表单
            JPanel right = new JPanel();
            right.setBackground(HEADER_LIGHT_GREEN);
            right.setBorder(BorderFactory.createCompoundBorder(
                    new LineBorder(BORDER_COLOR,1),
                    new EmptyBorder(12,12,12,12)
            ));
            right.setLayout(new GridBagLayout());
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.insets = new Insets(8,8,8,8);
            gbc.anchor = GridBagConstraints.WEST;

            txtClazzId = new JTextField(18);
            txtClazzId.setEditable(false); //主键不可编辑
            txtMajorId = new JTextField(18);

            int row=0;
            addRow(right,gbc,"班级编号：",txtClazzId,row++);
            addRow(right,gbc,"所属专业编号：",txtMajorId,row++);

            split.setRightComponent(new JScrollPane(right));
            add(split,BorderLayout.CENTER);

            //底部按钮栏
            JPanel btnBar = new JPanel();
            JButton refresh = new JButton("刷新列表");
            JButton save = new JButton("保存修改");
            JButton add = new JButton("新增班级");
            JButton del = new JButton("删除班级");
            styleBtn(refresh); styleBtn(save); styleBtn(add); styleBtn(del);
            btnBar.add(refresh);btnBar.add(save);btnBar.add(add);btnBar.add(del);
            add(btnBar,BorderLayout.SOUTH);

            //表格选中事件
            table.getSelectionModel().addListSelectionListener(e->{
                if(!e.getValueIsAdjusting()){
                    int r = table.getSelectedRow();
                    if(r>=0){
                        String clazzid = (String)clazzModel.getValueAt(r,0);
                        loadClazzInfo(clazzid);
                    }
                }
            });

            refresh.addActionListener(e->refreshClazzList());
            save.addActionListener(e->saveClazzEdit());
            add.addActionListener(e->addNewClazz());
            del.addActionListener(e->deleteClazz());

            // ★ 新增：查询事件绑定
            btnSearch.addActionListener(e -> doSearchClazz());
            btnShowAll.addActionListener(e -> refreshClazzList());
            txtSearchKey.addActionListener(e -> doSearchClazz()); // 回车触发
            cmbSearchType.addActionListener(e -> txtSearchKey.setText("")); // 切换类型清空输入

            SwingUtilities.invokeLater(this::refreshClazzList);
        }

        // ★ 新增：根据下拉框选择的方式查询班级
        private void doSearchClazz(){
            String type = (String) cmbSearchType.getSelectedItem();
            String key = txtSearchKey.getText().trim();

            if("按班级编号".equals(type)){
                // —— 按班级编号查单个班级 ——
                if(key.isEmpty()){
                    JOptionPane.showMessageDialog(this,"请输入班级编号");
                    return;
                }
                new Thread(()->{
                    Message req = new Message();
                    req.setType(MsgConst.QUERY_CLAZZ_BY_CLAZZID);
                    req.setData(key);
                    Message res = SocketClient.send(req);
                    SwingUtilities.invokeLater(()->{
                        if(res == null){
                            JOptionPane.showMessageDialog(this,"服务端无响应");
                            return;
                        }
                        clazzModel.setRowCount(0);
                        if(res.isSuccess() && res.getData() instanceof Clazz){
                            Clazz c = (Clazz) res.getData();
                            clazzModel.addRow(new Object[]{c.getClazzid(), c.getMajorid()});
                            // 自动选中该行，触发右侧详情加载
                            if(clazzModel.getRowCount() > 0){
                                ((JTable)((JScrollPane)((JPanel)((JSplitPane)getComponent(0)).getLeftComponent())
                                        .getComponent(1)).getViewport().getView())
                                        .setRowSelectionInterval(0, 0);
                            }
                        }else{
                            JOptionPane.showMessageDialog(this,"未找到该班级："+res.getResponseMsg());
                        }
                    });
                }).start();
            }else{
                // —— 按专业编号查该专业下所有班级 ——
                if(key.isEmpty()){
                    JOptionPane.showMessageDialog(this,"请输入专业编号");
                    return;
                }
                new Thread(()->{
                    Message req = new Message();
                    req.setType(MsgConst.QUERY_CLAZZ_BY_MAJORID);
                    req.setData(key);
                    Message res = SocketClient.send(req);
                    SwingUtilities.invokeLater(()->{
                        if(res == null){
                            JOptionPane.showMessageDialog(this,"服务端无响应");
                            return;
                        }
                        clazzModel.setRowCount(0);
                        if(res.isSuccess() && res.getData() instanceof List<?>){
                            List<Clazz> list = (List<Clazz>) res.getData();
                            if(list.isEmpty()){
                                JOptionPane.showMessageDialog(this,"该专业下暂无班级");
                                return;
                            }
                            for(Clazz c : list){
                                clazzModel.addRow(new Object[]{c.getClazzid(), c.getMajorid()});
                            }
                        }else{
                            JOptionPane.showMessageDialog(this,"查询失败："+res.getResponseMsg());
                        }
                    });
                }).start();
            }
        }

        //刷新全部班级列表
        private void refreshClazzList(){
            new Thread(()->{
                Message req = new Message();
                req.setType(MsgConst.QUERY_ALL_CLAZZ);
                Message res = SocketClient.send(req);
                SwingUtilities.invokeLater(()->{
                    if(res==null){
                        JOptionPane.showMessageDialog(this,"服务端无响应");
                        return;
                    }
                    if(res.isSuccess() && res.getData() instanceof List<?>){
                        clazzModel.setRowCount(0);
                        List<Clazz> list = (List<Clazz>)res.getData();
                        for(Clazz c:list){
                            clazzModel.addRow(new Object[]{c.getClazzid(), c.getMajorid()});
                        }
                    }else{
                        JOptionPane.showMessageDialog(this,"加载班级失败："+res.getResponseMsg());
                    }
                });
            }).start();
        }

        //根据班级编号查询单个详情
        private void loadClazzInfo(String clazzid){
            new Thread(()->{
                Message req = new Message();
                req.setType(MsgConst.QUERY_CLAZZ_BY_CLAZZID);
                req.setData(clazzid);
                Message res = SocketClient.send(req);
                SwingUtilities.invokeLater(()->{
                    if(res!=null && res.isSuccess() && res.getData() instanceof Clazz){
                        selectedClazz = (Clazz)res.getData();
                        txtClazzId.setText(selectedClazz.getClazzid());
                        txtMajorId.setText(selectedClazz.getMajorid());
                    }
                });
            }).start();
        }

        //保存修改
        private void saveClazzEdit(){
            if(selectedClazz==null){
                JOptionPane.showMessageDialog(this,"请先选中班级");
                return;
            }
            selectedClazz.setMajorid(txtMajorId.getText().trim());
            new Thread(()->{
                Message req = new Message();
                req.setType(MsgConst.UPDATE_CLAZZ);
                req.setData(selectedClazz);
                Message res = SocketClient.send(req);
                SwingUtilities.invokeLater(()->{
                    if(res!=null && res.isSuccess()){
                        JOptionPane.showMessageDialog(this,"班级修改成功");
                        refreshClazzList();
                    }else{
                        JOptionPane.showMessageDialog(this,"修改失败："+(res!=null?res.getResponseMsg():"无响应"));
                    }
                });
            }).start();
        }

        //新增班级
        private void addNewClazz(){
            String newClazzId = JOptionPane.showInputDialog("请输入新班级编号");
            if(newClazzId==null||newClazzId.trim().isEmpty()) return;
            Clazz c = new Clazz();
            c.setClazzid(newClazzId.trim());
            c.setMajorid(txtMajorId.getText().trim());
            new Thread(()->{
                Message req = new Message();
                req.setType(MsgConst.ADD_CLAZZ);
                req.setData(c);
                Message res = SocketClient.send(req);
                SwingUtilities.invokeLater(()->{
                    if(res!=null && res.isSuccess()){
                        JOptionPane.showMessageDialog(this,"新增班级成功");
                        refreshClazzList();
                    }else{
                        JOptionPane.showMessageDialog(this,"新增失败："+(res!=null?res.getResponseMsg():"无响应"));
                    }
                });
            }).start();
        }

        //删除班级
        private void deleteClazz(){
            if(selectedClazz==null) return;
            int opt = JOptionPane.showConfirmDialog(this,"确定删除该班级？","确认",JOptionPane.YES_NO_OPTION);
            if(opt!=JOptionPane.YES_OPTION) return;
            new Thread(()->{
                Message req = new Message();
                req.setType(MsgConst.DELETE_CLAZZ);
                req.setData(selectedClazz.getClazzid());
                Message res = SocketClient.send(req);
                SwingUtilities.invokeLater(()->{
                    if(res!=null && res.isSuccess()){
                        JOptionPane.showMessageDialog(this,"删除成功");
                        refreshClazzList();
                        txtClazzId.setText("");
                        txtMajorId.setText("");
                        selectedClazz=null;
                    }else{
                        JOptionPane.showMessageDialog(this,"删除失败："+(res!=null?res.getResponseMsg():"无响应"));
                    }
                });
            }).start();
        }

        private void addRow(JPanel panel, GridBagConstraints gbc, String labelText, JTextField field, int row){
            gbc.gridx = 0; gbc.gridy = row;
            panel.add(new JLabel(labelText), gbc);
            gbc.gridx = 1;
            panel.add(field, gbc);
        }
        private void styleBtn(JButton btn){
            btn.setBackground(BTN_GREEN);
            btn.setForeground(Color.WHITE);
            btn.setFocusPainted(false);
            btn.setBorder(new LineBorder(BTN_GREEN,1));
            btn.setFont(new Font("微软雅黑",Font.BOLD,14));
            btn.setPreferredSize(new Dimension(110,36));
        }
    }

  //====================标签页3：专业【完整网络连通】====================
    private class MajorInnerPanel extends JPanel {
        private DefaultTableModel majorModel;
        private JTextField txtMajorId, txtMajorName, txtDeptId;
        private Major selectedMajor;

        // ★ 新增：查询栏控件
        private JComboBox<String> cmbSearchType;
        private JTextField txtSearchKey;

        public MajorInnerPanel(){
            setLayout(new BorderLayout());
            setBackground(PAGE_BG);
            JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
            split.setDividerLocation(400);

            //左侧专业列表
            JPanel left = new JPanel(new BorderLayout());
            left.setBackground(CARD_BG);
            left.setBorder(BorderFactory.createTitledBorder("专业列表"));

            // ★ 新增：搜索栏
            JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 6));
            searchPanel.setBackground(CARD_BG);
            cmbSearchType = new JComboBox<>(new String[]{"按专业编号", "按院系编号"});
            txtSearchKey = new JTextField(14);
            JButton btnSearch = new JButton("查询");
            JButton btnShowAll = new JButton("显示全部");
            styleBtn(btnSearch);
            styleBtn(btnShowAll);
            btnSearch.setPreferredSize(new Dimension(80, 30));
            btnShowAll.setPreferredSize(new Dimension(90, 30));
            searchPanel.add(cmbSearchType);
            searchPanel.add(txtSearchKey);
            searchPanel.add(btnSearch);
            searchPanel.add(btnShowAll);
            left.add(searchPanel, BorderLayout.NORTH);

            String[] col = {"专业编号","专业名称","所属院系编号"};
            majorModel = new DefaultTableModel(col,0);
            JTable table = new JTable(majorModel);
            table.getTableHeader().setBackground(TABLE_HEADER_GREEN);
            table.getTableHeader().setForeground(HEADER_GREEN);
            left.add(new JScrollPane(table),BorderLayout.CENTER);
            split.setLeftComponent(left);

            //右侧详情表单
            JPanel right = new JPanel();
            right.setBackground(HEADER_LIGHT_GREEN);
            right.setBorder(BorderFactory.createCompoundBorder(
                    new LineBorder(BORDER_COLOR,1),
                    new EmptyBorder(12,12,12,12)
            ));
            right.setLayout(new GridBagLayout());
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.insets = new Insets(8,8,8,8);
            gbc.anchor = GridBagConstraints.WEST;

            txtMajorId = new JTextField(18);
            txtMajorId.setEditable(false); //主键不可编辑
            txtMajorName = new JTextField(18);
            txtDeptId = new JTextField(18);

            int row=0;
            addRow(right,gbc,"专业编号：",txtMajorId,row++);
            addRow(right,gbc,"专业名称：",txtMajorName,row++);
            addRow(right,gbc,"所属院系编号：",txtDeptId,row++);

            split.setRightComponent(new JScrollPane(right));
            add(split,BorderLayout.CENTER);

            //底部按钮栏
            JPanel btnBar = new JPanel();
            JButton refresh = new JButton("刷新列表");
            JButton save = new JButton("保存修改");
            JButton add = new JButton("新增专业");
            JButton del = new JButton("删除专业");
            styleBtn(refresh); styleBtn(save); styleBtn(add); styleBtn(del);
            btnBar.add(refresh);btnBar.add(save);btnBar.add(add);btnBar.add(del);
            add(btnBar,BorderLayout.SOUTH);

            //表格选中事件
            table.getSelectionModel().addListSelectionListener(e->{
                if(!e.getValueIsAdjusting()){
                    int r = table.getSelectedRow();
                    if(r>=0){
                        String majorid = (String)majorModel.getValueAt(r,0);
                        loadMajorInfo(majorid);
                    }
                }
            });

            refresh.addActionListener(e->refreshMajorList());
            save.addActionListener(e->saveMajorEdit());
            add.addActionListener(e->addNewMajor());
            del.addActionListener(e->deleteMajor());

            // ★ 新增：查询事件绑定
            btnSearch.addActionListener(e -> doSearchMajor());
            btnShowAll.addActionListener(e -> refreshMajorList());
            txtSearchKey.addActionListener(e -> doSearchMajor()); // 回车触发
            cmbSearchType.addActionListener(e -> txtSearchKey.setText("")); // 切换类型清空输入

            SwingUtilities.invokeLater(this::refreshMajorList);
        }

        // ★ 新增：根据下拉框选择的方式查询专业
        private void doSearchMajor(){
            String type = (String) cmbSearchType.getSelectedItem();
            String key = txtSearchKey.getText().trim();

            if("按专业编号".equals(type)){
                // —— 按专业编号查单个专业 ——
                if(key.isEmpty()){
                    JOptionPane.showMessageDialog(this,"请输入专业编号");
                    return;
                }
                new Thread(()->{
                    Message req = new Message();
                    req.setType(MsgConst.QUERY_MAJOR_BY_MAJORID);
                    req.setData(key);
                    Message res = SocketClient.send(req);
                    SwingUtilities.invokeLater(()->{
                        if(res == null){
                            JOptionPane.showMessageDialog(this,"服务端无响应");
                            return;
                        }
                        majorModel.setRowCount(0);
                        if(res.isSuccess() && res.getData() instanceof Major){
                            Major m = (Major) res.getData();
                            majorModel.addRow(new Object[]{m.getMajorid(), m.getMajorname(), m.getDeptid()});
                            // 自动选中该行，触发右侧详情加载
                            if(majorModel.getRowCount() > 0){
                                ((JTable)((JScrollPane)((JPanel)((JSplitPane)getComponent(0)).getLeftComponent())
                                        .getComponent(1)).getViewport().getView())
                                        .setRowSelectionInterval(0, 0);
                            }
                        }else{
                            JOptionPane.showMessageDialog(this,"未找到该专业："+res.getResponseMsg());
                        }
                    });
                }).start();
            }else{
                // —— 按院系编号查该院系下所有专业 ——
                if(key.isEmpty()){
                    JOptionPane.showMessageDialog(this,"请输入院系编号");
                    return;
                }
                new Thread(()->{
                    Message req = new Message();
                    req.setType(MsgConst.QUERY_MAJOR_BY_DEPTID);
                    req.setData(key);
                    Message res = SocketClient.send(req);
                    SwingUtilities.invokeLater(()->{
                        if(res == null){
                            JOptionPane.showMessageDialog(this,"服务端无响应");
                            return;
                        }
                        majorModel.setRowCount(0);
                        if(res.isSuccess() && res.getData() instanceof List<?>){
                            List<Major> list = (List<Major>) res.getData();
                            if(list.isEmpty()){
                                JOptionPane.showMessageDialog(this,"该院系下暂无专业");
                                return;
                            }
                            for(Major m : list){
                                majorModel.addRow(new Object[]{m.getMajorid(), m.getMajorname(), m.getDeptid()});
                            }
                        }else{
                            JOptionPane.showMessageDialog(this,"查询失败："+res.getResponseMsg());
                        }
                    });
                }).start();
            }
        }

        //刷新全部专业列表
        private void refreshMajorList(){
            new Thread(()->{
                Message req = new Message();
                req.setType(MsgConst.QUERY_ALL_MAJOR);
                Message res = SocketClient.send(req);
                SwingUtilities.invokeLater(()->{
                    if(res==null){
                        JOptionPane.showMessageDialog(this,"服务端无响应");
                        return;
                    }
                    if(res.isSuccess() && res.getData() instanceof List<?>){
                        majorModel.setRowCount(0);
                        List<Major> list = (List<Major>)res.getData();
                        for(Major m:list){
                            majorModel.addRow(new Object[]{
                                    m.getMajorid(),
                                    m.getMajorname(),
                                    m.getDeptid()
                            });
                        }
                    }else{
                        JOptionPane.showMessageDialog(this,"加载专业失败："+res.getResponseMsg());
                    }
                });
            }).start();
        }

        //根据专业编号查询单个详情
        private void loadMajorInfo(String majorid){
            new Thread(()->{
                Message req = new Message();
                req.setType(MsgConst.QUERY_MAJOR_BY_MAJORID);
                req.setData(majorid);
                Message res = SocketClient.send(req);
                SwingUtilities.invokeLater(()->{
                    if(res!=null && res.isSuccess() && res.getData() instanceof Major){
                        selectedMajor = (Major)res.getData();
                        txtMajorId.setText(selectedMajor.getMajorid());
                        txtMajorName.setText(selectedMajor.getMajorname());
                        txtDeptId.setText(selectedMajor.getDeptid());
                    }
                });
            }).start();
        }

        //保存修改
        private void saveMajorEdit(){
            if(selectedMajor==null){
                JOptionPane.showMessageDialog(this,"请先选中专业");
                return;
            }
            selectedMajor.setMajorname(txtMajorName.getText().trim());
            selectedMajor.setDeptid(txtDeptId.getText().trim());
            new Thread(()->{
                Message req = new Message();
                req.setType(MsgConst.UPDATE_MAJOR);
                req.setData(selectedMajor);
                Message res = SocketClient.send(req);
                SwingUtilities.invokeLater(()->{
                    if(res!=null && res.isSuccess()){
                        JOptionPane.showMessageDialog(this,"专业修改成功");
                        refreshMajorList();
                    }else{
                        JOptionPane.showMessageDialog(this,"修改失败："+(res!=null?res.getResponseMsg():"无响应"));
                    }
                });
            }).start();
        }

        //新增专业
        private void addNewMajor(){
            String newMajorId = JOptionPane.showInputDialog("请输入新专业编号");
            if(newMajorId==null||newMajorId.trim().isEmpty()) return;
            Major m = new Major();
            m.setMajorid(newMajorId.trim());
            m.setMajorname(txtMajorName.getText().trim());
            m.setDeptid(txtDeptId.getText().trim());
            new Thread(()->{
                Message req = new Message();
                req.setType(MsgConst.ADD_MAJOR);
                req.setData(m);
                Message res = SocketClient.send(req);
                SwingUtilities.invokeLater(()->{
                    if(res!=null && res.isSuccess()){
                        JOptionPane.showMessageDialog(this,"新增专业成功");
                        refreshMajorList();
                    }else{
                        JOptionPane.showMessageDialog(this,"新增失败："+(res!=null?res.getResponseMsg():"无响应"));
                    }
                });
            }).start();
        }

        //删除专业
        private void deleteMajor(){
            if(selectedMajor==null) return;
            int opt = JOptionPane.showConfirmDialog(this,"确定删除该专业？","确认",JOptionPane.YES_NO_OPTION);
            if(opt!=JOptionPane.YES_OPTION) return;
            new Thread(()->{
                Message req = new Message();
                req.setType(MsgConst.DELETE_MAJOR);
                req.setData(selectedMajor.getMajorid());
                Message res = SocketClient.send(req);
                SwingUtilities.invokeLater(()->{
                    if(res!=null && res.isSuccess()){
                        JOptionPane.showMessageDialog(this,"删除成功");
                        refreshMajorList();
                        txtMajorId.setText("");
                        txtMajorName.setText("");
                        txtDeptId.setText("");
                        selectedMajor=null;
                    }else{
                        JOptionPane.showMessageDialog(this,"删除失败："+(res!=null?res.getResponseMsg():"无响应"));
                    }
                });
            }).start();
        }

        private void addRow(JPanel panel, GridBagConstraints gbc, String labelText, JTextField field, int row){
            gbc.gridx = 0; gbc.gridy = row;
            panel.add(new JLabel(labelText), gbc);
            gbc.gridx = 1;
            panel.add(field, gbc);
        }
        private void styleBtn(JButton btn){
            btn.setBackground(BTN_GREEN);
            btn.setForeground(Color.WHITE);
            btn.setFocusPainted(false);
            btn.setBorder(new LineBorder(BTN_GREEN,1));
            btn.setFont(new Font("微软雅黑",Font.BOLD,14));
            btn.setPreferredSize(new Dimension(110,36));
        }
    }


  //====================标签页4：院系【完整网络连通】====================
    private class DeptInnerPanel extends JPanel {
        private DefaultTableModel deptModel;
        private JTextField txtDeptId, txtDeptName;
        private Department selectedDept;

        // ★ 新增：查询栏控件
        private JComboBox<String> cmbSearchType;
        private JTextField txtSearchKey;
        private JTable deptTable;   // ★ 提升为字段，方便查询后自动选中

        public DeptInnerPanel(){
            setLayout(new BorderLayout());
            setBackground(PAGE_BG);
            JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
            split.setDividerLocation(400);

            //左侧院系列表
            JPanel left = new JPanel(new BorderLayout());
            left.setBackground(CARD_BG);
            left.setBorder(BorderFactory.createTitledBorder("院系列表"));

            // ★ 新增：搜索栏
            JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 6));
            searchPanel.setBackground(CARD_BG);
            cmbSearchType = new JComboBox<>(new String[]{"按院系编号"});
            txtSearchKey = new JTextField(14);
            JButton btnSearch = new JButton("查询");
            JButton btnShowAll = new JButton("显示全部");
            styleBtn(btnSearch);
            styleBtn(btnShowAll);
            btnSearch.setPreferredSize(new Dimension(80, 30));
            btnShowAll.setPreferredSize(new Dimension(90, 30));
            searchPanel.add(cmbSearchType);
            searchPanel.add(txtSearchKey);
            searchPanel.add(btnSearch);
            searchPanel.add(btnShowAll);
            left.add(searchPanel, BorderLayout.NORTH);

            String[] col = {"院系编号","院系名称"};
            deptModel = new DefaultTableModel(col,0);
            deptTable = new JTable(deptModel);
            deptTable.getTableHeader().setBackground(TABLE_HEADER_GREEN);
            deptTable.getTableHeader().setForeground(HEADER_GREEN);
            left.add(new JScrollPane(deptTable),BorderLayout.CENTER);
            split.setLeftComponent(left);

            //右侧详情表单
            JPanel right = new JPanel();
            right.setBackground(HEADER_LIGHT_GREEN);
            right.setBorder(BorderFactory.createCompoundBorder(
                    new LineBorder(BORDER_COLOR,1),
                    new EmptyBorder(12,12,12,12)
            ));
            right.setLayout(new GridBagLayout());
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.insets = new Insets(8,8,8,8);
            gbc.anchor = GridBagConstraints.WEST;

            txtDeptId = new JTextField(18);
            txtDeptId.setEditable(false); //主键不可编辑
            txtDeptName = new JTextField(18);

            int row=0;
            addRow(right,gbc,"院系编号：",txtDeptId,row++);
            addRow(right,gbc,"院系名称：",txtDeptName,row++);

            split.setRightComponent(new JScrollPane(right));
            add(split,BorderLayout.CENTER);

            //底部按钮栏
            JPanel btnBar = new JPanel();
            JButton refresh = new JButton("刷新列表");
            JButton save = new JButton("保存修改");
            JButton add = new JButton("新增院系");
            JButton del = new JButton("删除院系");
            styleBtn(refresh); styleBtn(save); styleBtn(add); styleBtn(del);
            btnBar.add(refresh);btnBar.add(save);btnBar.add(add);btnBar.add(del);
            add(btnBar,BorderLayout.SOUTH);

            //表格选中事件
            deptTable.getSelectionModel().addListSelectionListener(e->{
                if(!e.getValueIsAdjusting()){
                    int r = deptTable.getSelectedRow();
                    if(r>=0){
                        String deptid = (String)deptModel.getValueAt(r,0);
                        loadDeptInfo(deptid);
                    }
                }
            });

            refresh.addActionListener(e->refreshDeptList());
            save.addActionListener(e->saveDeptEdit());
            add.addActionListener(e->addNewDept());
            del.addActionListener(e->deleteDept());

            // ★ 新增：查询事件绑定
            btnSearch.addActionListener(e -> doSearchDept());
            btnShowAll.addActionListener(e -> refreshDeptList());
            txtSearchKey.addActionListener(e -> doSearchDept()); // 回车触发
            cmbSearchType.addActionListener(e -> txtSearchKey.setText("")); // 切换类型清空输入

            SwingUtilities.invokeLater(this::refreshDeptList);
        }

        // ★ 新增：按院系编号查询
        private void doSearchDept(){
            String type = (String) cmbSearchType.getSelectedItem();
            String key = txtSearchKey.getText().trim();

            if("按院系编号".equals(type)){
                if(key.isEmpty()){
                    JOptionPane.showMessageDialog(this,"请输入院系编号");
                    return;
                }
                new Thread(()->{
                    Message req = new Message();
                    req.setType(MsgConst.QUERY_DEPT_BY_DEPTID);
                    req.setData(key);
                    Message res = SocketClient.send(req);
                    SwingUtilities.invokeLater(()->{
                        if(res == null){
                            JOptionPane.showMessageDialog(this,"服务端无响应");
                            return;
                        }
                        deptModel.setRowCount(0);
                        if(res.isSuccess() && res.getData() instanceof Department){
                            Department d = (Department) res.getData();
                            deptModel.addRow(new Object[]{d.getDeptid(), d.getDeptname()});
                            // 自动选中该行，触发右侧详情加载
                            if(deptTable.getRowCount() > 0){
                                deptTable.setRowSelectionInterval(0, 0);
                            }
                        }else{
                            JOptionPane.showMessageDialog(this,"未找到该院系："+res.getResponseMsg());
                        }
                    });
                }).start();
            }
            // 后续若扩展"按院系名称"可在这里追加 else if 分支
        }

        //刷新全部院系列表
        private void refreshDeptList(){
            new Thread(()->{
                Message req = new Message();
                req.setType(MsgConst.QUERY_ALL_DEPT);
                Message res = SocketClient.send(req);
                SwingUtilities.invokeLater(()->{
                    if(res==null){
                        JOptionPane.showMessageDialog(this,"服务端无响应");
                        return;
                    }
                    if(res.isSuccess() && res.getData() instanceof List<?>){
                        deptModel.setRowCount(0);
                        List<Department> list = (List<Department>)res.getData();
                        for(Department d:list){
                            deptModel.addRow(new Object[]{
                                    d.getDeptid(),
                                    d.getDeptname()
                            });
                        }
                    }else{
                        JOptionPane.showMessageDialog(this,"加载院系失败："+res.getResponseMsg());
                    }
                });
            }).start();
        }

        //根据院系编号查询单个详情
        private void loadDeptInfo(String deptid){
            new Thread(()->{
                Message req = new Message();
                req.setType(MsgConst.QUERY_DEPT_BY_DEPTID);
                req.setData(deptid);
                Message res = SocketClient.send(req);
                SwingUtilities.invokeLater(()->{
                    if(res!=null && res.isSuccess() && res.getData() instanceof Department){
                        selectedDept = (Department)res.getData();
                        txtDeptId.setText(selectedDept.getDeptid());
                        txtDeptName.setText(selectedDept.getDeptname());
                    }
                });
            }).start();
        }

        //保存修改
        private void saveDeptEdit(){
            if(selectedDept==null){
                JOptionPane.showMessageDialog(this,"请先选中院系");
                return;
            }
            selectedDept.setDeptname(txtDeptName.getText().trim());
            new Thread(()->{
                Message req = new Message();
                req.setType(MsgConst.UPDATE_DEPT);
                req.setData(selectedDept);
                Message res = SocketClient.send(req);
                SwingUtilities.invokeLater(()->{
                    if(res!=null && res.isSuccess()){
                        JOptionPane.showMessageDialog(this,"院系修改成功");
                        refreshDeptList();
                    }else{
                        JOptionPane.showMessageDialog(this,"修改失败："+(res!=null?res.getResponseMsg():"无响应"));
                    }
                });
            }).start();
        }

        //新增院系
        private void addNewDept(){
            String newDeptId = JOptionPane.showInputDialog("请输入新院系编号");
            if(newDeptId==null||newDeptId.trim().isEmpty()) return;
            Department d = new Department();
            d.setDeptid(newDeptId.trim());
            d.setDeptname(txtDeptName.getText().trim());
            new Thread(()->{
                Message req = new Message();
                req.setType(MsgConst.ADD_DEPT);
                req.setData(d);
                Message res = SocketClient.send(req);
                SwingUtilities.invokeLater(()->{
                    if(res!=null && res.isSuccess()){
                        JOptionPane.showMessageDialog(this,"新增院系成功");
                        refreshDeptList();
                    }else{
                        JOptionPane.showMessageDialog(this,"新增失败："+(res!=null?res.getResponseMsg():"无响应"));
                    }
                });
            }).start();
        }

        //删除院系
        private void deleteDept(){
            if(selectedDept==null) return;
            int opt = JOptionPane.showConfirmDialog(this,"确定删除该院系？","确认",JOptionPane.YES_NO_OPTION);
            if(opt!=JOptionPane.YES_OPTION) return;
            new Thread(()->{
                Message req = new Message();
                req.setType(MsgConst.DELETE_DEPT);
                req.setData(selectedDept.getDeptid());
                Message res = SocketClient.send(req);
                SwingUtilities.invokeLater(()->{
                    if(res!=null && res.isSuccess()){
                        JOptionPane.showMessageDialog(this,"删除成功");
                        refreshDeptList();
                        txtDeptId.setText("");
                        txtDeptName.setText("");
                        selectedDept=null;
                    }else{
                        JOptionPane.showMessageDialog(this,"删除失败："+(res!=null?res.getResponseMsg():"无响应"));
                    }
                });
            }).start();
        }

        private void addRow(JPanel panel, GridBagConstraints gbc, String labelText, JTextField field, int row){
            gbc.gridx = 0; gbc.gridy = row;
            panel.add(new JLabel(labelText), gbc);
            gbc.gridx = 1;
            panel.add(field, gbc);
        }
        private void styleBtn(JButton btn){
            btn.setBackground(BTN_GREEN);
            btn.setForeground(Color.WHITE);
            btn.setFocusPainted(false);
            btn.setBorder(new LineBorder(BTN_GREEN,1));
            btn.setFont(new Font("微软雅黑",Font.BOLD,14));
            btn.setPreferredSize(new Dimension(110,36));
        }
    }
}