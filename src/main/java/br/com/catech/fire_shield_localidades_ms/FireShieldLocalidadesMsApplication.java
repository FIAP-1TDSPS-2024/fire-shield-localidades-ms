package br.com.catech.fire_shield_localidades_ms;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableFeignClients
@EnableScheduling
public class FireShieldLocalidadesMsApplication {

	public static void main(String[] args) {
		SpringApplication.run(FireShieldLocalidadesMsApplication.class, args);
	}

}
