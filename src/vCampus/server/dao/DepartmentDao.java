package vCampus.server.dao;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.PreparedStatement;
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
			
		}catch(Exception e) {
			e.printStackTrace();
		}finally {
			DBUtil.close(conn,stmt,rs);
		}
		return deptList;
	}
	
	public Department findById(String deptid) {
		Department dept = null;
		Connection conn = null;
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		
		try {
			conn = DBUtil.getConnection();
			String sql = "SELECT deptid, deptname FROM Department WHERE deptid=?";
			pstmt = conn.prepareStatement(sql);
			pstmt.setString(1, deptid);
			rs = pstmt.executeQuery();
			
			if(rs.next()) {
				String name = rs.getString("deptname");
				dept = new Department(deptid,name);
			}
		}catch(Exception e) {
			e.printStackTrace();
		}finally {
			DBUtil.close(conn,pstmt,rs);
		}
		return dept;
	}
	
	public boolean add(Department dept) {
		boolean success = false;
		Connection conn = null;
		PreparedStatement pstmt = null;
		
		try {
			conn = DBUtil.getConnection();
			String sql = "INSERT INTO Department(deptid,deptname) VALUES (?,?)";
			pstmt = conn.prepareStatement(sql);
			pstmt.setString(1, dept.getDeptid());
			pstmt.setString(2, dept.getDeptname());
			
			int rows = pstmt.executeUpdate();
			if(rows>0) success = true;
			
		}catch(Exception e) {
			e.printStackTrace();
		}finally {
			DBUtil.close(conn,pstmt,null);
		}
		return success;
	}
	
	public boolean update(Department dept) {
		boolean success = false;
		Connection conn = null;
		PreparedStatement pstmt = null;
		
		try {
			conn = DBUtil.getConnection();
			String sql = "UPDATE Department SET deptname=? WHERE deptid=?";
			pstmt = conn.prepareStatement(sql);
			pstmt.setString(1, dept.getDeptname());
			pstmt.setString(2, dept.getDeptid());
			
			int rows = pstmt.executeUpdate();
			if(rows>0) success = true;
			
		}catch(Exception e) {
			e.printStackTrace();
		}finally {
			DBUtil.close(conn,pstmt,null);
		}
		return success;
	}
	
	public boolean delete(String deptid) {
		boolean success = false;
		Connection conn = null;
		PreparedStatement pstmt = null;
		
		try {
			conn = DBUtil.getConnection();
			String sql = "DELETE FROM Department WHERE deptid=?";
			pstmt = conn.prepareStatement(sql);
			pstmt.setString(1, deptid);
			
			int rows = pstmt.executeUpdate();
			if(rows>0) success = true;
			
		}catch(Exception e) {
			e.printStackTrace();
		}finally {
			DBUtil.close(conn,pstmt,null);
		}
		return success;
	}
}

