package vCampus.common.vo;

import java.io.Serializable;

public class Clazz implements Serializable{
	private static final long serialVersionUID = 1L;
	
	private String cid;
	//专业名称
	private String expername;
	//所属部门编号
	private String deptid;
	
	public Clazz() {
	}
	
	public Clazz(String cid,String expername,String deptid) {
		this.cid = cid;
		this.expername = expername;
		this.deptid = deptid;
	}
	
	public String getCid() {
        return cid;
    }

    public void setCid(String cid) {
        this.cid = cid;
    }

    public String getExpername() {
        return expername;
    }

    public void setExpername(String expername) {
        this.expername = expername;
    }

    public String getDeptid() {
        return deptid;
    }

    public void setDeptid(String deptid) {
        this.deptid = deptid;
    }
}
