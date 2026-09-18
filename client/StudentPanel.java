package vCampus.client;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.util.HashMap;
import java.util.Map;
import vCampus.common.Message;
import vCampus.common.consts.MsgConst;
import vCampus.common.vo.Clazz;
import vCampus.common.vo.Department;
import vCampus.common.vo.Major;
import vCampus.common.vo.Student;

public class StudentPanel extends JPanel {
	private Student loginStudent;

	// 配色 —— 对齐东大教务系统深绿色主题
	private static final Color HEADER_GREEN = new Color(20, 120, 100);
	private static final Color PAGE_BG = new Color(247, 249, 250);
	private static final Color CARD_BG = Color.WHITE;
	private static final Color BORDER_COLOR = new Color(210, 225, 220);
	private static final Color BTN_GREEN = new Color(70, 160, 100);

	public StudentPanel() {
		setBackground(PAGE_BG);
		setLayout(new BorderLayout());
		JLabel title = new JLabel("学生学籍信息", SwingConstants.CENTER);
		title.setFont(new Font("微软雅黑", Font.BOLD, 24));
		title.setForeground(Color.GRAY);
		add(title, BorderLayout.CENTER);
	}

	public StudentPanel(Student stu) {
		this.loginStudent = stu;
		setBackground(PAGE_BG);
		setLayout(new BorderLayout());
		buildStudentView();
	}

	private void buildStudentView() {
		System.out.println("===进入buildStudentView函数===");
		System.out.println("loginStudent对象是否为空：" + loginStudent);

		if (loginStudent == null) {
			add(new JLabel("学生信息加载失败！"), BorderLayout.CENTER);
			return;
		}

		// ============【新增】页面打开，从服务端拉取最新学生数据 ============
		Message queryReq = new Message();
		queryReq.setType(MsgConst.QUERY_STUDENT_BY_SCARD);

		String sendSCard = loginStudent.getSCard();
		System.out.println("【客户端发送查询的一卡通号】：" + sendSCard);

		queryReq.setData(loginStudent.getSCard());
		Message queryRes = SocketClient.send(queryReq);

		if (queryRes.isSuccess()) {
			loginStudent = (Student) queryRes.getData();
		} else {
			JOptionPane.showMessageDialog(this, "学生信息加载失败：" + queryRes.getResponseMsg());
			return;
		}
		// =================================================================

		// ========== 1.顶部绿色标题栏 ==========
		JPanel topBar = new JPanel(new BorderLayout());
		topBar.setBackground(HEADER_GREEN);
		topBar.setPreferredSize(new Dimension(0, 50));
		JLabel topTitle = new JLabel("学生基本信息");
		topTitle.setFont(new Font("微软雅黑", Font.BOLD, 20));
		topTitle.setForeground(Color.WHITE);
		topBar.add(topTitle, BorderLayout.WEST);

		// ========== 2.主体：左侧导航 + 右侧内容区 ==========
		JPanel mainBody = new JPanel(new BorderLayout());
		mainBody.setBackground(PAGE_BG);

		// -----左侧侧边导航栏-----
		JPanel sideNav = new JPanel();
		sideNav.setLayout(new BoxLayout(sideNav, BoxLayout.Y_AXIS));
		sideNav.setBackground(Color.WHITE);
		sideNav.setPreferredSize(new Dimension(140, 0));
		sideNav.setBorder(new LineBorder(BORDER_COLOR, 1));

		JButton btnBaseInfo = new JButton("基本信息");
		btnBaseInfo.setOpaque(true);
		btnBaseInfo.setBackground(HEADER_GREEN);
		btnBaseInfo.setForeground(Color.WHITE);
		btnBaseInfo.setBorderPainted(false);

		sideNav.add(Box.createVerticalStrut(30));
		sideNav.add(btnBaseInfo);

		// -----右侧滚动内容面板-----
		JPanel rightContent = new JPanel();
		rightContent.setLayout(new BoxLayout(rightContent, BoxLayout.Y_AXIS));
		rightContent.setBackground(PAGE_BG);
		rightContent.setBorder(new EmptyBorder(20, 25, 20, 20));

		// ----------------【板块1：个人基本信息】----------------
		JPanel panelPersonal = new JPanel(new GridBagLayout());
		panelPersonal.setBackground(CARD_BG);
		panelPersonal.setBorder(BorderFactory.createCompoundBorder(
				new LineBorder(BORDER_COLOR, 1),
				new EmptyBorder(18, 18, 18, 18)
		));

		GridBagConstraints gbc = new GridBagConstraints();
		gbc.insets = new Insets(8, 12, 8, 12);
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.anchor = GridBagConstraints.WEST;
		gbc.weightx = 1.0;

		JLabel labPersonalTitle = new JLabel("■ 个人基本信息");
		labPersonalTitle.setFont(new Font("微软雅黑", Font.BOLD, 16));
		labPersonalTitle.setForeground(HEADER_GREEN);
		gbc.gridwidth = 4;
		gbc.gridx = 0;
		gbc.gridy = 0;
		panelPersonal.add(labPersonalTitle, gbc);

		gbc.gridwidth = 1;
		int row = 1;

		// 第一行：一卡通号 / 学号
		gbc.gridx = 0; gbc.gridy = row; panelPersonal.add(new JLabel("一卡通号："), gbc);
		gbc.gridx = 1; gbc.gridy = row; panelPersonal.add(new JLabel(loginStudent.getStuCard()), gbc);
		gbc.gridx = 2; gbc.gridy = row; panelPersonal.add(new JLabel("学号："), gbc);
		gbc.gridx = 3; gbc.gridy = row; panelPersonal.add(new JLabel(loginStudent.getSId()), gbc);
		row++;

		// 第二行：姓名 / 性别（只读）
		gbc.gridx = 0; gbc.gridy = row; panelPersonal.add(new JLabel("姓名："), gbc);
		gbc.gridx = 1; gbc.gridy = row; panelPersonal.add(new JLabel(loginStudent.getSname()), gbc);
		gbc.gridx = 2; gbc.gridy = row; panelPersonal.add(new JLabel("性别："), gbc);
		gbc.gridx = 3; gbc.gridy = row; panelPersonal.add(new JLabel(loginStudent.getSgender()), gbc);
		row++;

		// 第三行：电话号码 / 电子邮箱
		JTextField txtPhone = new JTextField(loginStudent.getSphone());
		txtPhone.setBorder(new LineBorder(new Color(200, 220, 200), 1));
		gbc.gridx = 0; gbc.gridy = row; panelPersonal.add(new JLabel("电话号码："), gbc);
		gbc.gridx = 1; gbc.gridy = row; panelPersonal.add(txtPhone, gbc);

		JTextField txtEmail = new JTextField(loginStudent.getSemail());
		txtEmail.setBorder(new LineBorder(new Color(200, 220, 200), 1));
		gbc.gridx = 2; gbc.gridy = row; panelPersonal.add(new JLabel("电子邮箱："), gbc);
		gbc.gridx = 3; gbc.gridy = row; panelPersonal.add(txtEmail, gbc);
		row++;

		// 第四行：联系地址，横跨后两列
		JTextField txtAddress = new JTextField(loginStudent.getSaddress());
		txtAddress.setBorder(new LineBorder(new Color(200, 220, 200), 1));
		gbc.gridx = 0; gbc.gridy = row; panelPersonal.add(new JLabel("联系地址："), gbc);
		gbc.gridx = 1; gbc.gridy = row;
		gbc.gridwidth = 3;
		panelPersonal.add(txtAddress, gbc);
		gbc.gridwidth = 1;
		row++;

		// ★ 第五行：一卡通余额（只读）
		Double balanceObj = loginStudent.getSbalance();
		String balanceText = (balanceObj == null) ? "0.00" : String.format("%.2f", balanceObj);
		final JLabel labBalance = new JLabel(balanceText + " 元");
		labBalance.setFont(new Font("微软雅黑", Font.BOLD, 14));
		labBalance.setForeground(new Color(200, 80, 80)); // 红色突出余额

		gbc.gridx = 0; gbc.gridy = row; panelPersonal.add(new JLabel("一卡通余额："), gbc);
		gbc.gridx = 1; gbc.gridy = row;
		gbc.gridwidth = 3;
		panelPersonal.add(labBalance, gbc);
		gbc.gridwidth = 1;
		row++;

		// ----------------【板块2：学籍信息】----------------
		JPanel panelSchoolInfo = new JPanel(new GridBagLayout());
		panelSchoolInfo.setBackground(CARD_BG);
		panelSchoolInfo.setBorder(BorderFactory.createCompoundBorder(
				new LineBorder(BORDER_COLOR, 1),
				new EmptyBorder(18, 18, 18, 18)
		));

		GridBagConstraints gbc2 = new GridBagConstraints();
		gbc2.insets = new Insets(8, 12, 8, 12);
		gbc2.fill = GridBagConstraints.HORIZONTAL;
		gbc2.anchor = GridBagConstraints.WEST;
		gbc2.weightx = 1.0;

		JLabel labSchoolTitle = new JLabel("■ 学籍信息");
		labSchoolTitle.setFont(new Font("微软雅黑", Font.BOLD, 16));
		labSchoolTitle.setForeground(HEADER_GREEN);
		gbc2.gridwidth = 4;
		gbc2.gridx = 0;
		gbc2.gridy = 0;
		panelSchoolInfo.add(labSchoolTitle, gbc2);

		gbc2.gridwidth = 1;
		row = 1;

		// 第一行：入学年份 / 班级编号
		gbc2.gridx = 0; gbc2.gridy = row; panelSchoolInfo.add(new JLabel("入学年份："), gbc2);
		gbc2.gridx = 1; gbc2.gridy = row; panelSchoolInfo.add(new JLabel(String.valueOf(loginStudent.getEnrollYear())), gbc2);
		gbc2.gridx = 2; gbc2.gridy = row; panelSchoolInfo.add(new JLabel("班级编号："), gbc2);
		gbc2.gridx = 3; gbc2.gridy = row; panelSchoolInfo.add(new JLabel(loginStudent.getClazzid()), gbc2);
		row++;

		// ★ 第二行：院系 / 专业（异步填充）
		final JLabel labDeptName = new JLabel("查询中...");
		final JLabel labMajorName = new JLabel("查询中...");
		gbc2.gridx = 0; gbc2.gridy = row; panelSchoolInfo.add(new JLabel("院系："), gbc2);
		gbc2.gridx = 1; gbc2.gridy = row; panelSchoolInfo.add(labDeptName, gbc2);
		gbc2.gridx = 2; gbc2.gridy = row; panelSchoolInfo.add(new JLabel("专业："), gbc2);
		gbc2.gridx = 3; gbc2.gridy = row; panelSchoolInfo.add(labMajorName, gbc2);
		row++;

		// 第三行：已修学分 / 毕业所需学分（异步填充）
		final JLabel labFinishedCredit = new JLabel("统计中...");
		final JLabel labRequiredCredit = new JLabel("-");
		gbc2.gridx = 0; gbc2.gridy = row; panelSchoolInfo.add(new JLabel("已修学分："), gbc2);
		gbc2.gridx = 1; gbc2.gridy = row; panelSchoolInfo.add(labFinishedCredit, gbc2);
		gbc2.gridx = 2; gbc2.gridy = row; panelSchoolInfo.add(new JLabel("毕业所需学分："), gbc2);
		gbc2.gridx = 3; gbc2.gridy = row; panelSchoolInfo.add(labRequiredCredit, gbc2);
		row++;

		// ----------------底部：一键刷新 + 保存修改按钮----------------
		JPanel btnPanel = new JPanel();
		btnPanel.setBackground(PAGE_BG);

		JButton btnRefreshAll = new JButton("一键刷新");
		btnRefreshAll.setBackground(BTN_GREEN);
		btnRefreshAll.setForeground(Color.WHITE);
		btnRefreshAll.setOpaque(true);
		btnRefreshAll.setBorderPainted(false);
		btnRefreshAll.setPreferredSize(new Dimension(120, 35));

		JButton btnSave = new JButton("保存修改");
		btnSave.setBackground(BTN_GREEN);
		btnSave.setForeground(Color.WHITE);
		btnSave.setOpaque(true);
		btnSave.setBorderPainted(false);
		btnSave.setPreferredSize(new Dimension(120, 35));

		btnPanel.add(btnRefreshAll);
		btnPanel.add(btnSave);

		// 学籍锁定判断
		boolean canEdit = !loginStudent.isLocked();
		txtPhone.setEnabled(canEdit);
		txtEmail.setEnabled(canEdit);
		txtAddress.setEnabled(canEdit);
		btnSave.setEnabled(canEdit);

		// 加入右侧面板
		rightContent.add(panelPersonal);
		rightContent.add(Box.createVerticalStrut(20));
		rightContent.add(panelSchoolInfo);
		rightContent.add(Box.createVerticalStrut(25));
		rightContent.add(btnPanel);

		// 左右合并
		mainBody.add(sideNav, BorderLayout.WEST);
		mainBody.add(rightContent, BorderLayout.CENTER);

		// 最外层组装
		add(topBar, BorderLayout.NORTH);
		add(mainBody, BorderLayout.CENTER);

		// ============【新增】异步加载实时数据 ============
		loadFinishedCredit(loginStudent.getSCard(), labFinishedCredit);
		loadSchoolExtraInfo(loginStudent.getClazzid(), loginStudent.getEnrollYear(),
				labMajorName, labDeptName, labRequiredCredit);

		// ============ 一键刷新按钮事件 ============
		btnRefreshAll.addActionListener(e -> refreshAll(txtPhone, txtEmail, txtAddress,
				labBalance, labDeptName, labMajorName, labFinishedCredit, labRequiredCredit));

		// ============ 保存按钮事件，走Socket通信 ============
		btnSave.addActionListener(e -> {
			loginStudent.setSphone(txtPhone.getText().trim());
			loginStudent.setSemail(txtEmail.getText().trim());
			loginStudent.setSaddress(txtAddress.getText().trim());

			new Thread(() -> {
				Message req = new Message();
				req.setType(MsgConst.UPDATE_STUDENT);
				req.setData(loginStudent);
				Message res = SocketClient.send(req);

				SwingUtilities.invokeLater(() -> {
					if (res.isSuccess()) {
						JOptionPane.showMessageDialog(this, res.getResponseMsg());
					} else {
						JOptionPane.showMessageDialog(this, res.getResponseMsg(), "操作提示", JOptionPane.WARNING_MESSAGE);
					}
				});
			}).start();
		});
	}

	/**
	 * ★ 新增：一键刷新
	 * 重新从服务端拉取学生信息，刷新余额、院系、专业、已修学分、所需学分
	 */
	private void refreshAll(JTextField txtPhone, JTextField txtEmail, JTextField txtAddress,
							JLabel labBalance, JLabel labDeptName, JLabel labMajorName,
							JLabel labFinishedCredit, JLabel labRequiredCredit) {
		new Thread(() -> {
			// 1. 重新拉取学生基本信息
			Message req = new Message();
			req.setType(MsgConst.QUERY_STUDENT_BY_SCARD);
			req.setData(loginStudent.getSCard());
			Message res = SocketClient.send(req);

			if (res == null || !res.isSuccess() || !(res.getData() instanceof Student)) {
				SwingUtilities.invokeLater(() ->
						JOptionPane.showMessageDialog(this, "刷新失败：" +
								(res == null ? "服务端无响应" : res.getResponseMsg())));
				return;
			}

			Student latest = (Student) res.getData();
			loginStudent = latest;

			// 2. 更新界面上的基本信息、联系方式、余额
			Double bal = latest.getSbalance();
			String balanceText = (bal == null ? "0.00" : String.format("%.2f", bal)) + " 元";

			SwingUtilities.invokeLater(() -> {
				txtPhone.setText(latest.getSphone() == null ? "" : latest.getSphone());
				txtEmail.setText(latest.getSemail() == null ? "" : latest.getSemail());
				txtAddress.setText(latest.getSaddress() == null ? "" : latest.getSaddress());
				labBalance.setText(balanceText);
				labDeptName.setText("查询中...");
				labMajorName.setText("查询中...");
				labFinishedCredit.setText("统计中...");
				labRequiredCredit.setText("-");
			});

			// 3. 重新加载已修学分
			loadFinishedCredit(latest.getSCard(), labFinishedCredit);

			// 4. 重新加载院系、专业、所需学分
			loadSchoolExtraInfo(latest.getClazzid(), latest.getEnrollYear(),
					labMajorName, labDeptName, labRequiredCredit);

		}).start();
	}

	/**
	 * 异步查询已修学分（走 CreditService 统计）
	 */
	private void loadFinishedCredit(String sCard, JLabel target) {
		new Thread(() -> {
			Message req = new Message();
			req.setType(MsgConst.QUERY_COMPLETED_CREDIT);
			Map<String, String> p = new HashMap<>();
			p.put("studentId", sCard);
			p.put("semester", "");
			req.setData(p);
			Message res = SocketClient.send(req);

			String text = "0";
			if (res != null && res.isSuccess() && res.getData() instanceof Map) {
				Object v = ((Map<?, ?>) res.getData()).get("completedCredit");
				if (v != null) text = String.valueOf(v);
			}
			final String finalText = text;
			SwingUtilities.invokeLater(() -> target.setText(finalText));
		}).start();
	}

	/**
	 * 异步查询：专业名、院系名、培养方案所需学分
	 * 链路：班级 → 专业 → 院系 + 培养方案
	 */
	private void loadSchoolExtraInfo(String clazzId, String grade,
									 JLabel labMajorName, JLabel labDeptName, JLabel labRequiredCredit) {
		if (clazzId == null || clazzId.trim().isEmpty()) {
			SwingUtilities.invokeLater(() -> {
				labMajorName.setText("-");
				labDeptName.setText("-");
				labRequiredCredit.setText("-");
			});
			return;
		}
		new Thread(() -> {
			String majorName = null;
			String deptName = null;
			String creditText = "-";

			// 1. 班级 → 专业ID
			Message reqClazz = new Message();
			reqClazz.setType(MsgConst.QUERY_CLAZZ_BY_CLAZZID);
			reqClazz.setData(clazzId);
			Message resClazz = SocketClient.send(reqClazz);
			String majorId = null;
			if (resClazz != null && resClazz.isSuccess() && resClazz.getData() instanceof Clazz) {
				majorId = ((Clazz) resClazz.getData()).getMajorid();
			}

			// 2. 专业ID → 专业名 + 院系ID
			String deptId = null;
			if (majorId != null && !majorId.trim().isEmpty()) {
				Message reqMajor = new Message();
				reqMajor.setType(MsgConst.QUERY_MAJOR_BY_MAJORID);
				reqMajor.setData(majorId);
				Message resMajor = SocketClient.send(reqMajor);
				if (resMajor != null && resMajor.isSuccess() && resMajor.getData() instanceof Major) {
					Major major = (Major) resMajor.getData();
					majorName = major.getMajorname();
					deptId = major.getDeptid();
				}
			}

			// 3. 院系ID → 院系名
			if (deptId != null && !deptId.trim().isEmpty()) {
				Message reqDept = new Message();
				reqDept.setType(MsgConst.QUERY_DEPT_BY_DEPTID);
				reqDept.setData(deptId);
				Message resDept = SocketClient.send(reqDept);
				if (resDept != null && resDept.isSuccess() && resDept.getData() instanceof Department) {
					deptName = ((Department) resDept.getData()).getDeptname();
				}
			}

			// 4. 专业名 + 入学年份 → 培养方案所需学分
			if (majorName != null && !majorName.trim().isEmpty()
					&& grade != null && !grade.trim().isEmpty()) {
				Message reqProgram = new Message();
				reqProgram.setType(MsgConst.QUERY_PROGRAM_BY_MAJOR_AND_GRADE);
				Map<String, String> pp = new HashMap<>();
				pp.put("major", majorName);
				pp.put("grade", grade);
				reqProgram.setData(pp);
				Message resProgram = SocketClient.send(reqProgram);
				if (resProgram != null && resProgram.isSuccess() && resProgram.getData() instanceof Integer) {
					creditText = String.valueOf(resProgram.getData());
				}
			}

			final String fMajor = majorName == null ? "-" : majorName;
			final String fDept = deptName == null ? "-" : deptName;
			final String fCredit = creditText;
			SwingUtilities.invokeLater(() -> {
				labMajorName.setText(fMajor);
				labDeptName.setText(fDept);
				labRequiredCredit.setText(fCredit);
			});
		}).start();
	}
}