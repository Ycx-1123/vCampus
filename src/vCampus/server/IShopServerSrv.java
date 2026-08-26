package vCampus.server;
import java.util.List; // 导入 List 接口
import vCampus.common.shop.Product; // 导入 Product 类

public interface IShopServerSrv {
    List<Product> getAllProducts(); // 获取所有商品
    boolean purchaseProduct(int productId, int quantity); // 购买（扣库存）
}