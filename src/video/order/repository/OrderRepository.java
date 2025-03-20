package video.order.repository;

import video.jdbc.DBConnectionManager;
import video.movie.domain.ChargePolicy;
import video.order.domain.Order;
import video.user.domain.Grade;
import video.user.domain.User;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

//Orders 테이블의 CRUD 연산을 담당하는 클래스
//CRUD : Create Read Update Deleteee
public class OrderRepository {

    //대여 프로세스를 하나로 묶어서 처리
    public User processRental(Order order){
        Connection conn =null;
        try{
            conn = DBConnectionManager.getConnection();
            conn.setAutoCommit(false); //트랜잭션 시작.

            // 1. 영화 대여 처리 (orders 테이블에 insert)
            addOrder(conn,order);
            // 2. 영화 상태 업데이트 (rental = 'N')
            updateMovieRentalStatus(conn, order.getMovie().getSerialNumber(), "N");
            // 3. 사용자의 총 결제금액 계산
            int totalCharge = calculateTotalCharge(conn, order);

            // 4. 필요하다면 사용자 등급 업데이트 (기준치에 충족되는 회원만)
            Grade before = order.getUser().getGrade();
            order.getUser().setTotalPaying(totalCharge);
            if(order.getUser().getGrade() != before){
                updateUserGrade(conn, order.getUser());
            }

            conn.commit(); // 문제가 없다면 트랜잭션 커밋
        } catch (Exception e) {
            e.printStackTrace();
            try {
                conn.rollback(); // 오류 발생시 롤백
            } catch (SQLException ex) {
                ex.printStackTrace();
            }

        }finally {
            if(conn != null) {
                try {
                    // autoCommit 원래대로 활성화.
                    conn.setAutoCommit(true);
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }
            }
        }

        return  order.getUser();

    }

    public List<Map<String, Object>> showImpossibleRentalList(){
        //영화명(movies), 현재대여자(users), 반납예정일 (orders)출력일
        String sql = "SELECT " +
                "    m.MOVIE_NAME, " +
                "    u.USER_NAME, " +
                "    u.PHONE_NUMBER, " +
                "    o.return_date " +
                "from orders o " +
                "JOIN users u " +
                "    ON o.user_number = u.user_number " +
                "JOIN movies m " +
                "    ON o.serial_number = m.serial_number " +
                "WHERE m.RENTAL = 'N' " +
                "ORDER BY o.return_date ASC ";
        List<Map<String, Object>> rentalList = new ArrayList<>();
        try(Connection conn = DBConnectionManager.getConnection();
        PreparedStatement psrt = conn.prepareStatement(sql);
        ResultSet rs = psrt.executeQuery()) {
            while (rs.next()){
                Map<String, Object> row = new HashMap<>();
                row.put("movieName", rs.getString("movie_name"));
                row.put("userName", rs.getString("user_name"));
                row.put("phoneNumber", rs.getString("phone_number"));
                row.put("returnDate", rs.getDate("return_date"));
                rentalList.add(row);
            }

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return rentalList;
    }

    private void updateUserGrade(Connection conn, User user) throws Exception {
        String sql = "UPDATE users SET grade = ? WHERE user_number = ?";
        try(PreparedStatement prst = conn.prepareStatement(sql)) {
            prst.setString(1, user.getGrade().toString());
            prst.setInt(2, user.getUserNumber());
            prst.executeUpdate();
        }
    }

    private int calculateTotalCharge(Connection conn, Order order) throws Exception {
        String sql = "SELECT " +
                "u.user_number, u.user_name, " +
                "o.order_id, m.pub_year, o.order_date " +
                "FROM users u " +
                "JOIN orders o " +
                "ON u.user_number = o.user_number " +
                "JOIN movies m ON o.serial_number = m.serial_number " +
                "WHERE u.user_number = " + order.getUser().getUserNumber();

        int totalCharges = 0;
        try(PreparedStatement pstmt = conn.prepareStatement(sql);
            ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()){
                int pubYear = rs.getInt("pub_year");
                LocalDate orderDate = rs.getDate("order_date").toLocalDate();
                int charge = ChargePolicy.calculateDvdCharge(pubYear,orderDate);
                totalCharges += charge;
            }
        }
        return totalCharges;

    }

    private void updateMovieRentalStatus(
            Connection conn, int serialNumber, String possible) throws Exception {
        String sql = "UPDATE MOVIES SET RENTAL = ? WHERE SERIAL_NUMBER = ?";
        try(PreparedStatement pstmt = conn.prepareStatement(sql)){
            pstmt.setString(1,possible);
            pstmt.setInt(2, serialNumber);
            pstmt.executeUpdate();
        }

    }

    // 새 대여 주문 추가
    private void addOrder(Connection conn,Order order) throws Exception{
        String sql = "INSERT INTO ORDERS VALUES(order_seq.NEXTVAL, ?, ?, ?, ?) ";
        try(PreparedStatement prtmt = conn.prepareStatement(sql)) {
            prtmt.setInt(1,order.getUser().getUserNumber());
            prtmt.setInt(2,order.getMovie().getSerialNumber());
            prtmt.setDate(3, Date.valueOf(order.getOrderDate()));
            prtmt.setDate(4, Date.valueOf(order.getReturnDate()));

            prtmt.executeUpdate();

        }
    }







}
