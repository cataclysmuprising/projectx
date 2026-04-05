package com.github.projectx;

import com.github.projectx.persistence.PersistenceApplication;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.testng.AbstractTransactionalTestNGSpringContextTests;
import org.springframework.transaction.annotation.Transactional;
import org.testng.SkipException;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.concurrent.atomic.AtomicBoolean;

@ActiveProfiles("dev")
@SpringBootTest(
		classes = {PersistenceApplication.class},
		webEnvironment = SpringBootTest.WebEnvironment.NONE
)
@Transactional
public class CommonTestBase extends AbstractTransactionalTestNGSpringContextTests {
	protected static final long TEST_CREATE_USER_ID = 10009L;
	protected static final long TEST_UPDATE_USER_ID = 90001L;
	protected static final Logger testLogger = LogManager.getLogger("testLogs." + CommonTestBase.class.getName());
	private static final AtomicBoolean ADMIN_HISTORY_FK_CHECKED = new AtomicBoolean(false);

	@BeforeMethod
	public void beforeMethod(Method method) {
		testLogger.info("***** Unit-TEST : Testing method '{}' has started. *****", method.getName());
		//MockitoAnnotations.openMocks(this); // This could be pulled up into a shared base class
	}

	@AfterMethod
	public void afterMethod(Method method) {
		testLogger.info("----- Unit-TEST : Testing method '{}' has finished. -----", method.getName());
	}

	protected <T> void showEntriesOfCollection(Collection<T> collection) {
		if (collection != null) {
			for (Object obj : collection) {
				testLogger.info(" >>> {}", obj.toString());
			}
		}
	}

	protected void requireOptInTestProperty(String propertyName, String description) {
		if (!Boolean.parseBoolean(System.getProperty(propertyName, "false"))) {
			throw new SkipException("Skipped opt-in test. Set -D" + propertyName + "=true to run " + description + ".");
		}
	}
}
