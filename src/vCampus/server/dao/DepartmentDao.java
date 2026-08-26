package vCampus.server.dao;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import vCampus.common.vo.Department;

public class DepartmentDao {
	public List<Department> findAll(){
		List<Department> deptList = new ArrayList<>();
		
		Connection conn = null;
		Statement stmt = null;
		ResultSet rs = null;
		
		try {
			conn = DBUtil.getConnection();
			stmt = conn.createStatement();
			String sql = "SELECT deptid, deptname FROM Department";
			rs = stmt.executeQuery(sql);
			
			while(rs.next()) {
				String id = rs.getString("deptid");
				String name = rs.getString("deptname");
				
				Department dept = new Department(id,name);
				
				deptList.add(dept);
			}
		} catch(Exception e) {
			e.printStackTrace();
		} finally {
			DBUtil.close(conn);
		}
		
		return deptList;
	}
}

