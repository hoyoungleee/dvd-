package video.order.service;

import video.common.AppService;
import video.movie.domain.Movie;
import video.movie.repository.MovieRepository;
import video.order.domain.Order;
import video.order.domain.OverduePolicy;
import video.order.repository.OrderRepository;
import video.ui.AppUi;
import video.user.domain.User;
import video.user.repository.UserRepository;

import java.sql.Date;
import java.time.LocalDate;
import java.util.*;

import static video.ui.AppUi.*;

public class OrderService implements AppService {

    private final MovieRepository movieRepository = new MovieRepository();
    private final UserRepository userRepository = new UserRepository();
    private final OrderRepository orderRepository = new OrderRepository();

    @Override
    public void start() {
        while (true) {
            orderManagementScreen();
            int selection = inputInteger(">>> ");

            switch (selection) {
                case 1:
                    processOrderDvd();
                    break;
                case 2:
                    processReturnDvd();
                    break;
                case 3:
                    return;
                default:
                    System.out.println("\n### 메뉴를 다시 입력하세요.");
            }


        }
    }


    // DVD 대여 서비스 비즈니스 로직
    private void processOrderDvd() {
        while (true) {
            System.out.println("\n============================ 대여관리 시스템을 실행합니다. ============================");
            System.out.println("[ 1. 대여 가능 DVD 목록 보기 | 2. 대여중인 영화 반납예정일 확인하기 | 3. 이전으로 돌아가기 ]");
            int selection = inputInteger(">>> ");

            switch (selection) {
                case 1:
                    showRentalPossibleList();
                    break;
                case 2:
                    showRentalImpossibleList();
                    break;
                case 3:
                    return;
                default:
                    System.out.println("\n### 메뉴를 다시 입력하세요.");
            }

        }
    }



    // 대여 가능한 DVD 목록 보기
    private void showRentalPossibleList() {
        List<Movie> movieList = movieRepository.searchByRental(true);
        int count = movieList.size();

        List<Integer> movieNums = new ArrayList<>();

        if (count > 0) {
            System.out.printf("\n==================================== 검색 결과 (총 %d건) ====================================\n", count);
            for (Movie movie : movieList) {
                System.out.println(movie);
                movieNums.add(movie.getSerialNumber());
            }
            System.out.println("==========================================================================================");
            System.out.println("### 대여할 DVD의 번호를 입력하세요.");
            int movieNumber = inputInteger(">>> ");

            if (movieNums.contains(movieNumber)) {
                for (Movie movie : movieList) {
                    if (movie.getSerialNumber() == movieNumber) {
                        rentalProcess(movie);
                        break;
                    }
                }
            } else {
                System.out.println("\n### 대여가 가능한 영화 목록중에 선택해야 합니다.");
            }
        } else {
            System.out.println("\n### 대여 가능한 DVD가 없습니다 ㅠㅠ");
        }
    }

    private void rentalProcess(Movie rentalMovie) {

        System.out.printf("\n### [%s] DVD를 대여합니다.\n", rentalMovie.getMovieName());
        System.out.println("### 대여자의 이름을 입력하세요.");
        String name = inputString(">>> ");

        List<User> users = userRepository.findUserByName(name);
        Map<Integer, User> userMap = new HashMap<>();

        if(users.size() > 0) {
            System.out.println("\n===================================== 회원 조회 결과 =====================================");
            for (User user : users) {
                System.out.println(user);
                userMap.put(user.getUserNumber(), user);
            }
            System.out.println("========================================================================================");
            System.out.println("## 대여할 회원의 회원번호를 입력하세요.");
            int userNumber = inputInteger(">>> ");

            if (userMap.containsKey(userNumber)) {
                // 대여 완료 처리
                User rentalUser = userMap.get(userNumber); // 렌탈 유저 정보 획득.

                // 새로운 주문 생성
                Order newOrder = new Order(rentalUser, rentalMovie);
                rentalUser = orderRepository.processRental(newOrder);

                String phoneNumber = rentalUser.getPhoneNumber(); // 출력문을 위해 얻은 전화번호

                // lastIndexOf(str): 해당 문자열의 위치를 뒤에서부터 탐색.
                // 뒤에서부터 탐색을 시작해서 "-"을 찾아라 -> 그 "-" 이후로부터 끝까지 추출해라.
                System.out.printf("\n### [%s(%s) 회원님] 대여 처리가 완료되었습니다. 감사합니다!\n"
                        , rentalUser.getUserName(), rentalUser.getPhoneNumber().substring(phoneNumber.lastIndexOf("-") + 1));

                System.out.printf("### 현재 등급: [%s], 총 누적 결제금액: %d원\n", rentalUser.getGrade(), rentalUser.getTotalPaying());

            } else {
                System.out.println("\n### 검색된 회원의 번호를 입력하세요.");
            }

        } else {
            System.out.println("\n### 대여자 정보가 없습니다.");
        }

    }

    // 대여중(대여 불가능한)인 DVD 목록 보기
    private void showRentalImpossibleList() {
        List<Map<String, Object>> movieList
                = orderRepository.showImpossibleRentalList();
        int count = movieList.size();

        if (count > 0) {
            System.out.printf("\n==================================== 검색 결과 (총 %d건) ====================================\n", count);
            for (Map<String, Object> map : movieList) {
                String phoneNumber = (String) map.get("phoneNumber");
                String lastPhoneNumber = phoneNumber.substring(phoneNumber.lastIndexOf("-") + 1);
                System.out.printf("### 영화명: %s, 현재 대여자: %s(%s), 반납예정일: %s\n"
                        , map.get("movieName"), map.get("userName"), lastPhoneNumber,
                        map.get("returnDate"));
            }
            System.out.println("==========================================================================================");

        } else {
            System.out.println("\n### 대여 중인 DVD가 없습니다.");
        }

    }

    // DVD 반납 서비스 비즈니스 로직
    private void processReturnDvd() {
        System.out.println("\n============================ 반납 관리 시스템을 실행합니다. ============================");
        System.out.println("### 반납자의 이름을 입력하세요.");
        String name = inputString(">>> ");

        List<User> users = userRepository.findUserByName(name);
        int count = users.size();
        Map<Integer, User> userMap = new HashMap<>();

        if (count > 0) {
            System.out.printf("\n===================================== 조회 결과(총 %d건) =====================================\n", count);
            for (User user : users) {
                System.out.println(user);
                userMap.put(user.getUserNumber(), user);
            }
            System.out.println("========================================================================================");

            System.out.println("### 반납자의 회원 번호를 입력하세요.");
            int userNumber = inputInteger(">>> ");

            if (userMap.containsKey(userNumber)) {
                returnProcess(userMap.get(userNumber));
            } else {
                System.out.println("\n### 조회된 회원 번호를 입력하셔야 합니다.");
            }
        } else {
            System.out.println("\n### 반납자의 정보가 없습니다.");
        }
    }

    private void returnProcess(User user) {
        // "XXX 회원님의 대여 목록입니다" 라고 하면서 해당 회원의 대여 목록을 DB에서 가져와야 한다.
        // 반납할 DVD의 번호를 입력받아야 한다.
        // 입력한 번호가 대여중인 DVD인지 검증해야 한다. (아무 번호나 입력하지 않았는지 확인)
        // 대여중인 DVD가 맞다면 반납처리를 본격적으로 진행한다.
        // 영화의 대여 가능 여부를 변경해야 한다. -> DB에서 변경
        // 연체료 발생 여부를 확인하여 연체료가 존재한다면 추가로 얼마를 결제하라고 출력문을 띄워야 하고,
        // 연체료가 없다면 반납이 완료되었다는 출력문을 보여주어야 한다.
        List<Map<String, Object>> rentalList
                = orderRepository.showRentalListByUserNumber(user.getUserNumber());

        if (!rentalList.isEmpty()) {
            System.out.printf("\n### 현재 [%s] 회원님의 대여 목록입니다.\n", user.getUserName());
            System.out.println("======================================================================================");
            for (Map<String, Object> map : rentalList) {
                System.out.printf("### %s. 영화명: %s, 대여일자: %s, 반납일자: %s\n"
                        , map.get("serialNumber"), map.get("movieName"), map.get("orderDate"), map.get("returnDate"));
            }
            System.out.println("======================================================================================");
            System.out.println("### 반납할 DVD의 번호를 입력하세요.");
            int returnMovieNumber = inputInteger(">>> ");
            System.out.println("returnMovieNumber = " + returnMovieNumber);

            boolean flag = false;
            for (Map<String, Object> map : rentalList) {
                System.out.println("map = " + map);
                int serialNumber = (int) map.get("serialNumber");
                if (serialNumber == returnMovieNumber) {
                    System.out.println("반납 처리 진행 중 ...");
                    // 반납 처리
                    int orderId = (int) map.get("orderId");

                    orderRepository.updateReturnProcess(orderId, returnMovieNumber, "Y");
                    // 데이터베이스에서 날짜 타입의 값을 가져올 때 Date 타입으로 가져옵니다. -> LocalDate로 따로 변환이 필요.
                    LocalDate returnDate = ((Date) map.get("returnDate")).toLocalDate();
                    int overdueCharge = OverduePolicy.calculateOverdueCharge(returnDate);

                    if (overdueCharge > 0) {
                        System.out.printf("\n### 반납이 완료되었습니다. %d원을 추가로 결제해 주세요!\n", overdueCharge);
                    } else {
                        System.out.println("\n### 반납이 완료되었습니다. 즐거운 하루 되세요!");
                    }
                    flag = true;
                    break;
                }
            }
            if (!flag) System.out.println("\n### 해당 DVD는 반납 대상이 아닙니다.");
        } else {
            System.out.println("\n### 대여 이력이 없습니다.");
        }


    }


}







