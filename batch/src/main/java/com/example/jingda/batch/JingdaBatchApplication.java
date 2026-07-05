package com.example.jingda.batch;

import com.example.jingda.batch.accounting.CreditCardRepaymentJob;
import java.time.LocalDate;
import java.time.ZoneId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class JingdaBatchApplication {

    private static final Logger log = LoggerFactory.getLogger(JingdaBatchApplication.class);
    private static final String CREDIT_CARD_REPAYMENT = "credit-card-repayment";

    public static void main(String[] args) {
        SpringApplication application = new SpringApplication(JingdaBatchApplication.class);
        application.setWebApplicationType(WebApplicationType.NONE);
        System.exit(SpringApplication.exit(application.run(args)));
    }

    @Bean
    CommandLineRunner runBatch(
            CreditCardRepaymentJob creditCardRepaymentJob,
            @Value("${app.batch.code}") String batchCode,
            @Value("${app.batch.target-date:}") String targetDate,
            @Value("${app.batch.zone:Asia/Tokyo}") String zone
    ) {
        return args -> {
            LocalDate executionDate = targetDate == null || targetDate.isBlank()
                    ? LocalDate.now(ZoneId.of(zone))
                    : LocalDate.parse(targetDate);
            if (!CREDIT_CARD_REPAYMENT.equals(batchCode)) {
                throw new IllegalArgumentException("指定された batch は存在しません：" + batchCode);
            }
            int processedCount = creditCardRepaymentJob.repayDueCards(executionDate);
            log.info("Batch {} finished. targetDate={}, processedCount={}", batchCode, executionDate, processedCount);
        };
    }
}
