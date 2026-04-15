package com.pms.coupon;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import com.pms.coupon.support.IntegrationTestContainers;

@SpringBootTest
@ActiveProfiles("test")
class PmsCouponApplicationTests extends IntegrationTestContainers {

	@Test
	void contextLoads() {
	}

}
