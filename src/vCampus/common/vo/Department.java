package vCampus.common.vo;

import java.io.Serializable;

public class Department implements Serializable {
	private static final long serialVersionUID = 1L;
	
	//Department ID
	private String deptid;
	//Department name
	private String deptname;
	
	public Department() {
	}
	
	public Department(String deptid,String deptname) {
		this.deptid = deptid;
		this.deptname = deptname;
	}
	
	public String getDeptid() {
		return deptid;
	}
	
	public void setDeptid(String deptid) {
		this.deptid = deptid;
	}
	
	public String getDeptname() {
		return deptname;
	}
	
	public void setDeptname(String deptname) {
		this.deptname = deptname;
	}
	
	
}
