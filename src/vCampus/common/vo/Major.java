package vCampus.common.vo;

import java.io.Serializable;

public class Major implements Serializable {
	private static final long serialVersionUID = 1L;

    private String majorid;
    private String majorname;
    private String deptid; //外键：所属部门编号

    public Major() {
    }
    public Major(String majorid, String majorname, String deptid) {
        this.majorid = majorid;
        this.majorname = majorname;
        this.deptid = deptid;
    }

    public String getMajorid() {
        return majorid;
    }
    public void setMajorid(String majorid) {
        this.majorid = majorid;
    }
    public String getMajorname() {
        return majorname;
    }
    public void setMajorname(String majorname) {
        this.majorname = majorname;
    }
    public String getDeptid() {
        return deptid;
    }
    public void setDeptid(String deptid) {
        this.deptid = deptid;
    }
}
