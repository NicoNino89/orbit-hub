package br.com.orbithub.compartilhado.seguranca;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import java.time.Clock;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 00:20
 * Cartão Jira: KAN-57
 * Título: Configuração de segurança da API
 * Objetivo: Proteger a API com access token JWT (stateless), liberar só as rotas públicas e montar
 *           a assinatura, a conferência e a conversão dos tokens.
 * Decisões: Sem sessão HTTP no servidor; o refresh token vive em cookie HttpOnly só nas rotas de
 *           autenticação. CSRF desligado porque nenhuma rota autenticada usa cookie: o access token vai no
 *           cabeçalho Authorization, e o cookie de refresh é SameSite=Strict. As regras por papel ficam nos
 *           métodos (@PreAuthorize), verificadas no servidor (KAN-34).
 */
@Configuration(proxyBeanMethods = false)
@EnableMethodSecurity
@EnableConfigurationProperties(SegurancaProperties.class)
public class SegurancaConfig {

    /** Rotas abertas sem login: saúde, métricas, contrato da API e autenticação. */
    static final String[] ROTAS_PUBLICAS = {
        "/actuator/health/**",
        "/actuator/prometheus",
        "/api/v1/sobre",
        "/v3/api-docs/**",
        "/swagger-ui.html",
        "/swagger-ui/**",
        "/api/v1/auth/**",
        "/error"
    };

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 00:20
     * Cartão Jira: KAN-57
     * Título: Cadeia de filtros de segurança
     * Objetivo: Definir rotas públicas, exigir token no resto e ligar o resource server de JWT.
     * Decisões: Stateless; CORS pela política de CorsConfig; qualquer rota nova nasce protegida.
     */
    @Bean
    SecurityFilterChain cadeiaDeSeguranca(HttpSecurity http, JwtAuthenticationConverter conversor) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .cors(Customizer.withDefaults())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .httpBasic(b -> b.disable())
                .formLogin(f -> f.disable())
                .authorizeHttpRequests(a -> a
                        .requestMatchers(ROTAS_PUBLICAS).permitAll()
                        .anyRequest().authenticated())
                .oauth2ResourceServer(o -> o.jwt(j -> j.jwtAuthenticationConverter(conversor)));
        return http.build();
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 00:20
     * Cartão Jira: KAN-57
     * Título: Chave do JWT
     * Objetivo: Disponibilizar a chave HMAC para assinar e conferir tokens.
     * Decisões: Regras de obrigatoriedade e tamanho em {@link ChaveJwt}.
     */
    @Bean
    SecretKey chaveJwt(SegurancaProperties propriedades, @Value("${orbit.ambiente}") String ambiente) {
        return ChaveJwt.montar(propriedades.jwt().chave(), ambiente);
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 00:20
     * Cartão Jira: KAN-57
     * Título: Assinador de tokens
     * Objetivo: Criar o codificador que assina os access tokens.
     * Decisões: HS256 com a chave do Key Vault; a API é a única que emite e confere, então chave simétrica
     *           basta e evita gerir par de chaves.
     */
    @Bean
    JwtEncoder codificadorJwt(SecretKey chave) {
        return new NimbusJwtEncoder(new ImmutableSecret<>(chave));
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 00:20
     * Cartão Jira: KAN-57
     * Título: Conferência de tokens
     * Objetivo: Criar o decodificador que confere assinatura, emissor, validade e sessão.
     * Decisões: Algoritmo fixo em HS256 (recusa "alg" trocado); validadores padrão de tempo e emissor mais
     *           o de sessão ativa.
     */
    @Bean
    JwtDecoder decodificadorJwt(SecretKey chave, SegurancaProperties propriedades, VerificadorDeSessao verificador) {
        NimbusJwtDecoder decodificador = NimbusJwtDecoder.withSecretKey(chave)
                .macAlgorithm(MacAlgorithm.HS256)
                .build();
        decodificador.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
                JwtValidators.createDefaultWithIssuer(propriedades.jwt().emissor()),
                new SessaoAtivaValidator(verificador)));
        return decodificador;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 00:20
     * Cartão Jira: KAN-57
     * Título: Papéis a partir do token
     * Objetivo: Transformar a claim "papeis" nas autoridades ROLE_* do Spring Security.
     * Decisões: O "sub" do token é o id da conta; os papéis vêm do token para não consultar o banco.
     */
    @Bean
    JwtAuthenticationConverter conversorDeAutenticacao() {
        JwtGrantedAuthoritiesConverter papeis = new JwtGrantedAuthoritiesConverter();
        papeis.setAuthoritiesClaimName("papeis");
        papeis.setAuthorityPrefix("ROLE_");
        JwtAuthenticationConverter conversor = new JwtAuthenticationConverter();
        conversor.setJwtGrantedAuthoritiesConverter(papeis);
        return conversor;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 00:20
     * Cartão Jira: KAN-57
     * Título: Codificador de senhas
     * Objetivo: Gerar e conferir hashes de senha.
     * Decisões: Codificador delegante do Spring (bcrypt hoje), que grava o algoritmo no hash e permite
     *           trocar de algoritmo no futuro sem invalidar senhas antigas.
     */
    @Bean
    PasswordEncoder codificadorDeSenhas() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 00:20
     * Cartão Jira: KAN-57
     * Título: Relógio da aplicação
     * Objetivo: Fornecer o instante atual para emissão e expiração de tokens.
     * Decisões: Relógio injetado, em UTC, para os testes controlarem o tempo.
     */
    @Bean
    Clock relogio() {
        return Clock.systemUTC();
    }
}
