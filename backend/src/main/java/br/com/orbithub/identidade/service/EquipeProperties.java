package br.com.orbithub.identidade.service;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 10:39
 * Cartão Jira: KAN-75
 * Título: Configuração da equipe interna
 * Objetivo: Reunir os prazos do convite, do segundo fator e da revisão de acessos, e os dados do primeiro
 *           administrador criado na implantação.
 * Decisões: Valores padrão do catálogo de parâmetros (convite.validade_dias = 14, acesso.revisao_meses = 3).
 *           Passam a ser parâmetros editáveis no painel com a KAN-78. O primeiro administrador vem de variáveis
 *           de ambiente e só é criado se ainda não existir nenhum administrador ativo.
 */
@ConfigurationProperties("orbit.equipe")
public record EquipeProperties(Duration convite, Duration desafioSegundoFator, int revisaoMeses,
                               PrimeiroAdministrador primeiroAdministrador) {

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 10:39
     * Cartão Jira: KAN-75
     * Título: Dados do primeiro administrador
     * Objetivo: Informar e-mail e nome do primeiro administrador da implantação.
     * Decisões: E-mail vazio desliga a criação automática.
     */
    public record PrimeiroAdministrador(String email, String nome) {
    }
}
