package vCampus.common.vo;

import java.io.Serializable;

public class Course implements Serializable{
	private static final long serialVersionUID=1L;
	
	private String courseId;//课程编号
	private String courseName;//课程名称
	private double credit;//学分
	private int hours;//课时
	private String teacherId;//教师编号
	private String teacherName;//教师姓名
	private int capacity;//容量上限
	private int enrolledCount;//已选人数
	private String schedule;//上课时间
    
	public Course() {}
	
	public Course(String courseId,String courseName,double credit,int hours,String teacherId,String teacherName,int capacity,String schedule) {
		this.courseId=courseId;
		this.courseName=courseName;
		this.credit=credit;
		this.hours=hours;
		this.teacherId=teacherId;
		this.teacherName=teacherName;
		this.capacity=capacity;
		this.enrolledCount=0;
		this.schedule=schedule;
		}
		
		// Getter 和 Setter 方法
	public String getCourseId() { return courseId; }
	public void setCourseId(String courseId) { this.courseId = courseId; }
	public String getCourseName() { return courseName; }
	public void setCourseName(String courseName) { this.courseName = courseName; }
	public double getCredit() { return credit; }
	public void setCredit(double credit) { this.credit = credit; }
	public int getHours() { return hours; }
	public void setHours(int hours) { this.hours = hours; }
	public String getTeacherId() { return teacherId; }
	public void setTeacherId(String teacherId) { this.teacherId = teacherId; }
	public String getTeacherName() { return teacherName; }
	public void setTeacherName(String teacherName) { this.teacherName = teacherName; }
	public int getCapacity() { return capacity; }
	public void setCapacity(int capacity) { this.capacity = capacity; }
	public int getEnrolledCount() { return enrolledCount; }
	public void setEnrolledCount(int enrolledCount) { this.enrolledCount = enrolledCount; }
	public String getSchedule() { return schedule; }
	public void setSchedule(String schedule) { this.schedule = schedule; }
	
	//判断课程是否已满
	public boolean isFull() {
		return enrolledCount>=capacity;
	}
	public int getRemainingSlots() {
		return capacity-enrolledCount;
	}
}
