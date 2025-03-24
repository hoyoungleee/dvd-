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



    public void addMovie(Movie movie) {
        //컬럼 지정 안하고 바로 values로 넣을떄는 Default 일지라도 컬럼 개수 순서 맞춰서 넣어줘야
        //insert에 거부당하지 않는다.
        String sql = "INSERT INTO MOVIES VALUES(MOVIE_SEQ.NEXTVAL,?,?,?,?,?,?)";
        try(Connection connection = DBConnectionManager.getConnection();
            PreparedStatement prsmt = connection.prepareStatement(sql);) {

            prsmt.setString(1,movie.getMovieName());
            prsmt.setString(2,movie.getNation());
            prsmt.setInt(3,movie.getPubYear());
            prsmt.setString(4, "Y");
            prsmt.setInt(5,movie.getStock());
            prsmt.setString(6, "Y");

            prsmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public List<Movie> searchMovieList(Condition condition, String keyword) throws Exception {
        List<Movie> searchList = new ArrayList<>();
        String sql = "SELECT * FROM movies WHERE 1=1 ";
        if (condition == PUB_YEAR) {
            sql += "AND PUB_YEAR LIKE ? ";
        } else if (condition == NATION) {
            sql += "AND NATION LIKE ? ";
        } else if (condition == TITLE) {
            sql += "AND MOVIE_NAME LIKE ? ";
        }
        sql += "AND USE_YN = 'Y'";

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
        String sql = "UPDATE movies SET USE_YN = 'N' ,RENTAL = 'N' WHERE SERIAL_NUMBER= ?";
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

        String sql = "SELECT * FROM MOVIES WHERE rental = ? AND USE_YN = 'Y' AND STOCK > 0 ";

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
        Movie movie = new Movie(rs.getString("MOVIE_NAME"), rs.getString("NATION"), rs.getInt("PUB_YEAR"),rs.getInt("STOCK"));
        movie.setRental(rs.getString("RENTAL").equals("Y"));
        movie.setSerialNumber(rs.getInt("SERIAL_NUMBER"));
        return movie;
    }

    // 번호에 맞는 영화 객체를 단 하나만 리턴하는 메서드.
    public Movie searchMovie(int movieNumber) {
        return movieDatabase.get(movieNumber);
    }




}



















