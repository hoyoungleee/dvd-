package video.movie.repository;

import video.common.Condition;
import video.jdbc.DBConnectionManager;
import video.movie.domain.Movie;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static video.common.Condition.*;

public class MovieRepository {

    private static final Map<Integer, Movie> movieDatabase = new HashMap<>();

    static {
        insertTestMovieData();
    }

    //테스트 데이터 생성 및 삽입
    private static void insertTestMovieData() {
        Movie movie1 = new Movie("인터스텔라", "미국", 2014);
        Movie movie2 = new Movie("포레스트 검프", "미국", 1994);
        Movie movie3 = new Movie("너의 이름은", "일본", 2017);
        Movie movie4 = new Movie("라라랜드", "미국", 2016);
        Movie movie5 = new Movie("레옹", "프랑스", 1994);
        Movie movie6 = new Movie("어바웃 타임", "영국", 2013);
        Movie movie7 = new Movie("타이타닉", "미국", 1998);
        Movie movie8 = new Movie("인생은 아름다워", "이탈리아", 1999);
        Movie movie9 = new Movie("쇼생크 탈출", "미국", 1995);
        Movie movie10 = new Movie("기생충", "대한민국", 2019);

        movieDatabase.put(movie1.getSerialNumber(), movie1);
        movieDatabase.put(movie2.getSerialNumber(), movie2);
        movieDatabase.put(movie3.getSerialNumber(), movie3);
        movieDatabase.put(movie4.getSerialNumber(), movie4);
        movieDatabase.put(movie5.getSerialNumber(), movie5);
        movieDatabase.put(movie6.getSerialNumber(), movie6);
        movieDatabase.put(movie7.getSerialNumber(), movie7);
        movieDatabase.put(movie8.getSerialNumber(), movie8);
        movieDatabase.put(movie9.getSerialNumber(), movie9);
        movieDatabase.put(movie10.getSerialNumber(), movie10);
    }

    public void addMovie(Movie movie) {
        //컬럼 지정 안하고 바로 values로 넣을떄는 Default 일지라도 컬럼 개수 순서 맞춰서 넣어줘야
        //insert에 거부당하지 않는다.
        String sql = "INSERT INTO MOVIES VALUES(MOVIE_SEQ.NEXTVAL,?,?,?,?)";
        try(Connection connection = DBConnectionManager.getConnection();
            PreparedStatement prsmt = connection.prepareStatement(sql);) {

            prsmt.setString(1,movie.getMovieName());
            prsmt.setString(2,movie.getNation());
            prsmt.setInt(3,movie.getPubYear());
            prsmt.setString(4, "Y");

            prsmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public List<Movie> searchMovieList(Condition condition, String keyword) throws Exception {
        List<Movie> searchList = new ArrayList<>();
        String sql = "SELECT * FROM movies ";
        if (condition == PUB_YEAR) {
            sql += "where PUB_YEAR LIKE ?";
        } else if (condition == NATION) {
            sql += "where NATION LIKE ?";
        } else if (condition == TITLE) {
            sql += "where MOVIE_NAME LIKE ?";
        }
        try(Connection conn = DBConnectionManager.getConnection();
        PreparedStatement prsmt = conn.prepareStatement(sql)){
            if(condition != ALL){
                // LIKE 사용시 %, _기호를 따옴표  안에 넣어줘야 합니다.
                // ? 옆에 %쓰는게 아니라 ?를 채울떄 특정 단어에 %을 미리 세팅해서 채워야합니다.
                //처음 선언문 옆에 붙이면 %'keyword'% 이렇게 따옴표 감싸진 걸로 되어서 검색값에 ''가 포함됨
                prsmt.setString(1,"%"+keyword+"%");
            }
            ResultSet rs = prsmt.executeQuery();
            //다음값이 있다면 true, 없다면 false 반환
            while (rs.next()){
                //rs.get데이터타입(컬럼명)
                Movie movie = createMovieFromResultSet(rs);
                searchList.add(movie);
            }

        }catch (SQLException e){
            e.printStackTrace();
        }
        return searchList;
    }


    public void deleteMovie(int delMovieNum) {
        String sql = "DELETE FROM movies WHERE serial_number = ? ";
        try(Connection conn = DBConnectionManager.getConnection();
        PreparedStatement prsmt = conn.prepareStatement(sql)) {
            prsmt.setInt(1,delMovieNum);
            prsmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public List<Movie> searchByRental(boolean possible) {
        List<Movie> searchedList = new ArrayList<>();

        String sql = "SELECT * FROM MOVIES WHERE rental = ?";

        try(Connection connection = DBConnectionManager.getConnection();
         PreparedStatement prsmt = connection.prepareStatement(sql)){
            prsmt.setString(1, possible? "Y":"N");
            ResultSet rs = prsmt.executeQuery();
            while (rs.next()){
                Movie movie = createMovieFromResultSet(rs);
                searchedList.add(movie);
            }
        }catch (SQLException e){
            e.printStackTrace();
        }

        return searchedList;
    }

    //ResultSet에서 추출한 결과를 Movie 객체로 포장해주는 헬퍼 메서드
    private static Movie createMovieFromResultSet(ResultSet rs) throws SQLException {
        Movie movie = new Movie(rs.getString("MOVIE_NAME"), rs.getString("NATION"), rs.getInt("PUB_YEAR"));
        movie.setRental(rs.getString("RENTAL").equals("Y"));
        movie.setSerialNumber(rs.getInt("SERIAL_NUMBER"));
        return movie;
    }

    // 번호에 맞는 영화 객체를 단 하나만 리턴하는 메서드.
    public Movie searchMovie(int movieNumber) {
        return movieDatabase.get(movieNumber);
    }




}



















