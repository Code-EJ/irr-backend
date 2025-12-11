package org.code.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
/*exclude = {
    DataSourceAutoConfiguration.class,
    HibernateJpaAutoConfiguration.class,
    DataSourceTransactionManagerAutoConfiguration.class,
    }*/
public class IrrApplication {

  public static void main(String[] args) {
    SpringApplication.run(IrrApplication.class, args);
  }
}
