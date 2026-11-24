package br.com.orbithub.auditoria.api;

import br.com.orbithub.auditoria.service.TrilhaDeAuditoria;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 10:09
 * Cartão Jira: KAN-80
 * Título: Ligação do registro automático às rotas do backoffice
 * Objetivo: Aplicar o interceptor de auditoria a todas as rotas /api/v1/admin/**.
 * Decisões: Configuração do próprio módulo de auditoria, separada da de segurança.
 */
@Configuration(proxyBeanMethods = false)
class AuditoriaWebConfig implements WebMvcConfigurer {

    static final String ROTAS_DO_BACKOFFICE = "/api/v1/admin/**";

    private final ObjectProvider<TrilhaDeAuditoria> trilha;

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 10:09
     * Cartão Jira: KAN-80
     * Título: Criação da configuração
     * Objetivo: Receber a trilha usada pelo interceptor.
     * Decisões: ObjectProvider, para a configuração web subir também nos testes de fatia (@WebMvcTest), que
     *           não carregam os serviços.
     */
    AuditoriaWebConfig(ObjectProvider<TrilhaDeAuditoria> trilha) {
        this.trilha = trilha;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 10:09
     * Cartão Jira: KAN-80
     * Título: Registro do interceptor
     * Objetivo: Ligar o interceptor às rotas do backoffice.
     * Decisões: Só o backoffice; as rotas dos usuários da plataforma têm históricos próprios.
     */
    @Override
    public void addInterceptors(InterceptorRegistry registro) {
        registro.addInterceptor(new AuditoriaInterceptor(trilha)).addPathPatterns(ROTAS_DO_BACKOFFICE);
    }
}
