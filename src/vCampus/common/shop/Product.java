package vCampus.common.shop;

import java.io.Serializable;

/**
 * 商品实体类，用于客户端与服务器端之间的数据传输。
 * @author moxuan_pan
 * @version 1.0
 */
public class Product {

    private static final long serialVersionUID = 1L; // 建议加上序列化版本号

    private int id;
    private String name;
    private double price;
    private int stock;
    private String description;

    // 无参构造方法
    public Product() {}

    // 带参构造方法（方便创建对象）
    public Product(int id, String name, double price, int stock, String description) {
        this.id = id;
        this.name = name;
        this.price = price;
        this.stock = stock;
        this.description = description;
    }

    public static long getSerialversionuid() {
		return serialVersionUID;
	}

	// 所有的 Getter 和 Setter
    // 在 Eclipse 中：右键 → Source → Generate Getters and Setters... → 全选 → Generate
 
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public double getPrice() { return price; }
    public void setPrice(double price) { this.price = price; }

    public int getStock() { return stock; }
    public void setStock(int stock) { this.stock = stock; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    // 可选：重写 toString 方便调试
    @Override
    public String toString() {
        return "Product [id=" + id + ", name=" + name + ", price=" + price + ", stock=" + stock + "]";
    }
}