package vCampus.server.dao;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.PreparedStatement;
import java.util.ArrayList;
import java.util.List;
import vCampus.common.vo.Clazz;

public class ClazzDao {
	public List<Clazz> findAll(){
		List<Clazz> list = new ArrayList<>();
		Connection conn = null;
		Statement stmt = null;
		ResultSet rs = null;
		
		try {
			conn = DBUtil.getConnection();
			stmt = conn.createStatement();
			String sql = "SELECT clazzid, majorid FROM Clazz";
			rs = stmt.executeQuery(sql);
			
			while(rs.next()) {
				Clazz c =new Clazz();
				c.setClazzid(rs.getString("clazzid"));
				c.setMajorid(rs.getString("majorid"));
				list.add(c);
			}
		} catch(Exception e) {
			e.printStackTrace();
		} finally {
			DBUtil.close(conn, stmt, rs);
		}
		return list;
	}
	
	public Clazz findById(String clazzid) {
		Clazz c = null;
		Connection conn = null;
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		
		try {
			conn = DBUtil.getConnection();
			String sql = "SELECT clazzid, majorid FROM Clazz WHERE clazzid=?";
			pstmt = conn.prepareStatement(sql);
			pstmt.setString(1, clazzid);
			rs = pstmt.executeQuery();
			
			if(rs.next()) {
				c = new Clazz();
				c.setClazzid(rs.getString("clazzid"));
				c.setMajorid(rs.getString("majorid"));
			}
		}catch(Exception e) {
			e.printStackTrace();
		}finally {
			DBUtil.close(conn,pstmt,rs);
		}
		return c;
	}
	
	public List<Clazz> findByMajorId(String majorid){
		List<Clazz> list = new ArrayList<>();
		Connection conn = null;
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		
		try {
			conn = DBUtil.getConnection();
			String sql = "SELECT clazzid, majorid FROM Clazz WHERE majorid=?";
			pstmt = conn.prepareStatement(sql);
			pstmt.setString(1, majorid);
			rs = pstmt.executeQuery();
			
			while(rs.next()) {
				Clazz c = new Clazz();
				c.setClazzid(rs.getString("clazzid"));
				c.setMajorid(rs.getString("majorid"));
				list.add(c);
			}
		}catch(Exception e) {
			e.printStackTrace();
		}finally {
			DBUtil.close(conn,pstmt,rs);
		}
		return list;
	}
	
	public boolean add(Clazz clazz) {
		boolean success = false;
		Connection conn = null;
		PreparedStatement pstmt = null;
		
		try {
			conn = DBUtil.getConnection();
			String sql = "INSERT INTO Clazz(clazzid,majorid) VALUES (?,?)";
			pstmt = conn.prepareStatement(sql);
			pstmt.setString(1, clazz.getClazzid());
			pstmt.setString(2, clazz.getMajorid());
			
			int rows = pstmt.executeUpdate();
			if(rows>0) success = true;
			
		}catch(Exception e) {
			e.printStackTrace();
		}finally {
			DBUtil.close(conn,pstmt,null);
		}
		return success;
	}
	
	public boolean update(Clazz clazz) {
		boolean success = false;
		Connection conn = null;
		PreparedStatement pstmt = null;
		
		try {
			conn = DBUtil.getConnection();
			String sql = "UPDATE Clazz SET majorid=? WHERE clazzid=?";
			pstmt = conn.prepareStatement(sql);
			pstmt.setString(1, clazz.getMajorid());
			pstmt.setString(2, clazz.getClazzid());
			
			int rows = pstmt.executeUpdate();
			if(rows>0) success = true;
			
		}catch(Exception e) {
			e.printStackTrace();
		}finally {
			DBUtil.close(conn,pstmt,null);
		}
		return success;
	}
	
	public boolean delete(String clazzid) {
		boolean success = false;
		Connection conn = null;
		PreparedStatement pstmt = null;
		
		try {
			conn = DBUtil.getConnection();
			String sql = "DELETE FROM Clazz WHERE clazzid=?";
			pstmt = conn.prepareStatement(sql);
			pstmt.setString(1, clazzid);
			
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
