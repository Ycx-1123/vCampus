package vCampus.client;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import vCampus.common.Message;
import vCampus.common.consts.MsgConst;
import vCampus.common.vo.BankAccount;
import vCampus.common.vo.BankTransaction;

public final class BankClientSrv {
	private BankClientSrv() {
	}

	private static Message request(String type, Object data) {
		return SocketClient.send(new Message(type, data));
	}

	public static BankAccount getAccount(String holderCard) {
		Message m = request(MsgConst.BANK_GET_ACCOUNT, holderCard);
		return m.isSuccess() && m.getData() instanceof BankAccount ? (BankAccount) m.getData() : null;
	}

	public static BankAccount openAccount(String holderCard, String holderName, String bankPIN) {

		Map<String, Object> p = new HashMap<String, Object>();
		p.put("holderCard", holderCard);
		p.put("holderName", holderName);
		p.put("bankPIN", bankPIN);

		Message m = request(MsgConst.BANK_OPEN_ACCOUNT, p);

		return m.isSuccess() && m.getData() instanceof BankAccount ? (BankAccount) m.getData() : null;
	}

	public static Message deposit(String holderCard, double amount, String bankPIN) {

		Map<String, Object> p = new HashMap<String, Object>();
		p.put("holderCard", holderCard);
		p.put("amount", amount);
		p.put("bankPIN", bankPIN);

		return request(MsgConst.BANK_DEPOSIT, p);
	}

	public static Message withdraw(String holderCard, double amount, String bankPIN) {

		Map<String, Object> p = new HashMap<String, Object>();
		p.put("holderCard", holderCard);
		p.put("amount", amount);
		p.put("bankPIN", bankPIN);

		return request(MsgConst.BANK_WITHDRAW, p);
	}

	public static Message rechargeCampus(String holderCard, double amount, String bankPIN) {

		Map<String, Object> p = new HashMap<String, Object>();
		p.put("holderCard", holderCard);
		p.put("amount", amount);
		p.put("bankPIN", bankPIN);

		return request(MsgConst.BANK_RECHARGE_CAMPUS, p);
	}

	public static Message payDorm(String holderCard, double amount, String bankPIN, int billId) {

		Map<String, Object> p = new HashMap<String, Object>();

		p.put("holderCard", holderCard);
		p.put("amount", amount);
		p.put("bankPIN", bankPIN);
		p.put("billId", billId);

		return request(MsgConst.BANK_DORM_PAYMENT, p);
	}

	public static Message transfer(String holderCard, String targetBankCard, double amount, String bankPIN) {

		Map<String, Object> p = new HashMap<String, Object>();
		p.put("holderCard", holderCard);
		p.put("targetBankCard", targetBankCard);
		p.put("amount", amount);
		p.put("bankPIN", bankPIN);

		return request(MsgConst.BANK_TRANSFER, p);
	}

	public static List<BankTransaction> getTransactions(String holderCard) {

		Message m = request(MsgConst.BANK_GET_TRANSACTIONS, holderCard);

		if (m.isSuccess() && m.getData() instanceof List<?>) {
			return castList(m.getData());
		}

		return Collections.emptyList();
	}

	public static List<BankAccount> getAllAccounts(String adminCard) {

		Message m = request(MsgConst.BANK_ADMIN_GET_ACCOUNTS, adminCard);

		if (m.isSuccess() && m.getData() instanceof List<?>) {
			return castList(m.getData());
		}

		return Collections.emptyList();
	}

	public static Message setStatus(String adminCard, String bankCardId, int status) {

		Map<String, Object> p = new HashMap<String, Object>();
		p.put("adminCard", adminCard);
		p.put("bankCardId", bankCardId);
		p.put("status", status);

		return request(status == 1 ? MsgConst.BANK_ADMIN_UNFREEZE : MsgConst.BANK_ADMIN_FREEZE, p);
	}

	private static Message amountRequest(String type, String holderCard, double amount) {

		Map<String, Object> p = new HashMap<String, Object>();
		p.put("holderCard", holderCard);
		p.put("amount", amount);

		return request(type, p);
	}

	@SuppressWarnings("unchecked")
	private static <T> List<T> castList(Object value) {
		return (List<T>) value;
	}
}