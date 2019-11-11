package br.com.uol.partnership.consumer.test;

import br.com.uol.partnership.consumer.domain.model.ServiceVoucher;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.io.Files;
import org.junit.runner.RunWith;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.sql.Timestamp;
import java.util.List;

import static com.jayway.restassured.RestAssured.given;
import static java.util.UUID.randomUUID;
import static org.apache.http.HttpStatus.SC_OK;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.springframework.amqp.core.MessageDeliveryMode.PERSISTENT;
import static org.springframework.amqp.core.MessageProperties.CONTENT_TYPE_JSON;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static org.springframework.data.mongodb.core.query.Criteria.where;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = RANDOM_PORT)
public abstract class AbstractBaseTest extends AbstractTest{

    protected static final String SERVICE_ID = "20180204123456999999900001";

    protected static final String SERVICE_ID_VOUCHER_NOT_FOUND = "20180204123456999999900003";

    protected static final String SERVICE_ID_NOT_FOUND = "11111111111111111111111111";

    protected static final String NEW_SERVICE_ID = getTimestamp();

    protected static final String VOUCHER_TOKEN = "eaf4c24d-9c85-4f52-a2d7-85a093625a7b";

    protected static final String NEW_VOUCHER_TOKEN = randomUUID().toString();

    protected static final String VOUCHER_TOKEN_NOT_FOUND = "11111111-1111-1111-1111-111111111111";

    protected static final String PARTNER_CODE = "TIM";

    protected static final String PRODUCT_CODE = "VALIDAR";

    protected static final String EMAIL = "l-dev-canais-de-venda@uolinc.com";

    protected static final String COMPANY_NAME = "UOL Novos Canais";

    protected static final String CNPJ = "87756840000167";

    @Value("${voucher.data.service.url}")
    private String url;

    @Autowired
    protected ObjectMapper mapper;

    @Autowired
    private MongoTemplate mongoTemplate;

    public MongoTemplate mongoTemplate() {
        return mongoTemplate;
    }

    public String remoteAddress() {
        return url;
    }

    protected Message buildMessage(Object object)
            throws JsonProcessingException {

        byte[] json = mapper.writeValueAsBytes(object);
        return MessageBuilder
        		.withBody(json)
                .setDeliveryMode(PERSISTENT)
                .setContentType(CONTENT_TYPE_JSON)
                .setContentEncoding("UTF-8")
               .build();
    }

    protected void assertVoucher(String serviceId, String status)
            throws Exception {

        Thread.sleep(5000);

        Query query = new Query();
        query.addCriteria(where("serviceId").is(serviceId));
        List<ServiceVoucher> services = mongoTemplate.find(query,
                ServiceVoucher.class);

        assertEquals(1, services.size());

        ServiceVoucher service = services.get(0);
        assertNotNull(service);
        assertNotNull(service.getId());
        assertEquals(serviceId, service.getServiceId());
        assertNotNull(service.getVoucherToken());
        assertNotNull(service.getCreatedDate());

        given()
            .log().all()
            .when()
                .get(remoteAddress() + "/rs/partners/TIM/products/vouchers/"
                        + service.getVoucherToken())
            .then()
                .log().everything()
                .assertThat()
                .statusCode(SC_OK)
                    .body("item.token", equalTo(service.getVoucherToken()))
                    .body("item.number", equalTo(serviceId))
                    .body("item.status", equalTo(status))
                    .body("item.type.code", equalTo(PRODUCT_CODE))
                    .body("item.type.partner.partner-code", equalTo(PARTNER_CODE))
                    .body("item.additional-info.company-name", equalTo(COMPANY_NAME))
                    .body("item.additional-info.cnpj", equalTo(CNPJ))
                    .body("item.additional-info.e-mail", equalTo(EMAIL));
    }
}