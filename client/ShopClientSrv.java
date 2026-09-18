package vCampus.client;

import java.util.HashMap;
import java.util.Map;
import java.util.List;

import vCampus.common.Message;
import vCampus.common.shop.Order;
import vCampus.common.shop.OrderItem;
import vCampus.common.shop.Product;

public class ShopClientSrv {

	/**
	 * 上架新商品（管理员专用）
	 * 
	 * @param product 新商品对象（id 会自动生成，无需设置）
	 * @return true 表示上架成功
	 */
	public static boolean addProduct(Product product) {
		Message response = request("ADD_PRODUCT", product);
		return response != null && response.isSuccess();
	}

	/**
	 * 增加库存（管理员补货）
	 * 
	 * @param productId 商品ID
	 * @param amount    增加的数量（正数）
	 * @return true 表示增加成功
	 */
	public static boolean addStock(int productId, int amount) {
		Map<String, Object> params = new HashMap<>();
		params.put("productId", productId);
		params.put("amount", amount);

		Message response = request("ADD_STOCK", params);
		return response != null && response.isSuccess();
	}

	/**
	 * 下架商品（管理员专用）
	 * 
	 * @param productId 商品ID
	 * @return true 表示下架成功
	 */
	public static boolean offlineProduct(int productId) {
		Message response = request("DELETE_PRODUCT", productId);
		return response != null && response.isSuccess();
	}

	/**
	 * 获取所有商品
	 */
	public static List<Product> getAllProducts() {
		Message response = request("GET_ALL_PRODUCTS", null);
		if (response != null && response.isSuccess()) {
			return (List<Product>) response.getData();
		} else {
			System.err.println("服务器错误：" + responseMsg(response));
			return null;
		}
	}

	/**
	 * 提交订单（返回生成的 Order 对象，失败返回 null）
	 */
	public static Order submitOrder(List<OrderItem> items, String userId) {
		Map<String, Object> params = new HashMap<>();
		params.put("userId", userId);
		params.put("items", items);

		Message response = request("SUBMIT_ORDER", params);
		if (response != null && response.isSuccess()) {
			return (Order) response.getData();
		} else {
			System.err.println("下单失败：" + responseMsg(response));
			return null;
		}
	}

	/**
	 * 获取用户历史订单
	 */
	public static List<Order> getOrdersByUser(String userId) {
		Message response = request("GET_ORDERS", userId);
		if (response != null && response.isSuccess()) {
			return (List<Order>) response.getData();
		} else {
			System.err.println("获取订单失败：" + responseMsg(response));
			return null;
		}
	}

	/**
	 * 查询当前用户商店可用余额
	 */
	public static double getBalance(String userId, String payMethod) {
		Map<String, Object> params = new HashMap<>();
		params.put("userId", userId);
		params.put("payMethod", payMethod);

		Message response = request("GET_SHOP_BALANCE", params);
		if (response != null && response.isSuccess() && response.getData() instanceof Number) {
			return ((Number) response.getData()).doubleValue();
		}
		System.err.println("获取余额失败：" + responseMsg(response));
		return -1;
	}

	/**
	 * 提交订单（带支付方式）
	 * 
	 * @param params 包含 userId, items, payMethod, totalAmount
	 * @return Order 或 null
	 */
	public static Order submitOrderWithPayment(Map<String, Object> params) {
		Message response = request("SUBMIT_ORDER", params);
		if (response != null && response.isSuccess()) {
			return (Order) response.getData();
		} else {
			System.err.println("下单失败：" + responseMsg(response));
			return null;
		}
	}

	public static Message submitOrderWithPaymentMessage(Map<String, Object> params) {

		return request("SUBMIT_ORDER", params);
	}

	private static Message request(String type, Object data) {
		return SocketClient.send(new Message(type, data));
	}

	private static String responseMsg(Message response) {
		return response == null ? "服务器无响应" : response.getResponseMsg();
	}
}
