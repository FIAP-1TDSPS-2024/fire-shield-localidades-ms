package br.com.catech.fire_shield_localidades_ms.external_interface.feign;

import br.com.catech.fire_shield_localidades_ms.dto.EnderecoDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "localidade-api")
public interface LocalidadeAppClient {

    @GetMapping("/reverse")
    EnderecoDto buscarEnderecoPorCoordenadas(
            @RequestParam("lat") double latitude,
            @RequestParam("lon") double longitude,
            @RequestParam(name = "format", required = false, defaultValue = "json") String format);
}
