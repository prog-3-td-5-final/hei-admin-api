package school.hei.haapi.integration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ContextConfiguration;
import org.testcontainers.junit.jupiter.Testcontainers;
import school.hei.haapi.SentryConf;
import school.hei.haapi.endpoint.rest.api.PayingApi;
import school.hei.haapi.endpoint.rest.client.ApiClient;
import school.hei.haapi.endpoint.rest.client.ApiException;
import school.hei.haapi.endpoint.rest.model.CreateDelayPenaltyChange;
import school.hei.haapi.endpoint.rest.model.DelayPenalty;
import school.hei.haapi.endpoint.rest.model.Fee;
import school.hei.haapi.endpoint.rest.security.cognito.CognitoComponent;
import school.hei.haapi.integration.conf.AbstractContextInitializer;
import school.hei.haapi.integration.conf.TestUtils;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static school.hei.haapi.integration.conf.TestUtils.MANAGER1_TOKEN;
import static school.hei.haapi.integration.conf.TestUtils.STUDENT1_ID;
import static school.hei.haapi.integration.conf.TestUtils.anAvailableRandomPort;
import static school.hei.haapi.integration.conf.TestUtils.setUpCognito;

@SpringBootTest(webEnvironment = RANDOM_PORT)
@Testcontainers
@ContextConfiguration(initializers = TestIT.ContextInitializer.class)
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class TestIT {
  @MockBean
  private SentryConf sentryConf;
  @MockBean
  private CognitoComponent cognitoComponentMock;

  private static ApiClient anApiClient(String token) {
    return TestUtils.anApiClient(token, TestIT.ContextInitializer.SERVER_PORT);
  }

  static DelayPenalty delayPenalty() {
    return new DelayPenalty()
            .id("delay_penalty_id")
            .interestPercent(2)
            .interestTimerate(DelayPenalty.InterestTimerateEnum.DAILY)
            .graceDelay(3)
            .applicabilityDelayAfterGrace(3)
            .creationDatetime(Instant.parse("2022-11-15T08:25:25.00Z"));
  }

  static CreateDelayPenaltyChange createDelayPenaltyChange(){
    return new CreateDelayPenaltyChange()
            .interestPercent(2)
            .interestTimerate(CreateDelayPenaltyChange.InterestTimerateEnum.DAILY)
            .graceDelay(3)
            .applicabilityDelayAfterGrace(3);
  }

  @BeforeEach
  void setUp() {
    setUpCognito(cognitoComponentMock);
  }

  @Test
  void student_fee_change_after_change_delay_penalty_ok() throws ApiException {
    ApiClient manager1Client = anApiClient(MANAGER1_TOKEN);
    PayingApi api = new PayingApi(manager1Client);

    List<Fee> beforeChanges = api.getStudentFees(STUDENT1_ID,1,15,null);

    CreateDelayPenaltyChange createDelayPenaltyChange = createDelayPenaltyChange();
    DelayPenalty delayPenalty = api.createDelayPenaltyChange(createDelayPenaltyChange);

    List<Fee> actualFees = api.getStudentFees(STUDENT1_ID,1,15,null);

    assertEquals(3,delayPenalty.getGraceDelay());
    assertTrue(actualFees.containsAll(beforeChanges));
  }

  @Test
  void test1() throws ApiException {
    ApiClient manager1Client = anApiClient(MANAGER1_TOKEN);
    PayingApi api = new PayingApi(manager1Client);

    Fee fee = api.getStudentFeeById(STUDENT1_ID, "fee10_id");

    CreateDelayPenaltyChange createDelayPenaltyChange = createDelayPenaltyChange();
    createDelayPenaltyChange.setGraceDelay(1);
    DelayPenalty delayPenalty = api.createDelayPenaltyChange(createDelayPenaltyChange);

    Fee actualFee = api.getStudentFeeById(STUDENT1_ID, "fee10_id");

    assertEquals(204000,actualFee.getRemainingAmount());
  }


  static class ContextInitializer extends AbstractContextInitializer {
    public static final int SERVER_PORT = anAvailableRandomPort();

    @Override
    public int getServerPort() {
      return SERVER_PORT;
    }
  }
}
