package vCampus.common.vo;

import java.io.Serializable;

public class Student implements Serializable {
	private static final long serialVersionUID = 1L;
	
	private String sid;
	private String sname;
	private String sex;
	private String cid;
	private String email;
	private String idCard;
	
	public Student() {
	}
	
	public Student(String sid, String sname, String sex, String cid, String email, String idCard) {
        this.sid = sid;
        this.sname = sname;
        this.sex = sex;
        this.cid = cid;
        this.email = email;
        this.idCard = idCard;
    }
	
	public String getSid() {
        return sid;
    }

    public void setSid(String sid) {
        this.sid = sid;
    }

    public String getSname() {
        return sname;
    }

    public void setSname(String sname) {
        this.sname = sname;
    }

    public String getSex() {
        return sex;
    }

    public void setSex(String sex) {
        this.sex = sex;
    }

    public String getCid() {
        return cid;
    }

    public void setCid(String cid) {
        this.cid = cid;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getIdCard() {
        return idCard;
    }

    public void setIdCard(String idCard) {
        this.idCard = idCard;
    }
}
