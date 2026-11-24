package br.com.orbithub.identidade.service;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 11:08
 * Cartão Jira: KAN-78
 * Título: Configuração da equipe interna
 * Objetivo: Reunir o tempo para digitar o código do segundo fator e os dados do primeiro administrador criado na
 *           implantação.
 * Decisões: Só o que é técnico fica aqui; validade do convite e intervalo da revisão de acessos viraram parâmetros
 *           editáveis (KAN-78). O primeiro administrador vem de variáveis de ambiente e só é criado se ainda não
 *           existir nenhum administrador ativo.
 */
@ConfigurationProperties("orbit.equipe")
public record EquipeProperties(Duration desafioSegundoFator, PrimeiroAdministrador primeiroAdministrador) {

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
