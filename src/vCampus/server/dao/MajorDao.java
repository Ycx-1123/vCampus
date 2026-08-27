package vCampus.server.dao;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.PreparedStatement;
import java.util.ArrayList;
import java.util.List;
import vCampus.common.vo.Major;

public class MajorDao {
	public List<Major> findAll(){
		List<Major> list = new ArrayList<>();
		Connection conn = null;
		Statement stmt = null;
		ResultSet rs = null;
		
		try {
			conn = DBUtil.getConnection();
			stmt = conn.createStatement();
			String sql = "SELECT majorid, majorname, deptid FROM Major";
			rs = stmt.executeQuery(sql);
			
			while(rs.next()) {
				Major m = new Major();
				m.setMajorid(rs.getString("majorid"));
				m.setMajorname(rs.getString("majorname"));
				m.setDeptid(rs.getString("deptid"));
				list.add(m);
			}
			
		}catch(Exception e) {
			e.printStackTrace();
		}finally {
			DBUtil.close(conn,stmt,rs);
		}
		return list;
	}
	
	public Major findById(String majorid) {
		Major m = null;
		Connection conn = null;
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		
		try {
			conn = DBUtil.getConnection();
			String sql = "SELECT majorid, majorname, deptid FROM Major WHERE majorid=?";
			pstmt = conn.prepareStatement(sql);
			pstmt.setString(1, majorid);
			rs = pstmt.executeQuery();
			
			if(rs.next()) {
				m = new Major();
				m.setMajorid(rs.getString("majorid"));
				m.setMajorname(rs.getString("majorname"));
				m.setDeptid(rs.getString("deptid"));
			}
		}catch(Exception e) {
			e.printStackTrace();
		}finally {
			DBUtil.close(conn,pstmt,rs);
		}
		return m;
	}
	
	public List<Major> findByDeptId(String deptid){
		List<Major> list = new ArrayList<>();
		Connection conn = null;
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		
		try {
			conn = DBUtil.getConnection();
			String sql = "SELECT majorid, majorname, deptid FROM Major WHERE deptid=?";
			pstmt = conn.prepareStatement(sql);
			pstmt.setString(1, deptid);
			rs = pstmt.executeQuery();
			
			while(rs.next()) {
				Major m = new Major();
				m.setMajorid(rs.getString("majorid"));
				m.setMajorname(rs.getString("majorname"));
				m.setDeptid(rs.getString("deptid"));
				list.add(m);
			}
		}catch(Exception e) {
			e.printStackTrace();
		}finally {
			DBUtil.close(conn,pstmt,rs);
		}
		return list;
	}
	
	public boolean add(Major major) {
		boolean success = false;
		Connection conn = null;
		PreparedStatement pstmt = null;
		
		try {
			conn = DBUtil.getConnection();
			String sql = "INSERT INTO Major(majorid,majorname,deptid) VALUES (?,?,?)";
			pstmt = conn.prepareStatement(sql);
			pstmt.setString(1, major.getMajorid());
			pstmt.setString(2, major.getMajorname());
			pstmt.setString(3, major.getDeptid());
			
			int rows = pstmt.executeUpdate();
			if(rows>0) success = true;
			
		}catch(Exception e) {
			e.printStackTrace();
		}finally {
			DBUtil.close(conn,pstmt,null);
		}
		return success;
	}
	
	public boolean update(Major major) {
		boolean success = false;
		Connection conn = null;
		PreparedStatement pstmt = null;
		
		try {
			conn = DBUtil.getConnection();
			String sql = "UPDATE Major SET majorname=?, deptid=? WHERE majorid=?";
			pstmt = conn.prepareStatement(sql);
			pstmt.setString(1, major.getMajorname());
			pstmt.setString(2, major.getDeptid());
			pstmt.setString(3, major.getMajorid());
			
			int rows = pstmt.executeUpdate();
			if(rows>0) success = true;
			
		}catch(Exception e) {
			e.printStackTrace();
		}finally {
			DBUtil.close(conn,pstmt,null);
		}
		return success;
	}
	
	public boolean delete(String majorid) {
		boolean success = false;
		Connection conn = null;
		PreparedStatement pstmt = null;
		
		try {
			conn = DBUtil.getConnection();
			String sql = "DELETE FROM Major WHERE majorid=?";
			pstmt = conn.prepareStatement(sql);
			pstmt.setString(1, majorid);
			
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
