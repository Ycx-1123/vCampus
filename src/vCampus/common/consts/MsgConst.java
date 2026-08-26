package vCampus.common.consts;

public final class MsgConst {
	//====学籍管理type常量====
	//Department
	public static final String QUERY_ALL_DEPT = "QUERY_ALL_DEPT";
    public static final String ADD_DEPT = "ADD_DEPT";
    public static final String UPDATE_DEPT = "UPDATE_DEPT";
    public static final String DELETE_DEPT = "DELETE_DEPT";
    //Clazz
    public static final String QUERY_CLASS_BY_DEPTID = "QUERY_CLASS_BY_DEPTID";
    public static final String ADD_CLASS = "ADD_CLASS";
    public static final String UPDATE_CLASS = "UPDATE_CLASS";
    public static final String DELETE_CLASS = "DELETE_CLASS";
    //Student
    public static final String QUERY_STUDENT_BY_CID = "QUERY_STUDENT_BY_CID";
    public static final String QUERY_STUDENT_BY_SID = "QUERY_STUDENT_BY_SID";
    public static final String QUERY_ALL_STUDENT = "QUERY_ALL_STUDENT";
    public static final String ADD_STUDENT = "ADD_STUDENT";
    public static final String UPDATE_STUDENT = "UPDATE_STUDENT";
    public static final String DELETE_STUDENT = "DELETE_STUDENT";
    
    //====选课模块type常量====
    public static final String QUERY_ALL_COURSES="QUERY_ALL_COURSES";//查询所有课程
    public static final String SELECT_COURSE="SELECT_COURSE";//选课
    public static final String QUIT_COURSE="QUIT_COURSE";//退课
    public static final String GET_SELECTED_COURSES="GET_SELECTED_COURSES";//查询已选课程
    
    
    private MsgConst(){
    }
}
