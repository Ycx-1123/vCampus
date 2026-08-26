package vCampus.common.vo;

import java.io.Serializable;


public class Book implements Serializable {
	
	// 推荐添加 serialVersionUID，保证网络传输时版本一致，防止反序列化报错
	private static final long serialVersionUID = 1L;
	
	// --- 字段定义 (对应 Access 数据库 tbl_book 表) ---
	private String bId;         // ISBN码 (主键)
	private String bName;       // 书名
	private String bAuthor;     // 作者
	private String bPublisher;  // 出版社
	private int bTotal;         // 总库存量
	private int bAvailable;     // 当前可借余量

	// --- 构造函数 ---
	
	public Book() {
	}

	// 2. 全参构造函数（方便我们在代码里快速 new 一个图书对象）
	public Book(String bId, String bName, String bAuthor, String bPublisher, int bTotal, int bAvailable) {
		this.bId = bId;
		this.bName = bName;
		this.bAuthor = bAuthor;
		this.bPublisher = bPublisher;
		this.bTotal = bTotal;
		this.bAvailable = bAvailable;
	}

	// --- Getters 和 Setters (供外部读取和修改私有属性) ---

	public String getbId() {
		return bId;
	}

	public void setbId(String bId) {
		this.bId = bId;
	}

	public String getbName() {
		return bName;
	}

	public void setbName(String bName) {
		this.bName = bName;
	}

	public String getbAuthor() {
		return bAuthor;
	}

	public void setbAuthor(String bAuthor) {
		this.bAuthor = bAuthor;
	}

	public String getbPublisher() {
		return bPublisher;
	}

	public void setbPublisher(String bPublisher) {
		this.bPublisher = bPublisher;
	}

	public int getbTotal() {
		return bTotal;
	}

	public void setbTotal(int bTotal) {
		this.bTotal = bTotal;
	}

	public int getbAvailable() {
		return bAvailable;
	}

	public void setbAvailable(int bAvailable) {
		this.bAvailable = bAvailable;
	}

	// --- toString 方法 (方便我们在控制台 System.out.println 打印图书信息，用来调试) ---
	@Override
	public String toString() {
		return "Book [ISBN=" + bId + ", 书名=" + bName + ", 作者=" + bAuthor + 
			   ", 出版社=" + bPublisher + ", 总量=" + bTotal + ", 剩余=" + bAvailable + "]";
	}
}