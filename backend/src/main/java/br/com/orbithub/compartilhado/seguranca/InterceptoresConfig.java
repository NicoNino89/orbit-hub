package br.com.orbithub.compartilhado.seguranca;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 00:40
 * Cartão Jira: KAN-92
 * Título: Registro dos interceptores da API
 * Objetivo: Ligar o bloqueio por aceite pendente às rotas /api/**.
 * Decisões: Interceptor do Spring MVC (e não filtro), porque roda depois da autenticação e só nas rotas
 *           de controller.
 */
@Configuration(proxyBeanMethods = false)
class InterceptoresConfig implements WebMvcConfigurer {

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 00:40
     * Cartão Jira: KAN-92
     * Título: Registro do interceptor de aceite
     * Objetivo: Aplicar o bloqueio a todas as rotas da API.
     * Decisões: As exceções ficam dentro do próprio interceptor, num só lugar.
     */
    @Override
    public void addInterceptors(InterceptorRegistry registro) {
        registro.addInterceptor(new AceitePendenteInterceptor()).addPathPatterns("/api/**");
    }
}
