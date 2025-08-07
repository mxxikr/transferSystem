package com.transfer.system.policy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class PagingPolicyTest {

    private PagingPolicy pagingPolicy;

    private final int TEST_DEFAULT_PAGE = 0;
    private final int TEST_DEFAULT_SIZE = 20;
    private final int TEST_MAX_SIZE = 100;
    private final String TEST_SORT_FIELD = "createdTimeStamp";

    @BeforeEach
    void setUp() {
        pagingPolicy = new PagingPolicy(
                TEST_DEFAULT_PAGE,
                TEST_DEFAULT_SIZE,
                TEST_MAX_SIZE,
                TEST_SORT_FIELD
        );
    }

    // ========================= 페이지 검증 테스트 =========================
    @Nested
    class GetValidatedPageTest {

        /**
         * 페이지 검증 성공 - 정상적인 페이지 번호
         */
        @Test
        void getValidatedPage_success() {
            Integer validPage = 5;

            Integer result = pagingPolicy.getValidatedPage(validPage);

            assertEquals(5, result);
        }

        /**
         * 페이지 검증 성공 - 0 페이지
         */
        @Test
        void getValidatedPage_success_whenPageIsZero() {
            Integer validPage = 0;

            Integer result = pagingPolicy.getValidatedPage(validPage);

            assertEquals(0, result);
        }

        /**
         * 페이지 검증 - null 페이지일 때 기본 값 반환
         */
        @Test
        void getValidatedPage_returnDefault_whenPageIsNull() {
            Integer result = pagingPolicy.getValidatedPage(null);

            assertEquals(TEST_DEFAULT_PAGE, result);
        }

        /**
         * 페이지 검증 - 음수 페이지일 때 기본 값 반환
         */
        @Test
        void getValidatedPage_returnDefault_whenPageIsNegative() {
            Integer negativePage = -5;

            Integer result = pagingPolicy.getValidatedPage(negativePage);

            assertEquals(TEST_DEFAULT_PAGE, result);
        }

        /**
         * 페이지 검증 - 다양한 유효한 페이지 번호들
         */
        @ParameterizedTest
        @ValueSource(ints = {0, 1, 5, 10, 50, 100, 999})
        void getValidatedPage_success_validPageNumbers(Integer page) {
            Integer result = pagingPolicy.getValidatedPage(page);

            assertEquals(page, result);
        }

        /**
         * 페이지 검증 - 다양한 무효한 페이지 번호들
         */
        @ParameterizedTest
        @ValueSource(ints = {-1, -5, -10, -999})
        void getValidatedPage_returnDefault_invalidPageNumbers(Integer page) {
            Integer result = pagingPolicy.getValidatedPage(page);

            assertEquals(TEST_DEFAULT_PAGE, result);
        }

        /**
         * 페이지 검증 - 극한 값 처리
         */
        @Test
        void getValidatedPage_handleExtremeValues() {
            // 최대값 테스트
            Integer maxResult = pagingPolicy.getValidatedPage(Integer.MAX_VALUE);
            assertEquals(Integer.MAX_VALUE, maxResult);

            // 최소값 테스트
            Integer minResult = pagingPolicy.getValidatedPage(Integer.MIN_VALUE);
            assertEquals(TEST_DEFAULT_PAGE, minResult);
        }
    }

    // ========================= 페이지 크기 검증 테스트 =========================
    @Nested
    class GetValidatedSizeTest {

        /**
         * 페이지 크기 검증 성공 - 정상적인 크기
         */
        @Test
        void getValidatedSize_success() {
            int validSize = 30;

            int result = pagingPolicy.getValidatedSize(validSize);

            assertEquals(30, result);
        }

        /**
         * 페이지 크기 검증 성공 - 최대 크기
         */
        @Test
        void getValidatedSize_success_whenSizeIsMax() {
            int maxSize = TEST_MAX_SIZE;

            int result = pagingPolicy.getValidatedSize(maxSize);

            assertEquals(TEST_MAX_SIZE, result);
        }

        /**
         * 페이지 크기 검증 - 0 크기일 때 기본 값 반환
         */
        @Test
        void getValidatedSize_returnDefault_whenSizeIsZero() {
            int result = pagingPolicy.getValidatedSize(0);

            assertEquals(TEST_DEFAULT_SIZE, result);
        }

        /**
         * 페이지 크기 검증 - 음수 크기일 때 기본 값 반환
         */
        @Test
        void getValidatedSize_returnDefault_whenSizeIsNegative() {
            int negativeSize = -10;

            int result = pagingPolicy.getValidatedSize(negativeSize);

            assertEquals(TEST_DEFAULT_SIZE, result);
        }

        /**
         * 페이지 크기 검증 - 최대 크기 초과 시 최대값으로 조정
         */
        @Test
        void getValidatedSize_adjustToMax_whenSizeExceedsMaximum() {
            int oversizeRequest = TEST_MAX_SIZE + 50;

            int result = pagingPolicy.getValidatedSize(oversizeRequest);

            assertEquals(TEST_MAX_SIZE, result);
        }

        /**
         * 페이지 크기 검증 - 다양한 유효한 크기들
         */
        @ParameterizedTest
        @MethodSource("validSizeProvider")
        void getValidatedSize_success_validSizes(int size, String description) {
            int result = pagingPolicy.getValidatedSize(size);

            assertTrue(result > 0);
            assertTrue(result <= TEST_MAX_SIZE);

            // 유효한 범위 내라면 원래 값 그대로 반환
            if (size > 0 && size <= TEST_MAX_SIZE) {
                assertEquals(size, result);
            }
        }

        private static Stream<Arguments> validSizeProvider() {
            return Stream.of(
                Arguments.of(1, "최소 크기"),
                Arguments.of(10, "작은 크기"),
                Arguments.of(20, "기본 크기"),
                Arguments.of(50, "중간 크기"),
                Arguments.of(100, "최대 크기")
            );
        }

        /**
         * 페이지 크기 검증 - 다양한 무효한 크기들
         */
        @ParameterizedTest
        @ValueSource(ints = {-100, -10, -1, 0})
        void getValidatedSize_returnDefault_invalidSizes(int size) {
            int result = pagingPolicy.getValidatedSize(size);

            assertEquals(TEST_DEFAULT_SIZE, result);
        }

        /**
         * 페이지 크기 검증 - 극한값 처리
         */
        @Test
        void getValidatedSize_handleExtremeValues() {
            // 최대값 초과 테스트
            int maxResult = pagingPolicy.getValidatedSize(Integer.MAX_VALUE);
            assertEquals(TEST_MAX_SIZE, maxResult);

            // 최소값 테스트
            int minResult = pagingPolicy.getValidatedSize(Integer.MIN_VALUE);
            assertEquals(TEST_DEFAULT_SIZE, minResult);
        }
    }

    // ========================= 정렬 필드 테스트 =========================
    @Nested
    class GetTransactionSortFieldTest {

        /**
         * 거래 정렬 필드 조회 성공
         */
        @Test
        void getTransactionSortField_success() {
            String result = pagingPolicy.getTransactionSortField();

            assertNotNull(result);
            assertEquals(TEST_SORT_FIELD, result);
        }

        /**
         * 거래 정렬 필드 - null이 아님을 보장
         */
        @Test
        void getTransactionSortField_notNull() {
            String result = pagingPolicy.getTransactionSortField();

            assertNotNull(result);
            assertFalse(result.trim().isEmpty());
        }

        /**
         * 거래 정렬 필드 - 예상된 값 검증
         */
        @Test
        void getTransactionSortField_expectedValue() {
            String result = pagingPolicy.getTransactionSortField();

            assertEquals("createdTimeStamp", result);
        }
    }

    // ========================= 정책 설정값 테스트 =========================
    @Nested
    class PolicyConfigurationTest {

        /**
         * 기본 페이지 설정 확인
         */
        @Test
        void getDefaultPage_configuration() {
            int defaultPage = pagingPolicy.getDefaultPage();

            assertEquals(TEST_DEFAULT_PAGE, defaultPage);
            assertTrue(defaultPage >= 0);
        }

        /**
         * 기본 페이지 크기 설정 확인
         */
        @Test
        void getDefaultSize_configuration() {
            int defaultSize = pagingPolicy.getDefaultSize();

            assertEquals(TEST_DEFAULT_SIZE, defaultSize);
            assertTrue(defaultSize > 0);
        }

        /**
         * 최대 페이지 크기 설정 확인
         */
        @Test
        void getMaxSize_configuration() {
            int maxSize = pagingPolicy.getMaxSize();

            assertEquals(TEST_MAX_SIZE, maxSize);
            assertTrue(maxSize > 0);
            assertTrue(maxSize >= pagingPolicy.getDefaultSize());
        }

        /**
         * 정렬 필드 설정 확인
         */
        @Test
        void getTransactionSortField_configuration() {
            String sortField = pagingPolicy.getTransactionSortField();

            assertEquals(TEST_SORT_FIELD, sortField);
            assertNotNull(sortField);
            assertFalse(sortField.trim().isEmpty());
        }

        /**
         * 설정 값들의 일관성 확인
         */
        @Test
        void configuration_consistency() {
            int defaultPage = pagingPolicy.getDefaultPage();
            int defaultSize = pagingPolicy.getDefaultSize();
            int maxSize = pagingPolicy.getMaxSize();
            String sortField = pagingPolicy.getTransactionSortField();

            // 기본 값들이 유효한 범위에 있는지 확인
            assertTrue(defaultPage >= 0, "기본 페이지는 0 이상이어야 함");
            assertTrue(defaultSize > 0, "기본 크기는 양수여야 함");
            assertTrue(maxSize > 0, "최대 크기는 양수여야 함");
            assertTrue(maxSize >= defaultSize, "최대 크기는 기본 크기보다 크거나 같아야 함");
            assertNotNull(sortField, "정렬 필드는 null이 아니어야 함");
        }
    }

    // ========================= 경계값 및 예외 테스트 =========================
    @Nested
    class BoundaryAndExceptionTest {

        /**
         * 경계값 테스트 - 페이지 0
         */
        @Test
        void boundary_pageZero() {
            Integer result = pagingPolicy.getValidatedPage(0);

            assertEquals(0, result);
        }

        /**
         * 경계값 테스트 - 페이지 -1
         */
        @Test
        void boundary_pageMinusOne() {
            Integer result = pagingPolicy.getValidatedPage(-1);

            assertEquals(TEST_DEFAULT_PAGE, result);
        }

        /**
         * 경계값 테스트 - 크기 1
         */
        @Test
        void boundary_sizeOne() {
            int result = pagingPolicy.getValidatedSize(1);

            assertEquals(1, result);
        }

        /**
         * 경계값 테스트 - 크기 최대값
         */
        @Test
        void boundary_sizeMax() {
            int result = pagingPolicy.getValidatedSize(TEST_MAX_SIZE);

            assertEquals(TEST_MAX_SIZE, result);
        }

        /**
         * 경계값 테스트 - 크기 최대값 + 1
         */
        @Test
        void boundary_sizeMaxPlusOne() {
            int result = pagingPolicy.getValidatedSize(TEST_MAX_SIZE + 1);

            assertEquals(TEST_MAX_SIZE, result);
        }

        /**
         * 극한값 처리 - 매우 큰 페이지 번호
         */
        @Test
        void extreme_veryLargePage() {
            Integer hugePage = 999999999;

            Integer result = pagingPolicy.getValidatedPage(hugePage);

            assertEquals(hugePage, result);
        }

        /**
         * 극한값 처리 - 매우 큰 페이지 크기
         */
        @Test
        void extreme_veryLargeSize() {
            int hugeSize = 999999999;

            int result = pagingPolicy.getValidatedSize(hugeSize);

            assertEquals(TEST_MAX_SIZE, result);
        }
    }

    // ========================= 실제 사용 시나리오 테스트 =========================
    @Nested
    class RealWorldScenarioTest {

        /**
         * 일반적인 웹 요청 시나리오 - 첫 페이지
         */
        @Test
        void scenario_firstPageRequest() {
            Integer page = pagingPolicy.getValidatedPage(0);
            int size = pagingPolicy.getValidatedSize(20);

            assertEquals(0, page);
            assertEquals(20, size);
        }

        /**
         * 일반적인 웹 요청 시나리오 - 두 번째 페이지
         */
        @Test
        void scenario_secondPageRequest() {
            Integer page = pagingPolicy.getValidatedPage(1);
            int size = pagingPolicy.getValidatedSize(20);

            assertEquals(1, page);
            assertEquals(20, size);
        }

        /**
         * 모바일 요청 시나리오 - 작은 페이지 크기
         */
        @Test
        void scenario_mobileRequest() {
            Integer page = pagingPolicy.getValidatedPage(0);
            int size = pagingPolicy.getValidatedSize(10);

            assertEquals(0, page);
            assertEquals(10, size);
        }

        /**
         * 관리자 요청 시나리오 - 큰 페이지 크기
         */
        @Test
        void scenario_adminRequest() {
            Integer page = pagingPolicy.getValidatedPage(0);
            int size = pagingPolicy.getValidatedSize(50);

            assertEquals(0, page);
            assertEquals(50, size);
        }

        /**
         * 잘못된 요청 시나리오 - 파라미터 누락
         */
        @Test
        void scenario_missingParameters() {
            Integer page = pagingPolicy.getValidatedPage(null);
            int size = pagingPolicy.getValidatedSize(0);

            assertEquals(TEST_DEFAULT_PAGE, page);
            assertEquals(TEST_DEFAULT_SIZE, size);
        }

        /**
         * 악의적 요청 시나리오 - 비정상적으로 큰 값
         */
        @Test
        void scenario_maliciousRequest() {
            Integer page = pagingPolicy.getValidatedPage(-999);
            int size = pagingPolicy.getValidatedSize(999999);

            assertEquals(TEST_DEFAULT_PAGE, page);
            assertEquals(TEST_MAX_SIZE, size);
        }

        /**
         * 대량 데이터 처리 시나리오 - 최대 크기 요청
         */
        @Test
        void scenario_bulkDataProcessing() {
            Integer page = pagingPolicy.getValidatedPage(0);
            int size = pagingPolicy.getValidatedSize(TEST_MAX_SIZE);

            assertEquals(0, page);
            assertEquals(TEST_MAX_SIZE, size);
        }
    }

    // ========================= 통합 테스트 =========================
    @Nested
    class IntegrationTest {

        /**
         * 통합 테스트 - 모든 메서드가 함께 올바르게 작동하는지 확인
         */
        @Test
        void integration_allMethodsWorkTogether() {
            // Given
            Integer requestPage = 2;
            int requestSize = 25;

            // When
            Integer validatedPage = pagingPolicy.getValidatedPage(requestPage);
            int validatedSize = pagingPolicy.getValidatedSize(requestSize);
            String sortField = pagingPolicy.getTransactionSortField();

            // Then
            assertEquals(2, validatedPage);
            assertEquals(25, validatedSize);
            assertEquals("createdTimeStamp", sortField);

            // 모든 결과가 유효한지 확인
            assertTrue(validatedPage >= 0);
            assertTrue(validatedSize > 0);
            assertTrue(validatedSize <= TEST_MAX_SIZE);
            assertNotNull(sortField);
            assertFalse(sortField.trim().isEmpty());
        }

        /**
         * 통합 테스트 - 다양한 조합 시나리오
         */
        @ParameterizedTest
        @MethodSource("integrationScenarioProvider")
        void integration_variousScenarios(Integer inputPage, int inputSize,
                                          Integer expectedPage, int expectedSize, String testName) {
            Integer actualPage = pagingPolicy.getValidatedPage(inputPage);
            int actualSize = pagingPolicy.getValidatedSize(inputSize);

            assertEquals(expectedPage, actualPage, testName + " - 페이지 검증 실패");
            assertEquals(expectedSize, actualSize, testName + " - 크기 검증 실패");
        }

        private static Stream<Arguments> integrationScenarioProvider() {
            return Stream.of(
                Arguments.of(null, 0, 0, 20, "기본 값 시나리오"),
                Arguments.of(0, 10, 0, 10, "정상 요청 시나리오"),
                Arguments.of(-1, -5, 0, 20, "음수 입력 시나리오"),
                Arguments.of(5, 150, 5, 100, "크기 초과 시나리오"),
                Arguments.of(999, 1, 999, 1, "극한 페이지 시나리오")
            );
        }
    }
}