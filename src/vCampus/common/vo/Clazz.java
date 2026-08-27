package vCampus.common.vo;

import java.io.Serializable;

public class Clazz implements Serializable{
	private static final long serialVersionUID = 1L;
	
	private String clazzid;
    private String majorid; //外键：所属专业编号

    public Clazz() {
    }
    public Clazz(String clazzid, String majorid) {
        this.clazzid = clazzid;
        this.majorid = majorid;
    }

    public String getClazzid() {
        return clazzid;
    }
    public void setClazzid(String clazzid) {
        this.clazzid = clazzid;
    }
    public String getMajorid() {
        return majorid;
    }
    public void setMajorid(String majorid) {
        this.majorid = majorid;
    }
}
