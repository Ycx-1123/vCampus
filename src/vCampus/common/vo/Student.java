package vCampus.common.vo;

import java.io.Serializable;

public class Student implements Serializable {
	private static final long serialVersionUID = 1L;
	
	private String stuid;
    private String stuname;
    private String gender;
    private String clazzid;       //外键：所属班级编号
    private String enrollYear;    //入学年份（年级）
    private String stuCard;        //一卡通账号
    private Boolean isLocked;     //学籍是否锁定 true=锁定
    private Double finishedCredit;//已经修完的总学分
    private Double requiredCredit;//毕业最低所需学分

    public Student() {
    }

    public Student(String stuid, String stuname, String gender, String clazzid,
                   String enrollYear, String stuCard, boolean isLocked, double finishedCredit, double requiredCredit) {
        this.stuid = stuid;
        this.stuname = stuname;
        this.gender = gender;
        this.clazzid = clazzid;
        this.enrollYear = enrollYear;
        this.stuCard = stuCard;
        this.isLocked = isLocked;
        this.finishedCredit = finishedCredit;
        this.requiredCredit = requiredCredit;
    }

    public String getStuid() {
        return stuid;
    }
    public void setStuid(String stuid) {
        this.stuid = stuid;
    }
    public String getStuname() {
        return stuname;
    }
    public void setStuname(String stuname) {
        this.stuname = stuname;
    }
    public String getGender() {
        return gender;
    }
    public void setGender(String gender) {
        this.gender = gender;
    }
    public String getClazzid() {
        return clazzid;
    }
    public void setClazzid(String clazzid) {
        this.clazzid = clazzid;
    }
    public String getEnrollYear() {
        return enrollYear;
    }
    public void setEnrollYear(String enrollYear) {
        this.enrollYear = enrollYear;
    }
    //一卡通账号 getter‑setter
    public String getStuCard() {
        return stuCard;
    }
    public void setStuCard(String stuCard) {
        this.stuCard = stuCard;
    }
    public boolean isLocked() {
        return isLocked;
    }
    public void setLocked(boolean locked) {
        isLocked = locked;
    }
    public double getFinishedCredit() {
        return finishedCredit;
    }
    public void setFinishedCredit(double finishedCredit) {
        this.finishedCredit = finishedCredit;
    }
    public double getRequiredCredit() {
        return requiredCredit;
    }
    public void setRequiredCredit(double requiredCredit) {
        this.requiredCredit = requiredCredit;
    }
}
