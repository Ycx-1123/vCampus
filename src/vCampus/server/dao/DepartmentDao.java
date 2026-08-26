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
	
	public Department findById(String deptid) {
		Department dept = null;
		Connection conn = null;
		Statement stmt = null;
		ResultSet rs = null;
		try {
			conn = DBUtil.getConnection();
			stmt = conn.createStatement();
			String sql = "SELECT deptid,deptname FROM Department WHERE deptid='"+deptid+"'";
			rs = stmt.executeQuery(sql);
			if(rs.next()) {
				String id = rs.getString("deptid");
				String name = rs.getString("deptname");
                dept = new Department(id,name);
			}
		} catch(Exception e) {
			e.printStackTrace();
		} finally {
			DBUtil.close(conn);
		}
		return dept;
	}
	
	public boolean add(Department dept) {
		Connection conn = null;
		Statement stmt = null;
		try {
			conn = DBUtil.getConnection();
			stmt = conn.createStatement();
			String sql = "INSERT INTO Department(deptid,deptname) VALUES('"+dept.getDeptid()+"','"+dept.getDeptname()+"')";
			
			int rows = stmt.executeUpdate(sql);
			return rows>0;
		} catch(Exception e) {
			e.printStackTrace();
		} finally {
			DBUtil.close(conn);
		}
		return false;
	}
	
	public boolean update(Department dept) {
        Connection conn = null;
        Statement stmt = null;
        try {
            conn = DBUtil.getConnection();
            stmt = conn.createStatement();
            String sql = "UPDATE Department SET deptname='"+dept.getDeptname()
                    +"' WHERE deptid='"+dept.getDeptid()+"'";
            int rows = stmt.executeUpdate(sql);
            return rows>0;
        } catch (Exception e) {
            e.printStackTrace();
        }finally {
            DBUtil.close(conn);
        }
        return false;
    }
	
	public boolean delete(String deptid) {
        Connection conn = null;
        Statement stmt = null;
        try {
            conn = DBUtil.getConnection();
            stmt = conn.createStatement();
            String sql = "DELETE FROM Department WHERE deptid='"+deptid+"'";
            int rows = stmt.executeUpdate(sql);
            return rows>0;
        } catch (Exception e) {
            e.printStackTrace();
        }finally {
            DBUtil.close(conn);
        }
        return false;
    }
}

