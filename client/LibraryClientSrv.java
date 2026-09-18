package vCampus.client;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import vCampus.common.Message;
import vCampus.common.consts.MsgConst;
import vCampus.common.vo.Book;
import vCampus.common.vo.BookCategory;
import vCampus.common.vo.BookCopy;
import vCampus.common.vo.BookRecommendation;
import vCampus.common.vo.BorrowRecord;
import vCampus.common.vo.LibraryReader;
import vCampus.common.vo.ReservationRecord;

/**
 * 图书馆模块的客户端网络代理。
 *
 * <p>图书馆 UI 只调用本类，不直接处理 Socket 和 Message.type。</p>
 */
public final class LibraryClientSrv {

    private LibraryClientSrv() {
    }

    /** 发送一条图书馆请求，并返回服务端完整响应。 */
    public static Message request(String type, Object data) {
        return SocketClient.send(new Message(type, data));
    }

    public static List<Book> getAllBooks() {
        return getList(MsgConst.QUERY_ALL_BOOKS, null);
    }

    public static List<Book> searchBooks(String keyword) {
        return getList(MsgConst.SEARCH_BOOKS, keyword);
    }

    public static Message addBook(Book book) {
        return request(MsgConst.ADD_BOOK, book);
    }

    public static Message updateBook(Book book) {
        return request(MsgConst.UPDATE_BOOK, book);
    }

    public static Message deleteBook(String bookId) {
        return request(MsgConst.DELETE_BOOK, bookId);
    }

    public static List<BookCategory> getAllBookCategories() {
        return getList(MsgConst.QUERY_BOOK_CATEGORIES, null);
    }

    public static Message addBookCategory(BookCategory category) {
        return request(MsgConst.ADD_BOOK_CATEGORY, category);
    }

    public static Message updateBookCategory(BookCategory category) {
        return request(MsgConst.UPDATE_BOOK_CATEGORY, category);
    }

    public static Message deleteBookCategory(String categoryId) {
        return request(MsgConst.DELETE_BOOK_CATEGORY, categoryId);
    }

    public static List<BookCopy> getBookCopies(String bookId) {
        return getList(MsgConst.QUERY_BOOK_COPIES, bookId);
    }

    public static Message addBookCopy(BookCopy copy) {
        return request(MsgConst.ADD_BOOK_COPY, copy);
    }

    public static Message updateBookCopy(BookCopy copy) {
        return request(MsgConst.UPDATE_BOOK_COPY, copy);
    }

    public static Message deleteBookCopy(String copyId) {
        return request(MsgConst.DELETE_BOOK_COPY, copyId);
    }

    /** 借阅请求使用 BorrowRecord 传输副本、读者和日期信息。 */
    public static Message borrowBook(String copyId, String readerId, LocalDate borrowDate, LocalDate dueDate) {
        BorrowRecord record = new BorrowRecord();
        record.setCopyId(copyId);
        record.setReaderId(readerId);
        record.setBorrowDate(borrowDate);
        record.setDueDate(dueDate);
        return request(MsgConst.BORROW_BOOK, record);
    }

    public static Message returnBook(String recordId, LocalDate returnDate) {
        BorrowRecord record = new BorrowRecord();
        record.setRecordId(recordId);
        record.setReturnDate(returnDate);
        return request(MsgConst.RETURN_BOOK, record);
    }

    public static Message returnDamagedBook(String recordId, LocalDate returnDate) {
        BorrowRecord record = new BorrowRecord();
        record.setRecordId(recordId);
        record.setReturnDate(returnDate);
        return request(MsgConst.RETURN_DAMAGED_BOOK, record);
    }

    public static Message renewBook(String recordId, LocalDate newDueDate) {
        BorrowRecord record = new BorrowRecord();
        record.setRecordId(recordId);
        record.setDueDate(newDueDate);
        return request(MsgConst.RENEW_BOOK, record);
    }

    public static Message reserveBook(String bookId, String readerId, LocalDateTime reservationTime) {
        ReservationRecord record = new ReservationRecord();
        record.setBookId(bookId);
        record.setReaderId(readerId);
        record.setReservationTime(reservationTime);
        return request(MsgConst.RESERVE_BOOK, record);
    }

    public static Message cancelReservation(String reservationId) {
        return request(MsgConst.CANCEL_RESERVATION, reservationId);
    }

    public static List<ReservationRecord> getMyReservations(String readerId) {
        return getList(MsgConst.QUERY_MY_RESERVATIONS, readerId);
    }

    public static List<ReservationRecord> getReservationQueue(String bookId) {
        return getList(MsgConst.QUERY_RESERVATION_QUEUE, bookId);
    }

    public static List<BorrowRecord> getMyBorrowRecords(String readerId) {
        return getList(MsgConst.QUERY_MY_BORROW_RECORDS, readerId);
    }

    public static Message queryAdminBorrowRecords(vCampus.common.vo.LibraryAdminRequest query) {
        return request(MsgConst.LIBRARY_ADMIN_QUERY_BORROWS, query);
    }

    public static Message adminBorrow(vCampus.common.vo.LibraryAdminRequest operation) {
        return request(MsgConst.LIBRARY_ADMIN_BORROW, operation);
    }

    public static Message adminReturn(vCampus.common.vo.LibraryAdminRequest operation) {
        return request(MsgConst.LIBRARY_ADMIN_RETURN, operation);
    }

    public static LibraryReader getLibraryReader(String readerId) {
        Message response = request(MsgConst.QUERY_LIBRARY_READER, readerId);
        return response.isSuccess() && response.getData() instanceof LibraryReader
                ? (LibraryReader) response.getData() : null;
    }

    public static Message checkBorrowPermission(String readerId) {
        LibraryReader reader = new LibraryReader();
        reader.setReaderId(readerId);
        return request(MsgConst.CHECK_BORROW_PERMISSION, reader);
    }

    public static Message submitBookRecommendation(BookRecommendation recommendation) {
        return request(MsgConst.SUBMIT_BOOK_RECOMMENDATION, recommendation);
    }

    public static List<BookRecommendation> getMyBookRecommendations(String readerCardId) {
        return getList(MsgConst.QUERY_MY_BOOK_RECOMMENDATIONS, readerCardId);
    }

    public static List<BookRecommendation> getProcurementReferences() {
        return getList(MsgConst.QUERY_BOOK_PROCUREMENT_REFERENCES, null);
    }

    @SuppressWarnings("unchecked")
    private static <T> List<T> getList(String type, Object data) {
        Message response = request(type, data);
        if (response.isSuccess() && response.getData() instanceof List<?>) {
            return (List<T>) response.getData();
        }
        return Collections.emptyList();
    }
}
